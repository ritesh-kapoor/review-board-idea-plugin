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

import com.ritesh.idea.plugin.reviewboard.Review;
import org.apache.commons.lang.StringUtils;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * @author Ritesh
 */
public class ReviewTableModel extends AbstractTableModel {

    public enum Columns {
        SUMMARY(0, "Summary"),
        SUBMITTED_TO(1, "Submitted To"),
        SUBMITTER(2, "Submitter"),
        LAST_MODIFIED(3, "Last Modified");


        private int index;
        private String name;

        Columns(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    private List<Review> reviews;
    private final String[] columnNames =
            {Columns.SUMMARY.getName(), Columns.SUBMITTED_TO.getName(), Columns.SUBMITTER.getName(), Columns.LAST_MODIFIED.getName()};

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public ReviewTableModel(List<Review> reviews) {
        this.reviews = reviews;
    }


    @Override
    public int getRowCount() {
        return reviews.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return reviews.get(rowIndex).summary;
            case 1:
                return StringUtils.join(reviews.get(rowIndex).targetPeople, ',');
            case 2:
                return reviews.get(rowIndex).submitter;
            case 3:
                return reviews.get(rowIndex).lastUpdated;
        }
        return null;
    }
}
