// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Java21 standalone ABI/graph projection oracle over actual M3File generations. */
public final class M3FileBuildImpactContract {
    private static int checks;

    private M3FileBuildImpactContract() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-impact-");
        try {
            Path a = Path.of("src/A.java");
            Path b = Path.of("src/B.java");
            Path c = Path.of("src/C.java");
            Path d = Path.of("src/D.java");
            Path bin = Path.of("target/B.class");
            Path pom = Path.of("pom.xml");
            List<Path> files = List.of(a, b, c, d, bin, pom);
            Files.createDirectories(root.resolve("src"));
            Files.createDirectories(root.resolve("target"));
            for (Path file : files) Files.writeString(root.resolve(file), file.toString());
            var created = M3FileDeltaLedger.scan(root, null, files);
            var same = M3FileDeltaLedger.scan(root, created, files);
            int ai = same.physicalSlot(a), bi = same.physicalSlot(b), ci = same.physicalSlot(c);
            int di = same.physicalSlot(d), xi = same.physicalSlot(bin), pi = same.physicalSlot(pom);
            ok(ai >= 0 && bi >= 0 && ci >= 0 && di >= 0 && xi >= 0 && pi >= 0,
                    "all original physical identities retained");
            var empty = M3FileBuildImpact.plan(same, new long[same.slots()],
                    new int[] {ai, bi}, new int[] {bi, ci}, true);
            ok(empty.affectedSlots().length == 0 && !empty.globalInvalidation(), "exact no-op");
            ok(empty.unaffectedAt(ai) && !empty.requiresWorkAt(ci), "safe candidate only");
            failure(IndexOutOfBoundsException.class,
                    () -> M3FileBuildImpact.plan(same, new long[same.slots()],
                            new int[] {ai}, new int[] {-1}, true),
                    "invalid no-op graph edge must still be refused");

            Files.writeString(root.resolve(a), "changed implementation A");
            var source = M3FileDeltaLedger.scan(root, same, files);
            long[] facts = new long[source.slots()];
            int[] dep = {ai, bi, xi};
            int[] use = {bi, ci, ci};
            var unproven = M3FileBuildImpact.plan(source, facts, dep, use, true);
            eq(unproven.affectedSlots(), sorted(ai, bi, ci), "unproven ABI reaches reverse dependencies");
            ok(unproven.reasonAt(ai) == M3FileBuildImpact.DIRECT, "original source direct");
            ok((unproven.reasonAt(ci) & M3FileBuildImpact.DEPENDENT) != 0, "transitive dependent");
            ok(unproven.unaffectedAt(di), "unrelated source can be candidate");
            facts[ai] = M3FileBuildImpact.ABI_PROVEN_SAME;
            var stable = M3FileBuildImpact.plan(source, facts, dep, use, true);
            eq(stable.affectedSlots(), new int[] {ai}, "proven same ABI bounds impact");
            ok(stable.unaffectedAt(bi) && stable.unaffectedAt(ci), "no speculative dependent work");
            facts[ai] = M3FileBuildImpact.ABI_CHANGED;
            var changedAbi = M3FileBuildImpact.plan(source, facts, dep, use, true);
            eq(changedAbi.affectedSlots(), sorted(ai, bi, ci), "changed ABI propagates");
            facts[ai] = M3FileBuildImpact.ABI_PROVEN_SAME | M3FileBuildImpact.ABI_CHANGED;
            failure(IllegalArgumentException.class,
                    () -> M3FileBuildImpact.plan(source, facts, dep, use, true), "contradictory ABI facts");
            facts[ai] = 1L << 40;
            failure(IllegalArgumentException.class,
                    () -> M3FileBuildImpact.plan(source, facts, dep, use, true), "unknown ABI mask refused");
            facts[ai] = 0;
            var incomplete = M3FileBuildImpact.plan(source, facts, dep, use, false);
            ok(incomplete.globalInvalidation() && !incomplete.exactGraph(), "missing graph certificate");
            ok(incomplete.affectedSlots().length == source.slots(), "incomplete graph rebuild-all");
            ok((incomplete.reasonAt(di) & M3FileBuildImpact.CONSERVATIVE) != 0,
                    "unrelated node conservatively selected");
            failure(IndexOutOfBoundsException.class,
                    () -> M3FileBuildImpact.plan(source, new long[source.slots()],
                            new int[] {-1}, new int[] {ai}, false),
                    "invalid conservative graph edge must still be refused");

            Files.writeString(root.resolve(pom), "changed effective build input");
            var context = M3FileDeltaLedger.scan(root, source, files);
            var global = M3FileBuildImpact.plan(context, new long[context.slots()], dep, use, true);
            ok(global.globalInvalidation() && global.exactGraph(), "POM edit global invalidation");
            ok(global.affectedSlots().length == context.slots(), "POM edit cannot permit skipped work");
            ok((global.reasonAt(pi) & M3FileBuildImpact.DIRECT) != 0, "POM is a direct edit");

            var clean = M3FileDeltaLedger.scan(root, context, files);
            long[] cleanFacts = new long[clean.slots()];
            cleanFacts[xi] = M3FileBuildImpact.ABI_CHANGED;
            var externalClassAbi = M3FileBuildImpact.plan(clean, cleanFacts, dep, use, true);
            eq(externalClassAbi.affectedSlots(), sorted(xi, ci), "compiler-supplied binary ABI delta");
            cleanFacts[xi] = M3FileBuildImpact.UNKNOWN_SEMANTICS;
            var unknown = M3FileBuildImpact.plan(clean, cleanFacts, dep, use, true);
            eq(unknown.affectedSlots(), sorted(xi, ci), "unknown binary semantic facts propagate");
            Arrays.fill(cleanFacts, 0);
            cleanFacts[bi] = M3FileBuildImpact.CONTEXT_CHANGED;
            var changedContext = M3FileBuildImpact.plan(clean, cleanFacts, dep, use, true);
            eq(changedContext.affectedSlots(), sorted(bi, ci), "scoped compiler context invalidation");
            Arrays.fill(cleanFacts, 0);

            Files.delete(root.resolve(bin));
            var removed = M3FileDeltaLedger.scan(root, clean, files);
            var binaryRemoved = M3FileBuildImpact.plan(removed, cleanFacts, dep, use, true);
            eq(binaryRemoved.affectedSlots(), sorted(xi, ci), "removed .class cannot be reused");
            ok((binaryRemoved.reasonAt(xi) & M3FileBuildImpact.DIRECT) != 0,
                    "binary deletion is direct work");
            var goneAgain = M3FileDeltaLedger.scan(root, removed, files);
            var noRepeatedRemoval = M3FileBuildImpact.plan(goneAgain, cleanFacts, dep, use, true);
            ok(noRepeatedRemoval.affectedSlots().length == 0, "stable tombstone not redispatched");
            Files.write(root.resolve(bin), new byte[] {1, 2});
            var readded = M3FileDeltaLedger.scan(root, goneAgain, files);
            ok(readded.physicalSlot(bin) == xi, "restored .class uses same physical slot");

            var cycle = M3FileBuildImpact.plan(source, new long[source.slots()],
                    new int[] {ai, bi, bi, ci, ci}, new int[] {bi, ai, ci, bi, ci}, true);
            eq(cycle.affectedSlots(), sorted(ai, bi, ci), "cyclic and duplicate edges terminate");
            var emptySnapshot = M3FileDeltaLedger.scan(root, null, List.of());
            var emptyPlan = M3FileBuildImpact.plan(emptySnapshot, new long[0],
                    new int[0], new int[0], true);
            ok(emptyPlan.slots() == 0 && emptyPlan.affectedSlots().length == 0,
                    "zero-node boundary");
            failure(IllegalArgumentException.class,
                    () -> M3FileBuildImpact.plan(same, new long[0], new int[0], new int[0], true),
                    "unaligned source facts");
            failure(IllegalArgumentException.class,
                    () -> M3FileBuildImpact.plan(same, new long[same.slots()],
                            new int[] {ai}, new int[0], true), "unaligned edge lanes");
            failure(IndexOutOfBoundsException.class,
                    () -> M3FileBuildImpact.plan(same, new long[same.slots()],
                            new int[] {-1}, new int[] {0}, true), "bad dependency index");
            failure(IndexOutOfBoundsException.class,
                    () -> M3FileBuildImpact.plan(same, new long[same.slots()],
                            new int[] {0}, new int[] {same.slots()}, true), "bad dependent index");
            failure(IndexOutOfBoundsException.class, () -> stable.reasonAt(-1), "bad plan slot");

            // Independent slower fixed-point implementation; no shared CSR/frontier logic.
            Random random = new Random(20261020L);
            for (int t = 0; t < 1000; t++) {
                var base = (t & 1) == 0 ? source : clean;
                int n = base.slots(), edges = random.nextInt(3 * n + 1);
                int[] from = new int[edges], to = new int[edges];
                for (int i = 0; i < edges; i++) {
                    from[i] = random.nextInt(n);
                    to[i] = random.nextInt(n);
                }
                long[] semantic = new long[n];
                long[] variants = {0, M3FileBuildImpact.ABI_PROVEN_SAME,
                        M3FileBuildImpact.ABI_CHANGED, M3FileBuildImpact.CONTEXT_CHANGED,
                        M3FileBuildImpact.UNKNOWN_SEMANTICS};
                for (int i = 0; i < n; i++) semantic[i] = variants[random.nextInt(variants.length)];
                boolean complete = random.nextBoolean();
                var actual = M3FileBuildImpact.plan(base, semantic, from, to, complete);
                boolean[] expected = slowOracle(base, semantic, from, to, complete);
                for (int i = 0; i < n; i++) {
                    ok(actual.requiresWorkAt(i) == expected[i], "random graph closure " + t + ":" + i);
                }
                int[] aAffected = actual.affectedSlots();
                for (int k = 1; k < aAffected.length; k++) {
                    ok(aAffected[k] > aAffected[k - 1], "stable physical order");
                }
            }
            System.out.println("M3_FILE_IMPACT_PASS checks=" + checks);
        } finally {
            try (var stream = Files.walk(root)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); }
                    catch (IOException error) { throw new java.io.UncheckedIOException(error); }
                });
            }
        }
    }

    private static boolean[] slowOracle(M3FileDeltaLedger.Snapshot snapshot, long[] facts,
                                        int[] dependencies, int[] dependents, boolean complete) {
        int n = snapshot.slots();
        boolean global = !complete;
        boolean[] affected = new boolean[n], spreads = new boolean[n];
        for (int i = 0; i < n; i++) {
            boolean direct = snapshot.changedAt(i) || (facts[i] & (M3FileBuildImpact.ABI_CHANGED
                    | M3FileBuildImpact.CONTEXT_CHANGED | M3FileBuildImpact.UNKNOWN_SEMANTICS)) != 0;
            affected[i] = direct;
            spreads[i] = direct && (facts[i] & M3FileBuildImpact.ABI_PROVEN_SAME) == 0;
            if (snapshot.changedAt(i) && snapshot.kindAt(i) == M3FileDeltaLedger.Kind.BUILD_INPUT) {
                global = true;
            }
        }
        if (global) {
            Arrays.fill(affected, true);
            return affected;
        }
        boolean again;
        do {
            again = false;
            for (int e = 0; e < dependencies.length; e++) {
                if (!spreads[dependencies[e]]) continue;
                int dependent = dependents[e];
                if (!affected[dependent] || !spreads[dependent]) again = true;
                affected[dependent] = true;
                spreads[dependent] = true;
            }
        } while (again);
        return affected;
    }

    private static void ok(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError(description);
    }
    private static void eq(int[] actual, int[] expected, String description) {
        ok(Arrays.equals(actual, expected), description + ": " + Arrays.toString(actual));
    }
    private static int[] sorted(int... input) {
        Arrays.sort(input);
        return input;
    }
    private static void failure(Class<? extends Throwable> expected, Runnable action, String label) {
        checks++;
        try { action.run(); }
        catch (Throwable exception) {
            if (expected.isInstance(exception)) return;
            throw new AssertionError(label, exception);
        }
        throw new AssertionError(label + " was accepted");
    }
}
