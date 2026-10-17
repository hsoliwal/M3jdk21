// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

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

/** Real source-sealed OpenRewrite text receipt for the M3JDK21 wrapper proof workflow. */
final class M3Jdk21WrapperTreePinV2RecipeTest {
    private static final String CRATE = "m3jdk21-wrapper-recipe-pin-v2-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    private static final String FILE = ".github/workflows/m3-filtered-sorted-receiver.yml";
    private static M3HashPinnedTextSnapshotRecipe recipe() {
        return new M3HashPinnedTextSnapshotRecipe(CRATE);
    }
    private static String resource(String path) {
        try (var in = M3Jdk21WrapperTreePinV2RecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
    private static SourceFile plain(String text) {
        return PlainText.builder().sourcePath(Path.of(FILE)).text(text).build();
    }
    private static List<? extends SourceFile> changed(List<SourceFile> inputs) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        return recipe().run(new InMemoryLargeSourceSet(inputs), context, 1)
                .getChangeset().getAllResults().stream()
                .map(x -> x.getAfter()).toList();
    }
    @Test void exactOneTargetAndPreimage() {
        var item = recipe();
        assertEquals(List.of(FILE), item.targetPaths());
        assertTrue(item.preimageManifest().get(FILE).matches("[0-9a-f]{64}"));
        assertEquals(1, item.maxCycles());
    }
    @Test void actualOpenRewritePatchAndFixedPoint() {
        String before = resource("pre/m3-filtered-sorted-receiver.yml.txt");
        String after = resource("m3-filtered-sorted-receiver.yml.txt");
        var changed = changed(List.of(plain(before)));
        assertEquals(1, changed.size());
        assertNotNull(changed.getFirst());
        assertEquals(after, changed.getFirst().printAll());
        assertTrue(changed(List.of(plain(after))).isEmpty());
    }
    @Test void unauthorizedDriftAndMissingTargetRefuse() {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var recipe = recipe();
        assertThrows(RuntimeException.class, () ->
                recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1));
        assertThrows(RuntimeException.class, () ->
                recipe.run(new InMemoryLargeSourceSet(List.of(
                        plain(resource("pre/m3-filtered-sorted-receiver.yml.txt")
                                + "\n# tampered\n"))), context, 1));
    }
}
