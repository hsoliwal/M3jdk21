// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeMethodTargetToStatic.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import java.util.LinkedHashSet;
import java.util.Set;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public final class ChangeMethodTargetToStatic extends Recipe {
    /**
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations. " + "The original method call may or may not be a static method invocation.", example = "com.google.common.collect.ImmutableSet of(..)")
    private final String methodPattern;
    @Option(displayName = "Fully-qualified target type name", description = "A fully-qualified class name of the type upon which the static method is defined.", example = "java.util.Set")
    private final String fullyQualifiedTargetTypeName;
    @Option(displayName = "Return type after change", description = "Sometimes changing the target type also changes the return type. In the Guava example, changing from `ImmutableSet#of(..)` to `Set#of(..)` widens the return type from Guava\'s `ImmutableSet` to just `java.util.Set`.", example = "java.util.Set", required = false)
    @Nullable
    private final String returnType;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;
    @Option(displayName = "Match unknown types", description = "When enabled, include method invocations which appear to match if full type information is missing. " + "Using matchUnknownTypes can improve recipe resiliency for an AST with missing type information, but " + "also increases the risk of false-positive matches on unrelated method invocations.", required = false)
    @Nullable
    private final Boolean matchUnknownTypes;

    public ChangeMethodTargetToStatic(String methodPattern, String fullyQualifiedTargetTypeName, @Nullable String returnType, @Nullable Boolean matchOverrides) {
        this(methodPattern, fullyQualifiedTargetTypeName, returnType, matchOverrides, false);
    }

    @JsonCreator
    public ChangeMethodTargetToStatic(String methodPattern, String fullyQualifiedTargetTypeName, @Nullable String returnType, @Nullable Boolean matchOverrides, @Nullable Boolean matchUnknownTypes) {
        this.methodPattern = methodPattern;
        this.fullyQualifiedTargetTypeName = fullyQualifiedTargetTypeName;
        this.returnType = returnType;
        this.matchOverrides = matchOverrides;
        this.matchUnknownTypes = matchUnknownTypes;
    }

    @Override
    public String getDisplayName() {
        return "Change method target to static";
    }

    @Override
    public String getDescription() {
        return "Change method invocations to static method calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean matchUnknown = Boolean.TRUE.equals(matchUnknownTypes);
        ChangeMethodTargetToStaticVisitor visitor = new ChangeMethodTargetToStaticVisitor(new MethodMatcher(methodPattern, matchOverrides), matchUnknown);
        return matchUnknown ? visitor : Preconditions.check(new UsesMethod<>(methodPattern, matchOverrides), visitor);
    }


    private class ChangeMethodTargetToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final boolean matchUnknownTypes;
        private final JavaType.FullyQualified classType = JavaType.ShallowClass.build(fullyQualifiedTargetTypeName);

        public ChangeMethodTargetToStaticVisitor(MethodMatcher methodMatcher, boolean matchUnknownTypes) {
            this.methodMatcher = methodMatcher;
            this.matchUnknownTypes = matchUnknownTypes;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            boolean isStatic = method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static);
            boolean isSameReceiverType = method.getSelect() != null && TypeUtils.isOfClassType(method.getSelect().getType(), fullyQualifiedTargetTypeName);
            if ((!isStatic || !isSameReceiverType) && methodMatcher.matches(method, matchUnknownTypes)) {
                JavaType.Method transformedType = null;
                if (method.getMethodType() != null) {
                    maybeRemoveImport(method.getMethodType().getDeclaringType());
                    transformedType = method.getMethodType().withDeclaringType(classType);
                    if (!method.getMethodType().hasFlags(Flag.Static)) {
                        Set<Flag> flags = new LinkedHashSet<>(method.getMethodType().getFlags());
                        flags.add(Flag.Static);
                        transformedType = transformedType.withFlags(flags);
                    }
                    if (returnType != null) {
                        JavaType returnTypeType = JavaType.ShallowClass.build(returnType);
                        transformedType = transformedType.withReturnType(returnTypeType);
                    }
                }
                if (m.getSelect() == null) {
                    maybeAddImport(fullyQualifiedTargetTypeName, m.getSimpleName(), !matchUnknownTypes);
                } else {
                    maybeAddImport(fullyQualifiedTargetTypeName, !matchUnknownTypes);
                    m = method.withSelect(new J.Identifier(randomId(), method.getSelect() == null ? Space.EMPTY : method.getSelect().getPrefix(), Markers.EMPTY, emptyList(), classType.getClassName(), classType, null));
                }
                m = m.withMethodType(transformedType).withName(m.getName().withType(transformedType));
            }
            return m;
        }
    }

    /**
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    public String getFullyQualifiedTargetTypeName() {
        return this.fullyQualifiedTargetTypeName;
    }

    @Nullable
    public String getReturnType() {
        return this.returnType;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @Nullable
    public Boolean getMatchUnknownTypes() {
        return this.matchUnknownTypes;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeMethodTargetToStatic(methodPattern=" + this.getMethodPattern() + ", fullyQualifiedTargetTypeName=" + this.getFullyQualifiedTargetTypeName() + ", returnType=" + this.getReturnType() + ", matchOverrides=" + this.getMatchOverrides() + ", matchUnknownTypes=" + this.getMatchUnknownTypes() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeMethodTargetToStatic)) return false;
        final ChangeMethodTargetToStatic other = (ChangeMethodTargetToStatic) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$matchUnknownTypes = this.getMatchUnknownTypes();
        final Object other$matchUnknownTypes = other.getMatchUnknownTypes();
        if (this$matchUnknownTypes == null ? other$matchUnknownTypes != null : !this$matchUnknownTypes.equals(other$matchUnknownTypes)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        final Object this$fullyQualifiedTargetTypeName = this.getFullyQualifiedTargetTypeName();
        final Object other$fullyQualifiedTargetTypeName = other.getFullyQualifiedTargetTypeName();
        if (this$fullyQualifiedTargetTypeName == null ? other$fullyQualifiedTargetTypeName != null : !this$fullyQualifiedTargetTypeName.equals(other$fullyQualifiedTargetTypeName)) return false;
        final Object this$returnType = this.getReturnType();
        final Object other$returnType = other.getReturnType();
        if (this$returnType == null ? other$returnType != null : !this$returnType.equals(other$returnType)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeMethodTargetToStatic;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $matchUnknownTypes = this.getMatchUnknownTypes();
        result = result * PRIME + ($matchUnknownTypes == null ? 43 : $matchUnknownTypes.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        final Object $fullyQualifiedTargetTypeName = this.getFullyQualifiedTargetTypeName();
        result = result * PRIME + ($fullyQualifiedTargetTypeName == null ? 43 : $fullyQualifiedTargetTypeName.hashCode());
        final Object $returnType = this.getReturnType();
        result = result * PRIME + ($returnType == null ? 43 : $returnType.hashCode());
        return result;
    }
}
