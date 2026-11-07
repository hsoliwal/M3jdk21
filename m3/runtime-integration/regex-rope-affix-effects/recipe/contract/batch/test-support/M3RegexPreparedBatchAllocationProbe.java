// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Local qualification: fail every native allocation site and prove image retirement. */
import jdk.internal.mindex.M3RegexDpPlan;
import jdk.internal.mindex.M3RegexDpPlan.BatchBackend;
import jdk.internal.mindex.M3RegexDpPlan.BatchExecution;
import jdk.internal.mindex.M3RegexPreparedBatchTestSupport;

public final class M3RegexPreparedBatchAllocationProbe {
    public static void main(String[] args) {
        if (args.length != 1) throw new AssertionError("allocation library path required");
        M3RegexPreparedBatchTestSupport.load(args[0]);
        int failures = 0;
        for (long allowed = 0; allowed <= 5; allowed++) {
            M3RegexDpPlan plan = M3RegexDpPlan.prepare("a*.");
            M3RegexDpPlan.Effect[] effects = {
                    plan.summarizeUtf16("\ud83d", null), plan.identity(), plan.summarizeUtf16("\ude42", null)};
            try (M3RegexDpPlan.PreparedBatch batch = plan.prepareBatch(effects,
                    new int[] {0, 1, 2}, new int[] {0, 3}, M3RegexDpPlan.BatchLimits.defaults(), null)) {
                M3RegexPreparedBatchTestSupport.failAllocationAfter(allowed);
                try {
                    var result = batch.matches(BatchBackend.PREFER_JNI, null);
                    if (allowed < 5) throw new AssertionError("failed allocation became a truth result");
                    if (!result.matches(0) || result.execution() != BatchExecution.JNI) {
                        throw new AssertionError("successful native execution receipt");
                    }
                } catch (OutOfMemoryError expected) {
                    if (allowed >= 5) throw expected;
                    failures++;
                    if (M3RegexPreparedBatchTestSupport.liveAllocations() != 0) {
                        throw new AssertionError("partial native image leaked at allocation " + allowed);
                    }
                }
                M3RegexPreparedBatchTestSupport.failAllocationAfter(-1);
                if (!batch.matches(BatchBackend.JNI, null).matches(0)) {
                    throw new AssertionError("owner cannot retry after failed preparation");
                }
                if (M3RegexPreparedBatchTestSupport.liveAllocations() != 5) {
                    throw new AssertionError("unexpected retained allocation ownership");
                }
            }
            if (M3RegexPreparedBatchTestSupport.liveAllocations() != 0) {
                throw new AssertionError("closed native image leaked");
            }
        }
        System.out.println("M3_REGEX_PREPARED_ALLOCATION_PASS injectedFailures=" + failures + " liveAllocations=0");
    }
}
