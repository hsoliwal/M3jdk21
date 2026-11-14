// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate 2 of recipe/pmem-region-core-20261106 on the Java/FILE path: the relocated family replays the FILE oracle lanes
 * and 10,000 seeded operations through {@link PmemDump} without the native library, and the dump holds no
 * unexpected refusal class (every refusal is one of the contract's five). Prints M3_PMEM_FILE_REPLAY_PASS with the
 * line count and the digest; the donor-vs-relocated equality of the same dump is gate 3 (differential.sh).
 */
final class PmemFileReplayTest {
    private static final int OPERATIONS = 10_000;
    /** The refusal classes of the contract (and the filesystem's); a linkage or assertion error is never one. */
    private static final Set<String> CONTRACT_REFUSALS = Set.of("IllegalArgumentException", "IllegalStateException",
            "IndexOutOfBoundsException", "NullPointerException", "UnsupportedOperationException", "IOException",
            "UncheckedIOException", "NoSuchFileException", "AccessDeniedException", "FileSystemException",
            "FileAlreadyExistsException");
    /** A refusal in the dump: "!" + the exception's simple name (+ ": " + its erased message). */
    private static final Pattern REFUSAL = Pattern.compile("!([A-Za-z]+)");

    @Test
    void fileReplayHoldsOnlyContractRefusals() throws Exception {
        String dump = PmemDump.replay("jdk.m3.optane", "", "jdk.m3.optane", OPERATIONS, false);
        String[] lines = dump.split("\n");
        long refusals = 0;
        for (String line : lines) {
            Matcher refusal = REFUSAL.matcher(line);
            while (refusal.find()) {
                assertTrue(CONTRACT_REFUSALS.contains(refusal.group(1)), line);
                refusals++;
            }
        }
        assertEquals("RANDOM operations=" + OPERATIONS, lines[lines.length - 2]);
        assertTrue(lines[lines.length - 1].startsWith("TOTAL operations="), lines[lines.length - 1]);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(dump.getBytes(StandardCharsets.UTF_8));
        System.out.println("M3_PMEM_FILE_REPLAY_PASS operations=" + OPERATIONS + " lines=" + lines.length + " refusals="
                + refusals + " sha256_16=" + HexFormat.of().formatHex(digest, 0, 8));
    }
}
