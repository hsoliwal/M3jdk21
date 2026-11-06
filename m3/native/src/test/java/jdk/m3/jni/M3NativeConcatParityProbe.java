// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;
import java.util.Random;

/** Original classes and JNI C17 library required; no stub or alternate owner. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/crates/m3-doop-pool-native-c17-20261031/fixtures/M3NativeConcatParityProbe.java blob d63a91a2c7a9; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3NativeConcatParityProbe {
    private M3NativeConcatParityProbe() { }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(M3NativeLeaseScan.nativeAvailable(), "actual JNI scan library must load");
        check(M3JniByteConcat.nativeLibraryLoaded(), "actual JNI byte concat library must load");
        Random random = new Random(0x4D334A4E494CL);
        int cases = 0;
        for (int n = 0; n < 1024; n++) {
            try (var table = new M3ScopedLeaseTable()) {
                long leftId = table.allocate();
                long rightId = table.allocate();
                int leftLen = n % 17 == 0 ? n + 1031 : n % 102;
                int rightLen = n % 19 == 0 ? n + 4103 : n % 89;
                byte[] left = new byte[leftLen], right = new byte[rightLen];
                random.nextBytes(left);
                random.nextBytes(right);
                table.registerJniArray(leftId, left);
                table.registerJniArray(rightId, right);
                byte[] expected = Arrays.copyOf(left, leftLen + rightLen);
                System.arraycopy(right, 0, expected, leftLen, rightLen);
                byte[] actualJava = M3JniByteConcat.concat(table, leftId, left,
                        rightId, right, false);
                byte[] actualNative = M3JniByteConcat.concat(table, leftId, left,
                        rightId, right, true);
                check(Arrays.equals(actualJava, expected), "Java concat mismatch n=" + n);
                check(Arrays.equals(actualNative, expected), "native concat mismatch n=" + n);
                check(actualNative != left && actualNative != right, "output aliases input");
                check(table.jniArrayBorrowCount(leftId) == 0
                        && table.jniArrayBorrowCount(rightId) == 0
                        && table.pinCount(leftId) == 0 && table.pinCount(rightId) == 0,
                        "array lease leaked n=" + n);
                check(table.eligible(leftId) && table.eligible(rightId),
                        "eligible status mismatch n=" + n);
                cases++;
            }
        }
        try (var table = new M3ScopedLeaseTable()) {
            long id = table.allocate();
            byte[] array = { 9, -1, 0, 120 };
            table.registerJniArray(id, array);
            byte[] duplicate = M3JniByteConcat.concat(table, id, array, id, array, true);
            check(Arrays.equals(duplicate, new byte[] { 9, -1, 0, 120, 9, -1, 0, 120 }),
                    "duplicate JNI array source result incorrect");
            check(table.jniArrayBorrowCount(id) == 0 && table.pinCount(id) == 0,
                    "duplicate JNI source leaked leases");
            cases++;
        }
        System.out.println("M3_NATIVE_CONCAT_PARITY_PASS cases=" + cases + " native=true");
    }
}
