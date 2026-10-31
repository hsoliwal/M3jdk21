// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/HasSourceSet.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import static java.util.Objects.requireNonNull;

public final class HasSourceSet extends Recipe {
    @Option(displayName = "Source set", description = "The source set to search for.", example = "main")
    private final String sourceSet;

    @Override
    public String getDisplayName() {
        return "Find files in a source set";
    }

    @Override
    public String getDescription() {
        return "Source sets are a way to organize your source code into logical groups. " + "For example, Java projects commonly have a `main` source set for application code and " + "a `test` source set for test code. This recipe will find all files in a given source set.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    if (cu.getMarkers().findFirst(JavaSourceSet.class).filter(s -> s.getName().equals(sourceSet)).isPresent()) {
                        return SearchResult.found(cu);
                    }
                }
                return (J) tree;
            }
        };
    }

    public HasSourceSet(final String sourceSet) {
        this.sourceSet = sourceSet;
    }

    public String getSourceSet() {
        return this.sourceSet;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "HasSourceSet(sourceSet=" + this.getSourceSet() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof HasSourceSet)) return false;
        final HasSourceSet other = (HasSourceSet) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$sourceSet = this.getSourceSet();
        final Object other$sourceSet = other.getSourceSet();
        if (this$sourceSet == null ? other$sourceSet != null : !this$sourceSet.equals(other$sourceSet)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof HasSourceSet;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $sourceSet = this.getSourceSet();
        result = result * PRIME + ($sourceSet == null ? 43 : $sourceSet.hashCode());
        return result;
    }
}
