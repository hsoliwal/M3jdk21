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

/** Trusted exact source-image, scope-drift and fixed-point JUnit gate before candidate edits. */
final class NebulaM3TransferQualificationRecipeTest {
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/proof-gate/";
    private static final String PREIMAGE =
            RESOURCE + "NebulaM3TransferContractCli.before.java.txt";
    private static final String POSTIMAGE =
            RESOURCE + "NebulaM3TransferContractCli.after.java.txt";

    @Test
    void declarativeMavenRecipeResolvesTheReviewedSingleLeaf() throws Exception {
        var resource =
                getClass().getResource(
                        "/META-INF/rewrite/nebula-m3-transfer-qualification.yml");
        assertNotNull(resource);
        try (var stream = resource.openStream()) {
            var loader =
                    new org.openrewrite.config.YamlResourceLoader(
                            stream, resource.toURI(), new java.util.Properties());
            var environment = org.openrewrite.config.Environment.builder()
                    .load(loader).build();
            var recipe =
                    environment.activateRecipes(
                            "org.eclipse.nebula.m3.TransferQualification");
            assertTrue(recipe.validateAll().stream()
                    .allMatch(org.openrewrite.Validated::isValid));
            assertEquals(1, recipe.getRecipeList().size());
            assertEquals(
                    NebulaM3TransferQualificationRecipe.class,
                    recipe.getRecipeList().getFirst().getClass());
        }
    }

    @Test
    void exactSourcesAndTargetFencesArePinned() throws IOException {
        var recipe = new NebulaM3TransferQualificationRecipe();
        assertEquals(NebulaM3TransferQualificationRecipe.BEFORE, NebulaM3ExactJavaSnapshotRecipe.sha256(read(PREIMAGE)));
        assertEquals(NebulaM3TransferQualificationRecipe.AFTER, NebulaM3ExactJavaSnapshotRecipe.sha256(read(POSTIMAGE)));
        assertTrue(recipe.matches(Path.of(NebulaM3TransferQualificationRecipe.REPOSITORY_PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3TransferQualificationRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("widgets/CTree.java")));
        assertTrue(recipe.getTags().contains("file-local"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void onlyTheFailClosedSidecarIsAdded() throws IOException {
        String before = read(PREIMAGE);
        String after = read(POSTIMAGE);
        String anchor =
                "        Files.writeString(\n"
                        + "                output.resolve(\"transfer.sha256\"),\n";
        String addition =
                "        Files.writeString(\n"
                        + "                output.resolve(\"transfer-qualification.tsv\"),\n"
                        + "                NebulaM3ProofGate.pending(NebulaM3RecipeDagManifest.root()).toTsv(),\n"
                        + "                StandardCharsets.UTF_8);\n";
        assertTrue(before.contains(anchor));
        assertEquals(after, before.replace(anchor, addition + anchor));
        assertTrue(after.contains("NebulaM3ProofGate.pending"));
    }

    @Test
    void actualStructuredJavaRecipeReachesAnUnchangedSecondPass() throws IOException {
        var failures = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(failures::add);
        String before = read(PREIMAGE);
        String after = read(POSTIMAGE);
        SourceFile input = parsed(before, context,
                NebulaM3TransferQualificationRecipe.REPOSITORY_PATH);
        var recipe = new NebulaM3TransferQualificationRecipe();
        List<Result> first =
                recipe.run(new InMemoryLargeSourceSet(List.of(input)), context, 1)
                        .getChangeset().getAllResults();
        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(1, first.size());
        SourceFile output = first.getFirst().getAfter();
        assertNotNull(output);
        assertEquals(after, output.printAll());
        assertEquals(input.getId(), output.getId());
        assertEquals(input.getSourcePath(), output.getSourcePath());
        List<Result> second =
                recipe.run(new InMemoryLargeSourceSet(List.of(output)), context, 1)
                        .getChangeset().getAllResults();
        assertTrue(failures.isEmpty(), failures.toString());
        assertTrue(second.isEmpty());
    }

    @Test
    void staleSourceFailsClosedAndUnrelatedPathIsNotWritable() throws IOException {
        var recipe = new NebulaM3TransferQualificationRecipe();
        for (String image : List.of(read(PREIMAGE), read(POSTIMAGE))) {
            var failures = new ArrayList<Throwable>();
            var context = new InMemoryExecutionContext(failures::add);
            SourceFile drifted = parsed(image + "\n// drift\n", context,
                    NebulaM3TransferQualificationRecipe.REPOSITORY_PATH);
            SourceFile result = (SourceFile) recipe.getVisitor().visit(drifted, context);
            assertSame(drifted, result);
            assertEquals(1, failures.size());
            assertTrue(failures.getFirst().getMessage().contains("preimage drift"));
        }

        var failures = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(failures::add);
        SourceFile offTarget = parsed(read(PREIMAGE), context, "widgets/Other.java");
        assertSame(offTarget, recipe.getVisitor().visit(offTarget, context));
        assertTrue(failures.isEmpty(), failures.toString());
    }

    private static SourceFile parsed(
            String content, InMemoryExecutionContext context, String sourcePath) {
        var units = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(sourcePath), content)),
                        null, context)
                .toList();
        assertEquals(1, units.size());
        return units.getFirst();
    }

    private static String read(String resource) throws IOException {
        try (var stream =
                NebulaM3TransferQualificationRecipeTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("missing recipe image " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
