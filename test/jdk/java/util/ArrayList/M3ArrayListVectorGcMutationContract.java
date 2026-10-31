/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary M3JDK21 ArrayList and Vector mask compaction retains object identity across GC
 * @run main/othervm -XX:+UseG1GC -Xmx128m M3ArrayListVectorGcMutationContract
 * @run main/othervm -XX:+UseSerialGC -Xmx128m M3ArrayListVectorGcMutationContract
 */

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** Independent public contract; does not prove collector-specific card marks. */
public final class M3ArrayListVectorGcMutationContract {
    private static long checks;

    private M3ArrayListVectorGcMutationContract() { }

    public static void main(String[] args) {
        for (boolean vector : new boolean[] {false, true}) {
            for (int length : new int[] {511, 512, 513, 1024, 2049, 4096}) {
                for (int mode = 0; mode < 5; mode++) {
                    verify(vector, length, mode);
                }
            }
        }
        System.out.println("PASS M3ArrayListVectorGcMutationContract checks=" + checks);
    }

    private static void verify(boolean vector, int length, int mode) {
        List<Payload> list = vector ? new Vector<>() : new ArrayList<>();
        Payload[] original = new Payload[length];
        for (int index = 0; index < length; index++) {
            Payload value = new Payload(index);
            original[index] = value;
            list.add(value);
        }
        Iterator<Payload> prior = list.iterator();
        int[] invoked = {0};
        boolean expectedChange = false;
        for (int i = 0; i < length; i++) expectedChange |= deleted(i, length, mode);
        boolean changed = list.removeIf(value -> {
            int position = invoked[0]++;
            same(original[position], value);
            if ((position & 511) == 0) System.gc();
            return deleted(position, length, mode);
        });
        sameBoolean(expectedChange, changed);
        sameInt(length, invoked[0]);
        if (expectedChange) {
            try {
                prior.next();
                throw new AssertionError("Stale iterator did not fail fast");
            } catch (ConcurrentModificationException expected) {
                checks++;
            }
        } else {
            same(original[0], prior.next());
        }
        validate(original, list, length, mode);
        System.gc();
        validate(original, list, length, mode);
        list.clear();
        sameInt(0, list.size());
    }

    private static boolean deleted(int index, int length, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (index & 1) == 0;
            case 3 -> index >= length / 4 && index < (3 * length / 4);
            default -> index % 127 == 0;
        };
    }

    private static void validate(Payload[] original, List<Payload> actual,
            int length, int mode) {
        int kept = 0;
        for (int index = 0; index < length; index++) {
            if (!deleted(index, length, mode)) {
                Payload survivor = actual.get(kept++);
                same(original[index], survivor);
                sameInt(index, survivor.slot);
                sameInt(index & 255, survivor.data[0] & 255);
            }
        }
        sameInt(kept, actual.size());
    }

    private static void same(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError("Reference identity changed");
        checks++;
    }

    private static void sameInt(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", observed " + actual);
        }
        checks++;
    }

    private static void sameBoolean(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("Removal result changed");
        checks++;
    }

    private static final class Payload {
        private final int slot;
        private final byte[] data = new byte[96];

        private Payload(int slot) {
            this.slot = slot;
            data[0] = (byte) slot;
        }
    }
}
