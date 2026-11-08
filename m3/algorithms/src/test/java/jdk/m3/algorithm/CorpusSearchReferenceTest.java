// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import jdk.m3.collections.text.IntAhoCorasick;
import org.junit.jupiter.api.Test;

/**
 * Reference models of recipe/corpus-search-20261108 (M3JDK-authored): the relocated byte-pattern kernels against
 * naive scans and counts on random pattern forests, the facade against the automaton, the catalog entry points, the
 * cancellation contract, and the two NEC pairs (the first offsets of jdk.m3.collections.text.IntAhoCorasick; the
 * landed Rabin-Karp against String.indexOf, the contract the omitted donor lane shares).
 */
//FLLDCIM: TOP - reference oracles of recipe/corpus-search-20261108 (M3JDK-authored, test scope)
final class CorpusSearchReferenceTest {
    private static final byte[] ALPHABET = {0, 'a', 'b', (byte) 0xff};
    private static final char[] UNITS = {'a', 'b', '$', (char) 0, (char) 0xd800, (char) 0xdfff};
    private static final int ROUNDS = 400;

    @Test
    void firstOffsetsAgreeWithNaiveScansOnRandomForests() {
        Random random = new Random(20261108L);
        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> patterns = patterns(random, 1 + random.nextInt(24), 0, 1 + random.nextInt(7));
            byte[] haystack = bytes(random, random.nextInt(300), 1 + random.nextInt(ALPHABET.length));
            MultiPatternSearch.Prepared prepared = MultiPatternSearch.prepare(patterns);
            int[] expected = naiveFirstOffsets(patterns, haystack);
            assertArrayEquals(expected, MultiPatternSearch.firstOffsets(prepared, haystack));
            assertArrayEquals(expected, MultiPatternSearch.firstOffsetsJava(prepared, haystack));
            assertArrayEquals(expected, MultiPatternSearch.firstOffsets(prepared, haystack, ProgressMonitor.none()));
        }
    }

    @Test
    void prefixAndExactCountsAgreeWithNaiveCounts() {
        Random random = new Random(1108L);
        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> patterns = patterns(random, random.nextInt(16), 0, random.nextInt(6));
            MultiPatternSearch.Prepared prepared = MultiPatternSearch.prepare(patterns);
            byte[] probe = probe(random, patterns);
            assertEquals(naivePrefixCount(patterns, probe), prepared.prefixCount(probe));
            assertEquals(naiveExactCount(patterns, probe), prepared.exactCount(probe));
            assertEquals(naivePrefixCount(patterns, probe) != 0, prepared.startsWith(probe));
            assertEquals(naiveExactCount(patterns, probe) != 0, prepared.containsExact(probe));
            assertEquals(patterns.size(), prepared.prefixCount(new byte[0]));
        }
    }

    @Test
    void facadeAnswersExactlyLikeTheAutomaton() {
        Random random = new Random(7L);
        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> patterns = patterns(random, random.nextInt(16), 0, random.nextInt(6));
            MultiPatternSearch.Prepared prepared = MultiPatternSearch.prepare(patterns);
            PrefixIndex facade = PrefixIndex.prepare(patterns);
            byte[] probe = probe(random, patterns);
            assertEquals(prepared.patternCount(), facade.size());
            assertEquals(prepared.prefixCount(probe), facade.prefixCount(probe));
            assertEquals(prepared.exactCount(probe), facade.exactCount(probe));
            assertEquals(prepared.startsWith(probe), facade.startsWith(probe));
            assertEquals(prepared.containsExact(probe), facade.contains(probe));
        }
    }

    @Test
    void intAhoCorasickReportsTheSameFirstOffsetsForDistinctNonEmptyPatterns() {
        Random random = new Random(31L);
        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> patterns = distinct(patterns(random, 1 + random.nextInt(12), 1, 1 + random.nextInt(6)));
            byte[] haystack = bytes(random, random.nextInt(200), 1 + random.nextInt(ALPHABET.length));
            int[] expected = MultiPatternSearch.firstOffsets(MultiPatternSearch.prepare(patterns), haystack);
            int[] actual = new int[patterns.size()];
            Arrays.fill(actual, -1);
            IntAhoCorasick.compile(symbols(patterns)).scan(symbols(haystack), (patternId, start, end) -> {
                if (actual[patternId] < 0) {
                    actual[patternId] = start;
                }
            });
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void catalogNamesTheRelocatedEntryPoints() throws ReflectiveOperationException {
        String[][] expected = {
            {"aho-corasick-first-offsets", "STRING", "MultiPatternSearch.firstOffsets"},
            {"byte-prefix-count", "TRIE", "PrefixIndex.prefixCount"}};
        for (String[] row : expected) {
            AlgorithmShapeCatalog.Shape shape = AlgorithmShapeCatalog.shapes().stream()
                    .filter(candidate -> candidate.id().equals(row[0])).findFirst().orElseThrow();
            assertEquals(row[1], shape.family().name());
            assertEquals(row[2], shape.entryPoint());
            String[] parts = shape.entryPoint().split("\\.");
            Class<?> owner = Class.forName("jdk.m3.algorithm." + parts[0]);
            assertTrue(Arrays.stream(owner.getMethods()).anyMatch(method -> method.getName().equals(parts[1])),
                    shape.entryPoint());
        }
    }

    @Test
    void cancellationIsObservedInPreparationAndQueries() {
        CancelableProgressMonitor canceled = new CancelableProgressMonitor();
        canceled.setCanceled(true);
        List<byte[]> patterns = List.of(new byte[] {1, 2}, new byte[] {2});
        assertThrows(CancellationException.class, () -> MultiPatternSearch.prepare(patterns, canceled));
        assertThrows(CancellationException.class, () -> PrefixIndex.prepare(patterns, canceled));
        MultiPatternSearch.Prepared prepared = MultiPatternSearch.prepare(patterns);
        assertThrows(CancellationException.class,
                () -> MultiPatternSearch.prefixCount(prepared, new byte[] {1}, canceled));
        assertThrows(CancellationException.class,
                () -> MultiPatternSearch.firstOffsets(prepared, new byte[] {1, 2}, canceled));
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> MultiPatternSearch.firstOffsets(prepared, new byte[] {1, 2}));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void rabinKarpOwnerMatchesStringIndexOf() {
        Random random = new Random(1_000_003L);
        for (int round = 0; round < ROUNDS * 10; round++) {
            String text = units(random, random.nextInt(48));
            String pattern = random.nextInt(3) == 0 ? units(random, random.nextInt(5)) : slice(random, text);
            int expected = pattern.isEmpty() ? 0 : text.indexOf(pattern);
            assertEquals(expected, PrimitiveStringAlgorithms.rabinKarpIndexOf(text, pattern),
                    () -> text + " / " + pattern);
        }
    }

    private static int[] naiveFirstOffsets(List<byte[]> patterns, byte[] haystack) {
        int[] offsets = new int[patterns.size()];
        for (int index = 0; index < offsets.length; index++) {
            offsets[index] = naiveIndexOf(haystack, patterns.get(index));
        }
        return offsets;
    }

    private static int naiveIndexOf(byte[] haystack, byte[] pattern) {
        for (int start = 0; start + pattern.length <= haystack.length; start++) {
            if (Arrays.equals(haystack, start, start + pattern.length, pattern, 0, pattern.length)) {
                return start;
            }
        }
        return -1;
    }

    private static int naivePrefixCount(List<byte[]> patterns, byte[] probe) {
        int count = 0;
        for (byte[] pattern : patterns) {
            if (pattern.length >= probe.length && Arrays.equals(pattern, 0, probe.length, probe, 0, probe.length)) {
                count++;
            }
        }
        return count;
    }

    private static int naiveExactCount(List<byte[]> patterns, byte[] probe) {
        int count = 0;
        for (byte[] pattern : patterns) {
            if (Arrays.equals(pattern, probe)) {
                count++;
            }
        }
        return count;
    }

    private static List<byte[]> patterns(Random random, int count, int minLength, int maxLength) {
        List<byte[]> patterns = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int length = minLength + random.nextInt(Math.max(1, maxLength - minLength + 1));
            patterns.add(bytes(random, length, 1 + random.nextInt(ALPHABET.length)));
        }
        return patterns;
    }

    private static List<byte[]> distinct(List<byte[]> patterns) {
        List<byte[]> distinct = new ArrayList<>(patterns.size());
        for (byte[] pattern : patterns) {
            if (distinct.stream().noneMatch(seen -> Arrays.equals(seen, pattern))) {
                distinct.add(pattern);
            }
        }
        return distinct;
    }

    private static byte[] bytes(Random random, int length, int alphabet) {
        byte[] values = new byte[length];
        for (int index = 0; index < length; index++) {
            values[index] = ALPHABET[random.nextInt(alphabet)];
        }
        return values;
    }

    private static byte[] probe(Random random, List<byte[]> patterns) {
        if (!patterns.isEmpty() && random.nextInt(3) != 0) {
            byte[] pattern = patterns.get(random.nextInt(patterns.size()));
            return Arrays.copyOf(pattern, random.nextInt(pattern.length + 1));
        }
        return bytes(random, random.nextInt(4), ALPHABET.length);
    }

    private static int[] symbols(byte[] bytes) {
        int[] symbols = new int[bytes.length];
        for (int index = 0; index < bytes.length; index++) {
            symbols[index] = bytes[index] & 0xff;
        }
        return symbols;
    }

    private static int[][] symbols(List<byte[]> patterns) {
        int[][] symbols = new int[patterns.size()][];
        for (int index = 0; index < symbols.length; index++) {
            symbols[index] = symbols(patterns.get(index));
        }
        return symbols;
    }

    private static String units(Random random, int length) {
        char[] units = new char[length];
        for (int index = 0; index < length; index++) {
            units[index] = UNITS[random.nextInt(random.nextBoolean() ? 2 : UNITS.length)];
        }
        return new String(units);
    }

    private static String slice(Random random, String text) {
        if (text.isEmpty()) {
            return "";
        }
        int from = random.nextInt(text.length());
        int to = from + random.nextInt(Math.min(12, text.length() - from) + 1);
        return text.substring(from, to);
    }
}
