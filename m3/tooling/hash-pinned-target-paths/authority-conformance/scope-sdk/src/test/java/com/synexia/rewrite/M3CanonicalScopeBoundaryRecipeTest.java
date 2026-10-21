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
import java.lang.reflect.*;
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

/** Named recipe replay and actual emitted Java21 regressions; product model gates remain separate. */
@Execution(ExecutionMode.SAME_THREAD)
final class M3CanonicalScopeBoundaryRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/canonical-scope-boundary-20261020/";
    private static final String DESCRIPTOR = "/META-INF/rewrite/canonical-scope-boundary-20261020";
    private static final String NAME = "com.synexia.m3.CanonicalScopeBoundary";
    private static final String FORWARD_MANIFEST_SHA = "b60d40fc62a17b7d390bd41ca845db9e4ed3f50eb1596bed1a81977f24192826";
    private static final String INVERSE_MANIFEST_SHA = "88a922854f807a8c577ae4924cb97aebc5b9ac33a6f7498f06414f97cca548f5";
    private static final String FORWARD_DESCRIPTOR_SHA = "4701bbfbcc380a6169505987e892cf319956d648e8e2374805b93043e080a3e3";
    private static final String INVERSE_DESCRIPTOR_SHA = "0bf4d59620ca95f6b6192e791810d7a81f3f269a6a51f271f96412b46aa74486";
    private static final String SCOPE_OWNER = "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3ScopeInference.java";
    private static final String C_AFTER = "e95b11ebad61c551bc25df3276fa560a613d476bcee2d865ac272bd35bdade9d";
    private static final String HISTORICAL = ROOT + "historical/M3ScopeInference.java.txt";
    private static final String HISTORICAL_SHA = "ff929c5e4b4ad7a579b69034479294f37a3cbaac36127b6524d02f60049c068d";
    private static final String DIVERGENT = ROOT + "divergent/M3ScopeInference.java.txt";
    private static final String DIVERGENT_SHA = "13660b59f60ec2941ec19e9bc8964ffd4b04689cdc33cd19711fba14b8da09a4";
    private static final Map<String, String> BEFORE = Map.of(
            "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3EditScope.java", "962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab",
            SCOPE_OWNER, "9a6c54f42564c0ef82de3f40a2de17e6598569916c15255653bf79fbe3353579");
    private static final Set<String> PASSED = new LinkedHashSet<>();
    private static final Map<Boolean, String> EMITTED = new HashMap<>();
    private static int successfulRuns;
    private static int refusedRuns;
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String path) throws IOException {
        try (var stream = M3CanonicalScopeBoundaryRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest(boolean inverse) {
        return M3GpuToolJoinManifest.load(ROOT + (inverse ? "inverse/" : ""),
                M3CanonicalScopeBoundaryRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static void validateComposition() throws IOException {
        for (boolean inverse : new boolean[]{false, true}) {
            String manifestPath = ROOT + (inverse ? "inverse/" : "") + "manifest.tsv";
            assertEquals(inverse ? INVERSE_MANIFEST_SHA : FORWARD_MANIFEST_SHA,
                    M3GpuToolJoinManifest.sha256(resource(manifestPath)));
            var rows = manifest(inverse).targets();
            assertEquals(2, rows.size());
            var seen = new HashSet<String>();
            int changed = 0;
            for (var row : rows) {
                assertTrue(seen.add(row.path()), "duplicate manifest owner");
                boolean replace = row.path().equals(SCOPE_OWNER);
                String oldHash = inverse && replace ? C_AFTER : BEFORE.get(row.path());
                String newHash = !inverse && replace ? C_AFTER : BEFORE.get(row.path());
                assertEquals(oldHash, row.before(), row.path());
                assertEquals(newHash, row.after(), row.path());
                assertFalse(row.addition() || row.removal(), row.path());
                String before = resource(ROOT + "before/" + row.path());
                String after = resource(ROOT + "out/" + row.path());
                assertEquals(BEFORE.get(row.path()), M3GpuToolJoinManifest.sha256(before));
                assertEquals(replace ? C_AFTER : BEFORE.get(row.path()),
                        M3GpuToolJoinManifest.sha256(after));
                if (replace) {
                    changed++;
                    assertNotNull(row.text(), row.path());
                    assertEquals(inverse ? before : after, row.text());
                } else {
                    assertNull(row.text(), "KEEP has no replacement payload");
                    assertEquals(before, after, row.path());
                }
            }
            assertEquals(1, changed);
            assertEquals(BEFORE.keySet(), seen);
        }
        assertEquals(resource(ROOT + "before/" + SCOPE_OWNER),
                resource(ROOT + "inverse/out/" + SCOPE_OWNER));
    }
    private static Map<String, String> state(boolean after) throws IOException {
        var files = new LinkedHashMap<String, String>();
        for (var row : manifest(false).targets()) {
            files.put(row.path(), resource(ROOT + (after ? "out/" : "before/") + row.path()));
        }
        files.put("opaque/Unrelated.txt", "Scope Java-looking text remains opaque\n");
        return files;
    }
    private static Recipe named(boolean inverse) throws IOException {
        String descriptor = DESCRIPTOR + (inverse ? "-inverse" : "") + ".yml";
        assertEquals(inverse ? INVERSE_DESCRIPTOR_SHA : FORWARD_DESCRIPTOR_SHA,
                M3GpuToolJoinManifest.sha256(resource(descriptor)));
        try (var stream = M3CanonicalScopeBoundaryRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(stream, descriptor);
            var environment = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + descriptor), new Properties(),
                    M3CanonicalScopeBoundaryRecipe.class.getClassLoader())).build();
            String name = NAME + (inverse ? ".Inverse" : "");
            assertThrows(RecipeException.class, () -> environment.activateRecipes(name + ".Unknown"));
            Recipe activated = environment.activateRecipes(name);
            assertEquals(1, activated.getRecipeList().size());
            Recipe declaration = activated.getRecipeList().get(0);
            assertEquals(name, declaration.getName());
            assertEquals(DeclarativeRecipe.class, declaration.getClass());
            Class<?> expected = inverse ? M3CanonicalScopeBoundaryInverseRecipe.class
                    : M3CanonicalScopeBoundaryRecipe.class;
            assertEquals(List.of(expected), declaration.getRecipeList().stream().map(Object::getClass).toList());
            return activated;
        }
    }
    private static List<SourceFile> sources(Map<String, String> input, boolean reverse) {
        var entries = new ArrayList<>(input.entrySet());
        if (reverse) Collections.reverse(entries);
        var result = new ArrayList<SourceFile>();
        for (var entry : entries) {
            result.add(PlainText.builder().sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build());
        }
        return result;
    }
    private static Applied apply(Map<String, String> input, boolean reverse, boolean inverse) throws IOException {
        var original = new LinkedHashMap<>(input);
        var files = sources(input, reverse);
        var snapshot = files.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        var run = named(inverse).run(new InMemoryLargeSourceSet(files),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var results = run.getChangeset().getAllResults();
        var actual = new LinkedHashMap<>(input);
        for (var result : results) {
            SourceFile before = result.getBefore(), after = result.getAfter();
            assertNotNull(before); assertNotNull(after);
            assertEquals(SCOPE_OWNER, before.getSourcePath().toString().replace('\\', '/'));
            assertEquals(before.getSourcePath(), after.getSourcePath());
            assertEquals(before.getId(), after.getId());
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + SCOPE_OWNER), after.printAll());
            actual.put(SCOPE_OWNER, after.printAll());
            String previous = EMITTED.putIfAbsent(inverse, after.printAll());
            if (previous != null) assertEquals(previous, after.printAll());
        }
        assertEquals(input.get(SCOPE_OWNER).equals(resource(ROOT + (inverse ? "before/" : "out/") + SCOPE_OWNER))
                ? 0 : 1, results.size());
        assertEquals(state(!inverse), actual);
        assertEquals(original, input, "proposal mutated input map");
        assertEquals(snapshot, files.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList());
        successfulRuns++;
        return new Applied(actual, results.size());
    }
    private static void reject(List<SourceFile> input, String signature, boolean inverse) throws IOException {
        var before = input.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList();
        var errors = new ArrayList<Throwable>();
        try {
            var run = named(inverse).run(new InMemoryLargeSourceSet(input),
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
        assertEquals(before, input.stream().map(file -> file.getId() + "\n"
                + file.getSourcePath() + "\n" + file.printAll()).toList());
        refusedRuns++;
    }

    @Test void bothPartialStatesRoundTripAndReachFixedPointsInBothOrders() throws Exception {
        validateComposition();
        for (boolean after : new boolean[]{false, true}) {
            for (boolean reverse : new boolean[]{false, true}) {
                var forward = apply(state(after), reverse, false);
                var fixed = apply(forward.files(), !reverse, false);
                assertEquals(0, fixed.changes());
                var inverse = apply(fixed.files(), reverse, true);
                var inverseFixed = apply(inverse.files(), !reverse, true);
                assertEquals(0, inverseFixed.changes());
                assertEquals(state(false), inverseFixed.files());
            }
        }
        PASSED.add("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders");
    }

    @Test void everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation() throws Exception {
        for (boolean inverse : new boolean[]{false, true}) {
            for (String path : BEFORE.keySet()) {
                var drift = state(inverse);
                drift.put(path, drift.get(path) + "\n/* unknown owner drift */\n");
                reject(sources(drift, false), "source drift: " + path, inverse);
                var missing = state(inverse);
                missing.remove(path);
                reject(sources(missing, true), "required source missing: " + path, inverse);
                var duplicate = sources(state(inverse), false);
                duplicate.add(PlainText.builder().sourcePath(Path.of(path))
                        .text(state(inverse).get(path)).build());
                reject(duplicate, "duplicate source target: " + path, inverse);
            }
        }
        for (String fixture : List.of(HISTORICAL, DIVERGENT)) {
            String text = resource(fixture);
            assertEquals(fixture.equals(HISTORICAL) ? HISTORICAL_SHA : DIVERGENT_SHA,
                    M3GpuToolJoinManifest.sha256(text));
            for (boolean inverse : new boolean[]{false, true}) {
                assertNull(manifest(inverse).target(fixture), "lineage fixture is not a target");
                var input = state(inverse);
                input.put(SCOPE_OWNER, text);
                reject(sources(input, true), "source drift: " + SCOPE_OWNER, inverse);
            }
        }
        PASSED.add("everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation");
    }


    private static int compiledVariants, baselineCounterexamples, acceptedScopeCases;

    private record ScopeCase(String expected, List<String> paths) {}

    private static URLClassLoader compileActual(String label, Map<String, String> actual) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "actual Java 21 compiler required");
        assertEquals(21, Runtime.version().feature(), "qualification must use Java 21");
        Path work = Path.of(System.getProperty("m3.fixture.root")).resolve("target/scope-javac-" + label);
        assertFalse(Files.exists(work), "fresh compiler output required");
        Files.createDirectories(work);
        Path source = Files.createDirectory(work.resolve("src"));
        Path classes = Files.createDirectory(work.resolve("classes"));
        Path empty = Files.createDirectory(work.resolve("empty-classpath"));
        List<Path> paths = new ArrayList<>();
        for (String name : List.of("M3EditScope", "M3ScopeInference")) {
            String owner = "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/" + name + ".java";
            Path file = source.resolve(name + ".java");
            Files.writeString(file, actual.get(owner), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            paths.add(file);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var units = manager.getJavaFileObjectsFromPaths(paths);
            assertEquals(Boolean.TRUE, compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                            "-classpath", empty.toString(), "-d", classes.toString()), null, units).call(),
                    diagnostics.getDiagnostics().toString());
        }
        byte[] bytes = Files.readAllBytes(classes.resolve("com/synexia/rewrite/M3ScopeInference.class"));
        assertTrue(bytes.length > 8);
        assertEquals(65, ((bytes[6] & 255) << 8) | (bytes[7] & 255), "actual Java21 class");
        compiledVariants++;
        return new URLClassLoader(new URL[]{classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }

    private static String infer(Class<?> implementation, List<String> paths) throws ReflectiveOperationException {
        return implementation.getMethod("inferJavaTargets", List.class).invoke(null, paths).toString();
    }

    private static void refuseActual(Class<?> implementation, String path) {
        var failure = assertThrows(InvocationTargetException.class,
                () -> infer(implementation, List.of(path)), path);
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test void actualEmittedClassesPreserveAcceptedScopesAndFixCounterexamples() throws Exception {
        Map<String, String> candidate = apply(state(false), false, false).files();
        Map<String, String> restored = apply(candidate, true, true).files();
        assertEquals(state(false), restored);
        List<ScopeCase> corpus = List.of(
                new ScopeCase("FILE", List.of("src/main/java/p/A.java")),
                new ScopeCase("PACKAGE", List.of("src/main/java/p/A.java", "src/test/java/p/ATest.java")),
                new ScopeCase("PACKAGE", List.of("mod/src/main/java/p/A.java", "mod/src/test/java/p/ATest.java")),
                new ScopeCase("PACKAGE", List.of("src/java.base/share/classes/java/lang/String.java", "src/java.base/share/classes/java/lang/Object.java")),
                new ScopeCase("MODULE", List.of("src/java.base/share/classes/java/lang/String.java", "src/java.base/share/classes/java/util/Map.java")),
                new ScopeCase("MULTI_MODULE", List.of("src/java.base/share/classes/java/lang/String.java", "src/java.logging/share/classes/java/util/logging/Logger.java")),
                new ScopeCase("PACKAGE", List.of("bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/A.java", "bundles/org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/widgets/B.java")),
                new ScopeCase("FILE", List.of("cpullm-java/facet-release/tests/FacetExactCoverageChecks.java")),
                new ScopeCase("PACKAGE", List.of("lucene/core/src/java/org/apache/lucene/A.java", "lucene/core/src/test/org/apache/lucene/ATest.java")),
                new ScopeCase("PACKAGE", List.of("m3/core/src/com/m3/text/A.java", "m3/core/test/com/m3/text/ATest.java")),
                new ScopeCase("MULTI_MODULE", List.of("one/src/main/java/p/A.java", "two/src/main/java/p/B.java")),
                new ScopeCase("PACKAGE", List.of("tests/org.eclipse.swt.tests/ManualTests/org/eclipse/swt/tests/A.java", "tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/B.java")));
        try (URLClassLoader beforeLoader = compileActual("baseline", restored);
             URLClassLoader afterLoader = compileActual("candidate", candidate)) {
            Class<?> before = beforeLoader.loadClass("com.synexia.rewrite.M3ScopeInference");
            Class<?> after = afterLoader.loadClass("com.synexia.rewrite.M3ScopeInference");
            assertEquals(Arrays.stream(before.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers()))
                            .map(Method::toGenericString).sorted().toList(),
                    Arrays.stream(after.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers()))
                            .map(Method::toGenericString).sorted().toList(), "public interface unchanged");
            Class<?> location = afterLoader.loadClass("com.synexia.rewrite.M3ScopeInference$JavaLocation");
            assertEquals(List.of("module", "packageName"),
                    Arrays.stream(location.getRecordComponents()).map(RecordComponent::getName).toList());
            for (ScopeCase item : corpus) {
                assertEquals(item.expected(), infer(before, item.paths()), item.paths().toString());
                assertEquals(item.expected(), infer(after, item.paths()), item.paths().toString());
                for (String path : item.paths()) {
                    Object a = before.getMethod("describeJavaTarget", String.class).invoke(null, path);
                    Object b = after.getMethod("describeJavaTarget", String.class).invoke(null, path);
                    for (String accessor : List.of("module", "packageName"))
                        assertEquals(a.getClass().getMethod(accessor).invoke(a),
                                b.getClass().getMethod(accessor).invoke(b), path);
                }
                acceptedScopeCases++;
            }
            List<String> drivePaths = new ArrayList<>(List.of("C:/src/main/java/p/A.java", "c:/src/main/java/p/A.java", "C:\\src\\main\\java\\p\\A.java"));
            // Fixed-prefix refusal must not depend on LF, CR, NEL, LS or PS in the suffix.
            for (char separator : new char[]{(char) 0x000A, (char) 0x000D, (char) 0x0085, (char) 0x2028, (char) 0x2029}) {
                drivePaths.add("C:/src/main/java/p/A" + separator + ".java");
            }
            assertEquals(8, drivePaths.size());
            for (String path : drivePaths) {
                assertEquals("FILE", infer(before, List.of(path)), "baseline must reproduce observed acceptance");
                baselineCounterexamples++;
                refuseActual(after, path);
            }
            List<String> platformPair = List.of("src/java.base/share/classes/java/lang/String.java", "src/java.base/linux/classes/java/lang/Linux.java");
            assertEquals("PACKAGE", infer(before, platformPair), "baseline must reproduce lost platform boundary");
            baselineCounterexamples++;
            assertEquals("MODULE", infer(after, platformPair));
            for (String path : List.of("", "/src/main/java/p/A.java", "src/main/java/p/../A.java", "../src/main/java/p/A.java")) {
                refuseActual(before, path);
                refuseActual(after, path);
            }
        }
        assertEquals(2, compiledVariants);
        assertEquals(9, baselineCounterexamples);
        assertEquals(12, acceptedScopeCases);
        Path evidence = Path.of(System.getProperty("m3.fixture.root")).resolve("target/scope-java-execution.json");
        Files.writeString(evidence, "{\"schema\":\"M3_SCOPE_JAVA_EXECUTION_V1\",\"compiledVariants\":2,"
                + "\"baselineCounterexamples\":9,\"acceptedScopeCases\":12,\"javaRelease\":21,"
                + "\"beforeSha256\":\"" + BEFORE.get(SCOPE_OWNER) + "\",\"afterSha256\":\"" + C_AFTER + "\"}\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        PASSED.add("actualEmittedClassesPreserveAcceptedScopesAndFixCounterexamples");
    }

    @AfterEach void retainAuthenticOutputsOnlyAfterAllNamedMethodsPass() throws IOException {
        if (PASSED.size() < 3) return;
        assertEquals(Set.of("bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
                "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
                "actualEmittedClassesPreserveAcceptedScopesAndFixCounterexamples"), PASSED);
        assertEquals(18, successfulRuns);
        assertEquals(16, refusedRuns);
        assertEquals(2, compiledVariants);
        assertEquals(9, baselineCounterexamples);
        assertEquals(12, acceptedScopeCases);
        String id = System.getenv("M3_QUALIFICATION_ID"), head = System.getenv("EXPECTED_SOURCE_SHA");
        assertNotNull(id); assertNotNull(head);
        assertTrue(id.matches("[A-Za-z0-9_.-]{16,128}"));
        assertTrue(head.matches("[0-9a-f]{40}"));
        Path target = Path.of("target").toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS));
        for (Path cursor = target; cursor != null; cursor = cursor.getParent()) {
            assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
        }
        assertEquals(Set.of(false, true), EMITTED.keySet());
        for (boolean inverse : new boolean[]{false, true}) {
            Path output = target.resolve(inverse ? "inverse-emitted" : "emitted").resolve(SCOPE_OWNER);
            Files.createDirectories(output.getParent());
            for (Path cursor = output.getParent(); cursor != null; cursor = cursor.getParent()) {
                assertFalse(Files.isSymbolicLink(cursor), cursor.toString());
            }
            // Retain the actual SourceFile postimage observed in a successful named recipe run.
            String text = EMITTED.get(inverse);
            assertNotNull(text);
            assertEquals(resource(ROOT + (inverse ? "before/" : "out/") + SCOPE_OWNER), text);
            Files.writeString(output, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        String json = """
                {
                  "schema": 1,
                  "qualificationId": "%s",
                  "checkoutCommit": "%s",
                  "tests": 3,
                  "successfulRuns": 18,
                  "refusedRuns": 16,
                  "emittedOutputs": 2,
                  "sourceOnly": true,
                  "runtimeApplied": false,
                  "phaseAdvanced": false
                }
                """.formatted(id, head);
        Files.writeString(target.resolve("recipe-test-counters.json"), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("M3_CANONICAL_SCOPE_BOUNDARY_RECIPE_COUNTERS successful=18"
                + " refused=16 outputs=2 qualificationId=" + id);
    }
}
