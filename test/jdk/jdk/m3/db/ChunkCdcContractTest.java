// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the relocated {@link ChunkCdcContract} over a fixed corpus, the two committed hash-family fixture tables
 * (about 138 KB, regenerated only by the recipe), concatenated by the contract into one stream, and over its seeded
 * 1 MiB random stream: coverage, determinism, re-synchronisation after an insertion, per-chunk SHA-256 and refused
 * parameters.
 */
//FLLDCIM: CONTRACT - JUnit 5 wrapper of the relocated M3ChunkCdcContract (recipe m3/indexdb/recipe/hash-family-20261106)
final class ChunkCdcContractTest {
    @Test void boundariesCoverageStabilityAndDigests(@TempDir Path scratch) throws Exception {
        Path root = Path.of(ChunkCdcContractTest.class.getResource("hash-family/LHASH_FIXTURES.tsv").toURI()).getParent();
        Path list = Files.write(scratch.resolve("corpus.txt"), List.of("LHASH_FIXTURES.tsv", "LHASH_ORACLE.tsv"),
                StandardCharsets.UTF_8);
        ChunkCdcContract.main(new String[] {"--root=" + root, "--list=" + list});
    }
}
