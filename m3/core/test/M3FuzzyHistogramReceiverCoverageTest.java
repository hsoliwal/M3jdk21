/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fail-closed custody gate for the fuzzy position-mask receiver metadata.
 */
public final class M3FuzzyHistogramReceiverCoverageTest {
    private M3FuzzyHistogramReceiverCoverageTest() {
    }

    public static void main(String[] args) throws Exception {
        Path map = Path.of("m3", "lexicon", "synexia-fuzzy-histogram-target-map-10126.tsv");
        Path receipt = Path.of(
                "m3", "lexicon", "synexia-fuzzy-histogram-receiver-receipt-10126.tsv");
        String mapText = Files.readString(map, StandardCharsets.UTF_8);
        String receiptText = Files.readString(receipt, StandardCharsets.UTF_8);
        int checks = 0;
        check(mapText.contains("synexia_pr\t10126"), "source PR");
        checks++;
        check(mapText.contains("synexia_head\t7d005c8488a1dcfbf4ca30ea43d55764d8c6d765"),
                "source head");
        checks++;
        check(mapText.contains("target_owner\tcom.m3.text.M3PositionMaskFacts"),
                "receiver owner");
        checks++;
        check(mapText.contains(
                "precompute_fields\trow_count,block_count,entry_count,primitive_payload_bytes,"
                        + "root_hash,auto_native_histogram_max_rows,native_slab_rows,source_revision"),
                "field map");
        checks++;
        check(mapText.contains("source_payload_policy\tNO_MASK_IMAGE_BYTES_COPIED"),
                "payload policy");
        checks++;
        check(receiptText.contains("receiver_blob\t85e3b64f1bdc6589aae24480778354149b06ee46"),
                "receiver blob");
        checks++;
        check(receiptText.contains("receiver_test_blob\t70abf688071709c5ab49eb5ac48fb3df5e731976"),
                "receiver test blob");
        checks++;
        check(receiptText.contains("mapping_blob\t4019891ac797b75d2b2a3753d30bf6006e01a9dd"),
                "mapping blob");
        checks++;
        check(receiptText.contains("status\tTARGET_METADATA_CONTRACT_ONLY"),
                "status");
        checks++;
        check(receiptText.contains("source_payload\tNOT_COPIED"), "receipt payload");
        checks++;
        System.out.println("M3_FUZZY_HISTOGRAM_RECEIVER_COVERAGE_PASS checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
