/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;
import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Named source recipes only; owner-module and receiver execution remain separate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3JoinedRangeDirectoryRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/joined-range-directory-20261020/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/joined-range-directory-20261020.yml";
    private static final Set<String> CHANGE = Set.of(
            "m3/core/src/com/m3/text/JoinedM3StringPiece.java");
    private static final Set<String> KEEP = Set.of(
            "m3/core/src/com/m3/text/M3StringPiece.java",
            "m3/core/src/com/m3/text/LocalM3StringPiece.java",
            "m3/core/src/com/m3/text/LocalM3Arena.java",
            "m3/core/src/com/m3/text/StorageIdentity.java");
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static int forwardApplies;
    private static int inverseApplies;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3JoinedRangeDirectoryRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(String direction) {
        assertTrue(direction.equals("forward") || direction.equals("inverse"));
        return M3GpuToolJoinManifest.load(ROOT + direction + "/",
                M3JoinedRangeDirectoryRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        var forward = manifest("forward");
        var inverse = manifest("inverse");
        assertEquals(5, forward.targets().size());
        assertEquals(5, inverse.targets().size());
        Set<String> all = new HashSet<>(CHANGE);
        all.addAll(KEEP);
        assertEquals(5, all.size());
        assertEquals(all, forward.targets().stream().map(M3GpuToolJoinManifest.Target::path)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(all, inverse.targets().stream().map(M3GpuToolJoinManifest.Target::path)
                .collect(java.util.stream.Collectors.toSet()));
        for (String direction : List.of("forward", "inverse")) {
            var current = manifest(direction);
            assertEquals(CHANGE, current.targets().stream()
                    .filter(row -> !row.before().equals(row.after()))
                    .map(M3GpuToolJoinManifest.Target::path)
                    .collect(java.util.stream.Collectors.toSet()));
            for (var row : current.targets()) {
                assertFalse(row.addition() || row.removal(), row.path());
                String before = contents(direction + "/before/" + row.path());
                String after = contents(direction + "/out/" + row.path());
                assertEquals(row.before(), M3GpuToolJoinManifest.sha256(before), row.path());
                assertEquals(row.after(), M3GpuToolJoinManifest.sha256(after), row.path());
                if (KEEP.contains(row.path())) {
                    assertNull(row.text(), "KEEP has no replacement payload: " + row.path());
                    assertTrue(before.equals(after), "KEEP bytes differ: " + row.path());
                } else {
                    assertTrue(after.equals(row.text()), "sealed output differs: " + row.path());
                }
            }
        }
        for (var row : forward.targets()) {
            var reverse = inverse.target(row.path());
            assertNotNull(reverse, row.path());
            assertEquals(row.after(), reverse.before(), row.path());
            assertEquals(row.before(), reverse.after(), row.path());
            assertTrue(contents("forward/before/" + row.path()).equals(
                    contents("inverse/out/" + row.path())), "full inverse differs: " + row.path());
            assertTrue(contents("forward/out/" + row.path()).equals(
                    contents("inverse/before/" + row.path())), "full inverse preimage differs: " + row.path());
        }
    }
    private static Map<String, String> before(String direction) throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var row : manifest(direction).targets()) {
            input.put(row.path(), contents(direction + "/before/" + row.path()));
        }
        input.put("opaque/Unrelated.txt", "Proof-host and runtime-looking text stays opaque here\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) {
            result.add(PlainText.builder().sourcePath(Path.of(entry.getKey()))
                    .text(entry.getValue()).build());
        }
        return result;
    }
    private static Recipe named(String direction) throws IOException {
        String name = direction.equals("forward")
                ? "com.synexia.m3.JoinedRangeDirectory" : "com.synexia.m3.JoinedRangeDirectoryInverse";
        Class<?> adapter = direction.equals("forward")
                ? M3JoinedRangeDirectoryRecipe.class : M3JoinedRangeDirectoryInverseRecipe.class;
        try (var stream = M3JoinedRangeDirectoryRecipeTest.class.getResourceAsStream(DESCRIPTOR)) {
            assertNotNull(stream, DESCRIPTOR);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + DESCRIPTOR), new Properties(),
                    M3JoinedRangeDirectoryRecipe.class.getClassLoader())).build();
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe recipe = environment.activateRecipes(name);
            assertEquals(1, recipe.getRecipeList().size());
            Recipe declaration = recipe.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            assertEquals(List.of(adapter),
                    declaration.getRecipeList().stream().map(Object::getClass).toList());
            return recipe;
        }
    }
    private static Applied apply(Map<String, String> input, String direction, boolean reverse)
            throws IOException {
        var original = new LinkedHashMap<>(input);
        var errors = new ArrayList<Throwable>();
        var run = named(direction).run(new InMemoryLargeSourceSet(sources(input, reverse)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var results = run.getChangeset().getAllResults();
        for (var result : results) {
            assertNotNull(result.getAfter());
            output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        assertTrue(original.equals(input), "proposal mutated its source input");
        if (direction.equals("forward")) forwardApplies++; else inverseApplies++;
        return new Applied(output, results.size());
    }
    private static void exerciseStates(String direction) throws IOException {
        validateComposition();
        var targets = manifest(direction).targets().stream().toList();
        var changed = targets.stream().filter(row -> !row.before().equals(row.after())).toList();
        assertEquals(1, changed.size());
        // Exact independent before/after tuples are admitted by the existing kernel.
        // Prove every partial tuple converges; do not claim blanket mixed-state refusal.
        for (int state = 0; state < 2; state++) {
            var input = before(direction);
            for (int bit = 0; bit < changed.size(); bit++) {
                if ((state & (1 << bit)) != 0) {
                    var target = changed.get(bit);
                    input.put(target.path(), target.text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var first = apply(input, direction, reverse);
                assertEquals(1 - Integer.bitCount(state), first.changes());
                assertEquals(input.keySet(), first.files().keySet());
                for (var row : targets) {
                    assertEquals(row.after(), M3GpuToolJoinManifest.sha256(
                            first.files().get(row.path())), row.path());
                    assertTrue(contents(direction + "/out/" + row.path()).equals(
                            first.files().get(row.path())), "exact output differs: " + row.path());
                }
                assertEquals(input.get("opaque/Unrelated.txt"), first.files().get("opaque/Unrelated.txt"));
                var second = apply(first.files(), direction, !reverse);
                assertEquals(0, second.changes());
                assertTrue(first.files().equals(second.files()), "fixed point changed source bytes");
                if (state == 0 && !reverse) {
                    for (var row : changed) retain(direction, row.path(), first.files().get(row.path()));
                }
            }
        }
    }

    @Test void allTwoPartialStatesBothOrdersReachExactFixedPoint() throws Exception {
        exerciseStates("forward");
        actualEmittedFreshOwnershipProof();
        PASSED.add("allTwoPartialStatesBothOrdersReachExactFixedPoint");
    }
    @Test void allTwoInverseStatesBothOrdersRecoverExactBefore() throws Exception {
        exerciseStates("inverse");
        PASSED.add("allTwoInverseStatesBothOrdersRecoverExactBefore");
    }
    @Test void eachOwnedInputRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        validateComposition();
        for (String direction : List.of("forward", "inverse")) {
            for (var row : manifest(direction).targets()) {
                var drift = before(direction);
                drift.put(row.path(), drift.get(row.path()) + "\nunknown source drift\n");
                reject(sources(drift, false), direction, "source drift: " + row.path());
                var missing = before(direction);
                missing.remove(row.path());
                reject(sources(missing, true), direction, "required source missing: " + row.path());
                var duplicate = sources(before(direction), false);
                duplicate.add(PlainText.builder().sourcePath(Path.of(row.path()))
                        .text(before(direction).get(row.path())).build());
                reject(duplicate, direction, "duplicate source target: " + row.path());
            }
        }
        PASSED.add("eachOwnedInputRefusesDriftMissingAndDuplicateOwnership");
    }
    private static void reject(List<SourceFile> input, String direction, String signature)
            throws IOException {
        var original = input.stream().map(file -> file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named(direction).run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty(), "refusal produced output");
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
        assertTrue(original.equals(input.stream()
                .map(file -> file.getSourcePath() + "\n" + file.printAll()).toList()),
                "refusal changed original sources");
        refusedRuns++;
    }
    private static void retain(String direction, String name, String text) throws IOException {
        Path relative = Path.of(name);
        assertFalse(relative.isAbsolute());
        assertEquals(relative, relative.normalize());
        assertTrue(direction.equals("forward") || direction.equals("inverse"));
        Path root = Path.of("target/emitted", direction).toAbsolutePath().normalize();
        Path output = root.resolve(relative).normalize();
        assertTrue(output.startsWith(root));
        for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        Files.createDirectories(output.getParent());
        for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        Files.writeString(output, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static URLClassLoader compileOwnershipProof(String label, Map<String, String> owners) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "actual Java21 compiler required");
        assertEquals(21, Runtime.version().feature());
        Path work = Path.of(System.getProperty("m3.fixture.root")).resolve("target/fresh-owned-javac-" + label);
        assertFalse(Files.exists(work), "fresh Java fixture required");
        Files.createDirectories(work);
        Path source = Files.createDirectory(work.resolve("src"));
        Path classes = Files.createDirectory(work.resolve("classes"));
        Path empty = Files.createDirectory(work.resolve("empty-classpath"));
        List<Path> paths = new ArrayList<>();
        for (String name : List.of("JoinedM3StringPiece", "M3StringPiece", "LocalM3StringPiece", "LocalM3Arena", "StorageIdentity")) {
            Path file = source.resolve(name + ".java");
            Files.writeString(file, owners.get("m3/core/src/com/m3/text/" + name + ".java"),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            paths.add(file);
        }
        Path proof = source.resolve("M3JoinedFreshRangeProof.java");
        Files.writeString(proof, contents("proof/M3JoinedFreshRangeProof.java.txt"),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        paths.add(proof);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var units = manager.getJavaFileObjectsFromPaths(paths);
            assertEquals(Boolean.TRUE, compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                            "-classpath", empty.toString(), "-d", classes.toString()), null, units).call(),
                    diagnostics.getDiagnostics().toString());
        }
        byte[] bytes = Files.readAllBytes(classes.resolve("com/m3/text/JoinedM3StringPiece.class"));
        assertTrue(bytes.length > 8);
        assertEquals(65, ((bytes[6] & 255) << 8) | (bytes[7] & 255));
        return new URLClassLoader(new URL[]{classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }
    private static String runOwnershipProof(ClassLoader loader) throws ReflectiveOperationException {
        return (String) loader.loadClass("com.m3.text.M3JoinedFreshRangeProof").getMethod("run").invoke(null);
    }
    private static String replaceOnce(String source, String oldText, String newText) {
        assertEquals(1, source.split(java.util.regex.Pattern.quote(oldText), -1).length - 1);
        return source.replace(oldText, newText);
    }
    private static void actualEmittedFreshOwnershipProof() throws Exception {
        String owner = "m3/core/src/com/m3/text/JoinedM3StringPiece.java";
        var qualifiedBefore = before("forward");
        var candidate = new LinkedHashMap<>(qualifiedBefore);
        Path emitted = Path.of(System.getProperty("m3.fixture.root")).resolve("target/emitted/forward").resolve(owner);
        String actual = Files.readString(emitted, StandardCharsets.UTF_8);
        assertEquals(contents("forward/out/" + owner), actual, "actual forward recipe emission required");
        candidate.put(owner, actual);
        var identityMutant = new LinkedHashMap<>(candidate);
        identityMutant.put(owner, replaceOnce(actual, "        if (start == end)",
                "        if (start == 0 && end == length) return this;\n        if (start == end)"));
        var endsMutant = new LinkedHashMap<>(candidate);
        endsMutant.put(owner, replaceOnce(actual, "rangeEnds[i - first] = b - start;", "rangeEnds[i - first] = b;"));
        try (URLClassLoader before = compileOwnershipProof("qualified-range-before", qualifiedBefore);
             URLClassLoader after = compileOwnershipProof("after", candidate);
             URLClassLoader badIdentity = compileOwnershipProof("identity-mutant", identityMutant);
             URLClassLoader badEnds = compileOwnershipProof("ends-mutant", endsMutant)) {
            assertEquals("JOINED_FRESH_RANGE_PASS fixtures=5 ranges=466 nested=466 freshResults=1864 invalidRanges=25 indexRefusals=3738 aliasMutations=3", runOwnershipProof(before));
            assertEquals("JOINED_FRESH_RANGE_PASS fixtures=5 ranges=466 nested=466 freshResults=1864 invalidRanges=25 indexRefusals=3738 aliasMutations=3", runOwnershipProof(after));
            var identityFailure = assertThrows(InvocationTargetException.class, () -> runOwnershipProof(badIdentity));
            assertInstanceOf(AssertionError.class, identityFailure.getCause());
            assertEquals("fresh joined result required", identityFailure.getCause().getMessage());
            var endsFailure = assertThrows(InvocationTargetException.class, () -> runOwnershipProof(badEnds));
            assertInstanceOf(AssertionError.class, endsFailure.getCause());
            assertEquals("cumulative ends mismatch", endsFailure.getCause().getMessage());
        }
        Path receipt = Path.of(System.getProperty("m3.fixture.root")).resolve("target/fresh-owned-java-execution.json");
        Files.writeString(receipt, "{\n  \"schema\": \"M3_JOINED_FRESH_OWNERSHIP_FIXTURE_V1\",\n  \"javaRelease\": 21,\n  \"compiledVariants\": 4,\n  \"semanticCohorts\": 2,\n  \"rejectedMutants\": 2,\n  \"qualifiedBeforeSha256\": \"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3\",\n  \"afterSha256\": \"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e\",\n  \"proofSummary\": \"JOINED_FRESH_RANGE_PASS fixtures=5 ranges=466 nested=466 freshResults=1864 invalidRanges=25 indexRefusals=3738 aliasMutations=3\"\n}\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    @AfterEach void retainCountersOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("allTwoPartialStatesBothOrdersReachExactFixedPoint",
                "allTwoInverseStatesBothOrdersRecoverExactBefore",
                "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"), PASSED);
        assertEquals(8, forwardApplies);
        assertEquals(8, inverseApplies);
        assertEquals(30, refusedRuns);
        String id = System.getenv("M3_QUALIFICATION_ID"), head = System.getenv("EXPECTED_SOURCE_SHA");
        assertNotNull(id); assertNotNull(head);
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"));
        assertTrue(head.matches("[0-9a-f]{40}"));
        Path target = Path.of("target").toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS));
        for (Path cursor = target; cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        String json = """
                {
                  "schema": 1,
                  "qualificationId": "%s",
                  "checkoutCommit": "%s",
                  "tests": 3,
                  "successfulRuns": 16,
                  "refusedRuns": 30,
                  "emittedOutputs": 2,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("authoring-recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_JOINED_RANGE_DIRECTORY_RECIPE_COUNTERS successful=16"
                + " refused=30 outputs=2 qualificationId=" + id);
    }
}
