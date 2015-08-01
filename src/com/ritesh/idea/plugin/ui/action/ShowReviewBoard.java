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

package com.ritesh.idea.plugin.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.ritesh.idea.plugin.git.GitUtil;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.state.DefaultState;
import com.ritesh.idea.plugin.ui.ErrorManager;
import com.ritesh.idea.plugin.ui.panels.CreateReviewPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author Ritesh
 */
public class ShowReviewBoard extends AnAction {
    final Logger LOG = Logger.getInstance(ShowReviewBoard.class);


    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            final Project project = e.getProject();

            String diffContent;
            VcsRevisionNumber[] data = e.getData(VcsDataKeys.VCS_REVISION_NUMBERS);
            if (data != null) {
                diffContent = GitUtil.generateDiffFromRevision(project, project.getBaseDir(), data[0], data[data.length - 1]);
            } else {
                // final Change[] changes = e.getData(VcsDataKeys.CHANGES);
                diffContent = GitUtil.generateHeadDiff(project, project.getBaseDir());
            }
            showCreateReviewPanel(project, diffContent);
        } catch (Exception ex) {
            ErrorManager.showMessage(ex);
        }
    }

    private void showCreateReviewPanel(final Project project, final String diffContent) {
        DefaultState state = ReviewDataProvider.getDefaultState(project);
        final CreateReviewPanel createReviewPanel =
                new CreateReviewPanel(project, null, null, state.targetPeople, state.targetGroup, state.repository);
        if (createReviewPanel.showAndGet()) {
            ReviewDataProvider.saveDefaultState(project,
                    new DefaultState(createReviewPanel.getRepository(), createReviewPanel.getTargetPeople(),
                            createReviewPanel.getTargetGroup()));

            createReviewRequest(project, diffContent, createReviewPanel);
        }
    }

    private void createReviewRequest(final Project project, final String diffContent, final CreateReviewPanel createReviewPanel) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading Review") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    ReviewDataProvider.getInstance(project).
                            createReviewRequest(createReviewPanel.getSummary(), createReviewPanel.getDescription(),
                                    createReviewPanel.getTargetPeople(), createReviewPanel.getTargetGroup(),
                                    createReviewPanel.getRepositoryId(), diffContent);
                } catch (Exception ex) {
                    ErrorManager.showMessage(ex);
                }
            }
        });
    }
}
