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

package com.ritesh.idea.plugin.reviewboard.model;

import java.util.Date;

/**
 * @author Ritesh
 */
public class RBReviewRequestList extends RBModel {
    public int total_results;
    public ReviewRequest[] review_requests;

    public static class ReviewRequest {
        public String branch;
        public String id;
        public String description;
        public Date last_updated;
        public String status;
        public RBLink[] target_people;
        public RBLink[] target_groups;
        public Link links;
        public String summary;
    }

    public static class Link {
        public RBLink submitter;
        public RBLink repository;
    }
}
