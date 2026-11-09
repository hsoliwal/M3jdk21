// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import org.junit.jupiter.api.Test;

/** Runs the native parity probe when m3.lz.library names a built library (-Pnative-linux); prints SKIPPED otherwise. */
final class LzNativeParityProbeTest {
    @Test
    void jniAndJavaPrefixKernelsAgree() throws Exception {
        System.out.print(LzNativeParityProbe.runIfConfigured());
    }
}
