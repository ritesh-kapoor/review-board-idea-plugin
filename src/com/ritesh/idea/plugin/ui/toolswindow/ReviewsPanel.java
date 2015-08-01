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

package com.ritesh.idea.plugin.ui.toolswindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
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
import com.ritesh.idea.plugin.ui.DoNothingAction;
import com.ritesh.idea.plugin.ui.ErrorManager;
import com.ritesh.idea.plugin.ui.Icons;
import com.ritesh.idea.plugin.ui.diff.CommentsDiffTool;
import com.ritesh.idea.plugin.ui.diff.ReviewDiffRequest;
import com.ritesh.idea.plugin.ui.panels.CreateReviewPanel;
import com.ritesh.idea.plugin.util.Page;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * @author Ritesh
 */
public class ReviewsPanel extends JPanel {

//    reviewListActionGroup.add(new DoNothingAction(TextFieldWithAutoCompletion.create(project, Arrays.asList("asd", "Asd"), true, null)));
//    Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
    // TODO: Check for binary files

    final Logger LOG = Logger.getInstance(ReviewsPanel.class);

    private static final int COUNT = 25;

    private final Project project;
    private Page<Review> reviews;
    private Review selectedReview;
    private List<Review.File> selectedFiles;
    private Map<Review.File, List<Review.File.Comment>> selectedComments = new HashMap<>();
    private int start = 0, count = COUNT;

    private JBTable reviewsTable = new JBTable();
    private ReviewChangesTreeList changesTree;
    private ComboBox statusComboBox = new ComboBox(new String[]{"all", "discarded", "pending", "submitted"});
    private JBLabel page = new JBLabel();
    private JComponent mainReviewToolbar;

    private enum ReviewListFilter {
        ALL, INCOMING, OUTGOING
    }

    private ReviewListFilter reviewListFilter = ReviewListFilter.INCOMING;

    public ReviewsPanel(final Project project) {
        this.project = project;
        initUI();
        refreshReviews();
    }

    private void refreshReviews() {
        GuiUtils.enableChildren(this, false, mainReviewToolbar);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading reviews") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    String username = ReviewDataProvider.getConfiguration(project).username;
                    ReviewsPanel.this.reviews = ReviewDataProvider.getInstance(project).listReviews(
                            reviewListFilter == ReviewListFilter.OUTGOING ? username : null,
                            reviewListFilter == ReviewListFilter.INCOMING ? username : null,
                            (String) statusComboBox.getSelectedItem(), start, count);

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            page.setText(String.valueOf(start / COUNT + 1));
                            reviewsTable.setModel(new ReviewTableModel(reviews.getResult()));
                            reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUMMARY.getIndex()).setPreferredWidth(400);
                            reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTED_TO.getIndex()).setPreferredWidth(50);
                            reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTER.getIndex()).setPreferredWidth(50);
                            reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.LAST_MODIFIED.getIndex()).setPreferredWidth(50);
                            GuiUtils.enableChildren(true, ReviewsPanel.this);
                        }
                    });
                } catch (Exception e) {
                    ErrorManager.showMessage(e);
                }
            }
        });
    }

    private void loadSelectedReview() {
        if (reviewsTable.getSelectedRow() == -1) return;

        ApplicationManager.getApplication().invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        final Review selected = reviews.getResult().get(reviewsTable.getSelectedRow());
                        if (checkNewReview() &&
                                Messages.showOkCancelDialog("Do you want to discard your review?", "Confirmation",
                                        AllIcons.General.BalloonWarning) != Messages.OK) {
                            return;
                        }

                        clearReview();
                        GuiUtils.enableChildren(ReviewsPanel.this, false, mainReviewToolbar);
                        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading review") {
                            public void run(@NotNull final ProgressIndicator progressIndicator) {

                                try {
                                    List<Review.File> files = ReviewDataProvider.getInstance(project).files(selected,
                                            new ReviewDataProvider.Progress() {
                                                @Override
                                                public void progress(String text, float percentage) {
                                                    progressIndicator.setText(text);
                                                    progressIndicator.setFraction(percentage);
                                                }
                                            });

                                    final List<Change> changes = new ArrayList<>();
                                    for (Review.File file : files) {
                                        FilePath srcFilePath = FilePathImpl.createNonLocal(file.srcFileName, false);
                                        FilePath patchFilePath = FilePathImpl.createNonLocal(file.dstFileName, false);
                                        SimpleContentRevision original = new SimpleContentRevision(file.srcFileContents, srcFilePath, file.sourceRevision);
                                        SimpleContentRevision patched = new SimpleContentRevision(file.dstFileContents, patchFilePath, "New Change");
                                        changes.add(new Change(original, patched));
                                    }
                                    progressIndicator.setFraction(1.0);
                                    selectedReview = selected;
                                    selectedFiles = files;

                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        public void run() {
                                            changesTree.setChangesToDisplay(changes);
                                            GuiUtils.enableChildren(true, ReviewsPanel.this);
                                        }
                                    });
                                } catch (Exception e) {
                                    ErrorManager.showMessage(e);
                                }
                            }
                        });
                    }
                }
        );
    }

    private void editReview() {
        if (selectedReview != null) {
            final CreateReviewPanel reviewPanel = new CreateReviewPanel(project, selectedReview.summary,
                    selectedReview.description, join(selectedReview.targetPeople, ","),
                    join(selectedReview.targetGroups, ","), selectedReview.respository);
            if (reviewPanel.showAndGet()) {

                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating Review") {
                    @Override
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        try {
                            ReviewDataProvider.getInstance(project).updateReviewRequest(selectedReview,
                                    reviewPanel.getSummary(), reviewPanel.getDescription(),
                                    reviewPanel.getTargetPeople(), reviewPanel.getTargetGroup());
                            ApplicationManager.getApplication().invokeLater(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshReviews();
                                        }
                                    });
                        } catch (Exception e) {
                            ErrorManager.showMessage(e);
                        }
                    }
                });

            }
        }
    }

    private void createReview() {
        final List<Review.File.Comment> newComments = new ArrayList<>();
        for (List<Review.File.Comment> comments : selectedComments.values()) {
            for (Review.File.Comment comment : comments) {
                if (comment.id == null) newComments.add(comment);
            }
        }
        if (newComments.size() > 0) {
            final String reviewComment = Messages.showInputDialog(project, "Review Comment", "Review Comment", null);
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating Review") {
                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {
                    try {
                        ReviewDataProvider.getInstance(project).createReview(selectedReview, newComments, reviewComment,
                                new ReviewDataProvider.Progress() {
                                    @Override
                                    public void progress(String text, float percentage) {
                                        progressIndicator.setFraction(percentage);
                                        progressIndicator.setText(text);
                                    }
                                });

                        clearReview();
                        loadSelectedReview();
                    } catch (Exception e) {
                        ErrorManager.showMessage(e);
                    }
                }
            });
        }
    }


    private void discardReview() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Discarding Review") {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                try {
                    ReviewDataProvider.getInstance(project).discardedReviewRequest(selectedReview);
                } catch (Exception e) {
                    ErrorManager.showMessage(e);
                }
            }
        });
    }

    private void shipIt() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Ship It") {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                try {
                    ReviewDataProvider.getInstance(project).shipIt(selectedReview);
                } catch (Exception e) {
                    ErrorManager.showMessage(e);
                }
            }
        });
    }


    private void submitReview() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Submitting Review") {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                try {
                    ReviewDataProvider.getInstance(project).submittedReviewRequest(selectedReview);
                } catch (Exception e) {
                    ErrorManager.showMessage(e);
                }
            }
        });
    }

    private void loadNext() {
        if (reviews.getOffset() + COUNT < reviews.getTotal()) {
            start = reviews.getOffset() + COUNT;
            refreshReviews();
        }
    }

    private void loadPrevious() {
        if (reviews.getOffset() > 0) {
            start = reviews.getOffset() - COUNT;
            refreshReviews();
        }
    }

    private void loadFirst() {
        start = 0;
        refreshReviews();
    }

    private void loadLast() {
        start = (reviews.getTotal() / COUNT) * COUNT;
        refreshReviews();
    }

    private boolean checkNewReview() {
        for (List<Review.File.Comment> comments : selectedComments.values()) {
            for (Review.File.Comment comment : comments) {
                if (comment.id == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void clearReview() {
        selectedComments.clear();
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
                if (e.getValueIsAdjusting()) return;
                loadSelectedReview();
            }
        });

        statusComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshReviews();
            }
        });

        changesTree.setDoubleClickHandler(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Comments") {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final Change selectedChange = changesTree.getSelectedChanges().get(0);
                                    DiffRequest request = new ReviewDiffRequest(project, selectedChange);
                                    for (Review.File file : selectedFiles) {
                                        if (file.srcFileName.equals(selectedChange.getBeforeRevision().getFile().getPath())) {
                                            List<Review.File.Comment> comments = selectedComments.get(file);
                                            if (comments == null) {
                                                comments = ReviewDataProvider.getInstance(project).comments(selectedReview, file);
                                            }
                                            CommentsDiffTool commentsDiffTool = new CommentsDiffTool(file, comments);
                                            commentsDiffTool.show(request);
                                            selectedComments.put(file, commentsDiffTool.getComments());
                                            checkNewReview();
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    ErrorManager.showMessage(e);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private JComponent createMainReviewToolbar() {

        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardMainActionsGroup", false);
        actionGroup.add(new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                refreshReviews();
            }
        });
        actionGroup.add(new AnAction("Browse", "Open the selected review in browser", AllIcons.General.Web) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (selectedReview == null)
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewBoardUrl(project));
                else
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewUrl(project, selectedReview));
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
                loadSelectedReview();
            }
        });
        actionGroup.addAction(new AnAction("Publish Review", "Publish comments and changes to server", AllIcons.Actions.Export) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                createReview();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(checkNewReview());
            }
        });
        actionGroup.add(new Separator());
        actionGroup.addAll(changesTree.getTreeActions());
        return ActionManager.getInstance().createActionToolbar("ReviewBoardDiffActionGroup", actionGroup, true)
                .getComponent();
    }

    private JComponent createReviewListToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardListActionGroup", false);

        actionGroup.add(new DoNothingAction(new JBLabel("Status : ")));
        actionGroup.add(new DoNothingAction(statusComboBox));
        actionGroup.add(new Separator());
        actionGroup.add(new ToggleAction("Incoming", "Incoming reviews", AllIcons.Ide.IncomingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return reviewListFilter == ReviewListFilter.INCOMING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                reviewListFilter = ReviewListFilter.INCOMING;
                refreshReviews();
            }
        });
        actionGroup.add(new ToggleAction("Outgoing", "Outgoing reviews", AllIcons.Ide.OutgoingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return reviewListFilter == ReviewListFilter.OUTGOING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                reviewListFilter = ReviewListFilter.OUTGOING;
                refreshReviews();
            }
        });

        actionGroup.add(new ToggleAction("All", "All Reviews", AllIcons.General.ProjectStructure) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return reviewListFilter == ReviewListFilter.ALL;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                reviewListFilter = ReviewListFilter.ALL;
                refreshReviews();
            }
        });

        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("Edit Review", "Edit selected review", AllIcons.Actions.EditSource) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                editReview();
            }
        });

        actionGroup.add(new AnAction("Ship It", "Ship It", AllIcons.Actions.Resume) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                shipIt();
            }
        });
        actionGroup.add(new AnAction("Submit It", "Submit the selected review", AllIcons.Actions.MoveToAnotherChangelist) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                submitReview();
            }
        });

        actionGroup.add(new AnAction("Discard Review", "Discard the selected review", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                discardReview();
            }
        });


        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("First", "First", Icons.First) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadFirst();
            }
        });

        actionGroup.add(new AnAction("Back", "Back", Icons.Back) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadPrevious();
            }
        });

        actionGroup.add(new DoNothingAction(page));
        actionGroup.add(new AnAction("Forward", "Forward", Icons.Forward) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadNext();
            }
        });

        actionGroup.add(new AnAction("Last", "Last", Icons.Last) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadLast();
            }
        });

        return ActionManager.getInstance().createActionToolbar("ReviewBoardActions", actionGroup, true)
                .getComponent();
    }

}

