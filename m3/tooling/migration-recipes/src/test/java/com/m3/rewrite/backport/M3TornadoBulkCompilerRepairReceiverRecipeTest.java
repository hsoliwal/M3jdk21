// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3TornadoBulkCompilerRepairReceiverRecipeTest {
    private static final String CRATE = M3TornadoBulkCompilerRepairReceiverRecipe.CRATE;
    private static final String TARGET = "m3/ports/tornadovm-bulk/pom.xml";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tornado-bulk-compiler-20261014/";

    @Test
    void sourceSealIsSinglePomAndNoPromotion() {
        var owner = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(List.of(TARGET), owner.targetPaths());
        assertFalse(new M3TornadoBulkCompilerRepairReceiverRecipe().promotionAuthority());
        assertEquals("69a7d1f10be7ec90d03fdd12ab1f7948423ebe37482dfd9e7fa3fcbe2af8e78c",
                manifestPreimage(resource("manifest.tsv")));
    }

    @Test
    void preimageConvergesExactlyAndSecondPassIsEmpty() throws Exception {
        String before = resource("before-pom.xml.txt");
        String after = resource("pom.xml.txt");
        assertTrue(after.contains("<version>3.15.0</version>"));
        assertTrue(after.contains("<fork>true</fork>"));
        assertTrue(after.contains("<proc>none</proc>"));
        assertTrue(after.contains("<enablePreview>true</enablePreview>"));
        assertTrue(after.contains("<id>m3-synexia-atom-pattern</id>"));
        assertTrue(after.contains("<id>m3-synexia-atom-pattern-mastery</id>"));

        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var first = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(
                        List.of(PlainText.builder().sourcePath(Path.of(TARGET)).text(before).build())),
                context, 1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        var rewritten = first.getChangeset().getAllResults().getFirst().getAfter();
        assertEquals(after, rewritten.printAll());
        var second = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(List.of(rewritten)), context, 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    private static String manifestPreimage(String row) {
        return row.strip().split("\\t")[1];
    }

    private static String resource(String name) {
        try (InputStream stream =
                M3TornadoBulkCompilerRepairReceiverRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
