// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file.test;

import com.m3.file.M3BuildImpactPlan;
import com.m3.file.M3FileDeltaLedger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Public observations for original/applied comparison; finite graphs use an independent closure. */
public final class M3BuildImpactScratchContract {
    private static final MessageDigest OBSERVATIONS = digest();
    private static int checks;

    private M3BuildImpactScratchContract() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) throw new IllegalArgumentException("no arguments expected");
        Path root = Files.createTempDirectory("m3-impact-scratch-contract-");
        try {
            exercise(root);
            System.out.println("M3_IMPACT_SCRATCH_PUBLIC_PASS checks=" + checks
                    + " graphCases=4096 digest=" + HexFormat.of().formatHex(OBSERVATIONS.digest()));
        } finally {
            remove(root);
        }
    }

    private static void exercise(Path root) throws IOException {
        List<Path> tracked = List.of(Path.of("src/A.java"), Path.of("src/B.java"),
                Path.of("src/C.java"), Path.of("out/A.class"), Path.of("pom.xml"));
        for (Path relative : tracked) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "original:" + relative);
        }
        M3FileDeltaLedger.Snapshot seed = M3FileDeltaLedger.scan(root, null, tracked);
        M3FileDeltaLedger.Snapshot steady = M3FileDeltaLedger.scan(root, seed, tracked);
        int n = steady.slots();
        int[] sources = {steady.physicalSlot(Path.of("src/A.java")),
                steady.physicalSlot(Path.of("src/B.java")), steady.physicalSlot(Path.of("src/C.java"))};
        int[] noEdges = new int[n + 1];
        int[] noTargets = new int[0];
        byte[] safe = new byte[n];

        M3BuildImpactPlan initial = M3BuildImpactPlan.compute(seed, false, null, null, null);
        check(initial.requiresConservativeBuild(), "initial build input forces conservative plan");
        record("initial", initial, seed);
        M3BuildImpactPlan clean = M3BuildImpactPlan.compute(steady, true, noEdges, noTargets, safe);
        M3BuildImpactPlan cleanAgain = M3BuildImpactPlan.compute(steady, true, noEdges, noTargets, safe);
        M3BuildImpactPlan unknownClean = M3BuildImpactPlan.compute(steady, false, null, null, null);
        check(clean != cleanAgain, "fresh plan object on repeated no-work input");
        check(clean.isEmpty() && unknownClean.isEmpty(), "no changes return no impacted slots");
        check(!clean.requiresConservativeBuild() && !unknownClean.requiresConservativeBuild(),
                "no-change path retains its existing nonconservative disposition");
        check(clean.select(steady, M3BuildImpactPlan.Mode.AUTO, 0, 0).decision()
                == M3BuildImpactPlan.Decision.NO_OP, "thresholds do not turn empty plan into work");
        M3BuildImpactPlan.Selection explicit = clean.select(steady, M3BuildImpactPlan.Mode.FULL, -1, -1);
        check(explicit.decision() == M3BuildImpactPlan.Decision.FULL
                && explicit.selectedSourceCount() == 3, "explicit FULL on no-change snapshot");
        record("clean-verified", clean, steady);
        record("clean-unverified", unknownClean, steady);
        invalidGraphsStillFail(steady);

        Files.writeString(root.resolve("src/A.java"), "changed A");
        Files.writeString(root.resolve("src/B.java"), "changed B");
        M3FileDeltaLedger.Snapshot changed = M3FileDeltaLedger.scan(root, steady, tracked);
        M3BuildImpactPlan conservative = M3BuildImpactPlan.compute(changed, false, null, null, null);
        check(conservative.requiresConservativeBuild(), "unknown graph remains conservative");
        check(conservative.select(changed, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1).decision()
                == M3BuildImpactPlan.Decision.FULL, "incremental request cannot waive graph proof");
        record("conservative", conservative, changed);
        finiteGraphs(changed, sources);

        Files.writeString(root.resolve("pom.xml"), "changed build model");
        M3FileDeltaLedger.Snapshot model = M3FileDeltaLedger.scan(root, changed, tracked);
        M3BuildImpactPlan modelPlan = M3BuildImpactPlan.compute(model, true, noEdges, noTargets, safe);
        check(modelPlan.requiresConservativeBuild(), "verified graph cannot waive build-input delta");
        check(modelPlan.select(model, M3BuildImpactPlan.Mode.INCREMENTAL, -1, -1).reason()
                == M3BuildImpactPlan.SelectionReason.BUILD_INPUT_CHANGE, "full fallback reason");
        record("model", modelPlan, model);
        invalidGraphsStillFail(model);

        Files.delete(root.resolve("src/C.java"));
        M3FileDeltaLedger.Snapshot removed = M3FileDeltaLedger.scan(root, model, tracked);
        M3BuildImpactPlan cleanup = M3BuildImpactPlan.compute(removed, true, noEdges, noTargets, safe);
        M3BuildImpactPlan.Selection selection = cleanup.select(removed, M3BuildImpactPlan.Mode.AUTO, 0, 0);
        check(selection.hasWork() && selection.selectedSourceCount() == 0
                && Arrays.equals(selection.removedSlots(), new int[] {sources[2]}),
                "cleanup-only work retains removal without a live source selection");
        record("cleanup", cleanup, removed);
        M3FileDeltaLedger.Snapshot settled = M3FileDeltaLedger.scan(root, removed, tracked);
        M3BuildImpactPlan settledPlan = M3BuildImpactPlan.compute(settled, true, noEdges, noTargets, safe);
        check(settledPlan.isEmpty(), "historical tombstones do not create new work");
        record("settled", settledPlan, settled);
        refused("snapshot-instance", IllegalArgumentException.class,
                "file plan belongs to different snapshot instance",
                () -> clean.select(settled, M3BuildImpactPlan.Mode.FULL, -1, -1));
    }

    private static void invalidGraphsStillFail(M3FileDeltaLedger.Snapshot snapshot) {
        int count = snapshot.slots();
        int[] offsets = new int[count + 1];
        byte[] safe = new byte[count];
        refused("null-snapshot", NullPointerException.class, "snapshot",
                () -> M3BuildImpactPlan.compute(null, true, null, null, null));
        refused("null-offsets", NullPointerException.class, "dependentOffsets",
                () -> M3BuildImpactPlan.compute(snapshot, true, null, null, null));
        refused("null-targets", NullPointerException.class, "dependentTargets",
                () -> M3BuildImpactPlan.compute(snapshot, true, offsets, null, null));
        refused("null-safe", NullPointerException.class, "propagationSafe",
                () -> M3BuildImpactPlan.compute(snapshot, true, offsets, new int[0], null));
        refused("unverified-arrays", IllegalArgumentException.class,
                "unverified dependencies must not be supplied",
                () -> M3BuildImpactPlan.compute(snapshot, false, offsets, new int[0], safe));
        refused("dimensions", IllegalArgumentException.class, "mismatched M3 dependency-array dimensions",
                () -> M3BuildImpactPlan.compute(snapshot, true, new int[count], new int[0], safe));
        byte[] invalidSafe = safe.clone();
        invalidSafe[0] = 2;
        refused("safe-value", IllegalArgumentException.class, "invalid M3 dependency offsets or ABI flags",
                () -> M3BuildImpactPlan.compute(snapshot, true, offsets, new int[0], invalidSafe));
        int[] oneEdge = new int[count + 1];
        oneEdge[count] = 1;
        refused("endpoint-low", IllegalArgumentException.class, "invalid dependent physical slot",
                () -> M3BuildImpactPlan.compute(snapshot, true, oneEdge, new int[] {-1}, safe));
        refused("endpoint-high", IllegalArgumentException.class, "invalid dependent physical slot",
                () -> M3BuildImpactPlan.compute(snapshot, true, oneEdge, new int[] {count}, safe));
        int[] invalidOffsets = offsets.clone();
        invalidOffsets[1] = -1;
        refused("offset-order", IllegalArgumentException.class, "invalid M3 dependency offsets or ABI flags",
                () -> M3BuildImpactPlan.compute(snapshot, true, invalidOffsets, new int[0], safe));
    }

    private static void finiteGraphs(M3FileDeltaLedger.Snapshot snapshot, int[] sources) {
        int count = snapshot.slots();
        for (int graph = 0; graph < 512; graph++) {
            int[] offsets = new int[count + 1];
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if ((graph & (1 << (from * 3 + to))) != 0) offsets[sources[from] + 1]++;
                }
            }
            for (int slot = 0; slot < count; slot++) offsets[slot + 1] += offsets[slot];
            int[] targets = new int[offsets[count]];
            int[] cursor = offsets.clone();
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if ((graph & (1 << (from * 3 + to))) != 0) targets[cursor[sources[from]]++] = sources[to];
                }
            }
            for (int mask = 0; mask < 8; mask++) {
                byte[] safe = new byte[count];
                for (int source = 0; source < 3; source++) safe[sources[source]] = (byte) ((mask >>> source) & 1);
                int[] expected = independentClosure(snapshot, sources, graph, safe);
                M3BuildImpactPlan plan = M3BuildImpactPlan.compute(snapshot, true, offsets, targets, safe);
                check(!plan.requiresConservativeBuild(), "finite complete graph keeps verified disposition");
                int expectedCount = 0;
                for (int slot = 0; slot < count; slot++) {
                    check(plan.reasonAt(slot) == expected[slot], "finite graph reason mask");
                    if (expected[slot] != 0) expectedCount++;
                }
                int[] expectedSlots = new int[expectedCount];
                for (int slot = 0, at = 0; slot < count; slot++) {
                    if (expected[slot] != 0) expectedSlots[at++] = slot;
                }
                check(Arrays.equals(expectedSlots, plan.impactedSlots()), "ascending physical-slot order");
                int[] detached = plan.impactedSlots();
                detached[0] = -1;
                check(plan.impactedSlots()[0] >= 0, "result remains defensive");
                M3BuildImpactPlan.Selection selection = plan.select(snapshot, M3BuildImpactPlan.Mode.AUTO, 3, -1);
                check(selection.decision() == (expectedCount == 3
                        ? M3BuildImpactPlan.Decision.FULL : M3BuildImpactPlan.Decision.INCREMENTAL),
                        "cost threshold still follows affected live source count");
                record("graph:" + graph + ":safe:" + mask, plan, snapshot);
            }
        }
    }

    /** Repeated all-edge closure is deliberately independent of the production BFS queue. */
    private static int[] independentClosure(M3FileDeltaLedger.Snapshot snapshot, int[] sources,
            int graph, byte[] safe) {
        int[] reasons = new int[snapshot.slots()];
        boolean[] active = new boolean[reasons.length];
        for (int slot = 0; slot < reasons.length; slot++) {
            if (snapshot.changedAt(slot)) {
                reasons[slot] = M3BuildImpactPlan.SOURCE_DELTA;
                active[slot] = safe[slot] == 0;
            }
        }
        boolean expanded;
        do {
            expanded = false;
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if (active[sources[from]] && (graph & (1 << (from * 3 + to))) != 0) {
                        int target = sources[to];
                        reasons[target] |= M3BuildImpactPlan.DEPENDENT;
                        if (!active[target]) { active[target] = true; expanded = true; }
                    }
                }
            }
        } while (expanded);
        return reasons;
    }

    private static void record(String label, M3BuildImpactPlan plan, M3FileDeltaLedger.Snapshot snapshot) {
        StringBuilder value = new StringBuilder(label).append(':').append(plan.size()).append(':')
                .append(plan.isEmpty()).append(':').append(plan.requiresConservativeBuild())
                .append(':').append(Arrays.toString(plan.impactedSlots()));
        for (int slot = 0; slot < snapshot.slots(); slot++) value.append(':').append(plan.reasonAt(slot));
        for (M3BuildImpactPlan.Mode mode : M3BuildImpactPlan.Mode.values()) {
            M3BuildImpactPlan.Selection selected = plan.select(snapshot, mode, 3, 75);
            value.append('|').append(selected.requestedMode()).append(':').append(selected.decision())
                    .append(':').append(selected.reason()).append(':').append(selected.hasWork())
                    .append(':').append(selected.totalLiveFiles()).append(':').append(selected.totalLiveSources())
                    .append(':').append(selected.createdFiles()).append(':').append(selected.modifiedFiles())
                    .append(':').append(selected.removedFiles()).append(':').append(selected.affectedSourcePercent())
                    .append(':').append(Arrays.toString(selected.affectedSourceSlots()))
                    .append(':').append(Arrays.toString(selected.selectedSourceSlots()))
                    .append(':').append(Arrays.toString(selected.removedSlots()));
        }
        put(value.toString());
    }

    private static void refused(String label, Class<? extends RuntimeException> type, String message,
            Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("missing refusal: " + label);
        } catch (RuntimeException failure) {
            check(failure.getClass() == type && message.equals(failure.getMessage()),
                    "exact refusal: " + label + ':' + failure);
            put(label + ':' + failure.getClass().getName() + ':' + failure.getMessage());
        }
    }

    private static void put(String value) {
        for (int shift = 24; shift >= 0; shift -= 8) OBSERVATIONS.update((byte) (value.length() >>> shift));
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            OBSERVATIONS.update((byte) (unit >>> 8));
            OBSERVATIONS.update((byte) unit);
        }
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void remove(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException failure) { throw new UncheckedIOException(failure); }
            });
        }
    }
}
