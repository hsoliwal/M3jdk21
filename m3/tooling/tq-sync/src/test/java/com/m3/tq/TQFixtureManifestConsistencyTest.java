// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.tq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeException;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Real named recipe replay; this does not qualify current product custody or runtime gates. */
final class TQFixtureManifestConsistencyTest {
    private static final String FORWARD = "m3-tq-manifest-consistency-20261019";
    private static final String INVERSE = FORWARD + "-inverse";
    private static final String RESOURCE_BASE =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final String OWNER_ROOT = RESOURCE_BASE + "m3-tq-sync/";
    private static final String TARGET =
            "m3/tooling/tq-sync/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-sync/manifest.tsv";
    private static final String BEFORE_SHA = "415d3a8fe282c705dc387761af853bead2740ad43403487f013fd9d9553a6e2e";
    private static final String AFTER_SHA = "5fede1e09fb5ff23319605a6e986ebfb11ba213685f5a8df980534bd5ba40407";
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final Map<String, String> REVIEWED_AFTER = Map.of(
            "01-m3-tq.yml.txt", "67573f9e38b1ccd595e8c4be07b68f38a4cbe3d44eb81484d6f18d5e476ac60a",
            "11-plan.json.txt", "50d583d5ff9f0975e8d1970eaaf9613c9694ccc0e9664f9c2f92f5e901ba0a39",
            "12-manifest.tsv.txt", "d4748d025cf93a35fb50695d7ca85728a1b9a8b319beed4f55a4d4ca64070595",
            "16-workflow.yml.txt.txt", "67573f9e38b1ccd595e8c4be07b68f38a4cbe3d44eb81484d6f18d5e476ac60a");

    private record Applied(Map<String, String> files, List<Result> changes) {}

    @Test
    void namedForwardAndInverseReplay() throws Exception {
        String before = resource(RESOURCE_BASE + FORWARD + "/tq-manifest.tsv.txt.before");
        String after = resource(RESOURCE_BASE + FORWARD + "/tq-manifest.tsv.txt");
        assertEquals(BEFORE_SHA, sha(before.getBytes(StandardCharsets.UTF_8)));
        assertEquals(AFTER_SHA, sha(after.getBytes(StandardCharsets.UTF_8)));
        assertEquals(before, resource(RESOURCE_BASE + INVERSE + "/tq-manifest.tsv.txt"));
        assertEquals(after, resource(RESOURCE_BASE + INVERSE + "/tq-manifest.tsv.txt.before"));

        Recipe forward = named(false);
        Recipe inverse = named(true);
        SourceFile original = text(TARGET, before);
        Applied applied = apply(forward, List.of(original));
        assertEquals(1, applied.changes().size());
        assertEquals(after, applied.files().get(TARGET));
        assertEquals(before, original.printAll(), "the caller's preimage must survive");
        assertTrue(apply(forward, sources(applied.files())).changes().isEmpty());

        Applied rolledBack = apply(inverse, sources(applied.files()));
        assertEquals(1, rolledBack.changes().size());
        assertEquals(before, rolledBack.files().get(TARGET));
        assertTrue(apply(inverse, sources(rolledBack.files())).changes().isEmpty());
        Applied replayed = apply(forward, sources(rolledBack.files()));
        assertEquals(1, replayed.changes().size());
        assertEquals(applied.files(), replayed.files());

        SourceFile emitted = applied.changes().get(0).getAfter();
        assertNotNull(emitted);
        Path output = CRATE.resolve("target/generated-recipe").resolve(emitted.getSourcePath());
        Files.createDirectories(output.getParent());
        Files.writeString(output, emitted.printAll(), StandardCharsets.UTF_8);
        assertArrayEquals(after.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(output));
    }

    @Test
    void rejectsUnknownMissingAndDuplicate() throws Exception {
        for (boolean inverse : new boolean[] {false, true}) {
            Recipe recipe = named(inverse);
            String crate = inverse ? INVERSE : FORWARD;
            String before = resource(RESOURCE_BASE + crate + "/tq-manifest.tsv.txt.before");
            assertThrows(RuntimeException.class,
                    () -> run(recipe, List.of(text(TARGET, before + "\n# unknown drift\n"))));
            assertThrows(RuntimeException.class, () -> run(recipe, List.of()));
            assertThrows(RuntimeException.class,
                    () -> run(recipe, List.of(text(TARGET, before), text(TARGET, before))));
            assertEquals(before,
                    resource(RESOURCE_BASE + crate + "/tq-manifest.tsv.txt.before"));
        }
    }

    @Test
    void keepsUnrelatedAndMatchesCurrentManifest() throws Exception {
        String before = resource(RESOURCE_BASE + FORWARD + "/tq-manifest.tsv.txt.before");
        String after = resource(RESOURCE_BASE + FORWARD + "/tq-manifest.tsv.txt");
        String unrelated = "retained caller input\n";
        Applied applied = apply(named(false),
                List.of(text("unrelated.txt", unrelated), text(TARGET, before)));
        assertEquals(1, applied.changes().size());
        assertEquals(Set.of(TARGET, "unrelated.txt"), applied.files().keySet());
        assertEquals(unrelated, applied.files().get("unrelated.txt"));
        assertTrue(applied.changes().stream().noneMatch(
                change -> "unrelated.txt".equals(normalized(change.getAfter().getSourcePath()))));
        assertTrue(apply(named(false), sources(applied.files())).changes().isEmpty());
        Applied restored = apply(named(true), sources(applied.files()));
        assertEquals(before, restored.files().get(TARGET));
        assertEquals(unrelated, restored.files().get("unrelated.txt"));

        assertEquals(after, resource(OWNER_ROOT + "manifest.tsv"),
                "the current fixture manifest must be the real recipe postimage");
        assertEquals(after, Files.readString(CRATE.resolve(
                "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
                        + "m3-tq-sync/manifest.tsv"), StandardCharsets.UTF_8));

        List<String[]> oldRows = rows(before);
        List<String[]> newRows = rows(after);
        assertEquals(24, oldRows.size());
        assertEquals(24, newRows.size());
        Set<String> changed = new HashSet<>();
        for (int index = 0; index < 24; index++) {
            String[] oldRow = oldRows.get(index);
            String[] newRow = newRows.get(index);
            assertEquals(4, oldRow.length);
            assertEquals(4, newRow.length);
            assertEquals(oldRow[0], newRow[0], "target and order must survive");
            assertEquals(oldRow[1], newRow[1], "all BEFORE hashes must survive");
            assertEquals(oldRow[3], newRow[3], "resource names must survive");
            if (!oldRow[2].equals(newRow[2])) {
                assertEquals(REVIEWED_AFTER.get(newRow[3]), newRow[2]);
                assertTrue(changed.add(newRow[3]));
            }
            assertEquals(newRow[2], sha(resourceBytes(OWNER_ROOT + newRow[3])),
                    "the existing resource body must supply every AFTER hash");
        }
        assertEquals(REVIEWED_AFTER.keySet(), changed);
    }

    private static Recipe named(boolean inverse) throws IOException {
        String crate = inverse ? INVERSE : FORWARD;
        String descriptorPath = "/META-INF/rewrite/" + crate + ".yml";
        String name = inverse ? "com.m3.TQFixtureManifestConsistencyInverse20261019"
                : "com.m3.TQFixtureManifestConsistency20261019";
        String expected = inverse ? "1d2ec38b81eab59b803dbd5ea4176a6b5f60497b2686c5acc065294a18a110cd"
                : "cd87ac962328cf5dd1bb02745a83ee6272db28e505de6adbeeb1655d57b4c4d3";
        byte[] descriptor = resourceBytes(descriptorPath);
        assertEquals(expected, sha(descriptor), "the explicit descriptor must be exact");
        Environment environment = Environment.builder()
                .load(new YamlResourceLoader(new ByteArrayInputStream(descriptor),
                        URI.create("classpath:" + descriptorPath), new Properties(),
                        M3Jdk21HashPinnedTextSnapshotRecipe.class.getClassLoader()))
                .build();
        Recipe recipe = environment.activateRecipes(name);
        assertTrue(recipe.validate().isValid());
        assertEquals(1, recipe.getRecipeList().size());
        Recipe declarative = recipe.getRecipeList().get(0);
        assertEquals(DeclarativeRecipe.class, declarative.getClass());
        assertEquals(name, declarative.getName());
        assertEquals(1, declarative.getRecipeList().size());
        Recipe concrete = declarative.getRecipeList().get(0);
        assertEquals(M3Jdk21HashPinnedTextSnapshotRecipe.class, concrete.getClass());
        M3Jdk21HashPinnedTextSnapshotRecipe owner =
                (M3Jdk21HashPinnedTextSnapshotRecipe) concrete;
        assertEquals(crate, owner.getCrateName());
        assertEquals(List.of(TARGET), owner.targetPaths());
        assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".missing"));
        return recipe;
    }

    private static List<Result> run(Recipe recipe, List<SourceFile> input) {
        InMemoryExecutionContext context = new InMemoryExecutionContext(
                failure -> { throw new IllegalStateException(failure); });
        return recipe.run(new InMemoryLargeSourceSet(input), context, 1)
                .getChangeset().getAllResults();
    }

    private static Applied apply(Recipe recipe, List<SourceFile> input) {
        List<Result> changes = run(recipe, input);
        Map<String, String> files = new LinkedHashMap<>();
        for (SourceFile file : input) {
            String path = normalized(file.getSourcePath());
            assertFalse(files.containsKey(path));
            files.put(path, file.printAll());
        }
        for (Result change : changes) {
            assertNotNull(change.getBefore());
            assertNotNull(change.getAfter());
            String path = normalized(change.getBefore().getSourcePath());
            assertEquals(path, normalized(change.getAfter().getSourcePath()));
            assertEquals(TARGET, path);
            assertTrue(files.containsKey(path));
            files.put(path, change.getAfter().printAll());
        }
        return new Applied(Map.copyOf(files), List.copyOf(changes));
    }

    private static List<SourceFile> sources(Map<String, String> files) {
        return files.entrySet().stream().map(entry -> text(entry.getKey(), entry.getValue())).toList();
    }

    private static SourceFile text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static List<String[]> rows(String manifest) {
        return manifest.lines().filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String resource(String path) throws IOException {
        return new String(resourceBytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] resourceBytes(String path) throws IOException {
        try (var stream = TQFixtureManifestConsistencyTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }

    private static String sha(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
