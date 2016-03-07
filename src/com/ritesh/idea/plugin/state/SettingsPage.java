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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.components.JBCheckBox;
import com.ritesh.idea.plugin.exception.InvalidConfigurationException;
import com.ritesh.idea.plugin.messages.PluginBundle;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.ui.ExceptionHandler;
import com.ritesh.idea.plugin.ui.TaskUtil;
import com.ritesh.idea.plugin.ui.panels.LoginPanel;
import com.ritesh.idea.plugin.util.ThrowableFunction;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableObject;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * @author Ritesh
 */
public class SettingsPage implements Configurable {

    public static final String SETTINGS_DISPLAY_NAME = "Review Board";

    private LoginPanel loginPanel = new LoginPanel();
    private Configuration oldConfigurationState;
    private Project project;

    public SettingsPage(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return SETTINGS_DISPLAY_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        oldConfigurationState = ConfigurationPersistance.getInstance(project).getState();
        if (oldConfigurationState != null) {
            loginPanel.setUrl(oldConfigurationState.url);
            loginPanel.setUsername(oldConfigurationState.username);
            loginPanel.setPassword(oldConfigurationState.password);
            loginPanel.setUseRbTools(oldConfigurationState.useRbTools);
            loginPanel.setRBToolsFilePath(oldConfigurationState.rbToolsPath);
            loginPanel.toggleRBToolsPathVisibility(oldConfigurationState.useRbTools);
        }
        loginPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });
        loginPanel.addRBToolsFilePathListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRBToolsFileChooser();
            }
        });
        loginPanel.addUseRBToolsListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                loginPanel.toggleRBToolsPathVisibility(loginPanel.useRbTools());
            }
        });
        return loginPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        if (oldConfigurationState == null) {
            return !loginPanel.getUrl().isEmpty() || !loginPanel.getUsername().isEmpty() || !loginPanel.getPassword().isEmpty() 
                    || !loginPanel.getRBToolsFilePath().isEmpty();
        }
        return !Comparing.equal(loginPanel.getUrl(), oldConfigurationState.url) ||
                !Comparing.equal(loginPanel.getUsername(), oldConfigurationState.username) ||
                !Comparing.equal(loginPanel.useRbTools(), oldConfigurationState.useRbTools) ||
                !Comparing.equal(loginPanel.getPassword(), oldConfigurationState.password) ||
                !Comparing.equal(loginPanel.getRBToolsFilePath(), oldConfigurationState.rbToolsPath);
    }

    @Override
    public void apply() throws ConfigurationException {
        if(loginPanel.useRbTools() && loginPanel.getRBToolsFilePath().trim().length() <= 0) {
            Messages.showErrorDialog(PluginBundle.message(PluginBundle.RBTOOLS_PATH_ERROR_MESSAGE),
                    PluginBundle.message(PluginBundle.RBTOOLS_PATH_TITLE));
            return;
        }
        
        Configuration configuration = new Configuration(
                loginPanel.getUrl(), loginPanel.getUsername(), loginPanel.getPassword(), loginPanel.useRbTools(), loginPanel.getRBToolsFilePath());
        ConfigurationPersistance.getInstance(project).loadState(configuration);
        ReviewDataProvider.reset();
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
    
    private void showRBToolsFileChooser() {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                // This will display only the files without "." or with ".cmd"
                return !f.getName().contains(".") || f.getName().endsWith(".cmd");
            }

            @Override
            public String getDescription() {
                return "RBTools command file";
            }
        };
        chooser.setDialogTitle("Select path to RBTools");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        if(loginPanel.getRBToolsFilePath() != null && loginPanel.getRBToolsFilePath().length() > 0) {
            chooser.setSelectedFile(new File(loginPanel.getRBToolsFilePath()));
        }
        chooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY
                        .equals(evt.getPropertyName())) {
                    JFileChooser chooser = (JFileChooser)evt.getSource();
                    File curFile = chooser.getSelectedFile();
                    loginPanel.setRBToolsFilePath(curFile.getAbsolutePath());
                }
            }
        }) ;
        
        chooser.showDialog(loginPanel.getPanel(), "Select");
    }

    private void testConnection() {
        final MutableObject connException = new MutableObject();
        if (StringUtils.isEmpty(loginPanel.getUrl()) || StringUtils.isEmpty(loginPanel.getUsername())
                || StringUtils.isEmpty(loginPanel.getPassword())) {
            Messages.showErrorDialog(project, "Connection information provided is invalid.", "Invalid Settings");
            return;
        }
        TaskUtil.queueTask(project, PluginBundle.message(PluginBundle.CONNECTION_TEST_TITLE), true, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                try {
                    params.setIndeterminate(true);
                    ReviewDataProvider.getInstance(project)
                            .testConnection(loginPanel.getUrl(), loginPanel.getUsername(), loginPanel.getPassword());
                    // The task was not cancelled and is successful
                    connException.setValue(Boolean.TRUE);
                } catch (InvalidConfigurationException a) {
                } catch (final Exception exception) {
                    connException.setValue(exception);
                }
                return null;
            }
        }, null, null);

        if (connException.getValue() == Boolean.TRUE) {
            Messages.showInfoMessage(project, PluginBundle.message(PluginBundle.LOGIN_SUCCESS_MESSAGE),
                    PluginBundle.message(PluginBundle.CONNECTION_STATUS_TITLE));
        } else if (connException.getValue() instanceof Exception) {
            final ExceptionHandler.Message message = ExceptionHandler.getMessage((Exception) connException.getValue());
            Messages.showErrorDialog(message.message,
                    PluginBundle.message(PluginBundle.CONNECTION_STATUS_TITLE));
        }
    }
}
