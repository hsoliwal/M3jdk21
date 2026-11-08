// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzFourWayNativeInteropTest.java blob 5016af674383; recipe m3/compression/recipe/compression-20261106/relocate.py v3
final class M3LzFourWayNativeInteropTest {
    @Test
    void candidatePrefixDifferential() throws Exception {
        M3LzFourWayNativeInteropSelfTest.runAll();
    }
}
