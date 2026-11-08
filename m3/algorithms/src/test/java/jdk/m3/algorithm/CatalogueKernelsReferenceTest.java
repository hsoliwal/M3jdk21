// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

/**
 * Reference models of the kernels relocated by recipe/catalogue-kernels-20261107, independent of the donor's own
 * tests: the Z first hit against a naive window scan, the Z array against a naive longest-common-prefix scan and the
 * landed M3StringAlgorithms.zFunction (the NEC row of RELOCATION_MAP.tsv), the prefix-match geometry against a naive
 * scan, the one-mismatch window search against a brute-force Hamming count per window, and the LCS witness against the
 * full O(nm) dynamic-programming table. Prints one REFERENCE line per model.
 */
//FLLDCIM: TOP - reference oracles of recipe/catalogue-kernels-20261107 (M3JDK-authored)
final class CatalogueKernelsReferenceTest {
    private static final char[] UNITS = {'a', 'b', '$', '\u0000', '\ud800', '\udfff', '￿'};

    @Test
    void zSearchAndZArrayMatchNaiveScans() {
        Random random = new Random(20261107L);
        int cases = 0;
        for (int trial = 0; trial < 20_000; trial++) {
            String text = text(random, random.nextInt(40));
            String pattern = pattern(random, text);
            assertEquals(naiveFirst(text, pattern), StringAlgorithms.zSearch(text, pattern), "trial=" + trial);
            int[] z = StringAlgorithms.zFunction(text, ProgressMonitor.none());
            assertArrayEquals(naiveZ(text), z, "trial=" + trial);
            assertArrayEquals(StringAlgorithms.zFunction(text, ProgressMonitor.none()), z, "trial=" + trial);
            cases += 3;
        }
        System.out.println("REFERENCE z-search+z-array naive/landed-zFunction cases=" + cases);
    }

    @Test
    void prefixMatchLengthsMatchNaiveLongestCommonPrefix() {
        Random random = new Random(20261108L);
        int cases = 0;
        for (int trial = 0; trial < 10_000; trial++) {
            String text = text(random, random.nextInt(48));
            String pattern = text(random, random.nextInt(10));
            int[] actual = StringAlgorithms.prefixMatchLengths(text, pattern);
            for (int start = 0; start < text.length(); start++) {
                assertEquals(lcp(text, start, pattern), actual[start], "trial=" + trial + " start=" + start);
                cases++;
            }
        }
        System.out.println("REFERENCE z-prefix-geometry naive-lcp positions=" + cases);
    }

    @Test
    void prefixGeometryObservesCancellation() {
        ProgressMonitor canceled = new ProgressMonitor() {
            @Override
            public boolean isCanceled() {
                return true;
            }
        };
        assertThrows(CancellationException.class,
                () -> StringAlgorithms.prefixMatchLengths("a".repeat(4096), "a".repeat(64) + "b", canceled));
        assertThrows(CancellationException.class, () -> StringAlgorithms.zSearch("abc", "c", canceled));
        assertThrows(CancellationException.class, () -> OneMismatchWindowSearch.allIndices("abc", "ab", canceled));
    }

    @Test
    void oneMismatchWindowsMatchBruteForceHammingCount() {
        Random random = new Random(20261109L);
        int windows = 0;
        for (int trial = 0; trial < 10_000; trial++) {
            String text = text(random, random.nextInt(40));
            String pattern = pattern(random, text);
            if (pattern.isEmpty()) {
                continue;
            }
            int[] expected = bruteForce(text, pattern);
            assertArrayEquals(expected, OneMismatchWindowSearch.allIndices(text, pattern), "trial=" + trial);
            assertEquals(expected.length == 0 ? -1 : expected[0],
                    OneMismatchWindowSearch.firstIndex(text, pattern), "trial=" + trial);
            windows += Math.max(0, text.length() - pattern.length() + 1);
        }
        System.out.println("REFERENCE one-mismatch brute-force windows=" + windows);
    }

    @Test
    void lcsWitnessMatchesFullDynamicProgrammingTable() {
        Random random = new Random(20261110L);
        int[] alphabet = {-7, 0, 7, Integer.MIN_VALUE, Integer.MAX_VALUE};
        int cases = 0;
        for (int trial = 0; trial < 6_000; trial++) {
            int[] a = ints(random, random.nextInt(48), alphabet);
            int[] b = ints(random, random.nextInt(48), alphabet);
            int[] witness = DynamicProgrammingAlgorithms.javaWitness(a, b);
            int[][] table = table(a, b);
            assertEquals(table[a.length][b.length], witness.length, "trial=" + trial);
            assertTrue(subsequence(witness, a) && subsequence(witness, b), "trial=" + trial);
            assertEquals(table[a.length][b.length], DynamicProgrammingAlgorithms.referenceLength(a, b), "trial=" + trial);
            cases++;
        }
        int exhaustive = 0;
        for (int n = 0; n <= 4; n++) {
            for (int m = 0; m <= 4; m++) {
                for (int codeA = 0; codeA < pow3(n); codeA++) {
                    for (int codeB = 0; codeB < pow3(m); codeB++) {
                        int[] a = decode(codeA, n);
                        int[] b = decode(codeB, m);
                        int[] witness = DynamicProgrammingAlgorithms.javaWitness(a, b);
                        assertEquals(table(a, b)[n][m], witness.length);
                        assertTrue(subsequence(witness, a) && subsequence(witness, b));
                        exhaustive++;
                    }
                }
            }
        }
        System.out.println("REFERENCE lcs-witness full-dp-table random=" + cases + " exhaustive=" + exhaustive);
    }

    /**
     * The donor crate's own proof contract (its main, run on the donor side with --java: M3_INT_LCS_WITNESS_V1
     * cases=32651 sha256=97fe8bdf...): the same case sequence, the same digest of every witness, on the relocated
     * owner. The proof harness itself is not relocated (DROP rows); this replays its corpus.
     */
    @Test
    void lcsWitnessReproducesTheDonorProofDigest() throws java.security.NoSuchAlgorithmException {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        int[] cases = {0};
        java.util.function.BiConsumer<int[], int[]> verify = (a, b) -> {
            int[] witness = DynamicProgrammingAlgorithms.javaWitness(a, b);
            assertEquals(DynamicProgrammingAlgorithms.referenceLength(a, b), witness.length);
            assertTrue(DynamicProgrammingAlgorithms.isSubsequence(witness, a) && DynamicProgrammingAlgorithms.isSubsequence(witness, b));
            update(digest, witness.length);
            for (int value : witness) {
                update(digest, value);
            }
            cases[0]++;
        };
        verify.accept(new int[] {1, 2, 3, 4, 1}, new int[] {3, 4, 1, 2, 1, 3});
        verify.accept(new int[] {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE},
                new int[] {-1, Integer.MAX_VALUE, Integer.MIN_VALUE});
        verify.accept(new int[] {7, 7, 7, 7}, new int[] {7, 7});
        verify.accept(new int[] {}, new int[] {});
        verify.accept(new int[] {1, 2, 1, 2}, new int[] {2, 1, 2, 1});
        assertThrows(NullPointerException.class, () -> DynamicProgrammingAlgorithms.javaWitness(null, new int[] {1}));
        assertThrows(NullPointerException.class, () -> DynamicProgrammingAlgorithms.javaWitness(new int[] {1}, null));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgrammingAlgorithms.javaWitness(new int[4097], new int[] {1}));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgrammingAlgorithms.javaWitness(new int[4096], new int[4096]));
        cases[0] += 4;
        int[] alphabet = {-7, 0, 7};
        for (int n = 0; n <= 4; n++) {
            for (int m = 0; m <= 4; m++) {
                for (int codeA = 0; codeA < pow3(n); codeA++) {
                    for (int codeB = 0; codeB < pow3(m); codeB++) {
                        verify.accept(decode(codeA, n, alphabet), decode(codeB, m, alphabet));
                    }
                }
            }
        }
        Random rng = new Random(0x5EED20261105L);
        for (int k = 0; k < 18_000; k++) {
            int n = rng.nextInt(61);
            int m = rng.nextInt(61);
            int[] a = new int[n];
            int[] b = new int[m];
            for (int i = 0; i < n; i++) {
                a[i] = alphabet[rng.nextInt(alphabet.length)];
            }
            for (int i = 0; i < m; i++) {
                b[i] = alphabet[rng.nextInt(alphabet.length)];
            }
            if (n > 0 && m > 0 && k % 4 == 0) {
                a[rng.nextInt(n)] = b[rng.nextInt(m)];
            }
            verify.accept(a, b);
        }
        int[] largeA = new int[2_048];
        int[] largeB = new int[1_024];
        Arrays.fill(largeA, 17);
        Arrays.fill(largeB, 17);
        verify.accept(largeA, largeB);
        String sha = java.util.HexFormat.of().formatHex(digest.digest());
        assertEquals(32_651, cases[0]);
        assertEquals("97fe8bdf180b3e91f7072f7e8aac60b94f5fbaf998e76efad0eb018d8c21ff20", sha);
        System.out.println("REFERENCE lcs-witness donor-proof cases=" + cases[0] + " sha256=" + sha);
    }

    @Test
    void catalogNamesTheAddedEntryPoints() throws ReflectiveOperationException {
        List<String> added = List.of("z-search", "one-mismatch-window", "lcs-witness");
        int resolved = 0;
        for (AlgorithmShapeCatalog.Shape shape : AlgorithmShapeCatalog.shapes()) {
            if (!added.contains(shape.id())) {
                continue;
            }
            String[] entry = shape.entryPoint().split("\\.");
            Class<?> owner = Class.forName("jdk.m3.algorithm." + entry[0]);
            Method method = Arrays.stream(owner.getMethods()).filter(m -> m.getName().equals(entry[1]))
                    .findFirst().orElseThrow();
            assertTrue(Modifier.isStatic(method.getModifiers()), shape.entryPoint());
            resolved++;
        }
        assertEquals(added.size(), resolved);
        System.out.println("REFERENCE catalog entry points resolved=" + resolved);
    }

    private static int naiveFirst(String text, String pattern) {
        for (int start = 0; start + pattern.length() <= text.length(); start++) {
            if (lcp(text, start, pattern) == pattern.length()) {
                return start;
            }
        }
        return -1;
    }

    private static int[] naiveZ(String value) {
        int[] z = new int[value.length()];
        for (int index = 1; index < value.length(); index++) {
            z[index] = lcp(value, index, value);
        }
        return z;
    }

    private static int lcp(String text, int start, String pattern) {
        int length = 0;
        while (length < pattern.length() && start + length < text.length()
                && text.charAt(start + length) == pattern.charAt(length)) {
            length++;
        }
        return length;
    }

    private static int[] bruteForce(String text, String pattern) {
        int[] hits = new int[Math.max(0, text.length() - pattern.length() + 1)];
        int count = 0;
        for (int start = 0; start + pattern.length() <= text.length(); start++) {
            int mismatches = 0;
            for (int index = 0; index < pattern.length(); index++) {
                mismatches += text.charAt(start + index) == pattern.charAt(index) ? 0 : 1;
            }
            if (mismatches <= 1) {
                hits[count++] = start;
            }
        }
        return Arrays.copyOf(hits, count);
    }

    private static int[][] table(int[] a, int[] b) {
        int[][] table = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                table[i][j] = a[i - 1] == b[j - 1] ? table[i - 1][j - 1] + 1
                        : Math.max(table[i - 1][j], table[i][j - 1]);
            }
        }
        return table;
    }

    private static boolean subsequence(int[] needle, int[] haystack) {
        int found = 0;
        for (int index = 0; index < haystack.length && found < needle.length; index++) {
            if (haystack[index] == needle[found]) {
                found++;
            }
        }
        return found == needle.length;
    }

    private static int pow3(int exponent) {
        int value = 1;
        for (int index = 0; index < exponent; index++) {
            value *= 3;
        }
        return value;
    }

    private static void update(java.security.MessageDigest digest, int value) {
        for (int shift = 24; shift >= 0; shift -= 8) {
            digest.update((byte) (value >>> shift));
        }
    }

    private static int[] decode(int code, int size, int[] alphabet) {
        int[] result = new int[size];
        for (int index = 0; index < size; index++) {
            result[index] = alphabet[code % alphabet.length];
            code /= alphabet.length;
        }
        return result;
    }

    private static int[] decode(int code, int size) {
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = code % 3 - 1;
            code /= 3;
        }
        return values;
    }

    private static int[] ints(Random random, int length, int[] alphabet) {
        int[] values = new int[length];
        int size = 1 + random.nextInt(alphabet.length);
        for (int index = 0; index < length; index++) {
            values[index] = alphabet[random.nextInt(size)];
        }
        return values;
    }

    private static String text(Random random, int length) {
        char[] units = new char[length];
        int size = 2 + random.nextInt(UNITS.length - 1);
        for (int index = 0; index < length; index++) {
            units[index] = random.nextInt(8) == 0 ? (char) random.nextInt(Character.MAX_VALUE + 1)
                    : UNITS[random.nextInt(size)];
        }
        return new String(units);
    }

    private static String pattern(Random random, String text) {
        if (!text.isEmpty() && random.nextInt(3) != 0) {
            int from = random.nextInt(text.length());
            int to = from + random.nextInt(Math.min(10, text.length() - from) + 1);
            char[] units = text.substring(from, to).toCharArray();
            if (units.length > 0 && random.nextBoolean()) {
                units[random.nextInt(units.length)] = UNITS[random.nextInt(UNITS.length)];
            }
            return new String(units);
        }
        return text(random, random.nextInt(6));
    }
}
