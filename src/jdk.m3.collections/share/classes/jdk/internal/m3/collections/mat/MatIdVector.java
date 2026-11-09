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
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable repeated references stored only as int pool IDs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIdVector.java}
 * blob {@code 7e88a19deda19808eb89a68f1f4cd2996797f963} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIdVector<T> {
    private final MatPool<T> pool;
    private final int[] ids;

    public MatIdVector(MatPool<T> pool, int[] ids) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.ids = Objects.requireNonNull(ids, "ids").clone();
        for (int id : this.ids) pool.ref(id);
    }

    public int size() { return ids.length; }
    public MatPool<T> pool() { return pool; }
    public int idAt(int index) { return ids[Objects.checkIndex(index, ids.length)]; }
    public MatRef<T> get(int index) { return pool.ref(idAt(index)); }
    public int[] ids() { return ids.clone(); }
    public long payloadBytes() { return 4L * ids.length; }

    public MatBits equalTo(MatRef<T> value) {
        Objects.requireNonNull(value, "value");
        if (value.pool() != pool) throw new IllegalArgumentException("different pool");
        MatBits.Builder bits = MatBits.builder(ids.length);
        for (int i = 0; i < ids.length; i++) if (ids[i] == value.id()) bits.set(i);
        return bits.build();
    }
}
