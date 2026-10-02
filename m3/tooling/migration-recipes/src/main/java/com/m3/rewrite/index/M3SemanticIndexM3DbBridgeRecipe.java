// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDB;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/**
 * Composite OpenRewrite recipe that persists the semantic-index DataTables into M3IndexDB.
 *
 * <p>Source mutation authority remains false. The child recipe only inventories Java sources; this
 * bridge materializes the resulting rows into one compact content-addressed M3IndexDB artifact.
 */
public final class M3SemanticIndexM3DbBridgeRecipe extends Recipe {
    @Option(
            displayName = "M3IndexDB directory",
            description = "Explicit output directory for the external M3IndexDB tool-plane store.",
            example = "build/m3indexdb")
    private final String outputDirectory;

    private final transient M3SemanticIndexRecipe semanticIndex = new M3SemanticIndexRecipe();

    public M3SemanticIndexM3DbBridgeRecipe(String outputDirectory) {
        String checked = Objects.requireNonNull(outputDirectory, "outputDirectory").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("outputDirectory");
        }
        Path path = Path.of(checked).toAbsolutePath().normalize();
        if (path.getParent() == null) {
            throw new IllegalArgumentException("filesystem root is not an M3IndexDB output");
        }
        this.outputDirectory = checked;
    }

    @Override
    public String getDisplayName() {
        return "Persist M3 semantic index into M3IndexDB";
    }

    @Override
    public String getDescription() {
        return "Runs the non-mutating semantic-index recipe and persists its compact node/edge "
                + "payload into the inlined M3IndexDB store.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "m3indexdb",
                "semantic-index",
                "atomization",
                "patternization",
                "simhash",
                "non-mutating");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(semanticIndex);
    }

    @Override
    public void onComplete(ExecutionContext context) {
        var store = DataTableExecutionContextView.view(context).getDataTableStore();
        List<M3SemanticNodeTable.Row> nodes;
        List<M3SemanticEdgeTable.Row> edges;
        try (var nodeRows = store.getRows(M3SemanticNodeTable.class);
                var edgeRows = store.getRows(M3SemanticEdgeTable.class)) {
            nodes = nodeRows.toList();
            edges = edgeRows.toList();
        }

        byte[] payload = M3SemanticIndexPayload.encode(nodes, edges);
        try (M3IndexDB database = M3IndexDB.open(Path.of(outputDirectory))) {
            database.putArtifact("semantic-index", "m3.semantic-index", 1, payload);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot persist semantic index into M3IndexDB", failure);
        }
    }

    public String outputDirectory() {
        return outputDirectory;
    }
}
