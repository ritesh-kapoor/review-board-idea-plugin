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

package com.ritesh.idea.plugin.state;

/**
 * @author Ritesh
 */
public class DefaultState {

    public String targetPeople;
    public String repository;
    public String targetGroup;

    public DefaultState(String repository, String targetPeople, String targetGroup) {
        this.targetPeople = targetPeople;
        this.repository = repository;
        this.targetGroup = targetGroup;
    }

    public DefaultState() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultState state = (DefaultState) o;

        return !(targetPeople != null ? !targetPeople.equals(state.targetPeople) : state.targetPeople != null)
                && !(repository != null ? !repository.equals(state.repository) : state.repository != null)
                && !(targetGroup != null ? !targetGroup.equals(state.targetGroup) : state.targetGroup != null);

    }

    @Override
    public int hashCode() {
        int result = targetPeople != null ? targetPeople.hashCode() : 0;
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (targetGroup != null ? targetGroup.hashCode() : 0);
        return result;
    }
}
