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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.ritesh.idea.plugin.git.GitUtil;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.state.DefaultState;
import com.ritesh.idea.plugin.ui.ExceptionHandler;
import com.ritesh.idea.plugin.ui.TaskUtil;
import com.ritesh.idea.plugin.ui.panels.DraftReviewPanel;
import com.ritesh.idea.plugin.util.ThrowableFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ritesh
 */
public class ShowReviewBoard extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            final Project project = e.getProject();

            String diffContent;
            VcsRevisionNumber[] data = e.getData(VcsDataKeys.VCS_REVISION_NUMBERS);
            if (data != null) {
                diffContent = GitUtil.generateDiffFromRevision(project, project.getBaseDir(), data[0], data[data.length - 1]);
            } else {
                final Change[] changes = e.getData(VcsDataKeys.CHANGES);
                List<VirtualFile> virtualFiles = new ArrayList<>();
                for (Change change : changes) {
                    virtualFiles.add(change.getVirtualFile());
                }
                diffContent = GitUtil.generateHeadDiff(project, project.getBaseDir(), virtualFiles);
            }
            showCreateReviewPanel(project, diffContent);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    private void showCreateReviewPanel(final Project project, final String diffContent) {
        DefaultState state = ReviewDataProvider.getDefaultState(project);
        final DraftReviewPanel draftReviewPanel =
                new DraftReviewPanel(project, "Create Review Request", null, null, state.targetPeople, state.targetGroup, state.repository);
        if (draftReviewPanel.showAndGet()) {
            ReviewDataProvider.saveDefaultState(project,
                    new DefaultState(draftReviewPanel.getRepository(), draftReviewPanel.getTargetPeople(),
                            draftReviewPanel.getTargetGroup()));

            TaskUtil.queueTask(project, "Uploading Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
                @Override
                public Void throwableCall(ProgressIndicator params) throws Exception {
                    ReviewDataProvider.getInstance(project).
                            createReviewRequest(draftReviewPanel.getSummary(), draftReviewPanel.getDescription(),
                                    draftReviewPanel.getTargetPeople(), draftReviewPanel.getTargetGroup(),
                                    draftReviewPanel.getRepositoryId(), diffContent);
                    return null;
                }
            }, null, null);
        }
    }
}
