/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Every ARRAY_MAP atom has its four parts: a contract (Requires, Ensures, Effects, Cost) in its Javadoc, the same
 * contract word for word in its row of ATOMS.tsv, its own JUnit test in M3ArrayMapAtomsTest and its own row in
 * M3ArrayMapAtomsPerformanceTest. The atoms are read by reflection from the generated Atoms class, so an atom added to
 * the template without its four parts fails here.
 */
class M3ArrayMapAtomContractTest {

    private static final List<String> COLUMNS = List.of("atom", "signature", "requires", "ensures", "effects", "cost",
            "junit", "perf_row");
    private static final List<String> CLAUSES = List.of("requires", "ensures", "effects", "cost");
    private static final Pattern CONTRACT = Pattern.compile("^(?<summary>.+?) Requires: (?<requires>.+?)"
            + " Ensures: (?<ensures>.+?) Effects: (?<effects>.+?) Cost: (?<cost>.+)$");
    private static final String MEMBER = "../../implementations/apache-linked-map/src/main/java/jdk/m3/collections/"
            + "apache/M3LinkedMap.java";
    private static final String PERFORMANCE = "src/test/java/jdk/m3/collections/apache/"
            + "M3ArrayMapAtomsPerformanceTest.java";

    private static Path base() {
        return Path.of(System.getProperty("user.dir"));
    }

    static List<Map<String, String>> ledger() throws IOException {
        List<String> lines = Files.readAllLines(base().resolve("ATOMS.tsv"), StandardCharsets.UTF_8);
        assertEquals(COLUMNS, List.of(lines.get(0).split("\t", -1)), "ATOMS.tsv header");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            assertEquals(COLUMNS.size(), cells.length, "cells of " + line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < cells.length; c++) {
                assertFalse(cells[c].isBlank(), COLUMNS.get(c) + " of " + cells[0]);
                row.put(COLUMNS.get(c), cells[c]);
            }
            rows.add(row);
        }
        return rows;
    }

    static String signature(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes()).map(Class::getTypeName)
                .collect(Collectors.joining(",", "(", ")")) + method.getReturnType().getTypeName();
    }

    /** The atoms: the static, non-private methods the compiler declared in the Atoms class. */
    static Set<String> atoms() {
        return Arrays.stream(M3LinkedMap.Atoms.class.getDeclaredMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers()))
                .filter(m -> !m.isSynthetic())
                .map(M3ArrayMapAtomContractTest::signature)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** The Javadoc of the static method ATOM in SOURCE as one line: markers and leading asterisks dropped. */
    static String javadoc(String source, String atom) {
        Matcher m = Pattern.compile("/\\*\\*((?:(?!\\*/).)*)\\*/\\s*static\\s+[\\w\\[\\]]+\\s+" + atom + "\\(",
                Pattern.DOTALL).matcher(source);
        assertTrue(m.find(), atom + " has no Javadoc");
        return m.group(1).replaceAll("(?m)^\\s*\\*", " ").replaceAll("\\s+", " ").strip();
    }

    @Test
    void everyAtomHasAContractRow() throws IOException {
        Set<String> listed = new TreeSet<>();
        for (Map<String, String> row : ledger()) {
            assertTrue(row.get("signature").startsWith(row.get("atom") + "("), row.get("atom") + " signature");
            assertTrue(listed.add(row.get("signature")), row.get("atom") + " is listed once");
        }
        assertEquals(atoms(), listed, "ATOMS.tsv lists exactly the atoms of the Atoms class");
    }

    @Test
    void everyAtomStatesItsContractInItsJavadoc() throws IOException {
        String source = Files.readString(base().resolve(MEMBER), StandardCharsets.UTF_8);
        for (Map<String, String> row : ledger()) {
            Matcher contract = CONTRACT.matcher(javadoc(source, row.get("atom")));
            assertTrue(contract.matches(), row.get("atom") + " states Requires, Ensures, Effects and Cost");
            for (String clause : CLAUSES) {
                assertEquals(row.get(clause), contract.group(clause), row.get("atom") + " " + clause);
            }
        }
    }

    @Test
    void everyAtomHasItsOwnJUnitTest() throws Exception {
        Set<String> tests = new TreeSet<>();
        for (Map<String, String> row : ledger()) {
            Method test = M3ArrayMapAtomsTest.class.getDeclaredMethod(row.get("junit"));
            assertNotNull(test.getAnnotation(Test.class), row.get("junit") + " is a test");
            assertTrue(tests.add(row.get("junit")), row.get("junit") + " belongs to one atom");
        }
    }

    @Test
    void everyAtomHasItsOwnPerformanceRow() throws IOException {
        String performance = Files.readString(base().resolve(PERFORMANCE), StandardCharsets.UTF_8);
        Set<String> labels = new TreeSet<>();
        for (Map<String, String> row : ledger()) {
            assertTrue(performance.contains("row(\"" + row.get("perf_row") + "\""), row.get("atom") + " performance row");
            assertTrue(labels.add(row.get("perf_row")), row.get("perf_row") + " belongs to one atom");
        }
    }
}
