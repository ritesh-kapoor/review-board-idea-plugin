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

package com.ritesh.idea.plugin.ui.panels;

import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.ritesh.idea.plugin.reviewboard.Review;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

/**
 * @author Ritesh
 */
public class CommentsListViewPanel extends JPanel {
    private JBList commentList = new JBList();
    private EditorTextField commentEditor = new EditorTextField();
    private List<Review.File.Comment> comments;
    private DeleteEventListener onDelete;

    private void initUI() {

        final JBScrollPane scrollPane = new JBScrollPane(commentList);
        Splitter splitter = new Splitter(true);
        splitter.setProportion(0.7f);
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(commentEditor);

        commentEditor.setOneLineMode(false);
        commentList.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        commentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && commentList.getSelectedIndex() > -1
                        && commentList.getSelectedIndex() < comments.size()
                        && comments.get(commentList.getSelectedIndex()).id == null) {
                    commentList.remove(commentList.getSelectedIndex());
                    onDelete.onDelete(new DeleteEvent(e.getSource(), comments.get(commentList.getSelectedIndex())));
                }
            }
        });
        commentList.setCellRenderer(new ListCellRenderer<Review.File.Comment>() {
            @Override
            public Component getListCellRendererComponent(JList list, final Review.File.Comment value, final int index, boolean isSelected, boolean cellHasFocus) {
                return new CommentPanel(value.user, value.text, value.timestamp).getPanel();
            }
        });

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 200));
        add(splitter);
    }

    public void addListener(Action action) {
        commentEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "postComment");
        commentEditor.getActionMap().put("postComment", action);
    }

    public void addOnDeleteListener(DeleteEventListener onDelete) {
        this.onDelete = onDelete;
    }

    public interface DeleteEventListener extends EventListener {
        void onDelete(DeleteEvent event);
    }

    public class DeleteEvent extends EventObject {
        private final Review.File.Comment comment;

        public DeleteEvent(Object source, Review.File.Comment comment) {
            super(source);
            this.comment = comment;
        }

        public Review.File.Comment getComment() {
            return comment;
        }
    }

    public String getComment() {
        return commentEditor.getText();
    }

    @SuppressWarnings("unchecked")
    public CommentsListViewPanel(final List<Review.File.Comment> comments) {
        this.comments = comments;
        initUI();

        DefaultListModel<Review.File.Comment> model = new DefaultListModel<>();
        if (comments != null) {
            for (Review.File.Comment comment : comments) {
                model.addElement(comment);
            }
        }
        commentList.setModel(model);
    }

}
