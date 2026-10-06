// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Read-only OpenRewrite inventory for the pinned Synexia -> M3JDK21 Apache delivery manifest. */
public final class M3SynexiaImportInventoryRecipe
        extends ScanningRecipe<M3SynexiaImportInventoryRecipe.Inventory> {
    private static final String MANIFEST = "m3/synexia-import/synexia-seed-export.tsv";
    private transient ImportTable imports = new ImportTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory pinned Synexia Apache imports";
    }

    @Override
    public String getDescription() {
        return "Emits the exact Synexia revision/path/SHA/license import rows consumed by "
                + "M3JDK21 without modifying source or granting promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "synexia",
                "apache-2.0",
                "delivery-target",
                "inventory",
                "content-addressed",
                "non-mutating");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof PlainText text
                        && MANIFEST.equals(
                                text.getSourcePath().normalize().toString().replace('\', '/'))) {
                    if (inventory.rows != null) {
                        throw new IllegalStateException("duplicate Synexia import manifest");
                    }
                    inventory.rows = parse(text.getText());
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    @Override
    public List<? extends SourceFile> generate(Inventory inventory, ExecutionContext context) {
        List<Row> rows = inventory.rows;
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("Synexia import manifest missing");
        }
        rows.stream()
                .sorted(Comparator.comparing(Row::targetPath))
                .forEach(row -> imports.insertRow(context, row));
        return List.of();
    }

    private static List<Row> parse(String text) {
        List<String> lines = text.lines().toList();
        String header =
                "source_revision\ttarget_id\tcategory\tsource_path\ttarget_path\tsha256\tlicense\tmode";
        if (lines.isEmpty() || !header.equals(lines.getFirst())) {
            throw new IllegalArgumentException("invalid Synexia import header");
        }
        ArrayList<Row> rows = new ArrayList<>();
        String revision = null;
        String declaredRoot = null;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            if (line.startsWith("# root\t")) {
                if (declaredRoot != null) throw new IllegalArgumentException("duplicate root");
                declaredRoot = lowerSha(line.substring("# root\t".length()), "root");
                continue;
            }
            if (line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 8) throw new IllegalArgumentException("invalid import row");
            if (revision == null) revision = cells[0];
            if (!revision.equals(cells[0])) throw new IllegalArgumentException("revision drift");
            if (!"m3jdk21".equals(cells[1])) throw new IllegalArgumentException("target drift");
            if (!cells[4].startsWith("m3/vendor/synexia/")) {
                throw new IllegalArgumentException("vendor path escape");
            }
            if (!"Apache-2.0".equals(cells[6])) {
                throw new IllegalArgumentException("non-Apache automatic import");
            }
            if (!Set.of("APACHE_SOURCE", "APACHE_RECIPE_RESOURCE").contains(cells[7])) {
                throw new IllegalArgumentException("invalid import mode");
            }
            rows.add(new Row(
                    commit(cells[0]),
                    cells[2],
                    cells[3],
                    cells[4],
                    lowerSha(cells[5], "sha256"),
                    cells[6],
                    cells[7]));
        }
        if (rows.isEmpty() || declaredRoot == null) {
            throw new IllegalArgumentException("incomplete Synexia import manifest");
        }
        return List.copyOf(rows);
    }

    private static String commit(String value) {
        String checked = value.toLowerCase(java.util.Locale.ROOT);
        if (!(checked.matches("[0-9a-f]{40}") || checked.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("sourceRevision");
        }
        return checked;
    }

    private static String lowerSha(String value, String field) {
        String checked = value.toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    static final class Inventory {
        private List<Row> rows;
    }

    public static final class ImportTable extends DataTable<Row> {
        ImportTable(org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 Synexia Apache imports",
                    "Pinned Synexia source/recipe atoms available to M3JDK21 as verified donor snapshots.");
        }
    }

    public record Row(
            @Column(displayName = "Synexia revision", description = "Pinned Synexia commit.") String sourceRevision,
            @Column(displayName = "Category", description = "Imported capability family.") String category,
            @Column(displayName = "Source path", description = "Original Synexia path.") String sourcePath,
            @Column(displayName = "Target path", description = "M3JDK21 vendor snapshot path.") String targetPath,
            @Column(displayName = "SHA-256", description = "Exact source content identity.") String sha256,
            @Column(displayName = "License", description = "Automatic import license identity.") String license,
            @Column(displayName = "Mode", description = "Apache source/resource import mode.") String mode) {}
}
