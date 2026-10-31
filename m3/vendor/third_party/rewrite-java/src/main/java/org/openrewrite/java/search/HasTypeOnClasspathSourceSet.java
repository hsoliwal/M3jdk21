// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/HasTypeOnClasspathSourceSet.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import static java.util.Objects.requireNonNull;

public final class HasTypeOnClasspathSourceSet<P> extends JavaIsoVisitor<P> {
    private final String fullyQualifiedTypeName;

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            return cu.getMarkers().findFirst(JavaSourceSet.class).filter(sourceSet -> {
                for (JavaType.FullyQualified classpath : sourceSet.getClasspath()) {
                    if (TypeUtils.isOfClassType(classpath, fullyQualifiedTypeName)) {
                        return false;
                    }
                }
                return true;
            }).map(sourceSet -> cu).orElse(SearchResult.found(cu));
        }
        return super.visit(tree, p);
    }

    public HasTypeOnClasspathSourceSet(final String fullyQualifiedTypeName) {
        this.fullyQualifiedTypeName = fullyQualifiedTypeName;
    }

    public String getFullyQualifiedTypeName() {
        return this.fullyQualifiedTypeName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "HasTypeOnClasspathSourceSet(fullyQualifiedTypeName=" + this.getFullyQualifiedTypeName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof HasTypeOnClasspathSourceSet)) return false;
        final HasTypeOnClasspathSourceSet<?> other = (HasTypeOnClasspathSourceSet<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$fullyQualifiedTypeName = this.getFullyQualifiedTypeName();
        final Object other$fullyQualifiedTypeName = other.getFullyQualifiedTypeName();
        if (this$fullyQualifiedTypeName == null ? other$fullyQualifiedTypeName != null : !this$fullyQualifiedTypeName.equals(other$fullyQualifiedTypeName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof HasTypeOnClasspathSourceSet;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $fullyQualifiedTypeName = this.getFullyQualifiedTypeName();
        result = result * PRIME + ($fullyQualifiedTypeName == null ? 43 : $fullyQualifiedTypeName.hashCode());
        return result;
    }
}
