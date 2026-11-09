/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.range;

import java.util.Arrays;
import java.util.Objects;

/**
 * Checked long difference array: O(1) half-open range additions and O(n) materialization.
 *
 * <p>Updates are strongly atomic with respect to arithmetic overflow.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongDifferenceArray.java}
 * blob {@code b843e8421dfbdf53ed775b1308aeefaeb1436014} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongDifferenceArray {
    private final int length;
    private final long[] delta;

    public LongDifferenceArray(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length");
        }
        this.length = length;
        delta = new long[Math.addExact(length, 1)];
    }

    public int length() {
        return length;
    }

    public void rangeAdd(int fromInclusive, int toExclusive, long value) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, length);
        if (fromInclusive == toExclusive || value == 0L) {
            return;
        }
        long nextFrom = Math.addExact(delta[fromInclusive], value);
        long nextTo = Math.subtractExact(delta[toExclusive], value);
        delta[fromInclusive] = nextFrom;
        delta[toExclusive] = nextTo;
    }

    public void pointAdd(int index, long value) {
        Objects.checkIndex(index, length);
        rangeAdd(index, index + 1, value);
    }

    /** Returns a new exact materialized lane. Overflow aborts without mutating this object. */
    public long[] materialize() {
        long[] result = new long[length];
        long running = 0L;
        for (int i = 0; i < length; i++) {
            running = Math.addExact(running, delta[i]);
            result[i] = running;
        }
        return result;
    }

    /**
     * Adds the materialized delta lane to a new copy of {@code base}.
     * The caller's base array and this difference array remain unchanged on overflow.
     */
    public long[] applyTo(long[] base) {
        Objects.requireNonNull(base, "base");
        if (base.length != length) {
            throw new IllegalArgumentException(
                    "base length " + base.length + " != " + length);
        }
        long[] result = new long[length];
        long running = 0L;
        for (int i = 0; i < length; i++) {
            running = Math.addExact(running, delta[i]);
            result[i] = Math.addExact(base[i], running);
        }
        return result;
    }

    public void clear() {
        Arrays.fill(delta, 0L);
    }

    public long payloadBytes() {
        return 8L * delta.length;
    }
}
