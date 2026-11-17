// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static jdk.m3.precompute.Utf16Glob.MAX_PATTERN;
import static jdk.m3.precompute.Utf16Glob.MAX_TEXT;
import static jdk.m3.precompute.Utf16Glob.match;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Candidate-only UTF-16 whole-input glob matching; no regex or product-ABI authority. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-wildcard-glob-20261105/src/main/java/com/synexia/m3/proof/Utf16WildcardOracle.java blob 3727bd2168e5; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1; split members nativeMatch,<init>,requireValid,reference,rejects,compare,encoded,power,main
public final class Utf16GlobOracle {
    private static native boolean nativeMatch(char[] text, char[] pattern);

    private Utf16GlobOracle() { }

    private static void requireValid(char[] text, char[] pattern) {
        if (text == null || pattern == null) throw new NullPointerException("text/pattern");
        if (text.length > MAX_TEXT || pattern.length > MAX_PATTERN) {
            throw new IllegalArgumentException("UTF-16 glob input exceeds bound");
        }
    }

    /** Independent rolling-row DP oracle (O(n*m) time, O(m) Boolean storage).
     * This is a test oracle, not a production alternate dispatch.
     */
    public static boolean reference(char[] text, char[] pattern) {
        requireValid(text, pattern);
        boolean[] state = new boolean[pattern.length + 1];
        state[0] = true;
        for (int j = 1; j <= pattern.length; j++) {
            state[j] = pattern[j - 1] == '*' && state[j - 1];
        }
        for (char unit : text) {
            boolean diagonal = state[0];
            state[0] = false;
            for (int j = 1; j <= pattern.length; j++) {
                boolean previous = state[j];
                char glob = pattern[j - 1];
                state[j] = glob == '*' ? state[j] || state[j - 1]
                        : diagonal && (glob == '?' || glob == unit);
                diagonal = previous;
            }
        }
        return state[pattern.length];
    }

    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("MISSING_VALIDATION_REJECTION"); }
        catch (NullPointerException | IllegalArgumentException expected) { }
    }

    private static void compare(char[] text, char[] pattern, boolean useNative, MessageDigest digest) {
        char[] originalText = text.clone(), originalPattern = pattern.clone();
        boolean expected = reference(text, pattern);
        boolean actual = match(text, pattern);
        if (expected != actual) throw new AssertionError("GLOB_JAVA_DIFF text=" + Arrays.toString(text)
                + " pattern=" + Arrays.toString(pattern));
        if (useNative && nativeMatch(text, pattern) != expected) {
            throw new AssertionError("GLOB_JNI_DIFF text=" + Arrays.toString(text)
                    + " pattern=" + Arrays.toString(pattern));
        }
        if (!Arrays.equals(text, originalText) || !Arrays.equals(pattern, originalPattern)) {
            throw new AssertionError("GLOB_MUTATED_INPUT");
        }
        digest.update((byte) (expected ? 1 : 0));
    }

    private static char[] encoded(int value, int width, char[] alphabet) {
        char[] output = new char[width];
        for (int i = 0; i < width; i++) {
            output[i] = alphabet[value % alphabet.length];
            value /= alphabet.length;
        }
        return output;
    }

    private static int power(int base, int exp) {
        int count = 1;
        for (int i = 0; i < exp; i++) count *= base;
        return count;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--java".equals(args[0]) || "--native".equals(args[0]))) {
            throw new IllegalArgumentException("--java NO_LIB | --native ABSOLUTE_LIBRARY");
        }
        boolean nativeEnabled = "--native".equals(args[0]);
        if (nativeEnabled) {
            Path library = Path.of(args[1]);
            if (!library.isAbsolute()) throw new IllegalArgumentException("absolute native library path required");
            System.load(library.toString());
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int cases = 0;
        char[][] texts = {"".toCharArray(), "aa".toCharArray(), "adceb".toCharArray(),
                "acdcb".toCharArray(), new char[]{0, '\ud800', '\udc00', 0},
                new char[]{'*', '?', '\uffff'}, "abefcdgiescdfimde".toCharArray()};
        char[][] patterns = {"".toCharArray(), "a".toCharArray(), "*a*b".toCharArray(),
                "a*c?b".toCharArray(), new char[]{'?', '*', 0}, "??*".toCharArray(),
                "ab*cd?i*de".toCharArray()};
        for (char[] text : texts) for (char[] pattern : patterns) {
            compare(text, pattern, nativeEnabled, digest);
            cases++;
        }
        // Complete cross-product of tiny UTF-16 texts and patterns, including wildcard runs.
        char[] alphabet = {0, 'a', '\ud800'};
        char[] pAlphabet = {0, 'a', '?', '*'};
        for (int n = 0; n <= 4; n++) {
            for (int t = 0; t < power(alphabet.length, n); t++) {
                char[] text = encoded(t, n, alphabet);
                for (int m = 0; m <= 4; m++) {
                    for (int p = 0; p < power(pAlphabet.length, m); p++) {
                        compare(text, encoded(p, m, pAlphabet), nativeEnabled, digest);
                        cases++;
                    }
                }
            }
        }
        // Mixed deterministic fuzz: overlaps, wildcards, literal '*' in text, NUL and surrogate units.
        Random random = new Random(0x20261105_44L);
        char[] randomTextAlphabet = {0, 'a', 'b', 'c', '?', '*', '\ud800', '\udc00', '\uffff'};
        char[] randomPatternAlphabet = {0, 'a', 'b', 'c', '?', '*', '\ud800', '\udc00', '\uffff'};
        for (int i = 0; i < 32_000; i++) {
            int n = random.nextInt(96), m = random.nextInt(32);
            char[] text = new char[n], pattern = new char[m];
            for (int j = 0; j < n; j++) text[j] = randomTextAlphabet[random.nextInt(randomTextAlphabet.length)];
            for (int j = 0; j < m; j++) pattern[j] = randomPatternAlphabet[random.nextInt(randomPatternAlphabet.length)];
            if (m > 0 && i % 7 == 0) Arrays.fill(pattern, '*');
            compare(text, pattern, nativeEnabled, digest);
            cases++;
        }
        rejects(() -> match(null, new char[0]));
        rejects(() -> match(new char[0], null));
        rejects(() -> match(new char[MAX_TEXT + 1], new char[0]));
        rejects(() -> match(new char[0], new char[MAX_PATTERN + 1]));
        if (nativeEnabled) {
            rejects(() -> nativeMatch(null, new char[0]));
            rejects(() -> nativeMatch(new char[0], null));
            rejects(() -> nativeMatch(new char[MAX_TEXT + 1], new char[0]));
            rejects(() -> nativeMatch(new char[0], new char[MAX_PATTERN + 1]));
        }
        String root = HexFormat.of().formatHex(digest.digest());
        System.out.println("M3_UTF16_GLOB_V1 cases=" + cases + " sha256=" + root
                + " java=PASS jni=" + (nativeEnabled ? "PASS" : "NOT_RUN")
                + " nullOrOversizeRefused=4 inputImmutable=true chromeCalls=0 promotion=false");
    }
}
