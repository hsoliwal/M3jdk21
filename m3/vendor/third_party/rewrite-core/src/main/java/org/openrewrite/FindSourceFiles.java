// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FindSourceFiles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.SourcesFiles;
import java.nio.file.Path;

public final class FindSourceFiles extends Recipe {
    private final transient SourcesFiles results = new SourcesFiles(this);
    @Option(displayName = "File pattern", description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.", required = false, example = ".github/workflows/*.yml")
    @Nullable
    private final String filePattern;

    @Override
    public String getDisplayName() {
        return "Find files";
    }

    @Override
    public String getDescription() {
        return "Find files by source path.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Nullable
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    Path sourcePath = sourceFile.getSourcePath();
                    if (StringUtils.isBlank(filePattern) || PathUtils.matchesGlob(sourcePath, normalize(filePattern))) {
                        results.insertRow(ctx, new SourcesFiles.Row(sourcePath.toString(), tree.getClass().getSimpleName()));
                        return SearchResult.found(sourceFile);
                    }
                }
                return tree;
            }
        };
    }

    private static String normalize(String filePattern) {
        if (filePattern.startsWith("./") || filePattern.startsWith(".\\")) {
            return filePattern.substring(2);
        } else if (filePattern.startsWith("/") || filePattern.startsWith("\\")) {
            return filePattern.substring(1);
        }
        return filePattern;
    }

    public FindSourceFiles(@Nullable final String filePattern) {
        this.filePattern = filePattern;
    }

    public SourcesFiles getResults() {
        return this.results;
    }

    @Nullable
    public String getFilePattern() {
        return this.filePattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindSourceFiles(results=" + this.getResults() + ", filePattern=" + this.getFilePattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindSourceFiles)) return false;
        final FindSourceFiles other = (FindSourceFiles) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$filePattern = this.getFilePattern();
        final Object other$filePattern = other.getFilePattern();
        if (this$filePattern == null ? other$filePattern != null : !this$filePattern.equals(other$filePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindSourceFiles;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $filePattern = this.getFilePattern();
        result = result * PRIME + ($filePattern == null ? 43 : $filePattern.hashCode());
        return result;
    }
}
