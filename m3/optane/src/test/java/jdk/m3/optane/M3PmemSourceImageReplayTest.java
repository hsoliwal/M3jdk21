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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate 2 of recipe/pmem-source-image-20261106 on the Java/FILE path: the relocated source-image family replays the
 * source-path index, image, refusal and slice lanes and 10,000 seeded operations through {@link M3PmemSourceImageDump}
 * without the native library; the dump holds no unexpected refusal class, no path-index mismatch against
 * Arrays.binarySearch, no byte mismatch and no naive-search disagreement. Prints M3_PMEM_SOURCE_IMAGE_REPLAY_PASS with
 * the line count and the digest; the donor-vs-relocated equality of the same dump is gate 3 (differential.sh). On a
 * native host (the library loadable) it also runs the corpus-atom main oracle {@link M3PmemCorpusAtomUnitTest}.
 */
final class M3PmemSourceImageReplayTest {
    private static final int OPERATIONS = 10_000;
    /** The refusal classes of the contracts (and the filesystem's); a linkage or assertion error is never one. */
    private static final Set<String> CONTRACT_REFUSALS = Set.of("IllegalArgumentException", "IllegalStateException",
            "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException", "NullPointerException", "SecurityException",
            "IOException", "UncheckedIOException", "NoSuchFileException", "FileAlreadyExistsException",
            "FileSystemException", "InvalidPathException", "UnsupportedOperationException");
    /** A refusal in the dump: "!" + the exception's simple name (+ ": " + its erased message). */
    private static final Pattern REFUSAL = Pattern.compile("!([A-Za-z]+)");

    @Test
    void fileReplayHoldsOnlyContractRefusals() throws Exception {
        String dump = M3PmemSourceImageDump.replay("jdk.m3.optane", "M3", OPERATIONS, false);
        String[] lines = dump.split("\n");
        long refusals = 0;
        for (String line : lines) {
            Matcher refusal = REFUSAL.matcher(line);
            while (refusal.find()) {
                assertTrue(CONTRACT_REFUSALS.contains(refusal.group(1)), line);
                refusals++;
            }
            assertFalse(line.contains("MISMATCH") || line.contains("NAIVE=") || line.contains("DIFFERENT-ROOT"), line);
            if (line.startsWith("PATHINDEX ") && line.contains(" mismatches=")) assertTrue(line.contains(" mismatches=0 "), line);
        }
        assertEquals("RANDOM operations=" + OPERATIONS, lines[lines.length - 2]);
        assertTrue(lines[lines.length - 1].startsWith("TOTAL operations="), lines[lines.length - 1]);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(dump.getBytes(StandardCharsets.UTF_8));
        System.out.println("M3_PMEM_SOURCE_IMAGE_REPLAY_PASS operations=" + OPERATIONS + " lines=" + lines.length
                + " refusals=" + refusals + " sha256_16=" + HexFormat.of().formatHex(digest, 0, 8));
    }

    @Test
    void corpusAtomMainOracleOnNativeHosts() throws Exception {
        if (!M3PmemSdk.capabilities().nativeAvailable()) return;
        M3PmemCorpusAtomUnitTest.main(new String[0]);
    }
}
