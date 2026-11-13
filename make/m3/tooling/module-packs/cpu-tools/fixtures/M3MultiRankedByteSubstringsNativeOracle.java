// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3MultiRankedByteSubstrings;
import jdk.m3.tools.cpu.M3MultiRankedByteSubstringsNative;
import java.util.Arrays;
import java.util.Random;

/** Native-vs-Java exact batch rank triplets, detached outputs, and disable refusal. */
public final class M3MultiRankedByteSubstringsNativeOracle {
    private static long checks;
    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label + " @" + checks);
    }
    private static void compare(byte[][] documents, long[] queries) {
        M3MultiRankedByteSubstrings owner = M3MultiRankedByteSubstrings.copyOf(documents);
        int[] reference = owner.selectSpans(queries);
        int[] actual = M3MultiRankedByteSubstringsNative.selectSpans(owner, queries);
        check(Arrays.equals(reference, actual), "native parity");
        check(reference != actual, "fresh output");
        if (actual.length != 0) actual[0] ^= 42;
        check(Arrays.equals(reference, M3MultiRankedByteSubstringsNative.selectSpans(owner, queries)),
                "output mutation isolated");
        check(Arrays.equals(reference, owner.selectSpans(queries)), "java owner unchanged");
    }
    public static void main(String[] args) {
        if (!M3MultiRankedByteSubstringsNative.isAvailable()) {
            M3MultiRankedByteSubstrings empty = M3MultiRankedByteSubstrings.copyOf(new byte[0][]);
            try {
                M3MultiRankedByteSubstringsNative.selectSpans(empty, new long[0]);
                throw new AssertionError("unconfigured native backend accepted");
            } catch (IllegalStateException expected) { checks++; }
            System.out.println("M3_MULTI_RANKED_NATIVE_DISABLED_PASS checks=" + checks);
            return;
        }
        compare(new byte[0][], new long[0]);
        compare(new byte[][] {new byte[]{1,1,2},new byte[]{1,2,3}},new long[]{1,2,3,4,5});
        Random rng = new Random(0x4d3333314e415449L);
        for (int round = 0; round < 5500; round++) {
            byte[][] documents = new byte[rng.nextInt(1,5)][];
            for (int d = 0; d < documents.length; d++) {
                documents[d] = new byte[rng.nextInt(10)];
                rng.nextBytes(documents[d]);
            }
            M3MultiRankedByteSubstrings owner = M3MultiRankedByteSubstrings.copyOf(documents);
            long[] ranks = new long[rng.nextInt(18)];
            if (owner.distinctCount() == 0) ranks = new long[0];
            for (int i = 0; i < ranks.length; i++) ranks[i] = 1L + rng.nextInt((int) owner.distinctCount());
            compare(documents, ranks);
        }
        byte[][] large = {new byte[8192],new byte[8192],new byte[0]};
        rng.nextBytes(large[0]);rng.nextBytes(large[1]);
        M3MultiRankedByteSubstrings owner = M3MultiRankedByteSubstrings.copyOf(large);
        long[] ranks = new long[8192];
        for (int i = 0; i < ranks.length; i++) ranks[i] = 1L + rng.nextLong(owner.distinctCount());
        compare(large,ranks);
        // Java validates malformed input before invoking JNI, whether enabled or not.
        try {
            M3MultiRankedByteSubstringsNative.selectSpans(owner, new long[]{0});
            throw new AssertionError("rank zero admitted");
        } catch (IllegalArgumentException expected) { checks++; }
        System.out.println("M3_MULTI_RANKED_NATIVE_PASS checks=" + checks);
    }
}
