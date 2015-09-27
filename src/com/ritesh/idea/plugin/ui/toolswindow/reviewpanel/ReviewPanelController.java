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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ritesh.idea.plugin.reviewboard.Repository;
import com.ritesh.idea.plugin.reviewboard.Review;
import com.ritesh.idea.plugin.reviewboard.ReviewDataProvider;
import com.ritesh.idea.plugin.ui.TaskUtil;
import com.ritesh.idea.plugin.util.Page;
import com.ritesh.idea.plugin.util.ThrowableFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewPanelController {
    private Project project;
    private ReviewsPanel view;

    public enum ReviewListFilter {
        ALL, INCOMING, OUTGOING
    }

    private ReviewListFilter reviewListFilter = ReviewListFilter.INCOMING;
    private String status;
    private String repositoryId;
    private List<Repository> repositories;
    private Page<Review> reviews;
    private Review selectedReview;
    private static final int COUNT = 25;
    private int start = 0, count = COUNT;

    //Map of fileId and new comments list
    private Map<String, List<Review.File.Comment>> newComments = new HashMap<>();
    private List<Review.File> selectedFiles;

    public ReviewPanelController(Project project, ReviewsPanel view) {
        this.project = project;
        this.view = view;
    }

    public void refreshReviews() {
        view.enablePanel(false);
        TaskUtil.queueTask(project, "Loading review", false, new ThrowableFunction<ProgressIndicator, Page<Review>>() {
            @Override
            public Page<Review> throwableCall(final ProgressIndicator progressIndicator) throws Exception {

                final String username = ReviewDataProvider.getConfiguration(project).username;
                final String fromUser = reviewListFilter == ReviewListFilter.OUTGOING ? username : null;
                final String toUser = reviewListFilter == ReviewListFilter.INCOMING ? username : null;

                reviews = ReviewDataProvider.getInstance(project).listReviews(fromUser, toUser, status, repositoryId
                        , start, count);
                view.setReviewsList(start / COUNT + 1, reviews.getResult());
                view.enablePanel(true);
                return reviews;
            }
        }, null, null);
    }


    public void loadRepositories(final String defaultRepository) {
        TaskUtil.queueTask(project, "Loading Repositories", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                repositories = ReviewDataProvider.getInstance(project).repositories();
                List<String> repositoryNames = new ArrayList<>();
                for (Repository repository : repositories) {
                    repositoryNames.add(repository.name);
                }
                view.updateRepositories(repositoryNames, defaultRepository);
                return null;
            }
        }, null, null);

    }


    public void discardReview(final Review review) {
        TaskUtil.queueTask(project, "Discarding Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).discardedReviewRequest(review);
                return null;
            }
        }, null, null);
    }


    public void submitReview(final Review review) {
        TaskUtil.queueTask(project, "Submitting Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).submittedReviewRequest(review);
                return null;
            }
        }, null, null);
    }

    public void shipReview(final Review review) {
        TaskUtil.queueTask(project, "Ship It", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).shipIt(review);
                return null;
            }
        }, null, null);
    }


    public void loadComments(final Review.File file) {
        TaskUtil.queueTask(project, "Loading Comments", false, new ThrowableFunction<ProgressIndicator, List<Review.File.Comment>>() {
            @Override
            public List<Review.File.Comment> throwableCall(ProgressIndicator params) throws Exception {
                List<Review.File.Comment> comments = ReviewDataProvider.getInstance(project).comments(selectedReview, file);

                List<Review.File.Comment> commentsForFile = newComments.get(file.fileId);
                if (commentsForFile != null) comments.addAll(commentsForFile);

                view.showCommentsDiff(file, comments);
                return comments;
            }
        }, null, null);
    }


    public void updateNewComments(Review.File file, List<Review.File.Comment> comments) {
        if (comments.isEmpty()) {
            newComments.remove(file.fileId);
        } else {
            newComments.put(file.fileId, comments);
        }
    }

    public void publishReview(final String reviewComment) {
        view.enablePanel(false);
        final List<Review.File.Comment> comments = new ArrayList<>();
        for (List<Review.File.Comment> values : newComments.values()) {
            comments.addAll(values);
        }
        TaskUtil.queueTask(project, "Publishing Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                ReviewDataProvider.getInstance(project).createReview(selectedReview, comments, reviewComment,
                        new ReviewDataProvider.Progress() {
                            @Override
                            public void progress(String text, float percentage) {
                                progressIndicator.setFraction(percentage);
                                progressIndicator.setText(text);
                            }
                        });
                clearNewComments();
                return null;
            }
        }, new ThrowableFunction<Void, Void>() {
            @Override
            public Void throwableCall(Void params) throws Exception {
                loadSelectedReview();
                view.enablePanel(true);
                return null;
            }
        }, null);
    }


    public boolean confirmReviewPublish() {
        return Messages.showOkCancelDialog("Do you want to discard your review?", "Confirmation",
                AllIcons.General.BalloonWarning) != Messages.OK;
    }

    public void loadSelectedReview() {
        if (selectedReview == null) return;

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (commentsAvailableForPublish() && confirmReviewPublish()) {
                    return;
                }

                clearNewComments();
                view.enablePanel(false);

                TaskUtil.queueTask(project, "Loading review", false, new ThrowableFunction<ProgressIndicator, List<Review.File>>() {
                    @Override
                    public List<Review.File> throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                        List<Review.File> files = ReviewDataProvider.getInstance(project).files(selectedReview,
                                new ReviewDataProvider.Progress() {
                                    @Override
                                    public void progress(String text, float percentage) {
                                        progressIndicator.setText(text);
                                        progressIndicator.setFraction(percentage);
                                    }
                                });
                        view.enablePanel(true);
                        selectedFiles = files;
                        view.setCurrentReview(selectedFiles);
                        return files;
                    }
                }, null, null);

            }
        });
    }


    public void updateReviewRequest(final Review selectedReview, final String summary, final String description,
                                    final String targetPeople, final String targetGroup) {
        TaskUtil.queueTask(project, "Updating Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                ReviewDataProvider.getInstance(project).updateReviewRequest(selectedReview, summary, description,
                        targetPeople, targetGroup);
                return null;
            }
        }, null, null);
    }


    public void statusChanged(String status) {
        this.status = status;
        this.start = 0;
        refreshReviews();
    }

    public void repositoryChanged(String repositoryName) {
        this.repositoryId = null;
        for (Repository repository : repositories) {
            if (repository.name == repositoryName) {
                this.repositoryId = repository.id;
                break;
            }

        }
        this.start = 0;
        refreshReviews();
    }

    public void filterChanged(ReviewListFilter reviewListFilter) {
        this.reviewListFilter = reviewListFilter;
        this.start = 0;
        refreshReviews();
    }

    public void selectedReviewChanged(int selectedReviewIndex) {
        selectedReview = reviews.getResult().get(selectedReviewIndex);
        loadSelectedReview();
    }

    public Review selectedReview() {
        return selectedReview;
    }

    public ReviewListFilter reviewListFilter() {
        return reviewListFilter;
    }

    public List<Review.File> selectedFiles() {
        return selectedFiles;
    }

    private void clearNewComments() {
        newComments.clear();
    }

    public boolean commentsAvailableForPublish() {
        return !newComments.isEmpty();
    }

    public void loadNext() {
        if (hasNext()) {
            start = reviews.getOffset() + COUNT;
            refreshReviews();
        }
    }

    public void loadPrevious() {
        if (hasPrevious()) {
            start = reviews.getOffset() - COUNT;
            refreshReviews();
        }
    }

    public void loadFirst() {
        start = 0;
        refreshReviews();
    }

    public void loadLast() {
        start = (reviews.getTotal() / COUNT) * COUNT;
        refreshReviews();
    }

    public boolean hasNext() {
        return reviews != null && reviews.getOffset() + COUNT < reviews.getTotal();
    }

    public boolean hasPrevious() {
        return reviews != null && reviews.getOffset() > 0;
    }

    public boolean isFirstEnabled() {
        return !(start == 0);
    }

    public boolean isLastEnabled() {
        return !(reviews != null && start == ((reviews.getTotal() / COUNT) * COUNT));
    }

}
