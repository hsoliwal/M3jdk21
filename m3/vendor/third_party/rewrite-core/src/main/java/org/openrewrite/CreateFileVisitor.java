// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/CreateFileVisitor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.Objects.requireNonNull;

public final class CreateFileVisitor extends TreeVisitor<Tree, ExecutionContext> {
    @NonNull
    private final Path relativeFileName;
    @NonNull
    private final AtomicBoolean shouldCreate;

    @Override
    public Tree visit(@Nullable Tree tree, @NonNull ExecutionContext ctx) {
        SourceFile sourceFile = (SourceFile) requireNonNull(tree);
        if (relativeFileName.equals(sourceFile.getSourcePath())) {
            shouldCreate.set(false);
        }
        return sourceFile;
    }

    public CreateFileVisitor(@NonNull final Path relativeFileName, @NonNull final AtomicBoolean shouldCreate) {
        this.relativeFileName = relativeFileName;
        this.shouldCreate = shouldCreate;
    }

    @NonNull
    public Path getRelativeFileName() {
        return this.relativeFileName;
    }

    @NonNull
    public AtomicBoolean getShouldCreate() {
        return this.shouldCreate;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CreateFileVisitor(relativeFileName=" + this.getRelativeFileName() + ", shouldCreate=" + this.getShouldCreate() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CreateFileVisitor)) return false;
        final CreateFileVisitor other = (CreateFileVisitor) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$relativeFileName = this.getRelativeFileName();
        final Object other$relativeFileName = other.getRelativeFileName();
        if (this$relativeFileName == null ? other$relativeFileName != null : !this$relativeFileName.equals(other$relativeFileName)) return false;
        final Object this$shouldCreate = this.getShouldCreate();
        final Object other$shouldCreate = other.getShouldCreate();
        if (this$shouldCreate == null ? other$shouldCreate != null : !this$shouldCreate.equals(other$shouldCreate)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof CreateFileVisitor;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $relativeFileName = this.getRelativeFileName();
        result = result * PRIME + ($relativeFileName == null ? 43 : $relativeFileName.hashCode());
        final Object $shouldCreate = this.getShouldCreate();
        result = result * PRIME + ($shouldCreate == null ? 43 : $shouldCreate.hashCode());
        return result;
    }
}
