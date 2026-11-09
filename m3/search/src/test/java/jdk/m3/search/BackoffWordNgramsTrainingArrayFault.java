// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

/** Test-only copy failure injection; never referenced or packaged by production output. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/TrainingArrayFault.java blob 001920e21014; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class BackoffWordNgramsTrainingArrayFault {
    private static int failure;
    private static int attempts;
    private BackoffWordNgramsTrainingArrayFault() { }
    public static void configure(int position) { failure = position; attempts = 0; }
    public static int attempts() { return attempts; }
    private static void before() {
        if (++attempts == failure) throw new OutOfMemoryError("injected-array-copy-" + failure);
    }
    public static String[] copyOf(String[] array, int size) {
        before(); return java.util.Arrays.copyOf(array, size);
    }
    public static long[] copyOf(long[] array, int size) {
        before(); return java.util.Arrays.copyOf(array, size);
    }
    public static int[] copyOf(int[] array, int size) {
        before(); return java.util.Arrays.copyOf(array, size);
    }
    public static byte[] copyOf(byte[] array, int size) {
        before(); return java.util.Arrays.copyOf(array, size);
    }
}
