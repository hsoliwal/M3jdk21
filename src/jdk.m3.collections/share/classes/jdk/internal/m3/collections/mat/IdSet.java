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

import jdk.m3.arrays.SelectionBits;

import java.util.Objects;

/**
 * Bitmap set over one immutable value pool.
 *
 * <p>Lineage: absorbed from Synexia {@code
 * m3-mat-value/src/main/java/jdk/internal/m3/matvalue/IdSet.java}
 * blob {@code 7c20f01fcb64ab6d14609f1d3e99b962298ae897} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source
 * modulo names).
 */
public final class IdSet<T> {
    private final Pool<T> pool;
    private final SelectionBits bits;

    private IdSet(Pool<T> pool, SelectionBits bits) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.bits = Objects.requireNonNull(bits, "bits");
        if (bits.size() != pool.size()) throw new IllegalArgumentException("set universe != pool");
    }

    public static <T> Builder<T> builder(Pool<T> pool) { return new Builder<>(pool); }
    public Pool<T> pool() { return pool; }
    public int size() { return bits.cardinality(); }
    public boolean contains(Ref<T> ref) {
        Objects.requireNonNull(ref, "ref");
        return ref.pool() == pool && bits.get(ref.id());
    }
    public SelectionBits bits() { return SelectionBits.ofWords(bits.size(), bits.wordsCopy()); }
    public IdSet<T> union(IdSet<T> other) { require(other); return new IdSet<>(pool, bits.or(
            other.bits)); }
    public IdSet<T> intersect(IdSet<T> other) { require(other); return new IdSet<>(pool, bits.and(
            other.bits)); }
    public IdSet<T> xor(IdSet<T> other) { require(other); return new IdSet<>(pool, bits.xor(
            other.bits)); }
    public IdSet<T> minus(IdSet<T> other) { require(other); return new IdSet<>(pool, bits.andNot(
            other.bits)); }

    private void require(IdSet<T> other) {
        Objects.requireNonNull(other, "other");
        if (pool != other.pool) throw new IllegalArgumentException("different pools");
    }

    public static final class Builder<T> {
        private final Pool<T> pool;
        private final SelectionBits.Builder bits;
        private Builder(Pool<T> pool) {
            this.pool = Objects.requireNonNull(pool, "pool");
            this.bits = SelectionBits.builder(pool.size());
        }
        public Builder<T> add(Ref<T> ref) {
            Objects.requireNonNull(ref, "ref");
            if (ref.pool() != pool) throw new IllegalArgumentException("different pool");
            bits.set(ref.id());
            return this;
        }
        public IdSet<T> build() { return new IdSet<>(pool, bits.build()); }
    }
}
