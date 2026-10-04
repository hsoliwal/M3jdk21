// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3ReleaseJepDenominatorRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/release-jep-denominator-v2/pre/";

    private record Existing(String path, String fixture) {}

    private static final List<Existing> EXISTING = List.of(
            new Existing(
                    "m3/backports/JEP_CATALOGUE.tsv",
                    "release-jep-denominator-catalogue.txt"),
            new Existing(
                    "m3/backports/program_status.py",
                    "release-jep-denominator-status-code.txt"),
            new Existing(
                    "m3/backports/test_program_status.py",
                    "release-jep-denominator-status-test.txt"),
            new Existing(
                    "m3/backports/verify.py",
                    "release-jep-denominator-verifier.txt"),
            new Existing(
                    "m3/backports/README.md",
                    "release-jep-denominator-readme.txt"));

    @Test
    void elevenFileAtomsJoinAtFixedPointFromCurrentMaster() {
        var recipe = new M3ReleaseJepDenominatorRecipe();
        assertEquals(85, M3ReleaseJepDenominatorRecipe.JEP_DENOMINATOR);
        assertEquals(11, recipe.getRecipeList().size());
        recipe.getRecipeList().forEach(child ->
                assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, child));

        var first = recipe.run(new InMemoryLargeSourceSet(baseline()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(11, changes.size());
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter().printAll().contains("24\t404\tGenerational Shenandoah")));
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter().printAll().contains("24\t483\tAhead-of-Time Class Loading")));
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter().printAll().contains("25\t521\tGenerational Shenandoah")));
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter().printAll().contains("27\tRELEASED\t523,527,531,532,533,534,536,537,538")));
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter().getSourcePath().toString()
                        .replace('\\', '/')
                        .equals("m3/backports/BACKPORT_PASSES.tsv")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void oneDriftedExistingAtomFailsClosed() {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile first = sources.getFirst();
        sources.set(
                0,
                text(
                        first.getSourcePath().toString(),
                        first.printAll() + "# drift\n"));

        assertThrows(
                RuntimeException.class,
                () -> new M3ReleaseJepDenominatorRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() {
        return EXISTING.stream()
                .map(item -> text(item.path(), resource(item.fixture())))
                .toList();
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) {
        try (var input = M3ReleaseJepDenominatorRecipeTest.class.getResourceAsStream(PRE + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
