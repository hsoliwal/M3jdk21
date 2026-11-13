/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dependency-free Git oracle for source-sealed donor-delta candidate generation. */
public final class DonorDeltaPlannerSelfTest {
    private static int checks;

    private DonorDeltaPlannerSelfTest() { }

    /** Runs actual Java21/Git tests; no unexecuted assertion is reported as PASS. */
    public static void main(String[] args) throws Exception {
        Path scratch = Files.createTempDirectory("m3-donor-delta-test-");
        try {
            Path repo = scratch.resolve("donor");
            Files.createDirectories(repo);
            git(repo, "init", "-q");
            git(repo, "config", "user.email", "ci@example.invalid");
            git(repo, "config", "user.name", "M3 Test");
            file(repo, "src/java.base/share/classes/java/util/Fixture.java", "class Fixture { int x() { return 1; } }\n");
            file(repo, "src/hotspot/share/runtime/check.cpp", "int check() { return 1; }\n");
            file(repo, "make/Compiler.gmk", "all:\n\t@true\n");
            file(repo, "README.md", "before\n");
            git(repo, "add", "-A");
            git(repo, "commit", "-qm", "baseline");
            String baseline = git(repo, "rev-parse", "HEAD").trim();
            require(baseline.length() == 40, "baseline full SHA1");

            file(repo, "src/java.base/share/classes/java/util/Fixture.java", "class Fixture { int x() { return 2; } }\n");
            file(repo, "src/java.base/share/native/libjava/fastsearch.c", "int fastsearch(void) { return -1; }\n");
            Files.delete(repo.resolve("README.md"));
            git(repo, "add", "-A");
            // Git index mode controls the committed tree on Windows and POSIX alike.
            git(repo, "update-index", "--chmod=+x", "make/Compiler.gmk");
            git(repo, "commit", "-qm", "donor revision");
            String head = git(repo, "rev-parse", "HEAD").trim();
            require(!baseline.equals(head), "different revisions");

            Path out = scratch.resolve("output");
            var result = DonorDeltaPlanner.generate(repo, baseline, head, "bisheng-21", out);
            require(result.candidateCount() == 4, "four distinct Git deltas");
            String candidates = Files.readString(result.candidatesPath());
            String receipt = Files.readString(result.receiptPath());
            String[] candidateRows = candidates.stripTrailing().split("\n");
            require(candidateRows.length == 5, "candidate header and four rows");
            require(candidateRows[0].equals(DonorDeltaPlanner.CANDIDATES_HEADER), "exact header");
            require(candidates.contains("\tREADME.md\tREMOVED\t"), "deleted file visible");
            require(candidates.contains("\tmake/Compiler.gmk\tMODE_CHANGED\t"), "mode change visible");
            require(candidates.contains("/Fixture.java\tMODIFIED\t"), "source modification visible");
            require(candidates.contains("/fastsearch.c\tADDED\t"), "native addition visible");
            require(!candidates.contains("check.cpp"), "unchanged native file omitted");
            require(candidates.contains("\tREVIEW_ONLY\tHOLD_UNQUALIFIED\n"), "all changes unqualified");
            require(!candidates.contains("\tPASS\t"), "no invented qualification");
            require(receipt.contains("license_gate\tNOT_RUN"), "no invented licence proof");
            require(receipt.contains("jni_native_gate\tNOT_RUN"), "no invented JNI proof");
            require(receipt.contains("performance_gate\tNOT_RUN"), "no invented benchmark");
            require(receipt.contains("baseline_commit\t" + baseline), "exact baseline bound");
            require(receipt.contains("donor_commit\t" + head), "exact donor bound");
            require(receipt.contains("change_mode_changed\t1"), "mode change counted");

            byte[] previousCandidates = Files.readAllBytes(result.candidatesPath());
            byte[] previousReceipt = Files.readAllBytes(result.receiptPath());
            var repeat = DonorDeltaPlanner.generate(repo, baseline, head, "bisheng-21", out);
            require(result.candidatesSha256().equals(repeat.candidatesSha256()), "fixed-point digest");
            require(Arrays.equals(previousCandidates, Files.readAllBytes(repeat.candidatesPath())),
                    "candidate exact replay");
            require(Arrays.equals(previousReceipt, Files.readAllBytes(repeat.receiptPath())),
                    "receipt exact replay");

            // The uncommitted tree has no authority to change a Git-commit census.
            file(repo, "src/hotspot/share/runtime/check.cpp", "int check() { return 9999; }\n");
            var isolated = DonorDeltaPlanner.generate(repo, baseline, head, "bisheng-21", out);
            require(Arrays.equals(previousCandidates, Files.readAllBytes(isolated.candidatesPath())),
                    "working-tree isolation");

            refusal(() -> DonorDeltaPlanner.generate(repo, "HEAD", head, "bisheng-21",
                    scratch.resolve("bad-ref")), "mutable baseline revision refused");
            refusal(() -> DonorDeltaPlanner.generate(repo, head, baseline, "bisheng-21",
                    scratch.resolve("reverse")), "nonancestor reversed pair refused");
            refusal(() -> DonorDeltaPlanner.generate(repo, baseline, head, "../../bad",
                    scratch.resolve("bad-id")), "unsafe donor ID refused");
            refusal(() -> DonorDeltaPlanner.generate(repo.resolve("src"), baseline, head,
                    "bisheng-21", scratch.resolve("subdirectory")), "nonroot repository refused");
            refusal(() -> DonorDeltaPlanner.generate(repo, baseline, "0".repeat(40),
                    "bisheng-21", scratch.resolve("missing-object")), "missing Git object refused");

            // Occupied output differs: preflight all outputs and hard fail without overwriting.
            Files.writeString(out.resolve("RECEIPT.tsv"), "drift\n");
            refusal(() -> DonorDeltaPlanner.generate(repo, baseline, head, "bisheng-21", out),
                    "receipt drift refused");
            require(Arrays.equals(previousCandidates, Files.readAllBytes(out.resolve("CANDIDATES.tsv"))),
                    "unrelated candidate artifact not modified after refusal");

            // Empty delta is meaningful and deterministic, not a guessed donor optimization.
            var empty = DonorDeltaPlanner.generate(repo, baseline, baseline, "openjdk-21u",
                    scratch.resolve("empty"));
            require(empty.candidateCount() == 0, "equal commit zero delta");
            require(Files.readString(empty.candidatesPath()).equals(
                    DonorDeltaPlanner.CANDIDATES_HEADER + "\n"), "empty ledger header");

            // Git filenames containing TAB cannot round-trip into the exact TSV format.
            Files.writeString(repo.resolve("bad\tfile.java"), "class Bad { }\n");
            git(repo, "add", "-A");
            git(repo, "commit", "-qm", "malformed tracked path");
            String brokenHead = git(repo, "rev-parse", "HEAD").trim();
            refusal(() -> DonorDeltaPlanner.generate(repo, head, brokenHead, "bisheng-21",
                    scratch.resolve("bad-path")), "malformed Git tracked path refused");

            System.out.printf("M3_DONOR_DELTA_SELF_TEST\tchecks=%d\tresult=PASS%n", checks);
        } finally {
            deleteTree(scratch);
        }
    }

    private static void file(Path root, String path, String value) throws IOException {
        Path destination = root.resolve(path);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, value, StandardCharsets.UTF_8);
    }

    private static String git(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.waitFor() != 0) {
            throw new IOException("test Git command failed: " + String.join(" ", args) + ": " + output);
        }
        return output;
    }

    private static void refusal(Action action, String label) throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (IOException | IllegalArgumentException exception) {
            failed = true;
        }
        require(failed, label);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("M3_DONOR_DELTA_TEST_FAILURE: " + label);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }
}
