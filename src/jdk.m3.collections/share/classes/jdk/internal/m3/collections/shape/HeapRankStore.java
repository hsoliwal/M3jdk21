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
package jdk.internal.m3.collections.shape;

/**
 * Synexia packed-heap mechanics adapted to parallel reference and primitive rank lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/HeapRankStore.java}
 * blob {@code a6a4af0f16b80f902ad137b04c0fb69b41016b94} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class HeapRankStore<E> extends RankStore<E> {
    HeapRankStore(int maximum, boolean minimum) { super(maximum, minimum); }
    @Override int boundaryIndex() { return 0; }
    @Override void add(E value, long rank) {
        room(); int index = size++;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!before(rank, ranks[parent])) { break; }
            move(index, parent); index = parent;
        }
        write(index, value, rank);
    }
    private void sift(E value, long rank) {
        int index = 0;
        while (index < (size >>> 1)) {
            int child = index * 2 + 1;
            if (child + 1 < size && before(ranks[child + 1], ranks[child])) { child++; }
            if (!before(ranks[child], rank)) { break; }
            move(index, child); index = child;
        }
        write(index, value, rank);
    }
    @Override E removeBoundary() {
        requireValue(); E old = value(0); int last = --size;
        if (last != 0) { sift(value(last), ranks[last]); }
        erase(last); return old;
    }
    @Override void replaceBoundary(E value, long rank) { requireValue(); sift(value, rank); }
}
