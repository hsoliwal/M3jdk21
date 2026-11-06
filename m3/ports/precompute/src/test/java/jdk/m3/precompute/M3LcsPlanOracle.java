// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static jdk.m3.precompute.M3LcsPlan.bitVectorLength;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

/** First-party candidate-only UTF-16 code-unit LCS proof; no product mutation authority. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-lcs-bitvector-20261105/src/main/java/com/synexia/m3/proof/Utf16BitVectorLcsOracle.java blob 079e66607954; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1; split members MAX_TEXT,MAX_SHORT,nativeLength,<init>,validate,referenceLength,observed,digest,verify,reject,decode,main
public final class M3LcsPlanOracle {
    private static final int MAX_TEXT = 4_096;
    private static final int MAX_SHORT = 63;
    private static native int nativeLength(char[] first, char[] second);

    private M3LcsPlanOracle() { }

    private static void validate(char[] first, char[] second) {
        if (first == null || second == null) {
            throw new NullPointerException("first/second");
        }
        if (first.length > MAX_TEXT || second.length > MAX_TEXT
                || Math.min(first.length, second.length) > MAX_SHORT) {
            throw new IllegalArgumentException("bounded LCS requires <=4096 UTF-16 units each and min<=63");
        }
    }

    /** Independent rolling-row LCS DP, O(n*m), used only for differential qualification. */
    public static int referenceLength(char[] first, char[] second) {
        validate(first, second);
        char[] shorter = first.length <= second.length ? first : second;
        char[] longer = first.length <= second.length ? second : first;
        int[] row = new int[shorter.length + 1];
        for (char unit : longer) {
            int northwest = 0;
            for (int j = 1; j <= shorter.length; j++) {
                int prior = row[j];
                row[j] = unit == shorter[j - 1] ? northwest + 1 : Math.max(row[j], row[j - 1]);
                northwest = prior;
            }
        }
        return row[shorter.length];
    }

    private static int observed;
    private static MessageDigest digest;

    private static void verify(char[] first, char[] second, boolean nativeEnabled) {
        int expected = referenceLength(first, second);
        int result = bitVectorLength(first, second);
        if (result != expected) throw new AssertionError("LCS_JAVA_DIFF " + expected + " != " + result);
        if (nativeEnabled && nativeLength(first, second) != expected) {
            throw new AssertionError("LCS_NATIVE_DIFF");
        }
        if (result < 0 || result > Math.min(first.length, second.length)) {
            throw new AssertionError("LCS_BOUND_VIOLATION");
        }
        digest.update((byte) (result >>> 24));
        digest.update((byte) (result >>> 16));
        digest.update((byte) (result >>> 8));
        digest.update((byte) result);
        observed++;
    }

    private static void reject(Runnable javaTask, Runnable nativeTask, boolean nativeEnabled) {
        try {
            javaTask.run();
            throw new AssertionError("MISSING_JAVA_REJECTION");
        } catch (NullPointerException | IllegalArgumentException accepted) { /* admitted validation refusal */ }
        if (nativeEnabled) {
            try {
                nativeTask.run();
                throw new AssertionError("MISSING_NATIVE_REJECTION");
            } catch (NullPointerException | IllegalArgumentException accepted) { /* admitted validation refusal */ }
        }
    }

    private static char[] decode(int rank, int length, char[] alphabet) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = alphabet[rank % alphabet.length];
            rank /= alphabet.length;
        }
        return out;
    }

    /** Deterministic actual-Java and optional actual-JNI differential executable. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--java".equals(args[0]) || "--native".equals(args[0]))) {
            throw new IllegalArgumentException("--java NO_LIB | --native ABSOLUTE_LIBRARY");
        }
        boolean nativeEnabled = "--native".equals(args[0]);
        if (nativeEnabled) {
            var path = java.nio.file.Path.of(args[1]);
            if (!path.isAbsolute()) throw new IllegalArgumentException("absolute native library required");
            System.load(path.toString());
        }
        // All execution receipt state is reset per invocation; no accumulated same-JVM counts.
        observed = 0;
        digest = MessageDigest.getInstance("SHA-256");
        verify("abcde".toCharArray(), "ace".toCharArray(), nativeEnabled);
        verify("HARRY".toCharArray(), "SALLY".toCharArray(), nativeEnabled);
        verify("ABC".toCharArray(), "AC".toCharArray(), nativeEnabled);
        verify("AGGTAB".toCharArray(), "GXTXAYB".toCharArray(), nativeEnabled);
        verify("SHINCHAN".toCharArray(), "NOHARAAA".toCharArray(), nativeEnabled);
        verify(new char[0], new char[]{'a'}, nativeEnabled);
        verify(new char[]{0, '\ud800', '\udc00', 0}, new char[]{'\ud800', 0}, nativeEnabled);
        verify("aaaaa".toCharArray(), "aaa".toCharArray(), nativeEnabled);
        verify("abc".toCharArray(), "cba".toCharArray(), nativeEnabled);
        verify(new char[]{'\ud800'}, new char[]{'\udc00'}, nativeEnabled);
        char[] max = new char[MAX_TEXT];
        java.util.Arrays.fill(max, 'z');
        verify(max, new char[]{'z'}, nativeEnabled);
        verify(max, new char[0], nativeEnabled);
        char[] sixtyThree = new char[MAX_SHORT];
        java.util.Arrays.fill(sixtyThree, 'a');
        verify(max, sixtyThree, nativeEnabled);
        verify(sixtyThree, sixtyThree, nativeEnabled);
        reject(() -> bitVectorLength(null, new char[0]), () -> nativeLength(null, new char[0]), nativeEnabled);
        reject(() -> bitVectorLength(new char[0], null), () -> nativeLength(new char[0], null), nativeEnabled);
        reject(() -> bitVectorLength(new char[MAX_TEXT + 1], new char[0]),
                () -> nativeLength(new char[MAX_TEXT + 1], new char[0]), nativeEnabled);
        reject(() -> bitVectorLength(new char[64], new char[64]),
                () -> nativeLength(new char[64], new char[64]), nativeEnabled);
        char[] tinyAlphabet = new char[]{0, 'a', '\ud800'};
        for (int na = 0; na <= 3; na++) {
            int casesA = (int) Math.pow(tinyAlphabet.length, na);
            for (int va = 0; va < casesA; va++) {
                char[] a = decode(va, na, tinyAlphabet);
                for (int nb = 0; nb <= 3; nb++) {
                    int casesB = (int) Math.pow(tinyAlphabet.length, nb);
                    for (int vb = 0; vb < casesB; vb++) {
                        verify(a, decode(vb, nb, tinyAlphabet), nativeEnabled);
                    }
                }
            }
        }
        char[] alphabet = new char[]{0, 'a', 'b', 'c', '\ud800', '\udc00', '\uffff'};
        Random random = new Random(0x4C43535F20261105L);
        for (int k = 0; k < 46_000; k++) {
            int n = random.nextInt(112);
            int m = random.nextInt(64);
            char[] a = new char[n];
            char[] b = new char[m];
            for (int j = 0; j < n; j++) a[j] = alphabet[random.nextInt(alphabet.length)];
            for (int j = 0; j < m; j++) b[j] = alphabet[random.nextInt(alphabet.length)];
            if (m <= n && m > 0 && k % 5 == 0) {
                System.arraycopy(a, random.nextInt(n - m + 1), b, 0, m);
            }
            verify(a, b, nativeEnabled);
        }
        System.out.println("M3_UTF16_LCS_BITVECTOR_V1 cases=" + observed
                + " answerSha256=" + HexFormat.of().formatHex(digest.digest())
                + " java=PASS jni=" + (nativeEnabled ? "PASS" : "NOT_RUN")
                + " sourcePromotion=false chromeCalls=0");
    }
}
