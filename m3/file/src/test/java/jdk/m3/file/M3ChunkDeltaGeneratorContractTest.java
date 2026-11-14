// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/c278513bd458f4c9a69e4f5fe261c9450368be3b/synexia-fileindex/src/test/java/com/synexia/fileindex/content/chunk/DeltaGeneratorContractTest.java blob 20cce0f1c477; recipe m3/file/recipe/delta-generator-20261106/relocate.py v3
final class M3ChunkDeltaGeneratorContractTest {
    private static final String A = "%064x".formatted(1);
    private static final String B = "%064x".formatted(2);
    private final M3ChunkDeltaGenerator generator = new M3ChunkDeltaGenerator();
    @TempDir Path temporary;

    @Test void exhaustiveBinarySequencesPreserveExactScriptAndDistance() {
        List<List<String>> corpus = new ArrayList<>();
        for (int length = 0; length <= 6; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < length; i++) row.add((bits & (1 << i)) == 0 ? A : B);
                corpus.add(row);
            }
        }
        assertEquals(127, corpus.size());
        for (List<String> old : corpus) for (List<String> next : corpus) check(old, next);
    }

    @Test void randomAndSequentialListsPreserveTiesAndReconstruction() {
        Random random = new Random(0x4d3344494646L);
        List<String> values = List.of(A, B, "%064x".formatted(3), "%064x".formatted(4));
        for (int trial = 0; trial < 300; trial++) {
            List<String> old = new LinkedList<>(), next = new LinkedList<>();
            for (int i = random.nextInt(80); i > 0; i--) old.add(values.get(random.nextInt(values.size())));
            for (int i = random.nextInt(80); i > 0; i--) next.add(values.get(random.nextInt(values.size())));
            check(old, next);
        }
    }

    @Test void substitutionDistanceIsDistinctFromInsertDeleteScript() {
        assertEquals(1, M3ChunkDeltaGenerator.editDistance(List.of(A), List.of(B)));
        assertEquals(List.of(new M3ChunkDelta.Delete(A), new M3ChunkDelta.Insert(B, 0)),
                generator.generate(A, List.of(A), B, List.of(B)).operations());
    }

    @Test void overflowingDimensionsKeepPreimageExceptionType() {
        var huge = Collections.nCopies(Integer.MAX_VALUE, A);
        assertThrows(NegativeArraySizeException.class, () -> M3ChunkDeltaGenerator.editDistance(huge, List.of()));
        assertThrows(NegativeArraySizeException.class, () -> M3ChunkDeltaGenerator.editDistance(List.of(), huge));
        assertThrows(NegativeArraySizeException.class, () -> generator.generate(A, huge, B, List.of(B)));
        assertThrows(NegativeArraySizeException.class, () -> generator.generate(A, List.of(B), B, huge));
    }

    @Test void nullAndEmptySemanticsKeepOriginalEqualityReceiver() {
        assertEquals(2, M3ChunkDeltaGenerator.editDistance(List.of(), Arrays.asList(null, null)));
        assertEquals(2, M3ChunkDeltaGenerator.editDistance(Arrays.asList(null, null), List.of()));
        for (List<String> next : List.of(Arrays.asList((String) null), Arrays.asList(null, B, null))) {
            check(List.of(A, B), next);
        }
        assertThrows(NullPointerException.class, () -> M3ChunkDeltaGenerator.editDistance(Arrays.asList((String) null), List.of(A)));
        assertThrows(NullPointerException.class, () -> M3ChunkDeltaGenerator.editDistance(Arrays.asList(A, null, B), List.of(A)));
        assertThrows(NullPointerException.class, () -> M3ChunkDeltaGenerator.editDistance(null, List.of()));
        assertThrows(NullPointerException.class, () -> M3ChunkDeltaGenerator.editDistance(List.of(), null));
    }

    @Test void identicalRootsAndChunkSizesKeepExistingContracts() {
        assertEquals(List.of(new M3ChunkDelta.Keep(A)), generator.generate(A, List.of(A), A, List.of(B)).operations());
        Map<String, byte[]> store = new HashMap<>();
        byte[] data = {9, 8, 7};
        String hash = "%064x".formatted(0x987);
        store.put(hash, data);
        assertEquals(List.of(new M3ChunkDelta.Insert(hash, 3)),
                generator.generateWithSizes(A, List.of(), B, List.of(hash), store).operations());
    }

    @Test void deltaScriptFitsConstrainedHeap() throws Exception { runBounded("script"); }
    @Test void scalarDistanceFitsConstrainedHeap() throws Exception { runBounded("distance"); }

    private void runBounded(String mode) throws Exception {
        Path log = temporary.resolve(mode + ".log");
        Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx48m", "-cp", System.getProperty("java.class.path"), M3ChunkDeltaWorkspaceProbe.class.getName(), mode)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(p.waitFor(45, TimeUnit.SECONDS), "bounded probe timed out");
            assertEquals(0, p.exitValue(), java.nio.file.Files.readString(log));
        } finally { if (p.isAlive()) p.destroyForcibly(); }
    }

    private void check(List<String> old, List<String> next) {
        List<M3ChunkDelta.Operation> actual = generator.generate(A, old, B, next).operations();
        assertEquals(scriptOracle(old, next), actual);
        assertEquals(distanceOracle(old, next), M3ChunkDeltaGenerator.editDistance(old, next));
        int cursor = 0;
        List<String> rebuilt = new ArrayList<>();
        for (M3ChunkDelta.Operation op : actual) {
            if (op instanceof M3ChunkDelta.Keep keep) { assertEquals(old.get(cursor++), keep.hash()); rebuilt.add(keep.hash()); }
            else if (op instanceof M3ChunkDelta.Delete delete) assertEquals(old.get(cursor++), delete.hash());
            else if (op instanceof M3ChunkDelta.Insert insert) rebuilt.add(insert.hash());
            else fail("unrecognized operation");
        }
        assertEquals(old.size(), cursor);
        assertEquals(next, rebuilt);
    }

    // Frozen quadratic preimage recurrence and insertion-on-tie backtracking oracle.
    private static List<M3ChunkDelta.Operation> scriptOracle(List<String> old, List<String> next) {
        int n = old.size(), m = next.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) for (int j = 1; j <= m; j++)
            lcs[i][j] = old.get(i - 1).equals(next.get(j - 1)) ? lcs[i - 1][j - 1] + 1 : Math.max(lcs[i - 1][j], lcs[i][j - 1]);
        List<M3ChunkDelta.Operation> ops = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && old.get(i - 1).equals(next.get(j - 1))) { ops.add(new M3ChunkDelta.Keep(old.get(--i))); j--; }
            else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) ops.add(new M3ChunkDelta.Insert(next.get(--j), 0));
            else ops.add(new M3ChunkDelta.Delete(old.get(--i)));
        }
        Collections.reverse(ops);
        return ops;
    }

    private static int distanceOracle(List<String> old, List<String> next) {
        int[][] dp = new int[old.size() + 1][next.size() + 1];
        for (int i = 0; i <= old.size(); i++) dp[i][0] = i;
        for (int j = 0; j <= next.size(); j++) dp[0][j] = j;
        for (int i = 1; i <= old.size(); i++) for (int j = 1; j <= next.size(); j++)
            dp[i][j] = old.get(i - 1).equals(next.get(j - 1)) ? dp[i - 1][j - 1]
                    : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
        return dp[old.size()][next.size()];
    }
}
