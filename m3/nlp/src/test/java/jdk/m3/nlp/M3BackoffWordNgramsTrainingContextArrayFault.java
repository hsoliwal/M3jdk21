// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import java.util.Arrays;

/** Test-only source instrumentation target; never shipped in the product candidate. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/TrainingContextArrayFault.java blob 1a1ead327164; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsTrainingContextArrayFault {
    private static int failAt = -1;
    private static int copies;
    private M3BackoffWordNgramsTrainingContextArrayFault() { }
    public static void failAt(int value) { failAt = value; copies = 0; }
    public static void healthy() { failAt = -1; copies = 0; }
    private static void before() {
        if (++copies == failAt) throw new OutOfMemoryError("injected context array copy " + failAt);
    }
    public static <T> T[] copyOf(T[] source, int size) { before(); return Arrays.copyOf(source, size); }
    public static int[] copyOf(int[] source, int size) { before(); return Arrays.copyOf(source, size); }
    public static byte[] copyOf(byte[] source, int size) { before(); return Arrays.copyOf(source, size); }
}
