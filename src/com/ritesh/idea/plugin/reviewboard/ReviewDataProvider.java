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

package com.ritesh.idea.plugin.reviewboard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.ritesh.idea.plugin.exception.InvalidConfigurationException;
import com.ritesh.idea.plugin.reviewboard.model.*;
import com.ritesh.idea.plugin.state.Configuration;
import com.ritesh.idea.plugin.state.ConfigurationPersistance;
import com.ritesh.idea.plugin.state.DefaultState;
import com.ritesh.idea.plugin.state.DefaultStatePersistance;
import com.ritesh.idea.plugin.util.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableFloat;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * @author Ritesh
 */
public class ReviewDataProvider {
    private ReviewBoardClient client;
    private static Map<Project, ReviewDataProvider> reviewDataProviderMap = new WeakHashMap<>();

    public static ReviewDataProvider getInstance(Project project) {
        Configuration configuration = getConfiguration(project);

        if (!reviewDataProviderMap.containsKey(project)) {
            ReviewBoardClient client = new ReviewBoardClient(configuration.url
                    , configuration.username, configuration.password);
            reviewDataProviderMap.put(project, new ReviewDataProvider(client));
        }
        return reviewDataProviderMap.get(project);
    }

    public static void reset(){
        reviewDataProviderMap.clear();
    }

    public static Configuration getConfiguration(final Project project) {
        Configuration state = ConfigurationPersistance.getInstance(project).getState();
        if (state == null || StringUtils.isEmpty(state.url) || StringUtils.isEmpty(state.username) || StringUtils.isEmpty(state.password)) {
            throw new InvalidConfigurationException("Review board not configured properly");
        }
        return state;
    }

    public static DefaultState getDefaultState(Project project) {
        DefaultState state = DefaultStatePersistance.getInstance(project).getState();
        if (state == null) state = new DefaultState();
        return state;
    }

    private Reference<List<Repository>> repositoriesCache;

    private ReviewDataProvider(ReviewBoardClient client) {
        this.client = client;
    }

    public String reviewBoardUrl(Project project) {
        return getConfiguration(project).url;
    }

    public String reviewUrl(Project project, Review review) {
        return reviewBoardUrl(project) + "/r/" + review.id + "/";
    }

    public RBGroupList groups(String q) throws URISyntaxException, IOException {
        return client.groupsApi(q, 10);
    }

    public RBUserList users(String q) throws URISyntaxException, IOException {
        return client.usersApi(q);
    }

    public void updateReviewRequest(Review reviewRequest, String summary, String description, String targetPeople,
                                    String targetGroup) throws Exception {
        client.updateReviewApi(reviewRequest.id, description, summary, targetGroup, targetPeople, true);
    }


    public void createReviewRequest(String summary, String description, String targetPeople, String targetGroup,
                                    String repositoryId, String diffContent) throws Exception {
        RBCreateReview reviewRequestApi = client.createReviewRequestApi(repositoryId);
        String reviewRequestId = String.valueOf(reviewRequestApi.review_request.id);
        client.draftDiffUploadApi(reviewRequestId, diffContent, "/");
        client.updateReviewApi(reviewRequestId, description, summary, targetGroup, targetPeople, true);
    }

    public void discardedReviewRequest(Review reviewRequest) throws Exception {
        client.updateReviewRequestStatus(reviewRequest.id, "discarded");
    }

    public void submittedReviewRequest(Review reviewRequest) throws Exception {
        client.updateReviewRequestStatus(reviewRequest.id, "submitted");
    }

    public static void saveDefaultState(Project project, DefaultState defaultState) {
        DefaultStatePersistance.getInstance(project).loadState(defaultState);
    }

    public void testConnection(String url, String username, String password) throws Exception {
        client.testConnection(url, username, password);
    }

    public interface Progress {
        void progress(String text, float percentage);
    }

    public Page<Review> listReviews(String fromUser, String toUser, String status, String repositoryId, int start, int count) throws Exception {
        List<Review> reviews = new ArrayList<>();
        RBReviewRequestList reviewRequestList = client.reviewRequestListApi(fromUser, toUser, status, repositoryId, start, count);
        for (RBReviewRequestList.ReviewRequest request : reviewRequestList.review_requests) {
            String[] targetPeople = new String[request.target_people.length];
            for (int i = 0; i < targetPeople.length; i++) targetPeople[i] = request.target_people[i].title;

            String[] targetGroups = new String[request.target_groups.length];
            for (int i = 0; i < targetGroups.length; i++) targetGroups[i] = request.target_groups[i].title;

            Review.Builder reviewBuilder = new Review.Builder()
                    .id(request.id)
                    .summary(request.summary)
                    .description(request.description)
                    .branch(request.branch)
                    .lastUpdated(request.last_updated)
                    .status(request.status)
                    .targetPeople(targetPeople);
            if (request.links != null && request.links.submitter != null)
                reviewBuilder.submitter(request.links.submitter.title);
            if (request.links != null && request.links.repository != null)
                reviewBuilder.respository(request.links.repository.title);

            Review review = reviewBuilder.targetGroups(targetGroups)
                    .build();
            reviews.add(review);
        }
        return new Page<>(reviews, start, count, reviewRequestList.total_results);
    }

    public void shipIt(final Review reviewRequest) throws Exception {
        final RBReview review = client.createReviewApi(reviewRequest.id, true);
        client.updateReviewApi(reviewRequest.id, String.valueOf(review.review.id), true, null, null);
    }

    public void createReview(final Review reviewRequest, final List<Review.File.Comment> comments, String reviewComment,
                             final Progress progress) throws Exception {
        final RBReview review = client.createReviewApi(reviewRequest.id, null);
        final MutableFloat progressF = new MutableFloat(0f);

        for (final Review.File.Comment comment : comments) {
            progress.progress("Updating comment", progressF.floatValue());
            client.createDiffComment(reviewRequest.id, String.valueOf(review.review.id),
                    comment.file.fileId, comment.firstLine, comment.numberOfLines, comment.text, comment.issueOpened);
            progressF.setValue(progressF.floatValue() + 1.0f / (comments.size() - 1));
        }

        progress.progress("Making review public", progressF.floatValue());
        client.updateReviewApi(reviewRequest.id, String.valueOf(review.review.id), true, reviewComment, null);
        progress.progress("Review Completed", 1);
    }


    public List<Repository> repositories() throws Exception {
        if (repositoriesCache == null || repositoriesCache.get() == null) {
            final RBRepository repositories = client.repositories(200);
            List<Repository> result = new ArrayList<>();
            for (RBRepository.Repository repository : repositories.repositories) {
                result.add(new Repository(repository.id, repository.name));
            }
            repositoriesCache = new SoftReference<>(result);
            return result;
        }
        return repositoriesCache.get();
    }


    public List<Review.File> files(final Review review, final Progress progress) throws Exception {
        List<Review.File> result = new ArrayList<>();
        final List<Future> futures = new CopyOnWriteArrayList<>();
        final MutableFloat progressF = new MutableFloat(0f);
        final RBDiffList diffList = client.diffListApi(review.id);

        if (diffList.total_results > 0) {
            final String revision = String.valueOf(diffList.diffs[0].revision);
            RBFileDiff fileDiff = client.fileDiffApi(review.id, revision);

            for (final RBFileDiff.File file : fileDiff.files) {
                final Review.File diffFile = new Review.File();

                diffFile.fileId = file.id;
                diffFile.srcFileName = file.source_file;
                diffFile.dstFileName = file.dest_file;
                diffFile.sourceRevision = file.source_revision;
                diffFile.revision = revision;

                futures.add(ApplicationManager.getApplication().executeOnPooledThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                progress.progress("Loading file contents "
                                        + Paths.get(diffFile.srcFileName).getFileName(), progressF.floatValue());
                                diffFile.srcFileContents = client.contents(file.links.original_file.href);
                                progressF.setValue(progressF.floatValue() + 1.0f / diffList.total_results);
                                progress.progress("Completed loading contents", progressF.floatValue());
                            }
                        }
                ));

                futures.add(ApplicationManager.getApplication().executeOnPooledThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                progress.progress("Loading file contents "
                                        + Paths.get(diffFile.dstFileName).getFileName(), progressF.floatValue());
                                diffFile.dstFileContents = client.contents(file.links.patched_file.href);
                                progressF.setValue(progressF.floatValue() + 1.0f / diffList.total_results);
                                progress.progress("Completed loading contents", progressF.floatValue());
                            }
                        }
                ));
                result.add(diffFile);
            }
        }
        for (Future future : futures) future.get();
        return result;
    }

    public List<Review.File.Comment> comments(Review review, Review.File file) throws Exception {
        RBComments comments = client.diffCommentListApi(review.id, file.revision, file.fileId);
        List<Review.File.Comment> result = new ArrayList<>();
        for (RBComments.DiffComment diff_comment : comments.diff_comments) {
            Review.File.Comment comment = new Review.File.Comment();
            comment.id = diff_comment.id;
            comment.text = diff_comment.text;
            comment.issueOpened = diff_comment.issue_status;
            comment.firstLine = diff_comment.first_line;
            comment.numberOfLines = diff_comment.num_lines;
            comment.timestamp = diff_comment.timestamp;
            comment.user = diff_comment.links.user.title;
            comment.file = file;
            result.add(comment);
        }

        return result;
    }

}
