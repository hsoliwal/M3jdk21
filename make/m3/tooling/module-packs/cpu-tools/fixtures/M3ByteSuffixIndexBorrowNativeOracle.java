// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Random;

/** Real opt-in JNI parity across repeated immutable-index borrows. */
public final class M3ByteSuffixIndexBorrowNativeOracle {
    private static long checks;

    private M3ByteSuffixIndexBorrowNativeOracle() { }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message + " check=" + checks);
    }

    public static void main(String[] args) {
        if (!M3ByteSuffixIndexNative.isAvailable()) {
            throw new AssertionError("opt-in native library must be configured for JNI gate");
        }
        Random random = new Random(0x29c17beL);
        for (int trial = 0; trial < 500; trial++) {
            byte[] data = new byte[random.nextInt(640)];
            random.nextBytes(data);
            byte[] snapshot = data.clone();
            M3ByteSuffixIndex index = M3ByteSuffixIndex.copyOf(data);
            byte[] packed = new byte[64];
            random.nextBytes(packed);
            // Four nonempty queries, plus zero-length queries with no JNI fallback.
            int[] offsets = {0, 0, 5, 18, 18, 37, 64};
            int[] expected = index.queryBatch(packed, offsets);
            int[] first = M3ByteSuffixIndexNative.queryBatch(index, packed, offsets);
            check(index.activeNativeArrayBorrowCount() == 0,
                    "native synchronous borrow released before return");
            check(Arrays.equals(expected, first), "Java/JNI parity");
            check(first != expected, "result buffers must not alias");
            first[0] = Integer.MIN_VALUE;
            check(Arrays.equals(expected,
                    M3ByteSuffixIndexNative.queryBatch(index, packed, offsets)),
                    "repeat native invocation after output mutation");
            check(index.activeNativeArrayBorrowCount() == 0,
                    "second native borrow released");
            check(Arrays.equals(index.bytes(), snapshot), "borrowed text immutable");
            check(index.suffixArray().length == snapshot.length,
                    "suffix offsets retained");
            if (data.length > 0) data[0] ^= 31;
            check(Arrays.equals(index.bytes(), snapshot), "caller input detached");
        }
        System.out.println("M3_SUFFIX_BORROW_JNI_PASS checks=" + checks);
    }
}
