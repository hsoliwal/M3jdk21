// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/SourcesFileErrors.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.table;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SourcesFileErrors extends DataTable<SourcesFileErrors.Row> {
    public SourcesFileErrors(Recipe recipe) {
        super(recipe, "Source files that errored on a recipe", "The details of all errors produced by a recipe run.");
    }


    public static final class Row {
        @Column(displayName = "Source path", description = "The file that failed to parse.")
        private final String sourcePath;
        @Column(displayName = "Recipe that made changes", description = "The specific recipe that made a change.")
        private final String recipe;
        @Column(displayName = "Stack trace", description = "The stack trace of the failure.")
        private final String stackTrace;

        public Row(final String sourcePath, final String recipe, final String stackTrace) {
            this.sourcePath = sourcePath;
            this.recipe = recipe;
            this.stackTrace = stackTrace;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        public String getRecipe() {
            return this.recipe;
        }

        public String getStackTrace() {
            return this.stackTrace;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SourcesFileErrors.Row)) return false;
            final SourcesFileErrors.Row other = (SourcesFileErrors.Row) o;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$recipe = this.getRecipe();
            final Object other$recipe = other.getRecipe();
            if (this$recipe == null ? other$recipe != null : !this$recipe.equals(other$recipe)) return false;
            final Object this$stackTrace = this.getStackTrace();
            final Object other$stackTrace = other.getStackTrace();
            if (this$stackTrace == null ? other$stackTrace != null : !this$stackTrace.equals(other$stackTrace)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $recipe = this.getRecipe();
            result = result * PRIME + ($recipe == null ? 43 : $recipe.hashCode());
            final Object $stackTrace = this.getStackTrace();
            result = result * PRIME + ($stackTrace == null ? 43 : $stackTrace.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SourcesFileErrors.Row(sourcePath=" + this.getSourcePath() + ", recipe=" + this.getRecipe() + ", stackTrace=" + this.getStackTrace() + ")";
        }
    }
}
