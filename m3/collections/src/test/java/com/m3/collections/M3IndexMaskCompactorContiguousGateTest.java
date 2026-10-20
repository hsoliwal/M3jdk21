// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Direct JVM/JNI qualification against an independent scalar oracle.
package com.m3.collections;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class M3IndexMaskCompactorContiguousGateTest {
    @Test void contiguousAndSingleHoleRunsMatchIndependentScalarOracle() {
        assertTrue(M3IndexMaskCompactor.nativeAvailable(), "real JNI is required");
        int[] starts = {0,1,7,127,128,254,255,256,257,510,511,512,598,599};
        int[] lengths = {1,2,3,7,63,64,65,127,128,129,255,256,257,599,600};
        int[] rows = new int[628];
        long[] longs = new long[628];
        byte[] states = new byte[636];
        for (int i=0;i<rows.length;i++) {
            rows[i] = i%5==0 ? Integer.MIN_VALUE : i%7==0 ? Integer.MAX_VALUE : i%13-6;
            longs[i] = i%5==0 ? Long.MIN_VALUE : i%7==0 ? Long.MAX_VALUE : (long)i*0x100000001L-1000000000000L;
        }
        int runs=0, holes=0;
        for (int start : starts) {
            for (int length : lengths) {
                if (length > 600-start) continue;
                Arrays.fill(states,(byte)0);
                Arrays.fill(states,23+start,23+start+length,(byte)0x80);
                check(rows,longs,states); runs+=4;
                if(length>2) {
                    states[23+start+length/2]=0;
                    check(rows,longs,states); holes+=4;
                }
            }
        }
        assertEquals(576,runs);
        assertEquals(468,holes);
        System.out.println("REAL_JNI_CONTIGUOUS_PASS run_route_cases="+runs+" hole_route_cases="+holes);
    }
    private static void check(int[] rows,long[] longs,byte[] states) {
        int[] originalRows=rows.clone();
        long[] originalLongs=longs.clone();
        byte[] originalStates=states.clone();
        int[] expected=new int[600];
        long[] expectedLongs=new long[600];
        int n=0;
        for(int i=0;i<600;i++) if((states[23+i]&0x80)==0x80) {
            expected[n]=rows[17+i]; expectedLongs[n++]=longs[17+i];
        }
        expected=Arrays.copyOf(expected,n);
        expectedLongs=Arrays.copyOf(expectedLongs,n);
        int[] window=M3IndexMaskCompactor.selectWindow(rows,17,states,23,600,0x80,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        long[] longWindow=M3IndexMaskCompactor.selectLongWindow(longs,17,states,23,600,0x80,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        byte[] aligned=Arrays.copyOfRange(states,23,623);
        int[] whole=M3IndexMaskCompactor.select(Arrays.copyOfRange(rows,17,617),aligned,0x80,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        int[] range=M3IndexMaskCompactor.selectNativeRange(rows,17,600,aligned,0x80);
        assertArrayEquals(expected,window);
        assertArrayEquals(expectedLongs,longWindow);
        assertArrayEquals(expected,whole);
        assertArrayEquals(expected,range);
        assertNotSame(rows,window);
        assertNotSame(longs,longWindow);
        assertNotSame(rows,range);
        if(n>0) { window[0]^=1; longWindow[0]^=1L; whole[0]^=1; range[0]^=1; }
        assertArrayEquals(originalRows,rows);
        assertArrayEquals(originalLongs,longs);
        assertArrayEquals(originalStates,states);
    }
}
