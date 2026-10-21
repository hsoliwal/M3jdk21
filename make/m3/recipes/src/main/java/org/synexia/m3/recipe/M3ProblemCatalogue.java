/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Original, dependency-free M3 problem-area catalogue. No challenge solutions.
 */
package org.synexia.m3.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, offline searchable catalogue of problem areas and contract hints.
 *
 * <p>Stores rows as parallel column arrays. An inverted index uses only flat
 * String and int arrays (no per-record index nodes, Map.Entry, or linked list).
 * IDs, areas, techniques, and targets are independently indexed: an algorithm
 * problem is not restricted to one category. Nothing here proves that an
 * algorithm is equivalent to an existing JDK operation.
 */
public final class M3ProblemCatalogue {
    public static final String HEADER = "source\tid\ttitle\turl\tareas\ttechniques\tm3_targets"
            + "\tpreconditions\ttime\tspace\tlicense\tdecision\tstatus";

    private static final int SOURCE = 0;
    private static final int ID = 1;
    private static final int TITLE = 2;
    private static final int URL = 3;
    private static final int AREAS = 4;
    private static final int TECHNIQUES = 5;
    private static final int TARGETS = 6;
    private static final int CONDITIONS = 7;
    private static final int LICENSE = 10;
    private static final int DECISION = 11;
    private static final int STATUS = 12;
    private static final int COLUMN_COUNT = 13;

    private final String[][] columns;
    private final String[] postingKeys;
    private final int[] postingRows;

    private M3ProblemCatalogue(String[][] columns) {
        this.columns = columns;
        final PostingBuilder postings = new PostingBuilder(Math.max(8, size() * 7));
        for (int row = 0; row < size(); row++) {
            postings.add("source:" + columns[SOURCE][row], row);
            postings.add("id:" + columns[SOURCE][row] + ":" + columns[ID][row], row);
            postings.add("status:" + columns[STATUS][row], row);
            for (int field : new int[] {AREAS, TECHNIQUES, TARGETS}) {
                final String prefix = field == AREAS ? "area:" : field == TECHNIQUES
                        ? "technique:" : "target:";
                for (String token : columns[field][row].split(";", -1)) {
                    postings.add(prefix + token, row);
                }
            }
        }
        postingKeys = postings.keys();
        postingRows = postings.rows();
    }

    public static M3ProblemCatalogue load(Path input) throws IOException {
        Objects.requireNonNull(input, "input");
        final List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IOException("Catalogue header mismatch: " + input);
        }
        final String[][] columns = new String[COLUMN_COUNT][Math.max(0, lines.size() - 1)];
        final String[] identities = new String[columns[0].length];
        for (int line = 1; line < lines.size(); line++) {
            final String[] fields = lines.get(line).split("\t", -1);
            if (fields.length != COLUMN_COUNT) {
                throw new IOException("Expected 13 columns at line " + (line + 1));
            }
            for (int column = 0; column < COLUMN_COUNT; column++) {
                if (fields[column].isBlank() || !fields[column].equals(fields[column].strip())) {
                    throw new IOException("Blank/padded field " + column + " at " + (line + 1));
                }
                columns[column][line - 1] = fields[column];
            }
            validate(fields, line + 1);
            identities[line - 1] = fields[SOURCE] + ":" + fields[ID];
        }
        Arrays.sort(identities);
        for (int i = 1; i < identities.length; i++) {
            if (identities[i].equals(identities[i - 1])) {
                throw new IOException("Duplicate problem ID: " + identities[i]);
            }
        }
        return new M3ProblemCatalogue(columns);
    }

    private static void validate(String[] fields, int line) throws IOException {
        final String source = fields[SOURCE];
        final String id = fields[ID];
        final String url = fields[URL];
        final boolean validSource = switch (source) {
            case "LC" -> id.matches("[1-9][0-9]*")
                    && url.matches("https://leetcode\\.com/problems/[a-z0-9-]+/?");
            case "HR" -> id.matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                    && url.matches("https://www\\.hackerrank\\.com/challenges/[a-z0-9-]+/problem/?");
            case "GFG" -> id.matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                    && url.matches("https://www\\.geeksforgeeks\\.org/[a-z0-9/-]+/?");
            case "GH" -> id.matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                    && url.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:/.*)?");
            default -> false;
        };
        if (!validSource) {
            throw new IOException("Invalid source/ID/URL at line " + line);
        }
        validateTokens(fields[AREAS], line);
        validateTokens(fields[TECHNIQUES], line);
        validateTokens(fields[TARGETS], line);
        if (!fields[LICENSE].equals("metadata_link_only")) {
            throw new IOException("Donor source requires independent license review at " + line);
        }
        if (!fields[STATUS].matches("DISCOVERY|EVALUATED|REJECTED_CONTRACT|LOCAL_TESTED")) {
            throw new IOException("Unrecognized review status at " + line);
        }
        if (fields[CONDITIONS].length() > 500 || fields[DECISION].length() > 500) {
            throw new IOException("Overlong contract/decision at " + line);
        }
    }

    private static void validateTokens(String value, int line) throws IOException {
        final String[] parts = value.split(";", -1);
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("[a-z][a-z0-9_]*")) {
                throw new IOException("Invalid classification token at line " + line);
            }
            for (int j = 0; j < i; j++) {
                if (parts[j].equals(parts[i])) {
                    throw new IOException("Repeated classification at line " + line);
                }
            }
        }
    }

    public int size() {
        return columns[SOURCE].length;
    }

    public String value(int row, int column) {
        Objects.checkIndex(row, size());
        Objects.checkIndex(column, COLUMN_COUNT);
        return columns[column][row];
    }

    public int[] find(String dimension, String value) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(value, "value");
        if (!List.of("area", "technique", "target", "source", "status", "id")
                .contains(dimension)) {
            throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
        final String key = dimension + ":" + value;
        int first = lowerBound(key);
        int end = first;
        while (end < postingKeys.length && postingKeys[end].equals(key)) {
            end++;
        }
        return Arrays.copyOfRange(postingRows, first, end);
    }

    private int lowerBound(String key) {
        int low = 0;
        int high = postingKeys.length;
        while (low < high) {
            final int middle = low + ((high - low) >>> 1);
            if (postingKeys[middle].compareTo(key) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public String canonicalTsv() {
        final int[] indexes = new int[size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        sortIndexes(indexes, (a, b) -> {
            final int bySource = columns[SOURCE][a].compareTo(columns[SOURCE][b]);
            return bySource == 0 ? columns[ID][a].compareTo(columns[ID][b]) : bySource;
        });
        final StringBuilder output = new StringBuilder(HEADER).append('\n');
        for (int row : indexes) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                if (column != 0) {
                    output.append('\t');
                }
                output.append(columns[column][row]);
            }
            output.append('\n');
        }
        return output.toString();
    }

    /** A deterministic count of distinct tokens in one searchable dimension. */
    public String summarize(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        if (!List.of("area", "technique", "target", "source", "status")
                .contains(dimension)) {
            throw new IllegalArgumentException("Unknown summary dimension: " + dimension);
        }
        final String prefix = dimension + ":";
        final StringBuilder output = new StringBuilder();
        int index = lowerBound(prefix);
        while (index < postingKeys.length && postingKeys[index].startsWith(prefix)) {
            final String key = postingKeys[index];
            final int start = index;
            while (index < postingKeys.length && postingKeys[index].equals(key)) {
                index++;
            }
            output.append(key.substring(prefix.length()))
                    .append('\t').append(index - start).append('\n');
        }
        return output.toString();
    }

    /** This is an inspection CLI, never a candidate transformation or online crawler. */
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException("Usage: M3ProblemCatalogue <tsv>"
                    + " validate | stats | areas | targets | techniques"
                    + " | find <area|technique|target|source|status|id> <value>"
                    + " | normalize <output.tsv>");
        }
        final M3ProblemCatalogue catalogue = load(Path.of(args[0]));
        switch (args[1]) {
            case "validate" -> {
                if (args.length != 2) throw new IllegalArgumentException("validate takes no arguments");
                System.out.println("M3 catalogue validated: " + catalogue.size() + " problems");
            }
            case "stats" -> {
                if (args.length != 2) throw new IllegalArgumentException("stats takes no arguments");
                System.out.println("problems=" + catalogue.size()
                        + " postings=" + catalogue.postingKeys.length);
            }
            case "areas", "targets", "techniques" -> {
                if (args.length != 2) throw new IllegalArgumentException("summary takes no arguments");
                final String dimension = args[1].equals("areas") ? "area"
                        : args[1].equals("targets") ? "target" : "technique";
                System.out.print(catalogue.summarize(dimension));
            }
            case "find" -> {
                if (args.length != 4) throw new IllegalArgumentException("find requires dimension and value");
                for (int row : catalogue.find(args[2], args[3])) {
                    System.out.println(catalogue.value(row, SOURCE) + ":"
                            + catalogue.value(row, ID) + "\t"
                            + catalogue.value(row, TITLE) + "\t"
                            + catalogue.value(row, AREAS) + "\t"
                            + catalogue.value(row, TARGETS) + "\t"
                            + catalogue.value(row, STATUS));
                }
            }
            case "normalize" -> {
                if (args.length != 3) throw new IllegalArgumentException("normalize requires output path");
                Files.writeString(Path.of(args[2]), catalogue.canonicalTsv(), StandardCharsets.UTF_8);
            }
            default -> throw new IllegalArgumentException("Unknown command: " + args[1]);
        }
    }

    @FunctionalInterface
    private interface IntPairOrder {
        int compare(int left, int right);
    }

    /** Stable iterative merge; no boxed integer indexes or per-entry objects. */
    private static void sortIndexes(int[] order, IntPairOrder comparison) {
        if (order.length < 2) return;
        int[] input = order;
        int[] output = new int[order.length];
        for (long width = 1; width < order.length; width *= 2) {
            for (long start = 0; start < order.length; start += width * 2) {
                final int middle = (int) Math.min(start + width, order.length);
                final int end = (int) Math.min(start + width * 2, order.length);
                int left = (int) start;
                int right = middle;
                int target = (int) start;
                while (left < middle && right < end) {
                    output[target++] = comparison.compare(input[left], input[right]) <= 0
                            ? input[left++] : input[right++];
                }
                while (left < middle) output[target++] = input[left++];
                while (right < end) output[target++] = input[right++];
            }
            final int[] previous = input;
            input = output;
            output = previous;
        }
        if (input != order) System.arraycopy(input, 0, order, 0, order.length);
    }

    private static final class PostingBuilder {
        private String[] keys;
        private int[] rows;
        private int used;

        PostingBuilder(int initial) {
            keys = new String[initial];
            rows = new int[initial];
        }

        void add(String key, int row) {
            if (used == keys.length) {
                final int capacity = Math.addExact(used, Math.max(8, used >>> 1));
                keys = Arrays.copyOf(keys, capacity);
                rows = Arrays.copyOf(rows, capacity);
            }
            keys[used] = key;
            rows[used++] = row;
        }

        String[] keys() {
            prepare();
            return keys;
        }

        int[] rows() {
            return rows;
        }

        private void prepare() {
            final int[] order = new int[used];
            for (int i = 0; i < used; i++) order[i] = i;
            sortIndexes(order, (left, right) -> {
                final int byToken = keys[left].compareTo(keys[right]);
                return byToken != 0 ? byToken : Integer.compare(rows[left], rows[right]);
            });
            final String[] sortedKeys = new String[used];
            final int[] sortedRows = new int[used];
            for (int i = 0; i < used; i++) {
                sortedKeys[i] = keys[order[i]];
                sortedRows[i] = rows[order[i]];
            }
            keys = sortedKeys;
            rows = sortedRows;
        }
    }
}
