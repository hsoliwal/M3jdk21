// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;

/** Exact historical custody recovery and current retention integration, without runtime promotion. */
final class JccMergeRecoveryTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final String PREFIX = "jcc-merge-recovery-20261006";
    private static final List<String> CRATES = List.of(PREFIX + "-history-01",
            PREFIX + "-history-02", PREFIX + "-retention");
    private static final List<Integer> COUNTS = List.of(256, 187, 6);
    private static final List<String> MANIFEST_HASHES = List.of(
            "858bd7ce63f6904757507e6417df700fd7d0f319d421401cd48403d142c3418a",
            "6a6b11543829324bcafa49e72a764c24fb44e398c6bcb7a76c34dcab1d094896",
            "a8fed905d1de5d2e90a1912fc15bd9064c23a389475cd69b63ebe167cbd4fc1c");
    private static final ObjectMapper JSON = new ObjectMapper();
    private record Target(String crate, String path, String before, String after,
                          String beforeHash, String afterHash) {}

    @Test
    void exactHistoricalBlobsAndAllSixCurrentImagesMatchTheirIndependentPins() throws IOException {
        var all = targets();
        assertEquals(449, all.size());
        var paths = new HashSet<String>();
        for (Target target : all) assertTrue(paths.add(target.path()));
        String index = resource("/com/m3/rewrite/backport/jcc-merge-recovery-20261006/historical-files.tsv");
        var rows = index.lines().toList();
        assertEquals("path\tbytes\tsha256\tgit_blob_sha1\tsource_commit", rows.get(0));
        assertEquals(444, rows.size());
        Map<String, Target> byPath = new TreeMap<>();
        for (Target target : all) byPath.put(target.path(), target);
        long bytes = 0;
        for (String line : rows.subList(1, rows.size())) {
            String[] cells = line.split("\t", -1);
            assertEquals(5, cells.length);
            assertEquals("0483f79ae51dd578c2e4fec1b8431bd51b59b469", cells[4]);
            Target target = byPath.get(cells[0]);
            assertNotNull(target);
            assertNull(target.before());
            byte[] body = resource(ROOT + target.crate() + "/" + target.after()).getBytes(StandardCharsets.UTF_8);
            assertEquals(Long.parseLong(cells[1]), body.length);
            assertEquals(cells[2], target.afterHash());
            assertEquals(cells[3], gitBlob(body));
            bytes += body.length;
        }
        assertEquals(29_889_572L, bytes);
    }

    @Test
    void existingRecipeProduces449ExactOutputsAndFreshReplayMakesNoChanges() throws IOException {
        var before = beforeSources();
        var metadata = new BuildMetadata(UUID.fromString("0e901ac6-6bb1-4474-b764-432370c00ce1"),
                Map.of("jcc-recovery", "preserved-input-marker"));
        before.replaceAll(source -> source.withMarkers(source.getMarkers().add(metadata)));
        SourceFile sentinel = text("unrelated-preservation.txt", "retain unrelated bytes\n");
        before.add(sentinel);
        var workspace = new TreeMap<String, SourceFile>();
        for (SourceFile source : before) workspace.put(normalized(source), source);
        var actual = new TreeMap<String, String>();
        for (int i = 0; i < CRATES.size(); i++) {
            var errors = new ArrayList<Throwable>();
            var run = recipe(CRATES.get(i)).run(new InMemoryLargeSourceSet(new ArrayList<>(workspace.values())),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            var changes = run.getChangeset().getAllResults();
            assertEquals(COUNTS.get(i).intValue(), changes.size());
            for (var change : changes) {
                SourceFile next = assertInstanceOf(PlainText.class, change.getAfter());
                String path = normalized(next);
                if (i < 2) {
                    assertNull(change.getBefore());
                    assertFalse(workspace.containsKey(path));
                } else {
                    SourceFile old = assertInstanceOf(PlainText.class, change.getBefore());
                    assertEquals(old.getId(), next.getId());
                    assertEquals(old.getSourcePath(), next.getSourcePath());
                    assertEquals(old.getMarkers(), next.getMarkers().removeByType(RecipesThatMadeChanges.class));
                    assertEquals(old.getFileAttributes(), next.getFileAttributes());
                    assertEquals(old.getCharset(), next.getCharset());
                    assertEquals(old.isCharsetBomMarked(), next.isCharsetBomMarked());
                }
                var provenance = next.getMarkers().findAll(RecipesThatMadeChanges.class);
                assertEquals(1, provenance.size());
                assertEquals(1, provenance.get(0).getRecipes().size());
                var appliedStack = provenance.get(0).getRecipes().iterator().next();
                assertEquals(1, appliedStack.size());
                var applied = assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, appliedStack.get(0));
                assertEquals(CRATES.get(i), applied.getCrateName());
                assertNull(actual.put(path, next.printAll()));
                workspace.put(path, next);
            }
        }
        assertEquals(expectedAfter(), actual);
        for (String crate : CRATES) {
            var errors = new ArrayList<Throwable>();
            var run = recipe(crate).run(new InMemoryLargeSourceSet(new ArrayList<>(workspace.values())),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        }
        assertEquals("retain unrelated bytes\n", workspace.get("unrelated-preservation.txt").printAll());
        String directory = System.getProperty("m3.recovery.materialized");
        if (directory != null && !directory.isBlank()) {
            for (var item : actual.entrySet()) {
                Path path = Path.of(directory).resolve(item.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, item.getValue(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        }
    }

    @Test
    void namedMavenCompositionAndSerializedOwnerRetainTheSameRecipes() throws IOException {
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.JccMergeRecovery");
        var errors = new ArrayList<Throwable>();
        var run = named.run(new InMemoryLargeSourceSet(beforeSources()),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        var actual = new TreeMap<String, String>();
        for (var change : run.getChangeset().getAllResults()) {
            SourceFile after = assertInstanceOf(PlainText.class, change.getAfter());
            assertNull(actual.put(normalized(after), after.printAll()));
        }
        assertEquals(expectedAfter(), actual);
        RecipeSerializer serializer = new RecipeSerializer();
        for (String crate : CRATES) {
            var restored = assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class,
                    serializer.read(serializer.write(recipe(crate))));
            assertEquals(crate, restored.getCrateName());
        }
    }

    @Test
    void everyCurrentMissingOrDriftedInputRefusesWithoutPartialUpdates() throws IOException {
        String crate = CRATES.get(2);
        for (Target target : targets()) {
            if (target.before() == null) continue;
            var missing = beforeSources();
            missing.removeIf(source -> normalized(source).equals(target.path()));
            reject(crate, missing, "required JDK21 text source missing: " + target.path());
            missing.add(text(target.path(), resource(ROOT + crate + "/" + target.before()) + "external drift\n"));
            reject(crate, missing, "source drift: " + target.path());
        }
    }

    @Test
    void historicalConflictsAndDuplicateTargetsCannotPublishPartialHistory() throws IOException {
        for (String crate : CRATES.subList(0, 2)) {
            List<Target> group = targets().stream().filter(row -> row.crate().equals(crate)).toList();
            for (Target target : List.of(group.get(0), group.get(group.size() - 1))) {
                reject(crate, List.of(text(target.path(), "unrelated current owner\n")),
                        "source drift: " + target.path());
                String after = resource(ROOT + crate + "/" + target.after());
                reject(crate, List.of(text(target.path(), after), text(target.path(), after)),
                        "duplicate target: " + target.path());
            }
        }
    }

    @Test
    void cataloguePreservesTheTwoProductRowsAndAddsOnly443HistoricalCustodyObligations() throws IOException {
        Target catalogue = targets().stream().filter(row -> row.path().equals("m3/history/RETAINED_CAPABILITIES.tsv"))
                .findFirst().orElseThrow();
        String before = resource(ROOT + catalogue.crate() + "/" + catalogue.before());
        String after = resource(ROOT + catalogue.crate() + "/" + catalogue.after());
        assertEquals(3, before.lines().count());
        assertTrue(after.startsWith(before));
        var additions = after.substring(before.length()).lines().toList();
        assertEquals(2, additions.size());
        for (int i = 0; i < 2; i++) {
            String path = "m3/tooling/migration-recipes/src/main/resources" + ROOT + CRATES.get(i) + "/manifest.tsv";
            assertEquals(CRATES.get(i) + "\t" + path, additions.get(i));
        }
        assertEquals(443, COUNTS.get(0) + COUNTS.get(1));
    }

    private static List<Target> targets() throws IOException {
        var result = new ArrayList<Target>();
        for (int i = 0; i < CRATES.size(); i++) {
            String crate = CRATES.get(i);
            String manifest = resource(ROOT + crate + "/manifest.tsv");
            assertEquals(MANIFEST_HASHES.get(i), M3Jdk21HashPinnedTextSnapshotRecipe.sha256(manifest));
            JsonNode plan = JSON.readTree(resource(ROOT + crate + "/plan.json"));
            assertEquals("87590cb96fb0e2dc0f88fae8e01957f7179cd255", plan.path("destination_base").asText());
            assertEquals(COUNTS.get(i).intValue(), plan.path("outputs").size());
            var lines = manifest.lines().toList();
            assertEquals(COUNTS.get(i).intValue(), lines.size());
            int index = 0;
            for (JsonNode output : plan.path("outputs")) {
                String path = output.path("path").asText();
                JsonNode before = output.get("before");
                JsonNode after = output.get("after");
                String beforeResource = before.isNull() ? null : before.path("resource").asText();
                String beforeHash = before.isNull() ? "ABSENT" : before.path("sha256").asText();
                String afterResource = after.path("resource").asText();
                String afterHash = after.path("sha256").asText();
                assertEquals(path + "\t" + beforeHash + "\t" + afterHash + "\t" + afterResource, lines.get(index++));
                assertEquals(afterHash, M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource(ROOT + crate + "/" + afterResource)));
                if (beforeResource != null) assertEquals(beforeHash,
                        M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource(ROOT + crate + "/" + beforeResource)));
                result.add(new Target(crate, path, beforeResource, afterResource, beforeHash, afterHash));
            }
        }
        return result;
    }

    private static List<SourceFile> beforeSources() throws IOException {
        var result = new ArrayList<SourceFile>();
        for (Target target : targets()) if (target.before() != null)
            result.add(text(target.path(), resource(ROOT + target.crate() + "/" + target.before())));
        return result;
    }

    private static Map<String, String> expectedAfter() throws IOException {
        var result = new TreeMap<String, String>();
        for (Target target : targets()) result.put(target.path(), resource(ROOT + target.crate() + "/" + target.after()));
        return result;
    }

    private static void reject(String crate, List<SourceFile> input, String message) {
        var before = input.stream().map(source -> normalized(source) + "\0" + source.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe(crate).run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty(), "Refusal must not emit partial results");
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), message);
        assertTrue(errors.stream().anyMatch(error -> messages(error).contains(message)), errors.toString());
        assertEquals(before, input.stream().map(source -> normalized(source) + "\0" + source.printAll()).toList());
    }

    private static String messages(Throwable failure) {
        var result = new StringBuilder();
        for (int depth = 0; failure != null && depth < 32; depth++, failure = failure.getCause())
            result.append(failure.getMessage()).append('\n');
        return result.toString();
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe(String crate) {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(crate);
    }

    private static SourceFile text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static String resource(String path) throws IOException {
        try (var stream = JccMergeRecoveryTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String gitBlob(byte[] body) {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + body.length + "\0").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
