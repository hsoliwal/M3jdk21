// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import java.util.ArrayList;
import java.util.List;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** Builds identifier-light structural and normalized-logic fingerprints from OpenRewrite Java AST. */
final class M3JavaSemanticHasher {
    private M3JavaSemanticHasher() {}

    static M3IndexDbSemanticFingerprint fingerprint(String domain, J tree) {
        Collector collector = new Collector();
        new JavaIsoVisitor<Collector>() {
            @Override
            public J preVisit(J value, Collector state) {
                state.structure.add(value.getClass().getSimpleName());
                if (value instanceof J.Binary binary) {
                    state.logic.add("BINARY:" + binary.getOperator());
                } else if (value instanceof J.Unary unary) {
                    state.logic.add("UNARY:" + unary.getOperator());
                } else if (value instanceof J.Assignment assignment) {
                    state.logic.add("ASSIGN");
                } else if (value instanceof J.AssignmentOperation assignment) {
                    state.logic.add("ASSIGN_OP:" + assignment.getOperator());
                } else if (value instanceof J.MethodInvocation invocation) {
                    state.logic.add("CALL:" + invocation.getSimpleName() + ":" + invocation.getArguments().size());
                } else if (value instanceof J.NewClass constructor) {
                    state.logic.add("NEW:" + typeToken(constructor.getType()));
                } else if (value instanceof J.NewArray array) {
                    state.logic.add("NEW_ARRAY:" + array.getDimensions().size());
                } else if (value instanceof J.Literal literal) {
                    state.logic.add("LITERAL:" + typeToken(literal.getType()) + ":" + String.valueOf(literal.getValue()));
                } else if (value instanceof J.Identifier identifier) {
                    state.logic.add("IDENT_TYPE:" + typeToken(identifier.getType()));
                } else if (value instanceof J.VariableDeclarations declarations) {
                    state.logic.add("DECLARE:" + typeToken(declarations.getType()));
                } else if (value instanceof J.Return) {
                    state.logic.add("RETURN");
                } else if (value instanceof J.Throw) {
                    state.logic.add("THROW");
                } else if (value instanceof J.If) {
                    state.logic.add("IF");
                } else if (value instanceof J.ForLoop) {
                    state.logic.add("FOR");
                } else if (value instanceof J.ForEachLoop) {
                    state.logic.add("FOREACH");
                } else if (value instanceof J.WhileLoop) {
                    state.logic.add("WHILE");
                } else if (value instanceof J.DoWhileLoop) {
                    state.logic.add("DO_WHILE");
                } else if (value instanceof J.Switch) {
                    state.logic.add("SWITCH");
                } else if (value instanceof J.Ternary) {
                    state.logic.add("TERNARY");
                } else if (value instanceof J.TypeCast cast) {
                    state.logic.add("CAST:" + typeToken(cast.getType()));
                } else if (value instanceof J.InstanceOf instanceOf) {
                    state.logic.add("INSTANCEOF:" + typeToken(instanceOf.getType()));
                } else if (value instanceof J.Synchronized) {
                    state.logic.add("SYNCHRONIZED");
                } else if (value instanceof J.Try) {
                    state.logic.add("TRY");
                }
                return super.preVisit(value, state);
            }
        }.visit(tree, collector);

        @SuppressWarnings("deprecation")
        String exact = tree.printTrimmed();
        return M3IndexDbSemanticFingerprint.leaf(domain, collector.structure, collector.logic, exact);
    }

    private static String typeToken(JavaType type) {
        if (type == null) return "UNKNOWN";
        if (type instanceof JavaType.Primitive primitive) return primitive.name();
        if (type instanceof JavaType.FullyQualified fullyQualified) return fullyQualified.getFullyQualifiedName();
        if (type instanceof JavaType.Array array) return "ARRAY:" + typeToken(array.getElemType());
        return type.getClass().getSimpleName();
    }

    private static final class Collector {
        private final List<String> structure = new ArrayList<>();
        private final List<String> logic = new ArrayList<>();
    }
}
