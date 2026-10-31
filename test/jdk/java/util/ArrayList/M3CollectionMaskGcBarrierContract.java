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
 * @summary M3JDK21 ArrayList/Vector masked removeIf retains exact survivor references across G1 and Serial collections
 * @run main/othervm -Xms24m -Xmx96m -XX:+UseG1GC M3CollectionMaskGcBarrierContract
 * @run main/othervm -Xms24m -Xmx96m -XX:+UseSerialGC M3CollectionMaskGcBarrierContract
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Public Java-heap oracle for reference-bearing masked compaction. Both GC
 * configurations must run on the actual source-built M3JDK21 image; the test
 * does not claim a complete GC/card-mark or concurrency proof.
 */
public final class M3CollectionMaskGcBarrierContract {
    private static long checks;

    private static final class Cell {
        final int serial;
        final byte[] payload = new byte[96];
        Cell(int serial) { this.serial = serial; }
    }

    public static void main(String[] args) {
        for (boolean vector : new boolean[]{false, true}) {
            for (int size : new int[]{511, 512, 513, 1024, 3072}) {
                for (int mode = 0; mode < 6; mode++) {
                    verify(vector, size, mode);
                }
            }
        }
        System.out.println("M3CollectionMaskGcBarrierContract checks=" + checks);
    }

    private static void verify(boolean vector, int size, int mode) {
        List<Cell> elements = vector ? new Vector<>() : new ArrayList<>();
        Cell shared = new Cell(-1);
        Cell[] original = new Cell[size];
        for (int index = 0; index < size; index++) {
            Cell value = index % 53 == 0 ? null
                    : index % 29 == 0 ? shared : new Cell(index);
            original[index] = value;
            elements.add(value);
        }
        // Age reference-bearing backing storage before mutation, then force
        // allocation/collection again after compaction to exercise liveness.
        System.gc();
        boolean expectedChanged = false;
        for (int i = 0; i < size; i++) expectedChanged |= removed(i, size, mode);
        int[] callbacks = {0};
        boolean changed = elements.removeIf(value -> {
            int index = callbacks[0]++;
            if (index >= size || value != original[index]) {
                throw new AssertionError("Predicate order/reference identity changed");
            }
            checks++;
            return removed(index, size, mode);
        });
        same(expectedChanged, changed);
        same(size, callbacks[0]);
        byte[][] churn = new byte[16][];
        for (int i = 0; i < churn.length; i++) churn[i] = new byte[128 * 1024];
        if (churn[0].length != 128 * 1024) throw new AssertionError();
        System.gc();
        int survivor = 0;
        for (int index = 0; index < size; index++) {
            if (!removed(index, size, mode)) {
                if (elements.get(survivor++) != original[index]) {
                    throw new AssertionError("Collected/moved survivor at " + index
                            + " size=" + size + " mode=" + mode + " vector=" + vector);
                }
                checks++;
            }
        }
        same(survivor, elements.size());
        Cell appended = new Cell(-2);
        elements.add(appended);
        if (elements.get(survivor) != appended) throw new AssertionError("Append failed");
        checks++;
    }

    private static boolean removed(int index, int size, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (index & 1) == 0;
            case 3 -> index >= size / 4 && index < (size * 3) / 4;
            case 4 -> index % 127 == 0;
            default -> index % 37 == 0 || index % 37 == 36;
        };
    }

    private static void same(boolean left, boolean right) {
        if (left != right) throw new AssertionError("boolean mismatch");
        checks++;
    }

    private static void same(int left, int right) {
        if (left != right) throw new AssertionError(left + " != " + right);
        checks++;
    }
}
