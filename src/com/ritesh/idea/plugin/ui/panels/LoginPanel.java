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

import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Ritesh
 */
public class LoginPanel {
    private JPanel panel;
    private JTextField url;
    private JPasswordField password;
    private JTextField username;
    private JButton testConnection;
    private JBCheckBox useRbTools;
    private JTextField rbtPath;
    private JCheckBox useRbtPath;

    public LoginPanel() {
        useRbTools.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (useRbTools.isSelected()) {
                    if (useRbtPath.isSelected()) rbtPath.setEnabled(true);
                    useRbtPath.setEnabled(true);
                } else {
                    rbtPath.setEnabled(false);
                    useRbtPath.setEnabled(false);
                }
            }
        });

        rbtPath.setEnabled(false);
        useRbtPath.setEnabled(false);

        useRbtPath.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (useRbtPath.isSelected() && useRbTools.isSelected()) {
                    rbtPath.setEnabled(true);
                } else {
                    rbtPath.setEnabled(false);
                }
            }
        });
    }

    public void addActionListener(ActionListener l) {
        testConnection.addActionListener(l);
    }

    public JComponent getPanel() {
        return panel;
    }

    public String getUrl() {
        return url.getText().trim();
    }

    public String getUsername() {
        return username.getText().trim();
    }

    public String getPassword() {
        return String.valueOf(password.getPassword());
    }

    public void setUrl(String url) {
        this.url.setText(url);
    }

    public void setUsername(String username) {
        this.username.setText(username);
    }

    public void setPassword(String password) {
        this.password.setText(password);
    }

    public Boolean useRbTools() {
        return useRbTools.isSelected();
    }

    public void setUseRbTools(Boolean useRbTools) {
        this.useRbTools.setSelected(useRbTools == Boolean.TRUE);
    }

    public void setUseRbToolPath(String path) {
        if (path == null) {
            rbtPath.setText("");
            this.useRbtPath.setSelected(false);
        } else {
            rbtPath.setText(path);
            this.useRbtPath.setSelected(true);
        }
    }

    public String rbtPath() {
        if (useRbtPath.isSelected() && useRbTools.isSelected()) return rbtPath.getText();
        return null;
    }
}
