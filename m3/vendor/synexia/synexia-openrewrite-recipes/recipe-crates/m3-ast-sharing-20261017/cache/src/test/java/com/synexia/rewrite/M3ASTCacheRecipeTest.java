// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Real SDK admission/refusal/fixed-point tests; candidate outputs are the runtime test inputs. */
class M3ASTCacheRecipeTest {
    private static final String CANONICAL = "m3-ast-cache-canonical-20261017";
    private static final String TARGET = "m3-ast-cache-target-20261017";
    private static final String NATIVE = "m3-ast-cache-native-20261017";

    @Test void appliesExactPostimagesAndReachesFixedPoint() throws Exception {
        Map<String, List<SourceFile>> outputs = new HashMap<>();
        for (String crate : List.of(CANONICAL, TARGET)) {
            Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(crate);
            List<SourceFile> first = run(recipe, List.of());
            assertFalse(first.isEmpty());
            assertEquals(new M3HashPinnedJavaSnapshotRecipe(crate).targetPaths().size(), first.size());
            assertTrue(run(recipe, first).isEmpty());
            List<SourceFile> reversed = new ArrayList<>(first);
            Collections.reverse(reversed);
            assertTrue(run(recipe, reversed).isEmpty());
            outputs.put(crate, first);
            writeCandidate(crate, first);
        }
        List<SourceFile> nativeOutput = run(new M3HashPinnedTextSnapshotRecipe(NATIVE), List.of());
        assertEquals(2, nativeOutput.size());
        assertTrue(run(new M3HashPinnedTextSnapshotRecipe(NATIVE), nativeOutput).isEmpty());
        writeCandidate(NATIVE, nativeOutput);
        writeRuntimeInputs(outputs.get(CANONICAL), outputs.get(TARGET));
        if (Boolean.getBoolean("m3.cache.materialize")) {
            applyToExistingRoot(CANONICAL, Path.of(System.getProperty("m3.cache.synexia")), false);
            Path target = Path.of(System.getProperty("m3.cache.target"));
            applyToExistingRoot(TARGET, target, false);
            applyToExistingRoot(NATIVE, target, true);
        }
    }

    @Test void refusesSourceDriftAndDuplicatePaths() {
        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(CANONICAL);
        List<SourceFile> first = run(recipe, List.of());
        SourceFile source = first.getFirst();
        SourceFile drift = JavaParser.fromJavaVersion().build()
                .parse("class Unrelated { String decoy = \"class M3FileSourceFacts {}\"; }")
                .findFirst().orElseThrow().withSourcePath(source.getSourcePath());
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of(drift)));
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of(source, source)));
    }

    @Test void refusesTemplateDrift() throws Exception {
        String manifestName = "/com/synexia/rewrite/hash-pinned-java/" + CANONICAL + "/manifest.tsv";
        Path manifest = Path.of(getClass().getResource(manifestName).toURI());
        String resource = Files.readAllLines(manifest).getFirst().split("\t")[3];
        Path template = manifest.getParent().resolve(resource);
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, new String(original, StandardCharsets.UTF_8) + "\n// drift\n");
            assertThrows(IllegalStateException.class,
                    () -> run(new M3HashPinnedJavaSnapshotRecipe(CANONICAL), List.of()));
        } finally {
            Files.write(template, original);
        }
    }

    @Test void preservesUnrelatedHostileSource() {
        String hostile = "class Preserved { String fake = \"class Fake {} // compiler context\"; "
                + "String regex = \"[a-z]+\"; /* class Hidden {} */ }";
        SourceFile input = JavaParser.fromJavaVersion().build().parse(hostile)
                .findFirst().orElseThrow().withSourcePath(Path.of("unrelated/Preserved.java"));
        List<SourceFile> changes = run(new M3HashPinnedJavaSnapshotRecipe(CANONICAL), List.of(input));
        assertTrue(changes.stream().noneMatch(x -> x.getSourcePath().equals(input.getSourcePath())));
        assertEquals(hostile, input.printAll());
    }

    private static List<SourceFile> run(Recipe recipe, List<SourceFile> before) {
        List<Throwable> failures = new ArrayList<>();
        var context = new InMemoryExecutionContext(failures::add);
        var run = recipe.run(new InMemoryLargeSourceSet(before), context);
        if (!failures.isEmpty()) throw new IllegalStateException("recipe refused candidate", failures.getFirst());
        return run.getChangeset().getAllResults().stream().map(result -> result.getAfter()).toList();
    }

    private static void writeCandidate(String crate, List<SourceFile> sources) throws Exception {
        Path root = Path.of("target/candidate", crate);
        for (SourceFile file : sources) write(root, file);
    }

    private static void writeRuntimeInputs(List<SourceFile> canonical, List<SourceFile> target)
            throws Exception {
        List<SourceFile> sources = new ArrayList<>(canonical);
        sources.addAll(target.stream()
                .filter(x -> x.getSourcePath().toString().startsWith("m3/vendor/synexia/")).toList());
        for (SourceFile file : sources) {
            String path = file.getSourcePath().toString().replace('\\', '/');
            int start = path.indexOf("/src/main/java/");
            if (start >= 0) {
                SourceFile mapped = file.withSourcePath(Path.of(path.substring(start + 15)));
                write(Path.of("target/runtime-sources"), mapped);
            }
        }
    }

    private static void applyToExistingRoot(String crate, Path root, boolean text) throws Exception {
        Recipe recipe = text ? new M3HashPinnedTextSnapshotRecipe(crate)
                : new M3HashPinnedJavaSnapshotRecipe(crate);
        List<String> paths = text ? new M3HashPinnedTextSnapshotRecipe(crate).targetPaths()
                : new M3HashPinnedJavaSnapshotRecipe(crate).targetPaths();
        List<SourceFile> before = new ArrayList<>();
        for (String path : paths) {
            Path file = root.resolve(path);
            if (!Files.exists(file)) continue;
            SourceFile parsed = text
                    ? org.openrewrite.text.PlainTextParser.builder().build()
                            .parse(Files.readString(file)).findFirst().orElseThrow()
                    : JavaParser.fromJavaVersion().build().parse(Files.readString(file))
                            .findFirst().orElseThrow();
            before.add(parsed.withSourcePath(Path.of(path)));
        }
        List<SourceFile> changes = run(recipe, before);
        for (SourceFile file : changes) write(root, file);
        List<SourceFile> after = new ArrayList<>(before);
        after.removeIf(existing -> changes.stream()
                .anyMatch(changed -> changed.getSourcePath().equals(existing.getSourcePath())));
        after.addAll(changes);
        assertTrue(run(recipe, after).isEmpty());
    }

    private static void write(Path root, SourceFile source) throws Exception {
        Path target = root.resolve(source.getSourcePath()).normalize();
        if (!target.startsWith(root.normalize())) throw new IllegalArgumentException("outside source root");
        Files.createDirectories(target.getParent());
        Files.writeString(target, source.printAll(), StandardCharsets.UTF_8);
    }
}
