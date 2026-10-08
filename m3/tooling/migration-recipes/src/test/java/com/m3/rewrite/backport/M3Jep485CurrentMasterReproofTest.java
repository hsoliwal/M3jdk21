// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jep485CurrentMasterReproofTest {
    private static final String TARGET =
            "m3/backports/recipes/j485/REPROOF_REQUEST_20261008.tsv";
    private static final String EXPECTED =
            "key\tvalue\n"
                    + "source_head\t5fb3e5d0836df1d4529a3f5365da3f55e54323da\n"
                    + "purpose\tCURRENT_MASTER_JEP485_REPROOF\n"
                    + "canonical_product_receipt\tm3/backports/recipes/j485/CURRENT_TREE_RECEIPT.tsv\n"
                    + "expected_state\tCANDIDATE_MATERIALIZED_UNVERIFIED\n"
                    + "recipe_dag_required\ttrue\n"
                    + "java21_patched_module_required\ttrue\n"
                    + "openjdk_image_required\ttrue\n"
                    + "gatherer_jtreg_required\ttrue\n"
                    + "stream_regression_required\ttrue\n"
                    + "built_jdk_api_smoke_required\ttrue\n"
                    + "promotion_authority\tfalse\n";

    @Test
    void namedRecipeGeneratesOneEvidenceAtomThenStops() {
        var activated = Environment.builder()
                .scanRuntimeClasspath("com.m3.rewrite")
                .build()
                .activateRecipes("com.m3.jdk21.Jep485CurrentMasterReproof");
        assertEquals(
                "com.m3.jdk21.Jep485CurrentMasterReproof",
                activated.getRecipeList().getFirst().getName());

        var crate =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jep485-current-master-reproof-20261008");
        assertEquals(List.of(TARGET), crate.targetPaths());

        var first = crate.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        var results = first.getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile generated = results.getFirst().getAfter();
        assertEquals(TARGET, generated.getSourcePath().toString().replace('\\', '/'));
        assertEquals(EXPECTED, generated.printAll());
        assertTrue(EXPECTED.contains("promotion_authority\tfalse"));

        assertTrue(crate.run(
                        new InMemoryLargeSourceSet(List.of(generated)), context(), 1)
                .getChangeset()
                .getAllResults()
                .isEmpty());
    }

    @Test
    void driftedReproofEvidenceFailsClosed() {
        var crate =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jep485-current-master-reproof-20261008");
        SourceFile drift = PlainText.builder()
                .sourcePath(Path.of(TARGET))
                .text(EXPECTED + "# drift\n")
                .build();

        assertThrows(
                RuntimeException.class,
                () -> crate.run(
                                new InMemoryLargeSourceSet(List.of(drift)), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
