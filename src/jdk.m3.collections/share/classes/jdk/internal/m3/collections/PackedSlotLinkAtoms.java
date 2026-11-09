/*******************************************************************************
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
package jdk.internal.m3.collections;

/**
 * Stateless zero-object mechanics for doubly-linked stable integer slots.
 *
 * <p>Owners retain endpoint fields, free lists, payload lanes, indexes, counters and callback
 * contracts. Endpoint updates are returned as one packed primitive long.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedSlotLinkAtoms.java}
 * blob {@code 3158ab9f9c9621052dabea302515424468494f07} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedSlotLinkAtoms {
    private PackedSlotLinkAtoms() { }

    static long pack(int first, int last) {
        return ((long) first << 32) | (last & 0xffff_ffffL);
    }

    static int first(long endpoints) {
        return (int) (endpoints >> 32);
    }

    static int last(long endpoints) {
        return (int) endpoints;
    }

    static long linkLast(int[] previous, int[] next, int first, int last, int row) {
        previous[row] = last;
        next[row] = -1;
        if (last >= 0) {
            next[last] = row;
        } else {
            first = row;
        }
        return pack(first, row);
    }

    static long linkFirst(int[] previous, int[] next, int first, int last, int row) {
        previous[row] = -1;
        next[row] = first;
        if (first >= 0) {
            previous[first] = row;
        } else {
            last = row;
        }
        return pack(row, last);
    }

    static long unlink(int[] previous, int[] next, int first, int last, int row) {
        int before = previous[row];
        int after = next[row];
        if (before >= 0) {
            next[before] = after;
        } else {
            first = after;
        }
        if (after >= 0) {
            previous[after] = before;
        } else {
            last = before;
        }
        return pack(first, last);
    }

    static long moveLast(int[] previous, int[] next, int first, int last, int row) {
        if (row == last) {
            return pack(first, last);
        }
        long endpoints = unlink(previous, next, first, last, row);
        return linkLast(previous, next, first(endpoints), last(endpoints), row);
    }
}
