/* Copyright (c) 2026. SPDX-License-Identifier: Apache-2.0 */
package org.synexia.m3.recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Offline validations, per-category search, fixed point, and hostile inputs. */
public final class M3ProblemCatalogueTest {
    private M3ProblemCatalogueTest() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected TSV path");
        final Path seed = Path.of(args[0]);
        final M3ProblemCatalogue catalogue = M3ProblemCatalogue.load(seed);
        check(catalogue.size() >= 70, "seed coverage");
        check(catalogue.find("area", "bitmask").length >= 4, "bitmask problem family");
        check(catalogue.find("area", "graph").length >= 3, "graph problem family");
        check(catalogue.find("target", "collections").length >= 20, "collections targets");
        check(catalogue.find("target", "regex").length >= 1, "regex target");
        check(catalogue.find("source", "HR").length >= 15, "HackerRank diversity");
        check(catalogue.find("source", "LC").length >= 40, "LeetCode diversity");
        check(catalogue.find("id", "LC:136").length == 1, "XOR identity");
        check(catalogue.find("id", "HR:lonely-integer").length == 1, "HackerRank identity");
        check(catalogue.find("technique", "xor_reduction").length == 2,
                "paired XOR distinct from general lonely neighbor");
        check(catalogue.find("id", "LC:2150").length == 1, "lonely neighbor identity");
        check(catalogue.find("area", "nonexistent").length == 0, "unknown token empty");
        check(catalogue.summarize("area").split("\n").length >= 25,
                "wide independent problem-area coverage");
        check(catalogue.summarize("target").contains("collections\t"),
                "target enumeration is deterministic");
        try {
            catalogue.find("unsupported", "anything");
            throw new AssertionError("unknown dimension accepted");
        } catch (IllegalArgumentException expected) {
            // Intentional.
        }
        final Path work = Files.createTempDirectory("m3-catalogue");
        try {
            final Path canonical = work.resolve("canonical.tsv");
            M3ProblemCatalogue.main(new String[]{seed.toString(), "normalize", canonical.toString()});
            final String output = Files.readString(canonical);
            final Path again = work.resolve("canonical-again.tsv");
            M3ProblemCatalogue.main(new String[]{canonical.toString(), "normalize", again.toString()});
            check(output.equals(Files.readString(again)), "normalization fixed point");
            final String[] lines = output.split("\n");
            final Path sample = work.resolve("invalid.tsv");
            Files.writeString(sample, output + lines[1] + "\n");
            reject(sample, "duplicate ID");
            Files.writeString(sample, output.replaceFirst("https://leetcode\\.com/problems/", "https://evil.example/problems/"));
            reject(sample, "invalid external host");
            Files.writeString(sample, output.replaceFirst("metadata_link_only", "unknown_copied_source"));
            reject(sample, "license review");
            Files.writeString(sample, output + "LC\t9\tmalformed\n");
            reject(sample, "columns");
            final String synthetic = lines[1].replace("\t" + "metadata_link_only" + "\t",
                    "\t" + "metadata_link_only" + "\t");
            check(!synthetic.isEmpty(), "nonempty synthetic row");
            final Path large = work.resolve("large.tsv");
            final String[] data = Arrays.copyOfRange(lines, 1, lines.length);
            final StringBuilder generated = new StringBuilder(M3ProblemCatalogue.HEADER).append('\n');
            for (int i = 0; i < 12000; i++) {
                final String record = data[i % data.length];
                final String[] fields = record.split("\t", -1);
                fields[0] = "LC";
                fields[1] = String.valueOf(500000 + i);
                fields[3] = "https://leetcode.com/problems/synthetic-" + i + "/";
                generated.append(String.join("\t", fields)).append('\n');
            }
            Files.writeString(large, generated);
            final M3ProblemCatalogue loaded = M3ProblemCatalogue.load(large);
            check(loaded.size() == 12000, "large index");
            check(loaded.find("source", "LC").length == 12000, "large-source postings");
            check(loaded.find("id", "LC:511999").length == 1, "large-index binary lookup");
        } finally {
            try (var paths = Files.walk(work)) {
                for (Path path : paths.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("M3ProblemCatalogueTest PASS " + catalogue.size() + " problem areas indexed");
    }

    private static void reject(Path bad, String message) {
        try {
            M3ProblemCatalogue.load(bad);
            throw new AssertionError("Expected rejection: " + message);
        } catch (IOException expected) {
            // Deterministic validation failure.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
