/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/model/KnowledgeDocument.java (blob 4a2994a12d3b) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/** Canonical text-bearing unit used by every live, dump, WARC, file and media source. */
public record M3KnowledgeDocument(
        String documentId,
        String contentHash,
        URI sourceUri,
        String title,
        String mediaType,
        M3SourceKind sourceKind,
        String providerId,
        Instant retrievedAt,
        Instant publishedAt,
        String text,
        Map<String, String> metadata) {

    public M3KnowledgeDocument {
        documentId = requireText(documentId, "documentId");
        contentHash = requireText(contentHash, "contentHash");
        sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
        title = title == null ? "" : title;
        mediaType = mediaType == null ? "text/plain" : mediaType;
        sourceKind = Objects.requireNonNull(sourceKind, "sourceKind");
        providerId = providerId == null ? "" : providerId;
        retrievedAt = Objects.requireNonNull(retrievedAt, "retrievedAt");
        text = Objects.requireNonNull(text, "text");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static M3KnowledgeDocument create(
            URI sourceUri,
            String title,
            String mediaType,
            M3SourceKind sourceKind,
            String providerId,
            Instant retrievedAt,
            Instant publishedAt,
            String text,
            Map<String, String> metadata) {
        String hash = sha256(text.getBytes(StandardCharsets.UTF_8));
        String id = sha256((sourceUri.toASCIIString() + "\n" + hash).getBytes(StandardCharsets.UTF_8));
        return new M3KnowledgeDocument(
                id, hash, sourceUri, title, mediaType, sourceKind, providerId,
                retrievedAt, publishedAt, text, metadata);
    }

    public byte[] utf8() {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
