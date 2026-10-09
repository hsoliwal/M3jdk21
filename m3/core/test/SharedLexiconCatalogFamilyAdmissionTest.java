import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SharedLexiconFamilySidecarCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/** Proves that a verified family bundle is admitted by the normal catalog. */
public final class SharedLexiconCatalogFamilyAdmissionTest {
    private static int checks;
    private static final String DIGEST = "0123456789abcdef".repeat(4);

    public static void main(String[] args) throws Exception {
        Path complete = Files.createTempDirectory("m3lex-catalog-family-");
        try {
            writeCatalog(complete, true);
            assertCatalog(SharedLexiconCatalog.open(complete), "eager");
            assertCatalog(SharedLexiconCatalog.openLazy(complete), "lazy");

            Path legacy = Files.createTempDirectory("m3lex-catalog-legacy-");
            try {
                writeCatalog(legacy, false);
                try (CatalogClose ignored = new CatalogClose(SharedLexiconCatalog.open(legacy))) {
                    check(ignored.catalog.familyPrecompute().isEmpty(), "legacy family sidecar absent");
                }
            } finally {
                deleteTree(legacy);
            }

            Path partial = Files.createTempDirectory("m3lex-catalog-partial-");
            try {
                writeCatalog(partial, true);
                Files.delete(partial.resolve("synexia.translation.tsv"));
                expectIOException(() -> SharedLexiconCatalog.open(partial), "partial bundle rejected");
            } finally {
                deleteTree(partial);
            }

            SharedLexiconCatalog closed = SharedLexiconCatalog.open(complete);
            closed.close();
            expectIllegalState(closed::familyPrecompute, "close fences family getter");
            System.out.println("M3JDK_CATALOG_FAMILY_ADMISSION_PASS checks=" + checks
                    + " families=5 relations=2");
        } finally {
            deleteTree(complete);
        }
    }

    private static void assertCatalog(SharedLexiconCatalog catalog, String mode) {
        try {
            Optional<SharedLexiconFamilySidecarCatalog> optional = catalog.familyPrecompute();
            check(optional.isPresent(), mode + " admits family sidecar");
            SharedLexiconFamilySidecarCatalog family = optional.orElseThrow();
            var scope = new SharedLexiconFamilySidecarCatalog.SourceScope(
                    "source-a", "record-a", "rev-1", "owner-a");
            var translationKey = new SharedLexiconFamilySidecarCatalog.TranslationKey(
                    scope, "en", "hi", "lex-a", "src-a");
            check(family.translationAt(translationKey).orElseThrow().translatedTokenIdAt(0) == 7,
                    mode + " translation retained");
            var frequencyKey = new SharedLexiconFamilySidecarCatalog.FrequencyKey(
                    scope, new SharedLexiconCatalog.Coordinate(0, 0), "freq-1", "value-a");
            check(family.frequencyAt(frequencyKey).orElseThrow().frequencyAt(1) == 2,
                    mode + " frequency retained");
            var spellKey = new SharedLexiconFamilySidecarCatalog.SpellKey(
                    scope, "lex-a", "en", 2, 4, "spell-a");
            check(family.spellAt(spellKey).orElseThrow().tokenIdsForDelete("a")[1] == 1,
                    mode + " spell index retained");
            var tokenHashKey = new SharedLexiconFamilySidecarCatalog.TokenHashKey(
                    scope, new SharedLexiconCatalog.Coordinate(0, 0), "value-a", "tok-1", 0, 1);
            check(family.tokenHashAt(tokenHashKey).orElseThrow().tokenSha256().length == 1,
                    mode + " token hash retained");
            var prefixKey = new SharedLexiconFamilySidecarCatalog.PrefixKey(
                    scope, new SharedLexiconCatalog.Coordinate(0, 0), "prefix-1", "value-a", 7);
            check(family.prefixAt(prefixKey).orElseThrow().rangeCount(0, 1) == 1,
                    mode + " prefix counts retained");
            var relations = catalog.relatedLexemes().orElseThrow()
                    .findAll("source-a", "record-a");
            check(relations.size() == 2
                            && relations.get(0).lexeme().equals("alpha")
                            && relations.get(0).relatedLexeme().equals("omega")
                            && relations.get(1).relatedLexeme().equals("zeta")
                            && relations.get(0).coordinate().equals(new SharedLexiconCatalog.Coordinate(0, 0)),
                    mode + " directed related lexemes retained");
        } finally {
            catalog.close();
        }
    }

    private static void writeCatalog(Path root, boolean withFamily) throws Exception {
        Path image = root.resolve("synexia.m3lex");
        Files.writeString(root.resolve("words.txt"), "alpha\n", StandardCharsets.UTF_8);
        writeImageV2(image, List.of("alpha"));
        String imageDigest = sha256(Files.readAllBytes(image));
        write(root, "synexia.shards.tsv",
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tsynexia.m3lex\talpha\talpha\t1\t5\t" + imageDigest + "\n");
        write(root, "synexia.records.tsv",
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + "source-a\tpath\tdictionary\ten\trecord-a\talpha\t0\t0\t-\t-\t-\tfixture\t{}\n");
        write(root, "synexia.precompute.tsv",
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t5\t" + javaHash("alpha") + "\t5\t0\t0\tTrue\tTrue\tFalse\tfixture\n");
        write(root, "synexia.precompute-index.tsv",
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "fixture\t1\t1\t" + sha256("fixture\t1\t1\n") + "\n");
        if (withFamily) {
            writeFamily(root);
            writeRelated(root);
        }
    }

    private static void writeRelated(Path root) throws Exception {
        write(root, "synexia.related-sources.tsv", "source_id\nsource-a\n");
        write(root, "synexia.related.tsv",
                "source_id\trecord_id\tlexeme\trelated_lexeme\tshard_id\timage_row\n"
                        + "source-a\trecord-a\talpha\tomega\t0\t0\n"
                        + "source-a\trecord-a\talpha\tzeta\t0\t0\n");
    }

    private static void writeFamily(Path root) throws Exception {
        String scope = "source-a\trecord-a\trev-1\towner-a\t";
        String prefix = header("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint", "token_id", "prefix_counts")
                + scope + "0\t0\tprefix-1\tvalue-a\t7\t0,1\n";
        String spell = header("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "lexicon_fingerprint", "language", "max_edit_distance", "prefix_length",
                "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies")
                + scope + "lex-a\ten\t2\t4\tspell-a\ta\t0,1\t1:2\n";
        String frequency = header("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "derivation_version", "value_fingerprint", "frequencies")
                + scope + "0\t0\tfreq-1\tvalue-a\t1:2\n";
        String tokenHash = header("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "shard_id", "image_row", "value_fingerprint", "tokenizer_version", "range_start",
                "range_end", "range_fingerprint_first", "range_fingerprint_second", "token_sha256", "range_sha256")
                + scope + "0\t0\tvalue-a\ttok-1\t0\t1\t7\t11\t" + DIGEST + "\t" + DIGEST + "\n";
        String translation = header("source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
                "source_language", "target_language", "lexicon_fingerprint", "source_fingerprint",
                "translated_token_ids", "mapped_token_count")
                + scope + "en\thi\tlex-a\tsrc-a\t7\t1\n";
        write(root, "synexia.prefix-counts.tsv", prefix);
        write(root, "synexia.spell.tsv", spell);
        write(root, "synexia.token-frequency.tsv", frequency);
        write(root, "synexia.token-hash.tsv", tokenHash);
        write(root, "synexia.translation.tsv", translation);
        String index = header("schema_version", "family", "file", "rows", "sha256")
                + indexLine("prefix-counts", "synexia.prefix-counts.tsv", prefix)
                + indexLine("spell-index", "synexia.spell.tsv", spell)
                + indexLine("token-frequency", "synexia.token-frequency.tsv", frequency)
                + indexLine("token-hash-precompute", "synexia.token-hash.tsv", tokenHash)
                + indexLine("translation-projection", "synexia.translation.tsv", translation);
        write(root, SharedLexiconFamilySidecarCatalog.INDEX_FILE, index);
    }

    private static String indexLine(String family, String file, String content) throws Exception {
        long rows = content.lines().count() - 1;
        return "m3lex-family-v2\t" + family + "\t" + file + "\t" + rows + "\t" + sha256(content) + "\n";
    }

    private static void writeImageV2(Path target, List<String> words) throws Exception {
        long payload = 64L + 12L * words.size();
        long units = words.stream().mapToLong(String::length).sum();
        ByteBuffer bytes = ByteBuffer.allocate(Math.toIntExact(payload + 2L * units))
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0x4d334c4558303031L).putInt(2).putInt(words.size())
                .putLong(payload).putLong(units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            bytes.putInt(64 + row * 12, offset).putInt(64 + row * 12 + 4, word.length())
                    .putInt(64 + row * 12 + 8, javaHash(word));
            for (int at = 0; at < word.length(); at++) {
                char unit = word.charAt(at);
                int position = Math.toIntExact(payload) + (offset + at) * 2;
                bytes.put(position, (byte) unit).put(position + 1, (byte) (unit >>> 8));
            }
            offset += word.length();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.asReadOnlyBuffer().slice(0, 32));
        digest.update(bytes.asReadOnlyBuffer().slice(64, bytes.limit() - 64));
        bytes.put(32, digest.digest());
        Files.write(target, bytes.array());
    }

    private static String header(String... fields) { return String.join("\t", fields) + "\n"; }
    private static void write(Path root, String name, String content) throws Exception {
        Files.writeString(root.resolve(name), content, StandardCharsets.UTF_8);
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
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void expectIOException(Throwing action, String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (java.io.IOException expected) { checks++; }
    }
    private static void expectIllegalState(Throwing action, String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalStateException expected) { checks++; }
    }
    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (java.io.IOException failure) {
                    throw new RuntimeException(failure);
                }
            });
        }
    }
    private static final class CatalogClose implements AutoCloseable {
        private final SharedLexiconCatalog catalog;
        private CatalogClose(SharedLexiconCatalog catalog) { this.catalog = catalog; }
        @Override public void close() { catalog.close(); }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}

