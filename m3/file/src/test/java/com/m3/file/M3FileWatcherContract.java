// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real WatchService and successful-generation contracts, without a test framework.
 * A dependency-free single-source javac fixture checks actual changed behavior.
 * This gate does not qualify shared ASTs, compiler dependency graphs, processors,
 * Maven/Gradle integration, or complete source-to-output cleanup.
 */
public final class M3FileWatcherContract {
    private static final Duration EVENT_WAIT = Duration.ofSeconds(5);
    // Short tests cannot pass by waiting for the periodic reconciliation fallback.
    private static final Duration RECONCILE = Duration.ofHours(1);
    private static final Path IMAGE = Path.of("state", "facts.m3dl");
    private static int checks;
    private static int commandSequence;
    private static int compilerRuns;

    private M3FileWatcherContract() {}

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("m3-file-watcher-contract-");
        try {
            compiledGenerations(temporary.resolve("compiler"));
            failedRetrySuppression(temporary.resolve("failed-retry"));
            pendingGenerationPolicy(temporary.resolve("pending-content"), false);
            pendingGenerationPolicy(temporary.resolve("pending-timestamp"), true);
            refusedStaleCommit(temporary.resolve("stale"));
            nestedInputLifecycle(temporary.resolve("nested"));
            timestampRequestsAndLock(temporary.resolve("policy"));
            System.out.println("M3_FILE_WATCHER_PASS checks=" + checks + " compiler_runs=" + compilerRuns
                    + " scope=WatchService-and-single-source-javac shared_AST_integration=NOT_RUN");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void compiledGenerations(Path root) throws Exception {
        Path source = writeSource(root, 7);
        byte[] failedInput;
        byte[] successfulImage;
        try (M3FileWatcher watcher = open(root, List.of(Path.of("src")), false)) {
            var first = next(watcher, "initial source generation");
            state(first, "src/Watched.java", M3FileDeltaLedger.CREATED, "initial source is created");
            ok(!Files.exists(root.resolve(IMAGE)), "observing an input does not publish successful state");
            ok(compile(root).exitCode() == 0, "initial javac succeeds");
            value(root, 7);
            ok(watcher.commit(first), "successful compiler work commits its offered generation");
            noOp(watcher, "committed inputs are a no-op");

            long originalSize = Files.size(source);
            FileTime originalTime = Files.getLastModifiedTime(source);
            writeSource(root, 9);
            Files.setLastModifiedTime(source, originalTime);
            ok(Files.size(source) == originalSize && Files.getLastModifiedTime(source).equals(originalTime),
                    "fixture changes source with identical size and timestamp");
            var changed = next(watcher, "same-size same-time source event");
            state(changed, "src/Watched.java", M3FileDeltaLedger.MODIFIED, "SHA-256 detects restored-time edit");
            ok(compile(root).exitCode() == 0, "changed source compiles");
            value(root, 9);
            ok(watcher.commit(changed), "new compiled behavior commits");
            noOp(watcher, "changed generation is current after commit");

            successfulImage = Files.readAllBytes(root.resolve(IMAGE));
            Files.writeString(source, "public final class Watched { this is not valid Java; }\n");
            var failed = next(watcher, "invalid source event");
            failedInput = failed.digestAt(slot(failed, "src/Watched.java"));
            ok(compile(root).exitCode() != 0, "actual javac rejects invalid source");
            ok(Arrays.equals(successfulImage, Files.readAllBytes(root.resolve(IMAGE))),
                    "failed compiler work leaves the successful image unchanged");
            value(root, 9); // The previous class is not evidence that the failed input was built.
        }
        try (M3FileWatcher restarted = open(root, List.of(Path.of("src")), false)) {
            var pending = next(restarted, "failed input survives watcher restart");
            state(pending, "src/Watched.java", M3FileDeltaLedger.MODIFIED, "restart retains pending source work");
            ok(Arrays.equals(failedInput, pending.digestAt(slot(pending, "src/Watched.java"))),
                    "restart offers the actual failed input bytes");
            ok(Arrays.equals(successfulImage, Files.readAllBytes(root.resolve(IMAGE))),
                    "restart observation does not acknowledge failed work");
            writeSource(root, 3);
            var repaired = next(restarted, "repaired source event");
            ok(compile(root).exitCode() == 0, "repaired source compiles");
            value(root, 3);
            ok(restarted.commit(repaired), "repaired output advances the successful state");
            noOp(restarted, "repaired generation is current");
        }
    }

    private static void failedRetrySuppression(Path root) throws Exception {
        Path source = writeSource(root, 7);
        byte[] originalSource = Files.readAllBytes(source);
        FileTime originalTime = Files.getLastModifiedTime(source);
        try (M3FileWatcher watcher = open(root, List.of(Path.of(".")), false)) {
            var first = next(watcher, "retry fixture initial generation");
            ok(compile(root).exitCode() == 0, "retry fixture initially compiles");
            value(root, 7);
            ok(watcher.commit(first), "retry fixture successful generation commits");
            noOp(watcher, "retry fixture successful baseline is current");
            byte[] successfulImage = Files.readAllBytes(root.resolve(IMAGE));

            Files.writeString(source, "public final class Watched { this is not valid Java; }\n");
            var failed = next(watcher, "retry fixture invalid source generation");
            ok(compile(root).exitCode() != 0, "retry fixture performs an actual failed javac invocation");
            // Simulate clean/output activity at the registered project root. These
            // directory events must request a scan, not retry identical failed inputs.
            try (var outputs = Files.walk(root.resolve("out"))) {
                for (Path path : outputs.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            Files.createDirectories(root.resolve("out"));
            Files.writeString(root.resolve("out/build.log"), "excluded failed-build output\n");
            ok(!Files.exists(root.resolve("out/Watched.class")), "failed work has removed the prior output");
            noOp(watcher, "excluded output events must not retry identical failed inputs");
            noOp(watcher, "unchanged failed inputs remain pending without a busy retry");
            ok(Arrays.equals(successfulImage, Files.readAllBytes(root.resolve(IMAGE))),
                    "suppressed failed input never advances the successful ledger");

            watcher.requestBuild();
            var retry = watcher.awaitChange(Duration.ZERO).orElseThrow(() -> new AssertionError("explicit failed retry was lost"));
            ok(Arrays.equals(failed.digestAt(slot(failed, "src/Watched.java")), retry.digestAt(slot(retry, "src/Watched.java"))),
                    "explicit request retries the same failed source generation");
            ok(compile(root).exitCode() != 0, "explicit retry performs another actual failed javac invocation");
            noOp(watcher, "explicit failed retry does not enable later automatic retries");

            Files.write(source, originalSource);
            Files.setLastModifiedTime(source, originalTime);
            var restored = next(watcher, "repair restoring last successful source bytes");
            ok(!hasChanges(restored), "restored source is equal to the committed input baseline");
            ok(Arrays.equals(successfulImage, Files.readAllBytes(root.resolve(IMAGE))),
                    "restoring sources does not acknowledge missing output work");
            ok(compile(root).exitCode() == 0, "restored source rebuilds output removed by failed work");
            value(root, 7);
            ok(watcher.commit(restored), "restored source and rebuilt output may commit");
            noOp(watcher, "restored successful generation no longer requests work");
        }
    }

    private static void pendingGenerationPolicy(Path root, boolean timestamps) throws Exception {
        Path source = writeSource(root, 1);
        // This separate lane exercises periodic reconciliation with no native
        // notification requirement; the other watcher contracts use a one-hour interval.
        try (M3FileWatcher watcher = M3FileWatcher.open(root, List.of(Path.of("src")),
                List.of(Path.of("out")), IMAGE, Duration.ofMillis(20), timestamps)) {
            var first = next(watcher, "uncommitted initial generation");
            noOp(watcher, "periodic scans do not repeat an identical initial offer");
            ok(!Files.exists(root.resolve(IMAGE)), "suppressing an initial offer does not create successful state");

            FileTime originalTime = Files.getLastModifiedTime(source);
            Files.setLastModifiedTime(source, FileTime.from(originalTime.toInstant().plusSeconds(2)));
            if (timestamps) {
                var touched = next(watcher, "pending timestamp-sensitive touch");
                ok(!first.lastModifiedAt(slot(first, "src/Watched.java"))
                        .equals(touched.lastModifiedAt(slot(touched, "src/Watched.java"))),
                        "timestamp-sensitive pending identity includes modification time");
                noOp(watcher, "pending timestamp-sensitive touch is offered only once");
            } else noOp(watcher, "pending content-only identity ignores a timestamp-only touch");

            FileTime beforeEdit = Files.getLastModifiedTime(source);
            long beforeSize = Files.size(source);
            writeSource(root, 2);
            Files.setLastModifiedTime(source, beforeEdit);
            var edited = next(watcher, "pending same-size same-time content change");
            ok(Files.size(source) == beforeSize && Files.getLastModifiedTime(source).equals(beforeEdit),
                    "pending source edit restores size and timestamp");
            ok(!Arrays.equals(first.digestAt(slot(first, "src/Watched.java")), edited.digestAt(slot(edited, "src/Watched.java"))),
                    "pending input identity checks content SHA-256");
            noOp(watcher, "periodic reconciliation does not reoffer edited pending inputs");

            Path other = source.resolveSibling("Other.java");
            Files.move(source, other);
            var moved = next(watcher, "pending source path changed with identical bytes");
            ok(liveFiles(moved) == 1 && moved.liveAt(slot(moved, "src/Other.java")),
                    "pending input identity includes the complete live path set");
            noOp(watcher, "pending renamed input is offered only once");
            Files.delete(other);
            var removed = next(watcher, "pending input removed");
            ok(liveFiles(removed) == 0, "pending input identity detects complete removal");
            noOp(watcher, "pending empty inventory is offered only once");
        }
    }

    private static void refusedStaleCommit(Path root) throws Exception {
        Path source = root.resolve("src/input.txt");
        Path output = root.resolve("out/copied.txt");
        Files.createDirectories(source.getParent());
        Files.createDirectories(output.getParent());
        Files.writeString(source, "first");
        try (M3FileWatcher watcher = open(root, List.of(Path.of("src")), false)) {
            var first = next(watcher, "initial resource generation");
            Files.copy(source, output);
            ok(watcher.commit(first), "initial caller-owned resource work commits");
            noOp(watcher, "resource baseline is current");
            byte[] previousImage = Files.readAllBytes(root.resolve(IMAGE));

            Files.writeString(source, "second");
            var second = next(watcher, "second resource generation");
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(source, "third"); // Change after caller-owned work, before acknowledgement.
            ok(!watcher.commit(second), "mutation between offered work and commit is refused");
            ok(Arrays.equals(previousImage, Files.readAllBytes(root.resolve(IMAGE))),
                    "refused commit retains previous successful state");
            ok(Files.readString(output).equals("second"), "refused work did not magically update its output");
            var third = next(watcher, "new generation remains pending after refusal");
            ok(Arrays.equals(third.digestAt(slot(third, "src/input.txt")),
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))),
                    "pending generation describes the latest resource bytes");
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            ok(watcher.commit(third), "new caller-owned resource work commits");
            noOp(watcher, "replacement resource generation is current");

            watcher.requestBuild();
            var repeated = next(watcher, "explicit build of current resource generation");
            Files.writeString(output, "damaged caller-owned output");
            Files.writeString(source, "changed while building");
            ok(!watcher.commit(repeated), "observed mutation refuses an explicit unchanged build");
            Files.writeString(source, "third");
            var reverted = next(watcher, "observed commit refusal survives source reversion");
            ok(!hasChanges(reverted), "reverted input matches successful bytes but output still requires work");
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            ok(watcher.commit(reverted), "work after observed refusal may commit after repair");
            noOp(watcher, "known commit invalidation requests only one replacement offer");
        }
    }

    private static void nestedInputLifecycle(Path root) throws Exception {
        Files.createDirectories(root.resolve("inputs/src"));
        Path input = Path.of("inputs/src/late/deep");
        Path firstFile = root.resolve(input).resolve("First.java");
        Path nestedFile = root.resolve(input).resolve("inner/Nested.java");
        try (M3FileWatcher watcher = open(root, List.of(input), false)) {
            var empty = next(watcher, "initially absent nested input");
            ok(empty.slots() == 0 && watcher.commit(empty), "empty successful inventory is persisted");
            noOp(watcher, "absent nested input is stable");

            Files.createDirectories(firstFile.getParent());
            Files.writeString(firstFile, "class First {}\n");
            var appeared = next(watcher, "creation under initially absent nested input");
            state(appeared, relative(root, firstFile), M3FileDeltaLedger.CREATED, "nested input creation is observed");
            ok(watcher.commit(appeared), "new nested inventory commits");
            noOp(watcher, "new nested inventory is stable");

            Files.createDirectories(nestedFile.getParent());
            Files.writeString(nestedFile, "class Nested { int value = 1; }\n");
            var added = next(watcher, "new child directory and source");
            int nestedSlot = slot(added, relative(root, nestedFile));
            state(added, relative(root, nestedFile), M3FileDeltaLedger.CREATED, "child source enters inventory");
            ok(watcher.commit(added), "child source generation commits");
            noOp(watcher, "child directory registration is settled");
            Files.writeString(nestedFile, "class Nested { int value = 2; }\n");
            var edited = next(watcher, "event from newly registered child directory");
            state(edited, relative(root, nestedFile), M3FileDeltaLedger.MODIFIED, "new child directory is watched");
            ok(watcher.commit(edited), "child edit commits");
            noOp(watcher, "child edit is current");

            Files.delete(nestedFile);
            var removed = next(watcher, "nested source removal");
            state(removed, relative(root, nestedFile), M3FileDeltaLedger.REMOVED, "source removal is retained");
            ok(!removed.liveAt(nestedSlot) && watcher.commit(removed), "removed source becomes a committed tombstone");
            noOp(watcher, "removal is not repeatedly emitted");
            Files.delete(firstFile);
            Files.delete(nestedFile.getParent());
            Files.delete(firstFile.getParent());
            var missingDirectory = next(watcher, "input directory removal");
            state(missingDirectory, relative(root, firstFile), M3FileDeltaLedger.REMOVED, "removed input directory invalidates files");
            ok(watcher.commit(missingDirectory), "missing input directory commits its removals");
            noOp(watcher, "removed input directory is stable");
            Files.createDirectories(nestedFile.getParent());
            Files.writeString(nestedFile, "class Nested {}\n");
            var recreated = next(watcher, "nested input recreation");
            ok(slot(recreated, relative(root, nestedFile)) == nestedSlot, "recreation reuses original physical slot");
            state(recreated, relative(root, nestedFile), M3FileDeltaLedger.CREATED, "recreated source is created again");
            ok(watcher.commit(recreated), "recreated input generation commits");
        }
    }

    private static void timestampRequestsAndLock(Path root) throws Exception {
        Path source = writeSource(root, 1);
        try (M3FileWatcher watcher = open(root, List.of(Path.of(".")), true)) {
            var first = next(watcher, "timestamp-sensitive initial generation");
            ok(liveFiles(first) == 1, "state image and lock are excluded from a root inventory");
            ok(watcher.commit(first), "timestamp-sensitive initial state commits");
            noOp(watcher, "root inventory does not observe its own state writes as input changes");
            try {
                M3FileWatcher duplicate = open(root, List.of(Path.of(".")), true);
                duplicate.close();
                throw new AssertionError("same state image accepted duplicate active watcher");
            } catch (IOException expected) { checks++; }

            FileTime previousTime = Files.getLastModifiedTime(source);
            Files.setLastModifiedTime(source, FileTime.from(previousTime.toInstant().plusSeconds(2)));
            var touched = next(watcher, "timestamp-only event under explicit policy");
            state(touched, "src/Watched.java", M3FileDeltaLedger.MODIFIED, "timestamp-sensitive policy selects a touch");
            ok(Arrays.equals(first.digestAt(slot(first, "src/Watched.java")), touched.digestAt(slot(touched, "src/Watched.java"))),
                    "timestamp selection leaves content digest unchanged");
            ok(watcher.commit(touched), "timestamp-sensitive generation commits");
            noOp(watcher, "committed timestamp does not repeatedly request work");

            watcher.requestBuild();
            var requested = watcher.awaitChange(Duration.ZERO).orElseThrow(() -> new AssertionError("explicit request was lost"));
            ok(!hasChanges(requested), "explicit build request offers an unchanged generation");
            ok(watcher.commit(requested), "explicit unchanged generation can be acknowledged");
            Files.createDirectories(root.resolve("out/generated"));
            Files.writeString(root.resolve("out/generated/Excluded.java"), "class Excluded {}\n");
            noOp(watcher, "excluded generated output does not enter the input inventory");
        }
        try (M3FileWatcher reopened = open(root, List.of(Path.of(".")), true)) {
            ok(reopened.awaitChange(Duration.ZERO).isEmpty(), "close releases image lock and successful state survives reopen");
        }
    }

    private static M3FileWatcher open(Path root, List<Path> inputs, boolean timestamps) throws IOException {
        return M3FileWatcher.open(root, inputs, List.of(Path.of("out")), IMAGE, RECONCILE, timestamps);
    }

    private static M3FileDeltaLedger.Snapshot next(M3FileWatcher watcher, String claim) throws Exception {
        var result = watcher.awaitChange(EVENT_WAIT);
        ok(result.isPresent(), claim + " was not observed before the event deadline");
        return result.orElseThrow();
    }

    private static void noOp(M3FileWatcher watcher, String claim) throws Exception {
        ok(watcher.awaitChange(Duration.ofMillis(150)).isEmpty(), claim);
    }

    private static Path writeSource(Path root, int value) throws IOException {
        Path source = root.resolve("src/Watched.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "public final class Watched { public static void main(String[] args) { System.out.println("
                + value + "); } }\n");
        return source;
    }

    private static Execution compile(Path root) throws Exception {
        compilerRuns++;
        return run(root, "javac", List.of("-J-Xmx128m", "-J-XX:ActiveProcessorCount=2", "--release", "21",
                "-proc:none", "-encoding", "UTF-8", "-d", root.resolve("out").toString(), root.resolve("src/Watched.java").toString()));
    }

    private static void value(Path root, int expected) throws Exception {
        Execution result = run(root, "java", List.of("-Xmx128m", "-XX:ActiveProcessorCount=2",
                "-cp", root.resolve("out").toString(), "Watched"));
        ok(result.exitCode() == 0 && result.output().strip().equals(Integer.toString(expected)),
                "fresh JVM sees compiled value " + expected + ": " + result.output());
    }

    private static Execution run(Path root, String tool, List<String> arguments) throws Exception {
        String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        Path executable = Path.of(System.getProperty("java.home"), "bin", tool + suffix);
        ok(Files.isRegularFile(executable), "full JDK tool is available: " + tool);
        Files.createDirectories(root.resolve("out"));
        Path log = root.resolve("out/command-" + (++commandSequence) + ".log");
        var command = new ArrayList<String>();
        command.add(executable.toString());
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(variable);
        Process process = builder.start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new IOException(tool + " fixture exceeded 30 seconds");
            return new Execution(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private record Execution(int exitCode, String output) {}

    private static int slot(M3FileDeltaLedger.Snapshot snapshot, String path) {
        int found = snapshot.physicalSlot(Path.of(path));
        if (found < 0) throw new AssertionError("missing physical slot: " + path);
        return found;
    }

    private static void state(M3FileDeltaLedger.Snapshot snapshot, String path, long mask, String claim) {
        ok((snapshot.stateAt(slot(snapshot, path)) & mask) != 0, claim);
    }

    private static String relative(Path root, Path file) { return root.relativize(file).toString(); }

    private static int liveFiles(M3FileDeltaLedger.Snapshot snapshot) {
        int count = 0;
        for (int slot = 0; slot < snapshot.slots(); slot++) if (snapshot.liveAt(slot)) count++;
        return count;
    }

    private static boolean hasChanges(M3FileDeltaLedger.Snapshot snapshot) {
        for (int slot = 0; slot < snapshot.slots(); slot++) if (snapshot.changedAt(slot)) return true;
        return false;
    }

    private static void ok(boolean good, String claim) {
        checks++;
        if (!good) throw new AssertionError(claim);
    }
}
