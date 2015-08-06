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
import com.ritesh.idea.plugin.ui.ErrorManager;
import com.ritesh.idea.plugin.ui.toolswindow.MultiValueAutoComplete;
import com.ritesh.idea.plugin.util.ui.AutoCompletion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ritesh
 */
public class CreateReviewPanel extends DialogWrapper {
    private JPanel panel;
    private EditorTextField description;
    private EditorTextField summary;
    private EditorTextField targetGroup;
    private EditorTextField targetPeople;
    private ComboBox repository;
    private static final List<Repository> repositories = new CopyOnWriteArrayList<>();
    private Project project;


    private static void loadRepositories(final Project project, final CreateReviewPanel createReviewPanel) {
        createReviewPanel.setOKActionEnabled(false);
        try {
            repositories.clear();
            repositories.addAll(ReviewDataProvider.getInstance(project).repositories());
            for (Repository r : repositories) {
                createReviewPanel.repository.addItem(r.name);
            }
            createReviewPanel.setOKActionEnabled(true);
        } catch (Exception e) {
            ErrorManager.showMessage(e);
        }
    }

    @SuppressWarnings("unchecked")
    public CreateReviewPanel(Project project, String summary, String description, String targetPeople, String targetGroup, String selectedRepository) {
        super(project);
        this.project = project;

        if (repositories.isEmpty()) {
            loadRepositories(project, this);
        }

        this.summary.setText(summary);
        this.description.setText(description);
        this.targetPeople.setText(targetPeople);
        this.targetGroup.setText(targetGroup);
        AutoCompletion.enable(this.repository);
        for (Repository r : repositories) {
            this.repository.addItem(r.name);
        }
        this.repository.setSelectedItem(selectedRepository);
        AutoCompletion.enable(this.repository);

        super.init();
        setTitle("New Review");
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    public String getDescription() {
        return description.getText();
    }

    public String getSummary() {
        return summary.getText();
    }

    public String getTargetGroup() {
        return targetGroup.getText().trim();
    }

    public String getTargetPeople() {
        return clean(targetPeople.getText());
    }

    public String getRepository() {
        return (String) repository.getSelectedItem();
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

    public String clean(String string) {
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
        summary = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, editorCustomizations);
        description = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, editorCustomizations);
        targetPeople = MultiValueAutoComplete.create(
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
                            ErrorManager.showMessage(e);
                        }
                        return null;
                    }
                }
        );

        targetGroup = MultiValueAutoComplete.create(
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
                            ErrorManager.showMessage(e);
                        }
                        return null;
                    }
                }
        );
    }
}
