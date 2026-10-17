// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler;

import com.synexia.mindex.ast.MIndexAST;
import com.synexia.mindex.ast.MIndexASTEdge;
import com.synexia.mindex.ast.MIndexASTPool;
import com.synexia.mindex.compiler.grammar.MIndexTreeSitterNodeTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compiler-owned programming-language facts projected into the shared MIndexAST precompute IR.
 *
 * <p>Compiler/parser authority is not transferred to this class. javac remains Java authority and
 * Tree-sitter remains the syntax-source authority for imported node-type schemas. Source occurrence
 * hashes/provenance are deliberately separate from canonical structural identity.</p>
 */
public final class MIndexCompilerASTPrecompute {
    private MIndexCompilerASTPrecompute() {}

    public static MIndexAST java21(
            MIndexJava21FormalProfile profile, MIndexASTPool pool) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(pool, "pool");

        ArrayList<MIndexASTEdge> edges = new ArrayList<>(
                profile.keywordCount() + profile.operatorCount() + profile.astKindCount());

        for (int id = 0; id < profile.keywordCount(); id++) {
            MIndexAST keyword = pool.intern(
                    "precompute:language-keyword",
                    Map.of(
                            "id", Integer.toString(id),
                            "spelling", profile.keyword(id).toString()),
                    List.of());
            edges.add(new MIndexASTEdge("keyword:" + id, keyword));
        }

        for (int id = 0; id < profile.operatorCount(); id++) {
            MIndexAST operator = pool.intern(
                    "precompute:language-operator",
                    Map.of(
                            "id", Integer.toString(id),
                            "spelling", profile.operator(id).toString()),
                    List.of());
            edges.add(new MIndexASTEdge("operator:" + id, operator));
        }

        for (int id = 0; id < profile.astKindCount(); id++) {
            MIndexAST kind = pool.intern(
                    "precompute:language-ast-kind",
                    Map.of(
                            "id", Integer.toString(id),
                            "name", profile.astKindName(id).toString()),
                    List.of());
            edges.add(new MIndexASTEdge("ast-kind:" + id, kind));
        }

        return pool.intern(
                "precompute:programming-language-profile",
                Map.of(
                        "id", profile.id().toString(),
                        "version", profile.version().toString(),
                        "source-version", profile.sourceVersion().toString(),
                        "sha256", profile.sha256(),
                        "syntax-authority", profile.syntaxAuthority().toString(),
                        "semantic-authority", profile.semanticAuthority().toString(),
                        "keyword-count", Integer.toString(profile.keywordCount()),
                        "operator-count", Integer.toString(profile.operatorCount()),
                        "ast-kind-count", Integer.toString(profile.astKindCount())),
                edges);
    }

    public static MIndexAST treeSitterNodeTypes(
            MIndexTreeSitterNodeTypes nodeTypes, MIndexASTPool pool) {
        Objects.requireNonNull(nodeTypes, "nodeTypes");
        Objects.requireNonNull(pool, "pool");

        ArrayList<MIndexASTEdge> kinds = new ArrayList<>(nodeTypes.kindCount());
        for (int kind = 0; kind < nodeTypes.kindCount(); kind++) {
            ArrayList<MIndexASTEdge> fields = new ArrayList<>(nodeTypes.fieldCount(kind));
            for (int field = 0; field < nodeTypes.fieldCount(kind); field++) {
                LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
                attrs.put("name", nodeTypes.fieldName(kind, field).toString());
                attrs.put("required", Boolean.toString(nodeTypes.fieldRequired(kind, field)));
                attrs.put("multiple", Boolean.toString(nodeTypes.fieldMultiple(kind, field)));
                attrs.put("allowed-count",
                        Integer.toString(nodeTypes.allowedChildKindCount(kind, field)));
                for (int allowed = 0;
                        allowed < nodeTypes.allowedChildKindCount(kind, field);
                        allowed++) {
                    attrs.put("allowed:" + allowed,
                            nodeTypes.allowedChildKind(kind, field, allowed).toString());
                }
                MIndexAST fieldNode =
                        pool.intern("precompute:tree-sitter-field", attrs, List.of());
                fields.add(new MIndexASTEdge(
                        "field:" + nodeTypes.fieldName(kind, field), fieldNode));
            }

            MIndexAST kindNode = pool.intern(
                    "precompute:tree-sitter-kind",
                    Map.of(
                            "name", nodeTypes.kindName(kind).toString(),
                            "named", Boolean.toString(nodeTypes.named(kind)),
                            "field-count", Integer.toString(nodeTypes.fieldCount(kind))),
                    fields);
            kinds.add(new MIndexASTEdge(
                    "kind:" + nodeTypes.kindName(kind), kindNode));
        }

        return pool.intern(
                "precompute:tree-sitter-node-types",
                Map.of(
                        "language", nodeTypes.languageId().toString(),
                        "revision", nodeTypes.donorRevision().toString(),
                        "kind-count", Integer.toString(nodeTypes.kindCount())),
                kinds);
    }

    /**
     * Exact source provenance is a separate atom so two representation-equivalent node-type
     * documents can share the same canonical schema AST.
     */
    public static MIndexAST treeSitterProvenance(
            MIndexTreeSitterNodeTypes nodeTypes, MIndexASTPool pool) {
        Objects.requireNonNull(nodeTypes, "nodeTypes");
        Objects.requireNonNull(pool, "pool");
        return pool.intern(
                "precompute:source-provenance",
                Map.of(
                        "language", nodeTypes.languageId().toString(),
                        "revision", nodeTypes.donorRevision().toString(),
                        "source-sha256", nodeTypes.sourceSha256()),
                List.of());
    }
}
