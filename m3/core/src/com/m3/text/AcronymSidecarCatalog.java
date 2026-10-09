/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * File-backed receiver for the dedicated m3lex-acronym-v1 sidecar.
 *
 * <p>This reader is deliberately separate from the five-family m3lex-family-v2
 * bundle. It accepts source-authored acronym text only when source identity,
 * owner scope, row order, row count and bytes are all proven.</p>
 */
public final class AcronymSidecarCatalog {
    public static final String SCHEMA_VERSION = "m3lex-acronym-v1";
    public static final String SOURCE_ID = "dictlang.acronyms";
    public static final String DATA_FILE = "synexia.acronyms.tsv";
    public static final String INDEX_FILE = "synexia.acronyms.index.tsv";
    private static final String DATA_HEADER =
            "source_id\trecord_id\tsource_manifest_revision\towner_fingerprint"
                    + "\tacronym\texpansion\tdomain";
    private static final String INDEX_HEADER = "schema_version\tfile\trows\tsha256";
    private static final Pattern ACRONYM =
            Pattern.compile("[A-Za-z][A-Za-z0-9+.-]{0,31}");
    private static final Pattern DOMAIN = Pattern.compile("[a-z][a-z0-9._-]*");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public record Scope(String sourceManifestRevision, String ownerFingerprint) {
        public Scope {
            text(sourceManifestRevision, "sourceManifestRevision");
            digest(ownerFingerprint, "ownerFingerprint");
        }
    }

    public record Entry(Scope scope, M3LexiconPrecompute.AcronymPrecompute value) {
        public Entry {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(value, "value");
        }

        public String acronym() {
            return value.acronym();
        }

        public String expansion() {
            return value.expansion();
        }

        public String domain() {
            return value.domain();
        }
    }

    private final Scope scope;
    private final Map<String, Entry> entries;

    private AcronymSidecarCatalog(Scope scope, Map<String, Entry> entries) {
        this.scope = scope;
        this.entries = Map.copyOf(entries);
    }

    public static AcronymSidecarCatalog open(Path directory) throws IOException {
        Path root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException("sidecar directory");
        }
        Path data = regular(root.resolve(DATA_FILE), DATA_FILE);
        Path index = regular(root.resolve(INDEX_FILE), INDEX_FILE);
        String indexText = utf8(Files.readAllBytes(index), INDEX_FILE);
        String[] indexLines = lines(indexText, INDEX_HEADER, INDEX_FILE);
        if (indexLines.length != 2) {
            throw new IllegalArgumentException("acronym sidecar index row count");
        }
        String[] indexRow = fields(indexLines[1], 4, INDEX_FILE);
        if (!SCHEMA_VERSION.equals(indexRow[0]) || !DATA_FILE.equals(indexRow[1])) {
            throw new IllegalArgumentException("acronym sidecar index schema");
        }
        int expectedRows = nonNegative(indexRow[2], "rows");
        digest(indexRow[3], "sha256");
        byte[] dataBytes = Files.readAllBytes(data);
        if (!indexRow[3].equals(sha256(dataBytes))) {
            throw new IllegalArgumentException("acronym sidecar checksum");
        }

        String dataText = utf8(dataBytes, DATA_FILE);
        String[] dataLines = lines(dataText, DATA_HEADER, DATA_FILE);
        if (dataLines.length - 1 != expectedRows) {
            throw new IllegalArgumentException("acronym sidecar row count");
        }
        LinkedHashMap<String, Entry> values = new LinkedHashMap<>();
        Scope scope = null;
        String previous = null;
        for (int line = 1; line < dataLines.length; line++) {
            String[] row = fields(dataLines[line], 7, DATA_FILE);
            if (!SOURCE_ID.equals(row[0]) || !row[1].equals(row[4])) {
                throw new IllegalArgumentException("acronym sidecar identity");
            }
            Scope rowScope = new Scope(row[2], row[3]);
            if (scope == null) {
                scope = rowScope;
            } else if (!scope.equals(rowScope)) {
                throw new IllegalArgumentException("acronym sidecar scope drift");
            }
            String acronym = text(row[4], "acronym");
            if (!ACRONYM.matcher(acronym).matches()) {
                throw new IllegalArgumentException("invalid acronym");
            }
            String expansion = text(row[5], "expansion");
            if (expansion.isBlank()) {
                throw new IllegalArgumentException("blank expansion");
            }
            String domain = text(row[6], "domain");
            if (!DOMAIN.matcher(domain).matches()) {
                throw new IllegalArgumentException("invalid domain");
            }
            if (previous != null && previous.compareTo(acronym) >= 0) {
                throw new IllegalArgumentException("acronym rows not sorted and unique");
            }
            previous = acronym;
            Entry entry = new Entry(rowScope,
                    new M3LexiconPrecompute.AcronymPrecompute(acronym, expansion, domain));
            if (values.putIfAbsent(acronym, entry) != null) {
                throw new IllegalArgumentException("duplicate acronym");
            }
        }
        if (scope == null) {
            throw new IllegalArgumentException("empty acronym sidecar");
        }
        return new AcronymSidecarCatalog(scope, values);
    }

    public Scope scope() {
        return scope;
    }

    public int size() {
        return entries.size();
    }

    public Optional<Entry> find(String acronym) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(acronym, "acronym")));
    }

    private static Path regular(Path path, String name) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("missing or non-regular " + name);
        }
        return path;
    }

    private static String utf8(byte[] bytes, String name) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException(name + " UTF-8", failure);
        }
    }

    private static String[] lines(String text, String header, String name) {
        if (text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(name + " must use LF");
        }
        String[] lines = text.split("\\n", -1);
        if (lines.length < 3 || !header.equals(lines[0]) || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException(name + " header/termination");
        }
        return java.util.Arrays.copyOf(lines, lines.length - 1);
    }

    private static String[] fields(String line, int count, String name) {
        String[] fields = line.split("\\t", -1);
        if (fields.length != count) {
            throw new IllegalArgumentException(name + " field count");
        }
        return fields;
    }

    private static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty() || value.indexOf('\t') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(name);
        }
        return value;
    }

    private static String digest(String value, String name) {
        text(value, name);
        if (!SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(name);
        }
        return value;
    }

    private static int nonNegative(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new IllegalArgumentException(name);
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(name, failure);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }
}
