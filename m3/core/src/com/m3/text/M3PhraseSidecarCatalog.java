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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reloadable receiver for the independent {@code m3phrase-v1} sidecar.
 *
 * <p>The existing {@code m3lex-family-v2} bundle is intentionally unchanged.
 * Phrase rows are grouped by their complete vocabulary scope and admitted into
 * the immutable {@link M3PhrasePrecompute} trie only after canonical ordering
 * and scope validation.</p>
 */
public final class M3PhraseSidecarCatalog {
    public static final String SCHEMA_VERSION = M3PhrasePrecompute.SCHEMA_VERSION;
    public static final String FILE_NAME = "synexia.phrases.tsv";
    private static final String[] HEADER = {
            "source_id", "record_id", "source_revision", "vocabulary_fingerprint",
            "source_token_ids", "target_token_ids"
    };

    private final Map<M3PhrasePrecompute.Scope, M3PhrasePrecompute.Catalog> catalogs;
    private final int rowCount;

    private M3PhraseSidecarCatalog(
            Map<M3PhrasePrecompute.Scope, M3PhrasePrecompute.Catalog> catalogs,
            int rowCount) {
        this.catalogs = Map.copyOf(catalogs);
        this.rowCount = rowCount;
    }

    public static M3PhraseSidecarCatalog open(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IOException("not a sidecar directory");
        Path file = root.resolve(FILE_NAME).normalize();
        if (!file.getParent().equals(root)) throw new IOException("unsafe phrase sidecar path");
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf)
            throw new IOException("phrase sidecar must not have a BOM");
        if (containsByte(bytes, (byte) '\r'))
            throw new IOException("phrase sidecar must use LF");
        final String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException failure) {
            throw new IOException("phrase sidecar is not UTF-8", failure);
        }
        String[] lines = text.split("\\n", -1);
        int last = lines.length;
        if (last > 0 && lines[last - 1].isEmpty()) last--;
        if (last == 0 || !String.join("\t", HEADER).equals(lines[0]))
            throw new IOException("phrase sidecar header mismatch");

        Map<M3PhrasePrecompute.Scope, M3PhrasePrecompute.Builder> builders = new HashMap<>();
        List<String> previous = null;
        int rows = 0;
        for (int line = 1; line < last; line++) {
            String[] fields = lines[line].split("\\t", -1);
            if (fields.length != HEADER.length) throw new IOException("phrase field count mismatch");
            String sourceId = required(fields[0], "source_id");
            String recordId = required(fields[1], "record_id");
            String revision = required(fields[2], "source_revision");
            String vocabulary = required(fields[3], "vocabulary_fingerprint");
            int[] source = intArray(fields[4], "source_token_ids");
            if (source.length == 0) throw new IOException("source phrase must not be empty");
            int[] target = intArray(fields[5], "target_token_ids");
            List<String> key = List.of(sourceId, recordId, revision, vocabulary, fields[4]);
            if (previous != null && compare(previous, key) >= 0)
                throw new IOException("phrase rows are not sorted and unique");
            previous = key;
            M3PhrasePrecompute.Scope scope =
                    new M3PhrasePrecompute.Scope(sourceId, recordId, revision, vocabulary);
            builders.computeIfAbsent(scope, M3PhrasePrecompute::builder)
                    .put(new M3PhrasePrecompute.Phrase(scope, source, target));
            rows++;
        }
        Map<M3PhrasePrecompute.Scope, M3PhrasePrecompute.Catalog> catalogs = new HashMap<>();
        for (Map.Entry<M3PhrasePrecompute.Scope, M3PhrasePrecompute.Builder> entry
                : builders.entrySet()) {
            catalogs.put(entry.getKey(), entry.getValue().build());
        }
        return new M3PhraseSidecarCatalog(catalogs, rows);
    }

    public Optional<M3PhrasePrecompute.Catalog> phraseAt(M3PhrasePrecompute.Scope scope) {
        return Optional.ofNullable(catalogs.get(Objects.requireNonNull(scope, "scope")));
    }

    public int rowCount() {
        return rowCount;
    }

    private static String required(String value, String name) throws IOException {
        if (value.isEmpty() || value.indexOf('\t') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new IOException(name + " is invalid");
        return value;
    }

    private static int[] intArray(String value, String name) throws IOException {
        if (value.equals("-")) return new int[0];
        if (value.isEmpty()) throw new IOException(name + " is empty");
        String[] values = value.split(",", -1);
        int[] result = new int[values.length];
        for (int at = 0; at < values.length; at++) {
            try {
                result[at] = Integer.parseInt(values[at]);
            } catch (NumberFormatException failure) {
                throw new IOException(name + " is invalid", failure);
            }
            if (result[at] < 0) throw new IOException(name + " contains a negative ID");
        }
        return result;
    }

    private static int compare(List<String> left, List<String> right) {
        for (int at = 0; at < left.size(); at++) {
            int result = left.get(at).compareTo(right.get(at));
            if (result != 0) return result;
        }
        return 0;
    }

    private static boolean containsByte(byte[] bytes, byte value) {
        for (byte candidate : bytes) if (candidate == value) return true;
        return false;
    }
}
