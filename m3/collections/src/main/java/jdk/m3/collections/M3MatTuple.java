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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable tuple of IDs from one pool; no object references per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatTuple.java}
 * blob {@code 62d08c9508a1328bf18958643ecbe2c7fdf3d3ce} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatTuple<T> {
    private final M3MatPool<T> pool;
    private final int[] ids;

    public M3MatTuple(M3MatPool<T> pool, int... ids) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.ids = Objects.requireNonNull(ids, "ids").clone();
        for (int id : this.ids) pool.ref(id);
    }

    public M3MatPool<T> pool() { return pool; }
    public int size() { return ids.length; }
    public int idAt(int index) { return ids[Objects.checkIndex(index, ids.length)]; }
    public M3MatRef<T> get(int index) { return pool.ref(idAt(index)); }
    public int[] ids() { return ids.clone(); }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof M3MatTuple<?> tuple && pool == tuple.pool && Arrays.equals(ids, tuple.ids);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(pool) + Arrays.hashCode(ids);
    }
}
