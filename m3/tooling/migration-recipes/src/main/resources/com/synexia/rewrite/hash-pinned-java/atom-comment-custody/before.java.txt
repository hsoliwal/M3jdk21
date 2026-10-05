// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * File-local M3 atomization for a deliberately tiny proven semantic domain.
 *
 * <p>The recipe rewrites only a private static {@code int} method whose body is one return
 * statement and whose returned expression is composed solely from int parameters, int literals,
 * parentheses, unary + / - / ~, and non-throwing primitive int binary operators. It introduces no
 * member, visibility, dependency, allocation, I/O, synchronization, exception path or API change.
 *
 * <p>The source-only M3-IOP marker patternizes the extracted local leaf without adding a product
 * dependency. Discovery and mutation share {@link M3PureIntAtomEligibility}; more complex methods
 * are left unchanged for a broader-scope or partial-AST pass.
 */
public final class M3AtomizePureIntReturnRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 atomize pure private int return";
    }

    @Override
    public String getDescription() {
        return "Extracts one proven pure primitive-int return expression into a named local M3 "
                + "atom and records its IOP pattern role without changing the member surface.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "atomization",
                "patternization",
                "iop",
                "file-local",
                "behavior-contract-preserving");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate atomize = JavaTemplate.builder(
                            "/* M3-IOP: " + M3PureIntAtomEligibility.PATTERN_ROLE + " */ "
                                    + "int " + M3PureIntAtomEligibility.ATOM_NAME + " = #{any(int)}; "
                                    + "return " + M3PureIntAtomEligibility.ATOM_NAME + ";")
                    .contextSensitive()
                    .build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!M3PureIntAtomEligibility.eligible(candidate)) {
                    return candidate;
                }

                Expression expression = M3PureIntAtomEligibility.returnedExpression(candidate);
                return atomize.apply(
                        updateCursor(candidate),
                        candidate.getCoordinates().replaceBody(),
                        expression);
            }
        };
    }
}
