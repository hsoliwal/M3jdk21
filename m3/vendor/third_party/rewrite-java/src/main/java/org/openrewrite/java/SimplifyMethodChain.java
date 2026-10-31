// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/SimplifyMethodChain.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class SimplifyMethodChain extends Recipe {
    @Option(displayName = "Method pattern chain", description = "A list of method patterns that are called in sequence", example = "[\'java.util.Map keySet()\', \'java.util.Set contains(..)\']")
    private final List<String> methodPatternChain;
    @Option(displayName = "New method name", description = "The method name that will replace the existing name. The new method name target is assumed to have the same arguments as the last method in the chain.", example = "containsKey")
    private final String newMethodName;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false, example = "false")
    @Nullable
    private final Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Simplify a call chain";
    }

    @Override
    public String getDescription() {
        return "Simplify `a.b().c()` to `a.d()`.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("methodPatternChain", "Requires more than one pattern", methodPatternChain, c -> c != null && c.size() > 1));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<MethodMatcher> matchers = methodPatternChain.stream().map(matcher -> new MethodMatcher(matcher, matchOverrides)).collect(Collectors.toList());
        Collections.reverse(matchers);
        return Preconditions.check(new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                for (String method : methodPatternChain) {
                    doAfterVisit(new UsesMethod<>(method, matchOverrides));
                }
                return cu;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                Expression select = m;
                for (MethodMatcher matcher : matchers) {
                    if (!(select instanceof J.MethodInvocation) || !matcher.matches((J.MethodInvocation) select)) {
                        return m;
                    }
                    select = m.getSelect();
                }
                if (select instanceof J.MethodInvocation) {
                    assert m.getMethodType() != null;
                    JavaType.Method mt = m.getMethodType().withName(newMethodName);
                    return m.withSelect(((J.MethodInvocation) select).getSelect()).withName(m.getName().withSimpleName(newMethodName).withType(mt)).withMethodType(mt);
                }
                return m;
            }
        });
    }

    public SimplifyMethodChain(final List<String> methodPatternChain, final String newMethodName, @Nullable final Boolean matchOverrides) {
        this.methodPatternChain = methodPatternChain;
        this.newMethodName = newMethodName;
        this.matchOverrides = matchOverrides;
    }

    public List<String> getMethodPatternChain() {
        return this.methodPatternChain;
    }

    public String getNewMethodName() {
        return this.newMethodName;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "SimplifyMethodChain(methodPatternChain=" + this.getMethodPatternChain() + ", newMethodName=" + this.getNewMethodName() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof SimplifyMethodChain)) return false;
        final SimplifyMethodChain other = (SimplifyMethodChain) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$methodPatternChain = this.getMethodPatternChain();
        final Object other$methodPatternChain = other.getMethodPatternChain();
        if (this$methodPatternChain == null ? other$methodPatternChain != null : !this$methodPatternChain.equals(other$methodPatternChain)) return false;
        final Object this$newMethodName = this.getNewMethodName();
        final Object other$newMethodName = other.getNewMethodName();
        if (this$newMethodName == null ? other$newMethodName != null : !this$newMethodName.equals(other$newMethodName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof SimplifyMethodChain;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $methodPatternChain = this.getMethodPatternChain();
        result = result * PRIME + ($methodPatternChain == null ? 43 : $methodPatternChain.hashCode());
        final Object $newMethodName = this.getNewMethodName();
        result = result * PRIME + ($newMethodName == null ? 43 : $newMethodName.hashCode());
        return result;
    }
}
