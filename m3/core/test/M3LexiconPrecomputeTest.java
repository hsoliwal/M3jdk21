/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class M3LexiconPrecomputeTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static byte[] digest(int seed) {
        byte[] value = new byte[32];
        Arrays.fill(value, (byte)seed);
        return value;
    }

    public static void main(String[] args) {
        M3LexiconPrecompute.TranslationProjection translation =
                new M3LexiconPrecompute.TranslationProjection("lex-1", "en", "hi", "src-1",
                        new int[]{11, 0, 19});
        int[] translated = translation.translatedTokenIds();
        translated[0] = 99;
        check(translation.translatedTokenIdAt(0) == 11);
        check(translation.mappedTokenCount() == 3);
        check(translation.appliesTo("lex-1", "en", "hi", "src-1"));
        check(!translation.appliesTo("lex-2", "en", "hi", "src-1"));
        check(!translation.appliesTo("lex-1", "en", "hi", "src-2"));

        Map<String, int[]> deletes = new LinkedHashMap<>();
        deletes.put("", new int[]{1});
        deletes.put("helo", new int[]{2, 7});
        deletes.put("hello", new int[]{2});
        Map<Integer, Long> spellFrequencies = new LinkedHashMap<>();
        spellFrequencies.put(2, 40L);
        spellFrequencies.put(7, 3L);
        M3LexiconPrecompute.SpellIndex spell = new M3LexiconPrecompute.SpellIndex(
                "lex-1", "en", 2, 7, "spell-1", deletes, spellFrequencies);
        int[] spellIds = spell.tokenIdsForDelete("helo");
        spellIds[0] = 99;
        check(Arrays.equals(spell.tokenIdsForDelete("helo"), new int[]{2, 7}));
        check(spell.frequencyAt(2) == 40L && spell.frequencyAt(404) == 0L);
        check(List.copyOf(spell.deleteToTokenIds().keySet()).equals(List.of("", "hello", "helo")));
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.SpellIndex(
                "lex-1", "en", 2, 7, "spell-1", Map.of("x", new int[]{2, 2}), Map.of()));

        M3LexiconPrecompute.RangeFingerprint range =
                new M3LexiconPrecompute.RangeFingerprint(7L, 13L, 3);
        M3LexiconPrecompute.TokenHashPrecompute hashes =
                new M3LexiconPrecompute.TokenHashPrecompute("value-1", 0, 3,
                        new byte[][]{digest(1), digest(2), digest(3)}, range, digest(4));
        byte[][] tokenDigests = hashes.tokenSha256();
        tokenDigests[0][0] = 99;
        check(hashes.tokenSha256()[0][0] == 1);
        check(hashes.appliesTo("value-1", 0, 3, range));
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.TokenHashPrecompute(
                "value-1", 0, 2, new byte[][]{digest(1)}, range, digest(4)));

        M3LexiconPrecompute.PrefixCounts prefixes =
                new M3LexiconPrecompute.PrefixCounts("value-1", 7, new long[]{0, 1, 1, 2, 3});
        check(prefixes.rangeCount(1, 4) == 2L);
        long[] prefixCopy = prefixes.prefixCounts();
        prefixCopy[1] = 99;
        check(prefixes.prefixCounts()[1] == 1L);
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.PrefixCounts(
                "value-1", 7, new long[]{1, 1}));
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.PrefixCounts(
                "value-1", 7, new long[]{0, 2, 1}));
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.PrefixCounts(
                "value-1", 7, new long[0]));
        expect(IndexOutOfBoundsException.class, () -> prefixes.rangeCount(0, 6));

        M3LexiconPrecompute.TokenFrequency frequency = new M3LexiconPrecompute.TokenFrequency(
                "value-1", Map.of(3, 1, 1, 2, 2, 2));
        check(frequency.frequencyAt(1) == 2 && frequency.frequencyAt(9) == 0);
        check(List.copyOf(frequency.frequencies().keySet()).equals(List.of(1, 2, 3)));
        SharedLexiconPrecomputeCatalog.TranslationIdentity source =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-1", "en", "hi", "lex-1", "src-1");
        SharedLexiconPrecomputeCatalog.TranslationIdentity reverseSource =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-1", "hi", "en", "lex-1", "src-1");
        SharedLexiconPrecomputeCatalog.TranslationIdentity sourceVariant =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows.alt", "row-1", "en", "hi", "lex-1", "src-1");
        SharedLexiconPrecomputeCatalog.TranslationIdentity recordVariant =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-2", "en", "hi", "lex-1", "src-1");
        M3LexiconPrecompute.TranslationProjection reverseTranslation =
                new M3LexiconPrecompute.TranslationProjection("lex-1", "hi", "en", "src-1",
                        new int[]{21, 0, 29});
        SharedLexiconPrecomputeCatalog.Coordinate coordinate =
                new SharedLexiconPrecomputeCatalog.Coordinate(2, 7);
        SharedLexiconPrecomputeCatalog.Builder catalogBuilder = SharedLexiconPrecomputeCatalog.builder()
                .translation(source, translation)
                .translation(reverseSource, reverseTranslation)
                .translation(sourceVariant, translation)
                .translation(recordVariant, translation)
                .spell(new SharedLexiconPrecomputeCatalog.SpellScope(
                        "lex-1", "en", 2, 7, "spell-1"), spell)
                .tokenHashes(new SharedLexiconPrecomputeCatalog.TokenRange(
                        coordinate, "value-1", "tok-v1", 0, 3), hashes)
                .prefixCounts(new SharedLexiconPrecomputeCatalog.ValueToken(
                        coordinate, "value-1", 7), prefixes)
                .tokenFrequency(new SharedLexiconPrecomputeCatalog.ValueScope(
                        coordinate, "value-1"), frequency);
        SharedLexiconPrecomputeCatalog catalog = catalogBuilder.build();
        check(catalog.translationAt(source).orElseThrow().mappedTokenCount() == 3);
        check(catalog.translationAt(reverseSource).orElseThrow().translatedTokenIdAt(0) == 21);
        check(catalog.translationAt(sourceVariant).orElseThrow().translatedTokenIdAt(1) == 0);
        check(catalog.translationAt(recordVariant).orElseThrow().translatedTokenIdAt(2) == 19);
        expect(IllegalArgumentException.class, () -> catalogBuilder.translation(
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-1", "en", "hi", "lex-2", "src-1"),
                translation));
        expect(IllegalArgumentException.class, () -> catalogBuilder.translation(
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-1", "en", "hi", "lex-1", "src-2"),
                translation));
        check(catalog.spellAt(new SharedLexiconPrecomputeCatalog.SpellScope(
                "lex-1", "en", 2, 7, "spell-1")).isPresent());
        check(catalog.tokenHashesAt(new SharedLexiconPrecomputeCatalog.TokenRange(
                coordinate, "value-1", "tok-v1", 0, 3)).isPresent());
        check(catalog.prefixCountsAt(new SharedLexiconPrecomputeCatalog.ValueToken(
                coordinate, "value-1", 7)).orElseThrow().rangeCount(1, 4) == 2L);
        check(catalog.tokenFrequencyAt(new SharedLexiconPrecomputeCatalog.ValueScope(
                coordinate, "value-1")).orElseThrow().frequencyAt(2) == 2);
        expect(IllegalArgumentException.class, () -> catalogBuilder.translation(source, translation));

        M3LexiconPrecompute.IndexWordFacts wordFacts = new M3LexiconPrecompute.IndexWordFacts(
                7, 3, 0x1234L, 90L, 11L, 5, 44L, 8,
                new int[]{2, 4}, new long[]{101L, 103L}, new long[]{9L},
                new long[]{501L, 503L});
        int[] memberships = wordFacts.memberships();
        memberships[0] = 99;
        check(wordFacts.membershipAt(0) == 2 && wordFacts.membershipCount() == 2);
        check(wordFacts.conceptAt(1) == 103L && wordFacts.subjectAt(0) == 9L);
        check(wordFacts.expansionWordIdAt(1) == 503L);
        check(wordFacts.appliesTo(7, 0x1234L) && !wordFacts.appliesTo(8, 0x1234L));
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.IndexWordFacts(
                7, 3, 0x1234L, -1L, 11L, 5, 44L, 8,
                new int[0], new long[0], new long[0], new long[0]));

        M3LexiconPrecompute.IndexWordSignal wordSignal = new M3LexiconPrecompute.IndexWordSignal(
                5, 4, 0x00e9, 0x0065, 3, 12, -1L, 8, 16, 17L,
                32, 0x55L, 4, 0x77L, 19L);
        check(wordSignal.utf16Length() == 5 && wordSignal.codePointLength() == 4);
        check(wordSignal.frequencyRank() == 12 && wordSignal.lexicalRank() == 8);
        check(wordSignal.lemmaId() == -1L && wordSignal.phoneticId() == 17L
                && wordSignal.stemId() == 19L);
        expect(IllegalArgumentException.class, () -> new M3LexiconPrecompute.IndexWordSignal(
                1, 1, -1, -1, 0, 0, -1L, 0, 0, -1L, 0, 0L, 0, 0L, -1L));

        SharedLexiconPrecomputeCatalog.WordIdentity wordKey =
                new SharedLexiconPrecomputeCatalog.WordIdentity(
                        "dictlang.dictionary", "7", "café");
        SharedLexiconPrecomputeCatalog.WordIdentity signalKey =
                new SharedLexiconPrecomputeCatalog.WordIdentity(
                        "dictlang.frequency", "7", "café");
        SharedLexiconPrecomputeCatalog wordCatalog = SharedLexiconPrecomputeCatalog.builder()
                .wordFacts(wordKey, wordFacts)
                .wordSignal(signalKey, wordSignal)
                .build();
        check(wordCatalog.wordFactsAt(wordKey).orElseThrow().wordCount() == 3);
        check(wordCatalog.wordSignalAt(signalKey).orElseThrow().frequencyRank() == 12);
        check(wordCatalog.wordFactsAt(signalKey).isEmpty());
        expect(IllegalArgumentException.class, () -> SharedLexiconPrecomputeCatalog.builder()
                .wordFacts(wordKey, wordFacts).wordFacts(wordKey, wordFacts));
        expect(IllegalArgumentException.class, () -> SharedLexiconPrecomputeCatalog.builder()
                .wordFacts(signalKey, wordFacts));
        expect(IllegalArgumentException.class, () -> SharedLexiconPrecomputeCatalog.builder()
                .wordSignal(wordKey, wordSignal));

        System.out.println("M3JDK_TYPED_PRECOMPUTE_PASS checks=" + checks);
    }
}
