// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.acceptance;

import com.m3.arrays.M3ArrayNative;
import java.nio.file.Path;

/** Fresh-JVM oracle for a test provider that returns null without installing an exception. */
public final class PackedNullResultProbe {
    private PackedNullResultProbe() { }

    public static void main(String[] arguments) {
        if (arguments.length != 1) throw new IllegalArgumentException("exact test fixture library path required");
        M3ArrayNative.load(Path.of(arguments[0]));
        Runnable[] calls = {
            () -> M3ArrayNative.encodePackedRequired(new long[0], 40),
            () -> M3ArrayNative.decodePackedRequired(new byte[0], 40),
            () -> M3ArrayNative.encodeUnsigned64Required(new long[0]),
            () -> M3ArrayNative.decodeUnsigned64Required(new byte[0]),
            () -> M3ArrayNative.encodeZigZag64Required(new long[0]),
            () -> M3ArrayNative.decodeZigZag64Required(new byte[0])
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
