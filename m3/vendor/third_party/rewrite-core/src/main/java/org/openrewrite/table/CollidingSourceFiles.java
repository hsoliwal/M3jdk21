// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/CollidingSourceFiles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class CollidingSourceFiles extends DataTable<CollidingSourceFiles.Row> {
    public CollidingSourceFiles(Recipe recipe) {
        super(recipe, "Colliding source files", "Source files that have the same relative path.");
    }


    public static final class Row {
        @Column(displayName = "Relative path", description = "The relative path of the source file within its repository.")
        private final String relativePath;
        @Column(displayName = "Source file type", description = "The type of the source file.")
        private final String sourceFileType;

        public Row(final String relativePath, final String sourceFileType) {
            this.relativePath = relativePath;
            this.sourceFileType = sourceFileType;
        }

        public String getRelativePath() {
            return this.relativePath;
        }

        public String getSourceFileType() {
            return this.sourceFileType;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof CollidingSourceFiles.Row)) return false;
            final CollidingSourceFiles.Row other = (CollidingSourceFiles.Row) o;
            final Object this$relativePath = this.getRelativePath();
            final Object other$relativePath = other.getRelativePath();
            if (this$relativePath == null ? other$relativePath != null : !this$relativePath.equals(other$relativePath)) return false;
            final Object this$sourceFileType = this.getSourceFileType();
            final Object other$sourceFileType = other.getSourceFileType();
            if (this$sourceFileType == null ? other$sourceFileType != null : !this$sourceFileType.equals(other$sourceFileType)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $relativePath = this.getRelativePath();
            result = result * PRIME + ($relativePath == null ? 43 : $relativePath.hashCode());
            final Object $sourceFileType = this.getSourceFileType();
            result = result * PRIME + ($sourceFileType == null ? 43 : $sourceFileType.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "CollidingSourceFiles.Row(relativePath=" + this.getRelativePath() + ", sourceFileType=" + this.getSourceFileType() + ")";
        }
    }
}
