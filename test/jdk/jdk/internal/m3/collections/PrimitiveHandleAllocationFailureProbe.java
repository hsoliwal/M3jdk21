// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitiveSoaStore;

import java.lang.reflect.Field;

/** Isolated, genuinely memory-constrained allocation failure; no mocked allocator. */
final class PrimitiveHandleAllocationFailureProbe {
    private PrimitiveHandleAllocationFailureProbe() { }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrimitiveHandleTable table = new PrimitiveHandleTable(1,
                PrimitiveSoaStore.STATE_BYTES | PrimitiveSoaStore.VERSION_LONGS,
                PrimitiveKind.LONG);
        Field[] indexes = fields(PrimitiveHandleTable.class,
                "slotToRow", "rowToSlot", "generations", "nextFree");
        Field countField = field(PrimitiveHandleTable.class, "slotCount");
        Field freeField = field(PrimitiveHandleTable.class, "freeHead");
        PrimitiveSoaStore rows = (PrimitiveSoaStore) field(PrimitiveHandleTable.class, "rows").get(table);
        Field[] rowArrays = fields(PrimitiveSoaStore.class, "lanes", "states", "rowIds", "versions");
        Object[] priorIndexes = new Object[indexes.length];
        Object[] priorRowArrays = new Object[rowArrays.length];
        long[] input = {0};
        long first = table.add(input);
        for (int attempt = 1; attempt < 2_000_000; attempt++) {
            for (int i = 0; i < indexes.length; i++) priorIndexes[i] = indexes[i].get(table);
            for (int i = 0; i < rowArrays.length; i++) priorRowArrays[i] = rowArrays[i].get(rows);
            int oldCount = countField.getInt(table);
            int oldFree = freeField.getInt(table);
            int oldSize = table.size();
            long oldVersion = rows.version();
            long oldPayload = table.payloadBytes();
            input[0] = attempt;
            try {
                table.add(input, (byte) attempt, attempt);
            } catch (OutOfMemoryError expected) {
                boolean unchanged = oldCount == countField.getInt(table)
                        && oldFree == freeField.getInt(table) && oldSize == table.size()
                        && oldVersion == rows.version() && oldPayload == table.payloadBytes();
                for (int i = 0; i < indexes.length; i++) {
                    unchanged &= priorIndexes[i] == indexes[i].get(table);
                }
                for (int i = 0; i < rowArrays.length; i++) {
                    unchanged &= priorRowArrays[i] == rowArrays[i].get(rows);
                }
                unchanged &= table.contains(first) && table.getBits(first, 0) == 0;
                if (!unchanged) {
                    System.err.println("FAIL allocation failure published partial allocator state");
                    System.exit(2);
                }
                // Failed staged arrays can be reclaimed; existing handles remain usable.
                System.gc();
                if (!table.remove(first) || table.contains(first)) System.exit(3);
                System.out.println("PASS allocation failure preserved rows=" + oldSize);
                return;
            }
        }
        throw new AssertionError("fixture did not encounter allocation failure");
    }

    private static Field[] fields(Class<?> owner, String... names) throws NoSuchFieldException {
        Field[] result = new Field[names.length];
        for (int i = 0; i < names.length; i++) result[i] = field(owner, names[i]);
        return result;
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
}
