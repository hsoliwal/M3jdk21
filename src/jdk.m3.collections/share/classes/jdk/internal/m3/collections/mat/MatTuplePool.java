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
 * Frozen, exactly interned tuples of handles into one immutable value pool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatTuplePool.java}
 * blob {@code 6c80dfbfb9a973892e5e90c3dbbe8d6e21d3fbe4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatTuplePool<T> {
    private final MatPool<T> owner;
    private final MatInternedRows tuples;

    private MatTuplePool(MatPool<T> owner, MatInternedRows tuples) {
        this.owner = owner;
        this.tuples = tuples;
    }

    public static <T> Builder<T> builder(MatPool<T> owner, MatLimits limits) {
        return new Builder<>(owner, limits);
    }

    public MatPool<T> owner() { return owner; }
    public int size() { return tuples.size(); }
    public int arity(int tuple) { return tuples.length(tuple); }
    public int idAt(int tuple, int index) { return tuples.get(tuple, index); }
    public T valueAt(int tuple, int index) { return owner.value(idAt(tuple, index)); }
    public int[] ids(int tuple) { return tuples.row(tuple); }
    public int idOf(int... ids) { return tuples.idOf(ids); }
    public long payloadBytes() { return tuples.payloadBytes(); }

    public static final class Builder<T> {
        private final MatPool<T> owner;
        private final MatInternedRows.Builder tuples;

        private Builder(MatPool<T> owner, MatLimits limits) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.tuples = MatInternedRows.builder(limits);
        }

        public int size() { return tuples.size(); }

        public int intern(int... ids) {
            Objects.requireNonNull(ids, "ids");
            for (int id : ids) {
                if (id < 0 || id >= owner.size()) {
                    throw new IndexOutOfBoundsException("unknown owner ID: " + id);
                }
            }
            return tuples.intern(ids);
        }

        public MatTuplePool<T> build() { return new MatTuplePool<>(owner, tuples.build()); }
    }
}
