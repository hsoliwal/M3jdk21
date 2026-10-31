// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

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

    private static int[] sorted(int... slots) {
        Arrays.sort(slots);
        return slots;
    }
}
