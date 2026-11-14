// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/RecipeRun.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import java.util.List;
import java.util.Map;
import static java.util.Collections.emptyList;

public final class RecipeRun {
    private final Changeset changeset;
    private final Map<DataTable<?>, List<?>> dataTables;

    @Nullable
    public DataTable<?> getDataTable(String name) {
        for (DataTable<?> dataTable : dataTables.keySet()) {
            if (dataTable.getName().equals(name)) {
                return dataTable;
            }
        }
        return null;
    }

    @Nullable
    public <E> List<E> getDataTableRows(String name) {
        for (Map.Entry<DataTable<?>, List<?>> dataTableAndRows : dataTables.entrySet()) {
            if (dataTableAndRows.getKey().getName().equals(name)) {
                //noinspection unchecked
                return (List<E>) dataTableAndRows.getValue();
            }
        }
        return emptyList();
    }

    public RecipeRun(final Changeset changeset, final Map<DataTable<?>, List<?>> dataTables) {
        this.changeset = changeset;
        this.dataTables = dataTables;
    }

    public Changeset getChangeset() {
        return this.changeset;
    }

    public Map<DataTable<?>, List<?>> getDataTables() {
        return this.dataTables;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RecipeRun)) return false;
        final RecipeRun other = (RecipeRun) o;
        final Object this$changeset = this.getChangeset();
        final Object other$changeset = other.getChangeset();
        if (this$changeset == null ? other$changeset != null : !this$changeset.equals(other$changeset)) return false;
        final Object this$dataTables = this.getDataTables();
        final Object other$dataTables = other.getDataTables();
        if (this$dataTables == null ? other$dataTables != null : !this$dataTables.equals(other$dataTables)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $changeset = this.getChangeset();
        result = result * PRIME + ($changeset == null ? 43 : $changeset.hashCode());
        final Object $dataTables = this.getDataTables();
        result = result * PRIME + ($dataTables == null ? 43 : $dataTables.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RecipeRun(changeset=" + this.getChangeset() + ", dataTables=" + this.getDataTables() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RecipeRun withChangeset(final Changeset changeset) {
        return this.changeset == changeset ? this : new RecipeRun(changeset, this.dataTables);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RecipeRun withDataTables(final Map<DataTable<?>, List<?>> dataTables) {
        return this.dataTables == dataTables ? this : new RecipeRun(this.changeset, dataTables);
    }
}
