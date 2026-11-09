/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Pass 3 of the atom passes (atomize, JIT, patternize): every ARRAY_MAP atom is classified into exactly one pattern
 * of a closed catalogue. PATTERNS.tsv is the catalogue (pattern, shape, catalogue reference); ATOM_PATTERNS.tsv maps
 * each atom of ATOMS.tsv to one pattern. An atom without a pattern, a pattern outside the catalogue (UNKNOWN_PATTERN)
 * or a catalogue row no atom uses fails here, so a new atom is classified before it is admitted.
 */
class M3ArrayMapPatternPassTest {

    private static List<String[]> rows(String name, int columns) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(System.getProperty("user.dir")).resolve(name),
                StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            assertEquals(columns, cells.length, name + ": " + line);
            rows.add(cells);
        }
        return rows;
    }

    @Test
    void everyAtomHasExactlyOnePatternOfTheCatalogue() throws IOException {
        Map<String, String> catalogue = new LinkedHashMap<>();
        for (String[] row : rows("PATTERNS.tsv", 3)) {
            assertTrue(row[1].length() > 0 && row[2].length() > 0, row[0] + " has a shape and a catalogue reference");
            assertEquals(null, catalogue.put(row[0], row[2]), row[0] + " is listed once");
        }
        Map<String, String> classified = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        for (String[] row : rows("ATOM_PATTERNS.tsv", 2)) {
            assertEquals(null, classified.put(row[0], row[1]), row[0] + " is classified once");
            if (!catalogue.containsKey(row[1])) {
                unknown.add(row[0] + " UNKNOWN_PATTERN " + row[1]);
            }
        }
        assertEquals(List.of(), unknown);
        Set<String> atoms = new TreeSet<>();
        for (Map<String, String> row : M3ArrayMapAtomContractTest.ledger()) {
            atoms.add(row.get("atom"));
        }
        assertEquals(atoms, new TreeSet<>(classified.keySet()), "ATOM_PATTERNS.tsv classifies exactly the atoms");
        Set<String> unused = new TreeSet<>(catalogue.keySet());
        unused.removeAll(classified.values());
        assertEquals(Set.of(), unused, "every catalogue pattern is used by an atom");
    }
}
