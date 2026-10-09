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
    private static final String TRANSLATION_SOURCE_REVISION = "64a2ea61c73b548413fed6686a9daeeb0b9b0564";
    private static final String TRANSLATION_SOURCE_BLOB_SHA = "bb72c00e36f1835d824a34ab398b1fe5aadb1cb3";
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
        M3LexiconPrecompute.TranslationProjection boundTranslation =
                new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[]{11, 0, 19}, 3,
                        TRANSLATION_SOURCE_REVISION, TRANSLATION_SOURCE_BLOB_SHA);
        check(boundTranslation.sourceBound());
        SharedLexiconPrecomputeCatalog.TranslationIdentity boundIdentity =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-bound", "en", "hi", "lex-1", "src-1",
                        TRANSLATION_SOURCE_REVISION, TRANSLATION_SOURCE_BLOB_SHA);
        check(SharedLexiconPrecomputeCatalog.builder().translation(
                boundIdentity, boundTranslation).build()
                .translationAt(boundIdentity).orElseThrow().sourceBound());
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().translation(
                        new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                                "translate.rows", "row-bound", "en", "hi", "lex-1", "src-1",
                                TRANSLATION_SOURCE_REVISION,
                                "0000000000000000000000000000000000000000"),
                        boundTranslation));

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
        System.out.println("M3JDK_TYPED_PRECOMPUTE_PASS checks=" + checks);
    }
}
