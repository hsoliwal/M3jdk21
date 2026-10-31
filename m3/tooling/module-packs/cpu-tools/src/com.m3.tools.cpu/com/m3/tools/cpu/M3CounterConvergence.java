// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Primitive grow-only replicated counters and vector-clock comparison.
 * This is a convergent CRDT atom, not Raft, Byzantine consensus, or a transaction manager.
 */
public final class M3CounterConvergence {
    private M3CounterConvergence() { }

    public static long[] merge(long[] first, long[] second) {
        validate(first); validate(second);
        if (first.length != second.length) throw new IllegalArgumentException("replica count");
        long[] result = new long[first.length];
        for (int i = 0; i < first.length; i++) result[i] = Math.max(first[i], second[i]);
        return result;
    }

    public static long[] increment(long[] counters, int replica, long delta) {
        validate(counters);
        if (replica < 0 || replica >= counters.length || delta < 0)
            throw new IllegalArgumentException("replica or negative delta");
        long[] result = counters.clone();
        result[replica] = Math.addExact(result[replica], delta);
        return result;
    }

    public static long value(long[] counters) {
        validate(counters);
        long total = 0;
        for (long count : counters) total = Math.addExact(total, count);
        return total;
    }

    /** True iff first causally precedes second (strict componentwise partial order). */
    public static boolean happensBefore(long[] first, long[] second) {
        validate(first); validate(second);
        if (first.length != second.length) throw new IllegalArgumentException("vector length");
        boolean smaller = false;
        for (int i = 0; i < first.length; i++) {
            if (first[i] > second[i]) return false;
            smaller |= first[i] < second[i];
        }
        return smaller;
    }

    public static boolean concurrent(long[] a, long[] b) {
        validate(a); validate(b);
        if (a.length != b.length) throw new IllegalArgumentException("vector length");
        boolean smaller = false, greater = false;
        for (int i = 0; i < a.length; i++) {
            smaller |= a[i] < b[i]; greater |= a[i] > b[i];
        }
        return smaller && greater;
    }

    private static void validate(long[] values) {
        Objects.requireNonNull(values);
        if (values.length > 1_000_000) throw new IllegalArgumentException("replica budget");
        for (long value : values) if (value < 0) throw new IllegalArgumentException("negative counter");
    }
}
