// SPDX-FileCopyrightText: 2026 Synexia contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Fail-closed atomic pair publication, preserving existing sparse geometry. */
final class M3SparseVisibleRangeAtomicityTest {
    @Test void partialOutputIsNotCommittedForEmptyOrMeasuredModel() {
        M3SparseExtents rows=new M3SparseExtents(13);
        int[] out={754};
        assertThrows(ArrayIndexOutOfBoundsException.class,()->rows.visibleRange(0,10,out));
        assertArrayEquals(new int[]{754},out,"empty-range publication must be atomic");
        rows.reset(32);
        rows.setExtent(3,21);
        assertThrows(ArrayIndexOutOfBoundsException.class,()->rows.visibleRange(11,10,out));
        assertArrayEquals(new int[]{754},out,"measured-range publication must be atomic");
    }

    @Test void shortOutputWhenViewportIsZeroMustRemainUnchanged() {
        M3SparseExtents rows=new M3SparseExtents(5);
        rows.reset(19);
        int[] out={415};
        assertThrows(ArrayIndexOutOfBoundsException.class,()->rows.visibleRange(16,0,out));
        assertArrayEquals(new int[]{415},out);
    }

    @Test void zeroLengthArrayAndNullRetainHistoricalExceptionTypes() {
        M3SparseExtents rows=new M3SparseExtents(7);
        rows.reset(14);
        assertEquals("Index 0 out of bounds for length 0",assertThrows(
                ArrayIndexOutOfBoundsException.class,()->rows.visibleRange(0,10,new int[0])).getMessage());
        assertEquals("Index 1 out of bounds for length 1",assertThrows(
                ArrayIndexOutOfBoundsException.class,()->rows.visibleRange(0,10,new int[1])).getMessage());
        assertThrows(NullPointerException.class,()->rows.visibleRange(0,10,null));
        assertEquals("negative viewport",assertThrows(IllegalArgumentException.class,
                ()->rows.visibleRange(0,-1,new int[1])).getMessage());
    }

    @Test void regularIntervalsMatchIndependentBigIntegerEndpointOracle() {
        M3SparseExtents rows=new M3SparseExtents(7);
        rows.reset(100);
        rows.setExtent(3,0); rows.setExtent(20,41); rows.setExtent(54,0); rows.setExtent(99,82);
        Random random=new Random(0x5a28aa37L);
        for(int i=0;i<20000;i++) {
            long offset=switch(i%7) {case 0->Long.MAX_VALUE; case 1->Long.MIN_VALUE;
                case 2->rows.totalExtent()+21;case 3->-500;
                default -> random.nextLong(1000)-175;};
            int viewport=switch(i%5) {case 0->0;case 1->1;case 2->Integer.MAX_VALUE;
                default->random.nextInt(400);};
            int first=rows.rowAt(offset);
            int last=first;
            if(viewport>0) {
                BigInteger endpoint=BigInteger.valueOf(offset).add(BigInteger.valueOf(viewport-1L));
                long capped=endpoint.min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
                last=rows.rowAt(capped)+1;
            }
            int[] out={-9,-9};
            rows.visibleRange(offset,viewport,out);
            assertEquals(first,out[0],"first iteration "+i);
            assertEquals(last,out[1],"after last iteration "+i);
        }
    }

    @Test void giantSparseRowUniverseRequiresOnlyTwoOutputSlots() {
        M3SparseExtents rows=new M3SparseExtents(Integer.MAX_VALUE);
        rows.reset(Integer.MAX_VALUE);
        rows.setExtent(7,0);
        rows.setExtent(Integer.MAX_VALUE-1,Integer.MAX_VALUE);
        assertEquals(2,rows.measuredCount());
        int[] out=new int[2];
        rows.visibleRange(Long.MAX_VALUE, Integer.MAX_VALUE, out);
        assertEquals(Integer.MAX_VALUE-1,out[0]);
        assertEquals(Integer.MAX_VALUE,out[1]);
        assertEquals(2,rows.measuredCount());
    }
}
