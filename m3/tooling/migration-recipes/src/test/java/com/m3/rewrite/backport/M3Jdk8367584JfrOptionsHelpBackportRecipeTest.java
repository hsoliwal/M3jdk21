// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jdk8367584JfrOptionsHelpBackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-jfr-options-help-8367584/pre/";

    private static final String DCMD_CPP =
            "src/hotspot/share/jfr/dcmd/jfrDcmds.cpp";
    private static final String DCMD_HPP =
            "src/hotspot/share/jfr/dcmd/jfrDcmds.hpp";
    private static final String OPTION_SET =
            "src/hotspot/share/jfr/recorder/service/jfrOptionSet.cpp";
    private static final String TEST =
            "test/jdk/jdk/jfr/startupargs/TestOptionsHelp.java";

    @Test
    void compositeRecipeOwnsHotspotTextAndJavaTestAtoms() {
        Recipe recipe = new M3Jdk8367584JfrOptionsHelpBackportRecipe();

        assertEquals(
                "39de79eae23410e335d2d1ced8fe3b4d7937a541",
                M3Jdk8367584JfrOptionsHelpBackportRecipe.UPSTREAM_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());

        var textRecipe =
                assertInstanceOf(
                        M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().get(0));
        var javaRecipe =
                assertInstanceOf(
                        M3Jdk21HashPinnedSnapshotRecipe.class,
                        recipe.getRecipeList().get(1));

        assertEquals("jdk27-jfr-options-help-8367584-text", textRecipe.getCrateName());
        assertEquals("jdk27-jfr-options-help-8367584-java", javaRecipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("compatibility-split"));
        assertTrue(recipe.getTags().contains("module-scope"));
    }

    @Test
    void exactJdk21PreimagesReachHelpOnlyPostimageAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8367584JfrOptionsHelpBackportRecipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(baseline()),
                        context(),
                        1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(4, changes.size());
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        DCMD_CPP.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "Syntax : -XX:FlightRecorderOptions:[options]")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        DCMD_HPP.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "static void print_help(outputStream* out, bool startup);")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        OPTION_SET.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "strcmp(FlightRecorderOptions, \"help\") == 0")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        TEST.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "Multiple options are separated")));

        for (Result result : changes) {
            String body = result.getAfter().printAll();
            assertTrue(!body.contains("redact-argument"));
            assertTrue(!body.contains("redact-key"));
            assertTrue(!body.contains("JfrRedactedEvents"));
        }

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleHotspotPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile dcmd = sources.get(0);
        sources.set(0, text(DCMD_CPP, dcmd.printAll() + "// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8367584JfrOptionsHelpBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                text(DCMD_CPP, resource("jfrDcmds.cpp.before.txt")),
                text(DCMD_HPP, resource("jfrDcmds.hpp.before.txt")),
                text(OPTION_SET, resource("jfrOptionSet.cpp.before.txt")));
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8367584JfrOptionsHelpBackportRecipeTest.class
                        .getResourceAsStream(PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
