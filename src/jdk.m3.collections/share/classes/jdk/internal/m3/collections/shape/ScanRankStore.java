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
package jdk.internal.m3.collections.shape;

/**
 * Dense append storage with a scan to find the rank boundary.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ScanRankStore.java}
 * blob {@code ef094b77c767eb91058bd60723c7cdd1707cf2ba} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class ScanRankStore<E> extends RankStore<E> {
    ScanRankStore(int maximum, boolean minimum) { super(maximum, minimum); }
    @Override int boundaryIndex() {
        int best = 0;
        for (int i = 1; i < size; i++) { if (before(ranks[i], ranks[best])) { best = i; } }
        return best;
    }
    @Override void add(E value, long rank) { room(); write(size++, value, rank); }
    @Override E removeBoundary() {
        requireValue(); int index = boundaryIndex(); E old = value(index);
        move(index, --size); erase(size); return old;
    }
    @Override void replaceBoundary(E value, long rank) { requireValue(); write(boundaryIndex(), value, rank); }
}
