/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * Adapted from M3CollectionsContinuityRecipeTest, Git blob
 * 406bbd972956883e2ea989727d60ffa6662e34ae. */
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markers;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

/**
 * Source-custody conformance for the unchanged existing PlainText recipe.
 * Original Python/provider execution and full migration acceptance are separate gates.
 */
final class M3OriginalCliProviderRecipeTest {
    private static final String CRATE = "m3-original-mapping-provider-20261017";
    private static final String RES =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private static final String UNRELATED = "unrelated/provider-custody-seed.txt";
    private static final String SEED = "unchanged unrelated seed\n";

    private record Row(String path, String before, String after, String resource, String text) {
        boolean added() { return "ABSENT".equals(before); }
        boolean mutable() { return !before.equals(after); }
    }

    private record Scenario(List<SourceFile> files, PlainText seed) {}

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
    }

    private static String resource(String name) {
        try (var in = M3OriginalCliProviderRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static List<Row> readRows() {
        List<Row> result = new ArrayList<>();
        String previous = "";
        Set<String> paths = new HashSet<>();
        for (String line : resource(RES + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length, "manifest width");
            assertTrue(paths.add(cells[0]), "duplicate manifest target");
            assertTrue(previous.compareTo(cells[0]) < 0, "manifest order");
            previous = cells[0];
            String text = resource(RES + cells[3]);
            assertEquals(cells[2], sha(text), cells[0]);
            result.add(new Row(cells[0], cells[1], cells[2], cells[3], text));
        }
        assertFalse(result.isEmpty(), "empty manifest");
        return List.copyOf(result);
    }

    private static final List<Row> ROWS = readRows();

    private static List<Row> mutableRows() {
        return ROWS.stream().filter(Row::mutable).toList();
    }

    private static Map<String, String> images() {
        Map<String, String> result = new TreeMap<>();
        for (Row row : ROWS) assertNull(result.put(row.path(), row.text()));
        return result;
    }

    private static String beforeText(Row row) {
        assertFalse(row.added(), "ABSENT has no text preimage");
        String text = row.before().equals(row.after())
                ? row.text() : resource(RES + "before-" + row.resource());
        assertEquals(row.before(), sha(text), "preimage fixture: " + row.path());
        return text;
    }

    private static Map<String, String> state(long mask) {
        Map<String, String> result = new TreeMap<>();
        int bit = 0;
        for (Row row : ROWS) {
            if (!row.mutable()) {
                result.put(row.path(), row.text());
            } else {
                boolean after = (mask & (1L << bit++)) != 0;
                if (after) result.put(row.path(), row.text());
                else if (!row.added()) result.put(row.path(), beforeText(row));
            }
        }
        assertEquals(mutableRows().size(), bit);
        return result;
    }

    private static Scenario sources(Map<String, String> values, boolean reverse) {
        List<SourceFile> files = new ArrayList<>();
        values.forEach((path, text) -> files.add(PlainText.builder()
                .sourcePath(Path.of(path)).text(text).build()));
        PlainText seed = PlainText.builder().sourcePath(Path.of(UNRELATED)).text(SEED).build();
        files.add(seed);
        if (reverse) Collections.reverse(files);
        return new Scenario(files, seed);
    }

    private static String path(SourceFile file) {
        return file.getSourcePath().toString().replace('\\', '/');
    }

    private static List<Result> run(Scenario source) {
        var ctx = context();
        List<Result> result = recipe().run(new InMemoryLargeSourceSet(source.files()), ctx, 1)
                .getChangeset().getAllResults();
        assertEquals(SEED, source.seed().printAll());
        for (Result change : result) {
            assertNotNull(change.getAfter(), "removals are not admitted");
            assertNotEquals(UNRELATED, path(change.getAfter()), "unrelated source changed");
        }
        return result;
    }

    private static Map<String, String> assertReplay(Map<String, String> partial, boolean reverse) {
        Map<String, String> expected = images();
        Scenario source = sources(partial, reverse);
        Map<String, String> complete = new TreeMap<>();
        for (SourceFile file : source.files()) {
            if (!path(file).equals(UNRELATED)) {
                assertNull(complete.put(path(file), file.printAll()));
            }
        }
        Set<String> wanted = new TreeSet<>();
        for (Row row : ROWS) {
            if (!row.text().equals(partial.get(row.path()))) wanted.add(row.path());
        }
        Set<String> observed = new TreeSet<>();
        for (Result result : run(source)) {
            SourceFile after = result.getAfter();
            assertInstanceOf(PlainText.class, after);
            String name = path(after);
            assertTrue(wanted.contains(name), "unexpected recipe result: " + name);
            assertTrue(observed.add(name), "duplicate recipe result: " + name);
            if (partial.containsKey(name)) {
                assertNotNull(result.getBefore(), "CHANGE needs actual before source");
                assertEquals(name, path(result.getBefore()));
                assertEquals(partial.get(name), result.getBefore().printAll(), name);
            } else {
                assertNull(result.getBefore(), "ADD must have absent before source");
            }
            assertEquals(expected.get(name), after.printAll(), name);
            complete.put(name, after.printAll());
        }
        assertEquals(wanted, observed, "actual changed owners");
        assertEquals(expected, complete, "actual input-plus-results overlay");
        assertTrue(run(sources(complete, reverse)).isEmpty(), "actual second recipe fixed point");
        return complete;
    }

    private static void positiveBaseline() {
        assertReplay(images(), false);
    }

    @Test void allMutableStatesBothOrdersAndFixedPoint() {
        assertEquals(6, mutableRows().size(), "six reviewed mutable owners");
        assertEquals(5, ROWS.stream().filter(Row::added).count(), "five ADD owners");
        assertEquals(1, ROWS.stream().filter(row -> row.mutable() && !row.added()).count(),
                "one CHANGE owner");
        long states = 1L << mutableRows().size();
        assertEquals(64, states);
        for (long mask = 0; mask < states; mask++) {
            assertReplay(state(mask), false);
            assertReplay(state(mask), true);
        }
    }

    @Test void exactRecipeResultsDeliveredToDeclaredOutput() throws Exception {
        Map<String, String> actual = assertReplay(state(0), false);
        String destination = System.getProperty("m3.output.root");
        assertNotNull(destination, "Maven must declare m3.output.root");
        assertFalse(destination.isBlank(), "empty emitted overlay destination");
        Path root = Path.of(destination);
        assertTrue(root.isAbsolute(), "emitted overlay destination must be absolute");
        root = root.normalize();
        deliver(root, actual);
        Map<String, String> first = new TreeMap<>();
        for (String name : actual.keySet()) {
            first.put(name, Files.readString(root.resolve(name), StandardCharsets.UTF_8));
        }
        assertEquals(actual, first);
        deliver(root, actual);
        for (String name : actual.keySet()) {
            assertEquals(first.get(name), Files.readString(root.resolve(name), StandardCharsets.UTF_8));
        }
        assertTrue(run(sources(first, true)).isEmpty(), "delivered overlay is a recipe fixed point");
    }

    @Test void everyUnknownTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        for (Row row : ROWS) {
            positiveBaseline();
            var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(inventory);
            Map<String, String> changed = new TreeMap<>(all);
            changed.put(row.path(), "foreign source\n");
            sources(changed, false).files().forEach(file -> scanner.visit(file, ctx));
            var error = assertThrows(IllegalStateException.class,
                    () -> owner.generate(inventory, ctx));
            assertTrue(error.getMessage().contains("source drift: " + row.path()), row.path());
            assertThrows(IllegalStateException.class, () -> owner.getVisitor(inventory));
            assertEquals(images(), all, "baseline source map mutated");
        }
    }

    @Test void everyDuplicateTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        for (Row row : ROWS) {
            positiveBaseline();
            var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(inventory);
            sources(all, false).files().forEach(file -> scanner.visit(file, ctx));
            scanner.visit(PlainText.builder().sourcePath(Path.of(row.path()))
                    .text(row.text()).build(), ctx);
            var error = assertThrows(IllegalStateException.class,
                    () -> owner.generate(inventory, ctx));
            assertTrue(error.getMessage().contains("duplicate target: " + row.path()), row.path());
            assertEquals(images(), all, "baseline source map mutated");
        }
    }

    @Test void everyRequiredTargetMissingRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        for (Row row : ROWS) {
            if (row.added()) continue;
            positiveBaseline();
            var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(inventory);
            Map<String, String> missing = new TreeMap<>(all);
            missing.remove(row.path());
            sources(missing, false).files().forEach(file -> scanner.visit(file, ctx));
            var error = assertThrows(IllegalStateException.class,
                    () -> owner.generate(inventory, ctx));
            assertTrue(error.getMessage().contains(
                    "required JDK21 text source missing: " + row.path()), row.path());
            assertEquals(images(), all, "baseline source map mutated");
        }
    }

    @Test void everyNonPlainTextTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        for (Row row : ROWS) {
            positiveBaseline();
            var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(inventory);
            for (SourceFile file : sources(all, false).files()) {
                if (path(file).equals(row.path())) {
                    SourceFile wrong = new ParseError(Tree.randomId(), Markers.EMPTY,
                            Path.of(row.path()), null, "UTF-8", false, null, row.text(), null);
                    assertEquals(row.text(), wrong.printAll(), "type-fence source bytes");
                    scanner.visit(wrong, ctx);
                } else scanner.visit(file, ctx);
            }
            var error = assertThrows(IllegalStateException.class,
                    () -> owner.generate(inventory, ctx));
            assertTrue(error.getMessage().contains(
                    "not OpenRewrite PlainText: " + row.path()), row.path());
            assertFalse(error.getMessage().contains("duplicate target:"), row.path());
            assertFalse(error.getMessage().contains("source drift:"), row.path());
        }
    }

    private static void changedAfterScan(Map<String, String> source, Row row, String value) {
        var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
        var scanner = owner.getScanner(inventory);
        sources(source, false).files().forEach(file -> scanner.visit(file, ctx));
        assertTrue(owner.generate(inventory, ctx).isEmpty(), "all ADD owners are present");
        var visitor = owner.getVisitor(inventory);
        var changed = PlainText.builder().sourcePath(Path.of(row.path())).text(value).build();
        var error = assertThrows(RecipeRunException.class, () -> visitor.visit(changed, ctx));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains(
                "changed after scan: " + row.path()), row.path());
    }

    @Test void everyChangedAfterScanTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        for (Row row : ROWS) {
            positiveBaseline();
            changedAfterScan(all, row, "post-scan foreign source\n");
            if (row.mutable() && !row.added()) {
                changedAfterScan(all, row, beforeText(row));
                Map<String, String> before = new TreeMap<>(all);
                before.put(row.path(), beforeText(row));
                assertReplay(before, false);
                changedAfterScan(before, row, row.text());
            }
            assertEquals(images(), all, "baseline source map mutated");
        }
    }

    @Test void sealedManifestPathSafetyAndConvergedIdentity() {
        assertEquals(ROWS.stream().map(Row::path).toList(), recipe().targetPaths());
        assertFalse(images().containsKey(UNRELATED), "unrelated seed is not a target");
        for (Row row : ROWS) {
            assertTrue(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(row.path()), row.path());
            if (!row.added()) beforeText(row);
        }
        for (String unsafe : List.of("", "/absolute", "C:/drive", "../up", "a/../b",
                "a/./b", "a//b", "a/.GIT/config", "a\\b", "a/" + Character.toString(0) + "b")) {
            assertFalse(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(unsafe), unsafe);
        }
        var ctx = context(); var owner = recipe(); var inventory = owner.getInitialValue(ctx);
        var scanner = owner.getScanner(inventory);
        Scenario converged = sources(images(), true);
        converged.files().forEach(file -> scanner.visit(file, ctx));
        assertTrue(owner.generate(inventory, ctx).isEmpty());
        var visitor = owner.getVisitor(inventory);
        for (SourceFile file : converged.files()) assertSame(file, visitor.visit(file, ctx));
    }

    private static void rejectSymlinks(Path target) {
        assertTrue(target.isAbsolute(), "absolute owned output path required");
        for (Path cursor = target; cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), "symlink component: " + cursor);
        }
    }

    private static void checkTarget(Path root, String relative, String expected) throws IOException {
        assertTrue(images().containsKey(relative), "undeclared emitted target");
        assertTrue(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(relative));
        Path target = root.resolve(relative).normalize();
        assertTrue(target.startsWith(root), "outside emitted overlay");
        rejectSymlinks(target);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            assertTrue(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS),
                    "nonregular emitted target: " + relative);
            assertEquals(expected, Files.readString(target, StandardCharsets.UTF_8),
                    "occupied/drifted emitted target: " + relative);
        }
    }

    private static void checkOverlayInventory(Path root) throws IOException {
        rejectSymlinks(root);
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        assertTrue(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS), "emitted root is not directory");
        Set<String> expected = images().keySet();
        try (var paths = Files.walk(root)) {
            for (Path item : paths.toList()) {
                rejectSymlinks(item);
                if (Files.isDirectory(item, LinkOption.NOFOLLOW_LINKS)) continue;
                assertTrue(Files.isRegularFile(item, LinkOption.NOFOLLOW_LINKS), "nonregular overlay input");
                String name = root.relativize(item).toString().replace('\\', '/');
                assertTrue(expected.contains(name), "undeclared existing overlay file: " + name);
            }
        }
    }

    private static void deliver(Path root, Map<String, String> actual) throws IOException {
        assertEquals(images(), actual, "only complete actual recipe overlays may be emitted");
        checkOverlayInventory(root);
        // Preflight every path before any creation, including the last sorted path.
        for (var entry : actual.entrySet()) checkTarget(root, entry.getKey(), entry.getValue());
        for (var entry : actual.entrySet()) {
            checkTarget(root, entry.getKey(), entry.getValue());
            Path target = root.resolve(entry.getKey());
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(target.getParent());
                checkTarget(root, entry.getKey(), entry.getValue());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
            }
            assertEquals(entry.getValue(), Files.readString(target, StandardCharsets.UTF_8));
        }
        checkOverlayInventory(root);
    }

    private static Path temporaryBase() throws IOException {
        String declared = System.getProperty("m3.build.root");
        assertNotNull(declared, "Maven must declare m3.build.root");
        Path root = Path.of(declared).toAbsolutePath().normalize();
        rejectSymlinks(root);
        assertTrue(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS), "Maven build root missing");
        return Files.createTempDirectory(root, "m3-provider-custody-");
    }

    private static void removeOwnedFixture(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(item);
        }
    }

    private static void assertNoEarlierWrites(Path root, String occupied) {
        for (Row row : ROWS) {
            if (!row.path().equals(occupied)) {
                assertFalse(Files.exists(root.resolve(row.path()), LinkOption.NOFOLLOW_LINKS),
                        "earlier emitted target written: " + row.path());
            }
        }
    }

    @Test void deliveryPrechecksForeignContentsAndAllSymlinkComponents() throws Exception {
        Path base = temporaryBase();
        try {
            Map<String, String> actual = assertReplay(state(0), false);
            String last = actual.keySet().stream().reduce((a, b) -> b).orElseThrow();
            Path foreignRoot = Files.createDirectory(base.resolve("foreign-content"));
            Path occupied = foreignRoot.resolve(last);
            Files.createDirectories(occupied.getParent());
            Files.writeString(occupied, "late foreign target", StandardCharsets.UTF_8);
            assertThrows(AssertionError.class, () -> deliver(foreignRoot, actual));
            assertNoEarlierWrites(foreignRoot, last);
            assertEquals("late foreign target", Files.readString(occupied, StandardCharsets.UTF_8));

            String first = actual.keySet().iterator().next();
            Path leafRoot = Files.createDirectory(base.resolve("leaf-link"));
            Path leaf = leafRoot.resolve(first);
            Files.createDirectories(leaf.getParent());
            Files.createSymbolicLink(leaf, base);
            assertThrows(AssertionError.class, () -> deliver(leafRoot, actual));
            assertNoEarlierWrites(leafRoot, first);

            Path parentRoot = Files.createDirectory(base.resolve("parent-link"));
            Path outside = Files.createDirectory(base.resolve("outside"));
            Path firstComponent = Path.of(first).getName(0);
            Files.createSymbolicLink(parentRoot.resolve(firstComponent), outside);
            assertThrows(AssertionError.class, () -> deliver(parentRoot, actual));
            try (var children = Files.list(outside)) {
                assertEquals(0, children.count(), "parent symlink destination was written");
            }

            Path realRoot = Files.createDirectory(base.resolve("real-root"));
            Path rootLink = base.resolve("root-link");
            Files.createSymbolicLink(rootLink, realRoot);
            assertThrows(AssertionError.class, () -> deliver(rootLink, actual));
            try (var children = Files.list(realRoot)) {
                assertEquals(0, children.count(), "root symlink destination was written");
            }
        } finally {
            removeOwnedFixture(base);
        }
    }

    @Test void currentMappingReconciliationPreservesNewFieldsAndReviewedMigration()
            throws Exception {
        String name = "m3/docs/name-mapping.json";
        Row mapping = ROWS.stream().filter(row -> row.path().equals(name))
                .findFirst().orElseThrow();
        String before = beforeText(mapping);
        String after = assertReplay(state(0), false).get(name);
        assertNotNull(after, "actual recipe overlay must contain the canonical map");
        assertEquals("4247c5648cf6a5599f8840a7171f147f5c343bf9b97e7a386eb7f351f978c4f3",
                sha(before), "exact current receiver map preimage");

        // These reviewed spans cover the JSON object value, excluding its property label/comma.
        int valueStart = 40_771;
        int beforeValueEnd = 377_326;
        int afterValueEnd = 414_297;
        assertEquals('{', before.charAt(valueStart));
        assertEquals('{', after.charAt(valueStart));
        assertEquals('}', before.charAt(beforeValueEnd - 1));
        assertEquals('}', after.charAt(afterValueEnd - 1));
        assertEquals("9ed985fa03ee732c8c9d15c3b6e082764dd1e3c4e258ad4d29188bf008ded449",
                sha(before.substring(valueStart, beforeValueEnd)),
                "original A56 migration object value");
        assertEquals("128762e785aa39ac3c9ec7cb8c6fb797f3ed50cd213d9c19323c332930c9bf46",
                sha(after.substring(valueStart, afterValueEnd)),
                "independently reviewed D60 migration object value");
        assertArrayEquals(before.substring(0, valueStart).getBytes(StandardCharsets.UTF_8),
                after.substring(0, valueStart).getBytes(StandardCharsets.UTF_8),
                "every byte before the migration value is preserved");
        assertArrayEquals(before.substring(beforeValueEnd).getBytes(StandardCharsets.UTF_8),
                after.substring(afterValueEnd).getBytes(StandardCharsets.UTF_8),
                "every byte after the migration value is preserved");

        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var beforeJson = json.readTree(before);
        var afterJson = json.readTree(after);
        assertTrue(beforeJson.isObject());
        assertTrue(afterJson.isObject());
        assertEquals(59, beforeJson.path("mappings").size(), "current top-level mappings");
        assertEquals(59, afterJson.path("mappings").size(), "retained top-level mappings");
        assertTrue(beforeJson.has("optional_module_mappings"));
        assertTrue(afterJson.has("optional_module_mappings"));
        var outsideBefore =
                (com.fasterxml.jackson.databind.node.ObjectNode) beforeJson.deepCopy();
        var outsideAfter =
                (com.fasterxml.jackson.databind.node.ObjectNode) afterJson.deepCopy();
        outsideBefore.remove("migration");
        outsideAfter.remove("migration");
        assertEquals(outsideBefore, outsideAfter, "every nonmigration JSON field is retained");

        var beforeMigration = beforeJson.path("migration");
        var afterMigration = afterJson.path("migration");
        assertTrue(beforeMigration.isObject());
        assertTrue(afterMigration.isObject());
        var migrationContextBefore =
                (com.fasterxml.jackson.databind.node.ObjectNode) beforeMigration.deepCopy();
        var migrationContextAfter =
                (com.fasterxml.jackson.databind.node.ObjectNode) afterMigration.deepCopy();
        migrationContextBefore.remove("records");
        migrationContextAfter.remove("records");
        assertEquals(migrationContextBefore, migrationContextAfter,
                "migration version/source/target/coverage/gates remain unchanged");

        var beforeRecords = mappingRecordsById(beforeMigration.path("records"));
        var afterRecords = mappingRecordsById(afterMigration.path("records"));
        assertEquals(56, beforeRecords.size());
        assertEquals(60, afterRecords.size());
        assertTrue(afterRecords.keySet().containsAll(beforeRecords.keySet()),
                "no existing migration record may be removed");
        Set<String> added = new TreeSet<>(afterRecords.keySet());
        added.removeAll(beforeRecords.keySet());
        assertEquals(Set.of(
                "synexia.regex.ordered-relations.adapter",
                "synexia.regex.ordered-relations.dispatch",
                "synexia.regex.metadata-pressure",
                "synexia.regex.hierarchical-route-acceptance"), added,
                "only the four reviewed restore records are added");

        Map<String, Set<String>> lineageChanges = Map.of(
                "synexia.counterpart.MIndexRegexTrigramQuery",
                Set.of("targets", "sync", "lineage", "observation"),
                "synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup",
                Set.of("sources", "recipe", "sync", "observation"));
        int unchanged = 0;
        int changed = 0;
        for (var entry : beforeRecords.entrySet()) {
            String id = entry.getKey();
            var previous = entry.getValue();
            var current = afterRecords.get(id);
            if (lineageChanges.containsKey(id)) {
                assertEquals(lineageChanges.get(id), changedRecordFields(previous, current),
                        id + " has only its explicit reviewed lineage field changes");
                changed++;
            } else {
                assertEquals(previous, current, id + " whole record is preserved");
                unchanged++;
            }
        }
        assertEquals(54, unchanged);
        assertEquals(2, changed);
    }

    private static Map<String, com.fasterxml.jackson.databind.JsonNode> mappingRecordsById(
            com.fasterxml.jackson.databind.JsonNode records) {
        assertTrue(records.isArray(), "migration records must be an array");
        Map<String, com.fasterxml.jackson.databind.JsonNode> result = new TreeMap<>();
        for (var record : records) {
            assertTrue(record.isObject(), "migration record must be an object");
            String id = record.path("id").asText();
            assertFalse(id.isBlank(), "migration record ID is required");
            assertNull(result.put(id, record), "duplicate migration record ID: " + id);
        }
        return result;
    }

    private static Set<String> changedRecordFields(
            com.fasterxml.jackson.databind.JsonNode before,
            com.fasterxml.jackson.databind.JsonNode after) {
        assertTrue(before.isObject());
        assertNotNull(after, "reviewed lineage record must remain present");
        assertTrue(after.isObject());
        Set<String> names = new TreeSet<>();
        before.fieldNames().forEachRemaining(names::add);
        after.fieldNames().forEachRemaining(names::add);
        names.removeIf(name -> java.util.Objects.equals(before.get(name), after.get(name)));
        return names;
    }
}
