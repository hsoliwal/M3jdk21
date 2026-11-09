// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Arrays;

/** Old m3rankbatch remains readable without the additive unionValues0 export. */
public final class LongUnionLegacyContract {
    private LongUnionLegacyContract() { }

    public static void main(String[] args) {
        if (!LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("legacy JNI stub did not load");
        }
        long[] a = new long[256], b = new long[256];
        Arrays.fill(a, 1);
        Arrays.fill(b, 1);
        if (LongRankBatchNative.unionValues(a, b, false).length != 256) {
            throw new AssertionError("no Java fallback when additive native export is absent");
        }
        boolean refused = false;
        try {
            LongRankBatchNative.unionValues(a, b, false, true);
        } catch (UnsatisfiedLinkError expected) {
            refused = true;
        }
        if (!refused) {
            throw new AssertionError("explicit native-required request hid missing export");
        }
        System.out.println("PASS M3_UNION_LEGACY_ABI_FALLBACK");
    }
}
