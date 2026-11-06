// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import org.junit.jupiter.api.Test;

/** Runs the reference-model oracle of the chunk cache (5 configurations x 40,000 seeded operations). */
final class M3CompressedChunkCacheModelTest {
    @Test
    void cacheAgreesWithTheLinkedHashMapModel() throws Throwable {
        System.out.print(M3CompressedChunkCacheModel.runAll());
    }
}
