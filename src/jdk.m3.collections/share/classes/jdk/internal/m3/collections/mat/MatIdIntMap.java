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

import jdk.m3.arrays.SelectionBits;

import java.util.Objects;

/**
 * Direct-addressed int map over pool IDs plus a presence bitmap.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIdIntMap.java}
 * blob {@code 1be80983f33c1ecb4c8dac1ae641459180610c16} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIdIntMap<T> {
    private final MatPool<T> pool;
    private final int[] values;
    private final SelectionBits present;

    private MatIdIntMap(MatPool<T> pool, int[] values, SelectionBits present) {
        this.pool = pool;
        this.values = values.clone();
        this.present = present;
    }

    public static <T> Builder<T> builder(MatPool<T> pool) { return new Builder<>(pool); }
    public boolean containsKey(MatRef<T> ref) {
        return compatible(ref) && present.get(ref.id());
    }
    public int get(MatRef<T> ref) {
        if (!containsKey(ref)) throw new IllegalArgumentException("missing key");
        return values[ref.id()];
    }
    public int getOrDefault(MatRef<T> ref, int fallback) {
        return containsKey(ref) ? values[ref.id()] : fallback;
    }
    public long payloadBytes() { return 4L * values.length + present.payloadBytes(); }

    private boolean compatible(MatRef<T> ref) {
        return ref != null && ref.pool() == pool;
    }

    public static final class Builder<T> {
        private final MatPool<T> pool;
        private final int[] values;
        private final SelectionBits.Builder present;
        private Builder(MatPool<T> pool) {
            this.pool = Objects.requireNonNull(pool, "pool");
            this.values = new int[pool.size()];
            this.present = SelectionBits.builder(pool.size());
        }
        public Builder<T> put(MatRef<T> ref, int value) {
            Objects.requireNonNull(ref, "ref");
            if (ref.pool() != pool) throw new IllegalArgumentException("different pool");
            values[ref.id()] = value;
            present.set(ref.id());
            return this;
        }
        public MatIdIntMap<T> build() { return new MatIdIntMap<>(pool, values, present.build()); }
    }
}
