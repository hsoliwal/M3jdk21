// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Additive, source-sealed proof for the M3 object slot/index collection substrate. */
final class M3ObjectSlotIndexSnapshot20261017RecipeTest {
    private static final String CRATE = "m3-object-slot-index-snapshot-20261017";
    private static final String STORE =
            "m3/collections/src/main/java/com/m3/collections/M3ObjectSlotStore28.java";

    private static InMemoryExecutionContext ctx() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    @Test
    void exactAdditiveTargetsReachFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertEquals(List.of(
                "m3/collections/src/main/java/com/m3/collections/M3ObjectIndexSnapshot.java",
                STORE,
                "m3/collections/src/test/java/com/m3/collections/M3ObjectSlotIndexSelfTest.java",
                "m3/collections/src/test/java/com/m3/collections/M3ObjectSlotIndexTest.java"),
                recipe.targetPaths());

        var first = recipe.run(
                new InMemoryLargeSourceSet(List.<SourceFile>of()), ctx(), 1);
        assertEquals(4, first.getChangeset().getAllResults().size());

        Map<String, SourceFile> generated = new TreeMap<>();
        for (var result : first.getChangeset().getAllResults()) {
            SourceFile after = result.getAfter();
            assertNotNull(after);
            generated.put(after.getSourcePath().toString().replace('\\', '/'), after);
        }
        assertEquals(4, generated.size());
        assertTrue(generated.get(STORE).printAll().contains("M3ObjectStateSlots28"));
        assertTrue(generated.get(
                "m3/collections/src/main/java/com/m3/collections/M3ObjectIndexSnapshot.java")
                .printAll().contains("M3IndexMaskWindow.scan"));
        assertTrue(generated.get(
                "m3/collections/src/main/java/com/m3/collections/M3ObjectIndexSnapshot.java")
                .printAll().contains("M3SignalOrder.filtered"));

        var second = recipe.run(
                new InMemoryLargeSourceSet(List.copyOf(generated.values())), ctx(), 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void occupiedTargetRefusesInsteadOfOverwriting() {
        String drift = "package com.m3.collections; public final class M3ObjectSlotStore28 {}\n";
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(STORE), drift)), null, ctx()).toList();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(parsed), ctx(), 1));
    }
}
