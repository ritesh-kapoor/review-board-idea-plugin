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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.List;

/**
 * @author Ritesh
 */
public class CommentsListViewPanel<T> extends JPanel {
    private final JBList commentList = new JBList();
    private final EditorTextField commentEditor = new EditorTextField();
    private final ListCellRenderer listCellRenderer;

    private CommentListener listener;

    @SuppressWarnings("unchecked")
    public CommentsListViewPanel(List<T> list, ListCellRenderer listCellRenderer) {
        this.listCellRenderer = listCellRenderer;
        DefaultListModel listModel = new DefaultListModel();
        if (list != null) {
            for (T t : list) {
                listModel.addElement(t);
            }
        }
        commentList.setModel(listModel);
        initUI();
    }

    public void setListener(CommentListener<T> listener) {
        this.listener = listener;
    }

    private void initUI() {
        final JBScrollPane scrollPane = new JBScrollPane(commentList);
        Splitter splitter = new Splitter(true);
        splitter.setProportion(0.7f);
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(commentEditor);

        commentEditor.setOneLineMode(false);
        commentList.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        commentList.setCellRenderer(listCellRenderer);
        commentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && commentList.getSelectedIndex() > -1) {
                    listener.onDelete(commentList.getModel().getElementAt(commentList.getSelectedIndex()));
                }
            }
        });
        commentEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "postComment");
        commentEditor.getActionMap().put("postComment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommentsListViewPanel.this.listener.onAdd(commentEditor.getText());
            }
        });

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 200));
        add(splitter);
    }

    public interface CommentListener<T> extends EventListener {
        void onAdd(String value);

        void onDelete(T value);
    }
}
