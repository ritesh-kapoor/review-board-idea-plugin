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

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ritesh
 */
public class CommentPanel {
    private JBTextField comment;
    private JBLabel username;
    private JBLabel timestamp;
    private JPanel pane;

    public CommentPanel(String userName, String comment, Date timestamp) {
        this.username.setText(userName);
        this.comment.setText(comment);
        if (timestamp != null) {
            this.timestamp.setText(new SimpleDateFormat("yy-MM-dd HH:mm").format(timestamp));
        }

        this.comment.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        this.comment.setBorder(BorderFactory.createEmptyBorder());
    }

    public JComponent getPanel() {
        return pane;
    }
}
