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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** ContractProbe for adding one proof trigger without relaxing the proof itself. */
final class ProofTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/codec-proof/";
    private static final Path PATH = Path.of(".github/workflows/m3-indexdb-boundary-proof.yml");

    @Test
    void exactWorkflowReplaysAndReachesFixedPoint() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("codec-proof");
        var source = text(PATH, resource("before.yml.txt"));
        var unrelated = text(Path.of("keep.txt"), "keep\n");
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(source, unrelated)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile after = results.getFirst().getAfter();
        assertNotNull(after);
        assertEquals(resource("after.yml.txt"), after.printAll());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after, unrelated)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void onlyExactPushAllowlistChanges() {
        assertEquals(resource("before.yml.txt"), resource("after.yml.txt")
                .replace(", 'm3/codec-utf8-20261004-9d9c'", ""));
    }

    @Test
    void missingAndDriftedInputsAreRefused() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("codec-proof");
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(text(PATH, "drift\n"))), context(), 1)
                .getChangeset().getAllResults());
    }

    private static PlainText text(Path path, String source) {
        return PlainText.builder().sourcePath(path).text(source).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String resource(String name) {
        try (var input = ProofTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
