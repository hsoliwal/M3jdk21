// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/ResultOfMethodCallIgnored.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

public final class ResultOfMethodCallIgnored extends Recipe {
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "java.io.File mkdir*()")
    private final String methodPattern;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Result of method call ignored";
    }

    @Override
    public String getDescription() {
        return "Find locations where the result of the method call is being ignored.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    if (getCursor().getParentTreeCursor().getValue() instanceof J.Block) {
                        m = SearchResult.found(m);
                    }
                }
                return m;
            }
        };
    }

    public ResultOfMethodCallIgnored(final String methodPattern, @Nullable final Boolean matchOverrides) {
        this.methodPattern = methodPattern;
        this.matchOverrides = matchOverrides;
    }

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ResultOfMethodCallIgnored(methodPattern=" + this.getMethodPattern() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ResultOfMethodCallIgnored)) return false;
        final ResultOfMethodCallIgnored other = (ResultOfMethodCallIgnored) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ResultOfMethodCallIgnored;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        return result;
    }
}
