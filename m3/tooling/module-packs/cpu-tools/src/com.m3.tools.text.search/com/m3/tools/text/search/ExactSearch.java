// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.search;
import java.util.Arrays;
import java.util.Objects;
/** Exact UTF-16 String.indexOf semantics; overlapping hits and bounded output. */
public final class ExactSearch {
    private ExactSearch() { }
    public static int[] positions(String text, String needle, int maxHits) {
        Objects.requireNonNull(text); Objects.requireNonNull(needle);
        if (maxHits < 0) throw new IllegalArgumentException("negative limit");
        int[] buffer = new int[Math.min(maxHits, 16)];
        int used = 0, next = 0;
        while (used < maxHits && next <= text.length()) {
            int at = text.indexOf(needle, next);
            if (at < 0) break;
            if (used == buffer.length) buffer = Arrays.copyOf(buffer, (int) Math.min(maxHits, 2L * used + 1L));
            buffer[used++] = at;
            if (at == text.length()) break;
            next = at + 1;
        }
        return Arrays.copyOf(buffer, used);
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (!Arrays.equals(positions("aaaa","aa",99),new int[]{0,1,2}))
                throw new AssertionError("overlap");
            if (!Arrays.equals(positions("xy","",99),new int[]{0,1,2}))
                throw new AssertionError("empty needle");
            if (positions("a","a",0).length != 0) throw new AssertionError("bounded");
            System.out.println("M3_JMOD_TEST_PASS text.search");
        } else if (args.length >= 2) {
            System.out.println(Arrays.toString(positions(args[0],args[1],1000)));
        } else { throw new IllegalArgumentException("text and needle required"); }
    }
}
