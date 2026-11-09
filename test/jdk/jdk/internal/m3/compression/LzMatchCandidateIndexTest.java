// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzMatchCandidateIndexTest.java blob a2f08f6f7812; recipe m3/compression/recipe/compression-20261106/relocate.py v3
final class LzMatchCandidateIndexTest {
    @Test
    void primitiveWindowIndexContract() {
        LzMatchCandidateSelfTest.runAll();
    }
}
