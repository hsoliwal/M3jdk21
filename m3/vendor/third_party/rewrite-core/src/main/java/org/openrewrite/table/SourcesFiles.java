// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/SourcesFiles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.table;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SourcesFiles extends DataTable<SourcesFiles.Row> {
    public SourcesFiles(Recipe recipe) {
        super(recipe, "Source files that matched", "Source files that matched some criteria.");
    }


    public static final class Row {
        @Column(displayName = "Source path before the run", description = "The source path of the file before the run.")
        private final String sourcePath;
        @Column(displayName = "LST type", description = "The LST model type that the file is parsed as.")
        private final String type;

        public Row(final String sourcePath, final String type) {
            this.sourcePath = sourcePath;
            this.type = type;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SourcesFiles.Row)) return false;
            final SourcesFiles.Row other = (SourcesFiles.Row) o;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$type = this.getType();
            final Object other$type = other.getType();
            if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $type = this.getType();
            result = result * PRIME + ($type == null ? 43 : $type.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SourcesFiles.Row(sourcePath=" + this.getSourcePath() + ", type=" + this.getType() + ")";
        }
    }
}
