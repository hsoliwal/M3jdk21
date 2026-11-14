// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/DistinctCommitters.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import java.time.LocalDate;

public class DistinctCommitters extends DataTable<DistinctCommitters.Row> {
    public DistinctCommitters(Recipe recipe) {
        super(recipe, "Repository committers", "The distinct set of committers per repository.");
    }


    public static final class Row {
        @Column(displayName = "Name", description = "The name of the committer.")
        private final String name;
        @Column(displayName = "Email", description = "The email of the committer.")
        private final String email;
        @Column(displayName = "Last commit", description = "The date of this committer\'s last commit.")
        private final LocalDate lastCommit;
        @Column(displayName = "Number of commits", description = "The number of commits made by this committer.")
        private final int commits;

        public Row(final String name, final String email, final LocalDate lastCommit, final int commits) {
            this.name = name;
            this.email = email;
            this.lastCommit = lastCommit;
            this.commits = commits;
        }

        public String getName() {
            return this.name;
        }

        public String getEmail() {
            return this.email;
        }

        public LocalDate getLastCommit() {
            return this.lastCommit;
        }

        public int getCommits() {
            return this.commits;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DistinctCommitters.Row)) return false;
            final DistinctCommitters.Row other = (DistinctCommitters.Row) o;
            if (this.getCommits() != other.getCommits()) return false;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final Object this$email = this.getEmail();
            final Object other$email = other.getEmail();
            if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
            final Object this$lastCommit = this.getLastCommit();
            final Object other$lastCommit = other.getLastCommit();
            if (this$lastCommit == null ? other$lastCommit != null : !this$lastCommit.equals(other$lastCommit)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getCommits();
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final Object $email = this.getEmail();
            result = result * PRIME + ($email == null ? 43 : $email.hashCode());
            final Object $lastCommit = this.getLastCommit();
            result = result * PRIME + ($lastCommit == null ? 43 : $lastCommit.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DistinctCommitters.Row(name=" + this.getName() + ", email=" + this.getEmail() + ", lastCommit=" + this.getLastCommit() + ", commits=" + this.getCommits() + ")";
        }
    }
}
