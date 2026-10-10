/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LanguageNgramPrecompute;

import java.util.LinkedHashMap;
import java.util.Map;

public final class M3LanguageNgramPrecomputeTest {
    private static int checks;

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

    public static void main(String[] args) {
        Map<String, Long> unigrams = new LinkedHashMap<>();
        unigrams.put("the", 1000L);
        unigrams.put("cat", 10L);
        unigrams.put("cot", 100L);
        Map<M3LanguageNgramPrecompute.Bigram, Long> bigrams = new LinkedHashMap<>();
        M3LanguageNgramPrecompute.Bigram theCat =
                new M3LanguageNgramPrecompute.Bigram("the", "cat");
        M3LanguageNgramPrecompute.Bigram theCot =
                new M3LanguageNgramPrecompute.Bigram("the", "cot");
        bigrams.put(theCat, 1000L);
        bigrams.put(theCot, 1L);
        M3LanguageNgramPrecompute model = new M3LanguageNgramPrecompute(
                "context-fixture", "rev-1", "en", "sha256:fixture", "unicode-v1",
                unigrams, bigrams);

        check(model.unigramCount() == 3);
        check(model.bigramCount() == 2);
        check(model.totalUnigrams() == 1110L);
        check(model.unigramFrequency("cat") == 10L);
        check(model.bigramFrequency("the", "cat") == 1000L);
        check(model.bigramFrequency("missing", "cat") == 0L);
        check(model.hasBigrams());
        check(model.appliesTo("context-fixture", "rev-1", "en",
                "sha256:fixture", "unicode-v1"));
        check(!model.appliesTo("context-fixture", "rev-2", "en",
                "sha256:fixture", "unicode-v1"));
        check(model.conditionalProbability("the", "cat")
                > model.conditionalProbability("the", "cot"));
        check(model.contextScore("the", "cat", null) > 0.0d);
        check(model.contextScore(null, "cat", null) == 0.0d);

        unigrams.put("new", 999L);
        bigrams.put(new M3LanguageNgramPrecompute.Bigram("new", "entry"), 2L);
        check(model.unigramFrequency("new") == 0L);
        check(model.bigramCount() == 2);
        expect(UnsupportedOperationException.class, () -> model.unigrams().put("x", 1L));
        expect(UnsupportedOperationException.class, () -> model.bigrams().put(theCat, 2L));
        expect(IllegalArgumentException.class, () ->
                new M3LanguageNgramPrecompute.Bigram(" ", "cat"));
        expect(IllegalArgumentException.class, () -> new M3LanguageNgramPrecompute(
                "source", "revision", "en", "fingerprint", "tokenizer",
                Map.of("bad", -1L), Map.of()));
        expect(IllegalArgumentException.class, () -> new M3LanguageNgramPrecompute(
                "source", "revision", "en", "fingerprint", "tokenizer",
                Map.of(), Map.of(new M3LanguageNgramPrecompute.Bigram("a", "b"), -1L)));
        System.out.println("M3JDK_LANGUAGE_NGRAM_PRECOMPUTE_PASS checks=" + checks);
    }
}
