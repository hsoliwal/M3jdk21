// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Differential dump (gate 3 of the relocation, recipe/utf16-atoms-20261106/differential.sh). Replays the same
 * seeded inputs through the four UTF-16 atoms named by the arguments, reached by reflection so that the donor
 * classes and the relocated classes run the same driver, and prints one canonical line per case: 100,000 seeded
 * UTF-16 pairs (lone and paired surrogates, NUL, U+FFFF, full-range units, embedded copies, both argument orders)
 * for the bit-vector LCS length and its DP oracle, 100,000 seeded text/pattern pairs (wildcards in text and pattern,
 * all-star patterns, patterns derived from the text) for the whole-input glob and its DP oracle, plus the bound and
 * refusal cases of both contracts. Two sides agree when their dumps are byte-identical. The TOTAL line carries the
 * case counts and a SHA-256 over every input unit, so the inputs themselves are evidenced.
 * Usage: java -cp atom-classes;test-classes jdk.m3.precompute.M3Utf16AtomsDump LCS_FQN LCS_ORACLE_FQN GLOB_FQN GLOB_ORACLE_FQN
 */
public final class M3Utf16AtomsDump {
    private static final long LCS_SEED = 0x4C43535F32303236L;
    private static final long GLOB_SEED = 0x474C4F425F323036L;
    private static final int LCS_CASES = 100_000;
    private static final int GLOB_CASES = 100_000;
    private static final int LCS_MAX_TEXT = 4_096;
    private static final int LCS_MAX_SHORT = 63;
    private static final int LCS_LONGER_BOUND = 256;
    private static final int GLOB_MAX_TEXT = 1_048_576;
    private static final int GLOB_MAX_PATTERN = 65_536;
    private static final int GLOB_TEXT_BOUND = 128;
    private static final int GLOB_PATTERN_BOUND = 32;
    private static final int UNIT_RANGE = 0x10000;
    private static final int FULL_RANGE_EVERY = 4;
    private static final int FULL_RANGE_ONE_IN = 3;
    private static final int EMBED_EVERY = 5;
    private static final int LONE_SURROGATE_EVERY = 9;
    private static final int DERIVED_PATTERN_EVERY = 3;
    private static final int ALL_STAR_EVERY = 7;
    private static final int DERIVE_QUESTION_ONE_IN = 4;
    private static final int DERIVE_STAR_ONE_IN = 6;
    private static final int DERIVE_DROP_ONE_IN = 5;
    private static final String LCS_METHOD = "bitVectorLength";
    private static final String LCS_ORACLE_METHOD = "referenceLength";
    private static final String GLOB_METHOD = "match";
    private static final String GLOB_ORACLE_METHOD = "reference";
    private static final char[] LCS_ALPHABET = {
        0, 'a', 'b', 'c', 'z', 'Ā', '\ud800', '\udc00', '\udbff', '\udfff', '￿'};
    private static final char[] GLOB_TEXT_ALPHABET = {0, 'a', 'b', 'c', '?', '*', '\ud800', '\udc00', '￿'};
    private static final char[] GLOB_PATTERN_ALPHABET = {0, 'a', 'b', 'c', '?', '*', '\ud800', '\udc00', '￿'};
    private static final char[] NONE = new char[0];

    private final Method lcs;
    private final Method lcsOracle;
    private final Method glob;
    private final Method globOracle;
    private final MessageDigest inputs;
    private final PrintStream out;
    private long lcsCases;
    private long globCases;

    private M3Utf16AtomsDump(String[] names, PrintStream out) throws ReflectiveOperationException,
            NoSuchAlgorithmException {
        this.lcs = Class.forName(names[0]).getMethod(LCS_METHOD, char[].class, char[].class);
        this.lcsOracle = Class.forName(names[1]).getMethod(LCS_ORACLE_METHOD, char[].class, char[].class);
        this.glob = Class.forName(names[2]).getMethod(GLOB_METHOD, char[].class, char[].class);
        this.globOracle = Class.forName(names[3]).getMethod(GLOB_ORACLE_METHOD, char[].class, char[].class);
        this.inputs = MessageDigest.getInstance("SHA-256");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException, NoSuchAlgorithmException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        M3Utf16AtomsDump dump = new M3Utf16AtomsDump(args, out);
        dump.lcsFixed();
        dump.lcsSeeded();
        dump.globFixed();
        dump.globSeeded();
        out.print("TOTAL lcs_cases=" + dump.lcsCases + " glob_cases=" + dump.globCases
                + " input_sha256=" + HexFormat.of().formatHex(dump.inputs.digest()) + "\n");
        out.flush();
    }

    private void lcsFixed() {
        char[] max = new char[LCS_MAX_TEXT];
        Arrays.fill(max, 'z');
        char[] shortest = new char[LCS_MAX_SHORT];
        Arrays.fill(shortest, 'a');
        lcsLine("F abcde/ace", "abcde".toCharArray(), "ace".toCharArray());
        lcsLine("F HARRY/SALLY", "HARRY".toCharArray(), "SALLY".toCharArray());
        lcsLine("F AGGTAB/GXTXAYB", "AGGTAB".toCharArray(), "GXTXAYB".toCharArray());
        lcsLine("F empty/a", NONE, new char[]{'a'});
        lcsLine("F surrogates", new char[]{0, '\ud800', '\udc00', 0}, new char[]{'\ud800', 0});
        lcsLine("F lone/lone", new char[]{'\ud800'}, new char[]{'\udc00'});
        lcsLine("F max/z", max, new char[]{'z'});
        lcsLine("F max/empty", max, NONE);
        lcsLine("F max/63", max, shortest);
        lcsLine("F 63/63", shortest, shortest);
        lcsLine("R null/empty", null, NONE);
        lcsLine("R empty/null", NONE, null);
        lcsLine("R 4097/empty", new char[LCS_MAX_TEXT + 1], NONE);
        lcsLine("R 64/64", new char[LCS_MAX_SHORT + 1], new char[LCS_MAX_SHORT + 1]);
    }

    private void lcsSeeded() {
        Random random = new Random(LCS_SEED);
        for (int index = 0; index < LCS_CASES; index++) {
            int n = random.nextInt(LCS_LONGER_BOUND + 1);
            int m = random.nextInt(LCS_MAX_SHORT + 1);
            boolean fullRange = index % FULL_RANGE_EVERY == 0;
            char[] a = units(random, n, fullRange);
            char[] b = units(random, m, fullRange);
            if (m <= n && m > 0 && index % EMBED_EVERY == 0) {
                System.arraycopy(a, random.nextInt(n - m + 1), b, 0, m);
            }
            if (index % LONE_SURROGATE_EVERY == 0 && n > 0 && m > 0) {
                a[random.nextInt(n)] = '\ud800';
                b[random.nextInt(m)] = '\ud800';
            }
            lcsLine("S " + index + " n=" + n + " m=" + m, a, b);
        }
    }

    private char[] units(Random random, int length, boolean fullRange) {
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[index] = fullRange && random.nextInt(FULL_RANGE_ONE_IN) == 0
                    ? (char) random.nextInt(UNIT_RANGE)
                    : LCS_ALPHABET[random.nextInt(LCS_ALPHABET.length)];
        }
        return result;
    }

    private void lcsLine(String label, char[] a, char[] b) {
        digest(a);
        digest(b);
        out.print("L " + label + " fast=" + call(lcs, a, b) + " swapped=" + call(lcs, b, a)
                + " ref=" + call(lcsOracle, a, b) + "\n");
        lcsCases++;
    }

    private void globFixed() {
        char[] maxText = new char[GLOB_MAX_TEXT];
        Arrays.fill(maxText, 'a');
        char[] maxPattern = new char[GLOB_MAX_PATTERN];
        Arrays.fill(maxPattern, '?');
        char[] sameLength = new char[GLOB_MAX_PATTERN];
        Arrays.fill(sameLength, 'b');
        globLine("F aa/a", "aa".toCharArray(), "a".toCharArray(), true);
        globLine("F aa/*", "aa".toCharArray(), "*".toCharArray(), true);
        globLine("F adceb/*a*b", "adceb".toCharArray(), "*a*b".toCharArray(), true);
        globLine("F acdcb/a*c?b", "acdcb".toCharArray(), "a*c?b".toCharArray(), true);
        globLine("F empty/empty", NONE, NONE, true);
        globLine("F empty/**", NONE, "**".toCharArray(), true);
        globLine("F surrogate/?", new char[]{'\ud800'}, new char[]{'?'}, true);
        globLine("F pair/?", new char[]{'\ud800', '\udc00'}, new char[]{'?'}, true);
        globLine("F star-literal", new char[]{'*'}, new char[]{'*'}, true);
        globLine("B maxText/*", maxText, "*".toCharArray(), false);
        globLine("B maxText/*b", maxText, "*b".toCharArray(), false);
        globLine("B maxText/?*a", maxText, "?*a".toCharArray(), false);
        globLine("B 65536b/maxPattern", sameLength, maxPattern, false);
        globLine("B maxText/maxPattern", maxText, maxPattern, false);
        globLine("R null/empty", null, NONE, true);
        globLine("R empty/null", NONE, null, true);
        globLine("R 1048577/empty", new char[GLOB_MAX_TEXT + 1], NONE, true);
        globLine("R empty/65537", NONE, new char[GLOB_MAX_PATTERN + 1], true);
    }

    private void globSeeded() {
        Random random = new Random(GLOB_SEED);
        for (int index = 0; index < GLOB_CASES; index++) {
            int n = random.nextInt(GLOB_TEXT_BOUND + 1);
            int m = random.nextInt(GLOB_PATTERN_BOUND + 1);
            char[] text = new char[n];
            for (int position = 0; position < n; position++) {
                text[position] = GLOB_TEXT_ALPHABET[random.nextInt(GLOB_TEXT_ALPHABET.length)];
            }
            char[] pattern = new char[m];
            for (int position = 0; position < m; position++) {
                pattern[position] = GLOB_PATTERN_ALPHABET[random.nextInt(GLOB_PATTERN_ALPHABET.length)];
            }
            if (m > 0 && index % ALL_STAR_EVERY == 0) {
                Arrays.fill(pattern, '*');
            } else if (n > 0 && index % DERIVED_PATTERN_EVERY == 0) {
                pattern = derive(random, text);
            }
            globLine("S " + index + " n=" + n + " m=" + pattern.length, text, pattern, true);
        }
    }

    private static char[] derive(Random random, char[] text) {
        char[] pattern = new char[text.length];
        int length = 0;
        for (char unit : text) {
            if (random.nextInt(DERIVE_DROP_ONE_IN) == 0) {
                continue;
            }
            pattern[length++] = random.nextInt(DERIVE_QUESTION_ONE_IN) == 0 ? '?'
                    : random.nextInt(DERIVE_STAR_ONE_IN) == 0 ? '*' : unit;
        }
        return Arrays.copyOf(pattern, length);
    }

    private void globLine(String label, char[] text, char[] pattern, boolean oracle) {
        digest(text);
        digest(pattern);
        out.print("G " + label + " fast=" + call(glob, text, pattern)
                + (oracle ? " ref=" + call(globOracle, text, pattern) : "") + "\n");
        globCases++;
    }

    private String call(Method method, char[] first, char[] second) {
        try {
            return String.valueOf(method.invoke(null, first, second));
        } catch (InvocationTargetException refused) {
            return refused.getCause().getClass().getSimpleName();
        } catch (IllegalAccessException inaccessible) {
            throw new IllegalStateException(inaccessible);
        }
    }

    private void digest(char[] units) {
        if (units == null) {
            inputs.update((byte) 0xFF);
            return;
        }
        inputs.update((byte) (units.length >>> 24));
        inputs.update((byte) (units.length >>> 16));
        inputs.update((byte) (units.length >>> 8));
        inputs.update((byte) units.length);
        for (char unit : units) {
            inputs.update((byte) (unit >>> 8));
            inputs.update((byte) unit);
        }
    }
}
