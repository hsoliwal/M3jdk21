/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package jdk.m3.collections.tooling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Read-only searchable receiver evaluation ledger linked to the canonical bag catalogue. */
    //FLLDCIM: MRL - fold-close audit leaf L0054: ADD from PR #1084 head 6b16a20dcedf blob ffc026d13caa, ancestry-only landing b42ab7605b86 (tree == first parent); recipe m3/collections/qualification/foldaudit-collections-20261107/apply.py v1
    //FLLDCIM: MRL - MAPPED: the origin counts/ module has no pom on master; the file joins the real module tree
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
