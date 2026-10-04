// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticIndex;
import com.m3.indexdb.M3IndexDbSemanticKind;
import com.m3.indexdb.M3IndexDbSemanticNode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/**
 * Generates the complete M3 semantic hash graph as OpenRewrite output.
 *
 * <p>The FILE recipe generates atoms/nodes/edges independently. The MODULE scanner adds admitted
 * type relations. At fan-in M3IndexDB canonicalizes child order and recomputes every parent
 * fingerprint from ATOM through REPOSITORY. This recipe emits those canonical hashes but never
 * mutates source or writes a database.
 */
public final class M3WholeSemanticHashRecipe extends Recipe {
    @Option(
            displayName = "Repository semantic namespace",
            description = "Stable repository identity used in semantic keys.",
            example = "hsoliwal/M3jdk21",
            required = false)
    private final String repository;

    @Option(
            displayName = "Project semantic namespace",
            description = "Stable project identity used in semantic keys.",
            example = "M3JDK21",
            required = false)
    private final String project;

    @Option(
            displayName = "Library semantic namespace",
            description = "Stable library identity used in semantic keys.",
            example = "M3JDK21",
            required = false)
    private final String library;

    private final transient M3SemanticIndexRecipe semanticIndex;
    private final transient M3TypeRelationRecipe typeRelations;
    private transient M3SemanticHashTable hashes = new M3SemanticHashTable(this);
    private transient M3SemanticRootTable roots = new M3SemanticRootTable(this);

    public M3WholeSemanticHashRecipe() {
        this("hsoliwal/M3jdk21", "M3JDK21", "M3JDK21");
    }

    public M3WholeSemanticHashRecipe(
            String repository,
            String project,
            String library) {
        this.repository = token(repository, "repository");
        this.project = token(project, "project");
        this.library = token(library, "library");
        this.semanticIndex = new M3SemanticIndexRecipe(this.repository, this.project, this.library);
        this.typeRelations = new M3TypeRelationRecipe(this.repository);
    }

    @Override
    public String getDisplayName() {
        return "Generate whole M3 semantic hash";
    }

    @Override
    public String getDescription() {
        return "Generates canonical exact, structural, normalized logic and SimHash identities "
                + "from ATOM through REPOSITORY as non-mutating OpenRewrite DataTables.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "semantic-hash",
                "whole-repository",
                "atomization",
                "patternization",
                "iop",
                "structural-hash",
                "logic-hash",
                "simhash",
                "multi-pass",
                "non-mutating");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(semanticIndex, typeRelations);
    }

    @Override
    public void onComplete(ExecutionContext context) {
        var store = DataTableExecutionContextView.view(context).getDataTableStore();
        List<M3SemanticNodeTable.Row> nodeRows;
        List<M3SemanticEdgeTable.Row> edgeRows;
        try (var nodes = store.getRows(M3SemanticNodeTable.class);
                var edges = store.getRows(M3SemanticEdgeTable.class)) {
            nodeRows = nodes.toList();
            edgeRows = edges.toList();
        }

        M3IndexDbSemanticIndex index = M3SemanticIndexPayload.toIndex(nodeRows, edgeRows);
        List<M3IndexDbSemanticNode> canonical = index.nodes().stream()
                .sorted(Comparator
                        .comparing((M3IndexDbSemanticNode node) -> node.kind().ordinal())
                        .thenComparing(M3IndexDbSemanticNode::semanticKey))
                .toList();

        for (M3IndexDbSemanticNode node : canonical) {
            hashes.insertRow(
                    context,
                    new M3SemanticHashTable.Row(
                            node,
                            index.children(node.nodeId()).size(),
                            index.parents(node.nodeId()).size()));
            if (rootKind(node.kind())) {
                roots.insertRow(context, new M3SemanticRootTable.Row(node));
            }
        }
    }

    public String repository() {
        return repository;
    }

    public String project() {
        return project;
    }

    public String library() {
        return library;
    }

    private static boolean rootKind(M3IndexDbSemanticKind kind) {
        return kind == M3IndexDbSemanticKind.FILE
                || kind == M3IndexDbSemanticKind.PACKAGE
                || kind == M3IndexDbSemanticKind.MODULE
                || kind == M3IndexDbSemanticKind.LIBRARY
                || kind == M3IndexDbSemanticKind.PROJECT
                || kind == M3IndexDbSemanticKind.REPOSITORY;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
