// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

/**
 * Candidate-only Levenshtein distance atom over UTF-16 code units.
 * One operand must have at most 63 code units. The faster method uses a single
 * 64-bit Myers bit-vector; the independent reference uses ordinary row DP.
 * This is not a production JNI owner or a full LeetCode #72 implementation.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b42ec59741ed18117c18c72f84cc165ea4900b6e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-bitparallel-edit-20261105/src/main/java/com/synexia/m3/proof/Utf16BitParallelEditOracle.java blob 380723653791; recipe m3/ports/precompute/recipe/utf16-edit-oracle-20261107/relocate.py v2; split members MAX_UNITS,MAX_SHORT_SIDE,nativeDistance,<init>,admit,javaDistance,referenceDistance,digest,differentials,validationRefusals,verify,rejects,decode,main; dropped members ALPHABET; bound members javaDistance (bindings/M3EditDistancePlanOracle.javaDistance.java)
public final class M3EditDistancePlanOracle {
    public static final int MAX_UNITS = 4_096;
    public static final int MAX_SHORT_SIDE = 63;
    private static native int nativeDistance(char[] left, char[] right);

    private M3EditDistancePlanOracle() { }

    private static void admit(char[] left, char[] right) {
        if (left == null || right == null) throw new NullPointerException("left/right");
        if (left.length > MAX_UNITS || right.length > MAX_UNITS
                || Math.min(left.length, right.length) > MAX_SHORT_SIDE) {
            throw new IllegalArgumentException("bounded UTF-16 edit-distance inputs required");
        }
    }

    /**
     * The master owner under the proof domain of {@link #admit}: the shorter operand compiles to an
     * {@link M3EditDistancePlan} (its single-word Myers lane, at most 63 units) and the longer operand is the
     * value; the swapped orientation (the owner's two-row DP lane) must return the same distance. Declared
     * binding of the crate's candidate Myers method to the owner; the owner's wider bounds are not refused here.
     */
    //FLLDCIM: BIND - census L040: donor crate method javaDistance (candidate Myers body) replaced by the owner M3EditDistancePlan (Myers lane and DP lane); admit keeps the proof domain
    public static int javaDistance(char[] left, char[] right) {
        admit(left, right);
        char[] pattern = left.length <= right.length ? left : right;
        char[] text = left.length <= right.length ? right : left;
        int myers = M3EditDistancePlan.compile(CharBuffer.wrap(pattern)).distance(CharBuffer.wrap(text));
        int rows = M3EditDistancePlan.compile(CharBuffer.wrap(text)).distance(CharBuffer.wrap(pattern));
        if (myers != rows) throw new AssertionError("EDIT_LANE_DIFFERENTIAL: myers=" + myers + " rows=" + rows);
        return myers;
    }

    /** Independently written O(m*n) rolling-row dynamic-programming oracle. */
    public static int referenceDistance(char[] left, char[] right) {
        admit(left, right);
        char[] shortSide = left.length <= right.length ? left : right;
        char[] longSide = left.length <= right.length ? right : left;
        int[] previous = new int[shortSide.length + 1];
        for (int j = 0; j < previous.length; j++) previous[j] = j;
        for (int i = 1; i <= longSide.length; i++) {
            int diagonal = previous[0];
            previous[0] = i;
            for (int j = 1; j <= shortSide.length; j++) {
                int above = previous[j];
                int cost = longSide[i - 1] == shortSide[j - 1] ? 0 : 1;
                previous[j] = Math.min(diagonal + cost,
                        Math.min(above + 1, previous[j - 1] + 1));
                diagonal = above;
            }
        }
        return previous[shortSide.length];
    }

    private static MessageDigest digest;
    private static int differentials;
    private static int validationRefusals;
    private static void verify(char[] left, char[] right, boolean nativeEnabled) {
        char[] leftCopy = left.clone();
        char[] rightCopy = right.clone();
        int baseline = referenceDistance(left, right);
        int fast = javaDistance(left, right);
        if (fast != baseline) throw new AssertionError("EDIT_JAVA_DIFFERENTIAL: " + baseline + " vs " + fast);
        if (nativeEnabled && nativeDistance(left, right) != baseline) {
            throw new AssertionError("EDIT_JNI_DIFFERENTIAL");
        }
        if (!java.util.Arrays.equals(leftCopy, left) || !java.util.Arrays.equals(rightCopy, right)) {
            throw new AssertionError("SOURCE_ARRAY_MUTATION");
        }
        for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (baseline >>> shift));
        differentials++;
    }

    private static void rejects(Runnable operation) {
        try { operation.run(); throw new AssertionError("MISSING_INPUT_REFUSAL"); }
        catch (NullPointerException | IllegalArgumentException expected) { validationRefusals++; }
    }

    private static char[] decode(int ordinal, int length, char[] symbols) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = symbols[ordinal % symbols.length];
            ordinal /= symbols.length;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--java".equals(args[0]) || "--native".equals(args[0]))) {
            throw new IllegalArgumentException("--java NO_LIB | --native ABSOLUTE_LIBRARY");
        }
        boolean nativeEnabled = "--native".equals(args[0]);
        if (nativeEnabled) {
            var path = java.nio.file.Path.of(args[1]);
            if (!path.isAbsolute()) throw new IllegalArgumentException("absolute JNI library required");
            System.load(path.toString());
        }
        differentials = 0;
        validationRefusals = 0;
        digest = MessageDigest.getInstance("SHA-256");
        verify("horse".toCharArray(), "ros".toCharArray(), nativeEnabled);
        verify("intention".toCharArray(), "execution".toCharArray(), nativeEnabled);
        verify("geek".toCharArray(), "gesek".toCharArray(), nativeEnabled);
        verify(new char[0], new char[0], nativeEnabled);
        verify(new char[0], "abc".toCharArray(), nativeEnabled);
        verify("abc".toCharArray(), new char[0], nativeEnabled);
        verify(new char[]{0, '\ud800', '\udc00'}, new char[]{0, '\udc00', '\ud800'}, nativeEnabled);
        verify(new char[]{'a'}, new char[]{'A'}, nativeEnabled);
        verify(new char[63], new char[64], nativeEnabled);
        verify(new char[1], new char[MAX_UNITS], nativeEnabled);
        verify(new char[MAX_UNITS], new char[1], nativeEnabled);
        rejects(() -> javaDistance(null, new char[0]));
        rejects(() -> javaDistance(new char[0], null));
        rejects(() -> javaDistance(new char[64], new char[64]));
        rejects(() -> javaDistance(new char[MAX_UNITS + 1], new char[0]));
        rejects(() -> javaDistance(new char[0], new char[MAX_UNITS + 1]));
        if (nativeEnabled) {
            rejects(() -> nativeDistance(null, new char[0]));
            rejects(() -> nativeDistance(new char[0], null));
            rejects(() -> nativeDistance(new char[64], new char[64]));
            rejects(() -> nativeDistance(new char[0], new char[MAX_UNITS + 1]));
        }
        // Exhaustive small alphabet, with both operand orientations and UTF-16 NUL/surrogate units.
        char[] tinySymbols = new char[]{0, 'a', '\ud800'};
        for (int n = 0; n <= 4; n++) {
            int leftVariants = (int) Math.pow(tinySymbols.length, n);
            for (int m = 0; m <= 4; m++) {
                int rightVariants = (int) Math.pow(tinySymbols.length, m);
                for (int li = 0; li < leftVariants; li++) {
                    char[] leftUnits = decode(li, n, tinySymbols);
                    for (int ri = 0; ri < rightVariants; ri++) {
                        verify(leftUnits, decode(ri, m, tinySymbols), nativeEnabled);
                    }
                }
            }
        }
        Random rng = new Random(0x2E_D17_2026_1105L);
        char[] symbols = new char[]{0, 'a', 'b', 'c', 'A', '\ud800', '\udc00', '\u20ac', '\uffff'};
        for (int i = 0; i < 30_000; i++) {
            int n = rng.nextInt(96);
            int m = rng.nextInt(64);
            char[] first = new char[n];
            char[] second = new char[m];
            for (int j = 0; j < n; j++) first[j] = symbols[rng.nextInt(symbols.length)];
            for (int j = 0; j < m; j++) second[j] = symbols[rng.nextInt(symbols.length)];
            if (i % 5 == 0 && m <= n) System.arraycopy(first, 0, second, 0, m);
            verify(first, second, nativeEnabled);
            if (i % 13 == 0) verify(second, first, nativeEnabled);
        }
        System.out.println("M3_UTF16_BITPARALLEL_EDIT_PROOF_V1 differentials=" + differentials + " validationRefusals=" + validationRefusals +
                " sha256=" + HexFormat.of().formatHex(digest.digest()) +
                " java=PASS jni=" + (nativeEnabled ? "PASS" : "NOT_RUN") +
                " donorCodeCopied=false promotion=false");
    }
}
