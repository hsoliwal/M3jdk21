// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.acceptance;

import jdk.m3.arrays.ArrayNative;
import java.nio.file.Path;

/** Fresh-JVM oracle for a test provider that returns null without installing an exception. */
public final class PackedNullResultProbe {
    private PackedNullResultProbe() { }

    public static void main(String[] arguments) {
        if (arguments.length != 1) throw new IllegalArgumentException("exact test fixture library path required");
        ArrayNative.load(Path.of(arguments[0]));
        Runnable[] calls = {
            () -> ArrayNative.encodePackedRequired(new long[0], 40),
            () -> ArrayNative.decodePackedRequired(new byte[0], 40),
            () -> ArrayNative.encodeUnsigned64Required(new long[0]),
            () -> ArrayNative.decodeUnsigned64Required(new byte[0]),
            () -> ArrayNative.encodeZigZag64Required(new long[0]),
            () -> ArrayNative.decodeZigZag64Required(new byte[0])
        };
        for (Runnable call : calls) {
            try { call.run(); throw new AssertionError("null codec result escaped"); }
            catch (OutOfMemoryError expected) {
                if (!"packed codec result allocation failed".equals(expected.getMessage())) {
                    throw new AssertionError("wrong allocation failure", expected);
                }
            }
        }
        System.out.println("M3_PACKED_NULL_RESULT_PASS cases=6");
    }
}
