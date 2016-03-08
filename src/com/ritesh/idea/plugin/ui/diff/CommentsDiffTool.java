/*
 * Copyright 2015 Ritesh Kapoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritesh.idea.plugin.ui.diff;

import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.impl.DiffPanelImpl;
import com.intellij.openapi.diff.impl.DiffUtil;
import com.intellij.openapi.diff.impl.external.FrameDiffTool;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.ritesh.idea.plugin.reviewboard.Review;
import com.ritesh.idea.plugin.reviewboard.Review.File.Comment;
import com.ritesh.idea.plugin.ui.panels.CommentPanel;
import com.ritesh.idea.plugin.ui.panels.CommentsListViewPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ritesh
 */
public class CommentsDiffTool extends FrameDiffTool {
    private Review.File file;
    private List<Comment> comments;
    private List<RangeHighlighter> newCommentHighlighters = new ArrayList<>();
    private ListCellRenderer<Comment> listCellRenderer;
    private ActionListener actionListener;

    public CommentsDiffTool(Review.File file, List<Comment> comments) {
        this.file = file;
        this.comments = new ArrayList<>(comments);
        this.listCellRenderer = new ListCellRenderer<Comment>() {
            @Override
            public Component getListCellRendererComponent(JList list, final Comment value, final int index, boolean isSelected, boolean cellHasFocus) {
                return new CommentPanel(value.user, value.text, value.timestamp).getPanel();
            }
        };
    }

    @Override
    public void show(DiffRequest request) {
        final FrameWrapper frameWrapper = new FrameWrapper(request.getProject(), request.getGroupKey());
        final DiffPanelImpl diffPanel = createDiffPanelImpl(request, frameWrapper.getFrame(), frameWrapper);
        final Editor editor = diffPanel.getEditor2();
        updateHighLights(editor);

        editor.addEditorMouseListener(new EditorMouseAdapter() {
            @Override
            public void mouseClicked(EditorMouseEvent e) {
                if (e.getArea() != null && e.getArea().equals(EditorMouseEventArea.LINE_MARKERS_AREA)) {
                    final Point locationOnScreen = e.getMouseEvent().getLocationOnScreen();
                    final int lineNumber = EditorUtil.yPositionToLogicalLine(editor, e.getMouseEvent()) + 1;
                    showCommentsView(locationOnScreen, lineNumber, editor);
                }
            }
        });

        DiffUtil.initDiffFrame(request.getProject(), frameWrapper, diffPanel, diffPanel.getComponent());
        frameWrapper.setTitle(request.getWindowTitle());
        frameWrapper.show();
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void showCommentsView(Point locationOnScreen, final int lineNumber, final Editor editor) {
        List<Comment> comments = lineComments(CommentsDiffTool.this.comments).get(lineNumber);

        final CommentsListViewPanel<Comment> commentsListViewPanel = new CommentsListViewPanel(comments, listCellRenderer);
        final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(commentsListViewPanel, null)
                .setTitle("Comment")
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setAdText("Hit Ctrl+Enter to add comment & double click comment to delete.")
                .setResizable(true)
                .createPopup();
        popup.show(RelativePoint.fromScreen(locationOnScreen));


        commentsListViewPanel.setListener(new CommentsListViewPanel.CommentListener<Comment>() {
            @Override
            public void onAdd(String value, boolean issueOpened) {
                popup.dispose();
                Comment newComment = new Comment();
                newComment.text = value;
                newComment.firstLine = lineNumber;
                newComment.numberOfLines = 1;
                newComment.file = file;
                newComment.issueOpened = issueOpened;
                CommentsDiffTool.this.comments.add(newComment);
                updateHighLights(editor);
                actionListener.actionPerformed(new ActionEvent(this, 0, null));
            }

            @Override
            public void onDelete(Comment value) {
                if (value != null && value.id == null) {
                    CommentsDiffTool.this.comments.remove(value);
                    updateHighLights(editor);
                }
                popup.dispose();
            }
        });
    }

    private void updateHighLights(Editor editor) {
        MarkupModel markup = editor.getMarkupModel();

        for (RangeHighlighter customRangeHighlighter : newCommentHighlighters) {
            markup.removeHighlighter(customRangeHighlighter);
        }
        newCommentHighlighters.clear();

        int lineCount = markup.getDocument().getLineCount();

        Map<Integer, List<Comment>> lineComments = lineComments(comments);
        for (Map.Entry<Integer, List<Comment>> entry : lineComments.entrySet()) {
            if (entry.getKey() > lineCount) continue;

            boolean hasNewComments = false;
            for (Comment comment : entry.getValue()) {
                if (comment.id == null) {
                    hasNewComments = true;
                    break;
                }
            }

            TextAttributes attributes = new TextAttributes();
            if (hasNewComments) attributes.setBackgroundColor(JBColor.PINK);
            else attributes.setBackgroundColor(JBColor.YELLOW);

            RangeHighlighter rangeHighlighter = markup
                    .addLineHighlighter(entry.getKey() - 1, HighlighterLayer.SELECTION + (hasNewComments ? 2 : 1), attributes);
            rangeHighlighter.setGutterIconRenderer(new CommentGutterIconRenderer());
            newCommentHighlighters.add(rangeHighlighter);
        }
    }

    private Map<Integer, List<Comment>> lineComments(List<Comment> comments) {
        Map<Integer, List<Comment>> result = new HashMap<>();
        for (Comment comment : comments) {
            if (result.get(comment.firstLine) == null) {
                result.put(comment.firstLine, new ArrayList<Comment>());
            }
            result.get(comment.firstLine).add(comment);
        }
        return result;
    }

    public List<Comment> getNewComments() {
        final List<Review.File.Comment> newComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.id == null) newComments.add(comment);
        }
        return newComments;
    }
}
