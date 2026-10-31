// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FindCollidingSourceFiles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.CollidingSourceFiles;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FindCollidingSourceFiles extends ScanningRecipe<FindCollidingSourceFiles.Accumulator> {
    private final transient CollidingSourceFiles collidingSourceFiles = new CollidingSourceFiles(this);

    @Override
    public String getDisplayName() {
        return "Find colliding source files";
    }

    @Override
    public String getDescription() {
        return "Finds source files which share a path with another source file. " + "There should always be exactly one source file per path within a repository. " + "This is a diagnostic for finding problems in OpenRewrite parsers/build plugins.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree instanceof SourceFile;
                Path p = ((SourceFile) tree).getSourcePath();
                if (!acc.getSourcePaths().add(p)) {
                    acc.getDuplicates().add(p);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        acc.getSourcePaths().clear(); // we don't need this anymore, might as well free the memory sooner
        return super.generate(acc, ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            @Nullable
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    Path p = ((SourceFile) tree).getSourcePath();
                    if (acc.getDuplicates().contains(p)) {
                        collidingSourceFiles.insertRow(ctx, new CollidingSourceFiles.Row(p.toString(), tree.getClass().toString()));
                        return SearchResult.found(tree, "Duplicate source file " + p);
                    }
                }
                return tree;
            }
        };
    }


    static final class Accumulator {
        private final Set<Path> sourcePaths = new LinkedHashSet<>();
        private final Set<Path> duplicates = new LinkedHashSet<>();

        public Accumulator() {
        }

        public Set<Path> getSourcePaths() {
            return this.sourcePaths;
        }

        public Set<Path> getDuplicates() {
            return this.duplicates;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof FindCollidingSourceFiles.Accumulator)) return false;
            final FindCollidingSourceFiles.Accumulator other = (FindCollidingSourceFiles.Accumulator) o;
            final Object this$sourcePaths = this.getSourcePaths();
            final Object other$sourcePaths = other.getSourcePaths();
            if (this$sourcePaths == null ? other$sourcePaths != null : !this$sourcePaths.equals(other$sourcePaths)) return false;
            final Object this$duplicates = this.getDuplicates();
            final Object other$duplicates = other.getDuplicates();
            if (this$duplicates == null ? other$duplicates != null : !this$duplicates.equals(other$duplicates)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourcePaths = this.getSourcePaths();
            result = result * PRIME + ($sourcePaths == null ? 43 : $sourcePaths.hashCode());
            final Object $duplicates = this.getDuplicates();
            result = result * PRIME + ($duplicates == null ? 43 : $duplicates.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "FindCollidingSourceFiles.Accumulator(sourcePaths=" + this.getSourcePaths() + ", duplicates=" + this.getDuplicates() + ")";
        }
    }

    public FindCollidingSourceFiles() {
    }

    public CollidingSourceFiles getCollidingSourceFiles() {
        return this.collidingSourceFiles;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindCollidingSourceFiles(collidingSourceFiles=" + this.getCollidingSourceFiles() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindCollidingSourceFiles)) return false;
        final FindCollidingSourceFiles other = (FindCollidingSourceFiles) o;
        if (!other.canEqual((Object) this)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindCollidingSourceFiles;
    }

    @Override
    public int hashCode() {
        final int result = 1;
        return result;
    }
}
