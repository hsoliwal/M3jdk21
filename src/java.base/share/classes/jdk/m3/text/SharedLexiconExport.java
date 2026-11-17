/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Offline Java writer of a frozen, frequency-ranked M3LEX v2 catalog export.
 *
 * <p>For one source manifest and records file the directory is byte-identical to
 * {@code m3/runtime-integration/export-m3-lexicon.py}: v2 shard images,
 * {@code m3.shards.tsv}, {@code m3.records.tsv}, {@code m3.precompute-index.tsv},
 * {@code m3.precompute.tsv} and {@code m3.export.json}, with LF line ends on every
 * platform. No relation sidecar and no payload field map are written.</p>
 *
 * <p>{@link #writeFrequency} turns {@code WORD TAB COUNT} lines into one source family.
 * Word ids are frequency ranks: descending count, ties in UTF-16 order, most frequent
 * = 0. The rank is the record id; the image row stays a physical coordinate. The payload
 * keeps {@code corpus_count} and {@code frequency_rank}, and {@code source.commit} pins
 * the input bytes by SHA-256. Every byte is built before the first write, and
 * {@code m3.shards.tsv}, the file the catalog opens first, is written last.</p>
 */
final class SharedLexiconExport {
    private static final String SCHEMA = "m3-m3jdk-lexicon-export-2";
    private static final String SHARDS = "m3.shards.tsv";
    private static final String RECORDS = "m3.records.tsv";
    private static final String PROFILES = "m3.precompute-index.tsv";
    private static final String FACTS = "m3.precompute.tsv";
    private static final String EXPORT = "m3.export.json";
    private static final String PROFILE = "M3StringFacts";
    private static final String[] MANIFEST_HEADER = {"source_id", "canonical_name", "m3_path",
            "record_id_field", "mapping_fields", "precompute_target", "data_license",
            "data_policy", "precompute_fields"};
    private static final String[] RECORD_HEADER = {"source_id", "source_path", "source_kind",
            "language_tag", "record_id", "lexeme", "mapping_id", "mapping_name",
            "translation_profile", "precompute_profile", "precompute_payload"};
    private static final int SOURCE_ID = 0, RECORD_ID = 4, LEXEME = 5, PROFILE_ID = 9;
    private static final int PAYLOAD = 10;
    private static final String LICENSE =
            "operator-supplied frequency list; the license travels with the input";
    private static final String POLICY = "frozen frequency-ranked lexicon; record_id ="
            + " frequency rank (0 = most frequent, ties in UTF-16 order);"
            + " source.commit pins the input SHA-256";
    private static final String IDENTITY_RULE = "source_id + record_id is opaque and never"
            + " renumbered; image_row is only a physical M3LEX projection";
    private static final String DATA_POLICY = "operator-supplied snapshot only; no network"
            + " download or implicit license grant";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*");
    private static final Pattern COUNT = Pattern.compile("0|[1-9][0-9]{0,18}");
    private static final Comparator<Word> RANK = Comparator.<Word>comparingLong(Word::count)
            .reversed().thenComparing(Word::text);

    private SharedLexiconExport() { }

    /** One input word and its occurrence count in the frequency source. */
    private record Word(String text, long count) { }

    /** Provenance kept by m3.export.json; the input bytes are hashed, not retained. */
    private record Source(String repository, String commit, byte[] manifest, byte[] records,
                          Map<String, List<String>> precomputeFields) { }

    /** Ranks {@code WORD TAB COUNT} lines and publishes one export into an empty directory. */
    static void writeFrequency(String sourceId, String languageTag, Path input, Path output)
            throws IOException {
        requireName(sourceId, "source id");
        requireName(languageTag, "language tag");
        Path name = input.getFileName();
        String file = name == null ? "" : name.toString();
        if (file.isEmpty() || breaksField(file))
            throw new IOException("frequency input needs a file name without tabs or line breaks");
        byte[] raw = Files.readAllBytes(input);
        List<Word> ranked = readFrequency(raw);
        ranked.sort(RANK);
        List<String[]> rows = new ArrayList<>(ranked.size());
        for (int rank = 0; rank < ranked.size(); rank++)
            rows.add(row(sourceId, languageTag, file, rank, ranked.get(rank)));
        byte[] manifest = tsv(MANIFEST_HEADER, List.<String[]>of(new String[] {sourceId,
                sourceId, file, "frequency_rank", "lexeme,frequency_rank", PROFILE, LICENSE,
                POLICY, "-"}));
        Source source = new Source(file, "sha256:" + hex(sha256(raw)), manifest,
                tsv(RECORD_HEADER, rows), Map.of(sourceId, List.of()));
        publish(output, export(source, rows));
    }

    private static String[] row(String sourceId, String languageTag, String file, int rank,
                                Word word) {
        String id = Integer.toString(rank);
        String payload = "{\"corpus_count\":" + word.count() + ",\"frequency_rank\":" + id + "}";
        return new String[] {sourceId, file, "frequency", languageTag, id, word.text(), id,
                "frequency_rank", "-", PROFILE, payload};
    }

    private static List<Word> readFrequency(byte[] raw) throws IOException {
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(raw)).toString();
        if (text.isEmpty()) throw new IOException("frequency input is empty");
        String[] lines = text.split("\n", -1);
        int count = text.endsWith("\n") ? lines.length - 1 : lines.length;
        List<Word> words = new ArrayList<>(count);
        Set<String> seen = new HashSet<>();
        for (int at = 0; at < count; at++) {
            Word word = word(lines[at], at + 1);
            if (!seen.add(word.text()))
                throw new IOException("frequency line " + (at + 1) + ": duplicate word");
            words.add(word);
        }
        return words;
    }

    private static Word word(String line, int number) throws IOException {
        int tab = line.indexOf('\t');
        if (tab <= 0 || line.indexOf('\t', tab + 1) >= 0 || line.indexOf('\r') >= 0)
            throw new IOException("frequency line " + number + ": expected WORD TAB COUNT");
        String count = line.substring(tab + 1);
        if (!COUNT.matcher(count).matches()) throw invalidCount(number, null);
        try {
            return new Word(line.substring(0, tab), Long.parseLong(count));
        } catch (NumberFormatException overflow) {
            throw invalidCount(number, overflow);
        }
    }

    private static IOException invalidCount(int number, Throwable cause) {
        return new IOException("frequency line " + number
                + ": count is not a canonical nonnegative long", cause);
    }

    /** Output files in write order: shards, sidecars, export manifest, m3.shards.tsv. */
    private static Map<String, byte[]> export(Source source, List<String[]> rows)
            throws IOException {
        validate(rows);
        TreeSet<String> unique = new TreeSet<>();
        for (String[] row : rows) unique.add(row[LEXEME]);
        List<List<String>> shards = shards(unique);
        Map<String, byte[]> files = new LinkedHashMap<>();
        Map<String, int[]> physical = new HashMap<>();
        List<String[]> shardRows = new ArrayList<>(shards.size());
        for (int shard = 0; shard < shards.size(); shard++) {
            List<String> values = shards.get(shard);
            String file = shards.size() == 1 ? "m3.m3lex"
                    : String.format(Locale.ROOT, "synexia-%06d.m3lex", shard);
            byte[] image = SharedLexiconImage.encodeCatalogShard(values);
            files.put(file, image);
            shardRows.add(shardRow(shard, file, values, image));
            for (int row = 0; row < values.size(); row++)
                physical.put(values.get(row), new int[] {shard, row});
        }
        files.put(RECORDS, tsv(SharedLexiconCatalog.MAPPING_HEADER, mappingRows(rows, physical)));
        List<String[]> profiles = profileRows(rows);
        files.put(PROFILES, tsv(SharedLexiconCatalog.PROFILE_HEADER, profiles));
        files.put(FACTS, tsv(SharedLexiconCatalog.PRECOMPUTE_HEADER, factRows(rows, shards)));
        byte[] shardTsv = tsv(SharedLexiconCatalog.HEADER, shardRows);
        Map<String, Object> outputs = new TreeMap<>();
        files.forEach((name, bytes) -> outputs.put(name, hex(sha256(bytes))));
        outputs.put(SHARDS, hex(sha256(shardTsv)));
        files.put(EXPORT, exportJson(source, outputs, rows.size(), unique.size(), shards.size(),
                profiles.size()));
        files.put(SHARDS, shardTsv);
        return files;
    }

    /** The exporter's record admission: filled fields, unique identity, canonical payload. */
    private static void validate(List<String[]> rows) throws IOException {
        if (rows.isEmpty()) throw new IOException("lexicon records are empty");
        Set<List<String>> identities = new HashSet<>();
        for (String[] row : rows) {
            for (String field : row)
                if (field.isEmpty() || breaksField(field))
                    throw new IOException("record field is empty or holds a tab or line break");
            if (!identities.add(List.of(row[SOURCE_ID], row[RECORD_ID])))
                throw new IOException("duplicate source/record identity");
            if (!M3PrecomputePayload.parse(row[PAYLOAD]).canonicalJson().equals(row[PAYLOAD]))
                throw new IOException("precompute_payload is not canonical JSON");
        }
    }

    /** Exporter make_shards: a new shard at 65,536 records or the v2 unit budget. */
    private static List<List<String>> shards(Collection<String> sorted) {
        List<List<String>> shards = new ArrayList<>();
        List<String> current = new ArrayList<>();
        long units = 0;
        for (String value : sorted) {
            if (!current.isEmpty() && (current.size() >= SharedLexiconImage.MAX_RECORDS
                    || units + value.length() > SharedLexiconImage.MAX_V2_UNITS)) {
                shards.add(current);
                current = new ArrayList<>();
                units = 0;
            }
            current.add(value);
            units += value.length();
        }
        if (!current.isEmpty()) shards.add(current);
        return shards;
    }

    private static String[] shardRow(int shard, String file, List<String> values, byte[] image) {
        long units = 0;
        for (String value : values) units += value.length();
        return new String[] {Integer.toString(shard), file, escapeSidecar(values.get(0)),
                escapeSidecar(values.get(values.size() - 1)), Integer.toString(values.size()),
                Long.toString(units), hex(sha256(image))};
    }

    private static List<String[]> mappingRows(List<String[]> rows, Map<String, int[]> physical) {
        List<String[]> result = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            int[] at = physical.get(row[LEXEME]);
            result.add(new String[] {row[0], row[1], row[2], row[3], row[RECORD_ID],
                    escapeSidecar(row[LEXEME]), Integer.toString(at[0]), Integer.toString(at[1]),
                    row[6], row[7], row[8], row[PROFILE_ID], row[PAYLOAD]});
        }
        return result;
    }

    /** One row per profile; the fingerprint is SHA-256 of {@code profile\tsrc\timg\n}. */
    private static List<String[]> profileRows(List<String[]> rows) {
        Map<String, Integer> sources = new TreeMap<>();
        Map<String, Set<String>> images = new HashMap<>();
        for (String[] row : rows) {
            sources.merge(row[PROFILE_ID], 1, Integer::sum);
            images.computeIfAbsent(row[PROFILE_ID], ignored -> new HashSet<>()).add(row[LEXEME]);
        }
        List<String[]> result = new ArrayList<>(sources.size());
        sources.forEach((profile, count) -> {
            String imageCount = Integer.toString(images.get(profile).size());
            String line = profile + "\t" + count + "\t" + imageCount + "\n";
            result.add(new String[] {profile, count.toString(), imageCount,
                    hex(sha256(line.getBytes(StandardCharsets.UTF_8)))});
        });
        return result;
    }

    private static List<String[]> factRows(List<String[]> rows, List<List<String>> shards) {
        Map<String, Set<String>> profiles = new HashMap<>();
        for (String[] row : rows)
            profiles.computeIfAbsent(row[LEXEME], ignored -> new TreeSet<>()).add(row[PROFILE_ID]);
        List<String[]> result = new ArrayList<>(profiles.size());
        for (int shard = 0; shard < shards.size(); shard++) {
            List<String> values = shards.get(shard);
            for (int row = 0; row < values.size(); row++) {
                String value = values.get(row);
                result.add(facts(value, shard, row, String.join(" + ", profiles.get(value))));
            }
        }
        return result;
    }

    /** The exporter's facts_for row: the fixed facts SharedLexiconCatalog.PrecomputeFacts holds. */
    private static String[] facts(String text, int shard, int row, String profile) {
        int unpaired = 0, nonBmp = 0;
        boolean ascii = true, latin1 = true, space = false;
        for (int at = 0; at < text.length(); ) {
            int codePoint = text.codePointAt(at);
            if (Character.isSupplementaryCodePoint(codePoint)) nonBmp++;
            else if (Character.isSurrogate((char) codePoint)) unpaired++;
            ascii &= codePoint < 0x80;
            latin1 &= codePoint < 0x100;
            space |= pythonSpace(codePoint);
            at += Character.charCount(codePoint);
        }
        return new String[] {Integer.toString(shard), Integer.toString(row),
                Integer.toString(text.length()), Integer.toUnsignedString(text.hashCode()),
                Integer.toString(text.codePointCount(0, text.length())),
                Integer.toString(unpaired), Integer.toString(nonBmp), pythonBoolean(ascii),
                pythonBoolean(latin1), pythonBoolean(space), profile};
    }

    /** Python {@code str.isspace}: category Zs or bidi class WS, B or S (both on Unicode 15). */
    private static boolean pythonSpace(int codePoint) {
        byte bidi = Character.getDirectionality(codePoint);
        return Character.getType(codePoint) == Character.SPACE_SEPARATOR
                || bidi == Character.DIRECTIONALITY_WHITESPACE
                || bidi == Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR
                || bidi == Character.DIRECTIONALITY_SEGMENT_SEPARATOR;
    }

    private static String pythonBoolean(boolean value) { return value ? "True" : "False"; }

    private static byte[] exportJson(Source source, Map<String, Object> outputs, int records,
                                     int images, int shards, int profiles) throws IOException {
        Map<String, Object> fieldTypes = new TreeMap<>();
        for (String id : source.precomputeFields().keySet()) fieldTypes.put(id, Map.of());
        Map<String, Object> from = new TreeMap<>();
        from.put("repository", source.repository());
        from.put("commit", source.commit());
        from.put("manifest_sha256", hex(sha256(source.manifest())));
        from.put("records_sha256", hex(sha256(source.records())));
        from.put("relations_sha256", null);
        from.put("relation_policy", null);
        from.put("relation_sources", List.of());
        from.put("precompute_fields", new TreeMap<>(source.precomputeFields()));
        from.put("precompute_field_types", Map.of());
        from.put("precompute_field_types_by_source", fieldTypes);
        from.put("precompute_field_map_sha256", null);
        Map<String, Object> counts = new TreeMap<>(Map.of("source_records", records,
                "image_records", images, "source_families", source.precomputeFields().size(),
                "shards", shards, "precompute_profiles", profiles, "relation_records", 0));
        Map<String, Object> manifest = new TreeMap<>();
        manifest.put("schema", SCHEMA);
        manifest.put("source", from);
        manifest.put("target", target());
        manifest.put("counts", counts);
        manifest.put("outputs", outputs);
        manifest.put("identity_rule", IDENTITY_RULE);
        manifest.put("data_policy", DATA_POLICY);
        StringBuilder out = new StringBuilder();
        json(out, manifest, 0);
        return utf8(out.append('\n'));
    }

    private static Map<String, Object> target() {
        Map<String, Object> target = new TreeMap<>();
        target.put("repository", "https://github.com/hsoliwal/M3jdk21");
        target.put("image_format", "M3LEX001");
        target.put("image_version", 2);
        target.put("mapping_sidecar", RECORDS);
        target.put("mapping_payload_field",
                "precompute_payload (canonical JSON object; v1 input defaults to {})");
        target.put("shards_sidecar", SHARDS);
        target.put("precompute_index_sidecar", PROFILES);
        target.put("precompute_sidecar", FACTS);
        target.put("relation_sidecar", null);
        target.put("relation_sources_sidecar", null);
        return target;
    }

    /** Python json.dumps(indent=2, sort_keys=True) for sorted maps, lists, text and longs. */
    private static void json(StringBuilder out, Object value, int level) {
        if (value == null) out.append("null");
        else if (value instanceof String text) jsonString(out, text);
        else if (value instanceof Number number) out.append(number.longValue());
        else if (value instanceof Map<?, ?> map) jsonItems(out, map.entrySet(), '{', '}', level);
        else jsonItems(out, (List<?>) value, '[', ']', level);
    }

    private static void jsonItems(StringBuilder out, Collection<?> items, char open, char close,
                                  int level) {
        out.append(open);
        String separator = "";
        for (Object item : items) {
            out.append(separator).append('\n').append("  ".repeat(level + 1));
            separator = ",";
            if (item instanceof Map.Entry<?, ?> entry) {
                jsonString(out, (String) entry.getKey());
                out.append(": ");
                json(out, entry.getValue(), level + 1);
            } else json(out, item, level + 1);
        }
        if (!items.isEmpty()) out.append('\n').append("  ".repeat(level));
        out.append(close);
    }

    /** Python ensure_ascii string: short escapes, else lowercase 4-digit escapes. */
    private static void jsonString(StringBuilder out, String value) {
        out.append('"');
        for (int at = 0; at < value.length(); at++) {
            char unit = value.charAt(at);
            switch (unit) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (unit >= ' ' && unit <= '~') out.append(unit);
                    else out.append("\\u").append(HexFormat.of().toHexDigits(unit));
                }
            }
        }
        out.append('"');
    }

    /** Exporter escape_sidecar_text: backslash, C0 controls and lone surrogates escaped. */
    private static String escapeSidecar(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            if (Character.isHighSurrogate(unit) && at + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(at + 1))) {
                out.append(unit).append(text.charAt(++at));
            } else if (unit == '\\' || unit < 0x20 || Character.isSurrogate(unit)) {
                out.append("\\u").append(HexFormat.of().withUpperCase().toHexDigits(unit));
            } else out.append(unit);
        }
        return out.toString();
    }

    /** Python csv QUOTE_MINIMAL rows with a tab delimiter and LF terminator, UTF-8. */
    private static byte[] tsv(String[] header, List<String[]> rows) throws IOException {
        StringBuilder out = new StringBuilder();
        tsvLine(out, header);
        for (String[] row : rows) tsvLine(out, row);
        return utf8(out);
    }

    private static void tsvLine(StringBuilder out, String[] fields) {
        for (int at = 0; at < fields.length; at++) {
            String field = fields[at];
            if (at != 0) out.append('\t');
            if (field.indexOf('"') >= 0 || breaksField(field))
                out.append('"').append(field.replace("\"", "\"\"")).append('"');
            else out.append(field);
        }
        out.append('\n');
    }

    private static boolean breaksField(String field) {
        return field.indexOf('\t') >= 0 || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
    }

    private static byte[] utf8(CharSequence text) throws CharacterCodingException {
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }

    private static void requireName(String value, String what) throws IOException {
        if (!NAME.matcher(value).matches())
            throw new IOException(what + " must match " + NAME.pattern());
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }

    /** Creates or reuses an empty directory; never overwrites a file. */
    private static void publish(Path output, Map<String, byte[]> files) throws IOException {
        Files.createDirectories(output);
        try (Stream<Path> entries = Files.list(output)) {
            if (entries.findAny().isPresent())
                throw new IOException("output directory is not empty: " + output);
        }
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            try (FileChannel out = FileChannel.open(output.resolve(file.getKey()),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = ByteBuffer.wrap(file.getValue());
                while (bytes.hasRemaining()) out.write(bytes);
                out.force(true);
            }
        }
    }
}
