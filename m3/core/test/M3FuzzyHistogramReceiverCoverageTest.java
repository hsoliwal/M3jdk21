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
        check(mapText.contains("synexia_head\tab4edcbd7eb475a3c9abb12d4a0fc14703a826ec"),
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
        check(receiptText.contains("receiver_blob\t53cf28f7e40f4f8760a8263c0c4cc670fb403188"),
                "receiver blob");
        checks++;
        check(receiptText.contains("receiver_test_blob\t77d056490e6ce8cee577aa1e4fcafbf6c5c4edd4"),
                "receiver test blob");
        checks++;
        check(receiptText.contains("mapping_blob\tc1aff08504d7c11541463d2b9925c081e8e4abd0"),
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
