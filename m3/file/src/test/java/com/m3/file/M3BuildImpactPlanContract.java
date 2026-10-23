// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

// Modified 2026-10-21: Exercise count, mode, threshold and safety selection contracts.

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Deterministic Java21 real-file differential contract; no JUnit dependency. */
public final class M3BuildImpactPlanContract {
    private static int checks;
    private M3BuildImpactPlanContract() { }

    private static void check(boolean yes, String message) {
        checks++;
        if (!yes) throw new AssertionError(message);
    }
    private static void equalsSlots(int[] expected, M3BuildImpactPlan plan, String caseName) {
        check(Arrays.equals(expected, plan.impactedSlots()), caseName + " index mask");
        int[] detached = plan.impactedSlots();
        if (detached.length > 0) {
            detached[0] = -1;
            check(plan.impactedSlots()[0] >= 0, caseName + " private index memory");
        }
    }
    private static void reject(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("wrong refusal", t);
        }
        throw new AssertionError("missing refusal " + type.getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-build-impact-");
        try {
            for (String file : List.of("src/A.java", "src/B.java", "src/C.java", "src/D.java",
                    "out/A.class", "pom.xml")) {
                Path path = root.resolve(file);
                Files.createDirectories(path.getParent());
                Files.writeString(path, "original/" + file);
            }
            List<Path> tracked = List.of(Path.of("src/A.java"), Path.of("src/B.java"),
                    Path.of("src/C.java"), Path.of("src/D.java"), Path.of("out/A.class"),
                    Path.of("pom.xml"));
            M3FileDeltaLedger.Snapshot seed = M3FileDeltaLedger.scan(root, null, tracked);
            M3FileDeltaLedger.Snapshot steady = M3FileDeltaLedger.scan(root, seed, tracked);
            check(!steady.changedAt(steady.physicalSlot(Path.of("src/A.java"))), "steady bytes");
            int n = steady.slots();
            int a = steady.physicalSlot(Path.of("src/A.java"));
            int b = steady.physicalSlot(Path.of("src/B.java"));
            int c = steady.physicalSlot(Path.of("src/C.java"));
            int d = steady.physicalSlot(Path.of("src/D.java"));
            int klass = steady.physicalSlot(Path.of("out/A.class"));
            int pom = steady.physicalSlot(Path.of("pom.xml"));
            // A -> B -> C, D and class independent. CSR is over physical slots,
            // not a dependency-list order or a sequence of boxed map nodes.
            int[] counts = new int[n]; counts[a]++; counts[b]++;
            int[] offsets = new int[n + 1];
            for (int i = 0; i < n; i++) offsets[i + 1] = offsets[i] + counts[i];
            int[] targets = new int[2];
            int[] next = offsets.clone(); targets[next[a]++] = b; targets[next[b]++] = c;
            byte[] proof = new byte[n];
            M3BuildImpactPlan clean = M3BuildImpactPlan.compute(steady, true,
                    offsets, targets, proof);
            check(clean.isEmpty() && !clean.requiresConservativeBuild(), "clean cached plan");

            Files.writeString(root.resolve("src/A.java"), "modified A");
            M3FileDeltaLedger.Snapshot changed = M3FileDeltaLedger.scan(root, steady, tracked);
            var full = M3BuildImpactPlan.compute(changed, true, offsets, targets, proof);
            equalsSlots(sorted(a, b, c), full, "complete semantic graph");
            check(full.reasonAt(a) == M3BuildImpactPlan.SOURCE_DELTA, "direct source bit");
            check((full.reasonAt(b) & M3BuildImpactPlan.DEPENDENT) != 0, "dependent B");
            check(full.reasonAt(d) == 0 && full.reasonAt(klass) == 0,
                    "unaffected project and class reused");
            check(!full.requiresConservativeBuild(), "verified dependency graph");

            proof[a] = 1; // compiler-authenticated ABI-isolation result
            var stableAbi = M3BuildImpactPlan.compute(changed, true, offsets, targets, proof);
            equalsSlots(new int[] {a}, stableAbi, "ABI-safe source body");
            check(stableAbi.reasonAt(b) == 0, "unchanged dependent compile");
            proof[a] = 0;
            var conservative = M3BuildImpactPlan.compute(changed, false, null, null, null);
            equalsSlots(sorted(a, b, c, d, klass), conservative, "unverified graph fail closed");
            check(conservative.requiresConservativeBuild(), "unknown graph requires full build");
            check((conservative.reasonAt(d) & M3BuildImpactPlan.CONSERVATIVE) != 0,
                    "unrelated source retained in conservative closure");

            Files.writeString(root.resolve("pom.xml"), "<project>changed</project>");
            M3FileDeltaLedger.Snapshot buildInput = M3FileDeltaLedger.scan(root, changed, tracked);
            var modelDirty = M3BuildImpactPlan.compute(buildInput, true, offsets, targets, proof);
            equalsSlots(sorted(a, b, c, d, klass, pom), modelDirty,
                    "build model change forces invalidation");
            check(modelDirty.requiresConservativeBuild(), "POM change retains Maven authority");

            // Two overlapping changed sources: one safe but also reached through
            // another unsafe source must still propagate to its own consumers.
            Files.writeString(root.resolve("src/A.java"), "modified A twice");
            Files.writeString(root.resolve("src/B.java"), "modified B");
            M3FileDeltaLedger.Snapshot simultaneous = M3FileDeltaLedger.scan(root, buildInput, tracked);
            proof[b] = 1;
            var overlapping = M3BuildImpactPlan.compute(simultaneous, true, offsets, targets, proof);
            equalsSlots(sorted(a, b, c), overlapping, "mixed ABI sources propagate transitively");
            proof[b] = 0;

            reject(IllegalArgumentException.class,
                    () -> M3BuildImpactPlan.compute(changed, true, new int[n], targets, proof));
            reject(IllegalArgumentException.class,
                    () -> M3BuildImpactPlan.compute(changed, true, offsets,
                            new int[] {-1, 9999}, proof));
            reject(IllegalArgumentException.class,
                    () -> M3BuildImpactPlan.compute(changed, false, offsets, targets, proof));
            byte[] invalid = proof.clone(); invalid[a] = 2;
            reject(IllegalArgumentException.class,
                    () -> M3BuildImpactPlan.compute(changed, true, offsets, targets, invalid));
            check((M3BuildImpactPlan.DEPENDENT & M3BuildImpactPlan.CONSERVATIVE) == 0,
                    "distinct state masks");
            selectionContract(root.resolve("selection"));
            System.out.println("M3_BUILD_IMPACT_PASS checks=" + checks);
        } finally {
            try (var entries = Files.walk(root)) {
                entries.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                });
            }
        }
    }

    private static void selectionContract(Path root) throws Exception {
        List<Path> tracked = List.of(Path.of("src/A.java"), Path.of("src/B.java"),
                Path.of("src/C.java"), Path.of("src/D.java"), Path.of("out/A.class"),
                Path.of("pom.xml"), Path.of("src/Retired.java"));
        for (Path relative : tracked) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "original/" + relative);
        }
        var seed = M3FileDeltaLedger.scan(root, null, tracked);
        var initial = M3BuildImpactPlan.compute(seed, false, null, null, null)
                .select(seed, M3BuildImpactPlan.Mode.AUTO, -1, -1);
        selectionCounts(initial, 7, 5, 7, 0, 0, 5, 5, "initial files");
        Files.delete(root.resolve("src/Retired.java"));
        var retired = M3FileDeltaLedger.scan(root, seed, tracked);
        var steady = M3FileDeltaLedger.scan(root, retired, tracked);
        int n = steady.slots();
        int a = steady.physicalSlot(Path.of("src/A.java"));
        int b = steady.physicalSlot(Path.of("src/B.java"));
        int c = steady.physicalSlot(Path.of("src/C.java"));
        int d = steady.physicalSlot(Path.of("src/D.java"));
        check(n == 7, "historical tombstone retains physical slot");
        int[] counts = new int[n]; counts[a]++; counts[b]++;
        int[] offsets = new int[n + 1];
        for (int i = 0; i < n; i++) offsets[i + 1] = offsets[i] + counts[i];
        int[] targets = new int[2];
        int[] next = offsets.clone(); targets[next[a]++] = b; targets[next[b]++] = c;
        byte[] proof = new byte[n];
        var unchanged = M3BuildImpactPlan.compute(steady, true, offsets, targets, proof);
        var noop = unchanged.select(steady, M3BuildImpactPlan.Mode.AUTO, 0, 0);
        selectionDecision(noop, M3BuildImpactPlan.Decision.NO_OP,
                M3BuildImpactPlan.SelectionReason.NO_CHANGES, "unchanged zero thresholds");
        selectionCounts(noop, 6, 4, 0, 0, 0, 0, 0, "live totals exclude tombstone");
        check(!noop.hasWork(), "no delta has no work");
        check(unchanged.select(steady, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1)
                .decision() == M3BuildImpactPlan.Decision.NO_OP, "incremental no delta is no-op");
        var forced = unchanged.select(steady, M3BuildImpactPlan.Mode.FULL, -1, -1);
        selectionDecision(forced, M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.REQUESTED_FULL, "full without delta");
        selectionCounts(forced, 6, 4, 0, 0, 0, 0, 4, "forced full selection");
        check(forced.hasWork() && Arrays.equals(sorted(a, b, c, d), forced.selectedSourceSlots()),
                "explicit full selects all live sources");

        Files.writeString(root.resolve("src/A.java"), "modified A");
        var changed = M3FileDeltaLedger.scan(root, steady, tracked);
        var impact = M3BuildImpactPlan.compute(changed, true, offsets, targets, proof);
        var incremental = impact.select(changed, M3BuildImpactPlan.Mode.AUTO, 4, 100);
        selectionDecision(incremental, M3BuildImpactPlan.Decision.INCREMENTAL,
                M3BuildImpactPlan.SelectionReason.BELOW_THRESHOLDS, "below both thresholds");
        selectionCounts(incremental, 6, 4, 0, 1, 0, 3, 3, "source dependency closure");
        check(incremental.affectedSourcePercent() == 75.0,
                "percent denominator is live sources, not tracked files or slots");
        check(Arrays.equals(sorted(a, b, c), incremental.affectedSourceSlots())
                && Arrays.equals(sorted(a, b, c), incremental.selectedSourceSlots()),
                "incremental selects source closure");
        int[] detachedAffected = incremental.affectedSourceSlots(); detachedAffected[0] = -1;
        int[] detachedSelected = incremental.selectedSourceSlots(); detachedSelected[0] = -1;
        check(incremental.affectedSourceSlots()[0] >= 0
                && incremental.selectedSourceSlots()[0] >= 0, "selection arrays are detached");
        var countFull = impact.select(changed, M3BuildImpactPlan.Mode.AUTO, 3, -1);
        selectionDecision(countFull, M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.AFFECTED_FILE_THRESHOLD, "inclusive count boundary");
        selectionCounts(countFull, 6, 4, 0, 1, 0, 3, 4, "threshold expands selection only");
        selectionDecision(impact.select(changed, M3BuildImpactPlan.Mode.AUTO, -1, 75),
                M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.AFFECTED_PERCENT_THRESHOLD, "inclusive percentage boundary");
        check(impact.select(changed, M3BuildImpactPlan.Mode.AUTO, -1, 75.01).decision()
                == M3BuildImpactPlan.Decision.INCREMENTAL, "percentage above actual impact");
        selectionDecision(impact.select(changed, M3BuildImpactPlan.Mode.AUTO, 3, 75),
                M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.AFFECTED_FILE_THRESHOLD, "count reason has precedence");
        var requested = impact.select(changed, M3BuildImpactPlan.Mode.INCREMENTAL, 0, 0);
        selectionDecision(requested, M3BuildImpactPlan.Decision.INCREMENTAL,
                M3BuildImpactPlan.SelectionReason.REQUESTED_INCREMENTAL, "request bypasses cost thresholds");
        check(requested.requestedMode() == M3BuildImpactPlan.Mode.INCREMENTAL,
                "selection retains requested policy");
        check(impact.select(changed, M3BuildImpactPlan.Mode.AUTO, -1, -1).decision()
                == M3BuildImpactPlan.Decision.INCREMENTAL, "both thresholds may be disabled");
        var repeatedObservation = M3FileDeltaLedger.scan(root, steady, tracked);
        check(Arrays.equals(changed.digestAt(a), repeatedObservation.digestAt(a)),
                "repeat observation has identical bytes but distinct snapshot identity");
        reject(IllegalArgumentException.class,
                () -> impact.select(repeatedObservation, M3BuildImpactPlan.Mode.AUTO, -1, -1));
        Files.writeString(root.resolve("src/A.java"), "different A generation");
        var differentGeneration = M3FileDeltaLedger.scan(root, steady, tracked);
        check(differentGeneration.slots() == changed.slots()
                && !Arrays.equals(changed.digestAt(a), differentGeneration.digestAt(a)),
                "different content generation retains dimensions");
        for (int slot = 0; slot < n; slot++) {
            check(differentGeneration.stateAt(slot) == changed.stateAt(slot),
                    "different generation retains every direct delta state bit");
        }
        reject(IllegalArgumentException.class,
                () -> impact.select(differentGeneration, M3BuildImpactPlan.Mode.FULL, -1, -1));
        Files.writeString(root.resolve("src/A.java"), "modified A");
        proof[a] = 1;
        var safeBody = M3BuildImpactPlan.compute(changed, true, offsets, targets, proof)
                .select(changed, M3BuildImpactPlan.Mode.AUTO, 2, 50);
        selectionCounts(safeBody, 6, 4, 0, 1, 0, 1, 1, "semantic safe-body proof is preserved");
        proof[a] = 0;
        var unknown = M3BuildImpactPlan.compute(changed, false, null, null, null)
                .select(changed, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1);
        selectionDecision(unknown, M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.UNVERIFIED_GRAPH, "incremental cannot waive missing proof");
        selectionCounts(unknown, 6, 4, 0, 1, 0, 4, 4, "unknown graph compiles all live sources");

        Files.writeString(root.resolve("pom.xml"), "changed build model");
        var model = M3FileDeltaLedger.scan(root, changed, tracked);
        var modelPlan = M3BuildImpactPlan.compute(model, true, offsets, targets, proof);
        var modelSelection = modelPlan.select(model, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1);
        selectionDecision(modelSelection, M3BuildImpactPlan.Decision.FULL,
                M3BuildImpactPlan.SelectionReason.BUILD_INPUT_CHANGE, "build-input correctness fallback");
        selectionCounts(modelSelection, 6, 4, 0, 1, 0, 4, 4, "build-input direct count");
        reject(IllegalArgumentException.class,
                () -> impact.select(steady, M3BuildImpactPlan.Mode.AUTO, -1, -1));
        reject(IllegalArgumentException.class,
                () -> impact.select(model, M3BuildImpactPlan.Mode.FULL, -1, -1));

        Files.delete(root.resolve("src/D.java"));
        var deletion = M3FileDeltaLedger.scan(root, model, tracked);
        var deletionPlan = M3BuildImpactPlan.compute(deletion, true, offsets, targets, proof);
        var cleanup = deletionPlan.select(deletion, M3BuildImpactPlan.Mode.AUTO, 0, 0);
        selectionDecision(cleanup, M3BuildImpactPlan.Decision.INCREMENTAL,
                M3BuildImpactPlan.SelectionReason.BELOW_THRESHOLDS, "cleanup-only work");
        selectionCounts(cleanup, 5, 3, 0, 0, 1, 0, 0, "removal is not a live source");
        check(cleanup.hasWork() && Arrays.equals(new int[] {d}, cleanup.removedSlots()),
                "empty compile selection still retains removal cleanup");
        int[] detachedRemoved = cleanup.removedSlots(); detachedRemoved[0] = -1;
        check(cleanup.removedSlots()[0] == d, "removal array is detached");
        var fullCleanup = deletionPlan.select(deletion, M3BuildImpactPlan.Mode.FULL, -1, -1);
        check(Arrays.equals(sorted(a, b, c), fullCleanup.selectedSourceSlots())
                && Arrays.equals(new int[] {d}, fullCleanup.removedSlots()),
                "full selection excludes tombstones and preserves cleanup");
        var settled = M3FileDeltaLedger.scan(root, deletion, tracked);
        var settledSelection = M3BuildImpactPlan.compute(settled, true, offsets, targets, proof)
                .select(settled, M3BuildImpactPlan.Mode.AUTO, -1, -1);
        selectionCounts(settledSelection, 5, 3, 0, 0, 0, 0, 0, "old removals do not recur");
        check(!settledSelection.hasWork(), "settled tombstones do not trigger work");

        Files.delete(root.resolve("src/A.java"));
        var deletedOwner = M3FileDeltaLedger.scan(root, settled, tracked);
        var affectedByDeletion = M3BuildImpactPlan.compute(deletedOwner, true, offsets, targets, proof)
                .select(deletedOwner, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1);
        selectionCounts(affectedByDeletion, 4, 2, 0, 0, 1, 2, 2, "removed owner propagates through old graph");
        check(Arrays.equals(sorted(b, c), affectedByDeletion.selectedSourceSlots())
                && Arrays.equals(new int[] {a}, affectedByDeletion.removedSlots()),
                "dependency deletion separates live consumers from removed owner");

        Files.writeString(root.resolve("src/E.java"), "created E");
        var expandedTracked = new java.util.ArrayList<>(tracked);
        expandedTracked.add(Path.of("src/E.java"));
        var creation = M3FileDeltaLedger.scan(root, deletedOwner, expandedTracked);
        var creationPlan = noEdges(creation);
        var createdSelection = creationPlan.select(creation, M3BuildImpactPlan.Mode.AUTO, -1, 34);
        selectionCounts(createdSelection, 5, 3, 1, 0, 0, 1, 1, "creation appends a live source slot");
        check(createdSelection.decision() == M3BuildImpactPlan.Decision.INCREMENTAL,
                "fractional percent below threshold does not round up");
        check(creationPlan.select(creation, M3BuildImpactPlan.Mode.AUTO, -1, 33).decision()
                == M3BuildImpactPlan.Decision.FULL, "fractional percent above threshold selects full");
        reject(IllegalArgumentException.class,
                () -> impact.select(creation, M3BuildImpactPlan.Mode.AUTO, -1, -1));
        reject(IllegalArgumentException.class,
                () -> creationPlan.select(changed, M3BuildImpactPlan.Mode.AUTO, -1, -1));

        Files.writeString(root.resolve("src/B.java"), "modified B");
        Files.delete(root.resolve("src/C.java"));
        Files.writeString(root.resolve("src/F.java"), "created F");
        expandedTracked.add(Path.of("src/F.java"));
        var mixed = M3FileDeltaLedger.scan(root, creation, expandedTracked);
        var mixedSelection = noEdges(mixed).select(mixed, M3BuildImpactPlan.Mode.AUTO, -1, -1);
        selectionCounts(mixedSelection, 5, 3, 1, 1, 1, 2, 2, "mixed direct delta counts");
        check(Arrays.equals(new int[] {c}, mixedSelection.removedSlots()), "mixed removal retains original slot");

        for (double invalid : new double[] {-2, -0.5, 100.01, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            reject(IllegalArgumentException.class,
                    () -> impact.select(changed, M3BuildImpactPlan.Mode.AUTO, -1, invalid));
        }
        reject(IllegalArgumentException.class,
                () -> impact.select(changed, M3BuildImpactPlan.Mode.AUTO, -2, -1));
        reject(NullPointerException.class,
                () -> impact.select(null, M3BuildImpactPlan.Mode.AUTO, -1, -1));
        reject(NullPointerException.class, () -> impact.select(changed, null, -1, -1));
        Path emptyRoot = Files.createDirectory(root.resolve("empty"));
        var empty = M3FileDeltaLedger.scan(emptyRoot, null, List.of());
        var emptyPlan = noEdges(empty);
        var emptyNoop = emptyPlan.select(empty, M3BuildImpactPlan.Mode.AUTO, 0, 0);
        check(!emptyNoop.hasWork() && emptyNoop.affectedSourcePercent() == 0.0,
                "empty project has finite percentage and no automatic work");
        var emptyFull = emptyPlan.select(empty, M3BuildImpactPlan.Mode.FULL, -1, -1);
        check(emptyFull.hasWork() && emptyFull.decision() == M3BuildImpactPlan.Decision.FULL
                && emptyFull.selectedSourceCount() == 0, "explicit full is retained for empty project");
    }

    private static M3BuildImpactPlan noEdges(M3FileDeltaLedger.Snapshot snapshot) {
        return M3BuildImpactPlan.compute(snapshot, true, new int[snapshot.slots() + 1],
                new int[0], new byte[snapshot.slots()]);
    }

    private static void selectionDecision(M3BuildImpactPlan.Selection selection,
            M3BuildImpactPlan.Decision expected, M3BuildImpactPlan.SelectionReason reason,
            String name) {
        check(selection.decision() == expected, name + " decision");
        check(selection.reason() == reason, name + " reason");
    }

    private static void selectionCounts(M3BuildImpactPlan.Selection selection,
            int files, int sources, int created, int modified, int removed,
            int affected, int selected, String name) {
        check(selection.totalLiveFiles() == files, name + " live files");
        check(selection.totalLiveSources() == sources, name + " live sources");
        check(selection.createdFiles() == created, name + " created files");
        check(selection.modifiedFiles() == modified, name + " modified files");
        check(selection.removedFiles() == removed, name + " removed files");
        check(selection.directChangedFiles() == created + modified + removed, name + " direct total");
        check(selection.affectedSourceCount() == affected, name + " affected live sources");
        check(selection.selectedSourceCount() == selected, name + " selected live sources");
    }

    private static int[] sorted(int... slots) {
        Arrays.sort(slots);
        return slots;
    }
}
