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
import com.ritesh.idea.plugin.ui.panels.CommentsListViewPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ritesh
 */
public class CommentsDiffTool extends FrameDiffTool {
    private Review.File file;
    private List<Review.File.Comment> comments;
    private List<RangeHighlighter> customRangeHighlighters = new ArrayList<>();

    public CommentsDiffTool(Review.File file, List<Review.File.Comment> comments) {
        this.file = file;
        this.comments = comments;
    }

    @Override
    public void show(DiffRequest request) {
        final FrameWrapper frameWrapper = new FrameWrapper(request.getProject(), request.getGroupKey());
        frameWrapper.setTitle(request.getWindowTitle());

        final DiffPanelImpl diffPanel = createDiffPanelImpl(request, frameWrapper.getFrame(), frameWrapper);
        final Editor editor = diffPanel.getEditor2();
        highlightComments(editor);

        editor.addEditorMouseListener(new EditorMouseAdapter() {
            @Override
            public void mouseClicked(EditorMouseEvent e) {
                if (e.getArea().equals(EditorMouseEventArea.LINE_MARKERS_AREA)) {
                    final int lineNumber = EditorUtil.yPositionToLogicalLine(editor, e.getMouseEvent()) + 1;
                    final CommentsListViewPanel commentsListViewPanel = new CommentsListViewPanel(lineComments(comments).get(lineNumber));
                    final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(commentsListViewPanel, null)
                            .setTitle("Comment")
                            .setMovable(true)
                            .setAdText("Hit Ctrl+Enter to add comment & double click comment to delete.")
                            .setResizable(true)
                            .createPopup();
                    popup.show(RelativePoint.fromScreen(e.getMouseEvent().getLocationOnScreen()));

                    commentsListViewPanel.addOnDeleteListener(new CommentsListViewPanel.DeleteEventListener() {
                        @Override
                        public void onDelete(CommentsListViewPanel.DeleteEvent event) {
                            comments.remove(event.getComment());
                            highlightComments(editor);
                            popup.dispose();
                        }
                    });

                    commentsListViewPanel.addListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            popup.dispose();
                            Review.File.Comment comment = new Review.File.Comment();
                            comment.text = commentsListViewPanel.getComment();
                            comment.firstLine = lineNumber;
                            comment.numberOfLines = 1;
                            comment.file = file;
                            comments.add(comment);
                            highlightComments(editor);
                        }
                    });
                }
            }
        });

        DiffUtil.initDiffFrame(request.getProject(), frameWrapper, diffPanel, diffPanel.getComponent());
        frameWrapper.show();
    }

    private void highlightComments(Editor editor) {
        MarkupModel markup = editor.getMarkupModel();
        int lineCount = markup.getDocument().getLineCount();

        for (RangeHighlighter customRangeHighlighter : customRangeHighlighters) {
            markup.removeHighlighter(customRangeHighlighter);
        }
        customRangeHighlighters.clear();

        Map<Integer, List<Review.File.Comment>> lineComments = lineComments(comments);
        for (Map.Entry<Integer, List<Review.File.Comment>> entry : lineComments.entrySet()) {
            if (entry.getKey() > lineCount) continue;

            boolean containsNew = false;
            for (Review.File.Comment comment : entry.getValue()) {
                if (comment.id == null) {
                    containsNew = true;
                    break;
                }
            }

            TextAttributes attributes = new TextAttributes();
            if (containsNew) attributes.setBackgroundColor(JBColor.PINK);
            else attributes.setBackgroundColor(JBColor.YELLOW);

            RangeHighlighter rangeHighlighter = markup
                    .addLineHighlighter(entry.getKey() - 1, HighlighterLayer.SELECTION + (containsNew ? 2 : 1), attributes);
            rangeHighlighter.setGutterIconRenderer(new CommentGutterIconRenderer());
            customRangeHighlighters.add(rangeHighlighter);
        }
    }

    private Map<Integer, List<Review.File.Comment>> lineComments(List<Review.File.Comment> comments) {
        Map<Integer, List<Review.File.Comment>> result = new HashMap<>();
        for (Review.File.Comment comment : comments) {
            if (result.get(comment.firstLine) == null) {
                result.put(comment.firstLine, new ArrayList<Review.File.Comment>());
            }
            result.get(comment.firstLine).add(comment);
        }
        return result;
    }

    public List<Review.File.Comment> getComments() {
        return comments;
    }
}
