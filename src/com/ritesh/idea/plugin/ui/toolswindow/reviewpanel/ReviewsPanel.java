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

package com.ritesh.idea.plugin.ui.toolswindow.reviewpanel;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.SimpleContentRevision;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.ritesh.idea.plugin.reviewboard.Review;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.state.SettingsPage;
import com.ritesh.idea.plugin.ui.Icons;
import com.ritesh.idea.plugin.ui.action.CustomComponentActionBase;
import com.ritesh.idea.plugin.ui.diff.CommentsDiffTool;
import com.ritesh.idea.plugin.ui.diff.ReviewDiffRequest;
import com.ritesh.idea.plugin.ui.panels.DraftReviewPanel;
import com.ritesh.idea.plugin.ui.toolswindow.ReviewChangesTreeList;
import com.ritesh.idea.plugin.ui.toolswindow.ReviewTableModel;
import com.ritesh.idea.plugin.ui.toolswindow.reviewpanel.ReviewPanelController.ReviewListFilter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * @author Ritesh
 */
public class ReviewsPanel extends JPanel {

//    reviewListActionGroup.add(new DoNothingAction(TextFieldWithAutoCompletion.create(project, Arrays.asList("asd", "Asd"), true, null)));
//    Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
    // TODO: Check for binary files

    private JBTable reviewsTable = new JBTable();
    private ReviewChangesTreeList changesTree;
    private ComboBox statusComboBox = new ComboBox(new String[]{"all", "discarded", "pending", "submitted"});
    private ComboBox repositoryComboBox = new ComboBox(new String[]{"Select Repository"});
    private JBLabel page = new JBLabel();
    private JComponent mainReviewToolbar;

    final Logger LOG = Logger.getInstance(ReviewsPanel.class);

    private ReviewPanelController controller;
    private final Project project;

    public ReviewsPanel(final Project project) {
        this.project = project;
        this.controller = new ReviewPanelController(project, this);
        initUI();
    }

    public void setReviewsList(final int pageNumber, final List<Review> reviews) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                reviewsTable.setModel(new ReviewTableModel(reviews));
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUMMARY.getIndex()).setPreferredWidth(400);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTED_TO.getIndex()).setPreferredWidth(50);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTER.getIndex()).setPreferredWidth(50);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.LAST_MODIFIED.getIndex()).setPreferredWidth(50);
                page.setText(String.valueOf(pageNumber));
                GuiUtils.enableChildren(true, ReviewsPanel.this);
            }
        });
    }

    public void setCurrentReview(List<Review.File> files) {
        final List<Change> changes = new ArrayList<>();
        for (Review.File file : files) {
            FilePath srcFilePath = FilePathImpl.createNonLocal(file.srcFileName, false);
            FilePath patchFilePath = FilePathImpl.createNonLocal(file.dstFileName, false);
            SimpleContentRevision original = new SimpleContentRevision(file.srcFileContents, srcFilePath, file.sourceRevision);
            SimpleContentRevision patched = new SimpleContentRevision(file.dstFileContents, patchFilePath, "New Change");
            changes.add(new Change(original, patched));
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                changesTree.setChangesToDisplay(changes);
                GuiUtils.enableChildren(true, ReviewsPanel.this);
            }
        });
    }

    public void updateRepositories(final List<String> repositories, final String defaultRepository) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                repositoryComboBox.removeAllItems();
                repositoryComboBox.addItem("Select Repository");
                repositoryComboBox.setSelectedIndex(0);
                for (String repository : repositories) {
                    repositoryComboBox.addItem(repository);
                }
                if (defaultRepository == null) {
                    repositoryComboBox.setSelectedIndex(0);
                } else {
                    repositoryComboBox.setSelectedItem(defaultRepository);
                }
                repositoryComboBox.setEnabled(true);
            }
        });
    }

    //TODO remove join
    private void draftReview() {
        Review selectedReview = controller.selectedReview();
        if (selectedReview != null) {
            final DraftReviewPanel reviewPanel = new DraftReviewPanel(project, "Update Review Request", selectedReview.summary,
                    selectedReview.description, join(selectedReview.targetPeople, ","),
                    join(selectedReview.targetGroups, ","), selectedReview.respository);

            if (reviewPanel.showAndGet()) {
                controller.updateReviewRequest(selectedReview, reviewPanel.getSummary(), reviewPanel.getDescription(),
                        reviewPanel.getTargetPeople(), reviewPanel.getTargetGroup());
            }
        }
    }

    public void enablePanel(final boolean enable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                GuiUtils.enableChildren(ReviewsPanel.this, enable, mainReviewToolbar);
            }
        });
    }

    private void loadFileComments() {
        final Change selectedChange = changesTree.getSelectedChanges().get(0);
        List<Review.File> selectedFiles = controller.selectedFiles();
        for (Review.File file : selectedFiles) {
            if (file.srcFileName.equals(selectedChange.getBeforeRevision().getFile().getPath())) {
                controller.loadComments(file);
                break;
            }
        }
    }

    public void showCommentsDiff(final Review.File file, final List<Review.File.Comment> comments) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final Change selectedChange = changesTree.getSelectedChanges().get(0);
                DiffRequest request = new ReviewDiffRequest(project, selectedChange);
                final CommentsDiffTool commentsDiffTool = new CommentsDiffTool(file, comments);
                commentsDiffTool.setActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        controller.updateNewComments(file, commentsDiffTool.getNewComments());
                    }
                });
                commentsDiffTool.show(request);
            }
        });
    }

    private int getSelectedReviewIndex() {
        return reviewsTable.getSelectedRow();
    }

    @SuppressWarnings("unchecked")
    private void initUI() {
        setLayout(new BorderLayout());

        changesTree = new ReviewChangesTreeList(project, new ArrayList());

        mainReviewToolbar = createMainReviewToolbar();

        JPanel reviewsListPanel = new JPanel(new BorderLayout());
        JPanel toolbarGroup = new JPanel(new BorderLayout());
        toolbarGroup.add(mainReviewToolbar, BorderLayout.WEST);
        toolbarGroup.add(createReviewListToolbar(), BorderLayout.CENTER);

        reviewsListPanel.add(toolbarGroup, BorderLayout.PAGE_START);
        reviewsListPanel.add(new JBScrollPane(reviewsTable), BorderLayout.CENTER);

        JPanel diffPanel = new JPanel(new BorderLayout());
        diffPanel.add(createDiffPanelToolbar(), BorderLayout.PAGE_START);
        diffPanel.add(changesTree, BorderLayout.CENTER);

        Splitter splitter = new Splitter(false, 0.7f);
        splitter.setFirstComponent(reviewsListPanel);
        splitter.setSecondComponent(diffPanel);

        add(splitter);

        reviewsTable.setRowHeight(20);
        reviewsTable.setShowGrid(false);
        reviewsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || getSelectedReviewIndex() < 0) return;
                controller.selectedReviewChanged(getSelectedReviewIndex());
            }
        });

        statusComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.statusChanged((String) statusComboBox.getSelectedItem());
            }
        });
        repositoryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.repositoryChanged((String) repositoryComboBox.getSelectedItem());
            }
        });
        changesTree.setDoubleClickHandler(new Runnable() {
            @Override
            public void run() {
                loadFileComments();
            }
        });
        new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadFileComments();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(CommonShortcuts.getDiff().getShortcuts()), changesTree);

        // Trigger to load reviews
        statusComboBox.setSelectedIndex(0);
        repositoryComboBox.setEnabled(false);
        controller.loadRepositories(null);

    }

    private void refreshPanel() {
        repositoryComboBox.setEnabled(false);
        controller.loadRepositories((String) repositoryComboBox.getSelectedItem());
        controller.refreshReviews();
    }

    private JComponent createMainReviewToolbar() {

        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardMainActionsGroup", false);
        actionGroup.add(new AnAction("Refresh reviews", "Refresh reviews", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                refreshPanel();
            }
        });
        actionGroup.add(new AnAction("Browse", "Open the selected review in browser", AllIcons.General.Web) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (controller.selectedReview() == null)
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewBoardUrl(project));
                else
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewUrl(project, controller.selectedReview()));
            }
        });
        actionGroup.add(new AnAction("Settings", "Open review board settings", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsPage.SETTINGS_DISPLAY_NAME);
            }
        });

        return ActionManager.getInstance().createActionToolbar("ReviewBoardMainActionsGroup", actionGroup, true)
                .getComponent();

    }

    private JComponent createDiffPanelToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardDiffActionsGroup", false);
        actionGroup.add(new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadSelectedReview();
            }
        });
        actionGroup.addAction(new AnAction("Publish Review", "Publish comments and changes to server", AllIcons.Actions.Export) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (controller.commentsAvailableForPublish()) {
                    final String reviewComment = Messages.showInputDialog(project, "Review Comment", "Review Comment", null);
                    if (reviewComment != null) {
                        controller.publishReview(reviewComment);
                    }
                }
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.commentsAvailableForPublish());
            }
        });
        actionGroup.add(new Separator());
        actionGroup.addAll(changesTree.getTreeActions());
        return ActionManager.getInstance().createActionToolbar("ReviewBoardDiffActionGroup", actionGroup, true)
                .getComponent();
    }

    private JComponent createReviewListToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardListActionGroup", false);

        actionGroup.add(new Separator());
        actionGroup.add(new CustomComponentActionBase(new JBLabel("Status : ")));
        actionGroup.add(new CustomComponentActionBase(statusComboBox));
        actionGroup.add(new Separator());
        actionGroup.add(new CustomComponentActionBase(repositoryComboBox));
        actionGroup.add(new Separator());
        actionGroup.add(new ToggleAction("Incoming", "Show incoming reviews", AllIcons.Ide.IncomingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.reviewListFilter() == ReviewListFilter.INCOMING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                controller.filterChanged(ReviewListFilter.INCOMING);
            }
        });
        actionGroup.add(new ToggleAction("Outgoing", "Show outgoing reviews", AllIcons.Ide.OutgoingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.reviewListFilter() == ReviewListFilter.OUTGOING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                controller.filterChanged(ReviewListFilter.OUTGOING);
            }
        });

        actionGroup.add(new ToggleAction("All", "Show all Reviews", AllIcons.General.ProjectStructure) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.reviewListFilter() == ReviewListFilter.ALL;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                controller.filterChanged(ReviewListFilter.ALL);
            }
        });

        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("First", "Go to first page", Icons.First) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadFirst();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.isFirstEnabled());
            }
        });

        actionGroup.add(new AnAction("Back", "Move page backward", Icons.Back) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadPrevious();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.hasPrevious());
            }
        });

        actionGroup.add(new CustomComponentActionBase(page));
        actionGroup.add(new AnAction("Forward", "Move page forward", Icons.Forward) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadNext();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.hasNext());
            }
        });

        actionGroup.add(new AnAction("Last", "Go to last page", Icons.Last) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadLast();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.isLastEnabled());
            }
        });

        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("Edit Review", "Edit selected review", AllIcons.Actions.EditSource) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                draftReview();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.selectedReview() != null);
            }
        });

        actionGroup.add(new AnAction("Ship It", "Ship It", AllIcons.Graph.NodeSelectionMode) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.shipReview(controller.selectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.selectedReview() != null);
            }
        });
        actionGroup.add(new AnAction("Submit Review", "Submit the selected review", AllIcons.Actions.MoveToAnotherChangelist) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.submitReview(controller.selectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.selectedReview() != null);
            }
        });

        actionGroup.add(new AnAction("Discard Review", "Discard the selected review", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.discardReview(controller.selectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.selectedReview() != null);
            }
        });

        return ActionManager.getInstance().createActionToolbar("ReviewBoardActions", actionGroup, true)
                .getComponent();
    }


}

