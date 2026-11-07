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

import java.util.Objects;

/**
 * Bitmap set over one immutable value pool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIdSet.java}
 * blob {@code 7c20f01fcb64ab6d14609f1d3e99b962298ae897} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIdSet<T> {
    private final M3MatPool<T> pool;
    private final M3MatBits bits;

    private M3MatIdSet(M3MatPool<T> pool, M3MatBits bits) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.bits = Objects.requireNonNull(bits, "bits");
        if (bits.size() != pool.size()) throw new IllegalArgumentException("set universe != pool");
    }

    public static <T> Builder<T> builder(M3MatPool<T> pool) { return new Builder<>(pool); }
    public M3MatPool<T> pool() { return pool; }
    public int size() { return bits.cardinality(); }
    public boolean contains(M3MatRef<T> ref) {
        Objects.requireNonNull(ref, "ref");
        return ref.pool() == pool && bits.get(ref.id());
    }
    public M3MatBits bits() { return M3MatBits.ofWords(bits.size(), bits.wordsCopy()); }
    public M3MatIdSet<T> union(M3MatIdSet<T> other) { require(other); return new M3MatIdSet<>(pool, bits.or(other.bits)); }
    public M3MatIdSet<T> intersect(M3MatIdSet<T> other) { require(other); return new M3MatIdSet<>(pool, bits.and(other.bits)); }
    public M3MatIdSet<T> xor(M3MatIdSet<T> other) { require(other); return new M3MatIdSet<>(pool, bits.xor(other.bits)); }
    public M3MatIdSet<T> minus(M3MatIdSet<T> other) { require(other); return new M3MatIdSet<>(pool, bits.andNot(other.bits)); }

    private void require(M3MatIdSet<T> other) {
        Objects.requireNonNull(other, "other");
        if (pool != other.pool) throw new IllegalArgumentException("different pools");
    }

    public static final class Builder<T> {
        private final M3MatPool<T> pool;
        private final M3MatBits.Builder bits;
        private Builder(M3MatPool<T> pool) {
            this.pool = Objects.requireNonNull(pool, "pool");
            this.bits = M3MatBits.builder(pool.size());
        }
        public Builder<T> add(M3MatRef<T> ref) {
            Objects.requireNonNull(ref, "ref");
            if (ref.pool() != pool) throw new IllegalArgumentException("different pool");
            bits.set(ref.id());
            return this;
        }
        public M3MatIdSet<T> build() { return new M3MatIdSet<>(pool, bits.build()); }
    }
}
