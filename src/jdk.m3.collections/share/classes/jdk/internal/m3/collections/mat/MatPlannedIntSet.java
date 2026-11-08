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
 * Uniform facade over selected finite/adaptive int-set implementations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatPlannedIntSet.java}
 * blob {@code ef12abadb7b9305ad8136ebaaa9220fb9254bce1} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPlannedIntSet {
    public enum Kind { ADAPTIVE_FINITE, ROARING_SIGNED }

    private final Kind kind;
    private final MatAdaptiveIntSet adaptive;
    private final MatRoaringIntSet roaring;

    private MatPlannedIntSet(Kind kind, MatAdaptiveIntSet adaptive, MatRoaringIntSet roaring) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.adaptive = adaptive;
        this.roaring = roaring;
    }

    static MatPlannedIntSet adaptive(MatAdaptiveIntSet value) {
        return new MatPlannedIntSet(Kind.ADAPTIVE_FINITE, value, null);
    }

    static MatPlannedIntSet roaring(MatRoaringIntSet value) {
        return new MatPlannedIntSet(Kind.ROARING_SIGNED, null, value);
    }

    public Kind kind() { return kind; }
    public int size() { return kind == Kind.ADAPTIVE_FINITE ? adaptive.size() : roaring.size(); }
    public boolean contains(int value) {
        return kind == Kind.ADAPTIVE_FINITE ? adaptive.contains(value) : roaring.contains(value);
    }
    public int[] toArray() {
        return kind == Kind.ADAPTIVE_FINITE ? adaptive.toArray() : roaring.toArray();
    }
    public long primitivePayloadBytes() {
        return kind == Kind.ADAPTIVE_FINITE ? adaptive.primitivePayloadBytes() : roaring.primitivePayloadBytes();
    }
    public MatRoaringIntSet roaring() {
        if (kind != Kind.ROARING_SIGNED) {
            throw new IllegalStateException("selected set is not roaring/rank-select capable");
        }
        return roaring;
    }
}
