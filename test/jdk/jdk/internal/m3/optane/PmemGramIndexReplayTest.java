// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate 2 of recipe/pmem-gram-index-20261106 on the Java/FILE path: the relocated gram index, selected search and corpus
 * search replay the build, query, refusal, corpus and drift lanes and 2,000 seeded queries through
 * {@link PmemGramIndexDump} without the native library; every gram-filtered result equals the exact selected search
 * and the naive search (no UNEXPECTED line) and every refusal is a contract class. Prints
 * M3_PMEM_GRAM_INDEX_REPLAY_PASS with the line count and the digest; the donor-vs-relocated equality of the same dump is
 * gate 3 (differential.sh).
 */
final class PmemGramIndexReplayTest {
    private static final int OPERATIONS = 2_000;
    /** The refusal classes of the contracts; a linkage or assertion error is never one. */
    private static final Set<String> CONTRACT_REFUSALS = Set.of("IllegalArgumentException", "IllegalStateException",
            "IndexOutOfBoundsException", "NullPointerException", "SecurityException", "CancellationException",
            "IOException", "UncheckedIOException");
    /** A refusal in the dump: "!" + the exception's simple name (+ ": " + its erased message). */
    private static final Pattern REFUSAL = Pattern.compile("!([A-Za-z]+)");

    @Test
    void fileReplayEqualsExactSearchAndHoldsOnlyContractRefusals() throws Exception {
        String dump = PmemGramIndexDump.replay("jdk.m3.optane", "jdk.internal.m3.optane", "", OPERATIONS, false);
        String[] lines = dump.split("\n");
        long refusals = 0;
        long equal = 0;
        for (String line : lines) {
            Matcher refusal = REFUSAL.matcher(line);
            while (refusal.find()) {
                assertTrue(CONTRACT_REFUSALS.contains(refusal.group(1)), line);
                refusals++;
            }
            assertFalse(line.contains("UNEXPECTED"), line);
            if (line.contains(" EQUAL ORACLE")) equal++;
        }
        assertTrue(lines[lines.length - 1].startsWith("TOTAL operations="), lines[lines.length - 1]);
        assertEquals(1, dump.lines().filter(l -> l.equals("RANDOM operations=" + OPERATIONS)).count());
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(dump.getBytes(StandardCharsets.UTF_8));
        System.out.println("M3_PMEM_GRAM_INDEX_REPLAY_PASS operations=" + OPERATIONS + " lines=" + lines.length
                + " exact_equal=" + equal + " refusals=" + refusals + " sha256_16=" + HexFormat.of().formatHex(digest, 0, 8));
    }
}
