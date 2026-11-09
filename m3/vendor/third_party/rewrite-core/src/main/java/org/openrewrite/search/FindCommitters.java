// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/search/FindCommitters.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.search;

import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.table.CommitsByDay;
import org.openrewrite.table.DistinctCommitters;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FindCommitters extends Recipe {
    private final transient AtomicBoolean process = new AtomicBoolean(true);
    private final transient DistinctCommitters committers = new DistinctCommitters(this);
    private final transient CommitsByDay commitsByDay = new CommitsByDay(this);
    @Option(displayName = "From date", required = false, description = "Optional. Take into account only commits since this date (inclusive). Default will be the entire history.", example = "2023-01-01")
    @Nullable
    private final String fromDate;

    @Override
    public String getDisplayName() {
        return "Find committers on repositories";
    }

    @Override
    public String getDescription() {
        return "List the committers on a repository.";
    }

    @Override
    public int maxCycles() {
        // This recipe does not modify the LSTs and only requires the `GitProvenance` marker
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(process.get(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            @Nullable
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(GitProvenance.class).ifPresent(provenance -> {
                        process.set(false);
                        if (provenance.getCommitters() != null) {
                            LocalDate from = StringUtils.isBlank(fromDate) ? null : LocalDate.parse(fromDate).minusDays(1);
                            for (GitProvenance.Committer committer : provenance.getCommitters()) {
                                if (from == null || committer.getCommitsByDay().keySet().stream().anyMatch(day -> day.isAfter(from))) {
                                    committers.insertRow(ctx, new DistinctCommitters.Row(committer.getName(), committer.getEmail(), committer.getCommitsByDay().lastKey(), committer.getCommitsByDay().values().stream().mapToInt(Integer::intValue).sum()));
                                    committer.getCommitsByDay().forEach((day, commits) -> commitsByDay.insertRow(ctx, new CommitsByDay.Row(committer.getName(), committer.getEmail(), day, commits)));
                                }
                            }
                        }
                    });
                }
                return tree;
            }
        });
    }

    public FindCommitters(@Nullable final String fromDate) {
        this.fromDate = fromDate;
    }

    public AtomicBoolean getProcess() {
        return this.process;
    }

    public DistinctCommitters getCommitters() {
        return this.committers;
    }

    public CommitsByDay getCommitsByDay() {
        return this.commitsByDay;
    }

    @Nullable
    public String getFromDate() {
        return this.fromDate;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindCommitters(process=" + this.getProcess() + ", committers=" + this.getCommitters() + ", commitsByDay=" + this.getCommitsByDay() + ", fromDate=" + this.getFromDate() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindCommitters)) return false;
        final FindCommitters other = (FindCommitters) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$fromDate = this.getFromDate();
        final Object other$fromDate = other.getFromDate();
        if (this$fromDate == null ? other$fromDate != null : !this$fromDate.equals(other$fromDate)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindCommitters;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fromDate = this.getFromDate();
        result = result * PRIME + ($fromDate == null ? 43 : $fromDate.hashCode());
        return result;
    }
}
