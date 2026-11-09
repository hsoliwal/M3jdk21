// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ReorderMethodArguments.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import java.util.ArrayList;
import java.util.List;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * This recipe finds method invocations matching the given method pattern and reorders the arguments based on the ordered
 * array of parameter names.
 */
public final class ReorderMethodArguments extends Recipe {
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "com.yourorg.A foo(String, Integer, Integer)")
    private final String methodPattern;
    /**
     * An array of parameter names that indicates the new order in which those arguments should be arranged.
     */
    @Option(displayName = "New parameter names", description = "An array of parameter names that indicates the new order in which those arguments should be arranged.", example = "[foo, bar, baz]")
    private final String[] newParameterNames;
    /**
     * If the original method signature is not type-attributed, this is an optional list that indicates the original order
     * in which the arguments were arranged.
     */
    @Option(displayName = "Old parameter names", description = "If the original method signature is not type-attributed, this is an optional list that indicates the original order in which the arguments were arranged.", required = false, example = "[baz, bar, foo]")
    @Nullable
    private final String[] oldParameterNames;
    @Option(displayName = "Ignore type definition", description = "When set to `true` the definition of the old type will be left untouched. " + "This is useful when you\'re replacing usage of a class but don\'t want to rename it.", required = false)
    @Nullable
    private final Boolean ignoreDefinition;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }

    @Override
    public String getDisplayName() {
        return "Reorder method arguments";
    }

    @Override
    public String getDescription() {
        return "Reorder method arguments into the specified order.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    if (Boolean.TRUE.equals(ignoreDefinition)) {
                        J j = new DeclaresMethod<>(methodPattern, true).visitNonNull(cu, ctx);
                        if (cu != j) {
                            return cu;
                        }
                    }
                    return new UsesMethod<>(methodPattern).visitNonNull(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        }, new ReorderMethodArgumentsVisitor(new MethodMatcher(methodPattern, matchOverrides)));
    }


    private class ReorderMethodArgumentsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ReorderMethodArgumentsVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            return (J.MethodInvocation) visitMethodCall(super.visitMethodInvocation(method, ctx));
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            return (J.NewClass) visitMethodCall(super.visitNewClass(newClass, ctx));
        }

        private MethodCall visitMethodCall(MethodCall m) {
            if (methodMatcher.matches(m) && m.getMethodType() != null) {
                @SuppressWarnings("ConstantConditions")
                List<String> paramNames = oldParameterNames == null || oldParameterNames.length == 0 ? m.getMethodType().getParameterNames() : asList(oldParameterNames);
                List<JRightPadded<Expression>> originalArgs = getPaddedArguments(m);
                int resolvedParamCount = m.getMethodType().getParameterTypes().size();
                int i = 0;
                List<JRightPadded<Expression>> reordered = new ArrayList<>(originalArgs.size());
                List<String> reorderedNames = new ArrayList<>(originalArgs.size());
                List<JavaType> reorderedTypes = new ArrayList<>(originalArgs.size());
                List<Space> formattings = new ArrayList<>(originalArgs.size());
                List<Space> rightFormattings = new ArrayList<>(originalArgs.size());
                for (String name : newParameterNames) {
                    int fromPos = paramNames.indexOf(name);
                    if (originalArgs.size() > resolvedParamCount && fromPos == resolvedParamCount - 1) {
                        // this is a varargs argument
                        List<JRightPadded<Expression>> varargs = originalArgs.subList(fromPos, originalArgs.size());
                        reordered.addAll(varargs);
                        for (int j = 0; j < varargs.size(); j++) {
                            reorderedNames.add(name + j);
                            reorderedTypes.add(varargs.get(j).getElement().getType());
                        }
                        for (JRightPadded<Expression> exp : originalArgs.subList(i, (i++) + varargs.size())) {
                            formattings.add(exp.getElement().getPrefix());
                            rightFormattings.add(exp.getAfter());
                        }
                    } else if (fromPos >= 0 && originalArgs.size() > fromPos) {
                        JRightPadded<Expression> originalArg = originalArgs.get(fromPos);
                        reordered.add(originalArg);
                        reorderedNames.add(name);
                        reorderedTypes.add(originalArg.getElement().getType());
                        formattings.add(originalArgs.get(i).getElement().getPrefix());
                        rightFormattings.add(originalArgs.get(i++).getAfter());
                    }
                }
                boolean changed = false;
                i = 0;
                for (JRightPadded<Expression> expression : reordered) {
                    final int index = i;
                    reordered.set(i, expression.map(e -> e.withPrefix(formattings.get(index))).withAfter(rightFormattings.get(index)));
                    if (reordered.get(i) != originalArgs.get(i)) {
                        changed = true;
                    }
                    i++;
                }
                if (changed) {
                    JavaType.Method mt = m.getMethodType().withParameterNames(reorderedNames).withParameterTypes(reorderedTypes);
                    m = withPaddedArguments(m, reordered).withMethodType(mt);
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(mt));
                    }
                }
            }
            return m;
        }

        private List<JRightPadded<Expression>> getPaddedArguments(MethodCall m) {
            if (m instanceof J.MethodInvocation) {
                return ((J.MethodInvocation) m).getPadding().getArguments().getPadding().getElements();
            } else if (m instanceof J.NewClass) {
                return ((J.NewClass) m).getPadding().getArguments().getPadding().getElements();
            } else {
                throw new IllegalArgumentException("Unknown MethodCall type");
            }
        }

        private MethodCall withPaddedArguments(MethodCall m, List<JRightPadded<Expression>> reordered) {
            if (m instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return mi.getPadding().withArguments(mi.getPadding().getArguments().getPadding().withElements(reordered));
            } else if (m instanceof J.NewClass) {
                J.NewClass nc = (J.NewClass) m;
                return nc.getPadding().withArguments(nc.getPadding().getArguments().getPadding().withElements(reordered));
            } else {
                throw new IllegalArgumentException("Unknown MethodCall type");
            }
        }
    }

    public ReorderMethodArguments(final String methodPattern, final String[] newParameterNames, @Nullable final String[] oldParameterNames, @Nullable final Boolean ignoreDefinition, @Nullable final Boolean matchOverrides) {
        this.methodPattern = methodPattern;
        this.newParameterNames = newParameterNames;
        this.oldParameterNames = oldParameterNames;
        this.ignoreDefinition = ignoreDefinition;
        this.matchOverrides = matchOverrides;
    }

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    /**
     * An array of parameter names that indicates the new order in which those arguments should be arranged.
     */
    public String[] getNewParameterNames() {
        return this.newParameterNames;
    }

    /**
     * If the original method signature is not type-attributed, this is an optional list that indicates the original order
     * in which the arguments were arranged.
     */
    @Nullable
    public String[] getOldParameterNames() {
        return this.oldParameterNames;
    }

    @Nullable
    public Boolean getIgnoreDefinition() {
        return this.ignoreDefinition;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ReorderMethodArguments(methodPattern=" + this.getMethodPattern() + ", newParameterNames=" + java.util.Arrays.deepToString(this.getNewParameterNames()) + ", oldParameterNames=" + java.util.Arrays.deepToString(this.getOldParameterNames()) + ", ignoreDefinition=" + this.getIgnoreDefinition() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ReorderMethodArguments)) return false;
        final ReorderMethodArguments other = (ReorderMethodArguments) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$ignoreDefinition = this.getIgnoreDefinition();
        final Object other$ignoreDefinition = other.getIgnoreDefinition();
        if (this$ignoreDefinition == null ? other$ignoreDefinition != null : !this$ignoreDefinition.equals(other$ignoreDefinition)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        if (!java.util.Arrays.deepEquals(this.getNewParameterNames(), other.getNewParameterNames())) return false;
        if (!java.util.Arrays.deepEquals(this.getOldParameterNames(), other.getOldParameterNames())) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ReorderMethodArguments;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ignoreDefinition = this.getIgnoreDefinition();
        result = result * PRIME + ($ignoreDefinition == null ? 43 : $ignoreDefinition.hashCode());
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getNewParameterNames());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getOldParameterNames());
        return result;
    }
}
