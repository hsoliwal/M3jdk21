import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SharedLexiconFamilySidecarCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

/** Executable proof for the lossless five-family M3JDK sidecar receiver. */
public final class SharedLexiconFamilySidecarCatalogTest {
    private static int checks;
    private static final String DIGEST_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String DIGEST_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String DIGEST_C = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-family-proof-");
        try {
            writeBundle(root);
            SharedLexiconFamilySidecarCatalog catalog =
                    SharedLexiconFamilySidecarCatalog.open(root);
            SharedLexiconFamilySidecarCatalog.SourceScope scope =
                    new SharedLexiconFamilySidecarCatalog.SourceScope("source-a", "record-a", "rev-1", "owner-a");

            SharedLexiconFamilySidecarCatalog.TranslationKey translationKey =
                    new SharedLexiconFamilySidecarCatalog.TranslationKey(scope, "en", "hi", "lex-a", "src-a");
            M3LexiconPrecompute.TranslationProjection translation =
                    catalog.translationAt(translationKey).orElseThrow();
            check(translation.mappedTokenCount() == 2, "translation mapped count preserved");
            check(translation.translatedTokenIdAt(2) == 7, "translation IDs preserved");

            SharedLexiconFamilySidecarCatalog.SpellKey spellKey =
                    new SharedLexiconFamilySidecarCatalog.SpellKey(scope, "lex-a", "en", 2, 4, "spell-a");
            M3LexiconPrecompute.SpellIndex spell = catalog.spellAt(spellKey).orElseThrow();
            check(spell.tokenIdsForDelete("a").length == 2, "first spell delete retained");
            check(spell.tokenIdsForDelete("b")[0] == 3, "second spell delete retained");
            check(spell.frequencyAt(3) == 4L, "spell frequencies grouped once");

            SharedLexiconCatalog.Coordinate coordinate = new SharedLexiconCatalog.Coordinate(1, 2);
            var tokenHashKey = new SharedLexiconFamilySidecarCatalog.TokenHashKey(
                    scope, coordinate, "value-a", "tok-1", 0, 3);
            check(catalog.tokenHashAt(tokenHashKey).orElseThrow().tokenSha256().length == 3,
                    "token hash digests retained");
            var prefixKey = new SharedLexiconFamilySidecarCatalog.PrefixKey(
                    scope, coordinate, "prefix-1", "value-a", 7);
            check(catalog.prefixAt(prefixKey).orElseThrow().rangeCount(1, 4) == 2,
                    "prefix counts retained");
            var frequencyKey = new SharedLexiconFamilySidecarCatalog.FrequencyKey(
                    scope, coordinate, "freq-1", "value-a");
            check(catalog.frequencyAt(frequencyKey).orElseThrow().frequencyAt(2) == 2,
                    "token frequencies retained");
            check(catalog.rowCounts().equals(Map.of(
                    "prefix-counts", 1, "spell-index", 2, "token-frequency", 1,
                    "token-hash-precompute", 1, "translation-projection", 1)),
                    "row counts");

            String records = Files.readString(root.resolve("synexia.records.tsv"),
                    StandardCharsets.UTF_8);
            Files.writeString(root.resolve("synexia.records.tsv"),
                    records.replace("\trecord-a\t\\\"alpha\\\"\\\"quote\\\"\t1\t2\t",
                            "\trecord-a\talpha\t9\t9\t"),
                    StandardCharsets.UTF_8);
            expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(root),
                    "coordinate mismatch rejected");
            Files.writeString(root.resolve("synexia.records.tsv"),
                    records.replace("source-a\tsource-path",
                            "source-missing\tsource-path"),
                    StandardCharsets.UTF_8);
            expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(root),
                    "unknown source identity rejected");
            Files.writeString(root.resolve("synexia.records.tsv"), records,
                    StandardCharsets.UTF_8);

            byte[] originalRecordBytes = Files.readAllBytes(root.resolve("synexia.records.tsv"));
            byte[] bomRecords = new byte[originalRecordBytes.length + 3];
            bomRecords[0] = (byte) 0xef;
            bomRecords[1] = (byte) 0xbb;
            bomRecords[2] = (byte) 0xbf;
            System.arraycopy(originalRecordBytes, 0, bomRecords, 3, originalRecordBytes.length);
            Files.write(root.resolve("synexia.records.tsv"), bomRecords);
            expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(root),
                    "leading BOM rejected");
            Files.write(root.resolve("synexia.records.tsv"), originalRecordBytes);
            Files.writeString(root.resolve("synexia.records.tsv"),
                    records.replace("\n", "\r\n"), StandardCharsets.UTF_8);
            expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(root),
                    "CR line endings rejected");
            Files.write(root.resolve("synexia.records.tsv"), originalRecordBytes);

            Files.writeString(root.resolve("synexia.export.json"), "legacy\\n",
                    StandardCharsets.UTF_8);
            check(SharedLexiconFamilySidecarCatalog.open(root).rowCounts().equals(
                    catalog.rowCounts()), "legacy export files may coexist");
            Files.delete(root.resolve("synexia.export.json"));

            Path legacy = Files.createTempDirectory("m3lex-family-legacy-");
            try {
                check(SharedLexiconFamilySidecarCatalog.openOptional(legacy).isEmpty(),
                        "legacy export stays empty");
            } finally {
                deleteTree(legacy);
            }

            Path partial = Files.createTempDirectory("m3lex-family-partial-");
            try {
                Files.copy(root.resolve("synexia.precompute-family-index.tsv"),
                        partial.resolve("synexia.precompute-family-index.tsv"));
                expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(partial),
                        "partial bundle rejected");
            } finally {
                deleteTree(partial);
            }
            Files.writeString(root.resolve("synexia.spell.tsv"),
                    Files.readString(root.resolve("synexia.spell.tsv"), StandardCharsets.UTF_8)
                            + "tampered");
            expectIOException(() -> SharedLexiconFamilySidecarCatalog.open(root),
                    "checksum drift rejected");
            System.out.println("M3JDK_FAMILY_SIDECAR_PASS checks=" + checks + " families=5 spell_rows=2");
        } finally {
            deleteTree(root);
        }
    }

    private static void writeBundle(Path root) throws Exception {
        String scope = "source-a\trecord-a\trev-1\towner-a\t";
        String prefix = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint", "token_id", "prefix_counts")
                + scope + "1\t2\tprefix-1\tvalue-a\t7\t0,1,1,2,3\n";
        String spellHeader = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "lexicon_fingerprint", "language", "max_edit_distance", "prefix_length",
                "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies");
        String spell = spellHeader
                + scope + "lex-a\ten\t2\t4\tspell-a\ta\t1,3\t1:2,3:4\n"
                + scope + "lex-a\ten\t2\t4\tspell-a\tb\t3\t1:2,3:4\n";
        String frequency = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint", "frequencies")
                + scope + "1\t2\tfreq-1\tvalue-a\t1:2,2:2,3:1\n";
        String tokenHash = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "value_fingerprint", "tokenizer_version", "range_start",
                "range_end", "range_fingerprint_first", "range_fingerprint_second", "token_sha256", "range_sha256")
                + scope + "1\t2\tvalue-a\ttok-1\t0\t3\t7\t11\t" + DIGEST_A + ";" + DIGEST_B + ";" + DIGEST_C + "\t" + DIGEST_A + "\n";
        String translation = row("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "source_language", "target_language", "lexicon_fingerprint", "source_fingerprint",
                "translated_token_ids", "mapped_token_count")
                + scope + "en\thi\tlex-a\tsrc-a\t0,3,7\t2\n";
        writeFamily(root, "synexia.prefix-counts.tsv", prefix);
        writeFamily(root, "synexia.spell.tsv", spell);
        writeFamily(root, "synexia.token-frequency.tsv", frequency);
        writeFamily(root, "synexia.token-hash.tsv", tokenHash);
        writeFamily(root, "synexia.translation.tsv", translation);
        String records = row("source_id", "source_path", "source_kind", "language_tag",
                "record_id", "lexeme", "shard_id", "image_row", "mapping_id", "mapping_name",
                "translation_profile", "precompute_profile", "precompute_payload")
                + row("source-a", "source-path", "translation", "en", "record-a", "\"alpha\"\"quote\"",
                        "1", "2", "mapping-a", "name-a", "profile-a", "profile-a", "{}");
        Files.writeString(root.resolve("synexia.records.tsv"), records, StandardCharsets.UTF_8);
        String index = row("schema_version", "family", "file", "rows", "sha256")
                + indexLine("prefix-counts", "synexia.prefix-counts.tsv", prefix, 1)
                + indexLine("spell-index", "synexia.spell.tsv", spell, 2)
                + indexLine("token-frequency", "synexia.token-frequency.tsv", frequency, 1)
                + indexLine("token-hash-precompute", "synexia.token-hash.tsv", tokenHash, 1)
                + indexLine("translation-projection", "synexia.translation.tsv", translation, 1);
        Files.writeString(root.resolve(SharedLexiconFamilySidecarCatalog.INDEX_FILE), index,
                StandardCharsets.UTF_8);
    }

    private static String indexLine(String family, String file, String content, int rows) throws Exception {
        return "m3lex-family-v2\t" + family + "\t" + file + "\t" + rows + "\t" + sha256(content) + "\n";
    }

    private static void writeFamily(Path root, String name, String content) throws Exception {
        Files.writeString(root.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static String row(String... fields) { return String.join("\t", fields) + "\n"; }

    private static String sha256(String content) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private static void expectIOException(ThrowingRunnable action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (java.io.IOException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
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
    private interface ThrowingRunnable { void run() throws Exception; }
}
