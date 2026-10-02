// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.HashSet;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * File-local M3 atomization for a deliberately tiny proven semantic domain.
 *
 * <p>The recipe rewrites only a private static {@code int} method whose body is one return
 * statement and whose returned expression is composed solely from int parameters, int literals,
 * parentheses, unary + / - / ~, and non-throwing primitive int binary operators. It introduces no
 * member, visibility, dependency, allocation, I/O, synchronization, exception path or API change.
 *
 * <p>The source-only M3-IOP marker patternizes the extracted local leaf without adding a product
 * dependency. More complex methods are left unchanged for a broader-scope or partial-AST pass.
 */
public final class M3AtomizePureIntReturnRecipe extends Recipe {
    private static final String ATOM_NAME = "m3$pureIntAtom";

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
                            "/* M3-IOP: PURE_INT_EXPRESSION */ "
                                    + "int " + ATOM_NAME + " = #{any(int)}; "
                                    + "return " + ATOM_NAME + ";")
                    .contextSensitive()
                    .build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!candidate.hasModifier(J.Modifier.Type.Private)
                        || !candidate.hasModifier(J.Modifier.Type.Static)
                        || candidate.getBody() == null
                        || candidate.getBody().getStatements().size() != 1
                        || candidate.getReturnTypeExpression() == null
                        || candidate.getReturnTypeExpression().getType() != JavaType.Primitive.Int) {
                    return candidate;
                }

                if (!(candidate.getBody().getStatements().getFirst() instanceof J.Return returned)
                        || returned.getExpression() == null) {
                    return candidate;
                }

                Set<String> parameters = intParameters(candidate);
                if (parameters == null || parameters.contains(ATOM_NAME)) {
                    return candidate;
                }

                Expression expression = returned.getExpression();
                if (pureIntNodes(expression, parameters, 0) < 3) {
                    return candidate;
                }

                return atomize.apply(
                        updateCursor(candidate),
                        candidate.getCoordinates().replaceBody(),
                        expression);
            }
        };
    }

    private static Set<String> intParameters(J.MethodDeclaration method) {
        Set<String> names = new HashSet<>();
        for (J parameter : method.getParameters()) {
            if (!(parameter instanceof J.VariableDeclarations declarations)
                    || declarations.getTypeExpression() == null
                    || declarations.getTypeExpression().getType() != JavaType.Primitive.Int
                    || declarations.getVariables().size() != 1) {
                return null;
            }
            String name = declarations.getVariables().getFirst().getSimpleName();
            if (!names.add(name)) {
                return null;
            }
        }
        return names;
    }

    private static int pureIntNodes(Expression expression, Set<String> parameters, int depth) {
        if (depth > 128 || expression.getType() != JavaType.Primitive.Int) {
            return -1;
        }

        if (expression instanceof J.Identifier identifier) {
            return parameters.contains(identifier.getSimpleName()) ? 1 : -1;
        }

        if (expression instanceof J.Literal literal) {
            return literal.getValue() instanceof Integer ? 1 : -1;
        }

        if (expression instanceof J.Parentheses<?> parentheses
                && parentheses.getTree() instanceof Expression nested) {
            int nodes = pureIntNodes(nested, parameters, depth + 1);
            return nodes < 0 ? -1 : nodes + 1;
        }

        if (expression instanceof J.Unary unary) {
            if (unary.getOperator() != J.Unary.Type.Positive
                    && unary.getOperator() != J.Unary.Type.Negative
                    && unary.getOperator() != J.Unary.Type.Complement) {
                return -1;
            }
            int nodes = pureIntNodes(unary.getExpression(), parameters, depth + 1);
            return nodes < 0 ? -1 : nodes + 1;
        }

        if (expression instanceof J.Binary binary) {
            if (!safe(binary.getOperator())) {
                return -1;
            }
            int left = pureIntNodes(binary.getLeft(), parameters, depth + 1);
            int right = pureIntNodes(binary.getRight(), parameters, depth + 1);
            if (left < 0 || right < 0 || left + right > 4096) {
                return -1;
            }
            return left + right + 1;
        }

        return -1;
    }

    private static boolean safe(J.Binary.Type operator) {
        return operator == J.Binary.Type.Addition
                || operator == J.Binary.Type.Subtraction
                || operator == J.Binary.Type.Multiplication
                || operator == J.Binary.Type.LeftShift
                || operator == J.Binary.Type.RightShift
                || operator == J.Binary.Type.UnsignedRightShift
                || operator == J.Binary.Type.BitAnd
                || operator == J.Binary.Type.BitOr
                || operator == J.Binary.Type.BitXor;
    }
}
