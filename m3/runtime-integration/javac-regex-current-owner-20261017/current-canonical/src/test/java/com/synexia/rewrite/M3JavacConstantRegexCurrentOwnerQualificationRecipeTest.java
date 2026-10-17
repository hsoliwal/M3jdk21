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

/** Source-only current-owner qualification recipe scheduling; runtime proof remains separate. */
final class M3JavacConstantRegexCurrentOwnerQualificationRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            if (!target.addition()) {
                input.put(target.path(), contents("inputs/" + Path.of(target.path()).getFileName()));
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
                .build().activateRecipes("com.synexia.m3.JavacConstantRegexCurrentOwnerQualification");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.JavacConstantRegexCurrentOwnerQualification",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3JavacConstantRegexCurrentOwnerQualificationRecipe.class),
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


    @Test void allAdditionStatesAndOrdersReachExactFixedPoint() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(10, targets.size());
        var additions = targets.stream().filter(M3GpuToolJoinManifest.Target::addition).toList();
        assertEquals(4, additions.size());
        int custody = 0;
        for (var target : targets) {
            assertTrue(!target.removal());
            if (target.addition()) {
                assertEquals("ABSENT", target.before());
                assertEquals(target.after(), M3GpuToolJoinManifest.sha256(target.text()));
            } else {
                custody++;
                assertEquals(target.before(), target.after());
                assertEquals(target.before(), M3GpuToolJoinManifest.sha256(
                        contents("inputs/" + Path.of(target.path()).getFileName())));
            }
        }
        assertEquals(6, custody);
        for (int state = 0; state < 16; state++) {
            var input = before();
            for (int index = 0; index < additions.size(); index++) {
                if ((state & (1 << index)) != 0) {
                    var target = additions.get(index);
                    input.put(target.path(), target.text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(4 - Integer.bitCount(state), result.changes());
                var expected = new LinkedHashMap<>(input);
                for (var target : additions) expected.put(target.path(), target.text());
                assertEquals(expected, result.files(),
                        "only four qualification files may be added; all current owners stay exact");
                assertEquals(0, apply(result.files()).changes());
                assertOriginalContractPreserved(input, result.files());
                if (state == 0 && !reverse) {
                    for (var target : additions) retain(target.path(), result.files().get(target.path()));
                }
            }
        }
    }

    @Test void ownershipRefusesDriftMissingDuplicateAndPreservesUnrelated() throws Exception {
        int refused = 0;
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign qualification source\n");
            reject(sources(input), "source drift: " + target.path());
            refused++;
            if (!target.addition()) {
                input = before();
                input.remove(target.path());
                reject(sources(input), "required source missing: " + target.path());
                refused++;
            }
            var duplicate = sources(before());
            String exact = target.addition()
                    ? target.text() : before().get(target.path());
            if (target.addition()) {
                duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                        .text(exact).build());
            }
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(exact).build());
            reject(duplicate, "duplicate source target: " + target.path());
            refused++;
        }
        assertEquals(26, refused, "six KEEP triples plus four ADD foreign/duplicate pairs");
        var input = before();
        assertEquals(input.get("opaque/Unrelated.txt"), apply(input).files().get("opaque/Unrelated.txt"));
    }

    private static void assertOriginalContractPreserved(
            Map<String, String> input, Map<String, String> output) {
        String original = input.get("synexia-indexstring/src/test/java/com/synexia/indexstring/"
                + "MIndexJdkConstantRegexPlanContract.java");
        String current = output.get("synexia-openrewrite-recipes/crates/"
                + "m3-javac-regex-current-owner-qualification-20261017/src/test/java/"
                + "com/synexia/indexstring/MIndexJdkConstantRegexCurrentOwnerContract.java");
        assertNotNull(original);
        assertNotNull(current);
        String marker = "\n    private static long charSequenceChecks;\n";
        int start = current.indexOf(marker);
        int finish = current.lastIndexOf("}\n");
        assertTrue(start >= 0 && start == current.lastIndexOf(marker) && finish > start);
        String restored = current.substring(0, start) + current.substring(finish);
        String call = "        verifyCurrentCharSequences();\n";
        assertTrue(restored.indexOf(call) >= 0
                && restored.indexOf(call) == restored.lastIndexOf(call));
        restored = restored.replace(call, "");
        String currentClass = "public final class MIndexJdkConstantRegexCurrentOwnerContract {";
        String oldClass = "public final class MIndexJdkConstantRegexPlanContract {";
        String currentConstructor = "private MIndexJdkConstantRegexCurrentOwnerContract() {}";
        String oldConstructor = "private MIndexJdkConstantRegexPlanContract() {}";
        for (String declaration : List.of(currentClass, currentConstructor)) {
            assertTrue(restored.indexOf(declaration) >= 0
                    && restored.indexOf(declaration) == restored.lastIndexOf(declaration));
        }
        restored = restored.replace(currentClass, oldClass)
                .replace(currentConstructor, oldConstructor);
        assertEquals(original, restored,
                "all old differential cases and exception-timing checks remain byte exact");
        assertTrue(current.contains("scope=JDK_CHARSEQUENCE_ONLY PASS"));
        // These are recipe/source proofs. They do not execute the contract, MIndex owners or JNI.
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
                .resolve("target/emitted/javac-regex-current-owner-qualification").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
