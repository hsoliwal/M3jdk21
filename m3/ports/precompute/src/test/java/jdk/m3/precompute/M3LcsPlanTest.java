// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/** Source-owned JUnit acceptance candidate; actual Maven JUnit receipt is pending. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-lcs-bitvector-20261105/src/test/java/com/synexia/m3/proof/Utf16BitVectorLcsOracleTest.java blob 2283262a7711; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1
final class M3LcsPlanTest {
    @Test void representativePlatformContracts() {
        assertEquals(3, M3LcsPlan.bitVectorLength("abcde".toCharArray(), "ace".toCharArray()));
        assertEquals(2, M3LcsPlan.bitVectorLength("HARRY".toCharArray(), "SALLY".toCharArray()));
        assertEquals(4, M3LcsPlan.bitVectorLength("AGGTAB".toCharArray(), "GXTXAYB".toCharArray()));
        assertEquals(0, M3LcsPlan.bitVectorLength("AB".toCharArray(), "CD".toCharArray()));
    }

    @Test void utf16LiteralAndZeroLengthContracts() {
        assertEquals(0, M3LcsPlan.bitVectorLength(new char[0], new char[]{'a'}));
        assertEquals(1, M3LcsPlan.bitVectorLength(new char[]{0,'\ud800'}, new char[]{'\ud800'}));
        assertEquals(0, M3LcsPlan.bitVectorLength(new char[]{'\udc00'}, new char[]{'\ud800'}));
        assertEquals(63, M3LcsPlan.bitVectorLength(new char[63], new char[4096]));
    }

    @Test void refusesNullAndOverlength() {
        assertThrows(NullPointerException.class, () -> M3LcsPlan.bitVectorLength(null,new char[0]));
        assertThrows(NullPointerException.class, () -> M3LcsPlan.bitVectorLength(new char[0],null));
        assertThrows(IllegalArgumentException.class, () -> M3LcsPlan.bitVectorLength(new char[64],new char[64]));
        assertThrows(IllegalArgumentException.class, () -> M3LcsPlan.bitVectorLength(new char[4097],new char[0]));
    }

    @Test void exhaustiveSmallAlphabetAndInputImmutability() {
        char[] alphabet = new char[]{0,'a','\ud800'};
        for (int aLen = 0; aLen <= 3; aLen++) {
            int leftCases = (int)Math.pow(alphabet.length, aLen);
            for (int ai = 0; ai < leftCases; ai++) {
                char[] a = decode(ai,aLen,alphabet);
                for (int bLen = 0; bLen <= 3; bLen++) {
                    int rightCases = (int)Math.pow(alphabet.length, bLen);
                    for (int bi = 0; bi < rightCases; bi++) {
                        char[] b = decode(bi,bLen,alphabet);
                        char[] aFrozen = a.clone();
                        char[] bFrozen = b.clone();
                        assertEquals(M3LcsPlanOracle.referenceLength(a,b),
                                M3LcsPlan.bitVectorLength(a,b));
                        assertArrayEquals(aFrozen,a);
                        assertArrayEquals(bFrozen,b);
                    }
                }
            }
        }
    }

    private static char[] decode(int rank,int length,char[] alphabet) {
        char[] out = new char[length];
        for(int i=0;i<length;i++) { out[i] = alphabet[rank%alphabet.length];rank/=alphabet.length; }
        return out;
    }
}
