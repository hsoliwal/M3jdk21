/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SynexiaPrecomputePayload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class LangDexProjectionReceiverTest {
    private static final String SOURCE_ID = "unicodex.langdex.lexemes";
    private static final String RECORD_ID = "70001";
    private static final String LEXEME = "water";
    private static final String LEGACY_PAYLOAD = "{\"langdex_concept_id\":701,"
            + "\"langdex_confidence_permille\":1000,"
            + "\"langdex_evidence_mask\":1073741825,"
            + "\"langdex_feature_bits\":2147483647,"
            + "\"langdex_flags\":65535,"
            + "\"langdex_frequency\":123456789,"
            + "\"langdex_lexical_class_mask\":4611686018427387905,"
            + "\"langdex_semantic_class_mask\":2305843009213693953,"
            + "\"langdex_subject_id\":17,"
            + "\"langdex_target_lexeme_id\":70002}";
    private static final String PAYLOAD = "{\"langdex_concept_id\":701,"
            + "\"langdex_confidence_permille\":1000,"
            + "\"langdex_evidence_mask\":1073741825,"
            + "\"langdex_feature_bits\":2147483647,"
            + "\"langdex_flags\":65535,"
            + "\"langdex_frequency\":123456789,"
            + "\"langdex_lexical_class_mask\":4611686018427387905,"
            + "\"langdex_lexical_profile_confidence_permille\":735,"
            + "\"langdex_lexical_profile_evidence_mask\":17,"
            + "\"langdex_lexical_profile_feature_bits\":4660,"
            + "\"langdex_lexical_profile_lexical_class_mask\":12,"
            + "\"langdex_semantic_class_mask\":2305843009213693953,"
            + "\"langdex_subject_id\":17,"
            + "\"langdex_target_lexeme_id\":70002}";
    private static final List<String> REQUIRED_FIELDS = List.of(
            "langdex_concept_id", "langdex_confidence_permille", "langdex_evidence_mask",
            "langdex_feature_bits", "langdex_flags", "langdex_frequency",
            "langdex_lexical_class_mask", "langdex_semantic_class_mask",
            "langdex_subject_id", "langdex_target_lexeme_id");
    private static final List<String> EXPECTED_FIELDS = List.of(
            "langdex_concept_id", "langdex_confidence_permille", "langdex_evidence_mask",
            "langdex_feature_bits", "langdex_flags", "langdex_frequency",
            "langdex_lexical_class_mask", "langdex_lexical_profile_confidence_permille",
            "langdex_lexical_profile_evidence_mask", "langdex_lexical_profile_feature_bits",
            "langdex_lexical_profile_lexical_class_mask", "langdex_semantic_class_mask",
            "langdex_subject_id", "langdex_target_lexeme_id");
    private static final Map<String, String> EXPECTED_TYPES = Map.ofEntries(
            Map.entry("langdex_concept_id", "long"),
            Map.entry("langdex_confidence_permille", "int"),
            Map.entry("langdex_evidence_mask", "int"),
            Map.entry("langdex_feature_bits", "int"),
            Map.entry("langdex_flags", "int"),
            Map.entry("langdex_frequency", "long"),
            Map.entry("langdex_lexical_class_mask", "long"),
            Map.entry("langdex_lexical_profile_confidence_permille", "int"),
            Map.entry("langdex_lexical_profile_evidence_mask", "int"),
            Map.entry("langdex_lexical_profile_feature_bits", "int"),
            Map.entry("langdex_lexical_profile_lexical_class_mask", "long"),
            Map.entry("langdex_semantic_class_mask", "long"),
            Map.entry("langdex_subject_id", "int"),
            Map.entry("langdex_target_lexeme_id", "long"));
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String sha256(Path path) throws Exception {
        return sha256(Files.readAllBytes(path));
    }

    private static String profileHash(String profile, long sourceRecords, long imageRecords)
            throws Exception {
        return sha256((profile + "\t" + sourceRecords + "\t" + imageRecords + "\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void writeImage(Path path) throws Exception {
        int payloadOffset = 64 + 12;
        ByteBuffer bytes = ByteBuffer.allocate(payloadOffset + LEXEME.length() * 2)
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, 0x4d334c4558303031L).putInt(8, 2).putInt(12, 1)
                .putLong(16, payloadOffset).putLong(24, LEXEME.length());
        bytes.putInt(64, 0).putInt(68, LEXEME.length());
        int hash = 0;
        for (int at = 0; at < LEXEME.length(); at++) {
            char unit = LEXEME.charAt(at);
            hash = 31 * hash + unit;
            int destination = payloadOffset + at * 2;
            bytes.put(destination, (byte) unit).put(destination + 1, (byte) (unit >>> 8));
        }
        bytes.putInt(72, hash);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.capacity() - 64);
        System.arraycopy(digest.digest(), 0, bytes.array(), 32, 32);
        Files.write(path, bytes.array(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static void verifyManifestAndFieldMap() throws Exception {
        List<String> manifest = Files.readAllLines(
                Path.of("lexicon", "synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        List<String> rows = manifest.stream()
                .filter(line -> line.startsWith(SOURCE_ID + "\t")).toList();
        check(rows.size() == 1);
        String[] row = rows.get(0).split("\t", -1);
        check(row.length == 9);
        check(row[2].equals("synexia-unicodex/docs/langdex/UPSTREAM_SCHEMA.tsv"));
        check(row[3].equals("lexeme_id"));
        check(row[4].equals("glottocode,lemma,source"));
        check(row[5].equals("M3StringFacts + LangDexCoordinate"));
        check(row[6].contains("CC-BY-SA-3.0"));
        check(row[7].equals("schema/source pin only unless the exact licensed snapshot and a lossless row-identity policy are supplied; concept-link rows are separate and unmapped by this lexeme entry"));
        check(List.of(row[8].split(",")).equals(REQUIRED_FIELDS));

        List<String> fieldMap = Files.readAllLines(
                Path.of("lexicon", "synexia-precompute-field-map.tsv"), StandardCharsets.UTF_8);
        Map<String, String> actualTypes = new java.util.TreeMap<>();
        Map<String, String> ownerTargets = new java.util.TreeMap<>();
        int mappedRows = 0;
        int targetLexemeRows = 0;
        for (String line : fieldMap) {
            if (!line.startsWith("LangDex")) continue;
            String[] columns = line.split("\t", -1);
            check(columns.length == 7);
            check(columns[4].equals("synexia.records.tsv:precompute_payload"));
            check(columns[5].equals("MAPPED"));
            check(!columns[3].equals("image_row"));
            String prior = actualTypes.putIfAbsent(columns[3], columns[2]);
            check(prior == null || prior.equals(columns[2]));
            String ownerField = columns[0] + "." + columns[1];
            String priorOwnerTarget = ownerTargets.putIfAbsent(ownerField, columns[3]);
            check(priorOwnerTarget == null || priorOwnerTarget.equals(columns[3]));
            if (columns[1].equals("targetLexemeId")) {
                targetLexemeRows++;
                check(columns[0].equals("LangDexTranslation"));
                check(columns[3].equals("langdex_target_lexeme_id"));
            }
            mappedRows++;
        }
        check(mappedRows == 14);
        check(targetLexemeRows == 1);
        check(actualTypes.equals(EXPECTED_TYPES));
        check(ownerTargets.size() == 14);
        check(ownerTargets.get("LangDexWordProfile.lexicalClassMask")
                .equals("langdex_lexical_class_mask"));
        check(ownerTargets.get("LangDexWordProfile.featureBits").equals("langdex_feature_bits"));
        check(ownerTargets.get("LangDexWordProfile.evidenceMask").equals("langdex_evidence_mask"));
        check(ownerTargets.get("LangDexWordProfile.confidencePermille")
                .equals("langdex_confidence_permille"));
        check(ownerTargets.get("LangDexLexicalProfile.lexicalClassMask")
                .equals("langdex_lexical_profile_lexical_class_mask"));
        check(ownerTargets.get("LangDexLexicalProfile.featureBits")
                .equals("langdex_lexical_profile_feature_bits"));
        check(ownerTargets.get("LangDexLexicalProfile.evidenceMask")
                .equals("langdex_lexical_profile_evidence_mask"));
        check(ownerTargets.get("LangDexLexicalProfile.confidencePermille")
                .equals("langdex_lexical_profile_confidence_permille"));
    }

    private static void writeExport(Path directory) throws Exception {
        Path image = directory.resolve("langdex.m3lex");
        writeImage(image);
        Files.writeString(directory.resolve("synexia.shards.tsv"),
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tlangdex.m3lex\t" + LEXEME + "\t" + LEXEME + "\t1\t"
                        + LEXEME.length() + "\t" + sha256(image) + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.records.tsv"),
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\t"
                        + "mapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + SOURCE_ID + "\tsynexia-unicodex/docs/langdex/UPSTREAM_SCHEMA.tsv\tlangdex\ten\t"
                        + RECORD_ID + "\t" + LEXEME + "\t0\t0\tlangdex:" + RECORD_ID
                        + "\tLangDex test fixture\t-\tlangdex\t" + PAYLOAD + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.precompute.tsv"),
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\t"
                        + "non_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t5\t112903447\t5\t0\t0\tTrue\tTrue\tFalse\tlangdex\n",
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "langdex\t1\t1\t" + profileHash("langdex", 1, 1) + "\n",
                StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        verifyManifestAndFieldMap();
        Path directory = Files.createTempDirectory("m3-langdex-receiver-");
        try {
            writeExport(directory);
            SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(directory);
            try {
                check(catalog.recordCount() == 1);
                SharedLexiconCatalog.SourceMapping mapping =
                        catalog.findMapping(SOURCE_ID, RECORD_ID).orElseThrow();
                check(mapping.sourceId().equals(SOURCE_ID));
                check(mapping.recordId().equals(RECORD_ID));
                check(mapping.sourcePath().equals("synexia-unicodex/docs/langdex/UPSTREAM_SCHEMA.tsv"));
                check(mapping.lexeme().equals(LEXEME));
                check(mapping.mappingId().equals("langdex:" + RECORD_ID));
                check(mapping.precomputeProfile().equals("langdex"));
                check(mapping.coordinate().equals(new SharedLexiconCatalog.Coordinate(0, 0)));
                check(catalog.findMapping(SOURCE_ID, "0").isEmpty());
                check(catalog.textAt(mapping.coordinate()).equals(LEXEME));

                SynexiaPrecomputePayload payload =
                        catalog.findPrecomputePayload(SOURCE_ID, RECORD_ID).orElseThrow();
                check(payload.fieldNames().equals(new TreeSet<>(EXPECTED_FIELDS)));
                check(payload.canonicalJson().equals(PAYLOAD));
                check(payload.requireLong("langdex_concept_id") == 701L);
                check(payload.requireInt("langdex_confidence_permille") == 1000);
                check(payload.requireInt("langdex_evidence_mask") == 1073741825);
                check(payload.requireInt("langdex_feature_bits") == Integer.MAX_VALUE);
                check(payload.requireInt("langdex_flags") == 65535);
                check(payload.requireLong("langdex_frequency") == 123456789L);
                check(payload.requireLong("langdex_lexical_class_mask") == 4611686018427387905L);
                check(payload.requireInt("langdex_lexical_profile_confidence_permille") == 735);
                check(payload.requireInt("langdex_lexical_profile_evidence_mask") == 17);
                check(payload.requireInt("langdex_lexical_profile_feature_bits") == 4660);
                check(payload.requireLong("langdex_lexical_profile_lexical_class_mask") == 12L);
                check(payload.requireLong("langdex_semantic_class_mask") == 2305843009213693953L);
                SynexiaPrecomputePayload legacy = SynexiaPrecomputePayload.parse(LEGACY_PAYLOAD);
                check(legacy.fieldNames().equals(new TreeSet<>(REQUIRED_FIELDS)));
                check(legacy.field("langdex_lexical_profile_feature_bits").isEmpty());
                check(payload.requireInt("langdex_subject_id") == 17);
                check(payload.requireLong("langdex_target_lexeme_id") == 70002L);
                check(catalog.findPrecomputePayload(SOURCE_ID, RECORD_ID).orElseThrow() == payload);
                check(catalog.findPrecomputePayload(SOURCE_ID, "70002").isEmpty());

                SharedLexiconCatalog.PrecomputeFacts facts =
                        catalog.precomputeAt(new SharedLexiconCatalog.Coordinate(0, 0));
                check(facts.utf16Units() == 5);
                check(facts.javaHash() == 112903447L);
                check(facts.codePoints() == 5);
                check(facts.ascii() && facts.latin1() && !facts.containsWhitespace());
            } finally {
                catalog.close();
            }

            String nonCanonicalPayload = PAYLOAD.replace(
                    "{\"langdex_concept_id\":701,\"langdex_confidence_permille\":1000,",
                    "{\"langdex_confidence_permille\":1000,\"langdex_concept_id\":701,");
            String nonCanonical = Files.readString(
                    directory.resolve("synexia.records.tsv"), StandardCharsets.UTF_8)
                    .replace(PAYLOAD, nonCanonicalPayload);
            Files.writeString(directory.resolve("synexia.records.tsv"), nonCanonical, StandardCharsets.UTF_8);
            try {
                SharedLexiconCatalog.openLazy(directory);
                throw new AssertionError("non-canonical LangDex payload was accepted");
            } catch (IOException expected) {
                check(true);
            }
            System.out.println("M3JDK_LANGDEX_RECEIVER_PASS checks=" + checks
                    + " precomputeFields=" + EXPECTED_FIELDS.size() + " fieldMappings=14");
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.toList()) Files.delete(path);
            }
            Files.delete(directory);
        }
    }
}