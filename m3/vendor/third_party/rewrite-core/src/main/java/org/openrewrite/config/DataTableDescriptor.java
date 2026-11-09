// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/DataTableDescriptor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.config;

import java.util.List;

public final class DataTableDescriptor {
    private final String name;
    private final String displayName;
    private final String description;
    private final List<ColumnDescriptor> columns;

    public DataTableDescriptor(final String name, final String displayName, final String description, final List<ColumnDescriptor> columns) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.columns = columns;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public List<ColumnDescriptor> getColumns() {
        return this.columns;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DataTableDescriptor(name=" + this.getName() + ", displayName=" + this.getDisplayName() + ", description=" + this.getDescription() + ", columns=" + this.getColumns() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DataTableDescriptor)) return false;
        final DataTableDescriptor other = (DataTableDescriptor) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$columns = this.getColumns();
        final Object other$columns = other.getColumns();
        if (this$columns == null ? other$columns != null : !this$columns.equals(other$columns)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $columns = this.getColumns();
        result = result * PRIME + ($columns == null ? 43 : $columns.hashCode());
        return result;
    }
}
