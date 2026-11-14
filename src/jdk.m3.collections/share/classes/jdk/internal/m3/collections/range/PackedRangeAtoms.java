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
package jdk.internal.m3.collections.range;

/**
 * Shared overflow-safe mechanics for sorted inclusive long ranges.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedRangeAtoms.java}
 * blob {@code 2cf2f8497a730864bc6fee2bd6f38afd4df320b8} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedRangeAtoms {
    private PackedRangeAtoms() { }

    static void checkRange(long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("reversed inclusive range");
        }
    }

    static int containing(long[] starts, long[] ends, int size, long point) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (starts[mid] <= point) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        int row = low - 1;
        return row >= 0 && point <= ends[row] ? row : -1;
    }

    static int firstIntersection(long[] ends, int size, long from) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (ends[mid] < from) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    static boolean adjacent(long leftEnd, long rightStart) {
        return leftEnd != Long.MAX_VALUE && leftEnd + 1 == rightStart;
    }

    static long leftRemainderEnd(long from) {
        if (from == Long.MIN_VALUE) {
            throw new ArithmeticException("no predecessor for Long.MIN_VALUE");
        }
        return from - 1;
    }

    static long rightRemainderStart(long to) {
        if (to == Long.MAX_VALUE) {
            throw new ArithmeticException("no successor for Long.MAX_VALUE");
        }
        return to + 1;
    }

    static long inclusiveSpanExact(long from, long to) {
        checkRange(from, to);
        return Math.addExact(Math.subtractExact(to, from), 1L);
    }
}
