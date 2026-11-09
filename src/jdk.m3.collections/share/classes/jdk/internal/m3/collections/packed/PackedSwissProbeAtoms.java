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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: reuse one captured control word within each serial group probe.
package jdk.internal.m3.collections.packed;

/**
 * Shared zero-object Swiss-style grouped control-byte probing.
 *
 * <p>The portable 8-byte group classifier is mechanically adapted from Abseil SwissTable's
 * {@code GroupPortableImpl} at donor revision
 * {@code 33a35cf45087dffab22c0c9ae817021a45e93a47}, Apache-2.0. Synexia keeps its own
 * primitive table layout, hashing, growth, deletion, API and publication contracts.</p>
 *
 * <p>Match masks may contain the same documented carry-induced false positives as the Abseil
 * portable classifier. They never hide a real fingerprint match and never nominate EMPTY or
 * DELETED control bytes. Full-key comparison remains authoritative.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedSwissProbeAtoms.java}
 * blob {@code 0657a0a3d0c9dbeb6367f729950fc3b7c59120da} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedSwissProbeAtoms {
    static final int GROUP_SIZE = 8;
    static final byte EMPTY = (byte) 0x80;
    static final byte DELETED = (byte) 0xfe;

    private static final long LSBS = 0x0101010101010101L;
    private static final long MSBS = 0x8080808080808080L;

    private PackedSwissProbeAtoms() { }

    static float validateLoadFactor(float loadFactor, float maximum) {
        if (!(loadFactor > 0.0f && loadFactor <= maximum)) {
            throw new IllegalArgumentException("loadFactor");
        }
        return loadFactor;
    }

    static int threshold(int capacity, float loadFactor) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * loadFactor)));
    }

    static int groupMask(int capacity) {
        return (capacity >>> 3) - 1;
    }

    static byte fingerprint(int hash) {
        return (byte) ((hash >>> 25) & 0x7f);
    }

    static boolean isFull(byte control) {
        return control >= 0;
    }

    /** Little-endian packed control group; base is always an eight-slot group boundary. */
    static long groupWord(byte[] controls, int base) {
        return (controls[base] & 0xffL)
                | ((controls[base + 1] & 0xffL) << 8)
                | ((controls[base + 2] & 0xffL) << 16)
                | ((controls[base + 3] & 0xffL) << 24)
                | ((controls[base + 4] & 0xffL) << 32)
                | ((controls[base + 5] & 0xffL) << 40)
                | ((controls[base + 6] & 0xffL) << 48)
                | ((controls[base + 7] & 0xffL) << 56);
    }

    /** Candidate lanes for one seven-bit fingerprint; bits are lane-byte MSBs. */
    static long matchMask(byte[] controls, int base, byte fingerprint) {
        return matchMask(groupWord(controls, base), fingerprint);
    }

    private static long matchMask(long control, byte fingerprint) {
        long x = control ^ ((fingerprint & 0x7fL) * LSBS);
        return (x - LSBS) & ~x & MSBS;
    }

    /** Exact EMPTY lanes; bits are lane-byte MSBs. */
    static long emptyMask(byte[] controls, int base) {
        return emptyMask(groupWord(controls, base));
    }

    private static long emptyMask(long control) {
        return (control & ~(control << 6)) & MSBS;
    }

    /** Exact EMPTY or DELETED lanes; bits are lane-byte MSBs. */
    static long emptyOrDeletedMask(byte[] controls, int base) {
        return emptyOrDeletedMask(groupWord(controls, base));
    }

    private static long emptyOrDeletedMask(long control) {
        return (control & ~(control << 7)) & MSBS;
    }

    /** Exact full lanes; bits are lane-byte MSBs. */
    static long fullMask(byte[] controls, int base) {
        return (groupWord(controls, base) ^ MSBS) & MSBS;
    }

    static int firstLane(long mask) {
        return Long.numberOfTrailingZeros(mask) >>> 3;
    }

    static int findIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        byte fingerprint = fingerprint(hash);
        int groupMask = groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        while (true) {
            int base = group << 3;
            long control = groupWord(controls, base);
            long candidates = matchMask(control, fingerprint);
            while (candidates != 0L) {
                int lane = firstLane(candidates);
                int slot = base + lane;
                if (keys[slot] == key) {
                    return slot;
                }
                candidates &= candidates - 1L;
            }
            if (emptyMask(control) != 0L) {
                return -1;
            }
            group = (group + ++step) & groupMask;
        }
    }

    /**
     * Probe once for an existing key or insertion slot.
     *
     * @return existing slot when non-negative; otherwise insertion slot is {@code ~result}
     */
    static int findOrInsertionIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        byte fingerprint = fingerprint(hash);
        int groupMask = groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        int firstDeleted = -1;
        while (true) {
            int base = group << 3;
            long control = groupWord(controls, base);
            long candidates = matchMask(control, fingerprint);
            while (candidates != 0L) {
                int lane = firstLane(candidates);
                int slot = base + lane;
                if (keys[slot] == key) {
                    return slot;
                }
                candidates &= candidates - 1L;
            }

            long empty = emptyMask(control);
            long emptyOrDeleted = emptyOrDeletedMask(control);
            if (firstDeleted < 0) {
                long deleted = emptyOrDeleted & ~empty;
                if (deleted != 0L) {
                    firstDeleted = base + firstLane(deleted);
                }
            }
            if (empty != 0L) {
                int target = firstDeleted >= 0 ? firstDeleted : base + firstLane(empty);
                return ~target;
            }
            group = (group + ++step) & groupMask;
        }
    }

    static int rehashInsertionIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        int groupMask = groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        while (true) {
            int base = group << 3;
            long empty = emptyMask(controls, base);
            if (empty != 0L) {
                return base + firstLane(empty);
            }
            group = (group + ++step) & groupMask;
        }
    }
}
