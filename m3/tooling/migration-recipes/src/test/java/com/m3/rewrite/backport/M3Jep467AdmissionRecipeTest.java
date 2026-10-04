// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

final class M3Jep467AdmissionRecipeTest {
    private record Leaf(String path, String crate) {}

    private static final List<Leaf> LEAVES = List.of(
            new Leaf("m3/backports/JEP_CATALOGUE.tsv", "jep467-admission-catalogue"),
            new Leaf("m3/backports/BACKPORT_WORK_QUEUE.tsv", "jep467-admission-queue"));

    @Test
    void admissionLeavesReplayAndReachFixedPoint() {
        var recipe = new M3Jep467AdmissionRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(baseline()), context(), 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());
        assertTrue(changes.stream().map(Result::getAfter).map(SourceFile::printAll)
                .anyMatch(text -> text.contains("23\t467\tMarkdown Documentation Comments")));
        assertTrue(changes.stream().map(Result::getAfter).map(SourceFile::printAll)
                .anyMatch(text -> text.contains("JEP-467") && text.contains("LIBRARY_API")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void oneDriftedAdmissionLeafFailsClosed() {
        List<SourceFile> files = new ArrayList<>(baseline());
        SourceFile first = files.getFirst();
        files.set(0, text(first.getSourcePath().toString(), first.printAll() + "# drift\n"));
        assertThrows(RuntimeException.class, () ->
                new M3Jep467AdmissionRecipe()
                        .run(new InMemoryLargeSourceSet(files), context(), 1)
                        .getChangeset().getAllResults());
    }

    private static List<SourceFile> baseline() {
        return LEAVES.stream()
                .map(leaf -> text(leaf.path(), resource(leaf.crate() + "/pre.txt")))
                .toList();
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) {
        String root = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
        try (var input = M3Jep467AdmissionRecipeTest.class.getResourceAsStream(root + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}
