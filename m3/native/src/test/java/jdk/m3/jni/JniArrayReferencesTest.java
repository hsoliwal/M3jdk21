// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** JNI lifetime registration oracle, independent of native-library presence. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3JniArrayReferencesTest.java blob e6a9c98b2e23; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
class JniArrayReferencesTest {
    @Test
    void arrayExceptionRemainsPrimaryWhenNativeCleanupAlsoFails() {
        try (var table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new byte[8]);
            IllegalArgumentException primary = new IllegalArgumentException("native fault");
            IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                    () -> JniArrayReferences.withArray(table, id, () -> {
                        table.endJniArrayAccess(id); // adversarial early Release
                        throw primary;
                    }));
            assertSame(primary, actual);
            assertEquals(1, actual.getSuppressed().length);
            assertInstanceOf(IllegalStateException.class, actual.getSuppressed()[0]);
            assertEquals(0, table.jniArrayBorrowCount(id));
            assertTrue(table.eligible(id));
        }
    }

    @Test
    void criticalExceptionRemainsPrimaryWhenCleanupFails() {
        try (var table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new long[4]);
            AssertionError primary = new AssertionError("native critical fault");
            AssertionError actual = assertThrows(AssertionError.class,
                    () -> JniArrayReferences.withCritical(table, id, () -> {
                        table.endJniArrayAccess(id); // simulated native misuse
                        throw primary;
                    }));
            assertSame(primary, actual);
            assertEquals(1, actual.getSuppressed().length);
            assertInstanceOf(IllegalStateException.class, actual.getSuppressed()[0]);
            assertEquals(0, table.jniArrayBorrowCount(id));
        }
    }

    @Test
    void nativeArrayCountBlocksResidencyAndRestoresEligibility() {
        try (var table = new ScopedLeaseTable()) {
            long array = table.allocate();
            table.registerJniArray(array, new byte[4]);
            JniArrayReferences.withArray(table, array, () -> {
                assertEquals(1, table.jniArrayBorrowCount(array));
                assertFalse(table.eligible(array));
                assertFalse(table.tryRetire(array));
            });
            assertEquals(0, table.jniArrayBorrowCount(array));
            assertTrue(table.eligible(array));
        }
    }

    @Test
    void concatenationCountsTwoEdgesIncludingSameArrayTwice() {
        try (var table = new ScopedLeaseTable()) {
            long left = table.allocate();
            long right = table.allocate();
            table.registerJniArray(left, new byte[4]);
            table.registerJniArray(right, new byte[4]);
            long unrelated = table.allocate();
            JniArrayReferences.withConcat(table, left, right, (Runnable) () -> {
                assertEquals(1, table.jniArrayBorrowCount(left));
                assertEquals(1, table.jniArrayBorrowCount(right));
                long[] output = new long[table.size()];
                assertEquals(1, table.collectEligible(output, false));
                assertEquals(unrelated, output[0]);
                JniArrayReferences.withConcat(table, left, left, (Runnable) () -> {
                    assertEquals(3, table.jniArrayBorrowCount(left));
                    assertEquals(1, table.jniArrayBorrowCount(right));
                });
                assertEquals(1, table.jniArrayBorrowCount(left));
            });
            JniArrayReferences.withConcat(table, left, left, (Runnable) () ->
                    assertEquals(2, table.jniArrayBorrowCount(left)));
            assertEquals(0, table.jniArrayBorrowCount(left));
            assertEquals(0, table.jniArrayBorrowCount(right));
        }
    }

    @Test
    void retainedZeroCopySourcesKeepCountsUntilExplicitRelease() {
        try (var table = new ScopedLeaseTable()) {
            long left = table.allocate();
            long right = table.allocate();
            table.registerJniArray(left, new byte[4]);
            table.registerJniArray(right, new byte[4]);
            JniArrayReferences.retainSources(table, left, right);
            assertFalse(table.tryRetire(left));
            assertFalse(table.tryRetire(right));
            assertEquals(1, table.jniArrayBorrowCount(left));
            JniArrayReferences.releaseSources(table, left, right);
            assertTrue(table.tryRetire(left));
            assertTrue(table.tryRetire(right));
        }
    }

    @Test
    void failureDuringSecondRetainRollsBackFirst() {
        try (var table = new ScopedLeaseTable()) {
            long live = table.allocate();
            long retired = table.allocate();
            table.registerJniArray(live, new byte[4]);
            assertTrue(table.tryRetire(retired));
            assertThrows(IllegalStateException.class,
                    () -> JniArrayReferences.retainSources(table, live, retired));
            assertEquals(0, table.jniArrayBorrowCount(live));
            assertTrue(table.eligible(live));
        }
    }

    @Test
    void exceptionalNativeCallBalancesBothEdges() {
        try (var table = new ScopedLeaseTable()) {
            long left = table.allocate();
            long right = table.allocate();
            table.registerJniArray(left, new byte[4]);
            table.registerJniArray(right, new byte[4]);
            IllegalStateException failure = new IllegalStateException("JNI fault");
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> JniArrayReferences.withConcat(table, left, right,
                            (Runnable) () -> { throw failure; })));
            assertEquals(0, table.jniArrayBorrowCount(left));
            assertEquals(0, table.jniArrayBorrowCount(right));
            int returned = JniArrayReferences.withConcat(table, left, right,
                    () -> table.jniArrayBorrowCount(left) + table.jniArrayBorrowCount(right));
            assertEquals(2, returned);
            assertEquals(0, table.jniArrayBorrowCount(left));
        }
    }

    @Test
    void nativeCriticalAndStrongGlobalPinsBlockRecycling() {
        try (var table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new byte[4]);
            JniArrayReferences.withCritical(table, id, () -> {
                assertEquals(1, table.pinCount(id));
                assertFalse(table.eligible(id));
            });
            assertEquals(0, table.pinCount(id));
            JniArrayReferences.pinGlobal(table, id);
            JniArrayReferences.pinGlobal(table, id);
            assertEquals(2, table.pinCount(id));
            assertFalse(table.tryRetire(id));
            JniArrayReferences.unpinGlobal(table, id);
            assertEquals(1, table.pinCount(id));
            assertFalse(table.eligible(id));
            JniArrayReferences.unpinGlobal(table, id);
            assertTrue(table.eligible(id));
        }
    }

    /**
     * Model a checked exception escaping from native code despite the Runnable
     * or Supplier signature. Keep the same primary object if lease cleanup fails.
     */
    @Test
    void checkedNativeThrowableWinsOverLeaseCleanupInEveryScopedVariant() {
        for (int variant = 0; variant < 6; variant++) {
            try (var table = new ScopedLeaseTable()) {
                long left = table.allocate();
                long right = table.allocate();
                table.registerJniArray(left, new byte[4]);
                table.registerJniArray(right, new byte[4]);
                IOException primary = new IOException("JNI checked exception " + variant);
                int kind = variant;
                Runnable callback = () -> {
                    table.endJniArrayAccess(kind == 2 || kind == 3 ? right : left);
                    nativeChecked(primary);
                };
                java.util.function.Supplier<Integer> supplier = () -> {
                    callback.run();
                    return 7;
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    switch (kind) {
                        case 0 -> JniArrayReferences.withArray(table, left, callback);
                        case 1 -> JniArrayReferences.withCritical(table, left, callback);
                        case 2 -> JniArrayReferences.withConcat(table, left, right, callback);
                        case 3 -> JniArrayReferences.withConcat(table, left, right, supplier);
                        case 4 -> JniArrayReferences.withArrayBatch(table, new long[] {left}, callback);
                        case 5 -> JniArrayReferences.withArrayBatch(table, new long[] {left}, supplier);
                        default -> throw new AssertionError("unexpected variant");
                    }
                });
                assertSame(primary, actual);
                assertEquals(1, actual.getSuppressed().length);
                assertInstanceOf(IllegalStateException.class, actual.getSuppressed()[0]);
                assertEquals(0, table.jniArrayBorrowCount(left));
                assertEquals(0, table.jniArrayBorrowCount(right));
            }
        }
    }

    @Test
    void checkedNativeThrowableWithoutCleanupFailurePassesThroughUnchanged() {
        try (var table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new byte[4]);
            IOException primary = new IOException("native checked exception");
            IOException actual = assertThrows(IOException.class, () ->
                    JniArrayReferences.withArray(table, id, () -> nativeChecked(primary)));
            assertSame(primary, actual);
            assertEquals(0, actual.getSuppressed().length);
            assertEquals(0, table.jniArrayBorrowCount(id));
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwNative(Throwable primary) throws E {
        throw (E) primary;
    }

    private static void nativeChecked(Throwable primary) {
        JniArrayReferencesTest.<RuntimeException>throwNative(primary);
    }

    @Test
    void reverseReleasePreservesFirstAndSecondCleanupFailures() {
        try (var table = new ScopedLeaseTable()) {
            long left = table.allocate();
            long right = table.allocate();
            table.registerJniArray(left, new byte[4]);
            table.registerJniArray(right, new byte[4]);
            JniArrayReferences.retainSources(table, left, right);
            table.endJniArrayAccess(right);
            table.endJniArrayAccess(left);
            IllegalStateException first = assertThrows(IllegalStateException.class,
                    () -> JniArrayReferences.releaseSources(table, left, right));
            assertTrue(first.getMessage().contains(" " + right));
            assertEquals(1, first.getSuppressed().length);
            assertInstanceOf(IllegalStateException.class, first.getSuppressed()[0]);
            assertTrue(first.getSuppressed()[0].getMessage().contains(" " + left));
            assertEquals(0, table.jniArrayBorrowCount(left));
            assertEquals(0, table.jniArrayBorrowCount(right));
        }
    }

    @Test
    void checkedNativeFailureRetainsBothReverseCleanupErrors() {
        for (boolean supplier : new boolean[] {false, true}) {
            try (var table = new ScopedLeaseTable()) {
                long left = table.allocate();
                long right = table.allocate();
                table.registerJniArray(left, new byte[4]);
                table.registerJniArray(right, new byte[4]);
                IOException primary = new IOException("native checked failure");
                Runnable callback = () -> {
                    table.endJniArrayAccess(right);
                    table.endJniArrayAccess(left);
                    nativeChecked(primary);
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    if (supplier) {
                        JniArrayReferences.withConcat(table, left, right, () -> {
                            callback.run();
                            return 7;
                        });
                    } else {
                        JniArrayReferences.withConcat(table, left, right, callback);
                    }
                });
                assertSame(primary, actual);
                assertEquals(1, actual.getSuppressed().length);
                Throwable first = actual.getSuppressed()[0];
                assertInstanceOf(IllegalStateException.class, first);
                assertTrue(first.getMessage().contains(" " + right));
                assertEquals(1, first.getSuppressed().length);
                assertInstanceOf(IllegalStateException.class, first.getSuppressed()[0]);
                assertTrue(first.getSuppressed()[0].getMessage().contains(" " + left));
                assertEquals(0, table.jniArrayBorrowCount(left));
                assertEquals(0, table.jniArrayBorrowCount(right));
            }
        }
    }
}
