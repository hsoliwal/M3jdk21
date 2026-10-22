// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.atom;

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
 * <p>Pattern/IOP marking is deliberately a separate recipe atom so extraction can be verified
 * independently. Discovery and mutation share {@link NebulaM3PureIntAtomEligibility}; more complex methods
 * are left unchanged for a broader-scope or partial-AST pass.
 */
public final class NebulaM3AtomizePureIntReturnRecipe extends Recipe {
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
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate atomize = JavaTemplate.builder(
                            "int " + NebulaM3PureIntAtomEligibility.ATOM_NAME + " = #{any(int)}; "
                                    + "return " + NebulaM3PureIntAtomEligibility.ATOM_NAME + ";")
                    .contextSensitive()
                    .build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!NebulaM3PureIntAtomEligibility.eligible(candidate)) {
                    return candidate;
                }

                Expression expression = NebulaM3PureIntAtomEligibility.returnedExpression(candidate);
                return atomize.apply(
                        updateCursor(candidate),
                        candidate.getCoordinates().replaceBody(),
                        expression);
            }
        };
    }
}
