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

import java.util.Objects;

/**
 * Uniform facade over one selected immutable primitive int-vector representation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatPlannedIntVector.java}
 * blob {@code f1c6e7a883a0d323b21257e00fb0abdbeb91d828} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPlannedIntVector {
    public enum Kind { ADAPTIVE, BLOCK, SPARSE }

    private final Kind kind;
    private final MatAdaptiveIntVector adaptive;
    private final MatBlockIntVector block;
    private final MatSparseIntVector sparse;
    private final long payloadBytes;

    private MatPlannedIntVector(
            Kind kind,
            MatAdaptiveIntVector adaptive,
            MatBlockIntVector block,
            MatSparseIntVector sparse,
            long payloadBytes) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.adaptive = adaptive;
        this.block = block;
        this.sparse = sparse;
        this.payloadBytes = payloadBytes;
    }

    static MatPlannedIntVector adaptive(MatAdaptiveIntVector value) {
        return new MatPlannedIntVector(Kind.ADAPTIVE, value, null, null, value.primitivePayloadBytes());
    }

    static MatPlannedIntVector block(MatBlockIntVector value) {
        return new MatPlannedIntVector(Kind.BLOCK, null, value, null, value.primitivePayloadBytes());
    }

    static MatPlannedIntVector sparse(MatSparseIntVector value) {
        return new MatPlannedIntVector(Kind.SPARSE, null, null, value, value.primitivePayloadBytes());
    }

    public Kind kind() { return kind; }
    public int size() {
        return switch (kind) {
            case ADAPTIVE -> adaptive.size();
            case BLOCK -> block.size();
            case SPARSE -> sparse.size();
        };
    }

    public int get(int index) {
        return switch (kind) {
            case ADAPTIVE -> adaptive.get(index);
            case BLOCK -> block.get(index);
            case SPARSE -> sparse.get(index);
        };
    }

    public int[] toArray() {
        return switch (kind) {
            case ADAPTIVE -> adaptive.toArray();
            case BLOCK -> block.toArray();
            case SPARSE -> sparse.toArray();
        };
    }

    public long primitivePayloadBytes() { return payloadBytes; }
}
