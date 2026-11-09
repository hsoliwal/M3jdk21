// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BitapChunkedRegionProof.java (blob 891e5a7b456c) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/** Exact-source Java21/JNI probe for the bounded haystack-region candidate. */
public final class BitapChunkedRegionProof {
    private BitapChunkedRegionProof() {}
    private static long checks;

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("NATIVE_LIBRARY");
        System.load(args[0]);
        Random random = new Random(0xB17A2026L);
        int[] sizes = {0, 1, 63, 8191, 8192, 8193, 65535, 65536, 65537, 65538,
                73727, 73728, 73729, 131071, 131072, 131073, 262145};
        for (int caseNo = 0; caseNo < sizes.length * 5; caseNo++) {
            int n = sizes[caseNo % sizes.length];
            int m = 1 + (caseNo % 4 == 0 ? 62 : caseNo % 4 == 1 ? 1 : caseNo % 4 == 2 ? 2 : 3);
            byte[] h = new byte[n];
            byte[] needle = new byte[m];
            random.nextBytes(h);
            random.nextBytes(needle);
            if (caseNo % 9 == 0) { Arrays.fill(h, (byte)-1); Arrays.fill(needle, (byte)-1); }
            if (caseNo % 5 == 0 && n >= m) {
                int at = Math.min(n - m, n > 65536 ? 65536 - Math.min(m - 1, 40) : Math.max(0, n - m));
                System.arraycopy(needle, 0, h, at, m);
            }
            if (caseNo % 11 == 0 && n >= m) {
                System.arraycopy(needle, 0, h, n - m, m);
            }
            byte[] unmodified = h.clone();
            int[] offsets = {Integer.MIN_VALUE, -1, 0, 1, 8190, 8191, 8192,
                    65535, 65536, 65537, n / 2, n - m, Integer.MAX_VALUE};
            BitapExactSearch compiled = new BitapExactSearch(needle);
            for (int from : offsets) {
                int[] oracle = oracle(h, needle, from);
                eq(oracle[0], compiled.indexOf(h, from), caseNo, "java-first");
                eq(oracle[0], BitapExactSearch.indexOfNative(needle, h, from), caseNo, "jni-first");
                eq(oracle[1], compiled.lastIndexOf(h, from), caseNo, "java-last");
                eq(oracle[1], BitapExactSearch.lastIndexOfNative(needle, h, from), caseNo, "jni-last");
                eq(oracle[2], compiled.countOverlapping(h, from), caseNo, "java-overlap");
                eq(oracle[2], BitapExactSearch.countOverlappingNative(needle, h, from),
                        caseNo, "jni-overlap");
                if (from == 0 || from == 8191 || from == 65536) {
                    ByteBuffer view = ByteBuffer.allocateDirect(n + 8);
                    view.position(4);
                    view.put(h);
                    view.position(4);
                    view.limit(n + 4);
                    eq(oracle[0], compiled.indexOf(view.asReadOnlyBuffer(), from), caseNo, "direct-ro-first");
                    eq(oracle[1], compiled.lastIndexOf(view, from), caseNo, "direct-last");
                    eq(oracle[2], compiled.countOverlapping(view, from), caseNo, "direct-count");
                    eq(4, view.position(), caseNo, "direct-position");
                    eq(n + 4, view.limit(), caseNo, "direct-limit");
                }
            }
            if (!Arrays.equals(h, unmodified)) throw new AssertionError("JNI mutated source array");
            checks++;
        }
        byte[] same = new byte[63];
        Arrays.fill(same, (byte)0xff);
        eq(0, BitapExactSearch.indexOfNative(same, same, 0), -1, "alias-first");
        eq(0, BitapExactSearch.lastIndexOfNative(same, same, Integer.MAX_VALUE), -1, "alias-last");
        eq(1, BitapExactSearch.countOverlappingNative(same, same, 0), -1, "alias-count");
        System.out.println("M3_BITAP_CHUNK_REGION_PASS checks=" + checks + " seed=B17A2026 cases=" + (sizes.length * 5));
    }

    private static void eq(int expected, int actual, int trial, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + " trial=" + trial + " expected=" + expected + " actual=" + actual);
    }

    private static int[] oracle(byte[] h, byte[] p, int from) {
        int first = -1, last = -1, count = 0;
        int maximum = h.length - p.length;
        for (int i = 0; i <= maximum; i++) {
            boolean same = true;
            for (int j = 0; j < p.length; j++) if (h[i+j] != p[j]) {same = false;break;}
            if (!same) continue;
            if (i >= Math.max(0, from)) { if (first < 0) first = i; count++; }
            if (from >= 0 && i <= from) last = i;
        }
        return new int[]{first,last,count};
    }
}
