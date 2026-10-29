// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.acceptance;

import com.m3.arrays.M3ArrayNative;
import java.nio.file.Path;
import java.util.Arrays;

/** Fresh-process loading/old-library ABI probe; never hides a missing native codec. */
public final class PackedLoadContractProbe {
    private PackedLoadContractProbe() { }

    public static void main(String[] arguments) {
        if (arguments.length > 1) throw new IllegalArgumentException("optional exact baseline library path");
        System.clearProperty("m3.arrays.native.path");
        if (M3ArrayNative.isLoaded()) throw new AssertionError("fresh process required");
        if (arguments.length == 1) M3ArrayNative.load(Path.of(arguments[0]));
        if (M3ArrayNative.compareUtf16(new char[] {'a'}, 0, new char[] {'b'}, 0, 1) != -1) {
            throw new AssertionError("old compare behavior");
        }
        if (!Arrays.equals(new char[] {'a', 'b'}, M3ArrayNative.concatenateUtf16(new char[] {'a'}, new char[] {'b'}))) {
            throw new AssertionError("old concatenate behavior");
        }
        Runnable[] newCalls = {
            () -> M3ArrayNative.encodePackedRequired(new long[0], 40),
            () -> M3ArrayNative.decodePackedRequired(new byte[0], 40),
            () -> M3ArrayNative.encodeUnsigned64Required(new long[0]),
            () -> M3ArrayNative.decodeUnsigned64Required(new byte[0]),
            () -> M3ArrayNative.encodeZigZag64Required(new long[0]),
            () -> M3ArrayNative.decodeZigZag64Required(new byte[0])
        };
        for (Runnable call : newCalls) {
            try { call.run(); throw new AssertionError("required native operation silently substituted"); }
            catch (UnsatisfiedLinkError expected) { }
        }
        if (arguments.length == 1) {
            if (M3ArrayNative.allocateObjectsRequired(1).length != 1
                    || M3ArrayNative.allocateBytesRequired(1)[0] != 0
                    || M3ArrayNative.allocateIntsRequired(1)[0] != 0
                    || M3ArrayNative.allocateLongsRequired(1)[0] != 0) {
                throw new AssertionError("old required-native allocations");
            }
        }
        System.out.println("M3_PACKED_LOADING_PASS mode=" + (arguments.length == 0 ? "missing" : "baseline-library")
                + " newCallsRefused=6");
    }
}
