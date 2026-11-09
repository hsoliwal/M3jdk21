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
 * Immutable primitive snapshot of live arena records, stored lane-major.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatRecordSnapshot.java}
 * blob {@code c53a6ba6443909a374ec5abd675293d43d594b51} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatRecordSnapshot {
    private final int lanes;
    private final long[] handles;
    private final long[] values;

    MatRecordSnapshot(int lanes, long[] handles, long[] values) {
        if (lanes < 1) throw new IllegalArgumentException("lanes");
        this.lanes = lanes;
        this.handles = Objects.requireNonNull(handles, "handles").clone();
        this.values = Objects.requireNonNull(values, "values").clone();
        if (values.length != Math.multiplyExact(lanes, handles.length)) {
            throw new IllegalArgumentException("snapshot geometry mismatch");
        }
    }

    public int lanes() { return lanes; }
    public int rows() { return handles.length; }

    public long handle(int row) {
        return handles[Objects.checkIndex(row, handles.length)];
    }

    public long get(int row, int lane) {
        Objects.checkIndex(row, handles.length);
        Objects.checkIndex(lane, lanes);
        return values[lane * handles.length + row];
    }

    public long[] handles() { return handles.clone(); }

    public long[] laneValues(int lane) {
        Objects.checkIndex(lane, lanes);
        long[] out = new long[handles.length];
        System.arraycopy(values, lane * handles.length, out, 0, handles.length);
        return out;
    }

    public long primitivePayloadBytes() {
        return 8L * (handles.length + values.length);
    }
}
