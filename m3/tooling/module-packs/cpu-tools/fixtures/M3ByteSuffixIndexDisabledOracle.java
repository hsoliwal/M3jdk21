// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ByteSuffixIndex;
import jdk.m3.tools.cpu.M3ByteSuffixIndexNative;

/** Runs only in a separate JVM without any native path property. */
public final class M3ByteSuffixIndexDisabledOracle {
    public static void main(String[] args) {
        if (M3ByteSuffixIndexNative.isAvailable()) {
            throw new AssertionError("native backend unexpectedly loaded");
        }
        M3ByteSuffixIndex owner = M3ByteSuffixIndex.copyOf(new byte[]{1,2,3});
        try {
            M3ByteSuffixIndexNative.queryBatch(owner,new byte[]{2},new int[]{0,1});
            throw new AssertionError("disabled native query succeeded");
        } catch (IllegalStateException expected) {
            if (!"suffix native backend not configured".equals(expected.getMessage())) {
                throw new AssertionError("changed disabled-native contract", expected);
            }
        }
        try {
            M3ByteSuffixIndexNative.queryBatch(owner,new byte[]{2},new int[]{1,1});
            throw new AssertionError("invalid query was accepted before checking native");
        } catch (IllegalArgumentException expected) {
            // Validation must precede the native-availability gate.
        }
        System.out.println("M3_BYTE_SUFFIX_DISABLED_PASS checks=3");
    }
}
