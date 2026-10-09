/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

import java.util.Arrays;
import java.util.Map;

/**
 * Direct receiver proof for every typed Synexia precompute family.
 * The file-sidecar decoder has a separate proof; this one verifies the
 * immutable in-memory catalog and scope/identity isolation.
 */
public final class M3TypedPrecomputeReceiverTest {
    private static int checks;

    public static void main(String[] args) {
        var catalog = buildCatalog();

        rejectsMismatchedOwners();

        var translation = new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                "dictlang.translation", "row-1", "en", "hi", "lex-1", "src-1");
        check(catalog.translationAt(translation).orElseThrow().translatedTokenIdAt(1) == 7,
                "translation receiver");
        check(catalog.translationAt(new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                "dictlang.translation", "row-1", "en", "fr", "lex-1", "src-1")).isEmpty(),
                "translation target isolation");

        var spell = new SharedLexiconPrecomputeCatalog.SpellScope(
                "lex-1", "en", 2, 4, "spell-1");
        check(catalog.spellAt(spell).orElseThrow().frequencyAt(3) == 4L,
                "spell receiver");

        var coordinate = new SharedLexiconPrecomputeCatalog.Coordinate(1, 2);
        var range = new SharedLexiconPrecomputeCatalog.TokenRange(
                coordinate, "value-1", "tokenizer-1", 0, 2);
        check(catalog.tokenHashesAt(range).orElseThrow().tokenSha256().length == 2,
                "token hash receiver");

        var token = new SharedLexiconPrecomputeCatalog.ValueToken(coordinate, "value-1", 7);
        check(catalog.prefixCountsAt(token).orElseThrow().rangeCount(0, 2) == 2L,
                "prefix receiver");

        var value = new SharedLexiconPrecomputeCatalog.ValueScope(coordinate, "value-1");
        check(catalog.tokenFrequencyAt(value).orElseThrow().frequencyAt(7) == 3,
                "token frequency receiver");

        var number = new SharedLexiconPrecomputeCatalog.NumberIdentity(
                "dictlang.numbers.0-10000", "42", 42, "en");
        var numberValue = catalog.numberAt(number).orElseThrow();
        check(numberValue.value() == 42, "number receiver");
        check(numberValue.spelling().equals("42"), "number canonical spelling");
        check(numberValue.languageTag().equals("en"), "number language tag");
        check(catalog.numberAt(new SharedLexiconPrecomputeCatalog.NumberIdentity(
                "dictlang.numbers.0-10000", "42", 42, "hi")).isEmpty(),
                "number language isolation");

        var unit = new SharedLexiconPrecomputeCatalog.SiUnitIdentity(
                "dictlang.si-units", "meter");
        var si = catalog.siUnitAt(unit).orElseThrow();
        check(si.decimalExponent() == -6, "SI-unit exponent");
        check(si.dimensionPacked() == 0x01020304050607L, "SI-unit dimension");
        check(si.offset() == 273.15d, "SI-unit offset");
        check(si.prefixable(), "SI-unit prefixability");
        check(catalog.siUnitAt(new SharedLexiconPrecomputeCatalog.SiUnitIdentity(
                "dictlang.si-units", "missing")).isEmpty(),
                "SI-unit record isolation");

        var tokenHashes = catalog.tokenHashesAt(range).orElseThrow();
        byte[] digest = tokenHashes.rangeSha256();
        digest[0] ^= 1;
        check(tokenHashes.rangeSha256()[0] != digest[0], "range digest defensive copy");
        byte[][] tokenDigests = tokenHashes.tokenSha256();
        tokenDigests[0][0] ^= 1;
        check(tokenHashes.tokenSha256()[0][0] != tokenDigests[0][0],
                "token digest matrix defensive copy");
        check(tokenHashes.rangeFingerprint().first() == 11L
                        && tokenHashes.rangeFingerprint().second() == 22L
                        && tokenHashes.rangeFingerprint().length() == 2,
                "range fingerprint fields");

        System.out.println("M3JDK_TYPED_RECEIVER_MATRIX_PASS checks=" + checks + " families=7");
    }

    private static void rejectsMismatchedOwners() {
        var spellScope = new SharedLexiconPrecomputeCatalog.SpellScope(
                "lex-1", "en", 2, 4, "spell-1");
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().spell(spellScope,
                        new M3LexiconPrecompute.SpellIndex(
                                "other-lexicon", "en", 2, 4, "spell-1",
                                Map.of("a", new int[] {1, 3}), Map.of(3, 4L))));

        var coordinate = new SharedLexiconPrecomputeCatalog.Coordinate(1, 2);
        var range = new SharedLexiconPrecomputeCatalog.TokenRange(
                coordinate, "value-1", "tokenizer-1", 0, 2);
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().tokenHashes(range,
                        new M3LexiconPrecompute.TokenHashPrecompute(
                                "other-value", 0, 2,
                                new byte[][] {digest((byte) 0x11), digest((byte) 0x22)},
                                new M3LexiconPrecompute.RangeFingerprint(11L, 22L, 2),
                                digest((byte) 0x11))));

        var token = new SharedLexiconPrecomputeCatalog.ValueToken(coordinate, "value-1", 7);
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().prefixCounts(token,
                        new M3LexiconPrecompute.PrefixCounts(
                                "other-value", 7, new long[] {0, 1, 2})));

        var value = new SharedLexiconPrecomputeCatalog.ValueScope(coordinate, "value-1");
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().tokenFrequency(value,
                        new M3LexiconPrecompute.TokenFrequency(
                                "other-value", Map.of(7, 3))));
    }

    private static SharedLexiconPrecomputeCatalog buildCatalog() {
        byte[] a = digest((byte) 0x11);
        byte[] b = digest((byte) 0x22);
        var translationKey = new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                "dictlang.translation", "row-1", "en", "hi", "lex-1", "src-1");
        var spellKey = new SharedLexiconPrecomputeCatalog.SpellScope(
                "lex-1", "en", 2, 4, "spell-1");
        var coordinate = new SharedLexiconPrecomputeCatalog.Coordinate(1, 2);
        var rangeKey = new SharedLexiconPrecomputeCatalog.TokenRange(
                coordinate, "value-1", "tokenizer-1", 0, 2);
        var tokenKey = new SharedLexiconPrecomputeCatalog.ValueToken(coordinate, "value-1", 7);
        var valueKey = new SharedLexiconPrecomputeCatalog.ValueScope(coordinate, "value-1");
        var numberKey = new SharedLexiconPrecomputeCatalog.NumberIdentity(
                "dictlang.numbers.0-10000", "42", 42, "en");
        var unitKey = new SharedLexiconPrecomputeCatalog.SiUnitIdentity(
                "dictlang.si-units", "meter");

        return SharedLexiconPrecomputeCatalog.builder()
                .translation(translationKey, new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[] {2, 7}, 2))
                .spell(spellKey, new M3LexiconPrecompute.SpellIndex(
                        "lex-1", "en", 2, 4, "spell-1",
                        Map.of("a", new int[] {1, 3}), Map.of(3, 4L)))
                .tokenHashes(rangeKey, new M3LexiconPrecompute.TokenHashPrecompute(
                        "value-1", 0, 2, new byte[][] {a, b},
                        new M3LexiconPrecompute.RangeFingerprint(11L, 22L, 2), a))
                .prefixCounts(tokenKey, new M3LexiconPrecompute.PrefixCounts(
                        "value-1", 7, new long[] {0, 1, 2}))
                .tokenFrequency(valueKey, new M3LexiconPrecompute.TokenFrequency(
                        "value-1", Map.of(7, 3)))
                .number(numberKey, M3LexiconPrecompute.NumberPrecompute.canonical(42, "en"))
                .siUnit(unitKey, new M3LexiconPrecompute.SiUnitPrecompute(
                        -6, 0x01020304050607L, 273.15d, true))
                .build();
    }

    private static byte[] digest(byte fill) {
        byte[] result = new byte[32];
        Arrays.fill(result, fill);
        return result;
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + kind.getSimpleName());
        } catch (Throwable failure) {
            check(kind.isInstance(failure), "wrong exception: " + failure);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
