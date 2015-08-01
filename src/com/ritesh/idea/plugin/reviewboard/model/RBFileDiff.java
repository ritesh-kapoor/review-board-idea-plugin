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

/**
 * @author Ritesh
 */
public class RBFileDiff extends RBModel {
    public File[] files;
    public int total_results;

    public static class File {
        public String source_file;
        public String dest_file;
        public String id;
        public Links links;
        public String source_revision;

        public static class Links {
            public RBLink diff_comments;
            public RBLink patched_file;
            public RBLink original_file;
        }
    }
}
