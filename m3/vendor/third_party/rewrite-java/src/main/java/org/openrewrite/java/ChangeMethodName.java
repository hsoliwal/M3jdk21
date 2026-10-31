// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeMethodName.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

public final class ChangeMethodName extends Recipe {
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method declarations/invocations.", example = "org.mockito.Matchers anyVararg()")
    private final String methodPattern;
    @Option(displayName = "New method name", description = "The method name that will replace the existing name.", example = "any")
    private final String newMethodName;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;
    @Option(displayName = "Ignore type definition", description = "When set to `true` the definition of the old type will be left untouched. " + "This is useful when you\'re replacing usage of a class but don\'t want to rename it.", required = false)
    @Nullable
    private final Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newMethodName);
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> condition = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (Boolean.TRUE.equals(ignoreDefinition)) {
                        J j = new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != j) {
                            return cu;
                        }
                    } else {
                        cu = (JavaSourceFile) new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != tree) {
                            return cu;
                        }
                    }
                    return new UsesMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
        return Preconditions.check(condition, new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (classDecl == null) {
                    return m;
                }
                if (methodMatcher.matches(method, classDecl)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type)).withMethodType(type);
                }
                return m;
            }
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type)).withMethodType(type);
                }
                return m;
            }
            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getMethodType()) && !m.getReference().getSimpleName().equals(newMethodName)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withReference(m.getReference().withSimpleName(newMethodName)).withMethodType(type);
                }
                return m;
            }
            /**
             * The only time field access should be relevant to changing method names is static imports.
             * This exists to turn
             * import static com.abc.B.static1;
             * into
             * import static com.abc.B.static2;
             */
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
                if (methodMatcher.isFullyQualifiedClassReference(f)) {
                    Expression target = f.getTarget();
                    if (target instanceof J.FieldAccess) {
                        String className = target.printTrimmed(getCursor());
                        String fullyQualified = className + "." + newMethodName;
                        return TypeTree.build(fullyQualified).withPrefix(f.getPrefix());
                    }
                }
                return f;
            }
        });
    }

    public ChangeMethodName(final String methodPattern, final String newMethodName, @Nullable final Boolean matchOverrides, @Nullable final Boolean ignoreDefinition) {
        this.methodPattern = methodPattern;
        this.newMethodName = newMethodName;
        this.matchOverrides = matchOverrides;
        this.ignoreDefinition = ignoreDefinition;
    }

    public String getMethodPattern() {
        return this.methodPattern;
    }

    public String getNewMethodName() {
        return this.newMethodName;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @Nullable
    public Boolean getIgnoreDefinition() {
        return this.ignoreDefinition;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeMethodName(methodPattern=" + this.getMethodPattern() + ", newMethodName=" + this.getNewMethodName() + ", matchOverrides=" + this.getMatchOverrides() + ", ignoreDefinition=" + this.getIgnoreDefinition() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeMethodName)) return false;
        final ChangeMethodName other = (ChangeMethodName) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$ignoreDefinition = this.getIgnoreDefinition();
        final Object other$ignoreDefinition = other.getIgnoreDefinition();
        if (this$ignoreDefinition == null ? other$ignoreDefinition != null : !this$ignoreDefinition.equals(other$ignoreDefinition)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        final Object this$newMethodName = this.getNewMethodName();
        final Object other$newMethodName = other.getNewMethodName();
        if (this$newMethodName == null ? other$newMethodName != null : !this$newMethodName.equals(other$newMethodName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeMethodName;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $ignoreDefinition = this.getIgnoreDefinition();
        result = result * PRIME + ($ignoreDefinition == null ? 43 : $ignoreDefinition.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        final Object $newMethodName = this.getNewMethodName();
        result = result * PRIME + ($newMethodName == null ? 43 : $newMethodName.hashCode());
        return result;
    }
}
