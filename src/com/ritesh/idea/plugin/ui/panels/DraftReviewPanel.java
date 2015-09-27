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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.ritesh.idea.plugin.reviewboard.Repository;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.reviewboard.model.RBGroupList;
import com.ritesh.idea.plugin.reviewboard.model.RBUserList;
import com.ritesh.idea.plugin.ui.ExceptionHandler;
import com.ritesh.idea.plugin.ui.TaskUtil;
import com.ritesh.idea.plugin.ui.controls.MultiValueAutoComplete;
import com.ritesh.idea.plugin.util.ThrowableFunction;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ritesh
 */
public class DraftReviewPanel extends DialogWrapper {
    private JPanel panel;
    private EditorTextField descriptionTextBox;
    private EditorTextField summaryTextBox;
    private EditorTextField targetGroupTextBox;
    private EditorTextField targetPeopleTextBox;
    private ComboBox repositoryComboBox;
    private Project project;

    private List<Repository> repositories;

    @SuppressWarnings("unchecked")
    public DraftReviewPanel(final Project project, String dialogTitle,
                            String summary, String description, String targetPeople, String targetGroup,
                            final String repository) {
        super(project);
        this.project = project;

        this.summaryTextBox.setText(summary);
        this.descriptionTextBox.setText(description);
        this.targetPeopleTextBox.setText(targetPeople);
        this.targetGroupTextBox.setText(targetGroup);

        super.init();
        setTitle(dialogTitle);
        setOKActionEnabled(false);

        TaskUtil.queueTask(project, "Loading Repositories", true, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                params.setIndeterminate(true);
                repositories = ReviewDataProvider.getInstance(project).repositories();
                for (Repository r : repositories) {
                    DraftReviewPanel.this.repositoryComboBox.addItem(r.name);
                }
                DraftReviewPanel.this.repositoryComboBox.setSelectedItem(repository);
                DraftReviewPanel.this.setOKActionEnabled(true);
                return null;
            }
        }, null, null);
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    public String getDescription() {
        return descriptionTextBox.getText();
    }

    public String getSummary() {
        return summaryTextBox.getText();
    }

    public String getTargetGroup() {
        return targetGroupTextBox.getText().trim();
    }

    public String getTargetPeople() {
        return clean(targetPeopleTextBox.getText());
    }

    public String getRepository() {
        return (String) repositoryComboBox.getSelectedItem();
    }

    public String getRepositoryId() {
        String repositoryName = getRepository();
        for (Repository repository : repositories) {
            if (repository.name.equals(repositoryName)) {
                return repository.id;
            }
        }
        return null;
    }

    private String clean(String string) {
        String[] split = string.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) {
            String trim = s.trim();
            if (!trim.equals("")) list.add(s);
        }

        return StringUtils.join(list, ",");
    }

    private void createUIComponents() {
        List<EditorCustomization> editorCustomizations =
                Arrays.<EditorCustomization>asList(SoftWrapsEditorCustomization.ENABLED, SpellCheckingEditorCustomization.DISABLED);
        summaryTextBox = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, editorCustomizations);
        descriptionTextBox = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, editorCustomizations);
        targetPeopleTextBox = MultiValueAutoComplete.create(
                project, new MultiValueAutoComplete.DataProvider() {
                    @Override
                    public List<String> getValues(String prefix) {
                        try {
                            RBUserList users = ReviewDataProvider.getInstance(project).users(prefix);
                            List<String> userNames = new ArrayList<>();
                            for (RBUserList.RBUser user : users.users) {
                                userNames.add(user.username);
                            }
                            return userNames;
                        } catch (Exception e) {
                            ExceptionHandler.handleException(e);
                        }
                        return null;
                    }
                }
        );

        targetGroupTextBox = MultiValueAutoComplete.create(
                project, new MultiValueAutoComplete.DataProvider() {
                    @Override
                    public List<String> getValues(String prefix) {
                        try {
                            RBGroupList groups = ReviewDataProvider.getInstance(project).groups(prefix);
                            List<String> userNames = new ArrayList<>();
                            for (RBGroupList.Group user : groups.groups) {
                                userNames.add(user.name);
                            }
                            return userNames;
                        } catch (Exception e) {
                            ExceptionHandler.handleException(e);
                        }
                        return null;
                    }
                }
        );
    }
}
