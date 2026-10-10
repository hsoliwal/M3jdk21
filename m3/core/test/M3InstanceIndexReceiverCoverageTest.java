/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Receipt-bound coverage gate for proper-name/title metadata admission. */
public final class M3InstanceIndexReceiverCoverageTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) throws Exception {
        String targetMap = read("lexicon/synexia-instance-target-map.tsv");
        String receipt = read("lexicon/synexia-instance-target-receiver-receipt.tsv");
        String foss = read("lexicon/FOSS_REUSE_DECISION.tsv");

        String proper = row(targetMap, "proper-nouns\t");
        String titles = row(targetMap, "titles\t");
        check(proper.contains("\tcom.m3.text.M3InstanceIndex\t"));
        check(titles.contains("\tcom.m3.text.M3InstanceIndex\t"));
        check(proper.contains("TARGET_METADATA_CONTRACT_PROVEN"));
        check(titles.contains("TARGET_METADATA_CONTRACT_PROVEN"));
        check(proper.contains("REFERENCE_ONLY_PAYLOAD_NOT_ADMITTED"));
        check(titles.contains("REFERENCE_ONLY_PAYLOAD_NOT_ADMITTED"));
        check(proper.contains("instanceOfX"));
        check(proper.contains("PrecomputeFacts"));
        check(titles.contains("TitleRecord.identity"));
        check(titles.contains("PrecomputeFacts"));

        check(receipt.contains("schema\tM3JDK_INSTANCE_TARGET_RECEIVER_RECEIPT_V1"));
        check(receipt.contains("source_ref\tdevelop"));
        check(receipt.contains("source_blob\tb64b5ef0033d7bf64ef7b3a6d291e7d6c9221630"));
        check(receipt.contains("receiver_path\tm3/core/src/com/m3/text/M3InstanceIndex.java"));
        check(receipt.contains("test_path\tm3/core/test/M3InstanceIndexTest.java"));
        check(receipt.contains("target_map_path\tm3/lexicon/synexia-instance-target-map.tsv"));
        check(receipt.contains("foss_decision_path\tm3/lexicon/FOSS_REUSE_DECISION.tsv"));
        check(receipt.contains("contract_status\tTARGET_METADATA_CONTRACT_PROVEN"));
        check(receipt.contains("payload_status\tREFERENCE_ONLY_PAYLOAD_NOT_ADMITTED"));
        check(receipt.contains("ownership\tSynexia source owner; M3JDK metadata receiver"));

        String decision = row(foss, "m3jdk-instance-metadata-receiver\t");
        check(decision.contains("\tNO_FIT\t"));
        check(decision.contains("No external donor"));

        check(Files.exists(Path.of("core", "src", "com", "m3", "text", "M3InstanceIndex.java")));
        check(Files.exists(Path.of("core", "test", "M3InstanceIndexTest.java")));

        System.out.println("M3_INSTANCE_INDEX_RECEIVER_COVERAGE_PASS checks=" + checks);
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static String row(String text, String prefix) {
        return Arrays.stream(text.split("\\R", -1))
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row: " + prefix));
    }
}
