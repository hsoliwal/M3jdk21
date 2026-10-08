// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3PureIntCustodyTest {
    private static final Map<String, String> BLOBS =
            Map.of(
                    "M3PureIntAtomizeRecipe.java",
                    "15c6435b00d984cbc2a2308ecb7fc168ed74b80f",
                    "M3PureIntConvergenceRecipe.java",
                    "c2a7a3c93aa278d234c2ccfe59c0e1eb78161b95",
                    "M3PureIntDocumentationRecipe.java",
                    "cc3265ba0a6ea43d198618b0997f24b9f6fa7d1a",
                    "M3PureIntInventoryRecipe.java",
                    "59499e81f01b466256b1c68b85a053a2b569c76a",
                    "M3PureIntLeaf.java",
                    "e1f7fc04156b745f5d312ec2647fdb65f424f386",
                    "M3PureIntPatternizeRecipe.java",
                    "fd5882aa092629c3a389e754aafb1e0b1bc5f6d8");

    @Test
    void custodySourcesRemainExactSynexiaGitBlobs() throws Exception {
        Path root =
                Path.of(System.getProperty("basedir", "."))
                        .toAbsolutePath()
                        .normalize()
                        .resolve("src/main/java/com/synexia/rewrite");
        for (var entry : BLOBS.entrySet()) {
            byte[] bytes = Files.readAllBytes(root.resolve(entry.getKey()));
            assertEquals(entry.getValue(), gitBlob(bytes), entry.getKey());
        }
    }

    @Test
    void convergenceRecipeKeepsSmallOrderedAtoms() {
        Recipe recipe = new M3PureIntConvergenceRecipe();

        assertEquals(1, recipe.maxCycles());
        assertEquals(
                List.of(
                        M3PureIntInventoryRecipe.class,
                        M3PureIntAtomizeRecipe.class,
                        M3PureIntPatternizeRecipe.class,
                        M3PureIntDocumentationRecipe.class),
                recipe.getRecipeList().stream().map(Object::getClass).toList());
    }

    @Test
    void admittedLeafTransformsAndSecondPassIsFixedPoint() {
        String path = "src/main/java/example/Sample.java";
        String source =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a << 3) ^ b;
                    }
                }
                """;

        Map<String, String> first =
                apply(new M3PureIntConvergenceRecipe(), Map.of(path, source));

        assertEquals(1, first.size());
        String transformed = first.get(path);
        assertTrue(transformed.contains("int m3$pureIntAtom ="));
        assertTrue(transformed.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(transformed.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(
                apply(new M3PureIntConvergenceRecipe(), first).isEmpty(),
                "second recipe application must be unchanged");
    }

    @Test
    void broaderOrUnsafeShapesRemainUnchanged() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put(
                "src/main/java/example/PublicMethod.java",
                """
                package example;
                final class PublicMethod {
                    public static int compute(int a, int b) { return a + b; }
                }
                """);
        sources.put(
                "src/main/java/example/Division.java",
                """
                package example;
                final class Division {
                    private static int compute(int a, int b) { return a / b; }
                }
                """);
        sources.put(
                "src/main/java/example/Invocation.java",
                """
                package example;
                final class Invocation {
                    private static int id(int a) { return a; }
                    private static int compute(int a) { return id(a) + 1; }
                }
                """);

        assertTrue(apply(new M3PureIntConvergenceRecipe(), sources).isEmpty());
    }

    private static Map<String, String> apply(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        List<Parser.Input> inputs =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        Parser.Input.fromString(
                                                Path.of(entry.getKey()),
                                                entry.getValue()))
                        .toList();
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context)
                        .toList();

        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1);
        Map<String, String> after = new LinkedHashMap<>();
        run.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile file = result.getAfter();
                            if (file == null) {
                                throw new AssertionError("unexpected deletion");
                            }
                            after.put(
                                    file.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    file.printAll());
                        });
        return after;
    }

    private static String gitBlob(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(
                ("blob " + bytes.length + "\0")
                        .getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }
}
