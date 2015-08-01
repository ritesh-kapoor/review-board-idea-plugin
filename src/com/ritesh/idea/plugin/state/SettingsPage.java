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

package com.ritesh.idea.plugin.state;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.ui.ErrorManager;
import com.ritesh.idea.plugin.ui.panels.LoginForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Ritesh
 */
public class SettingsPage implements Configurable {

    public static final String NAME = "Review Board";
    private LoginForm form = new LoginForm();
    private Configuration oldState;
    private Project project;

    public SettingsPage(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        oldState = ConfigurationPersistance.getInstance(project).getState();
        if (oldState != null) {
            form.setUrl(oldState.url);
            form.setUsername(oldState.username);
            form.setPassword(oldState.password);
        }
        form.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    form.setMessage("");
                    ReviewDataProvider.getInstance(project)
                            .testConnection(form.getUrl(), form.getUsername(), form.getPassword());
                    form.setMessage("Connection established successfully.");
                } catch (Exception e1) {
                    ErrorManager.Message message = ErrorManager.getMessage(e1);
                    form.setMessage(message.message);
                }
            }
        });
        return form.getPanel();
    }

    @Override
    public boolean isModified() {
        if (oldState == null) {
            return !form.getUrl().isEmpty() || !form.getUsername().isEmpty() || !form.getPassword().isEmpty();
        }
        return !Comparing.equal(form.getUrl(), oldState.url) ||
                !Comparing.equal(form.getUsername(), oldState.username) ||
                !Comparing.equal(form.getPassword(), oldState.password);
    }

    @Override
    public void apply() throws ConfigurationException {
        Configuration configuration = new Configuration(form.getUrl(), form.getUsername(), form.getPassword());
        ConfigurationPersistance.getInstance(project).loadState(configuration);
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
