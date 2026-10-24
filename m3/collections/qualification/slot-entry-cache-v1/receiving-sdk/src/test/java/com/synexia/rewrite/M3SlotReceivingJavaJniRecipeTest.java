// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Adapted from M3FullJavaJniRecipeTest (4249744f876d8d6b42e1a09d32dbe77f623b9dd2): text-only exact receiving successor.
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.Validated;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.Markers;
import org.openrewrite.text.PlainText;

/** Real OpenRewrite execution against reviewed pre/post resources; never substitutes a text engine. */
final class M3SlotReceivingJavaJniRecipeTest {
    private static final String ID = "m3-slot-receiving-java-jni-20261024";
    private static final String NAME = "com.synexia.m3.SlotReceivingJavaJni20261024";
    private static final String RESOURCE = "/com/synexia/rewrite/hash-pinned-text/" + ID + "/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/" + ID + ".yml";
    private static final String ENGINE_SHA = "c4517401a7defd94ecc621bb2c19773bd6672c8fa3ee965c1ad37a63718bd92a";
    private static final String ENGINE_RELATIVE = "engine/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java";
    private static final Set<String> TARGETS = Set.of(
            ".github/workflows/m3-file-build-impact-20261020.yml",
            ".github/workflows/m3-file-delta-20261020.yml",
            ".github/workflows/m3-native-shadow-stream-progress-20261020.yml",
            ".github/workflows/m3-original-mapping-provider-qualification.yml",
            "m3/collections/qualification/full-java-jni-20261018/verify.py",
            "m3/migration/original-provider-20261017/source-binding.json");

    private record Row(String path, String before, String after, String template) { }
    private record Run(Map<String, SourceFile> files, List<Result> changes) { }

    @Test
    void exactEngineAndManifestAreSealed() throws Exception {
        assertEquals(21, Runtime.version().feature(), "actual SDK JVM must be Java21");
        Path crate = absoluteProperty("m3.sdk.crate");
        Path engine = crate.resolve(ENGINE_RELATIVE);
        assertEquals(ENGINE_SHA, sha(Files.readAllBytes(engine)));
        try (var paths = Files.walk(crate.resolve("engine"))) {
            assertEquals(Set.of(ENGINE_RELATIVE), paths.filter(Files::isRegularFile)
                    .map(crate::relativize).map(M3SlotReceivingJavaJniRecipeTest::normalized)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        Path output = absoluteProperty("m3.sdk.output");
        assertTrue(Files.isSameFile(output.resolve("classes"), Path.of(
                M3HashPinnedTextSnapshotRecipe.class.getProtectionDomain().getCodeSource().getLocation().toURI())));
        assertTrue(Files.isSameFile(output.resolve("test-classes"), Path.of(
                M3SlotReceivingJavaJniRecipeTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())));
        assertEquals(TARGETS, new TreeSet<>(rows().stream().map(Row::path).toList()));
        for (Row row : rows()) {
            assertEquals(row.after(), sha(resource(row.template())));
            if (!row.before().equals("ABSENT")) {
                assertEquals(row.before(), sha(resource("pre-" + row.template())));
            }
            Path receiver = receivingPath(row.path());
            assertEquals(row.after(), sha(Files.readAllBytes(receiver)), row.path());
        }
        assertEquals(TARGETS, Set.copyOf(new M3HashPinnedTextSnapshotRecipe(ID).targetPaths()));
    }

    @Test
    void exactEmissionFixedPointAndReceiverReadback() throws Exception {
        Run first = run(new M3HashPinnedTextSnapshotRecipe(ID), input());
        assertEquals(TARGETS, changedPaths(first));
        assertEquals(expected(), texts(first.files()));
        Run second = run(new M3HashPinnedTextSnapshotRecipe(ID), new ArrayList<>(first.files().values()));
        assertTrue(second.changes().isEmpty(), "actual zero-result fixed point");
        assertEquals(expected(), texts(second.files()));
        Path output = absoluteProperty("m3.sdk.output");
        Path emitted = output.resolve("receiver-sdk-emission");
        assertFalse(Files.exists(emitted), "preserve an existing emission; use a fresh owned output directory");
        Files.createDirectory(emitted);
        StringBuilder receipt = new StringBuilder(
                "# SDK_EMISSION_ONLY; all six actual Surefire methods must also pass\n"
                        + "# target_path\tbefore_sha256\tafter_sha256\temitted_sha256\treceiver_sha256\n");
        for (Row row : rows()) {
            SourceFile actual = Objects.requireNonNull(first.files().get(row.path()));
            byte[] bytes = actual.printAll().getBytes(StandardCharsets.UTF_8);
            assertEquals(row.after(), sha(bytes), row.path());
            assertArrayEquals(bytes, Files.readAllBytes(receivingPath(row.path())), row.path());
            Path destination = emitted.resolve(row.path()).normalize();
            assertTrue(destination.startsWith(emitted));
            Files.createDirectories(destination.getParent());
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            assertArrayEquals(bytes, Files.readAllBytes(destination), row.path());
            receipt.append(row.path()).append('\t').append(row.before()).append('\t')
                    .append(row.after()).append('\t').append(sha(Files.readAllBytes(destination)))
                    .append('\t').append(sha(Files.readAllBytes(receivingPath(row.path())))).append('\n');
        }
        Map<String, String> disk = new TreeMap<>();
        try (var paths = Files.walk(emitted)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                disk.put(normalized(emitted.relativize(path)), Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        assertEquals(expected(), disk, "exact emitted leaf set");
        Run readback = run(new M3HashPinnedTextSnapshotRecipe(ID), files(disk));
        assertTrue(readback.changes().isEmpty(), "emitted-byte zero-result readback");
        assertEquals(disk, texts(readback.files()));
        Files.writeString(output.resolve("receiving-sdk-receipt.tsv"), receipt.toString(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    @Test
    void namedYamlMatchesDirectEngine() {
        Recipe named;
        try (var stream = M3SlotReceivingJavaJniRecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream, DESCRIPTOR);
            var loader = new YamlResourceLoader(stream, URI.create("classpath:" + DESCRIPTOR), new Properties());
            named = Environment.builder().load(loader).build().activateRecipes(NAME);
            assertTrue(named.validateAll().stream().allMatch(Validated::isValid));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Run direct = run(new M3HashPinnedTextSnapshotRecipe(ID), input());
        Run composed = run(named, input());
        assertEquals(TARGETS, changedPaths(composed));
        assertEquals(texts(direct.files()), texts(composed.files()));
        Run replay = run(named, new ArrayList<>(composed.files().values()));
        assertTrue(replay.changes().isEmpty());
        assertEquals(expected(), texts(replay.files()));
    }

    @Test
    void unknownInputRemainsUnchanged() {
        String unknownPath = "docs/m3-slot-sdk-unrelated.txt";
        assertFalse(TARGETS.contains(unknownPath));
        SourceFile unknown = text(unknownPath, "Unrelated owner bytes remain exact.\r\n");
        List<SourceFile> state = input();
        state.add(unknown);
        Run out = run(new M3HashPinnedTextSnapshotRecipe(ID), state);
        assertEquals(TARGETS, changedPaths(out));
        assertSame(unknown, out.files().get(unknownPath));
        assertEquals(unknown.printAll(), out.files().get(unknownPath).printAll());
        Map<String, String> actual = texts(out.files());
        assertEquals(unknown.printAll(), actual.remove(unknownPath));
        assertEquals(expected(), actual);
    }

    @Test
    void everyTargetRefusesDriftMissingAndDuplicate() {
        for (Row row : rows()) {
            List<SourceFile> state = files(expected());
            state.removeIf(file -> name(file).equals(row.path()));
            state.add(text(row.path(), textResource(row.template()) + "\nintentional SDK drift\n"));
            refuse(state, "source drift");
            state = files(expected());
            state.add(text(row.path(), textResource(row.template())));
            refuse(state, "duplicate target");
            state = input();
            state.removeIf(file -> name(file).equals(row.path()));
            if (row.before().equals("ABSENT")) {
                // Absence is the admitted addition precondition, never a fabricated missing-source failure.
                Run addition = run(new M3HashPinnedTextSnapshotRecipe(ID), state);
                assertEquals(row.after(), sha(addition.files().get(row.path()).printAll()
                        .getBytes(StandardCharsets.UTF_8)));
            } else {
                refuse(state, "required source missing");
                state = input();
                state.removeIf(file -> name(file).equals(row.path()));
                state.add(text(row.path(), textResource("pre-" + row.template()) + "\npreimage drift\n"));
                refuse(state, "source drift");
            }
            var recipe = new M3HashPinnedTextSnapshotRecipe(ID);
            ExecutionContext context = context();
            var inventory = recipe.getInitialValue(context);
            for (SourceFile file : files(expected())) recipe.getScanner(inventory).visit(file, context);
            recipe.generate(inventory, context);
            SourceFile late = text(row.path(), textResource(row.template()) + "\nchanged after scan\n");
            IllegalStateException changed = assertThrows(IllegalStateException.class,
                    () -> recipe.getVisitor(inventory).visit(late, context));
            assertTrue(changed.getMessage().contains("target changed after scan"), changed.getMessage());
        }
    }

    @Test
    void replacementRetainsAttributesMarkersAndId() {
        int replaced = 0;
        for (Row selected : rows()) {
            if (selected.before().equals("ABSENT")) continue;
            BuildMetadata marker = new BuildMetadata(UUID.randomUUID(), Map.of("sdk-owner", selected.path()));
            FileAttributes attributes = new FileAttributes(null, null, null, true, true, true,
                    textResource("pre-" + selected.template()).getBytes(StandardCharsets.UTF_8).length);
            SourceFile before = text(selected.path(), textResource("pre-" + selected.template()))
                    .withMarkers(Markers.EMPTY.add(marker))
                    .withFileAttributes(attributes)
                    .withCharset(StandardCharsets.UTF_8)
                    .withCharsetBomMarked(true);
            assertEquals(selected.before(), sha(before.printAll().getBytes(StandardCharsets.UTF_8)));
            List<SourceFile> state = input();
            state.removeIf(file -> name(file).equals(selected.path()));
            state.add(before);
            var recipe = new M3HashPinnedTextSnapshotRecipe(ID);
            ExecutionContext context = context();
            var inventory = recipe.getInitialValue(context);
            for (SourceFile file : state) recipe.getScanner(inventory).visit(file, context);
            recipe.generate(inventory, context);
            SourceFile after = (SourceFile) recipe.getVisitor(inventory).visit(before, context);
            assertNotNull(after);
            assertEquals(selected.after(), sha(after.printAll().getBytes(StandardCharsets.UTF_8)));
            assertEquals(before.getId(), after.getId());
            assertEquals(before.getSourcePath(), after.getSourcePath());
            assertSame(before.getMarkers(), after.getMarkers(), "existing owner visitor preserves all markers");
            assertSame(marker, after.getMarkers().findFirst(BuildMetadata.class).orElseThrow());
            assertSame(attributes, after.getFileAttributes());
            assertEquals(before.getCharset(), after.getCharset());
            assertEquals(before.isCharsetBomMarked(), after.isCharsetBomMarked());
            replaced++;
        }
        assertTrue(replaced > 0, "the actual receiving manifest must exercise replacement metadata");
    }

    private static Run run(Recipe recipe, List<SourceFile> state) {
        List<Result> changes = recipe.run(new InMemoryLargeSourceSet(state), context(), 1)
                .getChangeset().getAllResults();
        Map<String, SourceFile> output = new TreeMap<>();
        for (SourceFile file : state) output.put(name(file), file);
        for (Result change : changes) {
            SourceFile after = Objects.requireNonNull(change.getAfter(), "deletion is not admitted");
            output.put(name(after), after);
        }
        return new Run(output, List.copyOf(changes));
    }

    private static Set<String> changedPaths(Run run) {
        Set<String> paths = new TreeSet<>();
        for (Result change : run.changes()) {
            assertNotNull(change.getAfter());
            assertTrue(paths.add(name(change.getAfter())), "duplicate actual result");
        }
        return paths;
    }

    private static void refuse(List<SourceFile> state, String message) {
        var recipe = new M3HashPinnedTextSnapshotRecipe(ID);
        ExecutionContext context = context();
        var inventory = recipe.getInitialValue(context);
        for (SourceFile file : state) recipe.getScanner(inventory).visit(file, context);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> recipe.generate(inventory, context));
        assertTrue(failure.getMessage().contains(message), failure.getMessage());
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        String previous = "";
        for (String line : textResource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            Row row = new Row(cells[0], cells[1], cells[2], cells[3]);
            assertTrue(TARGETS.contains(row.path()), row.path());
            assertTrue(previous.compareTo(row.path()) < 0, "sorted unique receiving paths");
            assertTrue(row.before().equals("ABSENT") || row.before().matches("[0-9a-f]{64}"));
            assertTrue(row.after().matches("[0-9a-f]{64}"));
            assertNotEquals(row.before(), row.after(), "every selected target must change");
            assertTrue(row.template().matches("[A-Za-z0-9_.-]+"));
            rows.add(row);
            previous = row.path();
        }
        assertEquals(TARGETS, new TreeSet<>(rows.stream().map(Row::path).toList()));
        return List.copyOf(rows);
    }

    private static List<SourceFile> input() {
        List<SourceFile> state = new ArrayList<>();
        for (Row row : rows()) {
            if (row.before().equals("ABSENT")) continue;
            String value = textResource("pre-" + row.template());
            assertEquals(row.before(), sha(value.getBytes(StandardCharsets.UTF_8)));
            state.add(text(row.path(), value));
        }
        return state;
    }

    private static Map<String, String> expected() {
        Map<String, String> output = new TreeMap<>();
        for (Row row : rows()) {
            String value = textResource(row.template());
            assertEquals(row.after(), sha(value.getBytes(StandardCharsets.UTF_8)));
            assertNull(output.put(row.path(), value));
        }
        return output;
    }

    private static List<SourceFile> files(Map<String, String> files) {
        List<SourceFile> result = new ArrayList<>();
        files.forEach((path, content) -> result.add(text(path, content)));
        return result;
    }

    private static Map<String, String> texts(Map<String, SourceFile> files) {
        Map<String, String> output = new TreeMap<>();
        files.forEach((path, source) -> output.put(path, source.printAll()));
        return output;
    }

    private static PlainText text(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static String name(SourceFile file) { return normalized(file.getSourcePath()); }
    private static String normalized(Path path) { return path.toString().replace('\\', '/'); }

    private static Path absoluteProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, name);
        assertFalse(value.isBlank(), name);
        Path path = Path.of(value);
        assertTrue(path.isAbsolute(), name + " must be explicit and absolute");
        return path.normalize();
    }

    private static Path receivingPath(String target) throws IOException {
        Path root = absoluteProperty("m3.receiver.root").toRealPath();
        Path path = root.resolve(target).normalize();
        assertTrue(path.startsWith(root));
        assertTrue(Files.isRegularFile(path), target);
        Path actual = path.toRealPath();
        assertTrue(actual.startsWith(root), "receiving target must stay inside the reviewed source root");
        return actual;
    }

    private static byte[] resource(String name) {
        try (var stream = M3SlotReceivingJavaJniRecipeTest.class.getResourceAsStream(RESOURCE + name)) {
            assertNotNull(stream, name);
            return stream.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String textResource(String name) {
        byte[] bytes = resource(name);
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertArrayEquals(bytes, text.getBytes(StandardCharsets.UTF_8), "lossless UTF-8 resource: " + name);
        return text;
    }

    private static ExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
