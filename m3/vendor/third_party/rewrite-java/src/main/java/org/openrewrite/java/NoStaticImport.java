// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/NoStaticImport.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import static java.util.Collections.emptyList;

public class NoStaticImport extends Recipe {
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "java.util.Collections emptyList()")
    private final String methodPattern;

    @Override
    public String getDisplayName() {
        return "Remove static import";
    }

    @Override
    public String getDescription() {
        return "Removes static imports and replaces them with qualified references. For example, `emptyList()` becomes `Collections.emptyList()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(m) && m.getSelect() == null && m.getMethodType() != null) {
                    // Do not replace calls to super() in constructors
                    if (m.getName().getSimpleName().equals("super")) {
                        return m;
                    }
                    JavaType.FullyQualified receiverType = m.getMethodType().getDeclaringType();
                    RemoveImport<ExecutionContext> op = new RemoveImport<>(receiverType.getFullyQualifiedName() + "." + method.getSimpleName(), true);
                    if (!getAfterVisit().contains(op)) {
                        doAfterVisit(op);
                    }
                    J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    // Do not replace if method is in java.lang.Object
                    if (
                    // Do not replace when we can not determine the enclosing class, such as in build.gradle files
                    // Do not replace if receiverType is the same as surrounding class
                    // Do not replace if method is in a wrapping outer class; not looking up more than one level
                    "java.lang.Object".equals(receiverType.getFullyQualifiedName()) || enclosingClass == null || enclosingClass.getType() == null || enclosingClass.getType().equals(receiverType) || enclosingClass.getType().getOwningClass() != null && enclosingClass.getType().getOwningClass().equals(receiverType) || 
                    // Do not replace if class extends receiverType
                    enclosingClass.getType().getSupertype() != null && enclosingClass.getType().getSupertype().equals(receiverType)) {
                        return m;
                    }
                    // Change method invocation to use fully qualified name
                    maybeAddImport(receiverType.getFullyQualifiedName());
                    m = m.withSelect(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), receiverType.getClassName(), receiverType, null));
                }
                return m;
            }
        });
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "NoStaticImport(methodPattern=" + this.getMethodPattern() + ")";
    }

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    public NoStaticImport(final String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof NoStaticImport)) return false;
        final NoStaticImport other = (NoStaticImport) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof NoStaticImport;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        return result;
    }
}
