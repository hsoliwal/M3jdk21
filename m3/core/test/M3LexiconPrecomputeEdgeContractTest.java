/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3LexiconPrecompute.IndexWordFacts;
import jdk.m3.text.M3LexiconPrecompute.IndexWordSignal;
import jdk.m3.text.M3LexiconPrecompute.PrefixCounts;
import jdk.m3.text.M3LexiconPrecompute.RangeFingerprint;
import jdk.m3.text.M3LexiconPrecompute.SiUnitPrecompute;
import jdk.m3.text.M3LexiconPrecompute.SpellIndex;
import jdk.m3.text.M3LexiconPrecompute.TokenFrequency;
import jdk.m3.text.M3LexiconPrecompute.TokenHashPrecompute;
import jdk.m3.text.M3LexiconPrecompute.TranslationProjection;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * Accessor, scope-identity and refusal contract for every immutable lexicon precompute owner.
 * Non-conforming input maps that report one key twice are modelled explicitly so the owners'
 * duplicate-key guards are exercised.
 */
public final class M3LexiconPrecomputeEdgeContractTest {
    private static int checks;
    private static final String FP = "sha256:lexicon";

    private M3LexiconPrecomputeEdgeContractTest() {}

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what + " [check " + checks + "]");
    }

    private static void expect(Class<? extends Throwable> type, String fragment, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)
                    && (fragment == null
                            || String.valueOf(thrown.getMessage()).contains(fragment))) {
                return;
            }
            throw new AssertionError(
                    "wrong refusal, wanted "
                            + type.getSimpleName()
                            + " containing '"
                            + fragment
                            + "'",
                    thrown);
        }
        throw new AssertionError(
                "missing " + type.getSimpleName() + " containing '" + fragment + "'");
    }

    /**
     * A map view whose forEach/entrySet reports the given entries verbatim, duplicate keys
     * included.
     */
    private static <K, V> Map<K, V> repeatingMap(List<Map.Entry<K, V>> entries) {
        return new AbstractMap<>() {
            @Override
            public Set<Map.Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override
                    public Iterator<Map.Entry<K, V>> iterator() {
                        return entries.iterator();
                    }

                    @Override
                    public int size() {
                        return entries.size();
                    }
                };
            }
        };
    }

    private static byte[] sha(int seed) {
        byte[] value = new byte[32];
        Arrays.fill(value, (byte) seed);
        return value;
    }

    public static void main(String[] args) {
        siUnit();
        translationProjection();
        spellIndex();
        indexWordFacts();
        indexWordSignal();
        tokenHashAndRanges();
        frequencies();
        System.out.println("M3_LEXICON_PRECOMPUTE_EDGE_CONTRACT_PASS checks=" + checks);
    }

    private static void siUnit() {
        SiUnitPrecompute low = new SiUnitPrecompute(-100, 7L, -273.15d, true);
        SiUnitPrecompute high = new SiUnitPrecompute(100, 0L, 0.0d, false);
        check(
                low.decimalExponent() == -100
                        && low.dimensionPacked() == 7L
                        && low.offset() == -273.15d
                        && low.prefixable()
                        && high.decimalExponent() == 100
                        && !high.prefixable(),
                "SI bounds");
        expect(
                IllegalArgumentException.class,
                "outside -100..100",
                () -> new SiUnitPrecompute(-101, 0L, 0.0d, false));
        expect(
                IllegalArgumentException.class,
                "outside -100..100",
                () -> new SiUnitPrecompute(101, 0L, 0.0d, false));
        expect(
                IllegalArgumentException.class,
                "offset is not finite",
                () -> new SiUnitPrecompute(0, 0L, Double.NaN, false));
        expect(
                IllegalArgumentException.class,
                "offset is not finite",
                () -> new SiUnitPrecompute(0, 0L, Double.NEGATIVE_INFINITY, false));
    }

    private static void translationProjection() {
        int[] ids = {0, 5, 2, 1};
        TranslationProjection derived = new TranslationProjection(FP, "en", "fr", "src", ids);
        ids[1] = 0;
        check(
                derived.mappedTokenCount() == 2 && derived.translatedTokenIdAt(1) == 5,
                "derived count, copied input");
        int[] out = derived.translatedTokenIds();
        out[0] = 9;
        check(derived.translatedTokenIdAt(0) == 0, "copied output");
        check(
                derived.lexiconFingerprint().equals(FP)
                        && derived.sourceLanguage().equals("en")
                        && derived.targetLanguage().equals("fr")
                        && derived.sourceFingerprint().equals("src"),
                "scope accessors");
        check(derived.appliesTo(FP, "en", "fr", "src"), "matching scope");
        check(!derived.appliesTo("other", "en", "fr", "src"), "lexicon differs");
        check(!derived.appliesTo(FP, "de", "fr", "src"), "source language differs");
        check(!derived.appliesTo(FP, "en", "de", "src"), "target language differs");
        check(!derived.appliesTo(FP, "en", "fr", "other"), "source fingerprint differs");
        expect(IndexOutOfBoundsException.class, null, () -> derived.translatedTokenIdAt(4));
        expect(IndexOutOfBoundsException.class, null, () -> derived.translatedTokenIdAt(-1));
        TranslationProjection declared =
                new TranslationProjection(FP, "en", "fr", "src", new int[] {1, 0, 2}, 2);
        check(declared.mappedTokenCount() == 2, "declared count accepted");
        check(
                new TranslationProjection(FP, "en", "fr", "src", new int[0], 0).mappedTokenCount()
                        == 0,
                "empty");
        expect(
                IllegalArgumentException.class,
                "mappedTokenCount outside projection",
                () -> new TranslationProjection(FP, "en", "fr", "src", new int[] {0}, -1));
        expect(
                IllegalArgumentException.class,
                "mappedTokenCount outside projection",
                () -> new TranslationProjection(FP, "en", "fr", "src", new int[] {0}, 2));
        expect(
                IllegalArgumentException.class,
                "mappedTokenCount does not match changed token IDs",
                () -> new TranslationProjection(FP, "en", "fr", "src", new int[] {0, 1}, 1));
        expect(
                IllegalArgumentException.class,
                "translated token id is negative",
                () -> new TranslationProjection(FP, "en", "fr", "src", new int[] {-1}));
        expect(
                NullPointerException.class,
                "translatedTokenIds",
                () -> new TranslationProjection(FP, "en", "fr", "src", null));
        expect(
                IllegalArgumentException.class,
                "lexiconFingerprint is empty",
                () -> new TranslationProjection(null, "en", "fr", "src", new int[0]));
        expect(
                IllegalArgumentException.class,
                "sourceLanguage is empty",
                () -> new TranslationProjection(FP, "", "fr", "src", new int[0]));
        expect(
                IllegalArgumentException.class,
                "targetLanguage is empty",
                () -> new TranslationProjection(FP, "en", null, "src", new int[0]));
        expect(
                IllegalArgumentException.class,
                "sourceFingerprint is empty",
                () -> new TranslationProjection(FP, "en", "fr", "", new int[0]));
    }

    private static void spellIndex() {
        Map<String, int[]> deletes = new HashMap<>();
        int[] abIds = {1, 4};
        deletes.put("ab", abIds);
        deletes.put("a", new int[] {2});
        SpellIndex index = new SpellIndex(FP, "en", 2, 7, "src", deletes, Map.of(1, 10L, 4, 0L));
        abIds[0] = 3;
        deletes.clear();
        check(
                Arrays.equals(index.tokenIdsForDelete("ab"), new int[] {1, 4}),
                "delete ids copied at admission");
        check(index.tokenIdsForDelete("zz").length == 0, "absent delete key");
        index.tokenIdsForDelete("ab")[0] = 99;
        check(index.tokenIdsForDelete("ab")[0] == 1, "delete ids copied at read");
        NavigableMap<String, int[]> view = index.deleteToTokenIds();
        check(List.copyOf(view.keySet()).equals(List.of("a", "ab")), "sorted delete keys");
        view.get("a")[0] = 50;
        check(index.tokenIdsForDelete("a")[0] == 2, "map view arrays are copies");
        expect(UnsupportedOperationException.class, null, () -> view.put("b", new int[0]));
        check(
                index.lexiconFingerprint().equals(FP)
                        && index.language().equals("en")
                        && index.maxEditDistance() == 2
                        && index.prefixLength() == 7
                        && index.sourceFingerprint().equals("src"),
                "scope accessors");
        check(
                index.frequencyAt(1) == 10L
                        && index.frequencyAt(4) == 0L
                        && index.frequencyAt(5) == 0L,
                "frequencies");
        expect(UnsupportedOperationException.class, null, () -> index.frequencies().put(9, 1L));
        expect(IllegalArgumentException.class, "tokenId is negative", () -> index.frequencyAt(-1));
        expect(NullPointerException.class, "deleteKey", () -> index.tokenIdsForDelete(null));
        expect(
                IllegalArgumentException.class,
                "maxEditDistance is negative",
                () -> new SpellIndex(FP, "en", -1, 0, "src", Map.of(), Map.of()));
        expect(
                IllegalArgumentException.class,
                "prefixLength is negative",
                () -> new SpellIndex(FP, "en", 0, -1, "src", Map.of(), Map.of()));
        expect(
                IllegalArgumentException.class,
                "language is empty",
                () -> new SpellIndex(FP, "", 0, 0, "src", Map.of(), Map.of()));
        expect(
                NullPointerException.class,
                "deleteToTokenIds",
                () -> new SpellIndex(FP, "en", 0, 0, "src", null, Map.of()));
        expect(
                NullPointerException.class,
                "frequencies",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of(), null));
        Map<String, int[]> nullKey = new HashMap<>();
        nullKey.put(null, new int[0]);
        expect(
                NullPointerException.class,
                "delete key",
                () -> new SpellIndex(FP, "en", 0, 0, "src", nullKey, Map.of()));
        Map<String, int[]> nullIds = new HashMap<>();
        nullIds.put("a", null);
        expect(
                NullPointerException.class,
                "delete token ids",
                () -> new SpellIndex(FP, "en", 0, 0, "src", nullIds, Map.of()));
        expect(
                IllegalArgumentException.class,
                "delete token id is negative",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of("a", new int[] {-2}), Map.of()));
        expect(
                IllegalArgumentException.class,
                "delete token ids not strictly sorted",
                () ->
                        new SpellIndex(
                                FP, "en", 0, 0, "src", Map.of("a", new int[] {3, 3}), Map.of()));
        expect(
                IllegalArgumentException.class,
                "duplicate delete key",
                () ->
                        new SpellIndex(
                                FP,
                                "en",
                                0,
                                0,
                                "src",
                                repeatingMap(
                                        List.of(
                                                Map.entry("a", new int[] {1}),
                                                Map.entry("a", new int[] {2}))),
                                Map.of()));
        Map<Integer, Long> nullFrequencyId = new HashMap<>();
        nullFrequencyId.put(null, 1L);
        expect(
                NullPointerException.class,
                "frequency token id",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of(), nullFrequencyId));
        Map<Integer, Long> nullFrequency = new HashMap<>();
        nullFrequency.put(1, null);
        expect(
                NullPointerException.class,
                "frequency value",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of(), nullFrequency));
        expect(
                IllegalArgumentException.class,
                "frequency token id is negative",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of(), Map.of(-1, 1L)));
        expect(
                IllegalArgumentException.class,
                "negative frequency",
                () -> new SpellIndex(FP, "en", 0, 0, "src", Map.of(), Map.of(1, -1L)));
        expect(
                IllegalArgumentException.class,
                "duplicate frequency key",
                () ->
                        new SpellIndex(
                                FP,
                                "en",
                                0,
                                0,
                                "src",
                                Map.of(),
                                repeatingMap(List.of(Map.entry(1, 2L), Map.entry(1, 3L)))));
    }

    private static IndexWordFacts facts(
            int language,
            int words,
            long corpusTokens,
            long documents,
            long corpusCount,
            int documentFrequency) {
        return new IndexWordFacts(
                "rev",
                language,
                words,
                77L,
                corpusTokens,
                documents,
                0x5,
                corpusCount,
                documentFrequency,
                new int[] {3},
                new long[] {11L, 12L},
                new long[] {21L},
                new long[] {31L, 32L, 33L});
    }

    private static void indexWordFacts() {
        int[] memberships = {1, 2};
        long[] concepts = {10L};
        IndexWordFacts facts =
                new IndexWordFacts(
                        "rev-1",
                        4,
                        1000,
                        -9L,
                        50_000L,
                        300L,
                        0x13,
                        42L,
                        17,
                        memberships,
                        concepts,
                        new long[] {5L, 6L},
                        new long[0]);
        memberships[0] = 99;
        concepts[0] = 99L;
        check(
                facts.sourceRevision().equals("rev-1")
                        && facts.languageId() == 4
                        && facts.wordCount() == 1000
                        && facts.lexiconFingerprint() == -9L
                        && facts.totalCorpusTokens() == 50_000L
                        && facts.totalDocuments() == 300L
                        && facts.flags() == 0x13
                        && facts.corpusCount() == 42L
                        && facts.documentFrequency() == 17,
                "scalar accessors");
        check(
                Arrays.equals(facts.memberships(), new int[] {1, 2})
                        && facts.membershipCount() == 2
                        && facts.membershipAt(1) == 2,
                "memberships copied");
        check(
                Arrays.equals(facts.conceptIds(), new long[] {10L})
                        && facts.conceptCount() == 1
                        && facts.conceptAt(0) == 10L,
                "concepts copied");
        check(
                Arrays.equals(facts.subjects(), new long[] {5L, 6L})
                        && facts.subjectCount() == 2
                        && facts.subjectAt(1) == 6L,
                "subjects");
        check(
                facts.expansionWordIds().length == 0 && facts.expansionSize() == 0,
                "empty expansion");
        facts.subjects()[0] = 0L;
        check(facts.subjectAt(0) == 5L, "subjects output copied");
        expect(IndexOutOfBoundsException.class, null, () -> facts.expansionWordIdAt(0));
        expect(IndexOutOfBoundsException.class, null, () -> facts.conceptAt(1));
        expect(IndexOutOfBoundsException.class, null, () -> facts.subjectAt(-1));
        expect(IndexOutOfBoundsException.class, null, () -> facts.membershipAt(2));
        check(facts.appliesTo(4, -9L), "matching language and fingerprint");
        check(!facts.appliesTo(5, -9L), "language differs");
        check(!facts.appliesTo(4, 9L), "fingerprint differs");
        IndexWordFacts full = facts(0, 0, 0L, 0L, 0L, 0);
        check(
                full.expansionSize() == 3
                        && full.expansionWordIdAt(2) == 33L
                        && Arrays.equals(full.expansionWordIds(), new long[] {31L, 32L, 33L}),
                "expansion accessors");
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(-1, 0, 0L, 0L, 0L, 0));
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(0, -1, 0L, 0L, 0L, 0));
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(0, 0, -1L, 0L, 0L, 0));
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(0, 0, 0L, -1L, 0L, 0));
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(0, 0, 0L, 0L, -1L, 0));
        expect(
                IllegalArgumentException.class,
                "negative dictionary fact",
                () -> facts(0, 0, 0L, 0L, 0L, -1));
        expect(
                IllegalArgumentException.class,
                "sourceRevision is empty",
                () ->
                        new IndexWordFacts(
                                "",
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0,
                                0L,
                                0,
                                new int[0],
                                new long[0],
                                new long[0],
                                new long[0]));
        expect(
                NullPointerException.class,
                "memberships",
                () ->
                        new IndexWordFacts(
                                "r",
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0,
                                0L,
                                0,
                                null,
                                new long[0],
                                new long[0],
                                new long[0]));
        expect(
                NullPointerException.class,
                "conceptIds",
                () ->
                        new IndexWordFacts(
                                "r",
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0,
                                0L,
                                0,
                                new int[0],
                                null,
                                new long[0],
                                new long[0]));
        expect(
                NullPointerException.class,
                "subjects",
                () ->
                        new IndexWordFacts(
                                "r",
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0,
                                0L,
                                0,
                                new int[0],
                                new long[0],
                                null,
                                new long[0]));
        expect(
                NullPointerException.class,
                "expansionWordIds",
                () ->
                        new IndexWordFacts(
                                "r",
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0,
                                0L,
                                0,
                                new int[0],
                                new long[0],
                                new long[0],
                                null));
    }

    private static IndexWordSignal signal(
            int utf16,
            int codePoints,
            int first,
            int last,
            int frequencyRank,
            long lemma,
            int lexicalRank,
            long phonetic,
            int script,
            long stem) {
        return new IndexWordSignal(
                "rev",
                utf16,
                codePoints,
                first,
                last,
                0x2,
                frequencyRank,
                lemma,
                lexicalRank,
                0x40,
                phonetic,
                0x8,
                0xF0F0L,
                script,
                -123L,
                stem);
    }

    private static void indexWordSignal() {
        IndexWordSignal word =
                new IndexWordSignal(
                        "rev-2",
                        3,
                        2,
                        'a',
                        0x1F600,
                        0x9,
                        12,
                        100L,
                        4,
                        0x30,
                        -1L,
                        0x6,
                        0x8000_0000_0000_0001L,
                        2,
                        0x1234_5678_9ABC_DEF0L,
                        7L);
        check(
                word.sourceRevision().equals("rev-2")
                        && word.utf16Length() == 3
                        && word.codePointLength() == 2
                        && word.firstCodePoint() == 'a'
                        && word.lastCodePoint() == 0x1F600
                        && word.flags() == 0x9
                        && word.frequencyRank() == 12
                        && word.lemmaId() == 100L
                        && word.lexicalRank() == 4
                        && word.morphologyMask() == 0x30
                        && word.phoneticId() == -1L
                        && word.posMask() == 0x6
                        && word.presence64() == 0x8000_0000_0000_0001L
                        && word.scriptOrdinal() == 2
                        && word.simHash64() == 0x1234_5678_9ABC_DEF0L
                        && word.stemId() == 7L,
                "all signal accessors");
        IndexWordSignal empty = signal(0, 0, -1, -1, 0, -1L, 0, -1L, 0, -1L);
        check(
                empty.firstCodePoint() == -1
                        && empty.lastCodePoint() == -1
                        && empty.codePointLength() == 0,
                "empty word uses -1 boundaries");
        String rank = "invalid dictionary signal length or rank";
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(-1, 0, -1, -1, 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(1, -1, -1, -1, 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(1, 2, 'a', 'a', 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(1, 1, 'a', 'a', -1, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(1, 1, 'a', 'a', 0, 0L, -1, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                rank,
                () -> signal(1, 1, 'a', 'a', 0, 0L, 0, 0L, -1, 0L));
        String boundary = "invalid dictionary signal boundary";
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(1, 1, 0x110000, 'a', 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(1, 1, 'a', -2, 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(0, 0, 'a', -1, 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(0, 0, -1, 'a', 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(1, 1, -1, 'a', 0, 0L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                boundary,
                () -> signal(1, 1, 'a', -1, 0, 0L, 0, 0L, 0, 0L));
        String optional = "invalid optional dictionary signal id";
        expect(
                IllegalArgumentException.class,
                optional,
                () -> signal(1, 1, 'a', 'a', 0, -2L, 0, 0L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                optional,
                () -> signal(1, 1, 'a', 'a', 0, 0L, 0, -2L, 0, 0L));
        expect(
                IllegalArgumentException.class,
                optional,
                () -> signal(1, 1, 'a', 'a', 0, 0L, 0, 0L, 0, -2L));
        expect(
                IllegalArgumentException.class,
                "sourceRevision is empty",
                () ->
                        new IndexWordSignal(
                                null, 0, 0, -1, -1, 0, 0, 0L, 0, 0, 0L, 0, 0L, 0, 0L, 0L));
    }

    private static void tokenHashAndRanges() {
        RangeFingerprint range = new RangeFingerprint(1L, 2L, 2);
        check(
                range.first() == 1L && range.second() == 2L && range.length() == 2,
                "range fingerprint accessors");
        check(new RangeFingerprint(0L, 0L, 0).length() == 0, "empty range fingerprint");
        expect(
                IllegalArgumentException.class,
                "negative range length",
                () -> new RangeFingerprint(0L, 0L, -1));
        byte[][] tokens = {sha(1), sha(2)};
        byte[] rangeSha = sha(9);
        TokenHashPrecompute hashes =
                new TokenHashPrecompute("value-fp", 3, 5, tokens, range, rangeSha);
        tokens[0][0] = 0;
        rangeSha[0] = 0;
        check(
                hashes.valueFingerprint().equals("value-fp")
                        && hashes.rangeStart() == 3
                        && hashes.rangeEnd() == 5,
                "range accessors");
        check(
                Arrays.equals(hashes.tokenSha256()[0], sha(1))
                        && Arrays.equals(hashes.rangeSha256(), sha(9)),
                "digests deep-copied at admission");
        hashes.tokenSha256()[1][0] = 0;
        hashes.rangeSha256()[0] = 0;
        check(
                Arrays.equals(hashes.tokenSha256()[1], sha(2))
                        && Arrays.equals(hashes.rangeSha256(), sha(9)),
                "digests deep-copied at read");
        check(hashes.rangeFingerprint().equals(range), "range fingerprint identity");
        check(
                hashes.appliesTo("value-fp", 3, 5, new RangeFingerprint(1L, 2L, 2)),
                "matching scope");
        check(!hashes.appliesTo("other", 3, 5, range), "value fingerprint differs");
        check(!hashes.appliesTo("value-fp", 4, 5, range), "start differs");
        check(!hashes.appliesTo("value-fp", 3, 6, range), "end differs");
        check(
                !hashes.appliesTo("value-fp", 3, 5, new RangeFingerprint(1L, 3L, 2)),
                "range fingerprint differs");
        RangeFingerprint none = new RangeFingerprint(0L, 0L, 0);
        check(
                new TokenHashPrecompute("v", 4, 4, new byte[0][], none, sha(3)).tokenSha256().length
                        == 0,
                "empty token range");
        expect(
                IllegalArgumentException.class,
                "valueFingerprint is empty",
                () -> new TokenHashPrecompute("", 0, 0, new byte[0][], none, sha(3)));
        expect(
                IllegalArgumentException.class,
                "invalid token range",
                () -> new TokenHashPrecompute("v", -1, 0, new byte[0][], none, sha(3)));
        expect(
                IllegalArgumentException.class,
                "invalid token range",
                () -> new TokenHashPrecompute("v", 2, 1, new byte[0][], none, sha(3)));
        expect(
                NullPointerException.class,
                "tokenSha256",
                () -> new TokenHashPrecompute("v", 0, 0, null, none, sha(3)));
        expect(
                IllegalArgumentException.class,
                "token digest count does not match range",
                () -> new TokenHashPrecompute("v", 0, 1, new byte[0][], none, sha(3)));
        expect(
                NullPointerException.class,
                "tokenSha256",
                () -> new TokenHashPrecompute("v", 0, 1, new byte[][] {null}, range, sha(3)));
        expect(
                IllegalArgumentException.class,
                "tokenSha256 must be SHA-256",
                () ->
                        new TokenHashPrecompute(
                                "v", 0, 1, new byte[][] {new byte[31]}, range, sha(3)));
        expect(
                NullPointerException.class,
                "rangeFingerprint",
                () -> new TokenHashPrecompute("v", 0, 0, new byte[0][], null, sha(3)));
        expect(
                IllegalArgumentException.class,
                "range fingerprint length mismatch",
                () -> new TokenHashPrecompute("v", 0, 0, new byte[0][], range, sha(3)));
        expect(
                NullPointerException.class,
                "rangeSha256",
                () -> new TokenHashPrecompute("v", 0, 0, new byte[0][], none, null));
        expect(
                IllegalArgumentException.class,
                "rangeSha256 must be SHA-256",
                () -> new TokenHashPrecompute("v", 0, 0, new byte[0][], none, new byte[33]));

        long[] counts = {0L, 2L, 2L, 7L};
        PrefixCounts prefix = new PrefixCounts("value-fp", 4, counts);
        counts[3] = 0L;
        check(
                prefix.valueFingerprint().equals("value-fp")
                        && prefix.tokenId() == 4
                        && prefix.rangeCount(1, 3) == 5L
                        && prefix.rangeCount(0, 0) == 0L
                        && Arrays.equals(prefix.prefixCounts(), new long[] {0L, 2L, 2L, 7L}),
                "prefix counts");
        expect(IndexOutOfBoundsException.class, null, () -> prefix.rangeCount(2, 1));
        expect(
                IllegalArgumentException.class,
                "tokenId is negative",
                () -> new PrefixCounts("v", -1, new long[] {0L}));
        expect(NullPointerException.class, "prefixCounts", () -> new PrefixCounts("v", 0, null));
        expect(
                IllegalArgumentException.class,
                "must start at zero",
                () -> new PrefixCounts("v", 0, new long[0]));
        expect(
                IllegalArgumentException.class,
                "must start at zero",
                () -> new PrefixCounts("v", 0, new long[] {1L}));
        expect(
                IllegalArgumentException.class,
                "must be monotonic",
                () -> new PrefixCounts("v", 0, new long[] {0L, 3L, 2L}));
    }

    private static void frequencies() {
        Map<Integer, Integer> input = new HashMap<>(Map.of(1, 4, 3, 0));
        TokenFrequency frequency = new TokenFrequency("value-fp", input);
        input.put(2, 8);
        check(
                frequency.valueFingerprint().equals("value-fp")
                        && frequency.frequencyAt(1) == 4
                        && frequency.frequencyAt(2) == 0
                        && frequency.frequencyAt(3) == 0
                        && List.copyOf(frequency.frequencies().keySet()).equals(List.of(1, 3)),
                "copied sorted frequencies");
        expect(UnsupportedOperationException.class, null, () -> frequency.frequencies().put(5, 1));
        expect(
                IllegalArgumentException.class,
                "tokenId is negative",
                () -> frequency.frequencyAt(-3));
        expect(NullPointerException.class, "frequencies", () -> new TokenFrequency("v", null));
        Map<Integer, Integer> nullId = new HashMap<>();
        nullId.put(null, 1);
        expect(
                NullPointerException.class,
                "frequency token id",
                () -> new TokenFrequency("v", nullId));
        Map<Integer, Integer> nullCount = new HashMap<>();
        nullCount.put(1, null);
        expect(
                NullPointerException.class,
                "frequency value",
                () -> new TokenFrequency("v", nullCount));
        expect(
                IllegalArgumentException.class,
                "frequency token id is negative",
                () -> new TokenFrequency("v", Map.of(-1, 1)));
        expect(
                IllegalArgumentException.class,
                "negative frequency",
                () -> new TokenFrequency("v", Map.of(1, -1)));
        expect(
                IllegalArgumentException.class,
                "duplicate frequency key",
                () ->
                        new TokenFrequency(
                                "v", repeatingMap(List.of(Map.entry(6, 1), Map.entry(6, 2)))));
        expect(
                IllegalArgumentException.class,
                "valueFingerprint is empty",
                () -> new TokenFrequency(null, Map.of()));
    }
}
