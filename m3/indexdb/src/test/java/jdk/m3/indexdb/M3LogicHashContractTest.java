// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the relocated {@link M3LogicHashContract} (the six cpu_lhash.py self_test invariants, then H1..H5 per file)
 * against the committed fixture oracle: {@code hash-family/LHASH_FIXTURES.tsv} holds every fixture input as
 * {@code path, byte length, base64 bytes} ({@code -} for the empty input; ASCII rows, so no checkout line-ending rule
 * can change an input, and no trailing whitespace), and
 * {@code hash-family/LHASH_ORACLE.tsv} holds the rows the LOCKED Python reference wrote for them (crate
 * {@code tools/oracle/oracle_lhash.py} + {@code cpu_lhash.py}; differential.sh regenerates both byte for byte).
 * The inputs are written to a temporary root first; every row must match.
 */
//FLLDCIM: CONTRACT - JUnit 5 wrapper of the relocated M3LogicHashContract (recipe m3/indexdb/recipe/hash-family-20261106)
final class M3LogicHashContractTest {
    @Test void pythonOracleRowsOfTheFixtureInputs(@TempDir Path root) throws Exception {
        Path fixtures = Path.of(M3LogicHashContractTest.class.getResource("hash-family/LHASH_FIXTURES.tsv").toURI());
        Path oracle = Path.of(M3LogicHashContractTest.class.getResource("hash-family/LHASH_ORACLE.tsv").toURI());
        List<String> rows = Files.readAllLines(fixtures, StandardCharsets.US_ASCII);
        List<String> inputs = rows.subList(1, rows.size());
        for (String row : inputs) {
            String[] cells = row.split("\t", -1);
            byte[] bytes = cells[2].equals("-") ? new byte[0] : Base64.getDecoder().decode(cells[2]);
            if (bytes.length != Integer.parseInt(cells[1])) throw new AssertionError("fixture length " + cells[0]);
            Files.write(root.resolve(cells[0]), bytes);
        }
        M3LogicHashContract.main(new String[] {"--root=" + root, "--oracle=" + oracle, "--min=" + inputs.size()});
    }
}
