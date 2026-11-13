// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** Sound bounded binary-CSP arc propagation using detached primitive domain masks. */
public final class M3ArcConsistency {
    private static final int MAX_VARIABLES = 64;
    private static final int MAX_VALUES = 16;
    private static final int MAX_CONSTRAINTS = 4096;
    private static final long MAX_CHECKS = 50_000_000L;

    private M3ArcConsistency() { }

    /**
     * Apply binary arc consistency to closure. Values are 0..valueCount-1;
     * domainMasks[var] has one bit per value. The directed relation of edge e
     * is relationRows[e * valueCount + leftValue], a mask of supported right values.
     * A self constraint is restricted to (value,value) on the diagonal.
     * Returns a fresh pruned domain or null on proven empty-domain inconsistency.
     * Nonempty arc-consistent domains do not prove the existence of a global solution.
     */
    public static int[] filter(int variables, int valueCount, int[] domainMasks,
            int[] leftVar, int[] rightVar, int[] relationRows, long maxChecks) {
        validate(variables, valueCount, domainMasks, leftVar, rightVar, relationRows, maxChecks);
        int[] domain = Arrays.copyOf(domainMasks, variables);
        for (int mask : domain) { if (mask == 0) { return null; } }
        int[] values = new int[valueCount];
        for (int v = 0; v < valueCount; v++) { values[v] = 1 << v; }
        long[] remaining = {maxChecks};
        boolean changed;
        do {
            changed = false;
            for (int edge = 0; edge < leftVar.length; edge++) {
                int left = leftVar[edge];
                int right = rightVar[edge];
                int start = edge * valueCount;
                int oldLeft = domain[left];
                int oldRight = domain[right];
                if (left == right) {
                    int diagonal = 0;
                    for (int v = 0; v < valueCount; v++) {
                        if ((oldLeft & values[v]) == 0) { continue; }
                        charge(remaining);
                        if ((relationRows[start + v] & values[v]) != 0) { diagonal |= values[v]; }
                    }
                    if (diagonal == 0) { return null; }
                    if (diagonal != oldLeft) { domain[left] = diagonal; changed = true; }
                    continue;
                }
                int newLeft = 0;
                for (int v = 0; v < valueCount; v++) {
                    if ((oldLeft & values[v]) == 0) { continue; }
                    charge(remaining);
                    if ((relationRows[start + v] & oldRight) != 0) { newLeft |= values[v]; }
                }
                if (newLeft == 0) { return null; }
                int newRight = 0;
                for (int w = 0; w < valueCount; w++) {
                    if ((oldRight & values[w]) == 0) { continue; }
                    for (int v = 0; v < valueCount; v++) {
                        if ((newLeft & values[v]) == 0) { continue; }
                        charge(remaining);
                        if ((relationRows[start + v] & values[w]) != 0) {
                            newRight |= values[w];
                            break;
                        }
                    }
                }
                if (newRight == 0) { return null; }
                if (newLeft != oldLeft) { domain[left] = newLeft; changed = true; }
                if (newRight != oldRight) { domain[right] = newRight; changed = true; }
            }
        } while (changed);
        return domain;
    }

    private static void charge(long[] remaining) {
        if (remaining[0] == 0L) {
            throw new IllegalStateException("arc-consistency work budget exhausted");
        }
        remaining[0]--;
    }

    private static void validate(int variables, int valueCount, int[] domainMasks,
            int[] leftVar, int[] rightVar, int[] relationRows, long maxChecks) {
        if (variables < 0 || variables > MAX_VARIABLES || valueCount < 1
                || valueCount > MAX_VALUES || domainMasks == null || leftVar == null
                || rightVar == null || relationRows == null || domainMasks.length != variables
                || leftVar.length > MAX_CONSTRAINTS || leftVar.length != rightVar.length
                || relationRows.length != leftVar.length * valueCount
                || maxChecks < 0L || maxChecks > MAX_CHECKS) {
            throw new IllegalArgumentException("invalid binary CSP or budget");
        }
        int allowed = (1 << valueCount) - 1;
        for (int domain : domainMasks) {
            if ((domain & ~allowed) != 0) {
                throw new IllegalArgumentException("domain has out-of-range values");
            }
        }
        for (int edge = 0; edge < leftVar.length; edge++) {
            if (leftVar[edge] < 0 || leftVar[edge] >= variables
                    || rightVar[edge] < 0 || rightVar[edge] >= variables) {
                throw new IllegalArgumentException("constraint vertex outside domain");
            }
        }
        for (int row : relationRows) {
            if ((row & ~allowed) != 0) {
                throw new IllegalArgumentException("binary relation has out-of-range values");
            }
        }
    }
}
