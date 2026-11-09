// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FindQuarks.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import static java.util.Objects.requireNonNull;

public final class FindQuarks extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find instances of type `Quark`";
    }

    @Override
    public String getDescription() {
        return "`Quark` source files are pointers to the existence of a file without capturing any of the contents of the file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark) {
                    return SearchResult.found(sourceFile);
                }
                return sourceFile;
            }
        };
    }

    public FindQuarks() {
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindQuarks()";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindQuarks)) return false;
        final FindQuarks other = (FindQuarks) o;
        if (!other.canEqual((Object) this)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindQuarks;
    }

    @Override
    public int hashCode() {
        final int result = 1;
        return result;
    }
}
