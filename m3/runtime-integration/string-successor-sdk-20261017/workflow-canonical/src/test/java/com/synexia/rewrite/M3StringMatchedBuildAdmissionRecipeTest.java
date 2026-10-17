/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual SDK proof of two existing workflow outputs; no product build is launched. */
final class M3StringMatchedBuildAdmissionRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-string-matched-build-admission-20261017/";
    private static final String RUNTIME = ".github/workflows/mindex-string-backing.yml";
    private static final String RECEIVING = ".github/workflows/m3-first-release-admission.yml";
    private static final String CUSTODY_STEP =
            "      - name: Verify immutable String checker successor custody\n" +
            "        run: python3 -I -S -B m3/compatibility/test_synexia_string_phase_checker_successor.py -v\n";
    private static final String HOLD_STEP =
            "      - name: Require measured matched-JDK output admission\n" +
            "        shell: bash\n" +
            "        run: |\n" +
            "          printf '%s\\n' 'MATCHED_JDK_OUTPUT_NOT_ADMITTED' >&2\n" +
            "          printf '%s\\n' 'No measured larger-output authorization is recorded for configure, build, images, resources and jtreg.' >&2\n" +
            "          printf '%s\\n' 'The bounded 512 MiB SDK reservation does not admit a complete JDK build.' >&2\n" +
            "          exit 1\n";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3StringMatchedBuildAdmissionRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3StringMatchedBuildAdmissionRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
        }
        input.put("opaque/Unrelated.txt", "fake workflow: make images and exit 0 stay opaque\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe namedRecipe() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes("com.synexia.m3.StringMatchedBuildAdmission");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.StringMatchedBuildAdmission",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3StringMatchedBuildAdmissionRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return recipe;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources(input)),
                new InMemoryExecutionContext(errors::add));
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

    @Test void everyPartialStateAndOrderPreservesBothPipelinesAndHoldsFullBuild(
            @TempDir Path temporary) throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(2, targets.size());
        assertEquals(List.of(RECEIVING, RUNTIME), targets.stream()
                .map(M3GpuToolJoinManifest.Target::path).toList());
        for (var target : targets) {
            assertFalse(target.addition());
            assertFalse(target.removal());
            assertFalse(target.before().equals(target.after()));
        }
        for (int state = 0; state < 4; state++) {
            var input = before();
            for (int i = 0; i < targets.size(); i++) {
                if ((state & (1 << i)) != 0) {
                    input.put(targets.get(i).path(), targets.get(i).text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(2 - Integer.bitCount(state), result.changes());
                for (var target : targets) {
                    assertEquals(target.after(),
                            M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
                }
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
                assertPipelinePreservation(result.files());
                if (state == 0 && !reverse) {
                    for (var target : targets) {
                        retain(target.path(), result.files().get(target.path()));
                    }
                }
            }
        }
        // Exercise only the exact printf/exit hold, never configure or any build command.
        Path log = temporary.resolve("matched-build-not-admitted.log");
        String holdScript = HOLD_STEP.substring(
                HOLD_STEP.indexOf("        run: |\n") + "        run: |\n".length()).lines()
                .map(line -> line.substring(10))
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        var process = new ProcessBuilder(System.getProperty("m3.test.shell", "bash"),
                "-c", holdScript).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "owned hold probe did not terminate");
            throw new AssertionError("matched-build hold probe timed out");
        }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        assertEquals(1, process.exitValue(), output);
        assertTrue(output.startsWith("MATCHED_JDK_OUTPUT_NOT_ADMITTED\n"), output);
        assertTrue(output.contains("512 MiB SDK reservation does not admit a complete JDK build."), output);
        retain("matched-build-not-admitted.log", output);
    }

    private static void assertPipelinePreservation(Map<String, String> files) throws IOException {
        String runtime = files.get(RUNTIME);
        assertEquals(1L, runtime.lines().filter(line ->
                line.equals("      - name: Verify immutable String checker successor custody")).count());
        assertEquals(1L, runtime.lines().filter(line ->
                line.equals("      - name: Require measured matched-JDK output admission")).count());
        assertEquals(contents("before/mindex-string-backing.yml"),
                runtime.replace(CUSTODY_STEP, "").replace(HOLD_STEP, ""));
        String oldCustody = "        run: python3 m3/synexia-import/current-full-borrow/check_string_phase.py\n";
        assertTrue(runtime.contains(oldCustody + CUSTODY_STEP), "new custody must follow existing custody");
        String java20 = "      - name: Set up Java 20 boot JDK for OpenJDK 21 build\n";
        assertTrue(runtime.contains(HOLD_STEP + java20), "nonzero hold must precede all full-build setup");
        assertFalse(HOLD_STEP.contains("continue-on-error"));
        assertFalse(HOLD_STEP.contains("if:"));
        assertEquals(2L, runtime.lines().filter(line -> line.startsWith("      - ")).count()
                - contents("before/mindex-string-backing.yml").lines()
                        .filter(line -> line.startsWith("      - ")).count());

        String receiving = files.get(RECEIVING);
        String first = "      - name: Qualify serial String successor repairs with original provider\n";
        String second = "      - name: Retain serial String SDK evidence and emitted files\n";
        String next = "      - name: Java differential proof\n";
        int start = receiving.indexOf(first);
        int end = receiving.indexOf(next, start);
        assertTrue(start >= 0 && end > start, "bounded SDK insertion must precede Java differential");
        String insertion = receiving.substring(start, end);
        assertEquals(2L, insertion.lines().filter(line -> line.startsWith("      - ")).count());
        assertTrue(insertion.contains(second));
        assertEquals(contents("before/m3-first-release-admission.yml"), receiving.replace(insertion, ""));
        assertTrue(insertion.contains("shutil.disk_usage(\".\").free - (512 << 20) >= (8 << 30)"));
        assertTrue(insertion.contains("if-no-files-found: error"));
        assertTrue(insertion.contains("          include-hidden-files: true\n"));
    }

    @Test void eachWorkflowRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign workflow source\n");
            reject(sources(input), "source drift: " + target.path());
            input = before();
            input.remove(target.path());
            reject(sources(input), "required source missing: " + target.path());
            var duplicate = sources(before());
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(before().get(target.path())).build());
            reject(duplicate, "duplicate source target: " + target.path());
        }
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
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

    private static void retain(String path, String content) throws IOException {
        Path output = Path.of(System.getProperty("m3.fixture.root"))
                .resolve("target/emitted/matched-build-admission").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
