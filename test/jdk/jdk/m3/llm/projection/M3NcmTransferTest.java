// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Preserve complete result transfer and cancellation at the 65,536-row boundary. */
class M3NcmTransferTest {
    private static Method nativeMethod(String name, Class<?>... types) throws ReflectiveOperationException {
        Method method = M3NcmNativeOps.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
    private static int[] invoke(Method method, Object... args) throws Throwable {
        try { return (int[]) method.invoke(null, args); }
        catch (InvocationTargetException wrapper) { throw wrapper.getCause(); }
    }
    private static float[] rows(int count) {
        float[] rows = new float[count];
        for (int i = 0; i < count; i++) rows[i] = i & 3;
        return rows;
    }
    private static int[] expected(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = (i & 3) < 2 ? 0 : 1;
        return result;
    }

    @Test void legacyExportKeepsAllIdsAndLowestIdTies() throws Throwable {
        float[] book = {0, 2, 2};
        if (!M3NcmNativeOps.available()) {
            assertArrayEquals(expected(65537), M3NcmNativeOps.nearest(rows(65537), 1, book,
                    M3NcmNativeOps.Mode.AUTO));
            assertThrows(UnsupportedOperationException.class, () -> M3NcmNativeOps.nearest(
                    rows(1), 1, book, M3NcmNativeOps.Mode.NATIVE_REQUIRED));
            return;
        }
        Method legacy = nativeMethod("nearestNative", float[].class, int.class, float[].class);
        for (int count : new int[]{65535, 65536, 65537, 131072}) {
            float[] input = rows(count);
            assertArrayEquals(expected(count), invoke(legacy, input, 1, book));
            assertArrayEquals(rows(count), input);
            assertArrayEquals(new float[]{0, 2, 2}, book);
        }
    }

    @Test void controlledTransferRetainsBoundaryCallbacksExceptionIdentityAndRecovery() throws Throwable {
        float[] book = {0, 2, 2};
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class, () -> M3NcmNativeOps.nearest(
                    rows(1), 1, book, M3NcmNativeOps.Mode.NATIVE_REQUIRED,
                    M3NcmOps.DEFAULT_BUDGET, () -> { }));
            return;
        }
        Method controlled = nativeMethod("nearestControlledNative", float[].class, int.class,
                float[].class, long.class, M3NcmOps.Cancellation.class);
        for (int count : new int[]{65535, 65536, 65537, 131072}) {
            float[] input = rows(count);
            AtomicInteger successCalls = new AtomicInteger();
            assertArrayEquals(expected(count), invoke(controlled, input, 1, book, count + 3L,
                    (M3NcmOps.Cancellation) successCalls::incrementAndGet));
            // Two input scans, three code norms, one checkpoint per row, transfer
            // at each completed 65,536 rows, and the final Java-array export.
            assertEquals(count + 6 + 2 * (count / 65536), successCalls.get());
            int beforeTransfer = count + 5 + count / 65536;
            for (int stop = beforeTransfer; stop <= beforeTransfer + count / 65536 + 1; stop++) {
                final int target = stop;
                AtomicInteger calls = new AtomicInteger();
                RuntimeException marker = new RuntimeException("transfer checkpoint " + stop);
                RuntimeException actual = assertThrows(RuntimeException.class, () ->
                        invoke(controlled, input, 1, book, count + 3L,
                                (M3NcmOps.Cancellation) () -> {
                                    if (calls.incrementAndGet() == target) throw marker;
                                }));
                assertSame(marker, actual);
                assertEquals(target, calls.get());
                assertArrayEquals(expected(count), invoke(controlled, input, 1, book, count + 3L, null));
                assertArrayEquals(rows(count), input);
                assertTrue(Arrays.equals(book, new float[]{0, 2, 2}));
            }
        }
    }
}
