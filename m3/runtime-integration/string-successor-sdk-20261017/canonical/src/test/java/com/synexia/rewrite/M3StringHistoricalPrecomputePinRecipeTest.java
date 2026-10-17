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
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual SDK scheduling plus only the three emitted pure mapping regressions. */
final class M3StringHistoricalPrecomputePinRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3StringHistoricalPrecomputePinRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3StringHistoricalPrecomputePinRecipeTest.class::getResourceAsStream,
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
        var run = new M3StringHistoricalPrecomputePinRecipe().run(
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
            var run = new M3StringHistoricalPrecomputePinRecipe().run(
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

    @Test void emittedFocusedPythonRegressionsExposeBaselineAndAcceptCandidate(
            @TempDir Path temporary) throws Exception {
        var result = apply(before());
        assertEquals(5, result.changes());
        for (var target : manifest().targets()) {
            Path output = temporary.resolve(target.path());
            Files.createDirectories(output.getParent());
            Files.writeString(output, result.files().get(target.path()), StandardCharsets.UTF_8);
            if (target.before().equals(target.after())) {
                assertEquals(before().get(target.path()), result.files().get(target.path()));
            }
        }
        Path checker = temporary.resolve(
                "m3/synexia-import/current-full-borrow/check_string_phase.py");
        Path test = checker.resolveSibling("test_string_phase_successors.py");
        Files.writeString(checker, before().get(
                "m3/synexia-import/current-full-borrow/check_string_phase.py"),
                StandardCharsets.UTF_8);
        String baseline = python(test, temporary.resolve("baseline.log"), 1);
        assertTrue(baseline.contains("FAIL: test_precompute_mapping_refuses_forged_historical_target"),
                baseline);
        assertTrue(baseline.contains("FAILED (failures=8)"), baseline);
        Files.writeString(checker, result.files().get(
                "m3/synexia-import/current-full-borrow/check_string_phase.py"),
                StandardCharsets.UTF_8);
        String candidate = python(test, temporary.resolve("candidate.log"), 0);
        assertTrue(candidate.contains("Ran 3 tests"), candidate);
        assertTrue(candidate.matches("(?s).*\\nOK\\s*"), candidate);
        Path compatibility = temporary.resolve(
                "m3/compatibility/test_synexia_string_phase_checker_successor.py");
        String custody = python(compatibility, temporary.resolve("custody.log"), 0);
        assertTrue(custody.contains("Ran 8 tests"), custody);
        assertTrue(custody.matches("(?s).*\\nOK\\s*"), custody);
        // Whole phase main, older successor tests and Java/JNI are independent held gates.
    }

    private static String python(Path test, Path log, int expectedExit) throws Exception {
        var command = new ArrayList<String>(List.of(
                System.getProperty("m3.test.python", "python3"), "-I", "-S", "-B",
                test.toString(), "-v"));
        if (test.getFileName().toString().equals("test_string_phase_successors.py")) {
            command.addAll(List.of(
                    "StringTargetSuccessorTest.test_precompute_mapping_keeps_historical_and_live_pins_distinct",
                    "StringTargetSuccessorTest.test_precompute_mapping_refuses_forged_historical_target",
                    "StringTargetSuccessorTest.test_precompute_mapping_refuses_missing_and_duplicate_cache_rows"));
        }
        var process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "owned Python did not terminate");
            throw new AssertionError("focused Python regression timed out");
        }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        assertEquals(expectedExit, process.exitValue(), output);
        return output;
    }
}
