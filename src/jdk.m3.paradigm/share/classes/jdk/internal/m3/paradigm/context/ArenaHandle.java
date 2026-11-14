/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

/**
 * 64-bit slot+generation address for one IndexArena. Zero is invalid.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextArenaHandle.java}
 * blob {@code 47015152c46aa8e022033dd4fda81ad3bc4470a0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ArenaHandle {
    public static final long INVALID = 0L;

    private ArenaHandle() {}

    public static long of(int slot, int generation) {
        if (slot < 0) throw new IllegalArgumentException("slot must be >= 0");
        if (generation <= 0) throw new IllegalArgumentException("generation must be > 0");
        long encodedSlot = Integer.toUnsignedLong(Math.addExact(slot, 1));
        return ((long) generation << Integer.SIZE) | encodedSlot;
    }

    public static int slot(long handle) {
        int encoded = (int) handle;
        if (handle == INVALID || encoded == 0) throw new IllegalArgumentException("invalid context arena handle");
        return encoded - 1;
    }

    public static int generation(long handle) {
        int generation = (int) (handle >>> Integer.SIZE);
        if (handle == INVALID || generation <= 0) throw new IllegalArgumentException("invalid context arena handle");
        return generation;
    }
}
