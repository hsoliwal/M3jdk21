// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21: distinct pending-only profile for the new Maven4 packet; derived from the qualified integrated helper.
package com.synexia.rewrite.memory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.synexia.rewrite.sealed.SealHash;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Bounded authoring/proof helper for this crate's pending correction records; no runtime engine. */
public final class M3RecoveryMemory {
    static final int MAX_BYTES = 1_048_576;
    static final int MAX_ARTIFACT_BYTES = 67_108_864;
    static final int MAX_HISTORY = 256;
    static final String PROFILE = "m3-maven4-effective-20261021/1.0";
    private static final JsonFactory JSON = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64)
                    .maxNumberLength(20).maxStringLength(262_144).build()).build();
    private static final Set<String> STATUSES = Set.of("PASS", "EXPECTED_FAILURE", "FAIL",
            "NOT_RUN", "BLOCKED", "PENDING");
    private M3RecoveryMemory() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw invalid("usage: canonicalize input | check event root events | publish input root events");
        Path input = Path.of(args[1]);
        byte[] data = readBounded(input);
        Object value = parse(data);
        validate(value);
        byte[] canonical = canonicalBytes(value);
        switch (args[0]) {
            case "canonicalize" -> {
                if (args.length != 2) throw invalid("canonicalize arguments");
                System.out.write(canonical);
            }
            case "check" -> {
                if (args.length != 4) throw invalid("check arguments");
                if (!Arrays.equals(data, canonical)) throw invalid("noncanonical event bytes");
                String digest = SealHash.bytes(canonical);
                if (!input.getFileName().toString().equals(digest + ".json")) throw invalid("event filename digest");
                verify(value, Path.of(args[2]), Path.of(args[3]));
                System.out.println("MOP_MEMORY_CHECK PASS digest=" + digest + " admission=PENDING");
            }
            case "publish" -> {
                if (args.length != 4) throw invalid("publish arguments");
                Path output = publish(value, Path.of(args[2]), Path.of(args[3]));
                System.out.println("MOP_MEMORY_PUBLISH " + output + " admission=PENDING");
            }
            default -> throw invalid("unknown command");
        }
    }

    static Object parse(byte[] data) throws IOException {
        if (data.length > MAX_BYTES) throw invalid("record byte bound");
        String text = SealHash.decode(data);
        try (JsonParser parser = JSON.createParser(text)) {
            if (parser.nextToken() == null) throw invalid("empty JSON");
            Object result = readValue(parser);
            if (parser.nextToken() != null) throw invalid("trailing JSON");
            return result;
        }
    }

    private static Object readValue(JsonParser parser) throws IOException {
        return switch (parser.currentToken()) {
            case START_OBJECT -> {
                Map<String, Object> object = new TreeMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() != JsonToken.FIELD_NAME) throw invalid("object field");
                    String name = parser.currentName();
                    if (parser.nextToken() == null) throw invalid("missing value");
                    object.put(name, readValue(parser));
                }
                yield object;
            }
            case START_ARRAY -> {
                List<Object> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (parser.currentToken() == null) throw invalid("unterminated array");
                    values.add(readValue(parser));
                }
                yield values;
            }
            case VALUE_STRING -> parser.getText();
            case VALUE_NUMBER_INT -> {
                if (!parser.getText().matches("0|-?[1-9][0-9]*")) throw invalid("noncanonical integer spelling");
                yield parser.getLongValue();
            }
            case VALUE_TRUE -> Boolean.TRUE;
            case VALUE_FALSE -> Boolean.FALSE;
            case VALUE_NULL -> null;
            default -> throw invalid("only signed 64-bit integer JSON numbers are allowed");
        };
    }

    static byte[] canonicalBytes(Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value, 0);
        byte[] bytes = SealHash.utf8(output.toString());
        if (bytes.length > MAX_BYTES) throw invalid("canonical byte bound");
        return bytes;
    }

    private static void append(StringBuilder out, Object value, int depth) {
        if (depth > 64) throw invalid("canonical depth bound");
        if (value == null) out.append("null");
        else if (value instanceof String text) quote(out, text);
        else if (value instanceof Boolean || value instanceof Long || value instanceof Integer) out.append(value);
        else if (value instanceof List<?> values) {
            out.append('[');
            for (int index = 0; index < values.size(); index++) {
                if (index != 0) out.append(',');
                append(out, values.get(index), depth + 1);
            }
            out.append(']');
        } else if (value instanceof Map<?, ?> values) {
            TreeMap<String, Object> ordered = new TreeMap<>();
            for (var entry : values.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !key.matches("[\\x20-\\x7e]+"))
                    throw invalid("schema keys must be ASCII");
                ordered.put(key, entry.getValue());
            }
            out.append('{');
            boolean first = true;
            for (var entry : ordered.entrySet()) {
                if (!first) out.append(',');
                first = false;
                quote(out, entry.getKey()); out.append(':');
                append(out, entry.getValue(), depth + 1);
            }
            out.append('}');
        } else throw invalid("unsupported JSON value");
    }

    private static void quote(StringBuilder out, String text) {
        SealHash.utf8(text); // Existing canonical owner rejects unpaired surrogates.
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            char unit = text.charAt(index);
            if (unit == '"' || unit == '\\') out.append('\\').append(unit);
            else if (unit < 0x20) {
                out.append("\\u00");
                out.append(Character.forDigit(unit >>> 4, 16));
                out.append(Character.forDigit(unit & 15, 16));
            } else out.append(unit);
        }
        out.append('"');
    }

    static void validate(Object value) {
        Map<?, ?> event = object(value, "schema", "profile", "recipe_id", "event_type", "admission",
                "source", "parents", "supersedes", "recipe_before", "recipe_after", "source_manifest_before",
                "source_manifest_after", "observation_epoch_seconds", "author", "license", "trigger",
                "root_cause", "change", "lesson", "applicability", "exclusions", "known_failures",
                "pending_gates", "unknowns", "atoms", "regression_fixtures", "verification", "replay", "environment");
        equal(event.get("schema"), "MOP-RCM/1.0", "schema");
        equal(event.get("profile"), PROFILE, "profile");
        equal(event.get("event_type"), "RECORDED", "event_type");
        equal(event.get("admission"), "PENDING", "admission");
        equal(event.get("license"), "Apache-2.0", "license");
        for (String name : List.of("recipe_id", "author", "trigger", "change", "lesson", "environment")) text(event.get(name));
        Map<?, ?> source = object(event.get("source"), "repository", "revision");
        text(source.get("repository"));
        if (!text(source.get("revision")).matches("[0-9a-f]{40}")) throw invalid("source revision");
        digestSet(event.get("parents")); digestSet(event.get("supersedes"));
        artifact(event.get("recipe_after")); artifact(event.get("source_manifest_before")); artifact(event.get("source_manifest_after"));
        if (event.get("recipe_before") != null) artifact(event.get("recipe_before"));
        if (!(event.get("observation_epoch_seconds") instanceof Long)) throw invalid("integer observation_epoch_seconds");
        Map<?, ?> cause = object(event.get("root_cause"), "status", "explanation");
        if (!Set.of("observed", "hypothesized", "disproved", "unresolved").contains(cause.get("status"))) throw invalid("root cause status");
        text(cause.get("explanation"));
        for (String name : List.of("applicability", "exclusions", "known_failures", "pending_gates", "unknowns")) texts(event.get(name));
        if (event.get("recipe_before") == null && list(event.get("unknowns")).isEmpty()) throw invalid("unknown predecessor needs explanation");
        if (list(event.get("atoms")).isEmpty()) throw invalid("at least one contract-bearing atom required");
        for (Object member : list(event.get("atoms"))) {
            Map<?, ?> atom = object(member, "id", "owner", "contract", "pattern", "applicability", "exclusions");
            for (String field : List.of("id", "owner", "contract", "pattern")) text(atom.get(field));
            texts(atom.get("applicability")); texts(atom.get("exclusions"));
        }
        for (Object fixture : list(event.get("regression_fixtures"))) artifact(fixture);
        if (list(event.get("regression_fixtures")).isEmpty()) throw invalid("regression fixture required");
        for (Object item : list(event.get("verification"))) {
            Map<?, ?> gate = object(item, "gate", "status", "command", "evidence");
            text(gate.get("gate")); gate(gate);
        }
        Map<?, ?> replay = object(event.get("replay"), "status", "command", "evidence");
        gate(replay);
        canonicalBytes(value); // Validate every scalar and key before accepting the record.
    }

    private static void gate(Map<?, ?> gate) {
        if (!STATUSES.contains(gate.get("status"))) throw invalid("verification status");
        texts(gate.get("command"));
        List<?> evidence = list(gate.get("evidence"));
        for (Object artifact : evidence) artifact(artifact);
        if (Set.of("PASS", "FAIL", "EXPECTED_FAILURE").contains(gate.get("status"))
                && (evidence.isEmpty() || list(gate.get("command")).isEmpty())) throw invalid("executed gate requires command and evidence");
    }

    private static void artifact(Object value) {
        Map<?, ?> artifact = object(value, "path", "sha256");
        String path = text(artifact.get("path"));
        Path parsed = Path.of(path);
        if (parsed.isAbsolute() || path.contains("\\") || path.chars().anyMatch(Character::isISOControl)
                || !parsed.normalize().toString().equals(path) || path.equals(".") || path.startsWith("../"))
            throw invalid("artifact path must be a normalized relative path");
        digest(artifact.get("sha256"));
    }

    private static void digestSet(Object value) {
        String previous = null;
        for (Object member : list(value)) {
            String next = digest(member);
            if (previous != null && previous.compareTo(next) >= 0) throw invalid("digest set must be unique and sorted");
            previous = next;
        }
    }

    private static String digest(Object value) { return SealHash.require(text(value)); }
    private static String text(Object value) {
        if (!(value instanceof String text) || text.isEmpty()) throw invalid("nonempty text required");
        SealHash.utf8(text);
        return text;
    }
    private static void texts(Object value) { for (Object member : list(value)) text(member); }
    private static List<?> list(Object value) {
        if (!(value instanceof List<?> list)) throw invalid("array required");
        return list;
    }
    private static Map<?, ?> object(Object value, String... fields) {
        if (!(value instanceof Map<?, ?> object) || !object.keySet().equals(Set.of(fields))) throw invalid("missing or unknown schema fields");
        return object;
    }
    private static void equal(Object value, String expected, String field) {
        if (!expected.equals(value)) throw invalid("unsupported " + field);
    }

    static void verify(Object value, Path root, Path events) throws IOException {
        validate(value);
        Path realRoot = root.toRealPath();
        Map<?, ?> event = (Map<?, ?>) value;
        verifyArtifacts(value, realRoot);
        Set<String> visited = new HashSet<>();
        verifyHistory(event, text(event.get("recipe_id")), realRoot, events, visited);
    }

    private static void verifyHistory(Map<?, ?> event, String recipe, Path root, Path events,
                                      Set<String> visited) throws IOException {
        for (String field : List.of("parents", "supersedes")) {
            for (Object member : list(event.get(field))) {
                String id = digest(member);
                if (!visited.add(id)) continue;
                if (visited.size() > MAX_HISTORY) throw invalid("history bound exceeded");
                Path file = events.resolve(id + ".json");
                if (Files.isSymbolicLink(file)) throw invalid("event symlink");
                byte[] bytes = readBounded(file);
                if (!SealHash.bytes(bytes).equals(id)) throw invalid("parent content digest mismatch");
                Object parentValue = parse(bytes); validate(parentValue);
                if (!Arrays.equals(bytes, canonicalBytes(parentValue))) throw invalid("noncanonical parent");
                Map<?, ?> parent = (Map<?, ?>) parentValue;
                equal(parent.get("recipe_id"), recipe, "parent recipe identity");
                verifyArtifacts(parent, root);
                verifyHistory(parent, recipe, root, events, visited);
            }
        }
    }

    private static void verifyArtifacts(Object value, Path root) throws IOException {
        if (value instanceof Map<?, ?> object) {
            if (object.keySet().equals(Set.of("path", "sha256"))) {
                Path file = root.resolve(text(object.get("path"))).toRealPath();
                if (!file.startsWith(root) || !Files.isRegularFile(file)) throw invalid("artifact outside root or not regular");
                try (var stream = Files.newInputStream(file)) {
                    if (Files.size(file) > MAX_ARTIFACT_BYTES) throw invalid("artifact byte bound");
                    byte[] bytes = stream.readNBytes(MAX_ARTIFACT_BYTES + 1);
                    if (bytes.length > MAX_ARTIFACT_BYTES) throw invalid("artifact byte bound");
                    if (!SealHash.bytes(bytes).equals(object.get("sha256"))) throw invalid("artifact digest mismatch: " + object.get("path"));
                }
            } else for (Object member : object.values()) verifyArtifacts(member, root);
        } else if (value instanceof List<?> list) for (Object member : list) verifyArtifacts(member, root);
    }

    static Path publish(Object value, Path root, Path events) throws IOException {
        verify(value, root, events);
        byte[] bytes = canonicalBytes(value);
        Files.createDirectories(events);
        Path target = events.resolve(SealHash.bytes(bytes) + ".json");
        Path temporary = Files.createTempFile(events, ".pending-", ".json");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                // A same-filesystem hard link publishes complete bytes with CREATE_NEW semantics.
                // Unsupported hard-link filesystems fail closed; no partial-copy fallback exists.
                Files.createLink(target, temporary);
            } catch (FileAlreadyExistsException existing) {
                if (Files.isSymbolicLink(target) || !Arrays.equals(readBounded(target), bytes))
                    throw invalid("existing event differs; overwrite refused");
            }
            return target;
        } finally { Files.deleteIfExists(temporary); }
    }

    private static byte[] readBounded(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) throw invalid("record byte bound");
        try (var stream = Files.newInputStream(path)) {
            byte[] result = stream.readNBytes(MAX_BYTES + 1);
            if (result.length > MAX_BYTES) throw invalid("record byte bound");
            return result;
        }
    }
    private static IllegalArgumentException invalid(String reason) { return new IllegalArgumentException(reason); }
}
