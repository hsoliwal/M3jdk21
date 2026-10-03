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

final class M3Jdk8316885CodeHeapAnalyticsBackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk22-codeheap-analytics-8316885/pre/";

    private static final String CPP = "src/hotspot/share/code/codeHeapState.cpp";
    private static final String HPP = "src/hotspot/share/code/codeHeapState.hpp";
    private static final String TEST =
            "test/hotspot/jtreg/serviceability/dcmd/compiler/"
                    + "CodeHeapAnalyticsMissingAggregate.java";

    @Test
    void compositeRecipeOwnsThreeIndependentFileAtoms() {
        Recipe recipe = new M3Jdk8316885CodeHeapAnalyticsBackportRecipe();

        assertEquals(
                "1230aed61d286fe9c09f46e2bab626d0e8fe0273",
                M3Jdk8316885CodeHeapAnalyticsBackportRecipe.UPSTREAM_COMMIT);
        assertEquals(3, recipe.getRecipeList().size());

        var cpp =
                assertInstanceOf(
                        M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().get(0));
        var hpp =
                assertInstanceOf(
                        M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().get(1));
        var java =
                assertInstanceOf(
                        M3Jdk21HashPinnedSnapshotRecipe.class,
                        recipe.getRecipeList().get(2));

        assertEquals("jdk22-codeheap-analytics-8316885-cpp", cpp.getCrateName());
        assertEquals("jdk22-codeheap-analytics-8316885-hpp", hpp.getCrateName());
        assertEquals("jdk22-codeheap-analytics-8316885-java", java.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("module-scope"));
        assertTrue(recipe.getTags().contains("dag-composable"));
    }

    @Test
    void exactMasterPreimagesReachDonorDiagnosticAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8316885CodeHeapAnalyticsBackportRecipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(baseline()),
                        context(),
                        1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(3, changes.size());
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        CPP.equals(path(result.getAfter()))
                                                && occurrences(
                                                                result.getAfter().printAll(),
                                                                "print_aggregate_missing(")
                                                        >= 13
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "No aggregated code heap data available. "
                                                                        + "Run function aggregate first.")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        HPP.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "static void print_aggregate_missing("
                                                                        + "outputStream* out, "
                                                                        + "const char* heapName);")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        TEST.equals(path(result.getAfter()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "Compiler.CodeHeap_Analytics aggregate")
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "shouldNotContain(MISSING)")));

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
        SourceFile cpp = sources.get(0);
        sources.set(0, text(CPP, cpp.printAll() + "// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8316885CodeHeapAnalyticsBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                text(CPP, resource("codeHeapState.cpp.before.txt")),
                text(HPP, resource("codeHeapState.hpp.before.txt")));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int offset = 0;
                (offset = text.indexOf(token, offset)) >= 0;
                offset += token.length()) {
            count++;
        }
        return count;
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8316885CodeHeapAnalyticsBackportRecipeTest.class
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
