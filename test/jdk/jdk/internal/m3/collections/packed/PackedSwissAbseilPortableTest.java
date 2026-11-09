// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PackedSwissAbseilPortableTest {
    private static final long MSBS = 0x8080808080808080L;

    @Test
    void portableGroupMasksMatchScalarControlSemantics() {
        Random random = new Random(0x41425345494cL);
        byte[] controls = new byte[PackedSwissProbeAtoms.GROUP_SIZE];

        for (int trial = 0; trial < 50_000; trial++) {
            for (int lane = 0; lane < controls.length; lane++) {
                int choice = random.nextInt(10);
                controls[lane] = choice == 0
                        ? PackedSwissProbeAtoms.EMPTY
                        : choice == 1
                                ? PackedSwissProbeAtoms.DELETED
                                : (byte) random.nextInt(128);
            }

            long empty = scalarMask(controls, PackedSwissProbeAtoms.EMPTY);
            long deleted = scalarMask(controls, PackedSwissProbeAtoms.DELETED);
            long full = MSBS & ~(empty | deleted);
            assertEquals(empty, PackedSwissProbeAtoms.emptyMask(controls, 0));
            assertEquals(empty | deleted, PackedSwissProbeAtoms.emptyOrDeletedMask(controls, 0));
            assertEquals(full, PackedSwissProbeAtoms.fullMask(controls, 0));

            for (int fingerprint = 0; fingerprint < 128; fingerprint++) {
                long exact = scalarMask(controls, (byte) fingerprint);
                long candidates =
                        PackedSwissProbeAtoms.matchMask(controls, 0, (byte) fingerprint);
                assertEquals(exact, candidates & exact, "real match must never be hidden");
                assertEquals(0L, candidates & (empty | deleted), "special controls cannot match");
            }
        }
    }

    @Test
    void portableProbeBehaviorMatchesScalarPreimage() {
        Random random = new Random(0x535749535350L);
        for (int trial = 0; trial < 20_000; trial++) {
            int capacity = 8 << random.nextInt(4);
            long[] keys = new long[capacity];
            byte[] controls = new byte[capacity];
            Arrays.fill(controls, PackedSwissProbeAtoms.EMPTY);

            int entries = random.nextInt(Math.max(1, capacity * 3 / 4));
            for (int entry = 0; entry < entries; entry++) {
                long key = random.nextLong();
                int slot = scalarFindOrInsertionIndex(keys, controls, key);
                if (slot >= 0) {
                    entry--;
                    continue;
                }
                int target = ~slot;
                keys[target] = key;
                controls[target] =
                        PackedSwissProbeAtoms.fingerprint(PackedSupport.mix(key));
            }

            for (int query = 0; query < 32; query++) {
                long key = query < entries ? keys[random.nextInt(capacity)] : random.nextLong();
                assertEquals(
                        scalarFindIndex(keys, controls, key),
                        PackedSwissProbeAtoms.findIndex(keys, controls, key));
                assertEquals(
                        scalarFindOrInsertionIndex(keys, controls, key),
                        PackedSwissProbeAtoms.findOrInsertionIndex(keys, controls, key));
            }

            byte[] fresh = new byte[capacity];
            Arrays.fill(fresh, PackedSwissProbeAtoms.EMPTY);
            for (int query = 0; query < 16; query++) {
                long key = random.nextLong();
                assertEquals(
                        scalarRehashInsertionIndex(new long[capacity], fresh, key),
                        PackedSwissProbeAtoms.rehashInsertionIndex(
                                new long[capacity], fresh, key));
            }
        }
    }

    private static long scalarMask(byte[] controls, byte value) {
        long result = 0L;
        for (int lane = 0; lane < controls.length; lane++) {
            if (controls[lane] == value) {
                result |= 1L << (lane * 8 + 7);
            }
        }
        return result;
    }

    private static int scalarFindIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        byte fingerprint = PackedSwissProbeAtoms.fingerprint(hash);
        int groupMask = PackedSwissProbeAtoms.groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        while (true) {
            int base = group << 3;
            boolean hasEmpty = false;
            for (int lane = 0; lane < PackedSwissProbeAtoms.GROUP_SIZE; lane++) {
                int slot = base + lane;
                byte control = controls[slot];
                if (control == fingerprint && keys[slot] == key) {
                    return slot;
                }
                if (control == PackedSwissProbeAtoms.EMPTY) {
                    hasEmpty = true;
                }
            }
            if (hasEmpty) {
                return -1;
            }
            group = (group + ++step) & groupMask;
        }
    }

    private static int scalarFindOrInsertionIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        byte fingerprint = PackedSwissProbeAtoms.fingerprint(hash);
        int groupMask = PackedSwissProbeAtoms.groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        int firstDeleted = -1;
        while (true) {
            int base = group << 3;
            int firstEmpty = -1;
            for (int lane = 0; lane < PackedSwissProbeAtoms.GROUP_SIZE; lane++) {
                int slot = base + lane;
                byte control = controls[slot];
                if (control == fingerprint && keys[slot] == key) {
                    return slot;
                }
                if (control == PackedSwissProbeAtoms.DELETED && firstDeleted < 0) {
                    firstDeleted = slot;
                } else if (control == PackedSwissProbeAtoms.EMPTY && firstEmpty < 0) {
                    firstEmpty = slot;
                }
            }
            if (firstEmpty >= 0) {
                return ~(firstDeleted >= 0 ? firstDeleted : firstEmpty);
            }
            group = (group + ++step) & groupMask;
        }
    }

    private static int scalarRehashInsertionIndex(long[] keys, byte[] controls, long key) {
        int hash = PackedSupport.mix(key);
        int groupMask = PackedSwissProbeAtoms.groupMask(keys.length);
        int group = hash & groupMask;
        int step = 0;
        while (true) {
            int base = group << 3;
            for (int lane = 0; lane < PackedSwissProbeAtoms.GROUP_SIZE; lane++) {
                int slot = base + lane;
                if (controls[slot] == PackedSwissProbeAtoms.EMPTY) {
                    return slot;
                }
            }
            group = (group + ++step) & groupMask;
        }
    }
}
