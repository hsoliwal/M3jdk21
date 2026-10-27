// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Standalone Java 21 byte-preserving edit contract across declared encodings. */
public final class M3FileEncodedEditsContract {
    private M3FileEncodedEditsContract() { }
    private static int checks;
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError("wrong refusal", failure);
            checks++; return;
        }
        throw new AssertionError("invalid edit accepted");
    }
    private static void caseOf(M3FileTextCoordinates.Encoding codec, String source,
                               String replacement, String expected, byte[] bom) {
        Charset cs = switch (codec) {
            case UTF8, UTF8_BOM -> StandardCharsets.UTF_8;
            case UTF16_LE -> StandardCharsets.UTF_16LE;
            case UTF16_BE -> StandardCharsets.UTF_16BE;
            case LATIN1 -> StandardCharsets.ISO_8859_1;
        };
        byte[] text = source.getBytes(cs), raw = new byte[bom.length + text.length];
        System.arraycopy(bom, 0, raw, 0, bom.length);
        System.arraycopy(text, 0, raw, bom.length, text.length);
        M3FileSource baseline = M3FileSource.fromBytes(raw);
        var builder = M3FileEncodedEdits.on(baseline, codec);
        // UTF-16 offsets are relative to the decoded source, even when the BOM
        // consumes physical bytes before the first text position.
        builder.replaceUtf16(1, source.length(), replacement);
        var finished = builder.apply();
        check(finished.coordinates().decodedText().equals(expected), "codec-specific roundtrip");
        check(finished.coordinates().encoding() == codec, "encoding preserved");
        check(finished.coordinates().contentStartByte() == bom.length, "BOM owned by source");
        check(Arrays.equals(Arrays.copyOf(finished.source().bytes(), bom.length), bom), "BOM kept");
        check(finished.byteMap().changes() == 1, "one original edit");
        check(!finished.source().sha256().equals(baseline.sha256()), "source digest changes");
        check(Arrays.equals(baseline.bytes(), raw), "original generation immutable");
        refuse(IllegalStateException.class, builder::apply);
    }
    public static void main(String[] args) {
        caseOf(M3FileTextCoordinates.Encoding.UTF8, "aΩ😀", "Z", "aZ", new byte[0]);
        caseOf(M3FileTextCoordinates.Encoding.UTF8_BOM, "aΩ😀", "é", "aé",
                new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf});
        caseOf(M3FileTextCoordinates.Encoding.UTF16_LE, "aΩ😀", "x", "ax",
                new byte[] {(byte) 0xff, (byte) 0xfe});
        caseOf(M3FileTextCoordinates.Encoding.UTF16_BE, "aΩ😀", "ζ", "aζ",
                new byte[] {(byte) 0xfe, (byte) 0xff});
        caseOf(M3FileTextCoordinates.Encoding.LATIN1, "aé", "c", "ac", new byte[0]);
        var latin = M3FileEncodedEdits.on(M3FileSource.fromBytes("ab".getBytes(
                StandardCharsets.ISO_8859_1)), M3FileTextCoordinates.Encoding.LATIN1);
        refuse(IllegalArgumentException.class, () -> latin.replaceUtf16(1, 2, "Ω"));
        refuse(IllegalArgumentException.class, () -> latin.replaceUtf16(1, 2, "\ud800"));
        var unicode = M3FileEncodedEdits.on(M3FileSource.fromBytes("a😀b".getBytes(
                StandardCharsets.UTF_8)), M3FileTextCoordinates.Encoding.UTF8);
        refuse(IllegalArgumentException.class, () -> unicode.replaceUtf16(2, 3, "x"));
        unicode.replaceUtf16(0, 1, "A").replaceUtf16(3, 4, "B");
        var result = unicode.apply();
        check(result.coordinates().decodedText().equals("A😀B"), "multi-edit with surrogate intact");
        check(result.byteMap().changes() == 2, "original offsets for both edits");
        refuse(IllegalStateException.class, () -> unicode.replaceUtf16(0, 0, "Q"));
        var empty = M3FileEncodedEdits.on(M3FileSource.fromBytes(new byte[0]),
                M3FileTextCoordinates.Encoding.UTF8).apply();
        check(empty.coordinates().utf16Length() == 0, "empty image supported");
        // Deterministic Unicode corpus including CRLF, supplementary characters,
        // zero-width insertions and all strict encoding paths.
        for (int n = 0; n < 250; n++) {
            String original = "A" + "x".repeat(n % 12) + "\r\n" + "😀" + "Z";
            var current = M3FileSource.fromBytes(original.getBytes(StandardCharsets.UTF_8));
            var edit = M3FileEncodedEdits.on(current, M3FileTextCoordinates.Encoding.UTF8);
            edit.replaceUtf16(1, 1, "Ω").replaceUtf16(original.length()-1,
                    original.length(), "Y");
            check(edit.apply().coordinates().decodedText().equals("AΩ" + original.substring(1,
                    original.length()-1) + "Y"), "deterministic multi-edit corpus");
        }
        System.out.println("M3_FILE_ENCODED_EDITS_PASS checks=" + checks);
    }
}
