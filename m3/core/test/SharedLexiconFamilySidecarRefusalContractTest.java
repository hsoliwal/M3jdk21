/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3LexiconPrecompute;
import jdk.m3.text.SharedLexiconCatalog;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.FrequencyKey;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.PrefixKey;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.SourceScope;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.SpellKey;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.TokenHashKey;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog.TranslationKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Refusal and edge-case contract for the all-or-nothing {@code m3lex-family-v2} bundle:
 * index integrity, canonical TSV framing, per-family value grammar, key ordering,
 * duplicate scopes and the public key records. Every refusal is an {@link IOException}
 * (or the documented record argument exception) and never a partially opened catalog.
 */
public final class SharedLexiconFamilySidecarRefusalContractTest {
    private static int checks;
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);
    private static final String C = "c".repeat(64);
    private static final String S = "source-a\trecord-a\trev-1\towner-a\t";
    private static final String COMMON = "source_id\trecord_id\tsource_manifest_revision\towner_fingerprint\t";
    private static final String[] FAMILIES = {"prefix-counts", "spell-index", "token-frequency",
            "token-hash-precompute", "translation-projection"};
    private static final String[] FILES = {"m3.prefix-counts.tsv", "m3.spell.tsv",
            "m3.token-frequency.tsv", "m3.token-hash.tsv", "m3.translation.tsv"};
    private static final String[] HEADERS = {
            COMMON + "shard_id\timage_row\tderivation_version\tvalue_fingerprint\ttoken_id\tprefix_counts",
            COMMON + "lexicon_fingerprint\tlanguage\tmax_edit_distance\tprefix_length\tsource_fingerprint"
                    + "\tdelete_key\tcandidate_token_ids\tfrequencies",
            COMMON + "shard_id\timage_row\tderivation_version\tvalue_fingerprint\tfrequencies",
            COMMON + "shard_id\timage_row\tvalue_fingerprint\ttokenizer_version\trange_start\trange_end"
                    + "\trange_fingerprint_first\trange_fingerprint_second\ttoken_sha256\trange_sha256",
            COMMON + "source_language\ttarget_language\tlexicon_fingerprint\tsource_fingerprint"
                    + "\ttranslated_token_ids\tmapped_token_count"};
    private static final int PREFIX = 0, SPELL = 1, FREQUENCY = 2, TOKEN_HASH = 3, TRANSLATION = 4;
    private static final String INDEX_HEADER = "schema_version\tfamily\tfile\trows\tsha256";

    private SharedLexiconFamilySidecarRefusalContractTest() { }

    /** Immutable five-family fixture; each mutation replaces one family file. */
    private record Bundle(String[] contents, int[] rows) {
        static Bundle base() {
            return new Bundle(new String[] {
                    family(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t0,1,1,2,3"),
                    family(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1,3\t1:2,3:4",
                            S + "lex-a\ten\t2\t4\tspell-a\tb\t3\t1:2,3:4"),
                    family(FREQUENCY, S + "1\t2\tfreq-1\tvalue-a\t1:2,2:2,3:1"),
                    family(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t3\t7\t11\t" + A + ";" + B + ";" + C + "\t" + A),
                    family(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,3,7\t2")},
                    new int[] {1, 2, 1, 1, 1});
        }

        Bundle with(int family, String content, int count) {
            String[] nextContents = contents.clone();
            int[] nextRows = rows.clone();
            nextContents[family] = content;
            nextRows[family] = count;
            return new Bundle(nextContents, nextRows);
        }

        Bundle rows(int family, String... lines) { return with(family, family(family, lines), lines.length); }

        List<String> indexLines() throws Exception {
            List<String> lines = new ArrayList<>();
            for (int f = 0; f < FAMILIES.length; f++) {
                lines.add("m3lex-family-v2\t" + FAMILIES[f] + "\t" + FILES[f] + "\t" + rows[f] + "\t" + sha256(contents[f]));
            }
            return lines;
        }

        void write(Path root) throws Exception { write(root, index(indexLines())); }

        void write(Path root, String index) throws Exception {
            for (int f = 0; f < FILES.length; f++) {
                Files.writeString(root.resolve(FILES[f]), contents[f], StandardCharsets.UTF_8);
            }
            Files.writeString(root.resolve(SharedLexiconFamilySidecarCatalog.INDEX_FILE), index, StandardCharsets.UTF_8);
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-family-refusal-");
        try {
            keyRecords();
            validEdgeForms(root);
            indexRefusals(root);
            framingRefusals(root);
            familyValueRefusals(root);
            orderingAndDuplicateRefusals(root);
            directoryRefusals(root);
        } finally {
            deleteTree(root);
        }
        System.out.println("M3JDK_FAMILY_SIDECAR_REFUSAL_PASS checks=" + checks);
    }

    private static void keyRecords() {
        SourceScope scope = new SourceScope("s", "r", "rev", "owner");
        var coordinate = new SharedLexiconCatalog.Coordinate(0, 0);
        expect(IllegalArgumentException.class, () -> new SourceScope(null, "r", "rev", "owner"), "null source id");
        expect(IllegalArgumentException.class, () -> new SourceScope("", "r", "rev", "owner"), "empty source id");
        expect(IllegalArgumentException.class, () -> new SourceScope("s\t", "r", "rev", "owner"), "tab in source id");
        expect(IllegalArgumentException.class, () -> new SourceScope("s", "r\n", "rev", "owner"), "LF in record id");
        expect(IllegalArgumentException.class, () -> new SourceScope("s", "r", "rev\r", "owner"), "CR in revision");
        expect(NullPointerException.class, () -> new TranslationKey(null, "en", "hi", "lex", "src"), "null scope");
        expect(IllegalArgumentException.class, () -> new SpellKey(scope, "lex", "en", -1, 0, "src"), "negative edit distance");
        expect(IllegalArgumentException.class, () -> new SpellKey(scope, "lex", "en", 0, -1, "src"), "negative prefix length");
        check(new SpellKey(scope, "lex", "en", 0, 0, "src").maxEditDistance() == 0, "zero spell scope admitted");
        expect(IllegalArgumentException.class, () -> new TokenHashKey(scope, coordinate, "v", "t", -1, 0), "negative range start");
        expect(IllegalArgumentException.class, () -> new TokenHashKey(scope, coordinate, "v", "t", 2, 1), "inverted range");
        check(new TokenHashKey(scope, coordinate, "v", "t", 1, 1).rangeEnd() == 1, "empty token range admitted");
        expect(IllegalArgumentException.class, () -> new PrefixKey(scope, coordinate, "d", "v", -1), "negative token id");
        expect(NullPointerException.class, () -> new PrefixKey(scope, null, "d", "v", 0), "null prefix coordinate");
        expect(IllegalArgumentException.class, () -> new FrequencyKey(scope, coordinate, "", "v"), "empty derivation version");
        check(new FrequencyKey(scope, coordinate, "d", "v").equals(new FrequencyKey(
                new SourceScope("s", "r", "rev", "owner"), new SharedLexiconCatalog.Coordinate(0, 0), "d", "v")),
                "frequency keys are value records");
    }

    /** Lawful extremes: zero-row families, missing trailing LF, '-' empty lists and empty token ranges. */
    private static void validEdgeForms(Path root) throws Exception {
        Bundle bundle = Bundle.base()
                .with(FREQUENCY, HEADERS[FREQUENCY], 0)                 // header only, no LF
                .with(PREFIX, HEADERS[PREFIX] + "\n", 0)               // header and LF only
                .with(TRANSLATION, HEADERS[TRANSLATION] + "\n" + S + "en\thi\tlex-a\tsrc-a\t-\t0", 1) // no final LF
                .rows(SPELL, S + "lex-a\ten\t0\t0\tspell-a\tx\t-\t-")
                .rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t5\t5\t-3\t9\t-\t" + B);
        bundle.write(root);
        SharedLexiconFamilySidecarCatalog catalog = SharedLexiconFamilySidecarCatalog.open(root);
        check(catalog.rowCounts().equals(Map.of("prefix-counts", 0, "spell-index", 1, "token-frequency", 0,
                "token-hash-precompute", 1, "translation-projection", 1)), "zero-row families are counted");
        SourceScope scope = new SourceScope("source-a", "record-a", "rev-1", "owner-a");
        var coordinate = new SharedLexiconCatalog.Coordinate(1, 2);
        M3LexiconPrecompute.TranslationProjection translation = catalog.translationAt(
                new TranslationKey(scope, "en", "hi", "lex-a", "src-a")).orElseThrow();
        check(translation.translatedTokenIds().length == 0 && translation.mappedTokenCount() == 0,
                "'-' is an empty translated-id list");
        M3LexiconPrecompute.SpellIndex spell = catalog.spellAt(
                new SpellKey(scope, "lex-a", "en", 0, 0, "spell-a")).orElseThrow();
        check(spell.tokenIdsForDelete("x").length == 0 && spell.frequencies().isEmpty(),
                "'-' is an empty candidate list and frequency map");
        M3LexiconPrecompute.TokenHashPrecompute hashes = catalog.tokenHashAt(
                new TokenHashKey(scope, coordinate, "value-a", "tok-1", 5, 5)).orElseThrow();
        check(hashes.tokenSha256().length == 0 && hashes.rangeFingerprint().first() == -3L
                && hashes.rangeFingerprint().length() == 0, "empty token range keeps signed fingerprint");
        check(hashes.rangeSha256()[0] == (byte) 0xbb, "range digest bytes decoded");
        check(catalog.prefixAt(new PrefixKey(scope, coordinate, "prefix-1", "value-a", 7)).isEmpty(),
                "zero-row prefix family has no keys");
        check(catalog.frequencyAt(new FrequencyKey(scope, coordinate, "freq-1", "value-a")).isEmpty(),
                "zero-row frequency family has no keys");
        check(catalog.translationAt(new TranslationKey(scope, "en", "fr", "lex-a", "src-a")).isEmpty(),
                "different target language is a different scope");
        expect(NullPointerException.class, () -> catalog.spellAt(null), "null lookup key");

        Path optional = Files.createTempDirectory(root, "optional-");
        Files.writeString(optional.resolve("m3.spell.tsv"), Bundle.base().contents()[SPELL], StandardCharsets.UTF_8);
        expectIo(() -> SharedLexiconFamilySidecarCatalog.openOptional(optional), "",
                "one family file without its index is a partial bundle, not a legacy export");
    }

    private static void indexRefusals(Path root) throws Exception {
        Bundle base = Bundle.base();
        expectIndex(root, base, 0, 0, "m3lex-family-v1", "invalid family index row", "schema version");
        expectIndex(root, base, 0, 1, "spell-index", "invalid family index row", "family order");
        expectIndex(root, base, 0, 2, "m3.prefix.tsv", "invalid family index row", "family file name");
        expectIndex(root, base, 0, 3, "-1", "invalid rows", "negative row count");
        expectIndex(root, base, 0, 3, "one", "invalid rows", "non-numeric row count");
        expectIndex(root, base, 0, 4, A.substring(1), "invalid SHA-256 digest", "short digest");
        expectIndex(root, base, 0, 4, "!" + A.substring(1), "invalid SHA-256 digest", "digest below '0'");
        expectIndex(root, base, 0, 4, "A" + A.substring(1), "invalid SHA-256 digest", "upper-case digest");
        expectIndex(root, base, 0, 4, "g" + A.substring(1), "invalid SHA-256 digest", "digest above 'f'");
        expectIndex(root, base, 0, 3, "2", "sidecar row count mismatch", "index row count drift");

        List<String> lines = base.indexLines();
        base.write(root, index(lines.subList(0, 4)));
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(root), "sidecar row count mismatch", "missing family index row");
        base.write(root, "schema\tfamily\tfile\trows\tsha256\n" + String.join("\n", lines) + "\n");
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(root), "sidecar header mismatch", "index header");
        base.write(root);
        Path index = root.resolve(SharedLexiconFamilySidecarCatalog.INDEX_FILE);
        byte[] bytes = Files.readAllBytes(index);
        bytes[2] = (byte) 0xff;
        Files.write(index, bytes);
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(root), "not valid UTF-8", "malformed UTF-8 index");
    }

    private static void framingRefusals(Path root) throws Exception {
        Bundle base = Bundle.base();
        expectOpen(root, base.with(PREFIX, family(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t0,1").replace("\n" + S, "\r\n" + S), 1),
                "header mismatch", "CRLF after header changes the header field");
        expectOpen(root, base.with(SPELL, HEADERS[SPELL] + "\n" + S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:2\r\n", 1),
                "CRLF sidecar is not canonical", "CRLF row");
        expectOpen(root, base.rows(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7"), "field count mismatch", "missing field");
        expectOpen(root, base.with(PREFIX, base.contents()[PREFIX], 2), "sidecar row count mismatch", "row count drift");
        expectOpen(root, base.with(PREFIX, "shard_id\n", 0), "sidecar header mismatch", "family header");
    }

    private static void familyValueRefusals(Path root) throws Exception {
        Bundle base = Bundle.base();
        // translation-projection
        expectOpen(root, base.rows(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,3,7\t4"), "translation count mismatch", "count above ids");
        expectOpen(root, base.rows(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,-3\t1"), "invalid translated_token_ids", "negative id");
        expectOpen(root, base.rows(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,3,7\t1"),
                "invalid translation-projection row", "mapped count disagrees with changed ids");
        expectOpen(root, base.rows(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,3,7\tx"), "invalid mapped_token_count", "count grammar");
        // spell-index
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t3,1\t1:2"),
                "candidate_token_ids is not strictly sorted", "candidate order");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1,1\t1:2"),
                "candidate_token_ids is not strictly sorted", "repeated candidate");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:2:3"), "invalid frequencies", "pair arity");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t3:1,1:2"), "frequencies keys not sorted", "key order");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:-2"), "negative frequencies value", "negative count");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:x"), "invalid frequencies value", "count grammar");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\tx:1"), "invalid frequencies key", "key grammar");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t-2\t4\tspell-a\ta\t1\t1:2"), "invalid max_edit_distance", "negative distance");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\t\t1\t1:2"),
                "invalid spell-index row", "empty delete key");
        // token-hash-precompute
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t3\t2\t7\t11\t" + A + "\t" + A),
                "range_end precedes range_start", "inverted range");
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t1\tx\t11\t" + A + "\t" + A),
                "invalid range_fingerprint_first", "fingerprint grammar");
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t1\t7\t11\t-\t" + A),
                "token_sha256 length mismatch", "'-' for a nonempty range");
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t2\t7\t11\t" + A + "\t" + A),
                "token_sha256 length mismatch", "digest count below range length");
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t1\t7\t11\t" + A + "\t" + A.toUpperCase()),
                "invalid SHA-256 digest", "upper-case range digest");
        expectOpen(root, base.rows(TOKEN_HASH, S + "1\t2\tvalue-a\ttok-1\t0\t1\t7\t11\t" + A + "\tz"),
                "invalid SHA-256 digest", "short range digest");
        // prefix-counts
        expectOpen(root, base.rows(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t-"), "must start at zero", "empty counts");
        expectOpen(root, base.rows(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t1,2"), "must start at zero", "nonzero origin");
        expectOpen(root, base.rows(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t0,2,1"), "not monotonic", "decreasing counts");
        expectOpen(root, base.rows(PREFIX, S + "1\t2\tprefix-1\tvalue-a\t7\t0,x"), "invalid prefix_counts", "count grammar");
        expectOpen(root, base.rows(PREFIX, S + "x\t2\tprefix-1\tvalue-a\t7\t0,1"), "invalid shard_id", "shard grammar");
        // token-frequency
        expectOpen(root, base.rows(FREQUENCY, S + "1\t2\tfreq-1\tvalue-a\t1:3000000000"),
                "frequencies value outside int range", "frequency above int range");
        expectOpen(root, base.rows(FREQUENCY, S + "1\t2\t\tvalue-a\t1:3"),
                "invalid token-frequency row", "empty derivation version");
    }

    private static void orderingAndDuplicateRefusals(Path root) throws Exception {
        Bundle base = Bundle.base();
        expectOpen(root, base.rows(PREFIX, S + "2\t2\tprefix-1\tvalue-a\t7\t0,1", S + "1\t2\tprefix-1\tvalue-a\t7\t0,1"),
                "not strictly sorted: prefix-counts", "descending keys");
        expectOpen(root, base.rows(TRANSLATION, S + "en\thi\tlex-a\tsrc-a\t0,3,7\t2", S + "en\thi\tlex-a\tsrc-a\t0,1\t0"),
                "not strictly sorted: translation-projection", "equal sort keys");
        // Numeric spellings sort as text but parse to the same logical key: the duplicate is refused.
        expectOpen(root, base.rows(PREFIX, S + "01\t2\tprefix-1\tvalue-a\t7\t0,1", S + "1\t2\tprefix-1\tvalue-a\t7\t0,1"),
                "duplicate prefix-counts key", "same coordinate spelled twice");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t02\t4\tspell-a\ta\t1\t1:2", S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:2"),
                "duplicate spell delete key", "same delete key in one scope");
        expectOpen(root, base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:2", S + "lex-a\ten\t2\t4\tspell-a\tb\t1\t1:3"),
                "spell frequencies differ within one scope", "frequency drift inside a scope");
        // Distinct scopes keep independent frequency maps.
        base.rows(SPELL, S + "lex-a\ten\t2\t4\tspell-a\ta\t1\t1:2", S + "lex-a\ten\t2\t4\tspell-b\ta\t1\t1:3").write(root);
        SharedLexiconFamilySidecarCatalog catalog = SharedLexiconFamilySidecarCatalog.open(root);
        SourceScope scope = new SourceScope("source-a", "record-a", "rev-1", "owner-a");
        check(catalog.spellAt(new SpellKey(scope, "lex-a", "en", 2, 4, "spell-b")).orElseThrow().frequencyAt(1) == 3L,
                "second scope frequency");
        check(catalog.spellAt(new SpellKey(scope, "lex-a", "en", 2, 4, "spell-a")).orElseThrow().frequencyAt(1) == 2L,
                "first scope frequency");
    }

    private static void directoryRefusals(Path root) throws Exception {
        Path file = root.resolve("plain-file");
        Files.writeString(file, "x", StandardCharsets.UTF_8);
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(file), "not a sidecar directory", "regular file root");
        expect(NullPointerException.class, () -> {
            try { SharedLexiconFamilySidecarCatalog.open(null); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
        }, "null directory");
    }

    private static void expectIndex(Path root, Bundle bundle, int row, int field, String value,
                                    String fragment, String why) throws Exception {
        List<String> lines = bundle.indexLines();
        String[] fields = lines.get(row).split("\t", -1);
        fields[field] = value;
        lines.set(row, String.join("\t", fields));
        bundle.write(root, index(lines));
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(root), fragment, why);
    }

    private static void expectOpen(Path root, Bundle bundle, String fragment, String why) throws Exception {
        bundle.write(root);
        expectIo(() -> SharedLexiconFamilySidecarCatalog.open(root), fragment, why);
    }

    private static String family(int family, String... rows) {
        StringBuilder text = new StringBuilder(HEADERS[family]).append('\n');
        for (String row : rows) text.append(row).append('\n');
        return text.toString();
    }

    private static String index(List<String> lines) {
        return INDEX_HEADER + "\n" + String.join("\n", lines) + "\n";
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private interface IoAction { void run() throws Exception; }

    private static void expectIo(IoAction action, String fragment, String why) {
        checks++;
        try {
            action.run();
        } catch (IOException expected) {
            String message = String.valueOf(expected.getMessage());
            if (!message.contains(fragment)) throw new AssertionError(why + ": unexpected message " + message, expected);
            return;
        } catch (Exception wrong) {
            throw new AssertionError(why + ": wrong failure", wrong);
        }
        throw new AssertionError(why + ": accepted");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action, String why) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(why + ": wrong exception", thrown);
        }
        throw new AssertionError(why + ": accepted");
    }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

}
