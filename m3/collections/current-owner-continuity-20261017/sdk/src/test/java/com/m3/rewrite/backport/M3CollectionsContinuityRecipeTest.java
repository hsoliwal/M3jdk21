/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/**
 * Adapted from unchanged M3CountedCollectionRecipeTest and M3FilteredSortedRecipeTest.
 * PlainText establishes source custody; original Java SDKs subsequently establish parsing.
 */
final class M3CollectionsContinuityRecipeTest {
    static final String CRATE = "m3-collections-current-owner-continuity-20261017";
    static final String RES = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
    }
    static String resource(String path) {
        try (var in = M3CollectionsContinuityRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    static List<String[]> rows() {
        return resource(RES + "manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }
    private static final Map<String, String> SEALED_IMAGES = Collections.unmodifiableMap(readImages());
    static Map<String, String> images() { return new TreeMap<>(SEALED_IMAGES); }
    private static Map<String, String> readImages() {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : rows()) assertNull(result.put(row[0], resource(RES + row[3])));
        return result;
    }
    static List<SourceFile> sources(Map<String, String> values, boolean reverse) {
        List<SourceFile> result = new ArrayList<>();
        values.forEach((path, text) -> result.add(PlainText.builder()
                .sourcePath(Path.of(path)).text(text).build()));
        result.add(PlainText.builder().sourcePath(Path.of("unrelated/seed.txt"))
                .text("unchanged unrelated seed\n").build());
        if (reverse) Collections.reverse(result);
        return result;
    }
    static List<Result> run(Map<String, String> source, boolean reverse) {
        var ctx = context();
        return recipe().run(new InMemoryLargeSourceSet(sources(source, reverse)), ctx, 1)
                .getChangeset().getAllResults();
    }
    static String sha(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    static void assertReplay(Map<String, String> partial, boolean reverse) {
        Map<String, String> expected = images();
        List<Result> results = run(partial, reverse);
        assertEquals(expected.size() - partial.size(), results.size());
        Map<String, String> complete = new TreeMap<>(partial);
        for (Result result : results) {
            assertNull(result.getBefore(), "all bootstrap targets are CREATE-only");
            assertInstanceOf(PlainText.class, result.getAfter());
            String path = result.getAfter().getSourcePath().toString().replace('\\', '/');
            assertFalse(partial.containsKey(path));
            assertEquals(expected.get(path), result.getAfter().printAll(), path);
            assertNull(complete.put(path, result.getAfter().printAll()));
        }
        assertEquals(expected, complete);
        assertTrue(run(complete, reverse).isEmpty(), "entire bootstrap fixed point");
    }
    @Test void exactReplayFixedPointAndOptionalDelivery() throws Exception {
        assertReplay(Map.of(), false);
        assertReplay(Map.of(), true);
        String destination = System.getProperty("m3.collections.continuity.target");
        if (destination == null || destination.isBlank()) return;
        Path root = Path.of(destination).toRealPath();
        Map<String, String> actual = new TreeMap<>();
        for (Result result : run(Map.of(), false)) {
            actual.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        assertEquals(images(), actual, "filesystem writes only actual recipe results");
        deliver(root, actual);
        assertTrue(run(actual, false).isEmpty());
        String output = System.getProperty("m3.continuity.output");
        assertNotNull(output, "explicit run-bound output directory");
        Path receipt = Path.of(output).resolve("bootstrap-receipt.tsv");
        rejectSymlink(root, receipt);
        Files.createDirectories(receipt.getParent());
        StringBuilder text = new StringBuilder("# Actual existing OpenRewrite PlainText engine results; repository-relative UTF-8 SHA-256\n");
        actual.forEach((path, value) -> text.append(path).append('\t').append(sha(value)).append('\n'));
        if (Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) {
            assertTrue(Files.isRegularFile(receipt, LinkOption.NOFOLLOW_LINKS));
            assertEquals(text.toString(), Files.readString(receipt, StandardCharsets.UTF_8));
        } else Files.writeString(receipt, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    @Test void independentPresentMissingAndPrefixSuffixSchedulesInBothOrders() {
        Map<String, String> all = images();
        List<String> paths = new ArrayList<>(all.keySet());
        Set<Set<String>> states = new LinkedHashSet<>();
        states.add(Set.of()); states.add(Set.copyOf(paths));
        for (int bit = 0; bit < paths.size(); bit++) {
            states.add(Set.of(paths.get(bit)));
            Set<String> except = new TreeSet<>(paths); except.remove(paths.get(bit));
            states.add(Set.copyOf(except));
            states.add(Set.copyOf(paths.subList(0, bit)));
            states.add(Set.copyOf(paths.subList(bit, paths.size())));
        }
        Set<String> alternating = new TreeSet<>();
        for (int bit = 0; bit < paths.size(); bit += 2) alternating.add(paths.get(bit));
        states.add(Set.copyOf(alternating));
        Set<String> inverse = new TreeSet<>(paths); inverse.removeAll(alternating);
        states.add(Set.copyOf(inverse));
        // Independent per-path, prefix, suffix and alternating schedules, not exhaustive 2^N.
        for (Set<String> state : states) {
            Map<String, String> partial = new TreeMap<>();
            state.forEach(path -> partial.put(path, all.get(path)));
            assertReplay(partial, false); assertReplay(partial, true);
        }
    }
    @Test void everyUnknownTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        assertReplay(all, false);
        for (String path : all.keySet()) {
            var ctx = context(); var owner = recipe(); var state = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(state);
            Map<String, String> changed = new TreeMap<>(all); changed.put(path, "foreign source\n");
            sources(changed, false).forEach(file -> scanner.visit(file, ctx));
            var error = assertThrows(IllegalStateException.class, () -> owner.generate(state, ctx));
            assertTrue(error.getMessage().contains("source drift: " + path), path);
            assertThrows(IllegalStateException.class, () -> owner.getVisitor(state));
        }
    }
    @Test void everyDuplicateTargetRefusesAfterItsPositiveBaseline() {
        Map<String, String> all = images();
        assertReplay(all, false);
        for (String path : all.keySet()) {
            var ctx = context(); var owner = recipe(); var state = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(state);
            sources(all, false).forEach(file -> scanner.visit(file, ctx));
            scanner.visit(PlainText.builder().sourcePath(Path.of(path)).text(all.get(path)).build(), ctx);
            var error = assertThrows(IllegalStateException.class, () -> owner.generate(state, ctx));
            assertTrue(error.getMessage().contains("duplicate target: " + path), path);
        }
    }
    @Test void everyChangedAfterScanTargetRefuses() {
        Map<String, String> all = images();
        assertReplay(all, false);
        for (String path : all.keySet()) {
            var ctx = context(); var owner = recipe(); var state = owner.getInitialValue(ctx);
            var scanner = owner.getScanner(state);
            sources(all, false).forEach(file -> scanner.visit(file, ctx));
            assertTrue(owner.generate(state, ctx).isEmpty());
            var visitor = owner.getVisitor(state);
            var changed = PlainText.builder().sourcePath(Path.of(path)).text("post-scan drift\n").build();
            var error = assertThrows(RecipeRunException.class, () -> visitor.visit(changed, ctx));
            assertInstanceOf(IllegalStateException.class, error.getCause());
            assertTrue(error.getCause().getMessage().contains("changed after scan: " + path), path);
        }
    }
    @Test void sealedManifestPathSafetyTypeFenceAndUnrelatedInputs() {
        Set<String> unique = new HashSet<>(); String previous = "";
        for (String[] row : rows()) {
            assertEquals(4, row.length); assertTrue(unique.add(row[0]));
            assertTrue(previous.compareTo(row[0]) < 0); previous = row[0];
            assertEquals("ABSENT", row[1]); assertEquals(row[2], sha(resource(RES + row[3])));
            assertTrue(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(row[0]));
        }
        assertTrue(unique.size() > 75 && unique.size() <= 256);
        assertEquals(new ArrayList<>(images().keySet()), recipe().targetPaths());
        for (String path : List.of("", "/absolute", "C:/drive", "../up", "a/../b", "a/./b",
                "a//b", "a/.GIT/config", "a\\b", "a/" + Character.toString(0) + "b")) {
            assertFalse(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(path), path);
        }
        var ctx = context(); var owner = recipe(); var state = owner.getInitialValue(ctx);
        var scanner = owner.getScanner(state);
        sources(images(), false).forEach(file -> scanner.visit(file, ctx));
        String target = images().keySet().stream().filter(path -> path.endsWith(".java")).findFirst().orElseThrow();
        var java = JavaParser.fromJavaVersion().build().parse(ctx, "class WrongType {}").findFirst().orElseThrow();
        scanner.visit(java.withSourcePath(Path.of(target)), ctx);
        var error = assertThrows(IllegalStateException.class, () -> owner.generate(state, ctx));
        assertTrue(error.getMessage().contains("not OpenRewrite PlainText: " + target));
    }
    static void rejectSymlink(Path root, Path target) {
        assertTrue(target.normalize().startsWith(root), "outside exclusive receiver");
        for (Path cursor = target; !cursor.equals(root); cursor = cursor.getParent()) {
            assertNotNull(cursor); assertFalse(Files.isSymbolicLink(cursor), "symlink: " + cursor);
        }
    }
    static void checkTarget(Path root, String relative, String after) throws IOException {
        assertTrue(images().containsKey(relative), "undeclared bootstrap target");
        Path target = root.resolve(relative).normalize(); rejectSymlink(root, target);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            assertTrue(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS));
            assertEquals(sha(after), sha(Files.readString(target, StandardCharsets.UTF_8)),
                    "occupied/drifted: " + relative);
        }
    }
    static void deliver(Path root, Map<String, String> actual) throws IOException {
        assertEquals(images(), actual);
        for (var entry : actual.entrySet()) checkTarget(root, entry.getKey(), entry.getValue());
        for (var entry : actual.entrySet()) {
            checkTarget(root, entry.getKey(), entry.getValue());
            Path target = root.resolve(entry.getKey());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) continue;
            Files.createDirectories(target.getParent());
            checkTarget(root, entry.getKey(), entry.getValue());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            assertEquals(entry.getValue(), Files.readString(target, StandardCharsets.UTF_8));
        }
    }
    static void remove(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
    @Test void deliveryRejectsSymlinksAndForeignContents() throws Exception {
        Path root = Files.createTempDirectory("m3-continuity-fence-");
        try {
            var first = images().entrySet().iterator().next();
            Path occupied = root.resolve(first.getKey()); Files.createDirectories(occupied.getParent());
            Files.writeString(occupied, "foreign");
            assertThrows(AssertionError.class, () -> checkTarget(root, first.getKey(), first.getValue()));
            Files.delete(occupied);
            Files.createSymbolicLink(occupied, root);
            assertThrows(AssertionError.class, () -> checkTarget(root, first.getKey(), first.getValue()));
        } finally { remove(root); }
    }
    @Test void completeDeliveryPrecheckRefusesBeforeAnyEarlierTargetWrite() throws Exception {
        Path root = Files.createTempDirectory("m3-continuity-precheck-");
        try {
            Map<String, String> all = images();
            String last = new ArrayList<>(all.keySet()).getLast();
            Path occupied = root.resolve(last); Files.createDirectories(occupied.getParent());
            Files.writeString(occupied, "late foreign target");
            assertThrows(AssertionError.class, () -> deliver(root, all));
            for (String path : all.keySet()) {
                if (!path.equals(last)) assertFalse(Files.exists(root.resolve(path), LinkOption.NOFOLLOW_LINKS), path);
            }
            assertEquals("late foreign target", Files.readString(occupied));
            Files.delete(occupied);
            deliver(root, all);
            Map<String, String> first = new TreeMap<>();
            for (String path : all.keySet()) first.put(path, Files.readString(root.resolve(path), StandardCharsets.UTF_8));
            deliver(root, all);
            for (String path : all.keySet()) assertEquals(first.get(path), Files.readString(root.resolve(path), StandardCharsets.UTF_8));
        } finally { remove(root); }
    }
}
