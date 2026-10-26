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
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
package com.m3.collections;

/** Checked array capacities and integer hash spreading shared by storage owners. */
final class M3SlotArrays {
    // Zero-length lanes are safe to share; insertion always installs writable storage.
    static final Object[] EMPTY_OBJECTS = new Object[0];
    static final int[] EMPTY_INTS = new int[0];
    static final byte[] EMPTY_BYTES = new byte[0];

    private M3SlotArrays() { }
    static int grow(int old) {
        if (old >= Integer.MAX_VALUE - 8) { throw new OutOfMemoryError("collection capacity exhausted"); }
        return (int) Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, (long) old + (old >> 1) + 1L));
    }
    static int doubled(int old) {
        if (old >= 1 << 30) { throw new OutOfMemoryError("hash index capacity exhausted"); }
        return old << 1;
    }
    static int hash(Object key) { int h = key == null ? 0 : key.hashCode(); return h ^ h >>> 16; }
    @SuppressWarnings("unchecked") static <T> T get(Object[] values, int slot) { return (T) values[slot]; }
}
