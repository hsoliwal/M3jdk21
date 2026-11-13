// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit.fixture;

/** Original deterministic Java/JNI probe; challenge sites are category references only. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-20261031/fixtures/AtomJitProbe.java blob 0520fac9fc56; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class AtomJitProbe {
    static { System.loadLibrary("m3_atom_jni"); }

    private AtomJitProbe() { }

    public static native long sum(int[] values);
    public static native long cppFold(int[] values);

    private static int firstMatch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        int[] prefix = new int[pattern.length()];
        for (int i = 1, j = 0; i < pattern.length(); i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            prefix[i] = j;
        }
        for (int i = 0, j = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == pattern.length()) return i - j + 1;
        }
        return -1;
    }

    private static long bestWindow(int[] data, int width) {
        if (width <= 0 || width > data.length) throw new IllegalArgumentException("width");
        long sum = 0;
        for (int i = 0; i < width; i++) sum += data[i];
        long best = sum;
        for (int i = width; i < data.length; i++) {
            sum += (long) data[i] - data[i - width];
            best = Math.max(best, sum);
        }
        return best;
    }

    public static void main(String[] args) {
        int[] data = {Integer.MAX_VALUE, -7, 0, Integer.MIN_VALUE, 17, 7, -9};
        long expectedSum = 7;
        long expectedFold = 0;
        for (int x : data) expectedFold += (long) x * x;
        if (sum(data) != expectedSum || cppFold(data) != expectedFold) {
            throw new AssertionError("JNI result changed");
        }
        if (sum(new int[0]) != 0 || cppFold(new int[0]) != 0) {
            throw new AssertionError("JNI empty-array contract changed");
        }
        if (firstMatch("ababaabababa", "ababa") != 0
                || firstMatch("a\u03a9b", "\u03a9") != 1
                || firstMatch("aaaa", "xyz") != -1
                || firstMatch("anything", "") != 0
                || firstMatch("mississippi", "issip") != 4) {
            throw new AssertionError("independent KMP cases changed");
        }
        if (bestWindow(new int[] {-5, -8, -1, -2}, 2) != -3) {
            throw new AssertionError("negative sliding window maximum changed");
        }
        long checksum = 0;
        for (int i = 0; i < 12_000; i++) {
            checksum += firstMatch("ababaabababa", "ababa");
            checksum += firstMatch("a\u03a9b", "\u03a9");
            checksum += firstMatch("aaaa", "xyz");
            checksum += bestWindow(data, 3);
            checksum ^= sum(data);
            checksum ^= cppFold(data);
        }
        System.out.println("M3_JAVA_JNI_ATOM_CHECKSUM=" + checksum);
    }
}
