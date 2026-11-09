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
package jdk.internal.m3.collections.mat;

import java.util.Objects;

/**
 * Bitmap set over one immutable value pool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIdSet.java}
 * blob {@code 7c20f01fcb64ab6d14609f1d3e99b962298ae897} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIdSet<T> {
    private final MatPool<T> pool;
    private final MatBits bits;

    private MatIdSet(MatPool<T> pool, MatBits bits) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.bits = Objects.requireNonNull(bits, "bits");
        if (bits.size() != pool.size()) throw new IllegalArgumentException("set universe != pool");
    }

    public static <T> Builder<T> builder(MatPool<T> pool) { return new Builder<>(pool); }
    public MatPool<T> pool() { return pool; }
    public int size() { return bits.cardinality(); }
    public boolean contains(MatRef<T> ref) {
        Objects.requireNonNull(ref, "ref");
        return ref.pool() == pool && bits.get(ref.id());
    }
    public MatBits bits() { return MatBits.ofWords(bits.size(), bits.wordsCopy()); }
    public MatIdSet<T> union(MatIdSet<T> other) { require(other); return new MatIdSet<>(pool, bits.or(other.bits)); }
    public MatIdSet<T> intersect(MatIdSet<T> other) { require(other); return new MatIdSet<>(pool, bits.and(other.bits)); }
    public MatIdSet<T> xor(MatIdSet<T> other) { require(other); return new MatIdSet<>(pool, bits.xor(other.bits)); }
    public MatIdSet<T> minus(MatIdSet<T> other) { require(other); return new MatIdSet<>(pool, bits.andNot(other.bits)); }

    private void require(MatIdSet<T> other) {
        Objects.requireNonNull(other, "other");
        if (pool != other.pool) throw new IllegalArgumentException("different pools");
    }

    public static final class Builder<T> {
        private final MatPool<T> pool;
        private final MatBits.Builder bits;
        private Builder(MatPool<T> pool) {
            this.pool = Objects.requireNonNull(pool, "pool");
            this.bits = MatBits.builder(pool.size());
        }
        public Builder<T> add(MatRef<T> ref) {
            Objects.requireNonNull(ref, "ref");
            if (ref.pool() != pool) throw new IllegalArgumentException("different pool");
            bits.set(ref.id());
            return this;
        }
        public MatIdSet<T> build() { return new MatIdSet<>(pool, bits.build()); }
    }
}
