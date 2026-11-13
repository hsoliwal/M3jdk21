// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from Synexia compiler M3File; independent JSON source receiver, no javac dependency.
package jdk.m3.jackson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Immutable JSON input snapshot with exact String ownership and separate byte/UTF-16 identities. */
public final class M3File implements CharSequence {
    private final URI sourceUri;
    private final String source;
    private final String sourceSha256;
    private final String byteSha256;

    private M3File(URI sourceUri, String source, String byteSha256) {
        this.sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
        this.source = Objects.requireNonNull(source, "source");
        this.sourceSha256 = M3SourceHash.utf16(source);
        this.byteSha256 = byteSha256;
    }

    /** Snapshot mutable characters once; a String already supplies its immutable M3 owner. */
    public static M3File of(URI sourceUri, CharSequence source) {
        return new M3File(sourceUri, Objects.requireNonNull(source, "source").toString(), null);
    }

    /** Read at most maxBytes and reject malformed/unmappable input using the supplied Charset. */
    public static M3File read(Path path, Charset charset, int maxBytes) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        if (maxBytes < 0) throw new IllegalArgumentException("negative source byte limit");
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(maxBytes);
            if (input.read() != -1) throw new IOException("source exceeds byte budget");
        }
        String source = charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        return new M3File(path.toAbsolutePath().normalize().toUri(), source, M3SourceHash.bytes(bytes));
    }

    public URI sourceUri() { return sourceUri; }
    public String source() { return source; }
    public String sourceSha256() { return sourceSha256; }
    /** Null for character-only inputs, which do not assert an original encoding. */
    public String byteSha256() { return byteSha256; }
    /** Independent reader over the same immutable source; the caller owns this reader. */
    public Reader openReader() { return new StringReader(source); }
    @Override public int length() { return source.length(); }
    @Override public char charAt(int index) { return source.charAt(index); }
    @Override public String subSequence(int start, int end) { return source.substring(start, end); }
    @Override public String toString() { return source; }
}
