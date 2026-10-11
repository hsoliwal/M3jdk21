// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/SetFilePermissions.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public final class SetFilePermissions extends Recipe {
    @Option(displayName = "File matcher", description = "Permissions will be applied to matching files. This is a glob expression.", example = "**/gradlew.bat")
    private final String fileMatcher;
    @Option(displayName = "Readable", description = "File read permission.")
    private final Boolean isReadable;
    @Option(displayName = "Writable", description = "File write permission.")
    private final Boolean isWritable;
    @Option(displayName = "Executable", description = "Files executable permission.")
    private final Boolean isExecutable;

    @Override
    public String getDisplayName() {
        return "Set file permission attributes";
    }

    @Override
    public String getDescription() {
        return "Set a file\'s read, write and executable permission attributes.";
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
                        if (sourceFile.getFileAttributes() == null) {
                            sourceFile = sourceFile.withFileAttributes(new FileAttributes(null, null, null, isReadable, isWritable, isExecutable, 0));
                        } else {
                            FileAttributes fileAttributes = sourceFile.getFileAttributes();
                            sourceFile = sourceFile.withFileAttributes(fileAttributes.withReadable(isReadable).withWritable(isWritable).withExecutable(isExecutable));
                        }
                        return sourceFile;
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }

    public SetFilePermissions(final String fileMatcher, final Boolean isReadable, final Boolean isWritable, final Boolean isExecutable) {
        this.fileMatcher = fileMatcher;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
        this.isExecutable = isExecutable;
    }

    public String getFileMatcher() {
        return this.fileMatcher;
    }

    public Boolean getIsReadable() {
        return this.isReadable;
    }

    public Boolean getIsWritable() {
        return this.isWritable;
    }

    public Boolean getIsExecutable() {
        return this.isExecutable;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "SetFilePermissions(fileMatcher=" + this.getFileMatcher() + ", isReadable=" + this.getIsReadable() + ", isWritable=" + this.getIsWritable() + ", isExecutable=" + this.getIsExecutable() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof SetFilePermissions)) return false;
        final SetFilePermissions other = (SetFilePermissions) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$isReadable = this.getIsReadable();
        final Object other$isReadable = other.getIsReadable();
        if (this$isReadable == null ? other$isReadable != null : !this$isReadable.equals(other$isReadable)) return false;
        final Object this$isWritable = this.getIsWritable();
        final Object other$isWritable = other.getIsWritable();
        if (this$isWritable == null ? other$isWritable != null : !this$isWritable.equals(other$isWritable)) return false;
        final Object this$isExecutable = this.getIsExecutable();
        final Object other$isExecutable = other.getIsExecutable();
        if (this$isExecutable == null ? other$isExecutable != null : !this$isExecutable.equals(other$isExecutable)) return false;
        final Object this$fileMatcher = this.getFileMatcher();
        final Object other$fileMatcher = other.getFileMatcher();
        if (this$fileMatcher == null ? other$fileMatcher != null : !this$fileMatcher.equals(other$fileMatcher)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof SetFilePermissions;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $isReadable = this.getIsReadable();
        result = result * PRIME + ($isReadable == null ? 43 : $isReadable.hashCode());
        final Object $isWritable = this.getIsWritable();
        result = result * PRIME + ($isWritable == null ? 43 : $isWritable.hashCode());
        final Object $isExecutable = this.getIsExecutable();
        result = result * PRIME + ($isExecutable == null ? 43 : $isExecutable.hashCode());
        final Object $fileMatcher = this.getFileMatcher();
        result = result * PRIME + ($fileMatcher == null ? 43 : $fileMatcher.hashCode());
        return result;
    }
}
