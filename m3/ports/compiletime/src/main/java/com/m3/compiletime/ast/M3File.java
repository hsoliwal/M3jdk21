// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.synexia.mindex.compiler.ast.MIndexJavaAST;
import com.synexia.mindex.compiler.ast.MIndexASTDocument;
import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import javax.tools.SimpleJavaFileObject;

/** Immutable compiler input; byte identity and javac's UTF-16 coordinates remain distinct. */
public final class M3File extends SimpleJavaFileObject {
    private final String source;
    private final String byteSha256;

    private M3File(URI uri, String source, String byteSha256) {
        super(Objects.requireNonNull(uri, "uri"), Kind.SOURCE);
        this.source = Objects.requireNonNull(source, "source");
        this.byteSha256 = byteSha256;
    }

    /** The supplied characters are copied once before the compiler observes them. */
    public static M3File of(URI uri, CharSequence source) {
        return new M3File(uri, Objects.requireNonNull(source, "source").toString(), null);
    }

    /** Read and decode one bounded, immutable file snapshot before creating the compiler task. */
    public static M3File read(Path path, Charset charset, int maxBytes) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        if (maxBytes < 0) throw new IllegalArgumentException("negative source limit");
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(maxBytes);
            if (input.read() != -1) throw new IOException("source exceeds byte budget");
        }
        String source = charset.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        return new M3File(path.toAbsolutePath().normalize().toUri(), source, sha256(bytes));
    }

    /**
     * Admit bytes already captured by the original M3File provider into this javac source.
     *
     * <p>No filesystem access occurs here. The remaining bytes of {@code captured} are
     * defensively copied once, so a later change to the caller's buffer cannot change the
     * decoded compiler input. The claimed provider digest is checked against those same
     * bytes before decoding, while sourceSha256() continues to identify the exact UTF-16
     * characters. This verifies the supplied capture, not an atomic filesystem snapshot.
     * Only this one content generation may be supplied to the corresponding compiler task.</p>
     *
     * @throws IOException if the byte limit, declared digest or strict decoding fails
     */
    public static M3File fromCaptured(URI uri, ByteBuffer captured, String expectedByteSha256,
            Charset charset, int maxBytes) throws IOException {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(captured, "captured");
        Objects.requireNonNull(charset, "charset");
        M3FileSourceFacts.requireSha256(expectedByteSha256, "expectedByteSha256");
        if (maxBytes < 0) throw new IllegalArgumentException("negative source limit");
        int length = captured.remaining();
        if (length > maxBytes) throw new IOException("source exceeds byte budget");
        byte[] bytes = new byte[length];
        captured.duplicate().get(bytes);
        String actual = sha256(bytes);
        if (!actual.equals(expectedByteSha256)) {
            throw new IOException("captured source byte digest mismatch");
        }
        String text = charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        return new M3File(uri, text, actual);
    }

    @Override public String getCharContent(boolean ignoreEncodingErrors) { return source; }
    @Override public Reader openReader(boolean ignoreEncodingErrors) { return new StringReader(source); }
    public String source() { return source; }
    public String sourceSha256() { return M3FileSourceFacts.sourceSha256(source); }
    /** Null for character-only inputs, which have no asserted original byte encoding. */
    public String byteSha256() { return byteSha256; }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
