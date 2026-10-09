/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3Lexicons;
import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SharedLexiconFamilySidecarCatalog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Contract proof for the target-side M3Lexicons adapter. */
public final class M3LexiconsFacadeTest {
    private static int checks;
    private static final String DIGEST_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String DIGEST_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String DIGEST_C = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lexicons-facade-");
        try {
            writeCatalog(root);
            try (M3Lexicons lexicons = M3Lexicons.open(root)) {
                check(lexicons.catalog() == lexicons.catalog(), "catalog identity is stable");
                check(lexicons.shardCount() == 1, "one shard");
                check(lexicons.recordCount() == 1, "one record");
                SharedLexiconCatalog.Coordinate coordinate =
                        lexicons.find("alpha").orElseThrow();
                check(coordinate.equals(new SharedLexiconCatalog.Coordinate(0, 0)),
                        "coordinate preserved");
                check(lexicons.textAt(coordinate).equals("alpha"), "text boundary");
                check(lexicons.findMappings("alpha").size() == 1, "mapping lookup");
                check(lexicons.findMapping("source-a", "record-a").isPresent(),
                        "source identity lookup");
                check(lexicons.prefix("al", 4).equals(List.of(coordinate)),
                        "prefix lookup");
                check(lexicons.precomputeAt(coordinate).javaHash() == javaHash("alpha"),
                        "precompute facts");
                check(lexicons.findPrecomputePayload("source-a", "record-a").isPresent(),
                        "opaque payload lookup");
                check(lexicons.precomputeProfiles().size() == 1, "profile lookup");
                SharedLexiconFamilySidecarCatalog family = lexicons.familyPrecompute().orElseThrow();
                check(family.rowCounts().equals(Map.of(
                        "prefix-counts", 1, "spell-index", 1, "token-frequency", 1,
                        "token-hash-precompute", 1, "translation-projection", 1)),
                        "all five Synexia sidecar families are admitted");
                SharedLexiconFamilySidecarCatalog.SourceScope scope =
                        new SharedLexiconFamilySidecarCatalog.SourceScope(
                                "source-a", "record-a", "rev-1", "owner-a");
                SharedLexiconCatalog.Coordinate coordinate =
                        new SharedLexiconCatalog.Coordinate(0, 0);
                check(family.prefixAt(new SharedLexiconFamilySidecarCatalog.PrefixKey(
                        scope, coordinate, "prefix-1", "value-a", 7)).orElseThrow()
                        .rangeCount(1, 3) == 1, "prefix sidecar lookup");
                check(family.spellAt(new SharedLexiconFamilySidecarCatalog.SpellKey(
                        scope, "lex-a", "en", 2, 4, "spell-a")).orElseThrow()
                        .frequencyAt(3) == 4L, "spell sidecar lookup");
                check(family.frequencyAt(new SharedLexiconFamilySidecarCatalog.FrequencyKey(
                        scope, coordinate, "freq-1", "value-a")).orElseThrow()
                        .frequencyAt(2) == 2, "frequency sidecar lookup");
                check(family.tokenHashAt(new SharedLexiconFamilySidecarCatalog.TokenHashKey(
                        scope, coordinate, "value-a", "tok-1", 0, 3)).orElseThrow()
                        .tokenSha256().length == 3, "token hash sidecar lookup");
                check(family.translationAt(new SharedLexiconFamilySidecarCatalog.TranslationKey(
                        scope, "en", "hi", "lex-a", "src-a")).orElseThrow()
                        .mappedTokenCount() == 2, "translation sidecar lookup");
                lexicons.warm();
            }
            M3Lexicons lazy = M3Lexicons.openLazy(root);
            check(lazy.find("alpha").isPresent(), "lazy facade lookup");
            lazy.close();
            expectIllegalState(() -> lazy.find("alpha"), "close fences facade");
            System.out.println("M3JDK_M3LEXICONS_FACADE_PASS checks=" + checks);
        } finally {
            deleteTree(root);
        }
    }

    private static void writeCatalog(Path root) throws Exception {
        Path image = root.resolve("synexia.m3lex");
        Files.writeString(root.resolve("words.txt"), "alpha\n", StandardCharsets.UTF_8);
        writeImageV2(image);
        String digest = sha256(Files.readAllBytes(image));
        write(root, "synexia.shards.tsv",
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tsynexia.m3lex\talpha\talpha\t1\t5\t" + digest + "\n");
        write(root, "synexia.records.tsv",
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + "source-a\tpath\tdictionary\ten\trecord-a\talpha\t0\t0\t-\t-\t-\tfixture\t{}\n");
        write(root, "synexia.precompute.tsv",
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t5\t" + javaHash("alpha")
                        + "\t5\t0\t0\tTrue\tTrue\tFalse\tfixture\n");
        write(root, "synexia.precompute-index.tsv",
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "fixture\t1\t1\t" + sha256("fixture\t1\t1\n") + "\n");
        writeFamilySidecars(root);
    }

    private static void writeFamilySidecars(Path root) throws Exception {
        String scope = "source-a\trecord-a\trev-1\towner-a\t";
        String prefix = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint",
                "token_id", "prefix_counts")
                + scope + "0\t0\tprefix-1\tvalue-a\t7\t0,1,1\n";
        String spell = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "lexicon_fingerprint", "language", "max_edit_distance", "prefix_length",
                "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies")
                + scope + "lex-a\ten\t2\t4\tspell-a\ta\t1,3\t1:2,3:4\n";
        String frequency = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint", "frequencies")
                + scope + "0\t0\tfreq-1\tvalue-a\t1:2,2:2\n";
        String tokenHash = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "value_fingerprint", "tokenizer_version", "range_start",
                "range_end", "range_fingerprint_first", "range_fingerprint_second",
                "token_sha256", "range_sha256")
                + scope + "0\t0\tvalue-a\ttok-1\t0\t3\t7\t11\t"
                + DIGEST_A + ";" + DIGEST_B + ";" + DIGEST_C + "\t" + DIGEST_A + "\n";
        String translation = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "source_language", "target_language", "lexicon_fingerprint", "source_fingerprint",
                "translated_token_ids", "mapped_token_count")
                + scope + "en\thi\tlex-a\tsrc-a\t0,3,7\t2\n";
        write(root, "synexia.prefix-counts.tsv", prefix);
        write(root, "synexia.spell.tsv", spell);
        write(root, "synexia.token-frequency.tsv", frequency);
        write(root, "synexia.token-hash.tsv", tokenHash);
        write(root, "synexia.translation.tsv", translation);
        String index = row("schema_version", "family", "file", "rows", "sha256")
                + indexLine("prefix-counts", "synexia.prefix-counts.tsv", prefix, 1)
                + indexLine("spell-index", "synexia.spell.tsv", spell, 1)
                + indexLine("token-frequency", "synexia.token-frequency.tsv", frequency, 1)
                + indexLine("token-hash-precompute", "synexia.token-hash.tsv", tokenHash, 1)
                + indexLine("translation-projection", "synexia.translation.tsv", translation, 1);
        write(root, SharedLexiconFamilySidecarCatalog.INDEX_FILE, index);
    }

    private static String indexLine(String family, String file, String content, int rows)
            throws Exception {
        return "m3lex-family-v2\t" + family + "\t" + file + "\t" + rows
                + "\t" + sha256(content) + "\n";
    }

    private static String row(String... fields) {
        return String.join("\t", fields) + "\n";
    }

    private static void writeImageV2(Path target) throws Exception {
        long payload = 64L + 12L;
        ByteBuffer bytes = ByteBuffer.allocate(Math.toIntExact(payload + 10L))
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0x4d334c4558303031L).putInt(2).putInt(1)
                .putLong(payload).putLong(5L);
        bytes.putInt(64, 0).putInt(68, 5).putInt(72, javaHash("alpha"));
        String value = "alpha";
        for (int at = 0; at < value.length(); at++) {
            int position = Math.toIntExact(payload) + at * 2;
            char unit = value.charAt(at);
            bytes.put(position, (byte) unit).put(position + 1, (byte) (unit >>> 8));
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.limit() - 64);
        bytes.position(32);
        bytes.put(digest.digest());
        Files.write(target, bytes.array());
    }

    private static int javaHash(String value) {
        int hash = 0;
        for (int at = 0; at < value.length(); at++) hash = 31 * hash + value.charAt(at);
        return hash;
    }

    private static String sha256(String value) throws Exception {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void write(Path root, String name, String content) throws Exception {
        Files.writeString(root.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void expectIllegalState(Throwing action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            checks++;
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (java.io.IOException failure) { throw new RuntimeException(failure); }
            });
        }
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
