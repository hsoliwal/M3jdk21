// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/DeleteMethodArgument.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * This recipe finds method invocations matching a method pattern and uses a zero-based argument index to determine
 * which argument is removed.
 */
public final class DeleteMethodArgument extends Recipe {
    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method invocations.", example = "com.yourorg.A foo(int, int)")
    private final String methodPattern;
    /**
     * A zero-based index that indicates which argument will be removed from the method invocation.
     */
    @Option(displayName = "Argument index", description = "A zero-based index that indicates which argument will be removed from the method invocation.", example = "0")
    private final int argumentIndex;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%d in methods `%s`", argumentIndex, methodPattern);
    }

    @Override
    public String getDisplayName() {
        return "Delete method argument";
    }

    @Override
    public String getDescription() {
        return "Delete an argument from method invocations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new DeleteMethodArgumentVisitor(new MethodMatcher(methodPattern)));
    }


    private class DeleteMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public DeleteMethodArgumentVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            return (J.MethodInvocation) visitMethodCall(m);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            return (J.NewClass) visitMethodCall(n);
        }

        private MethodCall visitMethodCall(MethodCall methodCall) {
            MethodCall m = methodCall;
            List<Expression> originalArgs = m.getArguments();
            if (methodMatcher.matches(m) && originalArgs.stream().filter(a -> !(a instanceof J.Empty)).count() >= argumentIndex + 1) {
                List<Expression> args = new ArrayList<>(originalArgs);
                args.remove(argumentIndex);
                if (args.isEmpty()) {
                    args = singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY));
                }
                m = m.withArguments(args);
                JavaType.Method methodType = m.getMethodType();
                if (methodType != null) {
                    List<String> parameterNames = new ArrayList<>(methodType.getParameterNames());
                    parameterNames.remove(argumentIndex);
                    List<JavaType> parameterTypes = new ArrayList<>(methodType.getParameterTypes());
                    parameterTypes.remove(argumentIndex);
                    m = m.withMethodType(methodType.withParameterNames(parameterNames).withParameterTypes(parameterTypes));
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(m.getMethodType()));
                    }
                }
            }
            return m;
        }
    }

    public DeleteMethodArgument(final String methodPattern, final int argumentIndex) {
        this.methodPattern = methodPattern;
        this.argumentIndex = argumentIndex;
    }

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    public String getMethodPattern() {
        return this.methodPattern;
    }

    /**
     * A zero-based index that indicates which argument will be removed from the method invocation.
     */
    public int getArgumentIndex() {
        return this.argumentIndex;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeleteMethodArgument(methodPattern=" + this.getMethodPattern() + ", argumentIndex=" + this.getArgumentIndex() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeleteMethodArgument)) return false;
        final DeleteMethodArgument other = (DeleteMethodArgument) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getArgumentIndex() != other.getArgumentIndex()) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeleteMethodArgument;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getArgumentIndex();
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        return result;
    }
}
