// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Contract oracle: source bytes, lease balance, aliasing and JNI parity. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3JniByteConcatTest.java blob 098b1f6bc009; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
final class M3JniByteConcatTest {
    @Test
    void emptyUnequalAndAliasedSourcesProduceFreshArraysWithoutLeakedPins() {
        try (var owner = new M3ScopedLeaseTable()) {
            long leftId = owner.allocate();
            long rightId = owner.allocate();
            byte[] left = { 9, -1, 0 };
            byte[] right = { 1, 2 };
            owner.registerJniArray(leftId, left);
            owner.registerJniArray(rightId, right);

            byte[] result = M3JniByteConcat.concat(owner, leftId, left,
                    rightId, right, false);
            assertArrayEquals(new byte[]{ 9, -1, 0, 1, 2 }, result);
            assertNotSame(result, left);
            assertNotSame(result, right);
            assertEquals(0, owner.jniArrayBorrowCount(leftId));
            assertEquals(0, owner.jniArrayBorrowCount(rightId));
            assertEquals(0, owner.pinCount(leftId));
            assertEquals(0, owner.pinCount(rightId));

            byte[] repeated = M3JniByteConcat.concat(owner, leftId, left,
                    leftId, left, false);
            assertArrayEquals(new byte[]{ 9, -1, 0, 9, -1, 0 }, repeated);
            assertEquals(0, owner.jniArrayBorrowCount(leftId));
            assertTrue(owner.eligible(leftId));
        }

        try (var owner = new M3ScopedLeaseTable()) {
            long id = owner.allocate();
            byte[] empty = new byte[0];
            owner.registerJniArray(id, empty);
            byte[] copy = M3JniByteConcat.concat(owner, id, empty, id, empty, true);
            assertEquals(0, copy.length);
            assertNotSame(empty, copy);
            assertEquals(0, owner.pinCount(id));
        }
    }

    @Test
    void nestedScopeBlocksReclamationEvenAfterInnerCall() {
        try (var owner = new M3ScopedLeaseTable()) {
            long id = owner.allocate();
            byte[] data = { 3, 5, 8 };
            owner.registerJniArray(id, data);
            M3JniArrayReferences.withArray(owner, id, () -> {
                assertEquals(1, owner.jniArrayBorrowCount(id));
                byte[] copy = M3JniByteConcat.concat(owner, id, data, id, data, true);
                assertArrayEquals(new byte[]{ 3, 5, 8, 3, 5, 8 }, copy);
                assertEquals(1, owner.jniArrayBorrowCount(id));
                assertFalse(owner.tryRetire(id));
            });
            assertEquals(0, owner.jniArrayBorrowCount(id));
            assertTrue(owner.tryRetire(id));
        }
    }

    @Test
    void refuseUnknownRetiredOrNullInputWithoutLeakingFirstBorrow() {
        try (var owner = new M3ScopedLeaseTable()) {
            long valid = owner.allocate();
            long retired = owner.allocate();
            byte[] input = { 42 };
            owner.registerJniArray(valid, input);
            assertTrue(owner.tryRetire(retired));
            assertThrows(IllegalStateException.class,
                    () -> M3JniByteConcat.concat(owner, valid, input,
                            retired, input, true));
            assertEquals(0, owner.jniArrayBorrowCount(valid));
            assertEquals(0, owner.pinCount(valid));
            assertThrows(NullPointerException.class,
                    () -> M3JniByteConcat.concat(owner, valid, input,
                            valid, null, false));
            assertEquals(0, owner.jniArrayBorrowCount(valid));
        }
    }

    @Test
    void deterministicDifferentialJavaAndOptionalNativePaths() {
        Random random = new Random(0x4d334a4e494cL);
        try (var owner = new M3ScopedLeaseTable()) {
            for (int i = 0; i < 128; i++) {
                int n = i % 33 == 0 ? 1025 + i : i % 129;
                int m = i % 19 == 0 ? 4097 + i : (127 - i) % 71;
                byte[] left = new byte[n];
                byte[] right = new byte[m];
                random.nextBytes(left);
                random.nextBytes(right);
                long a = owner.allocate();
                long b = owner.allocate();
                owner.registerJniArray(a, left);
                owner.registerJniArray(b, right);
                byte[] expected = Arrays.copyOf(left, n + m);
                System.arraycopy(right, 0, expected, n, m);
                assertArrayEquals(expected,
                        M3JniByteConcat.concat(owner, a, left, b, right, false));
                assertArrayEquals(expected,
                        M3JniByteConcat.concat(owner, a, left, b, right, true));
                assertEquals(0, owner.jniArrayBorrowCount(a));
                assertEquals(0, owner.jniArrayBorrowCount(b));
            }
            assertEquals(0, owner.activeJniArrayCount());
        }
    }

    @Test
    void nativeRegionChunkSeamsPreserveSourceBytesAndRegisteredBorrowBalance() {
        Random random = new Random(0x4d334a4e4943524cL);
        int[] lengths = { 0, 1, 2, 127, 128, 129, 1023, 1024, 1025,
                4095, 4096, 4097, 8191, 8192, 8193, 65535, 65536, 65537 };
        try (var owner = new M3ScopedLeaseTable()) {
            for (int length : lengths) {
                byte[] left = new byte[length];
                byte[] right = new byte[length / 2];
                random.nextBytes(left);
                random.nextBytes(right);
                byte[] leftBefore = left.clone();
                byte[] rightBefore = right.clone();
                long leftId = owner.allocate();
                long rightId = owner.allocate();
                owner.registerJniArray(leftId, left);
                owner.registerJniArray(rightId, right);
                byte[] expected = Arrays.copyOf(left, left.length + right.length);
                System.arraycopy(right, 0, expected, left.length, right.length);
                byte[] javaCopy = M3JniByteConcat.concat(owner, leftId, left,
                        rightId, right, false);
                byte[] nativeCopy = M3JniByteConcat.concat(owner, leftId, left,
                        rightId, right, true);
                assertArrayEquals(expected, javaCopy);
                assertArrayEquals(expected, nativeCopy);
                assertNotSame(nativeCopy, left);
                assertNotSame(nativeCopy, right);
                assertNotSame(nativeCopy, javaCopy);

                byte[] aliasExpected = Arrays.copyOf(left, left.length * 2);
                System.arraycopy(left, 0, aliasExpected, left.length, left.length);
                byte[] aliasCopy = M3JniByteConcat.concat(owner, leftId, left,
                        leftId, left, true);
                assertArrayEquals(aliasExpected, aliasCopy);
                assertArrayEquals(leftBefore, left);
                assertArrayEquals(rightBefore, right);
                assertEquals(0, owner.jniArrayBorrowCount(leftId));
                assertEquals(0, owner.jniArrayBorrowCount(rightId));
                assertEquals(0, owner.pinCount(leftId));
                assertEquals(0, owner.pinCount(rightId));
            }
            assertEquals(0, owner.activeJniArrayCount());
        }
    }

    /**
     * Negative and recovery oracle for the private JNI symbol, not a public bypass
     * of registered lease ownership. Reflection is used only inside the test:
     * arrays remain live Java arguments until each native call returns.
     */
    @Test
    void directNativeEntryNullFailureDoesNotPoisonLaterCalls()
            throws ReflectiveOperationException {
        if (Boolean.getBoolean("jdk.m3.jni.requireNative")) {
            assertTrue(M3JniByteConcat.nativeLibraryLoaded(),
                    "native-required direct JNI boundary must load the library");
        } else {
            Assumptions.assumeTrue(M3JniByteConcat.nativeLibraryLoaded(),
                    "optional JNI library unavailable");
        }
        Method nativeCopy = M3JniByteConcat.class.getDeclaredMethod(
                "concatNative", byte[].class, byte[].class);
        nativeCopy.setAccessible(true);

        InvocationTargetException leftFailure = assertThrows(
                InvocationTargetException.class,
                () -> nativeCopy.invoke(null, (Object) null, (Object) new byte[0]));
        assertInstanceOf(NullPointerException.class, leftFailure.getCause());
        InvocationTargetException rightFailure = assertThrows(
                InvocationTargetException.class,
                () -> nativeCopy.invoke(null, (Object) new byte[0], (Object) null));
        assertInstanceOf(NullPointerException.class, rightFailure.getCause());

        Random random = new Random(0x4d334e4154495645L);
        for (int size : new int[] { 0, 1, 128, 4095, 4096, 4097, 8193 }) {
            byte[] left = new byte[size];
            byte[] right = new byte[size / 3];
            random.nextBytes(left);
            random.nextBytes(right);
            byte[] leftBefore = left.clone();
            byte[] rightBefore = right.clone();
            byte[] expected = Arrays.copyOf(left, left.length + right.length);
            System.arraycopy(right, 0, expected, left.length, right.length);
            byte[] result = (byte[]) nativeCopy.invoke(null, (Object) left, (Object) right);
            assertArrayEquals(expected, result);
            assertNotSame(left, result);
            assertNotSame(right, result);
            assertArrayEquals(leftBefore, left);
            assertArrayEquals(rightBefore, right);

            byte[] alias = (byte[]) nativeCopy.invoke(null, (Object) left, (Object) left);
            byte[] doubled = Arrays.copyOf(left, left.length * 2);
            System.arraycopy(left, 0, doubled, left.length, left.length);
            assertArrayEquals(doubled, alias);
            assertNotSame(left, alias);
        }
    }

    @Test
    void actualNativeRequiredGateDoesNotSilentlyPassWithoutLibrary() {
        if (Boolean.getBoolean("jdk.m3.jni.requireNative")) {
            assertTrue(M3JniByteConcat.nativeLibraryLoaded(),
                    "native-required run must link the JNI owner library");
        } else {
            Assumptions.assumeTrue(M3JniByteConcat.nativeLibraryLoaded(),
                    "optional JNI byte-concatenation library unavailable");
        }
        try (var owner = new M3ScopedLeaseTable()) {
            long id = owner.allocate();
            byte[] source = { 1, 2, 3 };
            owner.registerJniArray(id, source);
            assertArrayEquals(new byte[]{ 1, 2, 3, 1, 2, 3 },
                    M3JniByteConcat.concat(owner, id, source, id, source, true));
            assertEquals(0, owner.jniArrayBorrowCount(id));
        }
    }
}
