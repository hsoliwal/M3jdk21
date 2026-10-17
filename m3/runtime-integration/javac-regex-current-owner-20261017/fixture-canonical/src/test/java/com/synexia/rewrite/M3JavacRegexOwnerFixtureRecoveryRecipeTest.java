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
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Add-only omitted fixture recovery; the original three recipe tests are compiled unchanged. */
final class M3JavacRegexOwnerFixtureRecoveryRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/";
    private static final String RECEIVING = "synexia-openrewrite-recipes/src/main/resources/"
            + "com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt";
    private static final String OWNER = "synexia-mindex/compiler/src/main/java/"
            + "com/synexia/mindex/compiler/MIndexCompilerASTPrecompute.java";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3JavacRegexOwnerFixtureRecoveryRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3JavacRegexOwnerFixtureRecoveryRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            if (!target.addition()) {
                input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
            }
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
                .build().activateRecipes("com.synexia.m3.JavacRegexOwnerFixtureRecovery");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.JavacRegexOwnerFixtureRecovery",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3JavacRegexOwnerFixtureRecoveryRecipe.class),
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


    @Test void fixtureRecoveryAddsExactOwnerAndReachesFixedPoint() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(5, targets.size());
        var additions = targets.stream().filter(M3GpuToolJoinManifest.Target::addition).toList();
        assertEquals(1, additions.size());
        var target = additions.get(0);
        assertEquals(RECEIVING, target.path());
        assertEquals("ABSENT", target.before());
        assertTrue(!target.removal());
        String owner = before().get(OWNER);
        assertNotNull(owner);
        assertEquals("74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834",
                M3GpuToolJoinManifest.sha256(owner));
        assertEquals(owner, target.text(), "recover the whole owner, never a narrowed stub");
        assertEquals(owner, contents("out/compiler-owner-before.java.txt"));
        try (var stream = M3JavacRegexOwnerFixtureRecoveryRecipeTest.class.getResourceAsStream(
                "/com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt")) {
            assertNotNull(stream, "the original three JUnit tests need the exact restored fixture");
            assertEquals(owner, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        String originalTestPath = "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/"
                + "M3JavacConstantRegexPrecomputeRecipeTest.java";
        String compiledOriginal = Files.readString(
                Path.of(System.getProperty("m3.fixture.root"))
                        .resolve("src/test/java/com/synexia/rewrite/"
                                + "M3JavacConstantRegexPrecomputeRecipeTest.java"),
                StandardCharsets.UTF_8);
        assertEquals(before().get(originalTestPath), compiledOriginal,
                "the actual original three JUnit test bodies must remain byte exact");
        int custody = 0;
        for (var owned : targets) {
            if (owned.addition()) continue;
            custody++;
            assertEquals(owned.before(), owned.after());
            assertEquals(owned.before(), M3GpuToolJoinManifest.sha256(
                    contents("before/" + Path.of(owned.path()).getFileName())));
            assertEquals(contents("before/" + Path.of(owned.path()).getFileName()), owned.text());
        }
        assertEquals(4, custody);
        for (int state = 0; state < 2; state++) {
            var input = before();
            if (state == 1) input.put(target.path(), target.text());
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(1 - state, result.changes());
                var expected = new LinkedHashMap<>(input);
                expected.put(target.path(), owner);
                assertEquals(expected, result.files(),
                        "only the omitted fixture may be added; every prior input remains exact");
                assertEquals(0, apply(result.files()).changes());
                if (state == 0 && !reverse) retain(target.path(), result.files().get(target.path()));
            }
        }
    }

    @Test void fixtureRecoveryRefusesAllForeignAndAmbiguousInputs() throws Exception {
        int refused = 0;
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign fixture source\n");
            reject(sources(input), "source drift: " + target.path());
            refused++;
            if (!target.addition()) {
                input = before();
                input.remove(target.path());
                reject(sources(input), "required source missing: " + target.path());
                refused++;
            }
            var duplicate = sources(before());
            if (target.addition()) {
                duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                        .text(target.text()).build());
            }
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(target.text()).build());
            reject(duplicate, "duplicate source target: " + target.path());
            refused++;
        }
        assertEquals(14, refused, "five drift, five duplicate and four required KEEP refusals");
        // Absence of the sole ADD target is legal and is exercised by the generation test.
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
        Path output = Path.of(System.getProperty("m3.fixture.root")).toAbsolutePath()
                .resolve("target/emitted/javac-regex-owner-fixture-recovery").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
