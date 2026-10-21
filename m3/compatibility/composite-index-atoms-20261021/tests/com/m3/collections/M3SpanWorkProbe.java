/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
/** Scratch source instrumentation; this is not a timing benchmark. */
public final class M3SpanWorkProbe {
    static long comparisons;
    private M3SpanWorkProbe() { }
    public static void main(String[] args) {
        M3SpanIndex spans = new M3SpanIndex();
        for (int i = 0; i < 2048; i++) spans.add(i * 2, i * 2L + 1, 0);
        int result = spans.snapshot(0, 0).earliestCovering(2049);
        if (result != 2048) throw new AssertionError("wrong result");
        System.out.println("span search comparisons=" + comparisons);
        if (comparisons > 12) throw new AssertionError("one prefix lower_bound must be sufficient");
    }
}
