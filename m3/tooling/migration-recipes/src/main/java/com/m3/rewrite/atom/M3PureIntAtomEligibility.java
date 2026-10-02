// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.HashSet;
import java.util.Set;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Single mechanical eligibility oracle shared by FILE-scope atom discovery and mutation.
 *
 * <p>Keeping discovery and transformation on the same predicate prevents candidate drift: a source
 * leaf cannot be reported as FILE-safe under one rule and transformed under another.
 */
final class M3PureIntAtomEligibility {
    static final String ATOM_NAME = "m3$pureIntAtom";
    static final String PATTERN_ROLE = "PURE_INT_EXPRESSION";

    private M3PureIntAtomEligibility() {}

    static boolean eligible(J.MethodDeclaration candidate) {
        if (!candidate.hasModifier(J.Modifier.Type.Private)
                || !candidate.hasModifier(J.Modifier.Type.Static)
                || candidate.getBody() == null
                || candidate.getBody().getStatements().size() != 1
                || candidate.getReturnTypeExpression() == null
                || candidate.getReturnTypeExpression().getType() != JavaType.Primitive.Int) {
            return false;
        }

        if (!(candidate.getBody().getStatements().getFirst() instanceof J.Return returned)
                || returned.getExpression() == null) {
            return false;
        }

        Set<String> parameters = intParameters(candidate);
        if (parameters == null || parameters.contains(ATOM_NAME)) {
            return false;
        }

        return pureIntNodes(returned.getExpression(), parameters, 0) >= 3;
    }

    static Expression returnedExpression(J.MethodDeclaration candidate) {
        if (!eligible(candidate)) {
            throw new IllegalArgumentException("method is not an admitted pure-int FILE atom");
        }
        J.Return returned = (J.Return) candidate.getBody().getStatements().getFirst();
        return returned.getExpression();
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
