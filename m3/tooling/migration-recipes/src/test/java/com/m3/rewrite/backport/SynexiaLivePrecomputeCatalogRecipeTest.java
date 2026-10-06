// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class SynexiaLivePrecomputeCatalogRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/synexia-live-precompute-catalog/";

    @Test
    void postimagesAreFixedPointAndDriftRefuses() {
        var recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe("synexia-live-precompute-catalog");
        List<SourceFile> after = after();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("synexia-live-precompute-catalog", recipe.getCrateName());

        List<SourceFile> drift = new ArrayList<>(after);
        PlainText first = (PlainText) drift.getFirst();
        drift.set(0, first.withText(first.getText() + "# drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(drift), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> after() {
        return List.of(
                text(".github/workflows/mindex-string-backing.yml", "00-workflow.yml.txt"),
                text("m3/docs/synexia-live-precompute-catalog.tsv", "01-catalog.tsv.txt"),
                text("m3/runtime-integration/check-synexia-precompute-catalog.py",
                        "02-check.py.txt"));
    }

    private static PlainText text(String path, String resource) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(resource(ROOT + resource))
                .build();
    }

    private static String resource(String name) {
        try (var stream = SynexiaLivePrecomputeCatalogRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read resource: " + name, failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
