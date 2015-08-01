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

import java.util.Date;

/**
 * @author Ritesh
 */
public class Review {
    public String id;
    public String summary;
    public String branch;
    public Date lastUpdated;
    public String status;
    public String[] targetPeople;
    public String submitter;
    public String respository;
    public String[] targetGroups;
    public String description;

    public static class File {
        public String fileId;
        public String srcFileName;
        public String dstFileName;
        public String srcFileContents;
        public String dstFileContents;
        public String sourceRevision;
        public String revision;

        public static class Comment {
            public boolean issueOpened;
            public int numberOfLines;
            public Date timestamp;
            public String id;
            public String issueStatus;
            public String text;
            public int firstLine;
            public String user;
            public File file;
        }
    }

    public static final class Builder {
        private Review review = new Review();

        public Builder() {
        }

        public Builder id(String id) {
            review.id = id;
            return this;
        }

        public Builder summary(String summary) {
            review.summary = summary;
            return this;
        }

        public Builder branch(String branch) {
            review.branch = branch;
            return this;
        }

        public Builder lastUpdated(Date lastUpdated) {
            review.lastUpdated = lastUpdated;
            return this;
        }

        public Builder status(String status) {
            review.status = status;
            return this;
        }

        public Builder targetPeople(String[] targetPeople) {
            review.targetPeople = targetPeople;
            return this;
        }

        public Builder submitter(String submitter) {
            review.submitter = submitter;
            return this;
        }

        public Builder respository(String respository) {
            review.respository = respository;
            return this;
        }

        public Review build() {
            return review;
        }


        public Builder targetGroups(String[] targetGroups) {
            review.targetGroups = targetGroups;
            return this;
        }

        public Builder description(String description) {
            review.description = description;
            return this;
        }
    }
}
