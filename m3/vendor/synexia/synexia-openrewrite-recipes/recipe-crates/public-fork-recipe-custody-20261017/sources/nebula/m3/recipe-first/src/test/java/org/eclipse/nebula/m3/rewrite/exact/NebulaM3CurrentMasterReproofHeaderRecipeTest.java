// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/**
 * Exact preimage, structured Java replay, fixed-point and source-fence proof.
 *
 * <p>Tests the bounded recipe before a product-tree application; the existing Nebula
 * CurrentMasterReproofTest is the independent, real ledger contract oracle after application.</p>
 */
final class NebulaM3CurrentMasterReproofHeaderRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/reproof-header/";
    private static final String BEFORE =
            ROOT + "NebulaM3CurrentMasterReproofTest.before.java.txt";
    private static final String AFTER =
            ROOT + "NebulaM3CurrentMasterReproofTest.after.java.txt";
    private static final String OLD_COLUMN = "schema\\tmaster_preimage\\tcanonical_repository";
    private static final String NEW_COLUMN =
            "schema\\tnebula_master_preimage\\tcanonical_repository";

    @Test
    void yamlEntryPointResolvesTheOneReviewedOpenRewriteLeaf() throws Exception {
        String resource = "/META-INF/rewrite/nebula-m3-current-master-reproof-header.yml";
        var url = getClass().getResource(resource);
        assertNotNull(url, "registered declarative OpenRewrite recipe");
        try (var input = url.openStream()) {
            var loader = new org.openrewrite.config.YamlResourceLoader(
                    input, url.toURI(), new java.util.Properties());
            var environment = org.openrewrite.config.Environment.builder()
                    .load(loader).build();
            var composite =
                    environment.activateRecipes("org.eclipse.nebula.m3.CurrentMasterReproofHeader");
            assertTrue(composite.validateAll().stream()
                    .allMatch(org.openrewrite.Validated::isValid));
            assertEquals(1, composite.getRecipeList().size());
            assertEquals(
                    NebulaM3CurrentMasterReproofHeaderRecipe.class,
                    composite.getRecipeList().getFirst().getClass());
        }
    }

    @Test
    void sourceHashesAndExactJavaPathsArePinned() throws IOException {
        var recipe = new NebulaM3CurrentMasterReproofHeaderRecipe();
        assertEquals(NebulaM3CurrentMasterReproofHeaderRecipe.BEFORE, NebulaM3ExactJavaSnapshotRecipe.sha256(resource(BEFORE)));
        assertEquals(NebulaM3CurrentMasterReproofHeaderRecipe.AFTER, NebulaM3ExactJavaSnapshotRecipe.sha256(resource(AFTER)));
        assertTrue(recipe.matches(Path.of(NebulaM3CurrentMasterReproofHeaderRecipe.REPOSITORY_PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3CurrentMasterReproofHeaderRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("widgets/Other.java")));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    @Test
    void exactlyOneHeaderLiteralChangesAndLedgerDataIdentityIsPreserved() throws IOException {
        String before = resource(BEFORE);
        String after = resource(AFTER);

        assertEquals(1, occurrences(before, OLD_COLUMN));
        assertEquals(0, occurrences(after, OLD_COLUMN));
        assertEquals(1, occurrences(after, NEW_COLUMN));
        assertEquals(after, before.replace(OLD_COLUMN, NEW_COLUMN));
        assertTrue(after.contains("CURRENT_NEBULA_REPROOF_20261013"));
        assertTrue(after.contains("assertEquals(4, lines.size())"));
    }

    @Test
    void openRewriteReplaysExactJavaLSTAndSecondExecutionIsFixedPoint() throws IOException {
        String before = resource(BEFORE);
        String after = resource(AFTER);
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile input = source(before, context,
                NebulaM3CurrentMasterReproofHeaderRecipe.REPOSITORY_PATH);
        assertTrue(errors.isEmpty(), errors.toString());

        var recipe = new NebulaM3CurrentMasterReproofHeaderRecipe();
        List<Result> first =
                recipe.run(new InMemoryLargeSourceSet(List.of(input)), context, 1)
                        .getChangeset()
                        .getAllResults();

        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, first.size());
        SourceFile generated = first.getFirst().getAfter();
        assertNotNull(generated);
        assertEquals(after, generated.printAll());
        assertEquals(input.getId(), generated.getId());
        assertEquals(input.getSourcePath(), generated.getSourcePath());

        List<Result> second =
                recipe.run(new InMemoryLargeSourceSet(List.of(generated)), context, 1)
                        .getChangeset()
                        .getAllResults();
        assertTrue(second.isEmpty());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void stalePreimageIsRejectedWithoutModifyingSource() throws IOException {
        for (String inputText : List.of(resource(BEFORE), resource(AFTER))) {
            var errors = new ArrayList<Throwable>();
            var context = new InMemoryExecutionContext(errors::add);
            SourceFile source = source(inputText + "\n// drift\n", context,
                    NebulaM3CurrentMasterReproofHeaderRecipe.REPOSITORY_PATH);
            SourceFile candidate = (SourceFile) new NebulaM3CurrentMasterReproofHeaderRecipe()
                    .getVisitor().visit(source, context);
            assertSame(source, candidate);
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("preimage drift"));
        }
    }

    @Test
    void exactContentAtUnrelatedPathHasNoMutationAuthority() throws IOException {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile source = source(resource(BEFORE), context, "other/Unrelated.java");
        SourceFile candidate = (SourceFile) new NebulaM3CurrentMasterReproofHeaderRecipe()
                .getVisitor().visit(source, context);
        assertSame(source, candidate);
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private static SourceFile source(
            String text, InMemoryExecutionContext context, String path) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), text)), null, context)
                .toList();
        assertEquals(1, parsed.size(), "expected one structured Java unit");
        return parsed.getFirst();
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String resource(String path) throws IOException {
        try (var input =
                NebulaM3CurrentMasterReproofHeaderRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing sealed test fixture " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
