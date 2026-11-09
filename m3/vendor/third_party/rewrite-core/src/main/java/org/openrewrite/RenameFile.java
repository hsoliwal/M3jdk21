// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/RenameFile.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class RenameFile extends Recipe {
    @Option(displayName = "File matcher", description = "Matching files will be renamed. This is a glob expression.", example = "**/application-*.yml")
    private final String fileMatcher;
    @Option(displayName = "The renamed file name", description = "Just the file name without the folder path that precedes it.", example = "application.yml")
    private final String fileName;

    @Override
    public String getDisplayName() {
        return "Rename a file";
    }

    @Override
    public String getDescription() {
        return "Rename a file while keeping it in the same directory.";
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
                    PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher);
                    if (pathMatcher.matches(sourcePath)) {
                        return ((SourceFile) tree).withSourcePath(sourcePath.resolveSibling(fileName).normalize());
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }

    public RenameFile(final String fileMatcher, final String fileName) {
        this.fileMatcher = fileMatcher;
        this.fileName = fileName;
    }

    public String getFileMatcher() {
        return this.fileMatcher;
    }

    public String getFileName() {
        return this.fileName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RenameFile(fileMatcher=" + this.getFileMatcher() + ", fileName=" + this.getFileName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RenameFile)) return false;
        final RenameFile other = (RenameFile) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$fileMatcher = this.getFileMatcher();
        final Object other$fileMatcher = other.getFileMatcher();
        if (this$fileMatcher == null ? other$fileMatcher != null : !this$fileMatcher.equals(other$fileMatcher)) return false;
        final Object this$fileName = this.getFileName();
        final Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RenameFile;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fileMatcher = this.getFileMatcher();
        result = result * PRIME + ($fileMatcher == null ? 43 : $fileMatcher.hashCode());
        final Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        return result;
    }
}
