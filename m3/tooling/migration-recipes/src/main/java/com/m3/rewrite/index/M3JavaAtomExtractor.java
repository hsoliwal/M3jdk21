// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * FILE-local semantic atom extractor over an already parsed OpenRewrite Java subtree.
 *
 * <p>This is intentionally a recognition layer, not a source mutation. It walks in deterministic
 * source/preorder and emits only nodes with admitted behavioral meaning. Token/identifier leaves
 * can be added by later bounded passes without changing the parent database contract.
 */
final class M3JavaAtomExtractor {
    private M3JavaAtomExtractor() {}

    static List<Atom> atoms(J tree) {
        Objects.requireNonNull(tree, "tree");
        ArrayList<Atom> atoms = new ArrayList<>();
        new JavaIsoVisitor<List<Atom>>() {
            @Override
            public J preVisit(J value, List<Atom> sink) {
                String role = role(value);
                if (role != null) {
                    sink.add(
                            new Atom(
                                    role,
                                    M3JavaSemanticHasher.fingerprint(
                                            "ATOM:" + role,
                                            value)));
                }
                return super.preVisit(value, sink);
            }
        }.visit(tree, atoms);
        return List.copyOf(atoms);
    }

    private static String role(J value) {
        if (value instanceof J.VariableDeclarations) return "DECLARATION";
        if (value instanceof J.Assignment) return "ASSIGNMENT";
        if (value instanceof J.AssignmentOperation) return "ASSIGNMENT_OPERATION";
        if (value instanceof J.Binary) return "BINARY";
        if (value instanceof J.Unary) return "UNARY";
        if (value instanceof J.MethodInvocation) return "CALL";
        if (value instanceof J.NewClass) return "CONSTRUCTOR";
        if (value instanceof J.NewArray) return "ARRAY_CONSTRUCTION";
        if (value instanceof J.Return) return "RETURN";
        if (value instanceof J.Throw) return "THROW";
        if (value instanceof J.If) return "IF";
        if (value instanceof J.ForLoop) return "FOR";
        if (value instanceof J.ForEachLoop) return "FOREACH";
        if (value instanceof J.WhileLoop) return "WHILE";
        if (value instanceof J.DoWhileLoop) return "DO_WHILE";
        if (value instanceof J.Switch) return "SWITCH";
        if (value instanceof J.Ternary) return "TERNARY";
        if (value instanceof J.TypeCast) return "CAST";
        if (value instanceof J.InstanceOf) return "INSTANCEOF";
        if (value instanceof J.Synchronized) return "SYNCHRONIZED";
        if (value instanceof J.Try) return "TRY";
        if (value instanceof J.Literal) return "LITERAL";
        return null;
    }

    record Atom(String role, M3IndexDbSemanticFingerprint fingerprint) {
        Atom {
            role = Objects.requireNonNull(role, "role");
            fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        }
    }
}
