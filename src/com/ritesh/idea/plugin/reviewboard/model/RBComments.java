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
public class RBComments extends RBModel {
    public int total_results;
    public DiffComment[] diff_comments;

    public static class DiffComment {
        public boolean issue_opened;
        public int num_lines;
        public Date timestamp;
        public String id;
        public boolean issue_status;
        public String text;
        public int first_line;
        public Links links;

        public static class Links {
            public RBLink user;
        }
    }
}
