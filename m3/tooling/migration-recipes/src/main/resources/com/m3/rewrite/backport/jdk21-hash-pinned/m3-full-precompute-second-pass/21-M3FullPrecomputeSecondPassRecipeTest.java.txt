// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3FullPrecomputeSecondPassRecipeTest {
    private static final String CRATE = "m3-full-precompute-second-pass";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";

    @Test
    void declarativeRecipeComposesJavaAndTextCrates() {
        String descriptor = resource("/META-INF/rewrite/m3-full-precompute-second-pass.yml");
        assertTrue(descriptor.contains("name: com.m3.M3FullPrecomputeSecondPass"));
        assertTrue(descriptor.contains("M3Jdk21HashPinnedSnapshotRecipe"));
        assertTrue(descriptor.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(descriptor.contains("crateName: " + CRATE));
    }

    @Test
    void reviewedJavaPostimagesAreARecipeFixedPoint() {
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> files = javaPostimages(context);
        assertFalse(files.isEmpty());
        var run =
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                        .run(new InMemoryLargeSourceSet(files), context, 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void reviewedTextAndNativePostimagesAreARecipeFixedPoint() {
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> files = textPostimages();
        assertFalse(files.isEmpty());
        var run =
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE)
                        .run(new InMemoryLargeSourceSet(files), context, 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    private static List<SourceFile> javaPostimages(InMemoryExecutionContext context) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (String[] cells : manifest(JAVA_ROOT)) {
            inputs.add(
                    Parser.Input.fromString(
                            Path.of(cells[0]), resource(JAVA_ROOT + cells[3])));
        }
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static List<SourceFile> textPostimages() {
        ArrayList<SourceFile> files = new ArrayList<>();
        for (String[] cells : manifest(TEXT_ROOT)) {
            files.add(
                    PlainText.builder()
                            .sourcePath(Path.of(cells[0]))
                            .text(resource(TEXT_ROOT + cells[3]))
                            .build());
        }
        return List.copyOf(files);
    }

    private static List<String[]> manifest(String root) {
        return resource(root + "manifest.tsv")
                .lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1))
                .peek(cells -> assertEquals(4, cells.length))
                .toList();
    }

    private static String resource(String path) {
        try (var stream =
                M3FullPrecomputeSecondPassRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
