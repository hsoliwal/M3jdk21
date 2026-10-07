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
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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
    private static final String[] MAPPING_HEADER = {
            "source_id", "source_path", "source_kind", "language_tag", "record_id", "lexeme",
            "shard_id", "image_row", "mapping_id", "mapping_name", "translation_profile",
            "precompute_profile", "precompute_payload"
    };
    private static final String[] PRECOMPUTE_HEADER = {
            "shard_id", "image_row", "utf16_units", "java_hash", "code_points",
            "unpaired_surrogates", "non_bmp_code_points", "ascii", "latin1",
            "contains_whitespace", "precompute_profile"
    };
    private static final String[] PROFILE_HEADER = {
            "precompute_profile", "source_records", "image_records", "sha256"
    };

    private final List<SharedLexiconImage> images;
    private final List<String> files;
    private final List<String> firstLexemes;
    private final List<String> lastLexemes;
    private final Map<Coordinate, List<SourceMapping>> mappings;
    private final Map<SourceIdentity, SourceMapping> mappingsByIdentity;
    private final Map<Coordinate, PrecomputeFacts> precompute;
    private final List<PrecomputeProfile> precomputeProfiles;
    private final long recordCount;

    private SharedLexiconCatalog(List<SharedLexiconImage> images, List<String> files,
                                 List<String> firstLexemes, List<String> lastLexemes,
                                 Map<Coordinate, List<SourceMapping>> mappings,
                                 Map<Coordinate, PrecomputeFacts> precompute,
                                 List<PrecomputeProfile> precomputeProfiles,
                                 long recordCount) {
        this.images = List.copyOf(images);
        this.files = List.copyOf(files);
        this.firstLexemes = List.copyOf(firstLexemes);
        this.lastLexemes = List.copyOf(lastLexemes);
        Map<Coordinate, List<SourceMapping>> mappingCopy = new HashMap<>();
        mappings.forEach((coordinate, values) -> mappingCopy.put(coordinate, List.copyOf(values)));
        this.mappings = Map.copyOf(mappingCopy);
        Map<SourceIdentity, SourceMapping> identityCopy = new HashMap<>();
        this.mappings.values().forEach(values -> values.forEach(mapping ->
                identityCopy.put(new SourceIdentity(mapping.sourceId(), mapping.recordId()), mapping)));
        this.mappingsByIdentity = Map.copyOf(identityCopy);
        this.precompute = Map.copyOf(precompute);
        this.precomputeProfiles = List.copyOf(precomputeProfiles);
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
        Map<Coordinate, List<SourceMapping>> mappings = readMappings(directory, images);
        Map<Coordinate, PrecomputeFacts> precompute = readPrecompute(directory, images);
        if (mappings.size() != recordCount || precompute.size() != recordCount)
            throw new IOException("metadata coverage does not match image records");
        validatePrecomputeProfiles(mappings, precompute);
        List<PrecomputeProfile> profiles = readPrecomputeProfiles(directory, mappings);
        return new SharedLexiconCatalog(images, files, firstLexemes, lastLexemes,
                mappings, precompute, profiles, recordCount);
    }

    public record Coordinate(int shardId, int imageRow) { }

    /** Stable Synexia identity, independent of the physical M3LEX projection. */
    public record SourceIdentity(String sourceId, String recordId) { }

    /** One preserved Synexia source identity; several may point at one lexeme. */
    public record SourceMapping(String sourceId, String sourcePath, String sourceKind,
                                String languageTag, String recordId, String lexeme,
                                Coordinate coordinate, String mappingId, String mappingName,
                                String translationProfile, String precomputeProfile,
                                String precomputePayload) { }

    /** Exported facts are retained as data; the Python verifier remains their cross-language authority. */
    public record PrecomputeFacts(Coordinate coordinate, long utf16Units, long javaHash,
                                  int codePoints, int unpairedSurrogates, int nonBmpCodePoints,
                                  boolean ascii, boolean latin1, boolean containsWhitespace,
                                  String precomputeProfile) { }

    /** Deterministic descriptor for one Synexia precompute owner/profile. */
    public record PrecomputeProfile(String profile, long sourceRecords, long imageRecords,
                                    String fingerprint) { }

    public int shardCount() { return images.size(); }
    public long recordCount() { return recordCount; }
    public List<String> shardFiles() { return files; }
    public List<SourceMapping> mappingsAt(Coordinate coordinate) {
        requireCoordinate(coordinate);
        return mappings.get(coordinate);
    }
    /** Returns the preserved mapping for one exact Synexia source identity. */
    public Optional<SourceMapping> findMapping(String sourceId, String recordId) {
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(recordId);
        return Optional.ofNullable(mappingsByIdentity.get(new SourceIdentity(sourceId, recordId)));
    }
    /** Finds all source mappings attached to one exact UTF-16 lexeme. */
    public List<SourceMapping> findMappings(String text) {
        Optional<Coordinate> coordinate = find(text);
        return coordinate.isEmpty() ? List.of() : mappingsAt(coordinate.orElseThrow());
    }
    public PrecomputeFacts precomputeAt(Coordinate coordinate) {
        requireCoordinate(coordinate);
        return precompute.get(coordinate);
    }
    public List<PrecomputeProfile> precomputeProfiles() { return precomputeProfiles; }

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

    /**
     * Returns lexically ordered physical coordinates for an exact UTF-16 prefix.
     * Each shard uses a mapped-byte lower bound; no record text is copied while
     * the candidate range is located or verified.
     */
    public List<Coordinate> prefix(String value, int limit) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("prefix required");
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit must be 1..100000");
        ArrayList<Coordinate> result = new ArrayList<>(Math.min(limit, 16));
        for (int shard = 0; shard < images.size() && result.size() < limit; shard++) {
            SharedLexiconImage image = images.get(shard);
            for (int row = image.lowerBound(value);
                 row < image.size() && result.size() < limit && image.startsWith(row, value); row++) {
                result.add(new Coordinate(shard, row));
            }
        }
        return List.copyOf(result);
    }

    /** Materializes exactly one requested UTF-16 record. */
    public String textAt(Coordinate coordinate) {
        Objects.requireNonNull(coordinate);
        requireCoordinate(coordinate);
        return images.get(coordinate.shardId()).recordText(coordinate.imageRow());
    }

    private void requireCoordinate(Coordinate coordinate) {
        Objects.requireNonNull(coordinate);
        if (coordinate.shardId() < 0 || coordinate.shardId() >= images.size()
                || coordinate.imageRow() < 0
                || coordinate.imageRow() >= images.get(coordinate.shardId()).size())
            throw new IndexOutOfBoundsException("coordinate=" + coordinate);
    }

    private static Map<Coordinate, List<SourceMapping>> readMappings(
            Path directory, List<SharedLexiconImage> images) throws IOException {
        List<String> lines = readSidecar(directory.resolve("synexia.records.tsv"), MAPPING_HEADER);
        Map<Coordinate, List<SourceMapping>> result = new HashMap<>();
        java.util.Set<List<String>> identities = new java.util.HashSet<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String[] fields = splitSidecar(lines.get(lineNumber), MAPPING_HEADER.length, lineNumber);
            requireFields(fields, lineNumber);
            List<String> identity = List.of(fields[0], fields[4]);
            if (!identities.add(identity)) throw malformed(lineNumber, "duplicate source identity");
            Coordinate coordinate = coordinate(fields[6], fields[7], images, lineNumber);
            String lexeme = decodeSidecarText(fields[5], lineNumber);
            if (!imageText(images, coordinate).equals(lexeme))
                throw malformed(lineNumber, "mapping lexeme does not match image coordinate");
            SourceMapping mapping = new SourceMapping(fields[0], fields[1], fields[2], fields[3],
                    fields[4], lexeme, coordinate, fields[8], fields[9], fields[10], fields[11], fields[12]);
            result.computeIfAbsent(coordinate, ignored -> new ArrayList<>()).add(mapping);
        }
        if (lines.size() == 1) throw new IOException("empty synexia.records.tsv");
        return result;
    }

    private static Map<Coordinate, PrecomputeFacts> readPrecompute(
            Path directory, List<SharedLexiconImage> images) throws IOException {
        List<String> lines = readSidecar(directory.resolve("synexia.precompute.tsv"), PRECOMPUTE_HEADER);
        Map<Coordinate, PrecomputeFacts> result = new HashMap<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String[] fields = splitSidecar(lines.get(lineNumber), PRECOMPUTE_HEADER.length, lineNumber);
            requireFields(fields, lineNumber);
            Coordinate coordinate = coordinate(fields[0], fields[1], images, lineNumber);
            if (result.containsKey(coordinate)) throw malformed(lineNumber, "duplicate precompute coordinate");
            String text = imageText(images, coordinate);
            long units = parseNonNegativeLong(fields[2], lineNumber, "utf16_units");
            long hash = parseUnsignedHash(fields[3], lineNumber);
            if (units != text.length() || hash != Integer.toUnsignedLong(javaHash(text)))
                throw malformed(lineNumber, "precompute text facts do not match image");
            int codePoints = parseNonNegativeInt(fields[4], lineNumber, "code_points");
            int unpaired = parseNonNegativeInt(fields[5], lineNumber, "unpaired_surrogates");
            int nonBmp = parseNonNegativeInt(fields[6], lineNumber, "non_bmp_code_points");
            PrecomputeFacts facts = new PrecomputeFacts(coordinate, units, hash, codePoints,
                    unpaired, nonBmp, parseBoolean(fields[7], lineNumber),
                    parseBoolean(fields[8], lineNumber), parseBoolean(fields[9], lineNumber), fields[10]);
            result.put(coordinate, facts);
        }
        if (lines.size() == 1) throw new IOException("empty synexia.precompute.tsv");
        return result;
    }

    private static void validatePrecomputeProfiles(
            Map<Coordinate, List<SourceMapping>> mappings,
            Map<Coordinate, PrecomputeFacts> precompute) throws IOException {
        for (Map.Entry<Coordinate, List<SourceMapping>> entry : mappings.entrySet()) {
            String available = precompute.get(entry.getKey()).precomputeProfile();
            for (SourceMapping mapping : entry.getValue()) {
                if (!containsProfile(available, mapping.precomputeProfile()))
                    throw new IOException("precompute owner dropped for " + entry.getKey()
                            + ": " + mapping.precomputeProfile());
            }
        }
    }

    private static boolean containsProfile(String available, String wanted) {
        String[] availableParts = available.split(" \\+ ", -1);
        for (String wantedPart : wanted.split(" \\+ ", -1)) {
            boolean found = false;
            for (String availablePart : availableParts) {
                if (availablePart.equals(wantedPart)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static List<PrecomputeProfile> readPrecomputeProfiles(
            Path directory, Map<Coordinate, List<SourceMapping>> mappings) throws IOException {
        List<String> lines = readSidecar(directory.resolve("synexia.precompute-index.tsv"), PROFILE_HEADER);
        Map<String, Integer> sourceCounts = new HashMap<>();
        Map<String, java.util.Set<Coordinate>> imageCoordinates = new HashMap<>();
        for (List<SourceMapping> values : mappings.values()) {
            for (SourceMapping mapping : values) {
                sourceCounts.merge(mapping.precomputeProfile(), 1, Math::addExact);
                imageCoordinates.computeIfAbsent(mapping.precomputeProfile(), ignored -> new java.util.HashSet<>())
                        .add(mapping.coordinate());
            }
        }
        List<PrecomputeProfile> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        String previous = null;
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String[] fields = splitSidecar(lines.get(lineNumber), PROFILE_HEADER.length, lineNumber);
            requireFields(fields, lineNumber);
            String profile = fields[0];
            if (!seen.add(profile) || (previous != null && previous.compareTo(profile) >= 0))
                throw malformed(lineNumber, "precompute profiles are not strictly sorted or are duplicated");
            if (!sourceCounts.containsKey(profile)) throw malformed(lineNumber, "unknown precompute profile");
            long sourceCount = parseNonNegativeLong(fields[1], lineNumber, "source_records");
            long imageCount = parseNonNegativeLong(fields[2], lineNumber, "image_records");
            if (sourceCount != sourceCounts.get(profile)
                    || imageCount != imageCoordinates.get(profile).size())
                throw malformed(lineNumber, "precompute profile cardinality mismatch");
            byte[] fingerprint = parseDigest(fields[3], lineNumber);
            String expected = HexFormat.of().formatHex(profileFingerprint(profile, sourceCount, imageCount));
            if (!HexFormat.of().formatHex(fingerprint).equals(expected))
                throw malformed(lineNumber, "precompute profile fingerprint mismatch");
            result.add(new PrecomputeProfile(profile, sourceCount, imageCount, expected));
            previous = profile;
        }
        if (result.isEmpty() || !seen.equals(sourceCounts.keySet()))
            throw new IOException("precompute profile coverage does not match mappings");
        return List.copyOf(result);
    }

    private static byte[] profileFingerprint(String profile, long sourceCount, long imageCount) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((profile + "\t" + sourceCount + "\t" + imageCount + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static List<String> readSidecar(Path path, String[] header) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(String.join("\t", header)))
            throw new IOException("invalid sidecar header: " + path.getFileName());
        return lines;
    }

    private static String[] splitSidecar(String line, int expectedFields, int lineNumber) throws IOException {
        if (line.isEmpty()) throw malformed(lineNumber, "empty sidecar row");
        ArrayList<String> fields = new ArrayList<>(expectedFields);
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int at = 0; at < line.length(); at++) {
            char current = line.charAt(at);
            if (quoted) {
                if (current == '"') {
                    if (at + 1 < line.length() && line.charAt(at + 1) == '"') {
                        field.append('"');
                        at++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else field.append(current);
            } else if (current == '\t') {
                fields.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (current == '"' && field.length() == 0 && !closedQuote) {
                quoted = true;
            } else if (closedQuote) {
                throw malformed(lineNumber, "characters after quoted sidecar field");
            } else field.append(current);
        }
        if (quoted) throw malformed(lineNumber, "unterminated quoted sidecar field");
        fields.add(field.toString());
        if (fields.size() != expectedFields) throw malformed(lineNumber, "wrong sidecar field count");
        return fields.toArray(String[]::new);
    }

    private static void requireFields(String[] fields, int lineNumber) throws IOException {
        for (String field : fields) if (field.isEmpty()) throw malformed(lineNumber, "empty sidecar field");
    }

    private static Coordinate coordinate(String shard, String row, List<SharedLexiconImage> images,
                                         int lineNumber) throws IOException {
        int shardId = parseNonNegativeInt(shard, lineNumber, "shard_id");
        int imageRow = parseNonNegativeInt(row, lineNumber, "image_row");
        if (shardId >= images.size() || imageRow >= images.get(shardId).size())
            throw malformed(lineNumber, "sidecar coordinate outside image");
        return new Coordinate(shardId, imageRow);
    }

    private static String imageText(List<SharedLexiconImage> images, Coordinate coordinate) {
        return images.get(coordinate.shardId()).recordText(coordinate.imageRow());
    }

    private static int javaHash(String value) {
        int hash = 0;
        for (int at = 0; at < value.length(); at++) hash = 31 * hash + value.charAt(at);
        return hash;
    }

    private static long parseUnsignedHash(String value, int lineNumber) throws IOException {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0 || parsed > 0xffff_ffffL) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException failure) {
            throw malformed(lineNumber, "invalid java_hash");
        }
    }

    private static boolean parseBoolean(String value, int lineNumber) throws IOException {
        if (value.equals("True")) return true;
        if (value.equals("False")) return false;
        throw malformed(lineNumber, "invalid boolean precompute fact");
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
