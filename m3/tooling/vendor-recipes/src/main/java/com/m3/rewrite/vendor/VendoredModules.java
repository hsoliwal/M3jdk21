// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The hash-pinned {@code THIRD_PARTY_MODULES.tsv}: for every {@code kind=MODULE} row, the inlined base version
 * ({@code version[:-len(suffix)]}) per group:artifact, exactly as {@code switch_consumers.py} builds {@code by_ga}.
 * Refuses a table whose SHA-256 differs from its pin, and a table with a quote character (the oracle reads it with
 * Python's {@code csv} QUOTE_MINIMAL dialect, which this reader does not reproduce).
 */
record VendoredModules(Map<GroupArtifact, Set<String>> byGa) {

    /** A {@code (group, artifact)} key. */
    record GroupArtifact(String group, String artifact) {
    }

    static VendoredModules load(Path tsv, String pinnedSha256, String suffix) {
        byte[] bytes = PyText.readBytes(tsv);
        String actual = PyText.sha256(bytes);
        if (!actual.equalsIgnoreCase(pinnedSha256)) {
            throw new IllegalStateException("modules table drifted from its pin: " + tsv + " sha256 " + actual + " != " + pinnedSha256);
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf('"') >= 0) {
            throw new IllegalStateException("quoted TSV fields are outside the oracle-equivalent subset: " + tsv);
        }
        return parse(text, suffix);
    }

    static VendoredModules parse(String text, String suffix) {
        List<String> lines = Arrays.stream(text.split("\r\n|\n|\r", -1)).filter(l -> !l.isEmpty()).toList();
        List<String> header = List.of(lines.get(0).split("\t", -1));
        Map<GroupArtifact, Set<String>> byGa = lines.subList(1, lines.size()).stream()
                .map(l -> l.split("\t", -1))
                .filter(c -> "MODULE".equals(column(header, c, "kind")))
                .collect(Collectors.groupingBy(c -> new GroupArtifact(column(header, c, "group"), column(header, c, "artifact")),
                        Collectors.mapping(c -> PyText.dropLast(column(header, c, "version"), suffix.length()),
                                Collectors.toCollection(TreeSet::new))));
        return new VendoredModules(Map.copyOf(byGa));
    }

    private static String column(List<String> header, String[] cells, String name) {
        int i = header.lastIndexOf(name); // csv.DictReader zips the header: a repeated name keeps its last column
        return i >= 0 && i < cells.length ? cells[i] : "";
    }

    Optional<Set<String>> inlined(String group, String artifact) {
        return Optional.ofNullable(byGa.get(new GroupArtifact(group, artifact)));
    }
}
