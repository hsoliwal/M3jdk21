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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable dense primitive-lane snapshot with no per-row objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneSnapshot.java}
 * blob {@code 2b4fc0d8c77b119cb2a475885d48f43f42314c04} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneSnapshot {
    private final long[][] lanes;
    private final int size;

    PackedLongLaneSnapshot(long[][] source, int size) {
        this.size = size;
        lanes = new long[source.length][];
        for (int lane = 0; lane < source.length; lane++) {
            lanes[lane] = Arrays.copyOf(source[lane], size);
        }
    }

    public int laneCount() {
        return lanes.length;
    }

    public int size() {
        return size;
    }

    public long get(int row, int lane) {
        Objects.checkIndex(row, size);
        Objects.checkIndex(lane, lanes.length);
        return lanes[lane][row];
    }

    public long[] laneToArray(int lane) {
        Objects.checkIndex(lane, lanes.length);
        return lanes[lane].clone();
    }

    public long[] rowToArray(int row) {
        Objects.checkIndex(row, size);
        long[] values = new long[lanes.length];
        for (int lane = 0; lane < lanes.length; lane++) {
            values[lane] = lanes[lane][row];
        }
        return values;
    }

    public long payloadBytes() {
        return 8L * size * lanes.length;
    }
}
