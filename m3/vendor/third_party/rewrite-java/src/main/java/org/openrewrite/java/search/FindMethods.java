// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindMethods.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds matching method invocations.
 */
public final class FindMethods extends Recipe {
    private final transient MethodCalls methodCalls = new MethodCalls(this);
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "java.util.List add(..)")
    private final String methodPattern;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Find method usages";
    }

    @Override
    public String getDescription() {
        return "Find method calls by pattern.";
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);
        return Preconditions.check(new UsesMethod<>(methodPattern, matchOverrides), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(javaSourceFile.getSourcePath().toString(), method.printTrimmed(getCursor()), method.getMethodType().getDeclaringType().getFullyQualifiedName(), method.getSimpleName(), method.getArguments().stream().map(Expression::getType).map(String::valueOf).collect(Collectors.joining(", "))));
                    }
                    m = SearchResult.found(m);
                }
                return m;
            }
            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getMethodType())) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(javaSourceFile.getSourcePath().toString(), memberRef.printTrimmed(getCursor()), memberRef.getMethodType().getDeclaringType().getFullyQualifiedName(), memberRef.getMethodType().getName(), memberRef.getArguments().stream().map(Expression::getType).map(String::valueOf).collect(Collectors.joining(", "))));
                    }
                    m = m.withReference(SearchResult.found(m.getReference()));
                }
                return m;
            }
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (methodMatcher.matches(newClass)) {
                    JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (javaSourceFile != null) {
                        methodCalls.insertRow(ctx, new MethodCalls.Row(javaSourceFile.getSourcePath().toString(), newClass.printTrimmed(getCursor()), newClass.getType().toString(), "<constructor>", newClass.getArguments().stream().map(Expression::getType).map(String::valueOf).collect(Collectors.joining(", "))));
                    }
                    n = SearchResult.found(n);
                }
                return n;
            }
        });
    }

    public static Set<J> find(J j, String methodPattern) {
        return find(j, methodPattern, false);
    }

    /**
     * @param j              The subtree to search.
     * @param methodPattern  A method pattern. See {@link MethodMatcher} for details about this syntax.
     * @param matchOverrides Whether to match overrides.
     * @return A set of {@link J.MethodInvocation}, {@link J.MemberReference}, and {@link J.NewClass} representing calls to this method.
     */
    public static Set<J> find(J j, String methodPattern, boolean matchOverrides) {
        FindMethods findMethods = new FindMethods(methodPattern, matchOverrides);
        findMethods.methodCalls.setEnabled(false);
        return TreeVisitor.collect(findMethods.getVisitor(), j, new HashSet<>()).stream().filter(t -> t instanceof J.MethodInvocation || t instanceof J.MemberReference || t instanceof J.NewClass).map(t -> (J) t).collect(Collectors.toSet());
    }

    public static Set<J.MethodDeclaration> findDeclaration(J j, String methodPattern) {
        return findDeclaration(j, methodPattern, false);
    }

    public static Set<J.MethodDeclaration> findDeclaration(J j, String methodPattern, boolean matchOverrides) {
        return TreeVisitor.collect(new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && methodMatcher.matches(method, getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class))) {
                    return SearchResult.found(method);
                } else if (methodMatcher.matches(method.getMethodType())) {
                    return SearchResult.found(method);
                }
                return super.visitMethodDeclaration(method, ctx);
            }
        }, j, new HashSet<>()).stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast).collect(Collectors.toSet());
    }

    public FindMethods(final String methodPattern, @Nullable final Boolean matchOverrides) {
        this.methodPattern = methodPattern;
        this.matchOverrides = matchOverrides;
    }

    public MethodCalls getMethodCalls() {
        return this.methodCalls;
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
        return "FindMethods(methodCalls=" + this.getMethodCalls() + ", methodPattern=" + this.getMethodPattern() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindMethods)) return false;
        final FindMethods other = (FindMethods) o;
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
        return other instanceof FindMethods;
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
