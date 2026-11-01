/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The census on a fixture whose atoms are known: which methods are atoms, their metrics, purity, array writes and the
 * pattern the real PATTERN_RULES.tsv assigns; the census of the same sources is a fixed point.
 */
class M3AtomCensusTest {

    private static final String FIXTURE = """
            package fixture;
            class F {
                static int counter;
                static int hash(Object key) { return key == null ? 0 : key.hashCode(); }
                static int nextSet(long[] mask, int from) { return Long.numberOfTrailingZeros(mask[from >>> 6]); }
                static void bump() { counter++; }
                static void fillFrom(int[] a, int v) { for (int i = 0; i < a.length; i++) { a[i] = v; } }
                static int plusOne(int a) { int b = a; b += 1; return b; }
                void instanceMethod() { }
                static class In {
                    static void siftDown(int[] heap, int k) { while (k > 0) { k--; } }
                    static synchronized int locked(int x) { return x; }
                }
                interface Api { void none(); }
            }
            """;

    private static Map<String, M3AtomCensus.Atom> census(Path dir) throws Exception {
        Path rules = Path.of(System.getProperty("user.dir"), "PATTERN_RULES.tsv");
        List<M3AtomCensus.Atom> atoms = M3AtomCensus.census("fixture", dir, M3AtomCensus.rules(rules));
        return atoms.stream().collect(Collectors.toMap(M3AtomCensus.Atom::method, Function.identity()));
    }

    @Test
    void staticMethodsWithBodiesAreTheAtomsWithTheirMetricsAndPatterns(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("fixture"));
        Files.writeString(dir.resolve("fixture/F.java"), FIXTURE, StandardCharsets.UTF_8);
        Map<String, M3AtomCensus.Atom> atoms = census(dir);
        assertEquals(List.of("bump", "fillFrom", "hash", "locked", "nextSet", "plusOne", "siftDown"),
                atoms.keySet().stream().sorted().toList(), "instance and abstract methods are not atoms");
        assertEquals("HASH_SPREAD", atoms.get("hash").pattern());
        assertEquals("BIT_SCAN", atoms.get("nextSet").pattern());
        assertEquals("HEAP_SIFT", atoms.get("siftDown").pattern());
        assertEquals(M3AtomCensus.UNKNOWN, atoms.get("plusOne").pattern());
        assertEquals("F.In", atoms.get("siftDown").type());
        assertEquals("fixture/F.java", atoms.get("hash").path());
        assertEquals("(long[],int)int", atoms.get("nextSet").signature());
        assertTrue(atoms.get("hash").pure() && atoms.get("plusOne").pure(), "local writes keep an atom pure");
        assertTrue(!atoms.get("bump").pure(), "a static field write is an effect");
        assertTrue(!atoms.get("locked").pure(), "a synchronized method is not pure");
        assertTrue(atoms.get("fillFrom").pure() && atoms.get("fillFrom").writesArrays(), "array writes are recorded");
        assertEquals(1, atoms.get("fillFrom").loops());
        assertEquals(1, atoms.get("siftDown").loops());
        assertEquals(1, atoms.get("hash").calls());
    }

    @Test
    void theCensusIsAFixedPoint(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("fixture"));
        Files.writeString(dir.resolve("fixture/F.java"), FIXTURE, StandardCharsets.UTF_8);
        Path rules = Path.of(System.getProperty("user.dir"), "PATTERN_RULES.tsv");
        List<String> first = M3AtomCensus.census(M3AtomCensus.census("fixture", dir, M3AtomCensus.rules(rules)));
        List<String> second = M3AtomCensus.census(M3AtomCensus.census("fixture", dir, M3AtomCensus.rules(rules)));
        assertEquals(first, second);
        assertEquals(M3AtomCensus.CENSUS_HEADER, first.get(0));
    }

    @Test
    void everyRuleCompilesAndNamesACatalogue() throws Exception {
        List<String> lines = Files.readAllLines(Path.of(System.getProperty("user.dir"), "PATTERN_RULES.tsv"),
                StandardCharsets.UTF_8);
        assertEquals("pattern\tname\tbody\tcatalogue", lines.get(0));
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length, line);
            assertTrue(!cells[3].isBlank(), cells[0] + " names its catalogue");
        }
        assertEquals(lines.size() - 1, M3AtomCensus.rules(Path.of(System.getProperty("user.dir"),
                "PATTERN_RULES.tsv")).size());
    }
}
