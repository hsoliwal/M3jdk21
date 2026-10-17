/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** Reuses the byte-identical admitted Java/text snapshot engines; never edits their outputs. */
final class M3FilteredSortedRecipeTest {
    static final String CRATE = "m3-filtered-sorted-receiver";
    static final String JAVA = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    static final String TEXT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    static final String MODULE = "m3/collections/";
    static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    static Recipe recipe(boolean java) {
        return java ? new M3HashPinnedJavaSnapshotRecipe(CRATE) : new M3HashPinnedTextSnapshotRecipe(CRATE);
    }
    static String resource(String path) {
        try (var in = M3FilteredSortedRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path); return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    static List<String[]> rows(boolean java) {
        return resource((java ? JAVA : TEXT) + "manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }
    static Map<String, String> images(boolean java, boolean before) {
        Map<String, String> values = new TreeMap<>();
        for (String[] row : rows(java)) {
            if (before && row[1].equals("ABSENT")) continue;
            values.put(row[0], resource((java ? JAVA : TEXT) + (before ? "pre/" : "") + row[3]));
        }
        return values;
    }
    static List<SourceFile> parse(boolean java, Map<String, String> source, ExecutionContext ctx) {
        List<Parser.Input> inputs = source.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        return java ? JavaParser.fromJavaVersion().build().parseInputs(inputs, null, ctx).toList()
                : PlainTextParser.builder().build().parseInputs(inputs, null, ctx).toList();
    }
    static List<Result> run(boolean java, Map<String, String> inputs) {
        var ctx = context();
        Map<String, String> source = inputs.isEmpty()
                ? Map.of(java ? "other/Seed.java" : "src/main/native/seed.c", java ? "class Seed {}\n" : "/* seed */\n")
                : inputs;
        return recipe(java).run(new InMemoryLargeSourceSet(parse(java, source, ctx)), ctx, 1)
                .getChangeset().getAllResults();
    }
    static String sha(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    @Test void exactReplayFixedPointAndOptionalDelivery() throws Exception {
        Map<String, String> delivered = new TreeMap<>();
        Map<String, String> preimages = new TreeMap<>();
        for (boolean java : List.of(true, false)) {
            Map<String, String> actual = new TreeMap<>();
            for (Result result : run(java, images(java, true))) {
                assertNotNull(result.getAfter());
                if (java) assertInstanceOf(J.CompilationUnit.class, result.getAfter());
                else assertInstanceOf(PlainText.class, result.getAfter());
                actual.put(result.getAfter().getSourcePath().toString(), result.getAfter().printAll());
            }
            assertEquals(images(java, false), actual);
            assertTrue(run(java, actual).isEmpty(), "fixed point");
            for (String[] row : rows(java)) {
                String target = (java ? "" : MODULE) + row[0];
                assertNull(delivered.put(target, actual.get(row[0])), "overlapping pass target");
                preimages.put(target, row[1]);
            }
        }
        String destination = System.getProperty("m3.wrappers.target");
        if (destination == null || destination.isBlank()) return;
        Path root = Path.of(destination).toRealPath();
        // Exclusive checkout required. Validate the complete closure before any filesystem write.
        for (var entry : delivered.entrySet()) checkTarget(root, entry.getKey(), preimages.get(entry.getKey()), entry.getValue());
        for (var entry : delivered.entrySet()) {
            Path target = root.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            checkTarget(root, entry.getKey(), preimages.get(entry.getKey()), entry.getValue());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.readString(target).equals(entry.getValue())) {
                    Path temp = Files.createTempFile(target.getParent(), ".m3-recipe-", ".tmp");
                    try {
                        Files.writeString(temp, entry.getValue(), StandardCharsets.UTF_8);
                        checkTarget(root, entry.getKey(), preimages.get(entry.getKey()), entry.getValue());
                        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(temp); }
                }
            } else Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            assertEquals(entry.getValue(), Files.readString(target));
        }
        StringBuilder receipt = new StringBuilder("# Actual existing OpenRewrite Java/text engine results; repository-relative UTF-8 SHA-256\n");
        delivered.forEach((path, text) -> receipt.append(path).append('\t').append(sha(text)).append('\n'));
        Path receiptPath = root.resolve(MODULE + "wrappers/recipe-receipt.tsv");
        rejectSymlink(root, receiptPath);
        Files.writeString(receiptPath, receipt, StandardCharsets.UTF_8);
    }
    static void rejectSymlink(Path root, Path target) {
        assertTrue(target.normalize().startsWith(root.resolve(MODULE)), "outside sealed owner");
        for (Path cursor = target; !cursor.equals(root); cursor = cursor.getParent()) {
            assertNotNull(cursor); assertFalse(Files.isSymbolicLink(cursor), "symlink: " + cursor);
        }
    }
    static void checkTarget(Path root, String relative, String before, String after) throws IOException {
        Path target = root.resolve(relative).normalize(); rejectSymlink(root, target);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            assertTrue(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS));
            String current = sha(Files.readString(target));
            assertTrue(current.equals(before) || current.equals(sha(after)), "occupied/drifted: " + relative);
        } else assertEquals("ABSENT", before, "missing preimage: " + relative);
    }
    @Test void sealedManifestHashesAndJavaTextSeparation() {
        Set<String> targets = new HashSet<>();
        for (boolean java : List.of(true, false)) {
            for (String[] row : rows(java)) {
                assertEquals(4, row.length);
                assertTrue(targets.add((java ? "" : MODULE) + row[0]));
                assertEquals(row[2], sha(resource((java ? JAVA : TEXT) + row[3])));
                if (!row[1].equals("ABSENT")) assertEquals(row[1], sha(resource((java ? JAVA : TEXT) + "pre/" + row[3])));
                assertEquals(java, row[0].endsWith(".java"));
            }
        }
        assertEquals(7, images(true, false).size());
        assertEquals(5, images(false, false).size());
        assertTrue(images(true, true).isEmpty());
        assertTrue(images(false, true).isEmpty());
    }
    @Test void eachTargetPresentAndMissingResumesWithoutReplacingAdmittedOutputs() {
        for (boolean java : List.of(true, false)) {
            Map<String, String> complete = images(java, false);
            List<String> paths = new ArrayList<>(complete.keySet());
            // Every target is independently present and independently missing;
            // cross-lane targets are disjoint. This is 18 Java + 12 text states,
            // not an assertion of exhaustive 2^12 whole-packet schedules.
            Set<Integer> masks = new TreeSet<>();
            int full = (1 << paths.size()) - 1;
            masks.add(0); masks.add(full);
            for (int bit = 0; bit < paths.size(); bit++) {
                masks.add(1 << bit); masks.add(full ^ (1 << bit));
            }
            if (java) { masks.add(full & 0x55); masks.add(full & 0x2a); }
            for (int mask : masks) {
                Map<String, String> partial = new TreeMap<>();
                for (int bit = 0; bit < paths.size(); bit++) {
                    if ((mask & (1 << bit)) != 0) partial.put(paths.get(bit), complete.get(paths.get(bit)));
                }
                List<Result> results = run(java, partial);
                assertEquals(complete.size() - partial.size(), results.size());
                for (Result result : results) {
                    assertNull(result.getBefore());
                    String path = result.getAfter().getSourcePath().toString();
                    assertFalse(partial.containsKey(path));
                    assertEquals(complete.get(path), result.getAfter().printAll());
                }
            }
        }
    }
    @Test void refusesEveryJavaDrift() {
        for (String target : images(true, false).keySet()) {
            var inputs = images(true, true); inputs.put(target, "class Occupied {}\n");
            var ctx = context(); var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
            var state = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(state);
            parse(true, inputs, ctx).forEach(file -> scanner.visit(file, ctx));
            assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
        }
    }
    @Test void unrelatedInputsAreNotModified() {
        for (boolean java : List.of(true, false)) {
            String path = java ? "other/Untouched.java" : "src/main/native/untouched.c";
            String text = java ? "class Untouched {}\n" : "/* unchanged */\n";
            var changes = run(java, Map.of(path, text));
            assertEquals(images(java, false).size(), changes.size());
            for (Result change : changes) {
                assertNull(change.getBefore(), "all refinement targets are additive");
                assertNotEquals(path, change.getAfter().getSourcePath().toString());
            }
        }
    }
    @Test void refusesEveryTextDrift() {
        for (String target : images(false, false).keySet()) {
            var ctx = context(); var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
            var state = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(state);
            parse(false, Map.of(target, "drift\n"), ctx).forEach(file -> scanner.visit(file, ctx));
            assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
        }
    }
    @Test void refusesDuplicatesAndNonJavaSubstitution() {
        var ctx = context(); var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var state = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(state);
        var inputs = parse(true, images(true, false), ctx);
        inputs.forEach(file -> scanner.visit(file, ctx)); scanner.visit(inputs.getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
        var textRecipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var textState = textRecipe.getInitialValue(ctx); var textScanner = textRecipe.getScanner(textState);
        var text = parse(false, images(false, false), ctx).getFirst();
        textScanner.visit(text, ctx); textScanner.visit(text, ctx);
        assertThrows(IllegalStateException.class, () -> textRecipe.generate(textState, ctx));
        var substituted = recipe.getInitialValue(ctx); var scan = recipe.getScanner(substituted);
        var original = images(true, false).entrySet().iterator().next();
        scan.visit(PlainText.builder().sourcePath(Path.of(original.getKey())).text(original.getValue()).build(), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(substituted, ctx));
    }
    @Test void donorPinsAndRuntimeTargetsHaveCatalogue() {
        String catalogue = resource(TEXT + "catalogue.tsv.txt");
        assertTrue(catalogue.contains("M3CollectionViews.java"));
        assertTrue(catalogue.contains("M3FilteredMaps.java"));
        assertTrue(catalogue.contains("RunSearchJni"));
        for (String row : catalogue.lines().filter(x -> !x.isBlank() && !x.startsWith("#")).toList()) {
            assertEquals(8, row.split("\t", -1).length);
        }
        String provenance = resource(TEXT + "provenance.json.txt");
        assertTrue(provenance.contains("a0227fc8a23b102613dd760a6180a8f39e344215"));
        assertTrue(provenance.contains("6c769c61d1851a7e4b08f63ea85c9458c939ae31"));
    }
    @Test void deliveryRejectsSymlinksAndForeignContents() throws Exception {
        Path root = Files.createTempDirectory("m3-sealed-delivery-");
        try {
            Path owner = root.resolve(MODULE); Files.createDirectories(owner);
            String path = MODULE + "occupied.java";
            Files.writeString(root.resolve(path), "foreign");
            assertThrows(AssertionError.class, () -> checkTarget(root, path, "ABSENT", "new"));
            Files.createSymbolicLink(owner.resolve("link"), root);
            assertThrows(AssertionError.class, () -> checkTarget(root, MODULE + "link/x.java", "ABSENT", "new"));
        } finally {
            try (var paths = Files.walk(root)) { paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (IOException e) { throw new IllegalStateException(e); } }); }
        }
    }
}
