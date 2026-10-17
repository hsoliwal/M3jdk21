// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** M3 recipe-first exact source and fixed-point proof of the two owner atoms. */
final class M3Jdk21GenericPurposeCompositionRecipeTest {
    private static final String CRATE = "m3jdk21-generic-filtered-purpose-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String MAIN = "m3/collections/src/main/java/com/m3/collections/";
    private static final String TEST = "m3/collections/src/test/java/com/m3/collections/";
    private static final String OWNER = MAIN + "M3FilteredMaps.java";
    private static final String SELF = TEST + "M3FilteredWrapperSelfTest.java";
    private static final String JUNIT = TEST + "M3FilteredPurposeCompositionTest.java";

    @Test
    void sourceSealedBeforeAfterAndNoOpSecondPass() {
        InMemoryExecutionContext context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertEquals(List.of(OWNER, JUNIT, SELF), recipe.targetPaths());
        var initial = Map.of(OWNER, resource("pre/M3FilteredMaps.java.txt"));
        var reviewed = Map.of(
                OWNER, resource("M3FilteredMaps.java.txt"),
                SELF, resource("M3FilteredWrapperSelfTest.java.txt"),
                JUNIT, resource("M3FilteredPurposeCompositionTest.java.txt"));
        Map<String, String> generated = new TreeMap<>();
        recipe.run(new InMemoryLargeSourceSet(parse(context, initial)), context, 1)
                .getChangeset().getAllResults().forEach(change -> {
                    assertNotNull(change.getAfter());
                    generated.put(change.getAfter().getSourcePath()
                                    .toString().replace('\\', '/'),
                            change.getAfter().printAll());
                });
        assertEquals(reviewed, generated);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(context, generated)),
                context, 1).getChangeset().getAllResults().isEmpty(),
                "fixed point must not produce another source edit");
    }

    @Test
    void originalJavaOwnerAndAllOutputsHaveExactDigests() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        String[] records = resource("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toArray(String[]::new);
        assertEquals(3, records.length);
        for (String record : records) {
            String[] f = record.split("\t", -1);
            assertEquals(4, f.length);
            assertEquals(f[1], recipe.preimageManifest().get(f[0]));
            assertEquals(f[2], sha(resource(f[3])));
            if (!"ABSENT".equals(f[1])) {
                assertEquals(f[1], sha(resource("pre/" + f[3])));
            }
        }
        assertEquals("ABSENT", recipe.preimageManifest().get(SELF));
        assertEquals("ABSENT", recipe.preimageManifest().get(JUNIT));
    }

    @Test
    void missingDriftedAndOccupiedSourcesRefuseWithoutGenerating() {
        refuse(Map.of(), "required source missing");
        refuse(Map.of(
                OWNER, resource("pre/M3FilteredMaps.java.txt") + "// drift\n"),
                "source drift");
        refuse(Map.of(
                OWNER, resource("pre/M3FilteredMaps.java.txt"),
                SELF, resource("M3FilteredWrapperSelfTest.java.txt") + "// occupied\n"),
                "source drift");
    }

    @Test
    void duplicateOrNonJavaSourceRefuses() {
        InMemoryExecutionContext context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var original = parse(context, Map.of(
                OWNER, resource("pre/M3FilteredMaps.java.txt"))).getFirst();
        var duplicated = recipe.getInitialValue(context);
        recipe.getScanner(duplicated).visit(original, context);
        recipe.getScanner(duplicated).visit(original, context);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(duplicated, context)).getMessage()
                .contains("duplicate target"));
        var plain = recipe.getInitialValue(context);
        recipe.getScanner(plain).visit(PlainText.builder()
                .sourcePath(Path.of(OWNER))
                .text(resource("pre/M3FilteredMaps.java.txt")).build(), context);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(plain, context)).getMessage()
                .contains("not a Java compilation unit"));
    }

    @Test
    void unrelatedCompilationUnitIsNotRewritten() {
        InMemoryExecutionContext context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var sources = parse(context, Map.of(
                OWNER, resource("pre/M3FilteredMaps.java.txt"),
                "src/main/java/Unrelated.java", "public final class Unrelated {}\n"));
        List<String> emitted = recipe.run(new InMemoryLargeSourceSet(sources),
                context, 1).getChangeset().getAllResults().stream()
                .map(result -> result.getAfter().getSourcePath()
                        .toString().replace('\\', '/'))
                .toList();
        assertEquals(3, emitted.size());
        assertTrue(emitted.stream().noneMatch(x -> x.endsWith("Unrelated.java")));
    }

    @Test
    void namedMavenRecipeResolvesIncumbentClassBackedEngine() {
        var activated = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(
                        "com.synexia.rewrite.M3Jdk21GenericPurposeMapComposition20261017");
        assertEquals(1, activated.getRecipeList().size());
        var named = activated.getRecipeList().getFirst();
        assertEquals("com.synexia.rewrite.M3Jdk21GenericPurposeMapComposition20261017", named.getName());
        assertEquals(1, named.getRecipeList().size());
        assertTrue(named.getRecipeList().stream()
                .anyMatch(candidate -> candidate instanceof M3HashPinnedJavaSnapshotRecipe));
    }

    private static void refuse(Map<String, String> sources, String expected) {
        InMemoryExecutionContext context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var scanned = recipe.getInitialValue(context);
        for (SourceFile file : parse(context, sources)) {
            recipe.getScanner(scanned).visit(file, context);
        }
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(scanned, context)).getMessage().contains(expected));
    }

    private static List<SourceFile> parse(
            InMemoryExecutionContext context, Map<String, String> values) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                new TreeMap<>(values).entrySet().stream()
                        .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue()))
                        .toList(), null, context).toList();
    }

    private static String resource(String name) {
        try (var in = M3Jdk21GenericPurposeCompositionRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String sha(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}
