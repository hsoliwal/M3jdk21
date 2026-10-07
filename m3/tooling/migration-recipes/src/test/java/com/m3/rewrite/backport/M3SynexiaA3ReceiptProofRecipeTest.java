// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3SynexiaA3ReceiptProofRecipeTest {
    private static final String CRATE = "synexia-a3-receipt-id-proof";
    private static final String PATH =
            "m3/tooling/a3/src/test/java/com/m3/a3/A3ApplyTest.java";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";

    @Test
    void exactA3TestRepairUsesJavaTreeAndReachesFixedPoint() throws Exception {
        SourceFile input = parse(resource("before.java.txt"));
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        var changes = recipe.run(
                        new InMemoryLargeSourceSet(List.of(input)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile output = changes.getFirst().getAfter();
        assertNotNull(output);
        assertEquals(resource("after.java.txt"), output.printAll());
        assertTrue(output.printAll().contains(
                "com.synexia.rewrite.atom.M3PureIntConvergenceRecipe.class.getName()"));
        assertTrue(!output.printAll().contains(
                "com.m3.rewrite.M3Java21Convergence\", receipt.recipe()"));
        assertTrue(recipe.run(
                        new InMemoryLargeSourceSet(List.of(output)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void namedRecipeResolvesAndDriftFailsClosed() throws Exception {
        var named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.SynexiaA3ReceiptProof");
        assertEquals(1, named.getRecipeList().size());

        SourceFile drifted = parse(resource("before.java.txt") + "\n// drift\n");
        assertThrows(RuntimeException.class, () -> named.run(
                        new InMemoryLargeSourceSet(List.of(drifted)), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void synexiaPacketCannotTargetOpenJdkOrCanonicalRecipeImplementation() {
        assertTrue(M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(PATH));
        assertTrue(!M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                "src/java.base/share/classes/java/lang/String.java"));
        assertTrue(!M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                "test/jdk/java/lang/String/Test.java"));
        assertTrue(!M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(
                ".m3/openrewrite-recipes/src/main/java/com/synexia/rewrite/"
                        + "M3HashPinnedJavaSnapshotRecipe.java"));
        assertThrows(
                IllegalStateException.class,
                () -> new M3Jdk21HashPinnedSnapshotRecipe(
                                "synexia-illegal-product-test")
                        .getInitialValue(context()));
    }

    private static SourceFile parse(String source) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(PATH), source)),
                        null,
                        context())
                .toList();
        assertEquals(1, parsed.size());
        assertEquals(source, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String resource(String name) throws IOException {
        try (var input = M3SynexiaA3ReceiptProofRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
