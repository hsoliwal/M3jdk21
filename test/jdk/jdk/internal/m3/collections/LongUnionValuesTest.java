// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** Admission of original M3JNI sorted-union values through the existing Java owner. */
final class LongUnionValuesTest {
    @Test
    void comparePureJavaAndAvailableNativeAgainstIndependentFrequencyOracle() {
        LongUnionValuesContract.contracts(
                LongRankBatchNative.nativeAvailable() ? "native" : "java");
    }

    @Test
    void exposesOnlyDetachedValuesFromExistingImmutableRankOwners() {
        LongRankIndex left = LongRankIndex.copyOf(new long[] {
                Long.MAX_VALUE, 2, 2, 1, Long.MIN_VALUE});
        LongRankIndex right = LongRankIndex.copyOf(new long[] {
                Long.MIN_VALUE, 1, 1, 1, 3, Long.MAX_VALUE});
        assertArrayEquals(new long[] {
                Long.MIN_VALUE, 1, 1, 1, 2, 2, 3, Long.MAX_VALUE},
                left.unionValues(right, false));
        assertArrayEquals(new long[] {
                Long.MIN_VALUE, 1, 2, 3, Long.MAX_VALUE},
                left.unionValues(right, true));
        assertEquals(left.unionCount(right), left.unionValues(right, false).length);
        long[] detached = left.unionValues(right, false);
        detached[0] = 99;
        assertEquals(Long.MIN_VALUE, left.select(0));
        assertEquals(Long.MIN_VALUE, right.select(0));
        assertThrows(NullPointerException.class, () -> left.unionValues(null, false));
    }
}
