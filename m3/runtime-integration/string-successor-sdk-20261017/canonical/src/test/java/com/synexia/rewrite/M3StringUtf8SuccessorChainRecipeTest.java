/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual named SDK scheduling, serial V3/V4 custody and the complete emitted source-only tests. */
final class M3StringUtf8SuccessorChainRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3StringUtf8SuccessorChainRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3StringUtf8SuccessorChainRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            if (!target.addition()) input.put(target.path(),
                    contents("before/" + Path.of(target.path()).getFileName()));
        }
        input.put("opaque/Unrelated.txt", "fake cursor strings remain opaque\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(
                new InMemoryLargeSourceSet(sources(input)), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var changes = run.getChangeset().getAllResults();
        for (var change : changes) {
            assertNotNull(change.getAfter());
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }

    @Test void everyPartialStateAndBothOrdersReachExactFixedPoint() throws Exception {
        var changed = manifest().targets().stream()
                .filter(target -> !target.before().equals(target.after())).toList();
        assertEquals(5, changed.size());
        for (int state = 0; state < 32; state++) {
            var input = before();
            for (int i = 0; i < changed.size(); i++) {
                if ((state & (1 << i)) != 0) input.put(changed.get(i).path(), changed.get(i).text());
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(5 - Integer.bitCount(state), result.changes());
                for (var target : manifest().targets()) {
                    assertEquals(target.after(),
                            M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
                }
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
            }
        }
    }

    @Test void eachRequiredInputAndAdditionRefusesDriftMissingAndDuplicates() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign source or occupied addition\n");
            reject(sources(input), "source drift: " + target.path());
            if (!target.addition()) {
                input = before();
                input.remove(target.path());
                reject(sources(input), "required source missing: " + target.path());
                var duplicate = sources(before());
                duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                        .text(before().get(target.path())).build());
                reject(duplicate, "duplicate source target: " + target.path());
            } else {
                var duplicate = sources(before());
                for (int copy = 0; copy < 2; copy++) {
                    duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                            .text(target.text()).build());
                }
                reject(duplicate, "duplicate source target: " + target.path());
            }
        }
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(
                    new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }

    private static Recipe namedRecipe() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes("com.synexia.m3.StringUtf8SuccessorChain");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.StringUtf8SuccessorChain",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3StringUtf8SuccessorChainRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return recipe;
    }

    @Test void namedRecipeEmitsSerialHistoryAndCompletePythonSpectrum(
            @TempDir Path temporary) throws Exception {
        var v4Before = before();
        String oldRoot = "/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/";
        var v3Manifest = M3GpuToolJoinManifest.load(oldRoot,
                M3StringUtf8SuccessorChainRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
        var serialBefore = new LinkedHashMap<>(v4Before);
        for (var target : v3Manifest.targets()) {
            if (target.addition()) {
                serialBefore.remove(target.path());
            } else {
                try (var stream = M3StringUtf8SuccessorChainRecipeTest.class.getResourceAsStream(
                        oldRoot + "before/" + Path.of(target.path()).getFileName())) {
                    assertNotNull(stream);
                    serialBefore.put(target.path(),
                            new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        var errors = new ArrayList<Throwable>();
        var v3 = new M3StringHistoricalPrecomputePinRecipe().run(
                new InMemoryLargeSourceSet(sources(serialBefore)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var serialAfter = new LinkedHashMap<>(serialBefore);
        var v3Changes = v3.getChangeset().getAllResults();
        assertEquals(5, v3Changes.size());
        for (var change : v3Changes) {
            assertNotNull(change.getAfter());
            String path = change.getAfter().getSourcePath().toString().replace('\\', '/');
            serialAfter.put(path, change.getAfter().printAll());
            retain("v3", path, change.getAfter().printAll());
        }
        assertEquals(v4Before, serialAfter, "V4 fixtures must be actual V3 SDK output");
        write(temporary, serialAfter);
        Path test = temporary.resolve(
                "m3/synexia-import/current-full-borrow/test_string_phase_successors.py");
        String baseline = python(test, temporary.resolve("v3-baseline.log"), 1);
        assertTrue(baseline.contains("Ran 26 tests"), baseline);
        assertTrue(baseline.contains("FAILED (failures=1, errors=2)"), baseline);
        var result = apply(serialAfter);
        assertEquals(5, result.changes());
        for (var target : manifest().targets()) {
            assertEquals(target.after(),
                    M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
            if (target.before().equals(target.after())) {
                assertEquals(serialAfter.get(target.path()), result.files().get(target.path()));
            } else {
                retain("v4", target.path(), result.files().get(target.path()));
            }
        }
        write(temporary, result.files());
        String candidate = python(test, temporary.resolve("v4-candidate.log"), 0);
        assertTrue(candidate.contains("Ran 26 tests"), candidate);
        assertTrue(candidate.matches("(?s).*\\nOK\\s*"), candidate);
        Path compatibility = temporary.resolve(
                "m3/compatibility/test_synexia_string_phase_checker_successor.py");
        String custody = python(compatibility, temporary.resolve("v4-custody.log"), 0);
        assertTrue(custody.contains("Ran 12 tests"), custody);
        assertTrue(custody.matches("(?s).*\\nOK\\s*"), custody);
        for (String name : List.of("v3-baseline.log", "v4-candidate.log", "v4-custody.log")) {
            retain("logs", name, Files.readString(temporary.resolve(name), StandardCharsets.UTF_8));
        }
        // Whole phase main and all product Java/JNI/jtreg gates remain independent.
    }

    private static void write(Path root, Map<String, String> files) throws IOException {
        for (var entry : files.entrySet()) {
            Path output = root.resolve(entry.getKey());
            Files.createDirectories(output.getParent());
            Files.writeString(output, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void retain(String version, String path, String content) throws IOException {
        Path root = Path.of(System.getProperty("m3.fixture.root")).resolve("target/emitted");
        Path output = root.resolve(version).resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }

    private static String python(Path test, Path log, int expectedExit) throws Exception {
        var command = new ArrayList<String>(List.of(
                System.getProperty("m3.test.python", "python3"), "-I", "-S", "-B",
                test.toString(), "-v"));
        var process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "owned Python did not terminate");
            throw new AssertionError("emitted Python regression timed out");
        }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        assertEquals(expectedExit, process.exitValue(), output);
        return output;
    }
}
