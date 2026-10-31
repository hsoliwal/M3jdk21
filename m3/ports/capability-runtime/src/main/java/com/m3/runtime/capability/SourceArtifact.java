// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * One immutable, source-addressable byte payload. A source owns exactly one defensive
 * copy, while arbitrarily many byte-range coordinates share it without retaining
 * per-range arrays. This is an opt-in JMOD substrate, not M3 String's payload owner.
 */
public final class SourceArtifact {
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    private final String id;
    private final String root;
    private final byte[] bytes;

    private SourceArtifact(String id, byte[] owned) {
        this.id = id;
        this.bytes = owned;
        this.root = digest(owned, 0, owned.length);
    }

    /** Snapshots the caller's buffer; subsequent caller mutations do not affect identity. */
    public static SourceArtifact copyOf(String id, byte[] source) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        if (id.isBlank() || id.length() > 256 || id.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("source identifier");
        }
        if (source.length > MAX_BYTES) throw new IllegalArgumentException("source byte budget");
        return new SourceArtifact(id, source.clone());
    }

    public String id() { return id; }
    public String root() { return root; }
    public int length() { return bytes.length; }
    public Span whole() { return span(0, bytes.length); }
    public Span span(int start, int length) { return new Span(this, start, length); }

    /** Coordinates retain the owner and cannot escape to a different payload. */
    public record Span(SourceArtifact source, int start, int length) {
        public Span {
            Objects.requireNonNull(source, "source");
            Objects.checkFromIndexSize(start, length, source.bytes.length);
        }
        public byte byteAt(int index) {
            Objects.checkIndex(index, length);
            return source.bytes[start + index];
        }
        public Span slice(int relativeStart, int count) {
            Objects.checkFromIndexSize(relativeStart, count, length);
            return new Span(source, start + relativeStart, count);
        }
        /** Identity covers the exact selected byte range, never guessed UTF-16 offsets. */
        public String sha256() { return digest(source.bytes, start, length); }
        /** Explicit compatibility materialization; mutations cannot change the source. */
        public byte[] copy() {
            return java.util.Arrays.copyOfRange(source.bytes, start, start + length);
        }
    }

    static String digest(byte[] data, int from, int count) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data, from, count);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 not available", unavailable);
        }
    }
}
