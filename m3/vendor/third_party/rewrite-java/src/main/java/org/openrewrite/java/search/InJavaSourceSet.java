// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/InJavaSourceSet.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import static java.util.Objects.requireNonNull;

public final class InJavaSourceSet<P> extends JavaIsoVisitor<P> {
    private final String sourceSet;

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            return cu.getMarkers().findFirst(JavaSourceSet.class).filter(sourceSet -> !sourceSet.getName().equals(this.sourceSet)).map(sourceSet -> cu).orElse(SearchResult.found(cu));
        }
        return super.visit(tree, p);
    }

    public InJavaSourceSet(final String sourceSet) {
        this.sourceSet = sourceSet;
    }

    public String getSourceSet() {
        return this.sourceSet;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "InJavaSourceSet(sourceSet=" + this.getSourceSet() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof InJavaSourceSet)) return false;
        final InJavaSourceSet<?> other = (InJavaSourceSet<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$sourceSet = this.getSourceSet();
        final Object other$sourceSet = other.getSourceSet();
        if (this$sourceSet == null ? other$sourceSet != null : !this$sourceSet.equals(other$sourceSet)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof InJavaSourceSet;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $sourceSet = this.getSourceSet();
        result = result * PRIME + ($sourceSet == null ? 43 : $sourceSet.hashCode());
        return result;
    }
}
