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

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ritesh.idea.plugin.diff.IVcsDiffProvider;
import com.ritesh.idea.plugin.diff.VcsDiffProviderFactory;
import com.ritesh.idea.plugin.messages.PluginBundle;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.state.DefaultState;
import com.ritesh.idea.plugin.ui.ExceptionHandler;
import com.ritesh.idea.plugin.ui.TaskUtil;
import com.ritesh.idea.plugin.ui.panels.DraftReviewPanel;
import com.ritesh.idea.plugin.util.ThrowableFunction;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Ritesh
 */
public class ShowReviewBoard extends AnAction {

    @Override
    public void actionPerformed(final AnActionEvent e) {
        try {
            final Project project = e.getProject();
            final IVcsDiffProvider vcsDiffProvider =
                    VcsDiffProviderFactory.getVcsDiffProvider(project, ReviewDataProvider.getConfiguration(project));
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    FileDocumentManager.getInstance().saveAllDocuments();
                }
            });
            if (vcsDiffProvider == null) {
                Notifications.Bus.notify(new Notification("ReviewBoard", PluginBundle.message(PluginBundle.UNSUPPORTED_VCS_TITLE),
                        PluginBundle.message(PluginBundle.UNSUPPORTED_VCS_MESSAGE), NotificationType.WARNING));
                return;
            }
            if (vcsDiffProvider.isFromRevision(project, e) ||
                    Messages.showOkCancelDialog(project, "Upload all local changes?", "Confirmation",
                            AllIcons.General.BalloonWarning) == Messages.OK) {
                TaskUtil.queueTask(project, "Generating diff", false, new ThrowableFunction<ProgressIndicator, Object>() {
                    @Override
                    public Object throwableCall(ProgressIndicator params) throws Exception {
                        final String diffContent;
                        try {
                            diffContent = vcsDiffProvider.generateDiff(project, e);
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (isEmpty(diffContent)) {
                                        Messages.showErrorDialog(project, "Cannot generate diff", "Error");
                                    } else {
                                        showCreateReviewPanel(project, diffContent);
                                    }
                                }
                            });
                        } catch (Exception ex) {
                            ExceptionHandler.handleException(ex);
                        }
                        return null;
                    }
                }, null, null);

            }
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
