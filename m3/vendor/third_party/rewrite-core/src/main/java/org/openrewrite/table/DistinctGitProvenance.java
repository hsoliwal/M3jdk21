// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/DistinctGitProvenance.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.marker.GitProvenance;

@JsonIgnoreType
public class DistinctGitProvenance extends DataTable<DistinctGitProvenance.Row> {
    public DistinctGitProvenance(Recipe recipe) {
        super(recipe, "Distinct Git Provenance", "List out the contents of each unique `GitProvenance` marker in the set of source files. " + "When everything is working correctly, exactly one such marker should be printed as all source files are " + "expected to come from the same repository / branch / commit hash.");
    }


    public static final class Row {
        @Column(displayName = "Origin", description = "The URL of the git remote.")
        private final String origin;
        @Column(displayName = "Branch", description = "The branch that was checked out at the time the source file was parsed.")
        private final String branch;
        @Column(displayName = "Changeset", description = "The commit hash of the changeset that was checked out.")
        private final String changeset;
        @Column(displayName = "AutoCRLF", description = "The value of the `core.autocrlf` git config setting.")
        private final GitProvenance.AutoCRLF autoCRLF;
        @Column(displayName = "EOL", description = "The value of the `core.eol` git config setting.")
        private final GitProvenance.EOL eol;

        public Row(final String origin, final String branch, final String changeset, final GitProvenance.AutoCRLF autoCRLF, final GitProvenance.EOL eol) {
            this.origin = origin;
            this.branch = branch;
            this.changeset = changeset;
            this.autoCRLF = autoCRLF;
            this.eol = eol;
        }

        public String getOrigin() {
            return this.origin;
        }

        public String getBranch() {
            return this.branch;
        }

        public String getChangeset() {
            return this.changeset;
        }

        public GitProvenance.AutoCRLF getAutoCRLF() {
            return this.autoCRLF;
        }

        public GitProvenance.EOL getEol() {
            return this.eol;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DistinctGitProvenance.Row)) return false;
            final DistinctGitProvenance.Row other = (DistinctGitProvenance.Row) o;
            final Object this$origin = this.getOrigin();
            final Object other$origin = other.getOrigin();
            if (this$origin == null ? other$origin != null : !this$origin.equals(other$origin)) return false;
            final Object this$branch = this.getBranch();
            final Object other$branch = other.getBranch();
            if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
            final Object this$changeset = this.getChangeset();
            final Object other$changeset = other.getChangeset();
            if (this$changeset == null ? other$changeset != null : !this$changeset.equals(other$changeset)) return false;
            final Object this$autoCRLF = this.getAutoCRLF();
            final Object other$autoCRLF = other.getAutoCRLF();
            if (this$autoCRLF == null ? other$autoCRLF != null : !this$autoCRLF.equals(other$autoCRLF)) return false;
            final Object this$eol = this.getEol();
            final Object other$eol = other.getEol();
            if (this$eol == null ? other$eol != null : !this$eol.equals(other$eol)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $origin = this.getOrigin();
            result = result * PRIME + ($origin == null ? 43 : $origin.hashCode());
            final Object $branch = this.getBranch();
            result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
            final Object $changeset = this.getChangeset();
            result = result * PRIME + ($changeset == null ? 43 : $changeset.hashCode());
            final Object $autoCRLF = this.getAutoCRLF();
            result = result * PRIME + ($autoCRLF == null ? 43 : $autoCRLF.hashCode());
            final Object $eol = this.getEol();
            result = result * PRIME + ($eol == null ? 43 : $eol.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DistinctGitProvenance.Row(origin=" + this.getOrigin() + ", branch=" + this.getBranch() + ", changeset=" + this.getChangeset() + ", autoCRLF=" + this.getAutoCRLF() + ", eol=" + this.getEol() + ")";
        }
    }
}
