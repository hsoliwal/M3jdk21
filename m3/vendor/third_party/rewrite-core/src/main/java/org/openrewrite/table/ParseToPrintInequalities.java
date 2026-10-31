// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/ParseToPrintInequalities.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class ParseToPrintInequalities extends DataTable<ParseToPrintInequalities.Row> {
    public ParseToPrintInequalities(Recipe recipe) {
        super(recipe, "Parser to print inequalities", "A list of files that parsers produced `SourceFile` which, when printed, " + "didn\'t match the original source code.");
    }


    public static final class Row {
        @Column(displayName = "Source path", description = "The file that failed to parse.")
        private final String sourcePath;
        @Column(displayName = "Diff", description = "The diff between the original source code and the printed `SourceFile`.")
        @Nullable
        private final String diff;

        public Row(final String sourcePath, @Nullable final String diff) {
            this.sourcePath = sourcePath;
            this.diff = diff;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        @Nullable
        public String getDiff() {
            return this.diff;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ParseToPrintInequalities.Row)) return false;
            final ParseToPrintInequalities.Row other = (ParseToPrintInequalities.Row) o;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$diff = this.getDiff();
            final Object other$diff = other.getDiff();
            if (this$diff == null ? other$diff != null : !this$diff.equals(other$diff)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $diff = this.getDiff();
            result = result * PRIME + ($diff == null ? 43 : $diff.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ParseToPrintInequalities.Row(sourcePath=" + this.getSourcePath() + ", diff=" + this.getDiff() + ")";
        }
    }
}
