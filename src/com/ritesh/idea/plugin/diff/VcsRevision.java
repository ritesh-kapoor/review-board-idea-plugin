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

package com.ritesh.idea.plugin.diff;

/**
 * Created by ritesh on 18/12/15.
 */
public class VcsRevision {
    private String fromRevision;
    private String toRevision;

    public VcsRevision(String fromRevision, String toRevision) {
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    public String fromRevision() {
        return fromRevision;
    }

    public String toRevision() {
        return toRevision;
    }
}
