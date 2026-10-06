/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only catalog over an exported Synexia shard manifest.
 *
 * <p>The catalog composes already-mapped M3LEX images. It retains shard
 * coordinates and never joins image payloads into one Java array. A caller
 * receives a String only at the explicit {@link #textAt(Coordinate)} boundary.
 * The manifest is checked before publication so a path, checksum, ordering or
 * sidecar encoding mistake cannot become a live lookup owner.</p>
 */
public final class SharedLexiconCatalog {
    private static final String[] HEADER = {
            "shard_id", "file", "first_lexeme", "last_lexeme", "image_records",
            "utf16_units", "sha256"
    };

    private final List<SharedLexiconImage> images;
    private final List<String> files;
    private final List<String> firstLexemes;
    private final List<String> lastLexemes;
    private final long recordCount;

    private SharedLexiconCatalog(List<SharedLexiconImage> images, List<String> files,
                                 List<String> firstLexemes, List<String> lastLexemes,
                                 long recordCount) {
        this.images = List.copyOf(images);
        this.files = List.copyOf(files);
        this.firstLexemes = List.copyOf(firstLexemes);
        this.lastLexemes = List.copyOf(lastLexemes);
        this.recordCount = recordCount;
    }

    /** Opens and validates the complete catalog before publishing it. */
    public static SharedLexiconCatalog open(Path exportDirectory) throws IOException {
        Objects.requireNonNull(exportDirectory);
        Path directory = exportDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) throw new IOException("not a catalog directory");
        Path manifest = directory.resolve("synexia.shards.tsv");
        List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(String.join("\t", HEADER)))
            throw new IOException("invalid shard manifest header");

        List<SharedLexiconImage> images = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> firstLexemes = new ArrayList<>();
        List<String> lastLexemes = new ArrayList<>();
        long recordCount = 0;
        String previousLast = null;
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.isEmpty()) throw malformed(lineNumber, "empty row");
            String[] fields = line.split("\\t", -1);
            if (fields.length != HEADER.length) throw malformed(lineNumber, "wrong field count");
            int shardId = parseNonNegativeInt(fields[0], lineNumber, "shard_id");
            if (shardId != images.size()) throw malformed(lineNumber, "non-contiguous shard_id");
            String fileName = fields[1];
            Path imagePath = safeChild(directory, fileName, lineNumber);
            String first = decodeSidecarText(fields[2], lineNumber);
            String last = decodeSidecarText(fields[3], lineNumber);
            int expectedRecords = parseNonNegativeInt(fields[4], lineNumber, "image_records");
            long expectedUnits = parseNonNegativeLong(fields[5], lineNumber, "utf16_units");
            byte[] expectedDigest = parseDigest(fields[6], lineNumber);
            if (expectedRecords == 0 || first.compareTo(last) > 0)
                throw malformed(lineNumber, "empty or inverted shard bounds");
            if (previousLast != null && previousLast.compareTo(first) >= 0)
                throw malformed(lineNumber, "shards are not globally UTF-16 sorted");

            byte[] actualDigest = sha256(imagePath);
            if (!MessageDigest.isEqual(expectedDigest, actualDigest))
                throw malformed(lineNumber, "shard SHA-256 mismatch");
            SharedLexiconImage image = SharedLexiconImage.open(imagePath);
            if (image.version() != 2 || image.size() != expectedRecords
                    || image.utf16Units() != expectedUnits
                    || !image.recordText(0).equals(first)
                    || !image.recordText(image.size() - 1).equals(last))
                throw malformed(lineNumber, "shard metadata does not match image");

            images.add(image);
            files.add(fileName);
            firstLexemes.add(first);
            lastLexemes.add(last);
            recordCount = Math.addExact(recordCount, expectedRecords);
            previousLast = last;
        }
        if (images.isEmpty()) throw new IOException("empty shard manifest");
        return new SharedLexiconCatalog(images, files, firstLexemes, lastLexemes, recordCount);
    }

    public record Coordinate(int shardId, int imageRow) { }

    public int shardCount() { return images.size(); }
    public long recordCount() { return recordCount; }
    public List<String> shardFiles() { return files; }

    /** Explicitly warms all mapped shards; this does not flatten their payloads. */
    public void warm() { for (SharedLexiconImage image : images) image.warm(); }

    /** Finds an exact UTF-16 record and returns its stable shard/image coordinate. */
    public Optional<Coordinate> find(String text) {
        Objects.requireNonNull(text);
        int low = 0, high = firstLexemes.size() - 1, candidate = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (firstLexemes.get(middle).compareTo(text) <= 0) {
                candidate = middle;
                low = middle + 1;
            } else high = middle - 1;
        }
        if (candidate < 0 || lastLexemes.get(candidate).compareTo(text) < 0)
            return Optional.empty();
        int row = images.get(candidate).findRecord(text);
        return row < 0 ? Optional.empty() : Optional.of(new Coordinate(candidate, row));
    }

    /** Materializes exactly one requested UTF-16 record. */
    public String textAt(Coordinate coordinate) {
        Objects.requireNonNull(coordinate);
        if (coordinate.shardId() < 0 || coordinate.shardId() >= images.size())
            throw new IndexOutOfBoundsException("shardId=" + coordinate.shardId());
        return images.get(coordinate.shardId()).recordText(coordinate.imageRow());
    }

    private static Path safeChild(Path directory, String fileName, int lineNumber) throws IOException {
        if (fileName.isEmpty() || fileName.equals(".") || fileName.equals("..")
                || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0)
            throw malformed(lineNumber, "unsafe shard file");
        Path path = directory.resolve(fileName).normalize();
        if (!path.getParent().equals(directory) || !path.getFileName().toString().equals(fileName))
            throw malformed(lineNumber, "unsafe shard file");
        return path;
    }

    private static int parseNonNegativeInt(String value, int lineNumber, String field) throws IOException {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException failure) {
            throw malformed(lineNumber, "invalid " + field);
        }
    }

    private static long parseNonNegativeLong(String value, int lineNumber, String field) throws IOException {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException failure) {
            throw malformed(lineNumber, "invalid " + field);
        }
    }

    private static byte[] parseDigest(String value, int lineNumber) throws IOException {
        if (value.length() != 64) throw malformed(lineNumber, "invalid sha256 length");
        try { return HexFormat.of().parseHex(value); }
        catch (IllegalArgumentException failure) { throw malformed(lineNumber, "invalid sha256"); }
    }

    private static String decodeSidecarText(String value, int lineNumber) throws IOException {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int at = 0; at < value.length();) {
            char current = value.charAt(at++);
            if (current != '\\') {
                decoded.append(current);
                continue;
            }
            if (at + 5 > value.length() || value.charAt(at) != 'u')
                throw malformed(lineNumber, "invalid UTF-16 sidecar escape");
            int unit = 0;
            for (int digit = 1; digit <= 4; digit++) {
                int nibble = Character.digit(value.charAt(at + digit), 16);
                if (nibble < 0) throw malformed(lineNumber, "invalid UTF-16 sidecar escape");
                unit = (unit << 4) | nibble;
            }
            decoded.append((char)unit);
            at += 5;
        }
        return decoded.toString();
    }

    private static byte[] sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0;) {
                    if (read != 0) digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static IOException malformed(int lineNumber, String detail) {
        return new IOException("invalid synexia.shards.tsv line " + (lineNumber + 1) + ": " + detail);
    }
}
