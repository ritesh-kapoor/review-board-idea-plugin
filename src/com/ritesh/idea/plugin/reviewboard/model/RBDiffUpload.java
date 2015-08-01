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

import java.util.Map;

/**
 * @author Ritesh
 */
public class RBDiffUpload {
    public String stat;
    public Diff diff;
    public Error err;
    public Map<String, Object> fields;
    public String reason;

    public static class Error {
        public String msg;
        public String code;

        @Override
        public String toString() {
            return "Error{" +
                    "msg='" + msg + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }

    public class Diff {
        public String name;

        @Override
        public String toString() {
            return "Diff{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "RBDiffUpload{" +
                "stat='" + stat + '\'' +
                ", diff=" + diff +
                '}';
    }
}
