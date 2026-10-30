// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Structured JSONL receipts and content hashes shared by atomize, pattern, gate and chain.
 *
 * <p>Lines carry no wall-clock time, so identical inputs give byte-identical receipts; time lives
 * only in M3RecipeMemory rows. Ported from Synexia M3MopReceipt (#13966), schema unchanged.</p>
 */
//FLLDCIM: MOP — every run writes a deterministic JSONL receipt
public final class M3MopReceipt {
    public static final String SCHEMA = "M3-MOP-RECEIPT/1";
    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";
    public static final String REFUSED = "REFUSED";
    public static final String HOLD = "HOLD";

    private M3MopReceipt() { }

    /** One receipt line: schema, goal, stage and status first, then the ordered fields. */
    public static String line(String goal, String stage, String status, Map<String, ?> fields) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("schema", SCHEMA);
        ordered.put("goal", goal);
        ordered.put("stage", stage);
        ordered.put("status", status);
        ordered.putAll(fields);
        return json(ordered);
    }

    /** Ordered flat JSON object; numbers and booleans raw, everything else a JSON string. */
    public static String json(Map<String, ?> fields) {
        return fields.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ':' + value(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    /** Replaces {@code file} atomically with the given lines (each terminated by '\n'). */
    public static Path write(Path file, List<String> lines) throws IOException {
        return writeBytes(file, lines.stream().map(line -> line + '\n').collect(Collectors.joining())
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Replaces {@code file} atomically with {@code bytes}. */
    public static Path writeBytes(Path file, byte[] bytes) throws IOException {
        Path target = file.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".m3-mop-", ".tmp");
        try {
            Files.write(temporary, bytes);
            move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return target;
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Domain-separated, length-prefixed SHA-256 of an ordered value list. */
    public static String canonical(String domain, String... values) {
        return sha256(Stream.concat(Stream.of(domain), Stream.of(values))
                .map(value -> value.length() + ":" + value + ";").collect(Collectors.joining()));
    }

    private static void move(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String value(Object value) {
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return quote(String.valueOf(value));
    }

    static String quote(String value) {
        return value.codePoints().mapToObj(M3MopReceipt::escape)
                .collect(Collectors.joining("", "\"", "\""));
    }

    private static String escape(int codePoint) {
        return switch (codePoint) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> codePoint < 0x20 ? String.format(Locale.ROOT, "\\u%04x", codePoint)
                    : Character.toString(codePoint);
        };
    }
}
