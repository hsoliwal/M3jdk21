// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/**
 * Bounded application proof using the unchanged canonical snapshot recipe owners.
 *
 * <p>Modified 2026-10-20 from M3NetBeansCollectionAtomsRecipeTest (Git blob
 * 40b52f4d85cc10388e628c462c0ee211ba015645): inventory-driven file phases,
 * scheduler refusal trials, exact dependency custody and one final publication.
 * Recovered 2026-10-20 from the later M3AbseilRefinementRecipeTest descendant,
 * removing only its unrelated dataset/native-composition adaptations. All receipts
 * are generated afresh. Adapted 2026-10-21 from PR12605 at
 * b6c35b9a1f528dd9e5a3d9f6f4c51ca360db43d7 for this distinct packet.
 * Dependency probes are source-sealed and limited to this packet; no old
 * 53-path or seven-event evidence is recovered. Product compiler, behavioral and native runtime gates
 * remain separate authorities.</p>
 */
final class M3IntegratedRefinementRecipeTest {
    private static final String ID = "m3-integrated-refinement-20261021";
    private static final String NAMED = "com.synexia.m3.IntegratedRefinement20261021";
    private static final String PARSER_CLASSPATH =
            "com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath";
    private static final Path CRATE = Path.of(Objects.requireNonNull(System.getProperty("crate"), "crate"))
            .toAbsolutePath().normalize();
    private static final Path TARGET = CRATE.resolve("target");
    private static final Map<String, String> OWNERS = Map.of(
            "M3EditScope.java", "962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab",
            "M3ScopeInference.java", "9a6c54f42564c0ef82de3f40a2de17e6598569916c15255653bf79fbe3353579",
            "M3HashPinnedJavaSnapshotRecipe.java", "083f8b1fd956fe2ed81168bc580d1e0f39a815401c50571d397ddba40150cf63",
            "M3HashPinnedTextSnapshotRecipe.java", "5a64bd502cb8aa7d51b78813b736e37a0a7f420233f405c4d2df08ee2eefe8bc");
    private static List<Path> parserClasspath;

    private record Phase(String id, String kind) { }
    private record Entry(String path, String before, String after, String resource) { }
    private record Trial(Map<String, String> changes, List<Throwable> failures) { }
    private record Proof(Phase phase, Map<String, String> output, String receipt) { }

    @BeforeAll
    static void canonicalOwnerAndClasspathIdentity() throws Exception {
        Files.createDirectories(TARGET);
        Files.writeString(TARGET.resolve("proof-status.tsv"), "status\tproof_sha256\nRUNNING\tPENDING\n");
        assertEquals(21, Runtime.version().feature(), "exact Java 21 proof toolchain");
        Path ownerRoot = CRATE.resolve("../../src/main/java/com/synexia/rewrite").normalize();
        for (var owner : OWNERS.entrySet()) {
            assertEquals(owner.getValue(), sha(Files.readAllBytes(ownerRoot.resolve(owner.getKey()))),
                    "unchanged canonical owner " + owner.getKey());
        }
        String configured = System.getProperty("parser.classpath");
        assertNotNull(configured, "actual compiled production parser classpath is required");
        assertFalse(configured.isBlank() || configured.contains("${"), "unresolved parser.classpath");
        parserClasspath = Arrays.stream(configured.split(java.io.File.pathSeparator, -1))
                .map(value -> {
                    assertFalse(value.isBlank(), "empty parser classpath member");
                    return Path.of(value).toAbsolutePath().normalize();
                }).toList();
        assertEquals(parserClasspath.size(), new HashSet<>(parserClasspath).size(), "duplicate classpath member");
        for (Path path : parserClasspath) {
            assertTrue(Files.exists(path), "missing actual dependency " + path);
            if (Files.isDirectory(path)) {
                try (var contents = Files.walk(path)) {
                    assertTrue(contents.anyMatch(file -> Files.isRegularFile(file)
                            && file.toString().endsWith(".class")), "uncompiled classpath directory " + path);
                }
            } else {
                assertTrue(Files.isRegularFile(path) && path.toString().endsWith(".jar")
                        && Files.size(path) > 0, "compiled dependency JAR required " + path);
            }
        }
        actualDependencyAttribution();
    }

    /** Require attributed calls to real compiled owners for this packet's declared scope. */
    private static void actualDependencyAttribution() throws Exception {
        String source = Files.readString(CRATE.resolve("attribution/DependencyCustody.java"));
        List<String> expected = Files.readString(CRATE.resolve("attribution/expected-methods.txt"))
                .lines().filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        assertFalse(expected.isEmpty(), "at least one actual owner attribution is required");
        assertEquals(expected.size(), new HashSet<>(expected).size(), "duplicate expected owner");
        J.CompilationUnit unit = (J.CompilationUnit) parse("java",
                Map.of("proof-dependencies/DependencyCustody.java", source)).getFirst();
        Set<String> methods = new HashSet<>();
        unit.getTypesInUse().getUsedMethods().forEach(method -> methods.add(
                method.getDeclaringType().getFullyQualifiedName() + "#" + method.getName()));
        for (String owner : expected) {
            assertTrue(owner.matches("[A-Za-z0-9_.$]+#[A-Za-z0-9_$]+"), "qualified owner method");
            assertTrue(methods.contains(owner), "missing actual owner attribution: " + owner
                    + "; resolved methods: " + new java.util.TreeSet<>(methods));
        }
    }

    @Test
    void qualifySerialFilePhasesAndPublishNamedRecipeOutput() throws Exception {
        for (String invalid : List.of("", "../escape", "UPPER", "a".repeat(81))) {
            assertThrows(IllegalArgumentException.class, () -> new M3HashPinnedJavaSnapshotRecipe(invalid));
            assertThrows(IllegalArgumentException.class, () -> new M3HashPinnedTextSnapshotRecipe(invalid));
        }
        List<Phase> phases = phases();
        List<Proof> proofs = new ArrayList<>();
        for (Phase phase : phases) proofs.add(qualify(phase));
        String yaml = namedYaml(phases);
        assertEquals(yaml, Files.readString(CRATE.resolve("../../src/main/resources/META-INF/rewrite/"
                + ID + ".yml")), "checked-in named recipe descriptor");
        Map<String, String> finalOutput = qualifyNamed(phases, proofs, yaml);
        publish(proofs, finalOutput, yaml);
    }

    private static List<Phase> phases() throws Exception {
        List<String> rows = Files.readString(CRATE.resolve("phases.tsv")).lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        assertFalse(rows.isEmpty(), "phase inventory required");
        assertEquals("id\tkind", rows.getFirst(), "phase inventory header");
        assertTrue(rows.size() > 1 && rows.size() <= 65, "phase budget: 1..64");
        List<Phase> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> paths = new HashSet<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split("\t", -1);
            assertEquals(2, cells.length, "phase row columns");
            assertTrue(cells[0].startsWith(ID) && cells[0].matches("[a-z0-9][a-z0-9-]{0,79}"), "phase ID");
            assertTrue(ids.add(cells[0]), "duplicate phase ID");
            assertTrue(Set.of("java", "text").contains(cells[1]), "phase kind");
            Phase phase = new Phase(cells[0], cells[1]);
            List<Entry> entries = manifest(phase);
            assertFalse(entries.isEmpty(), "empty phase");
            for (Entry entry : entries) {
                assertTrue(paths.add(entry.path()), "one serial phase owner per target path: " + entry.path());
            }
            result.add(phase);
        }
        assertTrue(paths.size() <= 256, "packet target budget");
        return List.copyOf(result);
    }

    private static List<Entry> manifest(Phase phase) throws Exception {
        List<Entry> entries = new ArrayList<>();
        for (String row : resource(phase, "manifest.tsv").lines().toList()) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] cells = row.split("\t", -1);
            assertEquals(4, cells.length, "manifest row columns");
            safeResolve(TARGET.resolve("receiver"), cells[0]);
            assertTrue(cells[1].equals("ABSENT") || cells[1].matches("[0-9a-f]{64}"), "preimage digest");
            assertTrue(cells[2].matches("[0-9a-f]{64}"), "postimage digest");
            assertTrue(cells[3].matches("[A-Za-z0-9_.-]+"), "resource filename");
            entries.add(new Entry(cells[0], cells[1], cells[2], cells[3]));
        }
        assertTrue(entries.size() <= 256, "canonical phase target budget");
        return List.copyOf(entries);
    }

    private static String resource(Phase phase, String name) throws Exception {
        String location = "/com/synexia/rewrite/hash-pinned-" + phase.kind() + "/" + phase.id() + "/" + name;
        try (var input = M3IntegratedRefinementRecipeTest.class.getResourceAsStream(location)) {
            return new String(Objects.requireNonNull(input, location).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> baseline(Phase phase) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Entry entry : manifest(phase)) {
            if (entry.before().equals("ABSENT")) continue;
            Path source = safeResolve(CRATE.resolve("preimages/" + phase.id()), entry.path());
            if (!Files.exists(source)) source = safeResolve(CRATE.resolve("preimages"), entry.path());
            String content = Files.readString(source);
            assertEquals(entry.before(), sha(content), "sealed preimage " + entry.path());
            assertNull(result.put(entry.path(), content), "duplicate manifest target");
        }
        return result;
    }

    private static Map<String, String> expected(Phase phase) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Entry entry : manifest(phase)) {
            String content = resource(phase, entry.resource());
            assertEquals(entry.after(), sha(content), "sealed postimage " + entry.path());
            assertNull(result.put(entry.path(), content), "duplicate manifest target");
        }
        return result;
    }

    private static InMemoryExecutionContext context() {
        var context = new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
        context.putMessage(PARSER_CLASSPATH, parserClasspath);
        return context;
    }

    private static List<SourceFile> parse(String kind, Map<String, String> files) {
        if (kind.equals("text")) {
            return files.entrySet().stream().map(entry -> (SourceFile) PlainText.builder()
                    .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build()).toList();
        }
        if (files.isEmpty()) return List.of();
        List<SourceFile> result = JavaParser.fromJavaVersion().classpath(parserClasspath).build().parseInputs(
                files.entrySet().stream().map(entry -> Parser.Input.fromString(
                        Path.of(entry.getKey()), entry.getValue())).toList(), null, context()).toList();
        assertEquals(files.size(), result.size(), "Java input count");
        for (SourceFile source : result) {
            assertInstanceOf(J.CompilationUnit.class, source, "Java LST admission");
            assertEquals(files.get(path(source)), source.printAll(), "lossless input parse");
        }
        return result;
    }

    private static Recipe recipe(Phase phase) {
        return phase.kind().equals("java") ? new M3HashPinnedJavaSnapshotRecipe(phase.id())
                : new M3HashPinnedTextSnapshotRecipe(phase.id());
    }

    private static Trial run(Recipe recipe, List<SourceFile> sources) {
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        var execution = new InMemoryExecutionContext(failures::add);
        execution.putMessage(PARSER_CLASSPATH, parserClasspath);
        Map<String, String> changes = new TreeMap<>();
        try {
            var results = recipe.run(new InMemoryLargeSourceSet(sources), execution, 1)
                    .getChangeset().getAllResults();
            for (var result : results) {
                SourceFile after = Objects.requireNonNull(result.getAfter(), "snapshot recipes cannot delete targets");
                assertNull(changes.put(path(after), after.printAll()), "duplicate scheduler output");
            }
        } catch (RuntimeException failure) {
            failures.add(failure);
        }
        return new Trial(Map.copyOf(changes), List.copyOf(failures));
    }

    private static Map<String, String> accepted(Trial trial) {
        assertTrue(trial.failures().isEmpty(), () -> "scheduler failures: " + trial.failures());
        return trial.changes();
    }

    private static void refused(Phase phase, List<SourceFile> sources, String signature) {
        Trial trial = run(recipe(phase), sources);
        assertTrue(trial.failures().stream().anyMatch(error -> hasMessage(error, signature)),
                () -> "missing refusal signature " + signature + ": " + trial.failures());
        assertTrue(trial.changes().isEmpty(), "refused scheduler run must not emit candidates");
    }

    private static boolean hasMessage(Throwable error, String signature) {
        for (int depth = 0; error != null && depth < 32; depth++, error = error.getCause()) {
            if (error.getMessage() != null && error.getMessage().contains(signature)) return true;
        }
        return false;
    }

    private static Proof qualify(Phase phase) throws Exception {
        Map<String, String> before = baseline(phase);
        Map<String, String> after = expected(phase);
        List<SourceFile> sources = parse(phase.kind(), before);
        Map<String, String> output = accepted(run(recipe(phase), sources));
        assertEquals(expectedChanges(before, after), output, "exact transformation " + phase.id());
        Map<String, String> complete = new TreeMap<>(before);
        complete.putAll(output);
        assertEquals(after, complete, "complete output and unchanged pins " + phase.id());
        assertTrue(accepted(run(recipe(phase), parse(phase.kind(), after))).isEmpty(), "fixed point");
        var reverse = new ArrayList<>(sources);
        Collections.reverse(reverse);
        assertEquals(output, accepted(run(recipe(phase), reverse)), "source-order replay");
        assertEquals(output, accepted(run(recipe(phase), parse(phase.kind(), before))), "serial repeat");
        parallelReplay(() -> accepted(run(recipe(phase), parse(phase.kind(), before))), output);
        mixedState(phase, before, after);
        int missing = sourceRefusals(phase, before, after);
        var duplicate = new ArrayList<>(sources.isEmpty() ? parse(phase.kind(), after) : sources);
        duplicate.add(duplicate.getFirst().withId(Tree.randomId()));
        refused(phase, duplicate, "duplicate target:");
        var decoys = new ArrayList<>(sources);
        decoys.addAll(unrelatedSources());
        assertEquals(output, accepted(run(recipe(phase), decoys)), "unrelated payload preservation");
        String receipt = "phase\tkind\ttargets\tmanifest_sha256\tbefore_root\tafter_root\tmissing_refusals\t"
                + "drift_refusals\tduplicate_refusal\tfixed_point\tmixed\torder\trepeat\tdecoys\tworkers\tcycles\n"
                + phase.id() + "\t" + phase.kind() + "\t" + after.size() + "\t"
                + sha(resource(phase, "manifest.tsv")) + "\t" + root(before) + "\t" + root(complete)
                + "\t" + missing + "\t" + after.size() + "\tPASS\tPASS\tPASS\tPASS\tPASS\tPASS\t2\t1\n";
        return new Proof(phase, Map.copyOf(complete), receipt);
    }

    /** Existing snapshot engines also seal equal before/after dependencies as real no-op phases. */
    private static Map<String, String> expectedChanges(Map<String, String> before, Map<String, String> after) {
        Map<String, String> changes = new TreeMap<>();
        for (var entry : after.entrySet()) {
            if (!Objects.equals(before.get(entry.getKey()), entry.getValue())) {
                changes.put(entry.getKey(), entry.getValue());
            }
        }
        return changes;
    }

    private static void mixedState(Phase phase, Map<String, String> before, Map<String, String> after)
            throws Exception {
        Map<String, String> mixed = new TreeMap<>(before);
        Map<String, String> changes = expectedChanges(before, after);
        int index = 0;
        for (Entry entry : manifest(phase)) {
            if ((index++ & 1) == 0) {
                mixed.put(entry.path(), after.get(entry.path()));
                changes.remove(entry.path());
            }
        }
        Map<String, String> actual = accepted(run(recipe(phase), parse(phase.kind(), mixed)));
        assertEquals(changes, actual, "mixed-state changes");
        mixed.putAll(actual);
        assertEquals(after, mixed, "mixed-state convergence");
    }

    private static int sourceRefusals(Phase phase, Map<String, String> before, Map<String, String> after)
            throws Exception {
        int missing = 0;
        for (Entry entry : manifest(phase)) {
            var drift = new TreeMap<>(before);
            drift.put(entry.path(), before.getOrDefault(entry.path(), after.get(entry.path()))
                    + (phase.kind().equals("java") ? "\n// source drift: class Fake { int mask; }\n"
                            : "\n# source drift: JNIEXPORT void fake(void);\n"));
            refused(phase, parse(phase.kind(), drift), "source drift:");
            if (!entry.before().equals("ABSENT")) {
                var absent = new TreeMap<>(before);
                absent.remove(entry.path());
                refused(phase, parse(phase.kind(), absent), "required source missing:");
                missing++;
            }
            if (phase.kind().equals("java")) {
                var wrongKind = new ArrayList<>(parse("java", before));
                wrongKind.removeIf(source -> path(source).equals(entry.path()));
                wrongKind.add(PlainText.builder().sourcePath(Path.of(entry.path()))
                        .text(before.getOrDefault(entry.path(), after.get(entry.path()))).build());
                refused(phase, wrongKind, "not a Java compilation unit:");
            }
        }
        return missing;
    }

    private static List<SourceFile> unrelatedSources() {
        String code = "package m3proof;\nfinal class OpaquePayload {\n"
                + "  String text = \"class Fake { int[] offsets; long mask; }\";\n"
                + "  String regex = \"(class|offsets|mask)\\\\s+\";\n"
                + "  String block = \"\"\"\nJNIEXPORT void fake(void); // int index = 0;\n\"\"\";\n"
                + "  /* class Ignored { void replaceAll() {} } */\n"
                + "  java.util.function.IntUnaryOperator operation = value -> value + 1;\n"
                + "  record Pair(int left, int right) {}\n}\n";
        List<SourceFile> result = new ArrayList<>(parse("java", Map.of("proof-decoys/OpaquePayload.java", code)));
        result.add(PlainText.builder().sourcePath(Path.of("proof-decoys/source.md"))
                .text("```java\nclass Fake { int[] offsets; long mask; }\n```\n").build());
        result.add(PlainText.builder().sourcePath(Path.of("proof-decoys/native.c"))
                .text("/* JNIEXPORT void replace(void); */\nconst char *text = \"class Fake {}\";\n").build());
        return result;
    }

    private static void parallelReplay(Callable<Map<String, String>> task, Map<String, String> expected)
            throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(task);
            var second = workers.submit(task);
            assertEquals(expected, first.get(60, TimeUnit.SECONDS), "independent width-two replay one");
            assertEquals(expected, second.get(60, TimeUnit.SECONDS), "independent width-two replay two");
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(30, TimeUnit.SECONDS), "bounded workers terminated");
        }
    }

    private static String namedYaml(List<Phase> phases) {
        StringBuilder yaml = new StringBuilder("# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project\n"
                + "# SPDX-License-Identifier: Apache-2.0\n"
                + "type: specs.openrewrite.org/v1beta/recipe\nname: " + NAMED
                + "\ndisplayName: M3 integrated refinement recovery 20261021\n"
                + "description: Apply independently qualified hash-pinned file phases.\nrecipeList:\n");
        for (Phase phase : phases) {
            yaml.append("  - com.synexia.rewrite.M3HashPinned")
                    .append(phase.kind().equals("java") ? "Java" : "Text")
                    .append("SnapshotRecipe:\n      crateName: ").append(phase.id()).append('\n');
        }
        return yaml.toString();
    }

    private static Recipe namedRecipe(String yaml) throws Exception {
        try (var input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            var loader = new YamlResourceLoader(input, URI.create("classpath:/" + ID + ".yml"), new Properties());
            Recipe recipe = Environment.builder().load(loader).build().activateRecipes(NAMED);
            assertTrue(recipe.validate().isValid(), "named recipe validation");
            return recipe;
        }
    }

    private static List<SourceFile> combined(List<Phase> phases, boolean postimages) throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (Phase phase : phases) result.addAll(parse(phase.kind(), postimages ? expected(phase) : baseline(phase)));
        return result;
    }

    private static Map<String, String> qualifyNamed(List<Phase> phases, List<Proof> proofs, String yaml)
            throws Exception {
        Map<String, String> expected = new TreeMap<>();
        for (Proof proof : proofs) expected.putAll(proof.output());
        List<SourceFile> input = combined(phases, false);
        Map<String, String> before = new TreeMap<>();
        for (SourceFile source : input) before.put(path(source), source.printAll());
        Map<String, String> output = accepted(run(namedRecipe(yaml), input));
        assertEquals(expectedChanges(before, expected), output, "named composition exact changes");
        Map<String, String> complete = new TreeMap<>(before);
        complete.putAll(output);
        assertEquals(expected, complete, "named composition complete output and unchanged pins");
        assertTrue(accepted(run(namedRecipe(yaml), combined(phases, true))).isEmpty(), "named fixed point");
        var reverse = new ArrayList<>(input);
        Collections.reverse(reverse);
        assertEquals(output, accepted(run(namedRecipe(yaml), reverse)), "named source order");
        assertEquals(output, accepted(run(namedRecipe(yaml), combined(phases, false))), "named serial repeat");
        parallelReplay(() -> accepted(run(namedRecipe(yaml), combined(phases, false))), output);
        List<SourceFile> mixed = new ArrayList<>();
        Map<String, String> remaining = new TreeMap<>();
        for (int i = 0; i < phases.size(); i++) {
            Phase phase = phases.get(i);
            mixed.addAll(parse(phase.kind(), (i & 1) == 0 ? expected(phase) : baseline(phase)));
            if ((i & 1) != 0) remaining.putAll(expectedChanges(baseline(phase), expected(phase)));
        }
        assertEquals(remaining, accepted(run(namedRecipe(yaml), mixed)), "named mixed-state changes");
        var decoys = new ArrayList<>(input);
        decoys.addAll(unrelatedSources());
        assertEquals(output, accepted(run(namedRecipe(yaml), decoys)), "named unrelated payload preservation");
        return Map.copyOf(complete);
    }

    private static void publish(List<Proof> proofs, Map<String, String> output, String yaml) throws Exception {
        Path receiver = TARGET.resolve("receiver");
        if (Files.exists(receiver)) {
            try (var paths = Files.walk(receiver)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    assertTrue(output.containsKey(receiver.relativize(path).toString().replace('\\', '/')),
                            "unmanifested receiver file: " + path);
                }
            }
        }
        Map<String, String> custody = new TreeMap<>(OWNERS);
        custody.put("phases.tsv", sha(Files.readAllBytes(CRATE.resolve("phases.tsv"))));
        custody.put("pom.xml", sha(Files.readAllBytes(CRATE.resolve("pom.xml"))));
        custody.put("test.java", sha(Files.readAllBytes(CRATE.resolve(
                "src/test/java/com/synexia/rewrite/M3IntegratedRefinementRecipeTest.java"))));
        custody.put("dependency_probe_sha256", sha(Files.readAllBytes(
                CRATE.resolve("attribution/DependencyCustody.java"))));
        custody.put("dependency_expectations_sha256", sha(Files.readAllBytes(
                CRATE.resolve("attribution/expected-methods.txt"))));
        custody.put("parser_classpath_root", classpathRoot());
        custody.put("parser_owner_attribution", "PASS");
        custody.put("java_runtime", Runtime.version().toString());
        custody.put("named_yaml_sha256", sha(yaml));
        custody.put("receiver_root", root(output));
        StringBuilder receipt = new StringBuilder("key\tvalue\n");
        for (var item : custody.entrySet()) receipt.append(item.getKey()).append('\t').append(item.getValue()).append('\n');
        for (Proof proof : proofs) receipt.append("phase_receipt_").append(proof.phase().id())
                .append('\t').append(sha(proof.receipt())).append('\n');
        receipt.append("scope\tsource-replay-only\nstatus\tPASS\nphases\t").append(proofs.size())
                .append("\ntargets\t").append(output.size())
                .append("\nnamed_fixed_point\tPASS\nnamed_mixed\tPASS\nnamed_order\tPASS\nnamed_repeat\tPASS\n")
                .append("named_decoys\tPASS\nindependent_workers\t2\ncycles_per_run\t1\n");
        for (var item : output.entrySet()) {
            Path path = safeResolve(receiver, item.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, item.getValue());
        }
        Path receipts = TARGET.resolve("phase-receipts");
        Files.createDirectories(receipts);
        for (Proof proof : proofs) Files.writeString(receipts.resolve(proof.phase().id() + ".tsv"), proof.receipt());
        Files.writeString(TARGET.resolve("named-recipe.yml"), yaml);
        Files.writeString(TARGET.resolve("recipe-proof.tsv"), receipt.toString());
        Files.writeString(TARGET.resolve("proof-status.tsv"), "status\tproof_sha256\nPASS\t" + sha(receipt.toString()) + "\n");
    }

    private static String classpathRoot() throws Exception {
        StringBuilder identity = new StringBuilder();
        for (int i = 0; i < parserClasspath.size(); i++) {
            Path member = parserClasspath.get(i);
            if (Files.isDirectory(member)) {
                try (var paths = Files.walk(member)) {
                    for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                        identity.append(i).append('/').append(member.relativize(path).toString().replace('\\', '/'))
                                .append('\t').append(sha(Files.readAllBytes(path))).append('\n');
                    }
                }
            } else {
                identity.append(i).append('/').append(member.getFileName()).append('\t')
                        .append(sha(Files.readAllBytes(member))).append('\n');
            }
        }
        return sha(identity.toString());
    }

    private static Path safeResolve(Path root, String relative) {
        Path child = Path.of(relative);
        assertFalse(child.isAbsolute(), "relative target path required");
        assertEquals(relative, child.normalize().toString().replace('\\', '/'), "normalized target path required");
        Path base = root.toAbsolutePath().normalize();
        Path result = base.resolve(child).normalize();
        assertTrue(result.startsWith(base) && !result.equals(base), "target escaped owner");
        for (Path path = result; path != null && path.startsWith(base); path = path.getParent()) {
            assertFalse(Files.isSymbolicLink(path), "symbolic target path is not admitted");
        }
        return result;
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static String root(Map<String, String> files) throws Exception {
        StringBuilder identity = new StringBuilder();
        for (var entry : new TreeMap<>(files).entrySet()) {
            identity.append(entry.getKey()).append('\t').append(sha(entry.getValue())).append('\n');
        }
        return sha(identity.toString());
    }

    private static String sha(String content) throws Exception {
        return sha(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
