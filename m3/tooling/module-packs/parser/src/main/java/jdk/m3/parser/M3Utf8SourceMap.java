// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Verified UTF-8 byte <-> UTF-16 code-unit boundary coordinates for one M3File.
 * No retained raw byte[] and no second source String: two primitive checkpoint lanes
 * every 128 Unicode scalar values. Queries scan at most 128 scalar values.
 *
 * A UTF-16 offset inside a surrogate pair and a byte offset inside a multibyte
 * scalar have no exact representation in the other coordinate system and refuse.
 * This map is not a proof of filesystem atomicity or parser source association.
 */
public final class M3Utf8SourceMap {
    private static final int STRIDE = 128;
    private final M3File source;
    private final int byteLength;
    private final int[] utf16Anchors, byteAnchors;

    private M3Utf8SourceMap(M3File source, int bytes, int[] chars, int[] offsets) {
        this.source = source;
        this.byteLength = bytes;
        this.utf16Anchors = chars;
        this.byteAnchors = offsets;
    }

    /**
     * Captured bytes must be the original already-read UTF-8 input, not bytes
     * reconstructed from text. Require the source's admitted raw-byte SHA-256
     * and strict UTF-8 decoding to match its immutable UTF-16 payload.
     * The array is borrowed only during this call; mutation afterward is harmless.
     */
    public static M3Utf8SourceMap verified(M3File source, byte[] captured, int maxBytes)
            throws CharacterCodingException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(captured, "captured");
        if (maxBytes < 0 || captured.length > maxBytes) {
            throw new IllegalArgumentException("UTF-8 input exceeds byte budget");
        }
        String claimed = source.byteSha256().orElseThrow(() ->
                new IllegalArgumentException("M3File has no captured original byte identity"));
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        if (!HexFormat.of().formatHex(digest.digest(captured)).equals(claimed)) {
            throw new IllegalArgumentException("captured bytes differ from M3File generation");
        }
        String decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(captured)).toString();
        if (!decoded.equals(source.text())) {
            throw new IllegalArgumentException("M3File was not decoded as strict UTF-8");
        }
        int[] charStarts = new int[Math.max(2, decoded.length() / STRIDE + 3)];
        int[] byteStarts = new int[charStarts.length];
        int checkpoints = 1, chars = 0, bytes = 0, scalars = 0;
        while (chars < decoded.length()) {
            if (scalars != 0 && scalars % STRIDE == 0) {
                if (checkpoints == charStarts.length) {
                    charStarts = Arrays.copyOf(charStarts, charStarts.length + (charStarts.length >>> 1) + 1);
                    byteStarts = Arrays.copyOf(byteStarts, charStarts.length);
                }
                charStarts[checkpoints] = chars;
                byteStarts[checkpoints++] = bytes;
            }
            int scalar = decoded.codePointAt(chars);
            bytes += utf8Width(scalar);
            chars += Character.charCount(scalar);
            scalars++;
        }
        if (bytes != captured.length) {
            throw new IllegalArgumentException("UTF-8 coordinate accounting differs from captured bytes");
        }
        if (charStarts[checkpoints - 1] != chars || byteStarts[checkpoints - 1] != bytes) {
            if (checkpoints == charStarts.length) {
                charStarts = Arrays.copyOf(charStarts, charStarts.length + 1);
                byteStarts = Arrays.copyOf(byteStarts, byteStarts.length + 1);
            }
            charStarts[checkpoints] = chars;
            byteStarts[checkpoints++] = bytes;
        }
        return new M3Utf8SourceMap(source, bytes,
                Arrays.copyOf(charStarts, checkpoints), Arrays.copyOf(byteStarts, checkpoints));
    }

    public M3File source() { return source; }
    public int utf16Length() { return source.length(); }
    public int byteLength() { return byteLength; }
    public int checkpointCount() { return utf16Anchors.length; }

    /** Exact UTF-8 byte position at a Unicode scalar boundary. */
    public int byteOffset(int utf16Offset) {
        if (utf16Offset < 0 || utf16Offset > source.length()) {
            throw new IndexOutOfBoundsException("UTF-16 offset");
        }
        int anchor = atOrBefore(utf16Anchors, utf16Offset);
        int chars = utf16Anchors[anchor], bytes = byteAnchors[anchor];
        while (chars < utf16Offset) {
            int scalar = Character.codePointAt(source, chars);
            chars += Character.charCount(scalar);
            if (chars > utf16Offset) throw new IllegalArgumentException("UTF-16 surrogate interior");
            bytes += utf8Width(scalar);
        }
        return bytes;
    }

    /** Exact UTF-16 code-unit boundary at the start or end of a UTF-8 scalar. */
    public int utf16Offset(int byteOffset) {
        if (byteOffset < 0 || byteOffset > byteLength) {
            throw new IndexOutOfBoundsException("UTF-8 byte offset");
        }
        int anchor = atOrBefore(byteAnchors, byteOffset);
        int chars = utf16Anchors[anchor], bytes = byteAnchors[anchor];
        while (bytes < byteOffset) {
            int scalar = Character.codePointAt(source, chars);
            bytes += utf8Width(scalar);
            if (bytes > byteOffset) throw new IllegalArgumentException("UTF-8 scalar interior");
            chars += Character.charCount(scalar);
        }
        return chars;
    }

    private static int atOrBefore(int[] lane, int value) {
        int found = Arrays.binarySearch(lane, value);
        return found >= 0 ? found : -found - 2;
    }
    private static int utf8Width(int scalar) {
        return scalar < 0x80 ? 1 : scalar < 0x800 ? 2 : scalar < 0x10000 ? 3 : 4;
    }
}
