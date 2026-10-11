// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeMethodTargetToVariable.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import java.util.LinkedHashSet;
import java.util.Set;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public final class ChangeMethodTargetToVariable extends Recipe {
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "org.mycorp.A method(..)")
    private final String methodPattern;
    @Option(displayName = "Variable name", description = "Name of variable to use as target for the modified method invocation.", example = "foo")
    private final String variableName;
    @Option(displayName = "Variable type", description = "Type attribution to use for the return type of the modified method invocation.", example = "java.lang.String")
    private final String variableType;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Change method target to variable";
    }

    @Override
    public String getDescription() {
        return "Change method invocations to method calls on a variable.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern, matchOverrides), new ChangeMethodTargetToVariableVisitor(new MethodMatcher(methodPattern, matchOverrides), JavaType.ShallowClass.build(variableType)));
    }


    private class ChangeMethodTargetToVariableVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final JavaType.Class variableType;

        private ChangeMethodTargetToVariableVisitor(MethodMatcher methodMatcher, JavaType.Class variableType) {
            this.methodMatcher = methodMatcher;
            this.variableType = variableType;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(m)) {
                JavaType.Method methodType = null;
                if (m.getMethodType() != null) {
                    maybeRemoveImport(m.getMethodType().getDeclaringType());
                    Set<Flag> flags = new LinkedHashSet<>(m.getMethodType().getFlags());
                    flags.remove(Flag.Static);
                    methodType = m.getMethodType().withDeclaringType(this.variableType).withFlags(flags);
                }
                m = m.withSelect(new J.Identifier(randomId(), m.getSelect() == null ? Space.EMPTY : m.getSelect().getPrefix(), Markers.EMPTY, emptyList(), variableName, this.variableType, null)).withMethodType(methodType).withName(m.getName().withType(methodType));
            }
            return m;
        }
    }

    public ChangeMethodTargetToVariable(final String methodPattern, final String variableName, final String variableType, @Nullable final Boolean matchOverrides) {
        this.methodPattern = methodPattern;
        this.variableName = variableName;
        this.variableType = variableType;
        this.matchOverrides = matchOverrides;
    }

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    public String getVariableName() {
        return this.variableName;
    }

    public String getVariableType() {
        return this.variableType;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeMethodTargetToVariable(methodPattern=" + this.getMethodPattern() + ", variableName=" + this.getVariableName() + ", variableType=" + this.getVariableType() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeMethodTargetToVariable)) return false;
        final ChangeMethodTargetToVariable other = (ChangeMethodTargetToVariable) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        final Object this$variableName = this.getVariableName();
        final Object other$variableName = other.getVariableName();
        if (this$variableName == null ? other$variableName != null : !this$variableName.equals(other$variableName)) return false;
        final Object this$variableType = this.getVariableType();
        final Object other$variableType = other.getVariableType();
        if (this$variableType == null ? other$variableType != null : !this$variableType.equals(other$variableType)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeMethodTargetToVariable;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        final Object $variableName = this.getVariableName();
        result = result * PRIME + ($variableName == null ? 43 : $variableName.hashCode());
        final Object $variableType = this.getVariableType();
        result = result * PRIME + ($variableType == null ? 43 : $variableType.hashCode());
        return result;
    }
}
