// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainTextParser;

class M3ArraySubstrateRecipeTest {
    private static final String CRATE = "m3-array-substrate-v1";
    private static final String JAVA_ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";

    @Test
    void javaPostimagesAreExactFixedPointAndAbsentCrateCanMaterialize() {
        M3HashPinnedJavaSnapshotRecipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(CRATE);

        InMemoryExecutionContext context =
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var materialize =
                recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        assertEquals(10, materialize.getChangeset().getAllResults().size());

        List<SourceFile> postimages =
                javaPostimages(context);
        var fixed =
                recipe.run(new InMemoryLargeSourceSet(postimages), context, 1);
        assertTrue(fixed.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void textPostimagesAreFixedPointAndDriftFailsClosed() {
        M3Jdk21HashPinnedTextSnapshotRecipe recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); });

        List<SourceFile> postimages = textPostimages(context);
        var fixed =
                recipe.run(new InMemoryLargeSourceSet(postimages), context, 1);
        assertTrue(fixed.getChangeset().getAllResults().isEmpty());

        ArrayList<SourceFile> drifted = new ArrayList<>(postimages);
        SourceFile first = drifted.getFirst();
        drifted.set(
                0,
                org.openrewrite.text.PlainText.builder()
                        .sourcePath(first.getSourcePath())
                        .text(first.printAll() + "\nDRIFT")
                        .build());
        assertThrows(
                IllegalStateException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(drifted), context, 1));
    }

    @Test
    void declarativeAdapterUsesCanonicalSynexiaJavaOwnerAndM3TextOwner() {
        String descriptor = resource("/META-INF/rewrite/m3-array-substrate.yml");
        assertTrue(descriptor.contains("name: com.m3.M3ArraySubstrate"));
        assertTrue(descriptor.contains("com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe"));
        assertTrue(descriptor.contains("com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(descriptor.contains("crateName: " + CRATE));
    }

    private static List<SourceFile> javaPostimages(InMemoryExecutionContext context) {
        TreeMap<String, String> templates = templates(JAVA_ROOT);
        List<Parser.Input> inputs =
                templates.entrySet().stream()
                        .map(entry -> Parser.Input.fromString(
                                Path.of(entry.getKey()), entry.getValue()))
                        .toList();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static List<SourceFile> textPostimages(InMemoryExecutionContext context) {
        TreeMap<String, String> templates = templates(TEXT_ROOT);
        List<Parser.Input> inputs =
                templates.entrySet().stream()
                        .map(entry -> Parser.Input.fromString(
                                Path.of(entry.getKey()), entry.getValue()))
                        .toList();
        return PlainTextParser.builder()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static TreeMap<String, String> templates(String root) {
        TreeMap<String, String> result = new TreeMap<>();
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            result.put(cells[0], resource(root + cells[3]));
        }
        return result;
    }

    private static String resource(String path) {
        try (var stream = M3ArraySubstrateRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
