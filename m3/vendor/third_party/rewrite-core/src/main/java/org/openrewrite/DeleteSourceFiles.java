// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/DeleteSourceFiles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.internal.lang.Nullable;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public final class DeleteSourceFiles extends Recipe {
    @Option(displayName = "File pattern", description = "A glob expression representing a file path to delete (relative to the project root).", example = ".github/workflows/*.yml")
    private final String filePattern;

    @Override
    public String getDisplayName() {
        return "Delete files";
    }

    @Override
    public String getDescription() {
        return "Delete files by source path.";
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
                    PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + filePattern);
                    if (pathMatcher.matches(sourcePath)) {
                        return null;
                    }
                }
                return tree;
            }
        };
    }

    public DeleteSourceFiles(final String filePattern) {
        this.filePattern = filePattern;
    }

    public String getFilePattern() {
        return this.filePattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeleteSourceFiles(filePattern=" + this.getFilePattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeleteSourceFiles)) return false;
        final DeleteSourceFiles other = (DeleteSourceFiles) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$filePattern = this.getFilePattern();
        final Object other$filePattern = other.getFilePattern();
        if (this$filePattern == null ? other$filePattern != null : !this$filePattern.equals(other$filePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeleteSourceFiles;
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
