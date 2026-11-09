// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
/** A sparse 8-million-row Boolean run should not require proportional temporary int[] lanes. */
public final class M3RunsMemoryProbe {
    public static void main(String[] args) {
        int n = 8_000_000;
        boolean[] kept = new boolean[n];
        java.util.Arrays.fill(kept, true);
        kept[n/2] = false;
        int[][] runs = M3ListDiff.runs(kept, true);
        if (runs[0].length != 1 || runs[0][0] != n/2 || runs[1][0] != 1)
            throw new AssertionError("run mismatch");
        System.out.println("PASS sparse runs="+runs[0].length+" n="+n);
    }
}
