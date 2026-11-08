// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/** Source-JUnit product entry; native loading is separately tested by exact-head CI. */
final class LongRankTripletSumTest {
    @Test void immutableSnapshotAndUniqueTripletValues() {
        long[] original = {-1L, 0L, 1L, 2L, -1L, -4L};
        LongRankIndex owner = LongRankIndex.copyOf(original);
        assertEquals(2L, owner.distinctTripletSumCount(0L));
        assertEquals(0L, owner.distinctTripletSumCount(Long.MIN_VALUE));
        assertArrayEquals(new long[] {-4L, -1L, -1L, 0L, 1L, 2L}, owner.toArray());
        assertArrayEquals(new long[] {-1L, 0L, 1L, 2L, -1L, -4L}, original);
        LongRankIndex extremes = LongRankIndex.copyOf(
                new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L});
        assertEquals(LongRankBatchNative.distinctTripletSumCountJava(extremes.toArray(), 0L),
                extremes.distinctTripletSumCount(0L));
    }

    @Test void independentBigIntegerOracleAndOptionalJNI() {
        LongRankTripletSumContract.main(new String[0]);
    }
}
