/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3LexiconPrecompute;
import jdk.m3.text.SharedLexiconPrecomputeCatalog;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.Coordinate;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.SpellScope;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.TokenRange;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.TranslationIdentity;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.ValueScope;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.ValueToken;
import jdk.m3.text.SharedLexiconPrecomputeCatalog.WordIdentity;

import java.util.Map;

/**
 * Key-record and builder-admission contract for the typed family catalog: every key
 * refuses empty/negative components, every family refuses a value whose own identity
 * disagrees with its key, scopes are unique, and lookups are exact value-record matches.
 */
public final class SharedLexiconPrecomputeCatalogContractTest {
    private static int checks;
    private static final byte[] DIGEST = new byte[32];

    private SharedLexiconPrecomputeCatalogContractTest() { }

    public static void main(String[] args) {
        keyRecords();
        translations();
        otherFamilies();
        wordFamilies();
        System.out.println("M3JDK_PRECOMPUTE_CATALOG_CONTRACT_PASS checks=" + checks);
    }

    private static void keyRecords() {
        expect(IllegalArgumentException.class, () -> new Coordinate(-1, 0), "negative shard");
        expect(IllegalArgumentException.class, () -> new Coordinate(0, -1), "negative image row");
        check(new Coordinate(0, 0).equals(new Coordinate(0, 0)), "zero coordinate admitted");
        expect(IllegalArgumentException.class, () -> new SpellScope(null, "en", 1, 1, "src"), "null lexicon");
        expect(IllegalArgumentException.class, () -> new SpellScope("lex", "", 1, 1, "src"), "empty language");
        expect(IllegalArgumentException.class, () -> new SpellScope("lex", "en", -1, 1, "src"), "negative edit distance");
        expect(IllegalArgumentException.class, () -> new SpellScope("lex", "en", 1, -1, "src"), "negative prefix length");
        Coordinate at = new Coordinate(1, 2);
        expect(NullPointerException.class, () -> new TokenRange(null, "v", "t", 0, 1), "null range coordinate");
        expect(IllegalArgumentException.class, () -> new TokenRange(at, "v", "t", -1, 1), "negative range start");
        expect(IllegalArgumentException.class, () -> new TokenRange(at, "v", "t", 2, 1), "inverted range");
        check(new TokenRange(at, "v", "t", 3, 3).endExclusive() == 3, "empty range admitted");
        expect(IllegalArgumentException.class, () -> new TokenRange(at, "v", "", 0, 1), "empty tokenizer version");
        expect(IllegalArgumentException.class, () -> new ValueToken(at, "v", -1), "negative token id");
        expect(NullPointerException.class, () -> new ValueToken(null, "v", 0), "null value-token coordinate");
        expect(IllegalArgumentException.class, () -> new ValueScope(at, ""), "empty value fingerprint");
        expect(IllegalArgumentException.class, () -> new TranslationIdentity("s", "r", "en", "hi", "lex", null),
                "null source fingerprint");
        expect(IllegalArgumentException.class, () -> new WordIdentity("s", "r", "", "rev"), "empty lexeme");
    }

    private static void translations() {
        TranslationIdentity identity = new TranslationIdentity("source", "record", "en", "hi", "lex", "fp");
        var projection = new M3LexiconPrecompute.TranslationProjection("lex", "en", "hi", "fp", new int[] {0, 2, 1});
        SharedLexiconPrecomputeCatalog.Builder builder = SharedLexiconPrecomputeCatalog.builder();
        check(builder.translation(identity, projection) == builder, "builder is fluent");
        expectMismatch(() -> builder.translation(identity,
                new M3LexiconPrecompute.TranslationProjection("lex", "fr", "hi", "fp", new int[0])), "source language");
        expectMismatch(() -> builder.translation(identity,
                new M3LexiconPrecompute.TranslationProjection("lex", "en", "fr", "fp", new int[0])), "target language");
        expectMismatch(() -> builder.translation(identity,
                new M3LexiconPrecompute.TranslationProjection("lex-2", "en", "hi", "fp", new int[0])), "lexicon");
        expectMismatch(() -> builder.translation(identity,
                new M3LexiconPrecompute.TranslationProjection("lex", "en", "hi", "fp-2", new int[0])), "source fingerprint");
        expect(IllegalArgumentException.class, () -> builder.translation(identity, projection), "duplicate translation scope");
        expect(NullPointerException.class, () -> builder.translation(null, projection), "null translation key");
        expect(NullPointerException.class, () -> builder.translation(identity, null), "null translation value");
        SharedLexiconPrecomputeCatalog catalog = builder.build();
        check(catalog.translationAt(new TranslationIdentity("source", "record", "en", "hi", "lex", "fp"))
                .orElseThrow().mappedTokenCount() == 2, "translation found by equal identity");
        check(catalog.translationAt(new TranslationIdentity("source", "record-2", "en", "hi", "lex", "fp")).isEmpty(),
                "record id is part of the translation scope");
        builder.translation(new TranslationIdentity("source", "record-2", "en", "hi", "lex", "fp"), projection);
        check(catalog.translationAt(new TranslationIdentity("source", "record-2", "en", "hi", "lex", "fp")).isEmpty(),
                "built catalog is a snapshot of the builder");
    }

    private static void otherFamilies() {
        SharedLexiconPrecomputeCatalog.Builder builder = SharedLexiconPrecomputeCatalog.builder();
        SpellScope spellScope = new SpellScope("lex", "en", 2, 4, "src");
        var spell = new M3LexiconPrecompute.SpellIndex("lex", "en", 2, 4, "src", Map.of("a", new int[] {1}), Map.of(1, 3L));
        builder.spell(spellScope, spell);
        expectMismatch(() -> builder.spell(spellScope,
                new M3LexiconPrecompute.SpellIndex("lex", "en", 1, 4, "src", Map.of(), Map.of())), "edit distance");
        expectMismatch(() -> builder.spell(spellScope,
                new M3LexiconPrecompute.SpellIndex("lex", "en", 2, 3, "src", Map.of(), Map.of())), "prefix length");
        expect(IllegalArgumentException.class, () -> builder.spell(spellScope, spell), "duplicate spell scope");

        Coordinate at = new Coordinate(1, 2);
        TokenRange range = new TokenRange(at, "value", "tok-1", 0, 1);
        var hashes = new M3LexiconPrecompute.TokenHashPrecompute("value", 0, 1, new byte[][] {DIGEST},
                new M3LexiconPrecompute.RangeFingerprint(5L, 6L, 1), DIGEST);
        builder.tokenHashes(range, hashes);
        expectMismatch(() -> builder.tokenHashes(new TokenRange(at, "value", "tok-1", 0, 2), hashes), "range end");
        expectMismatch(() -> builder.tokenHashes(new TokenRange(at, "other", "tok-1", 0, 1), hashes), "range value");

        ValueToken token = new ValueToken(at, "value", 7);
        var prefix = new M3LexiconPrecompute.PrefixCounts("value", 7, new long[] {0, 1, 3});
        builder.prefixCounts(token, prefix);
        expectMismatch(() -> builder.prefixCounts(new ValueToken(at, "value", 8), prefix), "prefix token");

        ValueScope scope = new ValueScope(at, "value");
        var frequency = new M3LexiconPrecompute.TokenFrequency("value", Map.of(4, 9));
        builder.tokenFrequency(scope, frequency);
        expectMismatch(() -> builder.tokenFrequency(new ValueScope(at, "other"), frequency), "frequency value");

        SharedLexiconPrecomputeCatalog catalog = builder.build();
        check(catalog.spellAt(new SpellScope("lex", "en", 2, 4, "src")).orElseThrow().frequencyAt(1) == 3L, "spell lookup");
        check(catalog.tokenHashesAt(new TokenRange(new Coordinate(1, 2), "value", "tok-1", 0, 1)).isPresent(), "hash lookup");
        check(catalog.tokenHashesAt(new TokenRange(new Coordinate(1, 2), "value", "tok-2", 0, 1)).isEmpty(),
                "tokenizer version is part of the range scope");
        check(catalog.prefixCountsAt(new ValueToken(new Coordinate(1, 2), "value", 7)).orElseThrow().rangeCount(0, 2) == 3L,
                "prefix lookup");
        check(catalog.tokenFrequencyAt(new ValueScope(new Coordinate(1, 3), "value")).isEmpty(),
                "image row is part of the frequency scope");
        check(catalog.tokenFrequencyAt(scope).orElseThrow().frequencyAt(4) == 9, "frequency lookup");
        expect(NullPointerException.class, () -> catalog.prefixCountsAt(null), "null lookup");
    }

    private static void wordFamilies() {
        SharedLexiconPrecomputeCatalog.Builder builder = SharedLexiconPrecomputeCatalog.builder();
        var facts = new M3LexiconPrecompute.IndexWordFacts("rev-1", 7, 3, 0x1234L, 90L, 11L, 5, 44L, 8,
                new int[] {2}, new long[] {101L}, new long[0], new long[0]);
        WordIdentity dictionary = new WordIdentity("dictlang.dictionary", "record", "word", "rev-1");
        expect(IllegalArgumentException.class, () -> builder.wordFacts(
                new WordIdentity("dictlang.frequency", "record", "word", "rev-1"), facts), "facts source family");
        expect(IllegalArgumentException.class, () -> builder.wordFacts(
                new WordIdentity("dictlang.dictionary", "record", "word", "rev-2"), facts), "facts revision");
        builder.wordFacts(dictionary, facts);
        var signal = new M3LexiconPrecompute.IndexWordSignal("rev-1", 4, 4, 'w', 'd', 0, 1, -1L, 2, 0, -1L,
                0, 0L, 0, 0L, -1L);
        WordIdentity frequency = new WordIdentity("dictlang.frequency", "record", "word", "rev-1");
        expect(IllegalArgumentException.class, () -> builder.wordSignal(dictionary, signal), "signal source family");
        expect(IllegalArgumentException.class, () -> builder.wordSignal(
                new WordIdentity("dictlang.frequency", "record", "word", "rev-0"), signal), "signal revision");
        builder.wordSignal(frequency, signal);
        expect(IllegalArgumentException.class, () -> builder.wordSignal(frequency, signal), "duplicate signal scope");
        SharedLexiconPrecomputeCatalog catalog = builder.build();
        check(catalog.wordFactsAt(dictionary).orElseThrow().wordCount() == 3, "word facts lookup");
        check(catalog.wordSignalAt(frequency).orElseThrow().utf16Length() == 4, "word signal lookup");
        check(catalog.wordFactsAt(frequency).isEmpty(), "facts and signals keep separate source families");
    }

    private static void expectMismatch(Runnable action, String why) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            if (!String.valueOf(expected.getMessage()).contains("does not match"))
                throw new AssertionError(why + ": unexpected message", expected);
            return;
        }
        throw new AssertionError(why + ": mismatched identity accepted");
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
}
