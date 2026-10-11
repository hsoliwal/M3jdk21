// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindDeprecatedMethods.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import java.util.Iterator;
import static java.util.Objects.requireNonNull;

public final class FindDeprecatedMethods extends Recipe {
    private static final AnnotationMatcher DEPRECATED_MATCHER = new AnnotationMatcher("java.lang.Deprecated");
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "java.util.List add(..)", required = false)
    @Nullable
    private final String methodPattern;
    @Option(displayName = "Ignore deprecated scopes", description = "When set to `true` deprecated methods used within deprecated methods or classes will be ignored.", required = false)
    @Nullable
    private final Boolean ignoreDeprecatedScopes;

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated methods";
    }

    @Override
    public String getInstanceNameSuffix() {
        if (methodPattern != null) {
            return "matching `" + methodPattern + "`";
        }
        return super.getInstanceNameSuffix();
    }

    @Override
    public String getDescription() {
        return "Find uses of deprecated methods in any API.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = methodPattern == null || methodPattern.isEmpty() ? null : new MethodMatcher(methodPattern, true);
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                        if (methodMatcher == null || methodMatcher.matches(method)) {
                            for (JavaType.FullyQualified annotation : method.getAnnotations()) {
                                if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                    return SearchResult.found(cu);
                                }
                            }
                        }
                    }
                }
                return (J) tree;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (method.getMethodType() != null) {
                    for (JavaType.FullyQualified annotation : method.getMethodType().getAnnotations()) {
                        if ((methodMatcher == null || methodMatcher.matches(method)) && TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                            if (Boolean.TRUE.equals(ignoreDeprecatedScopes)) {
                                Iterator<Cursor> cursorPath = getCursor().getPathAsCursors();
                                while (cursorPath.hasNext()) {
                                    Cursor ancestor = cursorPath.next();
                                    if (ancestor.getValue() instanceof J.MethodDeclaration && isDeprecated(ancestor)) {
                                        return m;
                                    }
                                    if (ancestor.getValue() instanceof J.ClassDeclaration && isDeprecated(ancestor)) {
                                        return m;
                                    }
                                }
                            }
                            m = SearchResult.found(m);
                        }
                    }
                }
                return m;
            }
            private boolean isDeprecated(Cursor cursor) {
                return service(AnnotationService.class).matches(cursor, DEPRECATED_MATCHER);
            }
        });
    }

    public FindDeprecatedMethods(@Nullable final String methodPattern, @Nullable final Boolean ignoreDeprecatedScopes) {
        this.methodPattern = methodPattern;
        this.ignoreDeprecatedScopes = ignoreDeprecatedScopes;
    }

    @Nullable
    public String getMethodPattern() {
        return this.methodPattern;
    }

    @Nullable
    public Boolean getIgnoreDeprecatedScopes() {
        return this.ignoreDeprecatedScopes;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindDeprecatedMethods(methodPattern=" + this.getMethodPattern() + ", ignoreDeprecatedScopes=" + this.getIgnoreDeprecatedScopes() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindDeprecatedMethods)) return false;
        final FindDeprecatedMethods other = (FindDeprecatedMethods) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        final Object other$ignoreDeprecatedScopes = other.getIgnoreDeprecatedScopes();
        if (this$ignoreDeprecatedScopes == null ? other$ignoreDeprecatedScopes != null : !this$ignoreDeprecatedScopes.equals(other$ignoreDeprecatedScopes)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindDeprecatedMethods;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        result = result * PRIME + ($ignoreDeprecatedScopes == null ? 43 : $ignoreDeprecatedScopes.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        return result;
    }
}
