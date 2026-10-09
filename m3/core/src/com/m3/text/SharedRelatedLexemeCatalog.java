/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable directed related-lexeme sidecar.
 *
 * <p>The sidecar is metadata, not String identity. It preserves the opaque
 * Synexia source identity, exact UTF-16 lexeme text, direction, and the
 * physical coordinate already admitted by {@link SharedLexiconCatalog}.</p>
 */
public final class SharedRelatedLexemeCatalog implements AutoCloseable {
    public static final String FILE = "synexia.related.tsv";
    private static final String HEADER =
            "source_id\trecord_id\tlexeme\trelated_lexeme\tshard_id\timage_row";
    private static final String MAPPING_HEADER =
            "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\t"
                    + "shard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\t"
                    + "precompute_profile\tprecompute_payload";

    private final List<Relation> relations;
    private final Map<SharedLexiconCatalog.SourceIdentity, List<Relation>> byIdentity;
    private volatile boolean closed;

    public record Relation(String sourceId, String recordId, String lexeme,
                           String relatedLexeme, SharedLexiconCatalog.Coordinate coordinate) {
        public Relation {
            Objects.requireNonNull(sourceId);
            Objects.requireNonNull(recordId);
            Objects.requireNonNull(lexeme);
            Objects.requireNonNull(relatedLexeme);
            Objects.requireNonNull(coordinate);
            if (sourceId.isEmpty() || recordId.isEmpty() || lexeme.isEmpty()
                    || relatedLexeme.isEmpty()) throw new IllegalArgumentException("empty relation");
        }

        public SharedLexiconCatalog.SourceIdentity identity() {
            return new SharedLexiconCatalog.SourceIdentity(sourceId, recordId);
        }
    }

    public static Optional<SharedRelatedLexemeCatalog> openOptional(Path exportDirectory)
            throws IOException {
        Path directory = Objects.requireNonNull(exportDirectory).toAbsolutePath().normalize();
        Path file = directory.resolve(FILE);
        return Files.isRegularFile(file)
                ? Optional.of(open(directory))
                : Optional.empty();
    }

    public static SharedRelatedLexemeCatalog open(Path exportDirectory) throws IOException {
        Path directory = Objects.requireNonNull(exportDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) throw new IOException("not a catalog directory");
        Map<SharedLexiconCatalog.SourceIdentity, Mapping> mappings =
                readMappings(directory.resolve("synexia.records.tsv"));
        Path file = directory.resolve(FILE);
        if (!Files.isRegularFile(file)) throw new IOException("related sidecar missing");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0)))
            throw new IOException("invalid related sidecar header");
        List<Relation> result = new ArrayList<>();
        Map<SharedLexiconCatalog.SourceIdentity, List<Relation>> byIdentity = new HashMap<>();
        Relation previous = null;
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            List<String> cells = parseTsvLine(lines.get(lineNumber), lineNumber);
            if (cells.size() != 6) throw malformed(lineNumber, "wrong field count");
            String sourceId = cells.get(0);
            String recordId = cells.get(1);
            String lexeme = decodeSidecarText(cells.get(2), lineNumber);
            String related = decodeSidecarText(cells.get(3), lineNumber);
            SharedLexiconCatalog.Coordinate coordinate = new SharedLexiconCatalog.Coordinate(
                    parseNonNegative(cells.get(4), lineNumber, "shard_id"),
                    parseNonNegative(cells.get(5), lineNumber, "image_row"));
            Relation relation = new Relation(sourceId, recordId, lexeme, related, coordinate);
            if (previous != null && compare(previous, relation) >= 0)
                throw malformed(lineNumber, "relations are not sorted or are duplicated");
            Mapping mapping = mappings.get(relation.identity());
            if (mapping == null) throw malformed(lineNumber, "relation identity is not mapped");
            if (!mapping.lexeme().equals(lexeme) || !mapping.coordinate().equals(coordinate))
                throw malformed(lineNumber, "relation mapping mismatch");
            byIdentity.computeIfAbsent(relation.identity(), ignored -> new ArrayList<>()).add(relation);
            result.add(relation);
            previous = relation;
        }
        return new SharedRelatedLexemeCatalog(result, byIdentity);
    }

    private SharedRelatedLexemeCatalog(List<Relation> relations,
                                       Map<SharedLexiconCatalog.SourceIdentity, List<Relation>> byIdentity) {
        this.relations = List.copyOf(relations);
        Map<SharedLexiconCatalog.SourceIdentity, List<Relation>> copied = new HashMap<>();
        byIdentity.forEach((identity, values) -> copied.put(identity, List.copyOf(values)));
        this.byIdentity = Map.copyOf(copied);
    }

    public List<Relation> relations() {
        ensureOpen();
        return relations;
    }

    /**
     * Returns the single relation for an identity when the normalized source has
     * exactly one target. Multi-target records must use {@link #findAll}.
     */
    public Optional<Relation> find(String sourceId, String recordId) {
        ensureOpen();
        List<Relation> values = findAll(sourceId, recordId);
        return values.size() == 1 ? Optional.of(values.get(0)) : Optional.empty();
    }

    public List<Relation> findAll(String sourceId, String recordId) {
        ensureOpen();
        List<Relation> values = byIdentity.get(new SharedLexiconCatalog.SourceIdentity(
                Objects.requireNonNull(sourceId), Objects.requireNonNull(recordId)));
        return values == null ? List.of() : values;
    }

    public List<Relation> findBySource(String sourceId) {
        ensureOpen();
        Objects.requireNonNull(sourceId);
        return relations.stream().filter(relation -> relation.sourceId().equals(sourceId)).toList();
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("related catalog closed");
    }

    private static List<String> parseTsvLine(String line, int lineNumber) throws IOException {
        List<String> cells = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int at = 0; at < line.length(); at++) {
            char current = line.charAt(at);
            if (quoted) {
                if (current == '"') {
                    if (at + 1 < line.length() && line.charAt(at + 1) == '"') {
                        field.append('"');
                        at++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else {
                    field.append(current);
                }
            } else if (closed) {
                if (current != '	') throw malformed(lineNumber, "invalid quoted TSV field");
                cells.add(field.toString());
                field.setLength(0);
                closed = false;
            } else if (current == '	') {
                cells.add(field.toString());
                field.setLength(0);
            } else if (current == '"' && field.isEmpty()) {
                quoted = true;
            } else {
                field.append(current);
            }
        }
        if (quoted) throw malformed(lineNumber, "unterminated quoted TSV field");
        cells.add(field.toString());
        return List.copyOf(cells);
    }

    private static Map<SharedLexiconCatalog.SourceIdentity, Mapping> readMappings(Path path)
            throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("mapping sidecar missing");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !MAPPING_HEADER.equals(lines.get(0)))
            throw new IOException("invalid mapping sidecar header");
        Map<SharedLexiconCatalog.SourceIdentity, Mapping> result = new HashMap<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            List<String> cells = parseTsvLine(lines.get(lineNumber), lineNumber);
            if (cells.size() != 13) throw malformed(lineNumber, "mapping field count");
            SharedLexiconCatalog.SourceIdentity identity =
                    new SharedLexiconCatalog.SourceIdentity(cells.get(0), cells.get(4));
            Mapping mapping = new Mapping(decodeSidecarText(cells.get(5), lineNumber),
                    new SharedLexiconCatalog.Coordinate(
                            parseNonNegative(cells.get(6), lineNumber, "shard_id"),
                            parseNonNegative(cells.get(7), lineNumber, "image_row")));
            if (result.put(identity, mapping) != null)
                throw malformed(lineNumber, "duplicate mapping identity");
        }
        return result;
    }

    private record Mapping(String lexeme, SharedLexiconCatalog.Coordinate coordinate) { }

    private static int compare(Relation left, Relation right) {
        int value = left.sourceId().compareTo(right.sourceId());
        if (value != 0) return value;
        value = left.recordId().compareTo(right.recordId());
        if (value != 0) return value;
        value = left.lexeme().compareTo(right.lexeme());
        if (value != 0) return value;
        return left.relatedLexeme().compareTo(right.relatedLexeme());
    }

    private static int parseNonNegative(String value, int lineNumber, String field)
            throws IOException {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException failure) {
            throw malformed(lineNumber, "invalid " + field);
        }
    }

    private static IOException malformed(int lineNumber, String message) {
        return new IOException("synexia.related.tsv:" + (lineNumber + 1) + ": " + message);
    }

    private static String decodeSidecarText(String value, int lineNumber) throws IOException {
        StringBuilder decoded = new StringBuilder();
        for (int at = 0; at < value.length();) {
            char current = value.charAt(at++);
            if (current != '\\') {
                decoded.append(current);
                continue;
            }
            if (at < value.length() && value.charAt(at) == '\\') {
                decoded.append('\\');
                at++;
                continue;
            }
            if (at + 5 > value.length() || value.charAt(at) != 'u') {
                throw malformed(lineNumber, "invalid UTF-16 escape");
            }
            String hex = value.substring(at + 1, at + 5);
            try {
                decoded.append((char) Integer.parseInt(hex, 16));
            } catch (NumberFormatException failure) {
                throw malformed(lineNumber, "invalid UTF-16 escape");
            }
            at += 5;
        }
        return decoded.toString();
    }
}
