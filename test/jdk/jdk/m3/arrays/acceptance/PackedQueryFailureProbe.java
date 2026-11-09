// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.acceptance;
import jdk.m3.arrays.ArrayNative;
import java.nio.file.Path;

/** Fresh actual JVM using a provider that cannot install a pending exception. */
public final class PackedQueryFailureProbe {
    private PackedQueryFailureProbe() { }
    public static void main(String[] args) {
        ArrayNative.load(Path.of(args[0]));
        for (int mode = 0; mode < 2; mode++) {
            try {
                if (mode == 0) ArrayNative.countPackedInRangeRequired(new byte[0], 40, 0, 0, 0, 0);
                else ArrayNative.firstPackedInRangeRequired(new byte[0], 40, 0, 0, 0, 0);
                throw new AssertionError("silent native failure escaped");
            } catch (OutOfMemoryError expected) {
                if (!"packed query could not report native failure".equals(expected.getMessage()))
                    throw new AssertionError(expected);
            }
        }
        System.out.println("PACKED_QUERY_FAILURE_GUARD_PASS cases=2 actual_JVM=1");
    }
}
