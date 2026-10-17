/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.tooling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Read-only searchable receiver evaluation ledger linked to the canonical bag catalogue. */
public final class M3CountedCatalogue {
    private M3CountedCatalogue() { }
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("catalogue.tsv [literal query]");
        String query = args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        int row = 0;
        try (var lines = Files.lines(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                row++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split("\t", -1);
                if (cells.length != 8) throw new IOException("Invalid catalogue row " + row);
                for (String cell : cells) if (cell.isBlank()) throw new IOException("Empty evaluation field at row " + row);
                if (line.toLowerCase(Locale.ROOT).contains(query)) System.out.println(line);
            }
        }
    }
}
