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
import java.util.UUID;

/**
 * Immutable UUID column stored as two primitive 64-bit lanes per row.
 *
 * <p>When all values are present, payload is exactly 16 bytes per UUID:
 * most-significant bits followed by least-significant bits. If nulls occur,
 * one compact presence bitmap is added. No 36-character spelling and no per-row
 * UUID object is retained; Java UUID objects are materialized only at the object boundary.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatUuidVector.java}
 * blob {@code 200c850a71df8cc9db6e3c32f588e510264ec44f} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatUuidVector {
    private final long[] most;
    private final long[] least;
    private final long[] present;

    public MatUuidVector(UUID[] values) {
        Objects.requireNonNull(values, "values");
        this.most = new long[values.length];
        this.least = new long[values.length];
        long[] presence = new long[(values.length + 63) >>> 6];
        boolean hasNull = false;
        for (int index = 0; index < values.length; index++) {
            UUID value = values[index];
            if (value == null) {
                hasNull = true;
                continue;
            }
            most[index] = value.getMostSignificantBits();
            least[index] = value.getLeastSignificantBits();
            presence[index >>> 6] |= 1L << (index & 63);
        }
        this.present = hasNull ? presence : null;
    }

    private MatUuidVector(long[] most, long[] least, long[] present) {
        this.most = most;
        this.least = least;
        this.present = present;
    }

    public static MatUuidVector ofWords(long[] most, long[] least) {
        Objects.requireNonNull(most, "most");
        Objects.requireNonNull(least, "least");
        if (most.length != least.length) {
            throw new IllegalArgumentException("UUID lane lengths differ");
        }
        return new MatUuidVector(most.clone(), least.clone(), null);
    }

    public int size() {
        return most.length;
    }

    public UUID get(int index) {
        Objects.checkIndex(index, most.length);
        if (isNull(index)) return null;
        return new UUID(most[index], least[index]);
    }

    public boolean isNull(int index) {
        Objects.checkIndex(index, most.length);
        return present != null && (present[index >>> 6] & (1L << (index & 63))) == 0L;
    }

    public long mostSignificantBits(int index) {
        Objects.checkIndex(index, most.length);
        if (isNull(index)) throw new IllegalStateException("UUID value is null");
        return most[index];
    }

    public long leastSignificantBits(int index) {
        Objects.checkIndex(index, least.length);
        if (isNull(index)) throw new IllegalStateException("UUID value is null");
        return least[index];
    }

    /** 16 bytes per row plus a presence bitmap only when nulls exist. */
    public long payloadBytes() {
        long valueBytes = Math.multiplyExact(16L, most.length);
        return present == null ? valueBytes : Math.addExact(valueBytes, 8L * present.length);
    }

    public MatBits equalTo(UUID target) {
        MatBits.Builder matches = MatBits.builder(most.length);
        if (target == null) {
            if (present == null) return matches.build();
            for (int index = 0; index < most.length; index++) {
                if (isNull(index)) matches.set(index);
            }
            return matches.build();
        }
        long targetMost = target.getMostSignificantBits();
        long targetLeast = target.getLeastSignificantBits();
        for (int index = 0; index < most.length; index++) {
            if (!isNull(index)
                    && most[index] == targetMost
                    && least[index] == targetLeast) {
                matches.set(index);
            }
        }
        return matches.build();
    }

    public long[] mostWords() {
        return most.clone();
    }

    public long[] leastWords() {
        return least.clone();
    }

    public long[] presenceWords() {
        return present == null ? new long[0] : present.clone();
    }
}
