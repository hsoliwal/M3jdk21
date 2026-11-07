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

/**
 * Opaque generational handle used to lower pointer identity into stable primitive coordinates.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatPointerHandle.java}
 * blob {@code 14704d778097d200151b44351c8279ef5eae4f75} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatPointerHandle {
    public static final long NULL = 0L;

    private M3MatPointerHandle() {}

    public static long of(int slot, int generation) {
        if (slot < 0 || generation == 0) {
            throw new IllegalArgumentException("slot must be nonnegative and generation nonzero");
        }
        long encodedSlot = Integer.toUnsignedLong(Math.addExact(slot, 1));
        return (Integer.toUnsignedLong(generation) << 32) | encodedSlot;
    }

    public static boolean isNull(long handle) {
        return handle == NULL;
    }

    public static int slot(long handle) {
        if (handle == NULL) throw new IllegalArgumentException("null handle");
        long encoded = handle & 0xffff_ffffL;
        if (encoded == 0L || encoded > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid pointer handle slot");
        }
        return (int) encoded - 1;
    }

    public static int generation(long handle) {
        if (handle == NULL) throw new IllegalArgumentException("null handle");
        int generation = (int) (handle >>> 32);
        if (generation == 0) throw new IllegalArgumentException("invalid pointer handle generation");
        return generation;
    }
}
