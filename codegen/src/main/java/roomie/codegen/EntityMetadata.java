/*
 * Copyright 2018 Anton Novikau
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

package roomie.codegen;


import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class EntityMetadata {
    private final String tableName;

    private VariableElement primaryKey;

    private List<VariableElement> columns = new ArrayList<>();

    public EntityMetadata(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public VariableElement getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(VariableElement primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void addColumn(VariableElement column) {
        this.columns.add(column);
    }

    public List<VariableElement> getColumns() {
        return columns;
    }

    public List<VariableElement> getAllColumns() {
        List<VariableElement> allColumns = new ArrayList<>();
        if (primaryKey != null) {
            allColumns.add(primaryKey);
        }
        allColumns.addAll(columns);

        return allColumns;
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(tableName) && primaryKey != null;
    }
}
