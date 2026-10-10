/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Receiver for the optional {@code m3lex-family-v2} precompute bundle.
 *
 * <p>The legacy M3LEX image and opaque per-record payload remain unchanged.
 * This catalog is a separate, all-or-nothing bundle for the six index-shaped
 * families whose values cannot be represented losslessly in one JSON scalar
 * map. The phrase family additionally preserves ordered token-ID rewrite rules.
 * Every key carries the originating Synexia source scope; physical image
 * coordinates are never promoted to source identity.</p>
 */
public final class SharedLexiconFamilySidecarCatalog {
    public static final String SCHEMA_VERSION = "m3lex-family-v2";
    public static final String INDEX_FILE = "synexia.precompute-family-index.tsv";
    private static final List<String> FAMILIES = List.of(
            "phrase-rewrite", "prefix-counts", "spell-index", "token-frequency",
            "token-hash-precompute", "translation-projection");
    private static final Map<String, String> FILES = Map.of(
            "phrase-rewrite", "synexia.phrases.tsv",
            "prefix-counts", "synexia.prefix-counts.tsv",
            "spell-index", "synexia.spell.tsv",
            "token-frequency", "synexia.token-frequency.tsv",
            "token-hash-precompute", "synexia.token-hash.tsv",
            "translation-projection", "synexia.translation.tsv");
    private static final String[] INDEX_HEADER = {"schema_version", "family", "file", "rows", "sha256"};
    private static final String[] COMMON = {"source_id", "record_id", "source_manifest_revision", "owner_fingerprint"};
    private static final String RECORD_FILE = "synexia.records.tsv";
    private static final String[] RECORD_HEADER = {
            "source_id", "source_path", "source_kind", "language_tag", "record_id", "lexeme",
            "shard_id", "image_row", "mapping_id", "mapping_name", "translation_profile",
            "precompute_profile", "precompute_payload"
    };
    private static final Set<String> COORDINATE_FAMILIES =
            Set.of("prefix-counts", "token-frequency", "token-hash-precompute");


    /** Stable source ownership carried by every family row. */
    public record SourceScope(String sourceId, String recordId,
                              String sourceManifestRevision, String ownerFingerprint) {
        public SourceScope {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceManifestRevision = text(sourceManifestRevision, "sourceManifestRevision");
            ownerFingerprint = text(ownerFingerprint, "ownerFingerprint");
        }
    }

    public record PhraseKey(M3PhrasePrecompute.Scope scope) {
        public PhraseKey {
            scope = Objects.requireNonNull(scope, "scope");
        }
    }

    public record TranslationKey(SourceScope scope, String sourceLanguage, String targetLanguage,
                                 String lexiconFingerprint, String sourceFingerprint) {
        public TranslationKey {
            scope = Objects.requireNonNull(scope, "scope");
            sourceLanguage = text(sourceLanguage, "sourceLanguage");
            targetLanguage = text(targetLanguage, "targetLanguage");
            lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
        }
    }

    /** The owner scope for one complete spell index; delete keys are rows within it. */
    public record SpellKey(SourceScope scope, String lexiconFingerprint, String language,
                           int maxEditDistance, int prefixLength, String sourceFingerprint) {
        public SpellKey {
            scope = Objects.requireNonNull(scope, "scope");
            lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            language = text(language, "language");
            sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
            if (maxEditDistance < 0 || prefixLength < 0) throw new IllegalArgumentException("negative spell scope");
        }
    }

    public record TokenHashKey(SourceScope scope, SharedLexiconCatalog.Coordinate coordinate,
                               String valueFingerprint, String tokenizerVersion,
                               int rangeStart, int rangeEnd) {
        public TokenHashKey {
            scope = Objects.requireNonNull(scope, "scope");
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
            tokenizerVersion = text(tokenizerVersion, "tokenizerVersion");
            if (rangeStart < 0 || rangeEnd < rangeStart) throw new IllegalArgumentException("invalid token range");
        }
    }

    public record PrefixKey(SourceScope scope, SharedLexiconCatalog.Coordinate coordinate,
                            String derivationVersion, String valueFingerprint, int tokenId) {
        public PrefixKey {
            scope = Objects.requireNonNull(scope, "scope");
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            derivationVersion = text(derivationVersion, "derivationVersion");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
            if (tokenId < 0) throw new IllegalArgumentException("negative tokenId");
        }
    }

    public record FrequencyKey(SourceScope scope, SharedLexiconCatalog.Coordinate coordinate,
                               String derivationVersion, String valueFingerprint) {
        public FrequencyKey {
            scope = Objects.requireNonNull(scope, "scope");
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            derivationVersion = text(derivationVersion, "derivationVersion");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
        }
    }

    private final Map<TranslationKey, M3LexiconPrecompute.TranslationProjection> translations;
    private final Map<SpellKey, M3LexiconPrecompute.SpellIndex> spells;
    private final Map<TokenHashKey, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes;
    private final Map<PrefixKey, M3LexiconPrecompute.PrefixCounts> prefixes;
    private final Map<FrequencyKey, M3LexiconPrecompute.TokenFrequency> frequencies;
    private final Map<PhraseKey, M3PhrasePrecompute.Catalog> phrases;
    private final Map<String, Integer> rowCounts;

    private SharedLexiconFamilySidecarCatalog(
            Map<TranslationKey, M3LexiconPrecompute.TranslationProjection> translations,
            Map<SpellKey, M3LexiconPrecompute.SpellIndex> spells,
            Map<TokenHashKey, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes,
            Map<PrefixKey, M3LexiconPrecompute.PrefixCounts> prefixes,
            Map<FrequencyKey, M3LexiconPrecompute.TokenFrequency> frequencies,
            Map<PhraseKey, M3PhrasePrecompute.Catalog> phrases,
            Map<String, Integer> rowCounts) {
        this.translations = Map.copyOf(translations);
        this.spells = Map.copyOf(spells);
        this.tokenHashes = Map.copyOf(tokenHashes);
        this.prefixes = Map.copyOf(prefixes);
        this.frequencies = Map.copyOf(frequencies);
        this.phrases = Map.copyOf(phrases);
        this.rowCounts = Map.copyOf(rowCounts);
    }

    /** Returns empty only for a completely legacy export with no family files. */
    public static Optional<SharedLexiconFamilySidecarCatalog> openOptional(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path root = directory.toAbsolutePath().normalize();
        boolean present = Files.exists(root.resolve(INDEX_FILE));
        for (String file : FILES.values()) present |= Files.exists(root.resolve(file));
        return present ? Optional.of(open(root)) : Optional.empty();
    }

    /** Opens and verifies one complete, checksummed family bundle. */
    public static SharedLexiconFamilySidecarCatalog open(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IOException("not a sidecar directory");
        Map<SourceIdentity, SharedLexiconCatalog.Coordinate> recordCoordinates =
                readRecordCoordinates(root);
        Map<String, IndexEntry> index = readIndex(root.resolve(INDEX_FILE));
        Map<TranslationKey, M3LexiconPrecompute.TranslationProjection> translations = new HashMap<>();
        Map<SpellKey, SpellAccumulator> spellGroups = new HashMap<>();
        Map<TokenHashKey, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes = new HashMap<>();
        Map<PrefixKey, M3LexiconPrecompute.PrefixCounts> prefixes = new HashMap<>();
        Map<FrequencyKey, M3LexiconPrecompute.TokenFrequency> frequencies = new HashMap<>();
        Map<PhraseKey, M3PhrasePrecompute.Builder> phraseGroups = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (String family : FAMILIES) {
            IndexEntry entry = index.get(family);
            Path file = safeChild(root, entry.file());
            byte[] bytes = Files.readAllBytes(file);
            if (!HexFormat.of().formatHex(sha256(bytes)).equals(entry.sha256()))
                throw new IOException("family sidecar checksum mismatch: " + family);
            List<String[]> rows = rows(bytes, fileHeader(family), entry.rows());
            List<String> previous = null;
            for (String[] row : rows) {
                validateScope(row, family, recordCoordinates);
                List<String> key = key(family, row);
                if (previous != null && compareKey(previous, key) >= 0)
                    throw new IOException("family rows are not strictly sorted: " + family);
                previous = key;
                try {
                    switch (family) {
                        case "phrase-rewrite" -> accumulate(phraseGroups, phrase(row));
                        case "translation-projection" -> add(translations, translation(row), family);
                        case "spell-index" -> accumulate(spellGroups, spell(row));
                        case "token-hash-precompute" -> add(tokenHashes, tokenHash(row), family);
                        case "prefix-counts" -> add(prefixes, prefix(row), family);
                        case "token-frequency" -> add(frequencies, frequency(row), family);
                        default -> throw new IOException("unknown family: " + family);
                    }
                } catch (IllegalArgumentException | NullPointerException failure) {
                    throw new IOException("invalid " + family + " row", failure);
                }
            }
            counts.put(family, rows.size());
        }
        Map<SpellKey, M3LexiconPrecompute.SpellIndex> spells = new HashMap<>();
        for (Map.Entry<SpellKey, SpellAccumulator> entry : spellGroups.entrySet())
            spells.put(entry.getKey(), entry.getValue().build(entry.getKey()));
        Map<PhraseKey, M3PhrasePrecompute.Catalog> phrases = new HashMap<>();
        for (Map.Entry<PhraseKey, M3PhrasePrecompute.Builder> entry : phraseGroups.entrySet())
            phrases.put(entry.getKey(), entry.getValue().build());
        return new SharedLexiconFamilySidecarCatalog(translations, spells, tokenHashes,
                prefixes, frequencies, phrases, counts);
    }

    public Optional<M3LexiconPrecompute.TranslationProjection> translationAt(TranslationKey key) {
        return Optional.ofNullable(translations.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3LexiconPrecompute.SpellIndex> spellAt(SpellKey key) {
        return Optional.ofNullable(spells.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3LexiconPrecompute.TokenHashPrecompute> tokenHashAt(TokenHashKey key) {
        return Optional.ofNullable(tokenHashes.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3LexiconPrecompute.PrefixCounts> prefixAt(PrefixKey key) {
        return Optional.ofNullable(prefixes.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3LexiconPrecompute.TokenFrequency> frequencyAt(FrequencyKey key) {
        return Optional.ofNullable(frequencies.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3PhrasePrecompute.Catalog> phraseAt(PhraseKey key) {
        return Optional.ofNullable(phrases.get(Objects.requireNonNull(key, "key")));
    }
    public Map<String, Integer> rowCounts() { return rowCounts; }

    private record IndexEntry(String family, String file, int rows, String sha256) { }
    private record SourceIdentity(String sourceId, String recordId) { }

    private static Map<SourceIdentity, SharedLexiconCatalog.Coordinate> readRecordCoordinates(
            Path root) throws IOException {
        Path path = safeChild(root, RECORD_FILE);
        List<String[]> rows = rows(Files.readAllBytes(path), RECORD_HEADER, -1);
        Map<SourceIdentity, SharedLexiconCatalog.Coordinate> result = new HashMap<>();
        for (String[] row : rows) {
            SourceIdentity identity = new SourceIdentity(
                    text(row[0], "source_id"), text(row[4], "record_id"));
            SharedLexiconCatalog.Coordinate coordinate =
                    new SharedLexiconCatalog.Coordinate(
                            nonNegativeInt(row[6], "shard_id"),
                            nonNegativeInt(row[7], "image_row"));
            if (result.put(identity, coordinate) != null)
                throw new IOException("duplicate source identity in " + RECORD_FILE);
        }
        return Map.copyOf(result);
    }

    private static void validateScope(String[] row, String family,
            Map<SourceIdentity, SharedLexiconCatalog.Coordinate> recordCoordinates)
            throws IOException {
        SourceIdentity identity = new SourceIdentity(
                text(row[0], "source_id"), text(row[1], "record_id"));
        SharedLexiconCatalog.Coordinate expected = recordCoordinates.get(identity);
        if (expected == null)
            throw new IOException(family + " sidecar references unknown source identity");
        if (COORDINATE_FAMILIES.contains(family)) {
            SharedLexiconCatalog.Coordinate actual =
                    new SharedLexiconCatalog.Coordinate(
                            nonNegativeInt(row[4], "shard_id"),
                            nonNegativeInt(row[5], "image_row"));
            if (!expected.equals(actual))
                throw new IOException(family + " sidecar coordinate mismatch");
        }
    }

    private static Map<String, IndexEntry> readIndex(Path path) throws IOException {
        List<String[]> rows = rows(Files.readAllBytes(path), INDEX_HEADER, FAMILIES.size());
        Map<String, IndexEntry> result = new LinkedHashMap<>();
        if (rows.size() != FAMILIES.size()) throw new IOException("family index coverage mismatch");
        for (int at = 0; at < rows.size(); at++) {
            String[] row = rows.get(at);
            if (!SCHEMA_VERSION.equals(row[0]) || !FAMILIES.get(at).equals(row[1])
                    || !FILES.get(row[1]).equals(row[2])) throw new IOException("invalid family index row");
            if (result.put(row[1], new IndexEntry(row[1], row[2], nonNegativeInt(row[3], "rows"), digest(row[4]))) != null)
                throw new IOException("duplicate family index row");
        }
        return result;
    }

    private static String[] fileHeader(String family) {
        return switch (family) {
            case "phrase-rewrite" -> concat(COMMON, "source_token_ids", "target_token_ids");
            case "translation-projection" -> concat(COMMON, "source_language", "target_language",
                    "lexicon_fingerprint", "source_fingerprint", "translated_token_ids", "mapped_token_count");
            case "spell-index" -> concat(COMMON, "lexicon_fingerprint", "language", "max_edit_distance",
                    "prefix_length", "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies");
            case "token-hash-precompute" -> concat(COMMON, "shard_id", "image_row", "value_fingerprint",
                    "tokenizer_version", "range_start", "range_end", "range_fingerprint_first",
                    "range_fingerprint_second", "token_sha256", "range_sha256");
            case "prefix-counts" -> concat(COMMON, "shard_id", "image_row", "derivation_version",
                    "value_fingerprint", "token_id", "prefix_counts");
            case "token-frequency" -> concat(COMMON, "shard_id", "image_row", "derivation_version",
                    "value_fingerprint", "frequencies");
            default -> throw new IllegalArgumentException("unknown family: " + family);
        };
    }

    private static String[] concat(String[] prefix, String... suffix) {
        String[] result = new String[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }

    private static List<String[]> rows(byte[] bytes, String[] header, int expectedRows) throws IOException {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf)
            throw new IOException("sidecar must be UTF-8 without BOM and use LF");
        final String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException failure) {
            throw new IOException("sidecar is not valid UTF-8", failure);
        }
        if (text.indexOf('\r') >= 0)
            throw new IOException("sidecar must be UTF-8 without BOM and use LF");
        String[] lines = text.split("\\n", -1);
        if (lines.length == 0 || !lines[0].equals(String.join("\t", header)))
            throw new IOException("sidecar header mismatch");
        int last = lines.length;
        if (last > 1 && lines[last - 1].isEmpty()) last--;
        List<String[]> result = new ArrayList<>();
        for (int line = 1; line < last; line++) {
            String[] fields = parseTsvLine(lines[line]);
            if (fields.length != header.length) throw new IOException("sidecar field count mismatch");
            result.add(fields);
        }
        if (expectedRows >= 0 && result.size() != expectedRows)
            throw new IOException("sidecar row count mismatch");
        return result;
    }

    private static String[] parseTsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        for (int at = 0; at < line.length(); at++) {
            char value = line.charAt(at);
            if (quoted) {
                if (value == '"') {
                    if (at + 1 < line.length() && line.charAt(at + 1) == '"') {
                        field.append('"');
                        at++;
                    } else {
                        quoted = false;
                        afterQuote = true;
                    }
                } else {
                    field.append(value);
                }
            } else if (afterQuote) {
                if (value == '\t') {
                    fields.add(field.toString());
                    field.setLength(0);
                    afterQuote = false;
                } else {
                    throw new IOException("invalid quoted TSV field");
                }
            } else if (value == '"') {
                if (field.length() != 0) throw new IOException("invalid quoted TSV field");
                quoted = true;
            } else if (value == '\t') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(value);
            }
        }
        if (quoted) throw new IOException("unterminated quoted TSV field");
        fields.add(field.toString());
        return fields.toArray(String[]::new);
    }

    private static List<String> key(String family, String[] row) {
        int count = switch (family) {
            case "phrase-rewrite" -> 5;
            case "translation-projection" -> 8;
            case "spell-index" -> 10;
            case "token-hash-precompute" -> 10;
            case "prefix-counts" -> 9;
            case "token-frequency" -> 8;
            default -> throw new IllegalArgumentException("unknown family: " + family);
        };
        return List.of(java.util.Arrays.copyOf(row, count));
    }

    private static int compareKey(List<String> left, List<String> right) {
        int fields = Math.min(left.size(), right.size());
        for (int at = 0; at < fields; at++) {
            int compared = compareCodePoints(left.get(at), right.get(at));
            if (compared != 0) return compared;
        }
        return Integer.compare(left.size(), right.size());
    }

    private static int compareCodePoints(String left, String right) {
        int leftAt = 0;
        int rightAt = 0;
        while (leftAt < left.length() && rightAt < right.length()) {
            int leftCodePoint = left.codePointAt(leftAt);
            int rightCodePoint = right.codePointAt(rightAt);
            if (leftCodePoint != rightCodePoint) return Integer.compare(leftCodePoint, rightCodePoint);
            leftAt += Character.charCount(leftCodePoint);
            rightAt += Character.charCount(rightCodePoint);
        }
        return Integer.compare(left.codePointCount(0, left.length()),
                right.codePointCount(0, right.length()));
    }

    private static Map.Entry<SourceScope, Integer> common(String[] row) {
        return Map.entry(new SourceScope(row[0], row[1], row[2], row[3]), 4);
    }

    private static PhraseEntry phrase(String[] row) throws IOException {
        SourceScope source = common(row).getKey();
        M3PhrasePrecompute.Scope scope = new M3PhrasePrecompute.Scope(
                source.sourceId(), source.recordId(), source.sourceManifestRevision(),
                source.ownerFingerprint());
        int[] sourceTokenIds = intArray(row[4], "source_token_ids");
        if (sourceTokenIds.length == 0) throw new IOException("source phrase must not be empty");
        int[] targetTokenIds = intArray(row[5], "target_token_ids");
        return new PhraseEntry(new PhraseKey(scope),
                new M3PhrasePrecompute.Phrase(scope, sourceTokenIds, targetTokenIds));
    }

    private static TranslationEntry translation(String[] row) throws IOException {
        SourceScope scope = common(row).getKey();
        int[] ids = intArray(row[8], "translated_token_ids");
        int count = nonNegativeInt(row[9], "mapped_token_count");
        if (count > ids.length) throw new IOException("translation count mismatch");
        String source = text(row[4], "source_language"), target = text(row[5], "target_language");
        String lexicon = text(row[6], "lexicon_fingerprint"), fingerprint = text(row[7], "source_fingerprint");
        return new TranslationEntry(new TranslationKey(scope, source, target, lexicon, fingerprint),
                new M3LexiconPrecompute.TranslationProjection(lexicon, source, target, fingerprint,
                        ids, count));
    }

    private static SpellEntry spell(String[] row) throws IOException {
        SourceScope scope = common(row).getKey();
        String lexicon = text(row[4], "lexicon_fingerprint"), language = text(row[5], "language");
        int max = nonNegativeInt(row[6], "max_edit_distance"), prefix = nonNegativeInt(row[7], "prefix_length");
        String source = text(row[8], "source_fingerprint");
        int[] ids = intArray(row[10], "candidate_token_ids", true);
        Map<Integer, Long> frequencies = longMap(row[11], "frequencies");
        String deleteKey = text(row[9], "delete_key", true);
        return new SpellEntry(new SpellKey(scope, lexicon, language, max, prefix, source),
                deleteKey, ids, frequencies);
    }

    private static TokenHashEntry tokenHash(String[] row) throws IOException {
        SourceScope scope = common(row).getKey();
        int shard = nonNegativeInt(row[4], "shard_id"), image = nonNegativeInt(row[5], "image_row");
        String value = text(row[6], "value_fingerprint"), tokenizer = text(row[7], "tokenizer_version");
        int start = nonNegativeInt(row[8], "range_start"), end = nonNegativeInt(row[9], "range_end");
        if (end < start) throw new IOException("range_end precedes range_start");
        long first = parseLong(row[10], "range_fingerprint_first"), second = parseLong(row[11], "range_fingerprint_second");
        byte[][] digests = digestMatrix(row[12], end - start, "token_sha256");
        byte[] rangeDigest = digestBytes(row[13], "range_sha256");
        var coordinate = new SharedLexiconCatalog.Coordinate(shard, image);
        var range = new M3LexiconPrecompute.RangeFingerprint(first, second, end - start);
        return new TokenHashEntry(new TokenHashKey(scope, coordinate, value, tokenizer, start, end),
                new M3LexiconPrecompute.TokenHashPrecompute(value, start, end, digests, range, rangeDigest));
    }

    private static PrefixEntry prefix(String[] row) throws IOException {
        SourceScope scope = common(row).getKey();
        var coordinate = new SharedLexiconCatalog.Coordinate(nonNegativeInt(row[4], "shard_id"),
                nonNegativeInt(row[5], "image_row"));
        long[] counts = longArray(row[9], "prefix_counts");
        if (counts.length == 0 || counts[0] != 0) throw new IOException("prefix counts must start at zero");
        for (int at = 1; at < counts.length; at++) if (counts[at] < counts[at - 1])
            throw new IOException("prefix counts are not monotonic");
        String version = text(row[6], "derivation_version"), value = text(row[7], "value_fingerprint");
        int token = nonNegativeInt(row[8], "token_id");
        return new PrefixEntry(new PrefixKey(scope, coordinate, version, value, token),
                new M3LexiconPrecompute.PrefixCounts(value, token, counts));
    }

    private static FrequencyEntry frequency(String[] row) throws IOException {
        SourceScope scope = common(row).getKey();
        var coordinate = new SharedLexiconCatalog.Coordinate(nonNegativeInt(row[4], "shard_id"),
                nonNegativeInt(row[5], "image_row"));
        String version = text(row[6], "derivation_version"), value = text(row[7], "value_fingerprint");
        return new FrequencyEntry(new FrequencyKey(scope, coordinate, version, value),
                new M3LexiconPrecompute.TokenFrequency(value, intMap(row[8], "frequencies")));
    }

    private record PhraseEntry(PhraseKey key, M3PhrasePrecompute.Phrase phrase) { }
    private record TranslationEntry(TranslationKey key, M3LexiconPrecompute.TranslationProjection value) { }
    private record SpellEntry(SpellKey key, String deleteKey, int[] tokenIds,
                              Map<Integer, Long> frequencies) { }
    private record TokenHashEntry(TokenHashKey key, M3LexiconPrecompute.TokenHashPrecompute value) { }
    private record PrefixEntry(PrefixKey key, M3LexiconPrecompute.PrefixCounts value) { }
    private record FrequencyEntry(FrequencyKey key, M3LexiconPrecompute.TokenFrequency value) { }

    private static <K, V> void add(Map<K, V> target, Map.Entry<K, V> entry, String family) throws IOException {
        if (target.put(entry.getKey(), entry.getValue()) != null) throw new IOException("duplicate " + family + " key");
    }
    private static void add(Map<TranslationKey, M3LexiconPrecompute.TranslationProjection> map,
                            TranslationEntry entry, String family) throws IOException { add(map, Map.entry(entry.key(), entry.value()), family); }
    private static void accumulate(Map<PhraseKey, M3PhrasePrecompute.Builder> groups,
                                   PhraseEntry entry) {
        groups.computeIfAbsent(entry.key(),
                key -> M3PhrasePrecompute.builder(key.scope())).put(entry.phrase());
    }

    private static void accumulate(Map<SpellKey, SpellAccumulator> groups, SpellEntry entry)
            throws IOException {
        SpellAccumulator accumulator = groups.computeIfAbsent(entry.key(), ignored -> new SpellAccumulator());
        accumulator.add(entry);
    }

    private static final class SpellAccumulator {
        private final Map<String, int[]> deleteToTokenIds = new TreeMap<>();
        private Map<Integer, Long> frequencies;

        private void add(SpellEntry entry) throws IOException {
            if (deleteToTokenIds.put(entry.deleteKey(), entry.tokenIds().clone()) != null)
                throw new IOException("duplicate spell delete key");
            if (frequencies == null) frequencies = new TreeMap<>(entry.frequencies());
            else if (!frequencies.equals(entry.frequencies()))
                throw new IOException("spell frequencies differ within one scope");
        }

        private M3LexiconPrecompute.SpellIndex build(SpellKey key) {
            return new M3LexiconPrecompute.SpellIndex(key.lexiconFingerprint(), key.language(),
                    key.maxEditDistance(), key.prefixLength(), key.sourceFingerprint(),
                    deleteToTokenIds, frequencies == null ? Map.of() : frequencies);
        }
    }
    private static void add(Map<TokenHashKey, M3LexiconPrecompute.TokenHashPrecompute> map,
                            TokenHashEntry entry, String family) throws IOException { add(map, Map.entry(entry.key(), entry.value()), family); }
    private static void add(Map<PrefixKey, M3LexiconPrecompute.PrefixCounts> map,
                            PrefixEntry entry, String family) throws IOException { add(map, Map.entry(entry.key(), entry.value()), family); }
    private static void add(Map<FrequencyKey, M3LexiconPrecompute.TokenFrequency> map,
                            FrequencyEntry entry, String family) throws IOException { add(map, Map.entry(entry.key(), entry.value()), family); }

    private static int[] intArray(String value, String name) throws IOException { return intArray(value, name, false); }
    private static int[] intArray(String value, String name, boolean sorted) throws IOException {
        if (value.equals("-")) return new int[0];
        String[] values = value.split(",", -1); int[] result = new int[values.length];
        int previous = -1;
        for (int at = 0; at < values.length; at++) {
            result[at] = nonNegativeInt(values[at], name);
            if (sorted && result[at] <= previous) throw new IOException(name + " is not strictly sorted");
            previous = result[at];
        }
        return result;
    }

    private static long[] longArray(String value, String name) throws IOException {
        if (value.equals("-")) return new long[0];
        String[] values = value.split(",", -1); long[] result = new long[values.length];
        for (int at = 0; at < values.length; at++) result[at] = parseLong(values[at], name);
        return result;
    }

    private static Map<Integer, Long> longMap(String value, String name) throws IOException {
        if (value.equals("-")) return Map.of();
        TreeMap<Integer, Long> result = new TreeMap<>(); int previous = -1;
        for (String item : value.split(",", -1)) {
            String[] pair = item.split(":", -1); if (pair.length != 2) throw new IOException("invalid " + name);
            int key = nonNegativeInt(pair[0], name + " key"); if (key <= previous) throw new IOException(name + " keys not sorted");
            long valuePart = parseLong(pair[1], name + " value"); if (valuePart < 0) throw new IOException("negative " + name + " value");
            result.put(key, valuePart); previous = key;
        }
        return result;
    }

    private static Map<Integer, Integer> intMap(String value, String name) throws IOException {
        Map<Integer, Long> longs = longMap(value, name); Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> entry : longs.entrySet()) {
            if (entry.getValue() > Integer.MAX_VALUE) throw new IOException(name + " value outside int range");
            result.put(entry.getKey(), entry.getValue().intValue());
        }
        return result;
    }

    private static byte[][] digestMatrix(String value, int expected, String name) throws IOException {
        if (value.equals("-")) { if (expected != 0) throw new IOException(name + " length mismatch"); return new byte[0][]; }
        String[] values = value.split(";", -1); if (values.length != expected) throw new IOException(name + " length mismatch");
        byte[][] result = new byte[values.length][]; for (int at = 0; at < values.length; at++) result[at] = digestBytes(values[at], name); return result;
    }
    private static byte[] digestBytes(String value, String name) throws IOException {
        String normalized = digest(value); try { return HexFormat.of().parseHex(normalized); }
        catch (IllegalArgumentException failure) { throw new IOException("invalid " + name, failure); }
    }
    private static String digest(String value) throws IOException {
        if (value.length() != 64 || !value.chars().allMatch(character ->
                (character >= '0' && character <= '9') || (character >= 'a' && character <= 'f')))
            throw new IOException("invalid SHA-256 digest");
        return value;
    }
    private static String text(String value, String name) {
        return text(value, name, false);
    }

    private static String text(String value, String name, boolean allowEmpty) {
        if (value == null || (!allowEmpty && value.isEmpty())
                || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0)
            throw new IllegalArgumentException(name + " is invalid");
        return value;
    }
    private static int nonNegativeInt(String value, String name) throws IOException {
        try { int parsed = Integer.parseInt(value); if (parsed < 0) throw new NumberFormatException(); return parsed; }
        catch (NumberFormatException failure) { throw new IOException("invalid " + name, failure); }
    }
    private static long parseLong(String value, String name) throws IOException {
        try { return Long.parseLong(value); }
        catch (NumberFormatException failure) { throw new IOException("invalid " + name, failure); }
    }
    private static Path safeChild(Path root, String file) throws IOException {
        if (file.isEmpty() || file.contains("/") || file.contains("\\") || file.equals(".") || file.equals(".."))
            throw new IOException("unsafe sidecar file");
        Path path = root.resolve(file).normalize(); if (!root.equals(path.getParent())) throw new IOException("unsafe sidecar file");
        if (!Files.isRegularFile(path)) throw new IOException("missing sidecar: " + file); return path;
    }
    private static byte[] sha256(byte[] value) throws IOException {
        try { return MessageDigest.getInstance("SHA-256").digest(value); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
