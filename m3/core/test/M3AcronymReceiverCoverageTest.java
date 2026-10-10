/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Receipt-bound gate for the Synexia acronym sidecar receiver. */
public final class M3AcronymReceiverCoverageTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) throws Exception {
        String targetMap = read("lexicon/synexia-acronym-target-map.tsv");
        String snapshot = read("lexicon/synexia-acronyms.tsv");
        String provenance = read("lexicon/synexia-acronym-provenance.json");
        String receipt = read("lexicon/synexia-acronym-target-receiver-receipt.tsv");

        String mapRow = row(targetMap, "dictlang.acronyms\t");
        check(mapRow.contains("\tM3LexiconPrecompute.AcronymPrecompute\t"));
        check(mapRow.contains("\tm3lex-acronym-v1\t"));
        check(mapRow.contains("\tSTAGED_PROVEN\t"));
        check(mapRow.contains("\tm3/lexicon/synexia-acronyms.tsv\t"));
        check(mapRow.contains("\tm3/lexicon/synexia-acronym-provenance.json\t"));
        check(mapRow.contains("\t4dd2d943eff28044362f843d9e42988e036d5e649910471949ae90666b29e27a\t40\t"));
        check(mapRow.contains("computing=12;data=6;java=7;networking=12;standards=3"));

        String[] snapshotRows = snapshot.split("\\R", -1);
        check(snapshotRows.length == 42);
        check(snapshotRows[0].equals(
                "source_id\trecord_id\tsource_manifest_revision\towner_fingerprint"
                        + "\tacronym\texpansion\tdomain"));
        Set<String> identities = new HashSet<>();
        for (int index = 1; index < snapshotRows.length - 1; index++) {
            String[] fields = snapshotRows[index].split("\\t", -1);
            check(fields.length == 7);
            check(fields[0].equals("dictlang.acronyms"));
            check(identities.add(fields[1]));
            check(fields[4].equals(fields[1]));
        }
        check(identities.size() == 40);

        check(provenance.contains(""source_commit": "9a990b710bf173258c29474083383d4c6b1e7d4e""));
        check(provenance.contains(""source_blob": "5bb16c13b6be693153a0656b8beb61af860cb52f""));
        check(provenance.contains(""snapshot_sha256": "4dd2d943eff28044362f843d9e42988e036d5e649910471949ae90666b29e27a""));
        check(provenance.contains(""record_count": 40"));
        check(provenance.contains(""status": "STAGED_PROVEN""));

        check(receipt.contains("schema\tM3JDK_ACRONYM_TARGET_RECEIVER_RECEIPT_V1"));
        check(receipt.contains("source_id\tdictlang.acronyms"));
        check(receipt.contains("source_blob\t5bb16c13b6be693153a0656b8beb61af860cb52f"));
        check(receipt.contains("target_map_blob\tc4985b3505a919271e7c1d6515d515bb70df6aa4"));
        check(receipt.contains("snapshot_blob\t15fc92ae35a32659768201a8c238a1b6b474d530"));
        check(receipt.contains("provenance_blob\t6f3ff19820782f55fafbefde99c9a47cbb833a62"));
        check(receipt.contains("receiver_owner\tM3LexiconPrecompute.AcronymPrecompute"));
        check(receipt.contains("catalog_owner\tAcronymSidecarCatalog"));
        check(receipt.contains("contract_status\tSTAGED_PROVEN_TYPED_RECEIVER"));
        check(receipt.contains("payload_status\tSOURCE_AUTHORED_SNAPSHOT_PINNED"));
        check(receipt.contains("hosted_status\tNOT_CLAIMED"));
        check(receipt.contains("source_foss_decision\thsoliwal/com.synexia:cognix-nlp/docs/FOSS_REUSE_DECISION.tsv:synexia-to-m3jdk-lexicon-map"));

        check(Files.exists(Path.of("core", "test", "M3AcronymPrecomputeTest.java")));
        check(Files.exists(Path.of("core", "test", "AcronymSidecarCatalogTest.java")));

        System.out.println("M3_ACRONYM_RECEIVER_COVERAGE_PASS checks=" + checks);
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
