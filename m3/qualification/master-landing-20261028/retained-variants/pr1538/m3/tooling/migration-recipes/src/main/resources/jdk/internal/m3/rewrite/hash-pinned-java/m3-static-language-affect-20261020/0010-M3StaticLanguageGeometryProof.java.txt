// SPDX-License-Identifier: Apache-2.0
package com.m3.lexicon;

/** Dependency-free Java/JNI differential proof for static language geometry and Luhn scoring. */
public final class M3StaticLanguageGeometryProof {
    private static long checks;

    private M3StaticLanguageGeometryProof() { }

    public static void main(String[] args) {
        boolean nativeRequired = args.length != 0 && "--native-required".equals(args[0]);
        M3StaticLanguageGeometry geometry = fixture();

        equal(6, geometry.wordCount(), "word count");
        equal(1049L, geometry.totalFrequency(), "frequency total");
        equal(1, geometry.conceptOverlap(2, 3), "concept overlap");
        equal(1, geometry.subjectOverlap(2, 3), "subject overlap");
        equal(1, geometry.classOverlap(2, 3), "class overlap");
        equal(12L, geometry.pairCount(2, 3), "pair count");
        equal(M3StaticLanguageGeometry.Q20_ONE / 2,
                geometry.pairAssociationQ20(2, 3), "pair association");

        var band = geometry.defaultLuhnFrequencyBand();
        check(!band.contains(1000L), "dominant word excluded");
        check(!band.contains(1L), "singleton excluded");
        check(band.contains(20L), "content word retained");

        int[] words = {1, 1, 1, 2, 3, 4, 2, 1, 1, 3, 6};
        int[] ends = {3, 6, 10, 11};
        M3LuhnSummarizer.Config config = new M3LuhnSummarizer.Config(
                new M3StaticLanguageGeometry.FrequencyBand(2L, 100L),
                1,
                M3LuhnSummarizer.WEIGHT_ONE,
                M3LuhnSummarizer.WEIGHT_ONE / 4,
                M3LuhnSummarizer.WEIGHT_ONE / 8,
                0L,
                0L);
        M3LuhnSummarizer summarizer = new M3LuhnSummarizer(geometry, config);
        M3LuhnSummarizer.Summary summary = summarizer.summarize(words, ends, 2);
        equal(2, summary.sentenceIndexes().length, "summary count");
        equal(1, summary.rankedScores().getFirst().sentenceIndex(), "strongest sentence");

        boolean nativeAvailable = M3LanguageNative.available();
        if (nativeRequired && !nativeAvailable) {
            throw new AssertionError("native M3 language kernel required but unavailable");
        }
        if (nativeAvailable) {
            long[] score = new long[ends.length];
            int[] significant = new int[ends.length];
            int[] span = new int[ends.length];
            check(M3LanguageNative.scoreLuhnClusters(
                    geometry, words, ends, config.significantFrequencyBand(),
                    config.maximumGap(), score, significant, span), "native call");
            int start = 0;
            for (int sentence = 0; sentence < ends.length; sentence++) {
                var javaScore = summarizer.score(sentence, words, start, ends[sentence]);
                equal(javaScore.luhnQ20(), score[sentence], "JNI Luhn score " + sentence);
                equal(javaScore.significantWords(), significant[sentence],
                        "JNI significant " + sentence);
                equal(javaScore.strongestClusterSpan(), span[sentence],
                        "JNI span " + sentence);
                start = ends[sentence];
            }
        }

        System.out.println("M3_STATIC_LANGUAGE_PASS checks=" + checks
                + " native=" + nativeAvailable);
    }

    private static M3StaticLanguageGeometry fixture() {
        long information = 1L << 0;
        long object = 1L << 1;
        return M3StaticLanguageGeometry.builder(6)
                .word(1, 1000L, 0L, 0, new long[0], new long[0])
                .word(2, 20L, information, 1, new long[] {10L}, new long[] {100L})
                .word(3, 15L, information, 2, new long[] {10L, 20L}, new long[] {100L, 200L})
                .word(4, 10L, object, 0, new long[] {30L}, new long[] {300L})
                .word(5, 1L, 0L, 0, new long[0], new long[0])
                .word(6, 3L, information, 0, new long[] {40L}, new long[] {100L})
                .pair(2, 3, 12L, M3StaticLanguageGeometry.Q20_ONE / 2)
                .pair(3, 4, 6L, M3StaticLanguageGeometry.Q20_ONE / 4)
                .freeze();
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
}
