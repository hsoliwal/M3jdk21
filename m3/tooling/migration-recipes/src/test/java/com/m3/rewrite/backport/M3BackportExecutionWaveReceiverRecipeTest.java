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

final class M3BackportExecutionWaveReceiverRecipeTest {
    private static final String DOC = "m3/backports/BACKPORT_EXECUTION_WAVES.md";
    private static final String PLANNER = "m3/backports/backport_execution_waves.py";
    private static final String POM = "m3/backports/pom.xml";
    private static final String TEST = "m3/backports/test_backport_execution_waves.py";
    private static final String PRE =
            "/com/m3/rewrite/backport/backport-execution-waves/pre/";

    @Test
    void exactPomPreimageMaterializesPlannerAndReachesFixedPoint() throws IOException {
        var wrapper = new M3BackportExecutionWaveReceiverRecipe();
        assertEquals(1, wrapper.maxCycles());
        assertEquals(1, wrapper.getRecipeList().size());

        var receiver =
                (M3Jdk21HashPinnedTextSnapshotRecipe) wrapper.getRecipeList().getFirst();
        assertEquals("jdk21-backport-execution-waves", receiver.getCrateName());
        assertEquals(List.of(DOC, PLANNER, POM, TEST), receiver.targetPaths());

        var first =
                wrapper.run(
                        new InMemoryLargeSourceSet(
                                List.of(file(POM, resource(PRE + "pom.xml.txt")))),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());

        List<SourceFile> after =
                changes.stream()
                        .map(
                                change -> {
                                    assertNotNull(change.getAfter());
                                    return change.getAfter();
                                })
                        .toList();

        assertTrue(
                wrapper.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftAndMissingRequiredPomFailClosed() throws IOException {
        var wrapper = new M3BackportExecutionWaveReceiverRecipe();
        String before = resource(PRE + "pom.xml.txt");

        assertThrows(
                RuntimeException.class,
                () ->
                        wrapper.run(
                                        new InMemoryLargeSourceSet(
                                                List.of(file(POM, before + "\n<!-- drift -->\n"))),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        assertThrows(
                RuntimeException.class,
                () ->
                        wrapper.run(
                                        new InMemoryLargeSourceSet(List.of()),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void receiverIsPlanningOnly() {
        var wrapper = new M3BackportExecutionWaveReceiverRecipe();

        assertTrue(wrapper.getTags().contains("candidate-only"));
        assertTrue(wrapper.getTags().contains("scheduler"));
        assertTrue(wrapper.getDescription().contains("do not edit JDK product source"));
    }

    private static PlainText file(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String resource(String path) throws IOException {
        try (var in = M3BackportExecutionWaveReceiverRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing exact before-fixture: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
