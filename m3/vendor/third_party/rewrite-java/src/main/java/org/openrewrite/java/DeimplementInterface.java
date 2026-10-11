// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/DeimplementInterface.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public final class DeimplementInterface<P> extends JavaIsoVisitor<P> {
    private final String fullyQualifiedInterfaceName;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        maybeRemoveImport(fullyQualifiedInterfaceName);
        return super.visitClassDeclaration(classDecl, p).withImplements(ListUtils.map(classDecl.getImplements(), impl -> TypeUtils.isOfClassType(impl.getType(), fullyQualifiedInterfaceName) ? null : impl));
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        if (method.getMethodType() != null && method.getMethodType().isInheritedFrom(fullyQualifiedInterfaceName)) {
            //noinspection ConstantConditions
            return null;
        }
        return super.visitMethodDeclaration(method, p);
    }

    public DeimplementInterface(final String fullyQualifiedInterfaceName) {
        this.fullyQualifiedInterfaceName = fullyQualifiedInterfaceName;
    }

    public String getFullyQualifiedInterfaceName() {
        return this.fullyQualifiedInterfaceName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeimplementInterface(fullyQualifiedInterfaceName=" + this.getFullyQualifiedInterfaceName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeimplementInterface)) return false;
        final DeimplementInterface<?> other = (DeimplementInterface<?>) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
        final Object other$fullyQualifiedInterfaceName = other.getFullyQualifiedInterfaceName();
        if (this$fullyQualifiedInterfaceName == null ? other$fullyQualifiedInterfaceName != null : !this$fullyQualifiedInterfaceName.equals(other$fullyQualifiedInterfaceName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeimplementInterface;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
        result = result * PRIME + ($fullyQualifiedInterfaceName == null ? 43 : $fullyQualifiedInterfaceName.hashCode());
        return result;
    }
}
