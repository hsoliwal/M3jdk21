// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Charset-aware editor over the original M3FileSource and M3FileEditBatch.
 * Offsets are decoded UTF-16 code units, while the existing batch owns all
 * physical byte mutations and immutable generations. No second source store.
 *
 * <p>The codec is selected explicitly; no platform default or Unicode
 * replacement path is allowed. The source BOM is immutable metadata: edits
 * cannot touch its bytes, and replacements are encoded without a BOM. All
 * source boundaries must represent complete Unicode scalars. Every completed
 * image is validated with the same source codec before publication.
 */
public final class M3FileEncodedEdits {
    private final M3FileSource original;
    private final M3FileTextCoordinates coordinates;
    private final M3FileEditBatch delegate;
    private boolean published;

    private M3FileEncodedEdits(M3FileSource source, M3FileTextCoordinates.Encoding encoding) {
        original = Objects.requireNonNull(source, "source");
        coordinates = new M3FileTextCoordinates(source, Objects.requireNonNull(encoding, "encoding"));
        delegate = M3FileEditBatch.on(source);
    }
    public static M3FileEncodedEdits on(M3FileSource source,
                                       M3FileTextCoordinates.Encoding encoding) {
        return new M3FileEncodedEdits(source, encoding);
    }
    public String originalSha256() { return original.sha256(); }
    public int edits() { return delegate.edits(); }
    public M3FileTextCoordinates coordinates() { return coordinates; }

    /** All ranges are from the original decoded generation, not intermediate edits. */
    public M3FileEncodedEdits replaceUtf16(int from, int to, String replacement) {
        if (published) throw new IllegalStateException("source edits already published");
        Objects.requireNonNull(replacement, "replacement");
        if (from < 0 || to < from || to > coordinates.utf16Length())
            throw new IndexOutOfBoundsException("decoded edit boundaries outside source");
        int startByte = coordinates.byteAtUtf16(from);
        int endByte = coordinates.byteAtUtf16(to);
        byte[] raw = strictEncode(replacement, coordinates.encoding());
        delegate.replaceBytes(startByte, endByte, raw);
        return this;
    }
    /** Original immutable batch map retains byte-coordinate boundary affinities. */
    public Applied apply() {
        if (published) throw new IllegalStateException("source edits already published");
        published = true;
        M3FileEditBatch.Applied result = delegate.apply();
        M3FileTextCoordinates image = new M3FileTextCoordinates(result.source(),
                coordinates.encoding());
        return new Applied(result.source(), result.map(), image);
    }
    public record Applied(M3FileSource source, M3FileEditBatch.ChangeMap byteMap,
                          M3FileTextCoordinates coordinates) {
        public Applied {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(byteMap, "byteMap");
            Objects.requireNonNull(coordinates, "coordinates");
            byteMap.requireNew(source);
            coordinates.requireSource(source);
        }
    }

    private static byte[] strictEncode(String text, M3FileTextCoordinates.Encoding encoding) {
        Charset charset = switch (encoding) {
            case UTF8, UTF8_BOM -> StandardCharsets.UTF_8;
            case UTF16_LE -> StandardCharsets.UTF_16LE;
            case UTF16_BE -> StandardCharsets.UTF_16BE;
            case LATIN1 -> StandardCharsets.ISO_8859_1;
        };
        var encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            ByteBuffer out = encoder.encode(CharBuffer.wrap(text));
            byte[] copy = new byte[out.remaining()];
            out.get(copy);
            return copy;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("replacement cannot be represented by declared codec",
                    invalid);
        }
    }
}
