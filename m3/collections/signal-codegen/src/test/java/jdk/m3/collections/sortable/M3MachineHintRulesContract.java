// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.collections.sortable;

import java.util.Arrays;
import java.util.Random;

/** Deterministic independent source-fact oracle, including unknown and refusal. */
public final class M3MachineHintRulesContract {
    private static long checks;
    private M3MachineHintRulesContract() { }

    public static void main(String[] args) {
        int[] counts = new int[M3MachineHintRules.FACT_COUNT];
        long allSourceKnown = (1L << 20) - 1L;
        M3MachineHintRules empty = M3MachineHintRules.fromFacts(counts, new long[]{allSourceKnown});
        for (int i = 0; i < M3MachineHintRules.COUNT; i++) {
            verify(empty.state(i) == M3MachineHintRules.State.NOT_OBSERVED, "known zero");
        }
        counts[0] = 1; counts[1] = 2; counts[3] = 1; counts[7] = 1;
        counts[9] = 1; counts[11] = 1; counts[14] = 1; counts[15] = 1;
        int[] before = counts.clone();
        long[] mask = {allSourceKnown};
        M3MachineHintRules all = M3MachineHintRules.fromFacts(counts, mask);
        verify(all.knownMask() == 127L && all.candidateMask() == 127L, "all review signals");
        verify(Arrays.equals(before, counts) && mask[0] == allSourceKnown, "retained arrays");
        long[] snapshot = all.masks(); snapshot[0] = 0L;
        verify(all.knownMask() == 127L, "fresh primitive snapshot");
        verify(!all.promotionAuthority() && !all.sourceMutationAuthority()
                && !all.nativeLoweringAuthority(), "no promotion authority");
        verify("ITERATIVE_INVOCATION".equals(M3MachineHintRules.name(2)), "stable ID");
        long missingCalls = allSourceKnown & ~(1L << 7);
        counts[7] = -44; // ignored unknown, never fabricated zero or false absence.
        M3MachineHintRules partial = M3MachineHintRules.fromFacts(counts, new long[]{missingCalls});
        verify(partial.state(M3MachineHintRules.INTERFACE_INVOCATION)
                == M3MachineHintRules.State.UNKNOWN, "missing call relation");
        verify(partial.state(M3MachineHintRules.ITERATIVE_INVOCATION)
                == M3MachineHintRules.State.UNKNOWN, "missing iteration-call relation");
        verify(partial.state(M3MachineHintRules.FIELD_METHOD_RELATION)
                == M3MachineHintRules.State.CANDIDATE, "known model-only relation");
        refused(IllegalArgumentException.class, () -> M3MachineHintRules.fromFacts(new int[21], new long[]{1}));
        refused(IllegalArgumentException.class, () -> M3MachineHintRules.fromFacts(new int[22], new long[]{1L << 22}));
        refused(IllegalArgumentException.class, () -> M3MachineHintRules.fromFacts(new int[22], new long[2]));
        refused(NullPointerException.class, () -> M3MachineHintRules.fromFacts(null, new long[]{0}));
        refused(NullPointerException.class, () -> M3MachineHintRules.fromFacts(new int[22], null));
        refused(IllegalArgumentException.class, () -> new M3MachineHintRules(1L, 2L));
        refused(IllegalArgumentException.class, () -> new M3MachineHintRules(128L, 0L));
        refused(IllegalArgumentException.class, () -> new M3MachineHintRules(0L, 128L));
        refused(IndexOutOfBoundsException.class, () -> all.state(-1));
        refused(IndexOutOfBoundsException.class, () -> M3MachineHintRules.name(7));
        int[] bad = new int[22]; bad[3] = -1;
        refused(IllegalArgumentException.class, () -> M3MachineHintRules.fromFacts(bad, new long[]{1L << 3}));
        verify(M3MachineHintRules.fromFacts(bad, new long[]{0}).knownMask() == 0L,
                "unknown negative value ignored");
        Random random = new Random(20261102L);
        int[] required = {0, 1, 3, 7, 9, 10, 11, 12, 14, 15};
        for (int trial = 0; trial < 30_000; trial++) {
            int[] fact = new int[22];
            long valid = 0;
            for (int id : required) {
                fact[id] = random.nextInt(3);
                if (random.nextBoolean()) valid |= 1L << id;
            }
            M3MachineHintRules actual = M3MachineHintRules.fromFacts(fact, new long[]{valid});
            long[] expectedRequirements = {
                bits(0,1), bits(3,7), bits(11,7), bits(1,14),
                bits(9,10,12), bits(15), bits(11,9,10,12)
            };
            boolean[] oracle = {
                fact[0]>0 && fact[1]>0,
                fact[3]>0 && fact[7]>0,
                fact[11]>0 && fact[7]>0,
                fact[1]>0 && fact[14]>0,
                fact[9]>0 || fact[10]>0 || fact[12]>0,
                fact[15]>0,
                fact[11]>0 && (fact[9]>0 || fact[10]>0 || fact[12]>0)
            };
            long expectedKnown = 0, expectedCandidate = 0;
            for (int id=0; id<7; id++) {
                if ((valid & expectedRequirements[id]) != expectedRequirements[id]) continue;
                expectedKnown |= 1L << id;
                if (oracle[id]) expectedCandidate |= 1L << id;
            }
            verify(actual.knownMask() == expectedKnown, "known oracle " + trial);
            verify(actual.candidateMask() == expectedCandidate, "candidate oracle " + trial);
            for (int id=0; id<7; id++) {
                M3MachineHintRules.State expected = (expectedKnown & (1L<<id)) == 0
                        ? M3MachineHintRules.State.UNKNOWN
                        : (expectedCandidate & (1L<<id)) == 0
                        ? M3MachineHintRules.State.NOT_OBSERVED
                        : M3MachineHintRules.State.CANDIDATE;
                verify(actual.state(id) == expected, "tri-state oracle " + trial + ":" + id);
            }
            M3MachineHintRules restored = new M3MachineHintRules(actual.knownMask(), actual.candidateMask());
            verify(Arrays.equals(actual.masks(), restored.masks()), "portable two-long image");
        }
        System.out.println("M3_MACHINE_HINTS_PASS checks=" + checks + " category_count=" + M3MachineHintRules.COUNT);
    }

    private static long bits(int... ids) {
        long mask = 0L;
        for (int id : ids) mask |= 1L << id;
        return mask;
    }
    private static void verify(boolean passed, String message) {
        checks++;
        if (!passed) throw new AssertionError(message);
    }
    private static void refused(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable problem) {
            verify(expected.isInstance(problem), "wrong refusal type " + problem);
            return;
        }
        throw new AssertionError("expected refusal: " + expected.getName());
    }
}
