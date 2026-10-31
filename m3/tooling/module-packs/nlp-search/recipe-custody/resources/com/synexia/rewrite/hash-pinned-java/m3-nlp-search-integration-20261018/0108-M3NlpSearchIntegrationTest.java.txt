// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Executes the existing canonical SDK recipes; this class supplies fixtures and target receipts. */
class M3NlpSearchIntegrationTest {
    private static final String ID = recipeId();
    private static final Path CRATE = Path.of(System.getProperty("m3.integration.crate"));
    private record Entry(String path, String before, String after, String template) { }

    private static String recipeId() {
        String id = System.getProperty("m3.integration.id", "m3-nlp-search-integration-20261018");
        if (!id.matches("[a-z0-9][a-z0-9-]{0,79}")) throw new IllegalArgumentException("Invalid recipe ID");
        return id;
    }

    private static List<String> kinds() {
        return switch (System.getProperty("m3.integration.kind", "both")) {
            case "both" -> List.of("java", "text");
            case "java" -> List.of("java");
            case "text" -> List.of("text");
            default -> throw new IllegalArgumentException("Unknown integration recipe kind");
        };
    }

    @BeforeAll static void requireExactExistingCanonicalOwners() throws Exception {
        Path owner = Path.of(System.getProperty("m3.integration.owner"));
        Map<String, String> sources = Map.of(
            "M3HashPinnedJavaSnapshotRecipe.java", "67f7b81126e4736c7b61b444fa6545153a61e23c012975ca796fcee623ab6625",
            "M3HashPinnedTextSnapshotRecipe.java", "fbf1ea9efad784afef5ac201ff7fc324551f77a099ebfe2f79b70c3a17c731ef",
            "M3EditScope.java", "962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab",
            "M3ScopeInference.java", "13660b59f60ec2941ec19e9bc8964ffd4b04689cdc33cd19711fba14b8da09a4");
        for (var source : sources.entrySet()) {
            assertEquals(source.getValue(), sha(Files.readString(owner.resolve("com/synexia/rewrite/" + source.getKey()))),
                    "exact existing Synexia SDK owner: " + source.getKey());
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static String resource(String kind, String name) throws IOException {
        String path = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + ID + "/" + name;
        try (var stream = M3NlpSearchIntegrationTest.class.getResourceAsStream(path)) {
            return new String(Objects.requireNonNull(stream, path).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<Entry> manifest(String kind) throws IOException {
        return resource(kind, "manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1))
                .map(c -> new Entry(c[0], c[1], c[2], c[3])).toList();
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, String> before(String kind) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Entry row : manifest(kind)) {
            assertEquals(row.after, sha(resource(kind, row.template)), "sealed template");
            if (!row.before.equals("ABSENT")) {
                String text = resource(kind, "before-" + row.template);
                assertEquals(row.before, sha(text), "protected exact preimage");
                result.put(row.path, text);
            }
        }
        return result;
    }

    private static List<SourceFile> parse(String kind, Map<String, String> files) {
        if (kind.equals("text")) {
            return files.entrySet().stream().map(e -> (SourceFile) PlainText.builder()
                    .sourcePath(Path.of(e.getKey())).text(e.getValue()).build()).toList();
        }
        var inputs = files.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(inputs, null, context()).toList();
        assertEquals(files.size(), parsed.size(), "complete Java source admission");
        for (SourceFile source : parsed) {
            assertInstanceOf(J.CompilationUnit.class, source);
            assertEquals(files.get(source.getSourcePath().toString()), source.printAll());
        }
        return parsed;
    }

    private static Recipe recipe(String kind) {
        return kind.equals("java") ? new M3HashPinnedJavaSnapshotRecipe(ID)
                : new M3HashPinnedTextSnapshotRecipe(ID);
    }

    private static Map<String, String> execute(String kind, List<SourceFile> input) {
        var result = new TreeMap<String, String>();
        for (var change : recipe(kind).run(new InMemoryLargeSourceSet(input), context(), 1)
                .getChangeset().getAllResults()) {
            SourceFile output = Objects.requireNonNull(change.getAfter());
            assertNull(result.put(output.getSourcePath().toString(), output.printAll()), "unique output");
        }
        return result;
    }

    private static void refuse(String kind, List<SourceFile> files) {
        if (kind.equals("java")) {
            var owner = new M3HashPinnedJavaSnapshotRecipe(ID);
            var state = owner.getInitialValue(context());
            for (SourceFile file : files) owner.getScanner(state).visit(file, context());
            assertThrows(IllegalStateException.class, () -> owner.generate(state, context()));
        } else {
            var owner = new M3HashPinnedTextSnapshotRecipe(ID);
            var state = owner.getInitialValue(context());
            for (SourceFile file : files) owner.getScanner(state).visit(file, context());
            assertThrows(IllegalStateException.class, () -> owner.generate(state, context()));
        }
    }

    private static void qualify(String kind) throws Exception {
        var original = before(kind);
        var inputs = parse(kind, original);
        var after = execute(kind, inputs);
        assertEquals(manifest(kind).size(), after.size(), "all reviewed changes execute");
        for (Entry row : manifest(kind)) assertEquals(row.after, sha(after.get(row.path)));
        assertTrue(execute(kind, parse(kind, after)).isEmpty(), "every-stage exact fixed point");
        var reversed = new ArrayList<>(inputs);
        Collections.reverse(reversed);
        assertEquals(after, execute(kind, reversed), "input order cannot change outputs");
        try (var workers = Executors.newFixedThreadPool(2)) {
            var one = workers.submit(() -> execute(kind, parse(kind, original)));
            var two = workers.submit(() -> execute(kind, parse(kind, original)));
            assertEquals(after, one.get(), "independent execution one");
            assertEquals(after, two.get(), "independent execution two");
        }
        for (Entry row : manifest(kind)) {
            var drift = new TreeMap<>(original);
            String baseline = row.before.equals("ABSENT") ? resource(kind, row.template) : original.get(row.path);
            String decoy = kind.equals("java")
                    ? "\n// class Fake { String text = \"MIndexFile; toString();\"; }\n"
                    : "\n# protected source drift\n";
            drift.put(row.path, baseline + decoy);
            refuse(kind, parse(kind, drift));
            if (!row.before.equals("ABSENT")) {
                var missing = new TreeMap<>(original);
                missing.remove(row.path);
                refuse(kind, parse(kind, missing));
            }
        }
        if (!inputs.isEmpty()) {
            var duplicate = new ArrayList<>(inputs);
            duplicate.add(inputs.getFirst());
            refuse(kind, duplicate);
        }
        Path output = CRATE.resolve("target/receiver");
        for (var entry : after.entrySet()) {
            Path path = output.resolve(entry.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
        }
        Path receipts = CRATE.resolve("target/evidence");
        Files.createDirectories(receipts);
        Files.writeString(receipts.resolve(kind + "-sdk.tsv"),
                "recipe_id\tmanifest_sha256\texecution\tchanges\tfixed_point\treversed\tworkers\tdrift\tmissing\n"
                + ID + "\t" + sha(resource(kind, "manifest.tsv")) + "\tSDK_EXECUTION\t"
                + after.size() + "\tPASS\tPASS\t2\tPASS\tPASS\n");
        Files.writeString(CRATE.resolve("target/test-classpath.txt"), System.getProperty("java.class.path"));
    }

    @Test void javaSdkExecutionFixedPointRefusalAndDeterminism() throws Exception {
        Assumptions.assumeTrue(kinds().contains("java"), "Selected recipe kind");
        qualify("java");
    }
    @Test void controlSdkExecutionFixedPointRefusalAndDeterminism() throws Exception {
        Assumptions.assumeTrue(kinds().contains("text"), "Selected recipe kind");
        qualify("text");
    }

    private static Path admittedTarget(Path root, String relative) {
        assertTrue(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS), "receiver root is not a directory");
        assertFalse(Path.of(relative).isAbsolute(), "absolute receiver path");
        Path result = root.resolve(relative).normalize();
        assertTrue(result.startsWith(root) && !result.equals(root), "receiver path escape");
        Path part = root;
        for (Path component : root.relativize(result)) {
            part = part.resolve(component);
            if (!Files.exists(part, LinkOption.NOFOLLOW_LINKS)) continue;
            assertFalse(Files.isSymbolicLink(part), "symlink receiver component: " + part);
            if (part.equals(result)) {
                assertTrue(Files.isRegularFile(part, LinkOption.NOFOLLOW_LINKS), "non-regular receiver: " + part);
            } else {
                assertTrue(Files.isDirectory(part, LinkOption.NOFOLLOW_LINKS), "non-directory receiver parent: " + part);
            }
        }
        return result;
    }

    private static String observed(Path root, String relative) throws Exception {
        Path path = admittedTarget(root, relative);
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS) ? sha(Files.readString(path)) : "ABSENT";
    }

    @Test void receiverPreflightRejectsUnsafeFilesystemShapes(@TempDir Path directory) throws Exception {
        Path root = directory.toRealPath();
        Files.writeString(root.resolve("plain"), "retained");
        Files.createDirectory(root.resolve("folder"));
        Files.createSymbolicLink(root.resolve("dangling"), root.resolve("missing"));
        Files.createSymbolicLink(root.resolve("alias"), root.resolve("folder"));
        Files.createSymbolicLink(root.resolve("external"), root.getParent());
        for (String rejected : List.of("../escaped", root.resolve("absolute").toString(),
                "plain/child", "folder", "dangling", "alias/child", "external/child")) {
            assertThrows(AssertionError.class, () -> admittedTarget(root, rejected), rejected);
        }
        assertEquals("ABSENT", observed(root, "new/child"));
        assertThrows(AssertionError.class, () -> admittedTarget(root.resolve("plain"), "child"));
        assertEquals(sha("retained"), observed(root, "plain"));
        assertEquals("retained", Files.readString(root.resolve("plain")));
    }

    @Test void applicationOnlyUsesExecutedOutputsAndSealedTargetPreimages() throws Exception {
        String targetProperty = System.getProperty("m3.integration.target");
        Assumptions.assumeTrue(targetProperty != null, "Explicit target required for application");
        Path target = Path.of(targetProperty).toRealPath();
        Map<String, String> output = new TreeMap<>();
        Map<String, String> receipts = new TreeMap<>();
        Map<String, String> preimages = new TreeMap<>();
        for (String kind : kinds()) {
            var before = before(kind);
            var executed = execute(kind, parse(kind, before));
            for (Entry row : manifest(kind)) {
                String actual = observed(target, row.path);
                assertTrue(actual.equals(row.before) || actual.equals(row.after), "target drift: " + row.path);
                assertEquals(row.after, sha(executed.get(row.path)));
                output.put(row.path, executed.get(row.path));
                receipts.put(row.path, actual + "\t" + row.after);
                preimages.put(row.path, actual);
            }
        }
        // Whole packet preflight has succeeded. Only SDK-emitted, independently hashed values are written.
        for (var entry : output.entrySet()) {
            Path path = admittedTarget(target, entry.getKey());
            String current = observed(target, entry.getKey());
            assertEquals(preimages.get(entry.getKey()), current, "receiver changed after preflight: " + entry.getKey());
            if (current.equals(sha(entry.getValue()))) continue;
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
            assertEquals(sha(entry.getValue()), sha(Files.readString(path)), "receiving-tree readback");
        }
        Path receipt = CRATE.resolve("target/evidence/application.tsv");
        Files.createDirectories(receipt.getParent());
        var rows = new ArrayList<String>();
        rows.add("recipe_id\ttarget\tobserved_preimage\texecuted_postimage");
        receipts.forEach((path, hashes) -> rows.add(ID + "\t" + path + "\t" + hashes));
        Files.write(receipt, rows, StandardCharsets.UTF_8);
    }
}
