// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Finite seeded delta/selection fixture with actual javac and a full-build oracle.
 * The fixture-authored graph A -> B -> C describes compile-time constants; Leaf
 * is independent. This is not production graph extraction, shared-AST integration,
 * annotation-processor/output-ownership qualification, or a build framework.
 */
public final class M3ProjectDeltaPermutationContract {
    private static final long[] SEEDS = {0x4d330001L, 0x4d330002L};
    private static final Set<String> ALL = Set.of("A", "B", "C", "Leaf");
    private static int checks, compilerRuns, generations;
    private final Path root;
    private final List<Path> inputs = new ArrayList<>(List.of(Path.of("src/A.java"),
            Path.of("src/B.java"), Path.of("src/C.java"), Path.of("src/Leaf.java"), Path.of("compiler.options")));
    private final Random random;
    private final boolean timestamps;
    private M3FileDeltaLedger.Snapshot baseline;
    private String operation = "initial";

    private M3ProjectDeltaPermutationContract(Path root, long seed) {
        this.root = root;
        this.random = new Random(seed);
        this.timestamps = (seed & 1) != 0;
    }

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("m3-project-delta-permutations-");
        boolean passed = false;
        try {
            for (long seed : SEEDS) {
                var fixture = new M3ProjectDeltaPermutationContract(temporary.resolve(Long.toHexString(seed)), seed);
                try { fixture.exercise(); }
                catch (Exception | AssertionError failure) {
                    System.err.println("M3_PROJECT_DELTA_FAILURE seed=" + seed + " operation="
                            + fixture.operation + " fixture=" + fixture.root);
                    throw failure;
                }
            }
            System.out.println("M3_PROJECT_DELTA_PERMUTATION_PASS seeds=" + Arrays.toString(SEEDS)
                    + " generations=" + generations + " compiler_runs=" + compilerRuns + " checks=" + checks
                    + " scope=four-source-fixture shared_AST_integration=NOT_RUN native=NOT_RUN");
            passed = true;
        } finally {
            if (passed) erase(temporary); // Unexpected failures retain sources, state and compiler diagnostics.
        }
    }

    private void exercise() throws Exception {
        Files.createDirectories(root.resolve("src"));
        Files.createDirectories(root.resolve("empty-source-path"));
        write("A", "public class A { public static final int V = 1; }\n");
        write("B", "public class B { public static final int V = A.V + 1; }\n");
        write("C", "public class C { public static int value() { return B.V; } }\n");
        write("Leaf", "public class Leaf { public static int value() { return 1; } }\n");
        Files.writeString(root.resolve("compiler.options"), "fixture-context=1\n");
        step(ALL, 5, 0);
        var operations = new ArrayList<>(List.of("edit", "touch", "delete", "failure", "config"));
        Collections.shuffle(operations, random);
        for (String next : operations) {
            operation = next;
            switch (next) {
                case "edit" -> {
                    Path source = root.resolve("src/A.java");
                    FileTime time = Files.getLastModifiedTime(source);
                    long size = Files.size(source);
                    write("A", "public class A { public static final int V = 2; }\n");
                    Files.setLastModifiedTime(source, time);
                    ok(Files.size(source) == size && Files.getLastModifiedTime(source).equals(time),
                            "same-size/restored-time mutation fixture");
                    step(Set.of("A", "B", "C"), 1, 0);
                }
                case "touch" -> {
                    Path source = root.resolve("src/A.java");
                    FileTime old = Files.getLastModifiedTime(source);
                    Files.setLastModifiedTime(source, FileTime.from(old.toInstant().plusSeconds(5)));
                    ok(!Files.getLastModifiedTime(source).equals(old), "touch fixture changes observed time");
                    step(timestamps ? Set.of("A", "B", "C") : Set.of(), timestamps ? 1 : 0, 0);
                }
                case "delete" -> {
                    int original = slot(baseline, "Leaf");
                    Files.delete(root.resolve("src/Leaf.java"));
                    step(Set.of(), 1, 1);
                    ok(!Files.exists(root.resolve("out/Leaf.class")), "removed source output is absent");
                    write("Leaf", "public class Leaf { public static int value() { return 2; } }\n");
                    step(Set.of("Leaf"), 1, 0);
                    ok(slot(baseline, "Leaf") == original, "recreated source retains its physical slot");
                }
                case "failure" -> failedCompilation();
                case "config" -> {
                    Files.writeString(root.resolve("compiler.options"), "fixture-context=2\n");
                    step(ALL, 1, 0); // Build-input context changes conservatively select every source.
                }
                default -> throw new AssertionError(next);
            }
        }
    }

    private void failedCompilation() throws Exception {
        byte[] image = Files.readAllBytes(root.resolve("state.m3dl"));
        Map<String, byte[]> outputs = classes(root.resolve("out"));
        write("B", "public class B { invalid Java here; }\n");
        var failed = scan();
        var selection = plan(failed).select(failed, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1);
        ok(names(failed, selection.selectedSourceSlots()).equals(Set.of("B", "C")), "failed edit closure");
        prepareStage(failed, selection);
        ok(compile(root.resolve("stage"), paths(failed, selection.selectedSourceSlots())) != 0,
                "actual javac rejects invalid source");
        ok(Arrays.equals(image, Files.readAllBytes(root.resolve("state.m3dl"))), "failed build retains state");
        equalClasses(outputs, classes(root.resolve("out")));
        baseline = M3FileDeltaLedger.load(root.resolve("state.m3dl"), root);
        var retry = scan();
        ok(retry.changedAt(slot(retry, "B"))
                && Arrays.equals(failed.digestAt(slot(failed, "B")), retry.digestAt(slot(retry, "B"))),
                "reload retains exact failed bytes as pending work");
        write("B", "public class B { public static final int V = A.V + 2; }\n");
        step(Set.of("B", "C"), 1, 0);
    }

    private void step(Set<String> expected, int direct, int removed) throws Exception {
        var snapshot = scan();
        var plan = plan(snapshot);
        var selection = plan.select(snapshot, M3BuildImpactPlan.Mode.AUTO, -1, -1);
        int live = Files.exists(root.resolve("src/Leaf.java")) ? 4 : 3;
        ok(names(snapshot, selection.selectedSourceSlots()).equals(expected), operation + " selected sources");
        ok(selection.totalLiveSources() == live && selection.totalLiveFiles() == live + 1
                && selection.directChangedFiles() == direct && selection.removedFiles() == removed,
                operation + " exact file and delta counts");
        ok(selection.hasWork() == (direct != 0), operation + " no-op versus cleanup work");
        ok(plan.select(snapshot, M3BuildImpactPlan.Mode.FULL, -1, -1).selectedSourceCount() == live,
                "explicit full selects every live source");
        if (!expected.isEmpty() && !plan.requiresConservativeBuild()) {
            ok(plan.select(snapshot, M3BuildImpactPlan.Mode.AUTO, expected.size(), -1).decision()
                    == M3BuildImpactPlan.Decision.FULL, "inclusive affected count threshold");
            ok(plan.select(snapshot, M3BuildImpactPlan.Mode.AUTO, -1, 100.0 * expected.size() / live).decision()
                    == M3BuildImpactPlan.Decision.FULL, "inclusive affected percentage threshold");
            ok(plan.select(snapshot, M3BuildImpactPlan.Mode.INCREMENTAL, 0, 0).decision()
                    == M3BuildImpactPlan.Decision.INCREMENTAL, "explicit incremental bypasses only cost policy");
        }
        if (plan.requiresConservativeBuild()) {
            ok(plan.select(snapshot, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1).decision()
                    == M3BuildImpactPlan.Decision.FULL, "incremental request cannot bypass correctness");
        }
        prepareStage(snapshot, selection);
        ok(compile(root.resolve("stage"), paths(snapshot, selection.selectedSourceSlots())) == 0,
                operation + " selected javac succeeds");
        erase(root.resolve("full"));
        List<Path> all;
        try (var sources = Files.list(root.resolve("src"))) { all = sources.sorted().toList(); }
        ok(compile(root.resolve("full"), all) == 0, operation + " clean full javac succeeds");
        equalClasses(classes(root.resolve("full")), classes(root.resolve("stage")));
        erase(root.resolve("out"));
        Files.move(root.resolve("stage"), root.resolve("out"));
        M3FileDeltaLedger.save(root.resolve("state.m3dl"), snapshot);
        baseline = M3FileDeltaLedger.load(root.resolve("state.m3dl"), root);
        var unchanged = scan();
        ok(plan(unchanged).select(unchanged, M3BuildImpactPlan.Mode.AUTO, 0, 0).decision()
                == M3BuildImpactPlan.Decision.NO_OP, operation + " successful generation reaches no-op");
        generations++;
    }

    private M3FileDeltaLedger.Snapshot scan() throws IOException {
        Collections.shuffle(inputs, random);
        var observed = M3FileDeltaLedger.scan(root, baseline, inputs, timestamps);
        Collections.reverse(inputs);
        var reversed = M3FileDeltaLedger.scan(root, baseline, inputs, timestamps);
        ok(observed.slots() == reversed.slots(), "input permutation preserves slot count");
        for (int i = 0; i < observed.slots(); i++) {
            ok(observed.pathAt(i).equals(reversed.pathAt(i)) && observed.stateAt(i) == reversed.stateAt(i)
                    && Arrays.equals(observed.digestAt(i), reversed.digestAt(i)), "input permutation preserves facts");
        }
        return observed;
    }

    private static M3BuildImpactPlan plan(M3FileDeltaLedger.Snapshot snapshot) {
        int[] offsets = new int[snapshot.slots() + 1];
        var targets = new ArrayList<Integer>();
        for (int i = 0; i < snapshot.slots(); i++) {
            offsets[i] = targets.size();
            if (i == slot(snapshot, "A")) targets.add(slot(snapshot, "B"));
            if (i == slot(snapshot, "B")) targets.add(slot(snapshot, "C"));
        }
        offsets[snapshot.slots()] = targets.size();
        // Complete only for this fixed grammar/fixture; never use these edges for user projects.
        return M3BuildImpactPlan.compute(snapshot, true, offsets,
                targets.stream().mapToInt(Integer::intValue).toArray(), new byte[snapshot.slots()]);
    }

    private void prepareStage(M3FileDeltaLedger.Snapshot snapshot, M3BuildImpactPlan.Selection selection)
            throws IOException {
        erase(root.resolve("stage"));
        Files.createDirectories(root.resolve("stage"));
        if (Files.isDirectory(root.resolve("out"))) {
            try (var files = Files.list(root.resolve("out"))) {
                for (Path file : files.toList()) Files.copy(file, root.resolve("stage").resolve(file.getFileName()));
            }
        }
        for (int slot : selection.removedSlots()) {
            String name = Path.of(snapshot.pathAt(slot)).getFileName().toString().replace(".java", ".class");
            Files.deleteIfExists(root.resolve("stage").resolve(name));
        }
    }

    private int compile(Path output, List<Path> sources) throws Exception {
        Files.createDirectories(output);
        if (sources.isEmpty()) return 0;
        String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "javac" + suffix).toString(),
                "-J-Xmx128m", "-J-XX:ActiveProcessorCount=2", "--release", "21", "-proc:none", "-g:none",
                "-encoding", "UTF-8", "-implicit:none", "-sourcepath", root.resolve("empty-source-path").toString(),
                "-classpath", output.toString(), "-d", output.toString()));
        for (Path source : sources) command.add(source.toString());
        Path log = root.resolve("javac-" + (++compilerRuns) + ".log");
        var builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(variable);
        Process process = builder.start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new IOException("javac timeout: " + log);
            return process.exitValue();
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
        }
    }

    private void write(String name, String content) throws IOException { Files.writeString(root.resolve("src/" + name + ".java"), content); }
    private static int slot(M3FileDeltaLedger.Snapshot s, String name) { return s.physicalSlot(Path.of("src/" + name + ".java")); }
    private static List<Path> paths(M3FileDeltaLedger.Snapshot s, int[] slots) {
        return Arrays.stream(slots).mapToObj(i -> s.root().resolve(s.pathAt(i))).toList();
    }
    private static Set<String> names(M3FileDeltaLedger.Snapshot s, int[] slots) {
        var names = new TreeSet<String>();
        for (Path path : paths(s, slots)) names.add(path.getFileName().toString().replace(".java", ""));
        return names;
    }
    private static Map<String, byte[]> classes(Path directory) throws IOException {
        var result = new TreeMap<String, byte[]>();
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) result.put(file.getFileName().toString(), Files.readAllBytes(file));
        }
        return result;
    }
    private static void equalClasses(Map<String, byte[]> expected, Map<String, byte[]> actual) {
        ok(expected.keySet().equals(actual.keySet()), "full-build oracle class ownership");
        for (String path : expected.keySet()) ok(Arrays.equals(expected.get(path), actual.get(path)), "full-build oracle bytes: " + path);
    }
    private static void erase(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
    private static void ok(boolean value, String claim) {
        checks++;
        if (!value) throw new AssertionError(claim);
    }
}
