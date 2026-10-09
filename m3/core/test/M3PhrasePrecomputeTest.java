/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import com.m3.text.M3PhrasePrecompute;
import java.util.Arrays;

public final class M3PhrasePrecomputeTest {
    private static int checks;

    public static void main(String[] args) {
        M3PhrasePrecompute.Scope scope = new M3PhrasePrecompute.Scope(
                "translate.index-phrases", "phrases-1", "synexia-r1", "lexicon-sha");
        int[] source = {1, 2, 3};
        int[] replacement = {8};
        M3PhrasePrecompute.Phrase longest =
                new M3PhrasePrecompute.Phrase(source, replacement);
        source[0] = 99;
        replacement[0] = 99;

        M3PhrasePrecompute.Catalog catalog = M3PhrasePrecompute.builder(scope)
                .put(new M3PhrasePrecompute.Phrase(new int[]{1, 2}, new int[]{9}))
                .put(longest)
                .put(new M3PhrasePrecompute.Phrase(new int[]{4}, new int[]{7}))
                .build();

        check(M3PhrasePrecompute.SCHEMA_VERSION.equals("m3phrase-v1"));
        check(catalog.phraseCount() == 3);
        check(catalog.maxSourceLength() == 3);
        check(Arrays.equals(catalog.rewrite(new int[]{1, 2, 3, 4, 5}),
                new int[]{8, 7, 5}));
        check(catalog.longestMatchAt(new int[]{1, 2, 3}, 0).orElseThrow()
                .consumedLength() == 3);
        check(Arrays.equals(catalog.longestMatchAt(new int[]{1, 2, 3}, 0)
                .orElseThrow().replacementTokenIds(), new int[]{8}));
        check(catalog.longestMatchAt(new int[]{1, 2, 3}, 2).isEmpty());
        check(catalog.longestMatchAt(new int[]{4}, 0).orElseThrow()
                .consumedLength() == 1);

        int[] returned = longest.sourceTokenIds();
        returned[0] = 77;
        int[] returnedReplacement = longest.replacementTokenIds();
        returnedReplacement[0] = 77;
        check(Arrays.equals(catalog.rewrite(new int[]{1, 2, 3}), new int[]{8}));
        check(catalog.scope().equals(scope));

        M3PhrasePrecompute.Catalog emptyReplacement = M3PhrasePrecompute.builder(scope)
                .put(new M3PhrasePrecompute.Phrase(new int[]{6, 7}, new int[0]))
                .build();
        check(Arrays.equals(emptyReplacement.rewrite(new int[]{6, 7, 4}), new int[]{7}));
        check(emptyReplacement.longestMatchAt(new int[]{6, 7}, 0).orElseThrow()
                .replacementTokenIds().length == 0);

        expect(IllegalArgumentException.class, () ->
                new M3PhrasePrecompute.Phrase(new int[0], new int[0]));
        expect(IllegalArgumentException.class, () ->
                new M3PhrasePrecompute.Scope("", "x", "r", "f"));
        M3PhrasePrecompute.Catalog overwritten = M3PhrasePrecompute.builder(scope)
                .put(new M3PhrasePrecompute.Phrase(new int[]{1}, new int[]{2}))
                .put(new M3PhrasePrecompute.Phrase(new int[]{1}, new int[]{3}))
                .build();
        check(overwritten.phraseCount() == 1);
        check(Arrays.equals(overwritten.rewrite(new int[]{1}), new int[]{3}));

        M3PhrasePrecompute.Catalog empty = M3PhrasePrecompute.builder(scope).build();
        check(empty.phraseCount() == 0);
        check(Arrays.equals(empty.rewrite(new int[]{4, 5}), new int[]{4, 5}));
        expect(IndexOutOfBoundsException.class, () ->
                catalog.longestMatchAt(new int[]{1}, 2));

        System.out.println("M3_PHRASE_PRECOMPUTE_PASS checks=" + checks
                + " phrases=" + catalog.phraseCount());
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }
}
