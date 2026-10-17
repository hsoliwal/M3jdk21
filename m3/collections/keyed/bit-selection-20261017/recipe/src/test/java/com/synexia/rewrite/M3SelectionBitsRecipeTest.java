/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Actual named SDK execution, not template-copy qualification; exports only after all assertions. */
final class M3SelectionBitsRecipeTest {
    private static final String CRATE = "m3-selection-bits-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String NAME = "com.synexia.m3jdk.PackedKeyedSelection20261017";
    private static String resource(String path) {
        try (InputStream in = M3SelectionBitsRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<String[]> rows() {
        return resource(ROOT + "manifest.tsv").lines().filter(s -> !s.isBlank())
                .map(s -> s.split("\t", -1)).toList();
    }
    private static Map<String, String> snapshots(boolean before) {
        Map<String, String> out = new TreeMap<>();
        for (String[] row : rows()) {
            if (before && row[1].equals("ABSENT")) continue;
            assertNull(out.put(row[0], resource(ROOT + (before ? "pre/" : "") + row[3])));
        }
        return out;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException("SDK execution failed", error); });
    }
    private static List<SourceFile> parse(Map<String, String> input) {
        List<Parser.Input> sources = new ArrayList<>();
        new TreeMap<>(input).forEach((path, text) -> sources.add(Parser.Input.fromString(Path.of(path), text)));
        List<SourceFile> parsed = new ArrayList<>();
        parsed.addAll(JavaParser.fromJavaVersion().build().parseInputs(sources, null, context()).toList());
        return parsed;
    }
    private static Recipe named() {
        return Environment.builder().scanRuntimeClasspath().build().activateRecipes(NAME);
    }
    private static Map<String, String> apply(Recipe recipe, List<SourceFile> input) {
        Map<String, String> out = new TreeMap<>();
        recipe.run(new InMemoryLargeSourceSet(input), context(), 1).getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no deletion");
            assertNull(out.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll()), "duplicate output");
        });
        return out;
    }
    @Test void qualifyNamedRecipeAndExportActualResults() throws Exception {
        Map<String, String> before = snapshots(true), after = snapshots(false);
        assertEquals(1, before.size()); assertEquals(3, after.size());
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst());
        for (String[] row : rows()) {
            assertEquals(4, row.length);
            assertEquals(row[2], sha(after.get(row[0])));
            if (!row[1].equals("ABSENT")) assertEquals(row[1], sha(before.get(row[0])));
        }
        // Exact predecessor is sealed from the #1072 committed recipe template.
        assertEquals("ff0bb5901c8a8f67ef2ec07ea794f829102c09495a91c3d2e3f0c316baa8b262",
                sha(before.values().iterator().next()));
        Map<String, String> emitted = apply(named(), parse(before));
        assertEquals(after, emitted, "actual SDK Results differ from templates");
        assertTrue(apply(named(), parse(emitted)).isEmpty(), "not a fixed point");
        for (String installed : after.keySet()) {
            Map<String, String> partial = new LinkedHashMap<>(before);
            partial.put(installed, after.get(installed));
            partial.put("src/main/java/demo/Unrelated.java", "package demo; class Unrelated {}\n");
            Map<String, String> wanted = new TreeMap<>(after); wanted.remove(installed);
            assertEquals(wanted, apply(named(), parse(partial)), "partial/unrelated source failure");
        }
        assertThrows(RuntimeException.class, () -> apply(named(), parse(Map.of())));
        for (String path : before.keySet()) {
            Map<String, String> missing = new TreeMap<>(before); missing.remove(path);
            assertThrows(RuntimeException.class, () -> apply(named(), parse(missing)));
            Map<String, String> drift = new TreeMap<>(before); drift.put(path, drift.get(path) + "\n// drift\n");
            assertThrows(RuntimeException.class, () -> apply(named(), parse(drift)));
        }
        for (String path : after.keySet()) if (!before.containsKey(path)) {
            Map<String, String> occupied = new TreeMap<>(before);
            occupied.put(path, "class Occupied {}\n");
            assertThrows(RuntimeException.class, () -> apply(named(), parse(occupied)));
        }
        String path = before.keySet().iterator().next();
        List<SourceFile> wrongKind = new ArrayList<>(parse(before));
        wrongKind.removeIf(f -> f.getSourcePath().equals(Path.of(path)));
        wrongKind.add(PlainText.builder().sourcePath(Path.of(path)).text(before.get(path)).build());
        assertThrows(RuntimeException.class, () -> apply(named(), wrongKind));
        var ctx = context(); var provider = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = provider.getInitialValue(ctx); var scanner = provider.getScanner(inventory);
        for (SourceFile input : parse(before)) { scanner.visit(input, ctx); scanner.visit(input, ctx); }
        assertThrows(IllegalStateException.class, () -> provider.generate(inventory, ctx));
        String output = System.getProperty("m3.bits.sdk.output");
        if (output != null && !output.isBlank()) export(emitted, Path.of(output));
    }
    private static void export(Map<String, String> results, Path requested) throws Exception {
        Path root = requested.toAbsolutePath().normalize();
        for (Path p = root; p != null; p = p.getParent()) assertFalse(Files.isSymbolicLink(p), "symlink output");
        assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS), "SDK output must be absent");
        Files.createDirectories(root);
        StringBuilder receipt = new StringBuilder("# actual named SDK Results; qualified before export\n");
        for (Map.Entry<String, String> entry : new TreeMap<>(results).entrySet()) {
            Path file = root.resolve(entry.getKey()).normalize();
            assertTrue(file.startsWith(root) && !file.equals(root));
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            receipt.append(entry.getKey()).append('\t').append(sha(entry.getValue())).append('\n');
        }
        Files.writeString(root.resolve("SDK_RECEIPT.tsv"), receipt.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }
}
