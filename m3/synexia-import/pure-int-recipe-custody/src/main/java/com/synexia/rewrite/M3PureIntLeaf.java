// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.HashSet;
import java.util.Set;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Shared semantic oracle for the deliberately tiny M3 pure-int FILE domain.
 *
 * <p>This owner is independent Synexia code. Nebula's EPL-2.0 recipe laboratory is provenance and
 * proving evidence only; its implementation is not copied or relicensed here.</p>
 */
final class M3PureIntLeaf {
    static final String ATOM_NAME = "m3$pureIntAtom";
    static final String PATTERN_ROLE = "PURE_INT_EXPRESSION";
    static final String IOP_MARKER = "M3-IOP: " + PATTERN_ROLE;
    static final String DOC_MARKER =
            "M3-ATOM: " + ATOM_NAME + "; Pattern/IOP: " + PATTERN_ROLE + ".";

    private M3PureIntLeaf() {}

    static boolean eligible(J.MethodDeclaration method) {
        if (!baseMethod(method)
                || method.getBody().getStatements().size() != 1
                || !(method.getBody().getStatements().getFirst() instanceof J.Return returned)
                || returned.getExpression() == null) {
            return false;
        }
        Set<String> parameters = intParameters(method);
        return parameters != null
                && !parameters.contains(ATOM_NAME)
                && nodeCount(returned.getExpression(), parameters, 0) >= 3;
    }

    static boolean atomized(J.MethodDeclaration method) {
        if (!baseMethod(method)
                || method.getBody().getStatements().size() != 2
                || !(method.getBody().getStatements().getFirst()
                        instanceof J.VariableDeclarations declarations)
                || declarations.getTypeExpression() == null
                || declarations.getTypeExpression().getType() != JavaType.Primitive.Int
                || declarations.getVariables().size() != 1
                || !(method.getBody().getStatements().get(1) instanceof J.Return returned)
                || !(returned.getExpression() instanceof J.Identifier returnedIdentifier)) {
            return false;
        }

        J.VariableDeclarations.NamedVariable variable = declarations.getVariables().getFirst();
        if (!ATOM_NAME.equals(variable.getSimpleName())
                || variable.getInitializer() == null
                || !ATOM_NAME.equals(returnedIdentifier.getSimpleName())) {
            return false;
        }

        Set<String> parameters = intParameters(method);
        return parameters != null
                && !parameters.contains(ATOM_NAME)
                && nodeCount(variable.getInitializer(), parameters, 0) >= 3;
    }

    static Expression returnedExpression(J.MethodDeclaration method) {
        if (!eligible(method)) {
            throw new IllegalArgumentException("method is not an admitted pure-int FILE leaf");
        }
        return ((J.Return) method.getBody().getStatements().getFirst()).getExpression();
    }

    static J.VariableDeclarations atomVariable(J.MethodDeclaration method) {
        if (!atomized(method)) {
            throw new IllegalArgumentException("method is not an admitted atomized pure-int FILE leaf");
        }
        return (J.VariableDeclarations) method.getBody().getStatements().getFirst();
    }

    private static boolean baseMethod(J.MethodDeclaration method) {
        return method.hasModifier(J.Modifier.Type.Private)
                && method.hasModifier(J.Modifier.Type.Static)
                && method.getBody() != null
                && method.getReturnTypeExpression() != null
                && method.getReturnTypeExpression().getType() == JavaType.Primitive.Int;
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
            if (!names.add(declarations.getVariables().getFirst().getSimpleName())) {
                return null;
            }
        }
        return names;
    }

    private static int nodeCount(Expression expression, Set<String> parameters, int depth) {
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
            int nestedCount = nodeCount(nested, parameters, depth + 1);
            return nestedCount < 0 ? -1 : nestedCount + 1;
        }
        if (expression instanceof J.Unary unary) {
            if (unary.getOperator() != J.Unary.Type.Positive
                    && unary.getOperator() != J.Unary.Type.Negative
                    && unary.getOperator() != J.Unary.Type.Complement) {
                return -1;
            }
            int nestedCount = nodeCount(unary.getExpression(), parameters, depth + 1);
            return nestedCount < 0 ? -1 : nestedCount + 1;
        }
        if (expression instanceof J.Binary binary) {
            if (!safe(binary.getOperator())) {
                return -1;
            }
            int left = nodeCount(binary.getLeft(), parameters, depth + 1);
            int right = nodeCount(binary.getRight(), parameters, depth + 1);
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
