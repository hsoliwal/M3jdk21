// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;

/** Old m3rankbatch remains readable without the additive unionValues0 export. */
public final class M3LongUnionLegacyContract {
    private M3LongUnionLegacyContract() { }

    public static void main(String[] args) {
        if (!M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("legacy JNI stub did not load");
        }
        long[] a = new long[256], b = new long[256];
        Arrays.fill(a, 1);
        Arrays.fill(b, 1);
        if (M3LongRankBatchNative.unionValues(a, b, false).length != 256) {
            throw new AssertionError("no Java fallback when additive native export is absent");
        }
        boolean refused = false;
        try {
            M3LongRankBatchNative.unionValues(a, b, false, true);
        } catch (UnsatisfiedLinkError expected) {
            refused = true;
        }
        if (!refused) {
            throw new AssertionError("explicit native-required request hid missing export");
        }
        System.out.println("PASS M3_UNION_LEGACY_ABI_FALLBACK");
    }
}
