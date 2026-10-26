// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Strict external parser coordinate crosswalk through one retained source owner. */
public final class M3FileTextCoordinatesContract {
    private static int checks;
    private static void ok(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> cls, Runnable f) {
        try { f.run(); }
        catch (Throwable ex) {
            if (!cls.isInstance(ex)) throw new AssertionError("wrong refusal", ex);
            checks++; return;
        }
        throw new AssertionError("accepted malformed source / ambiguous position");
    }
    private static byte[] prefix(byte[] prefix, byte[] payload) {
        byte[] result = Arrays.copyOf(prefix, prefix.length + payload.length);
        System.arraycopy(payload, 0, result, prefix.length, payload.length);
        return result;
    }
    private static void mapping(String value, M3FileTextCoordinates.Encoding encoding,
                                byte[] encoded, int bom) {
        var source = M3FileSource.fromBytes(encoded);
        var map = new M3FileTextCoordinates(source, encoding);
        ok(map.sourceSha256().equals(source.sha256()), "original source generation sealed");
        if (encoded.length > 0) {
            ok(source.byteAt(0) == (encoded[0] & 0xff), "source byte accessor is not lossy");
            refuse(IndexOutOfBoundsException.class, () -> source.byteAt(encoded.length));
        }
        ok(map.decodedText().equals(value), "exact declared codec decoding");
        ok(map.utf16Length() == value.length() && map.contentStartByte() == bom,
                "UTF-16 units and BOM");
        ok(map.byteAtUtf16(0) == bom && map.utf16AtByte(bom) == 0, "initial BOM boundary");
        int valid = 0;
        for (int u = 0; u <= value.length(); u++) {
            if (u > 0 && u < value.length() && Character.isHighSurrogate(value.charAt(u - 1))
                    && Character.isLowSurrogate(value.charAt(u))) {
                int at = u;
                refuse(IllegalArgumentException.class, () -> map.byteAtUtf16(at));
                continue;
            }
            int atByte = map.byteAtUtf16(u);
            ok(map.utf16AtByte(atByte) == u, "codec round-trip");
            ok(map.byteAtPoint(map.pointAtByte(atByte)) == atByte, "Tree-sitter point inverse");
            valid++;
        }
        ok(map.utf16AtByte(source.length()) == value.length(), "EOF decoded boundary");
        ok(map.unicodeScalars() == value.codePointCount(0, value.length()), "Unicode scalar count");
        ok(valid > 0, "nonempty coordinate domain");
        for (int i = 0; i < map.lines(); i++) {
            ok(map.lineAtByte(map.lineStartByte(i)) == i,
                    "line start is monotonic and exact");
            ok(map.utf16ColumnAtByte(map.lineStartByte(i)) == 0, "line start UTF16 column zero");
            ok(map.byteAtUtf16Point(i, 0) == map.lineStartByte(i), "line origin maps both systems");
        }
        var different = M3FileSource.fromBytes(prefix(new byte[] {1}, encoded));
        refuse(IllegalStateException.class, () -> map.requireSource(different));
        map.requireSource(source);
        refuse(IllegalArgumentException.class, () -> map.byteAtPoint(new M3FileTextCoordinates.Point(0, -1)));
        refuse(IndexOutOfBoundsException.class, () -> map.byteAtUtf16(-1));
        refuse(IndexOutOfBoundsException.class, () -> map.utf16AtByte(source.length() + 1));
        for (int byteAt = bom + 1; byteAt < source.length(); byteAt++) {
            // Coordinate parser may never pick a byte *inside* one scalar.
            int at = byteAt;
            try {
                int actual = map.utf16AtByte(at);
                ok(map.byteAtUtf16(actual) == at, "nonmid scalar accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
    }
    public static void main(String[] args) {
        String mixed = "Aé😀\r\nx\rZ\ny";
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF8,
                mixed.getBytes(StandardCharsets.UTF_8), 0);
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF8_BOM,
                prefix(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf},
                        mixed.getBytes(StandardCharsets.UTF_8)), 3);
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF16_LE,
                prefix(new byte[] {(byte) 0xff, (byte) 0xfe},
                        mixed.getBytes(StandardCharsets.UTF_16LE)), 2);
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF16_BE,
                prefix(new byte[] {(byte) 0xfe, (byte) 0xff},
                        mixed.getBytes(StandardCharsets.UTF_16BE)), 2);
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF16_LE,
                mixed.getBytes(StandardCharsets.UTF_16LE), 0);
        mapping(mixed, M3FileTextCoordinates.Encoding.UTF16_BE,
                mixed.getBytes(StandardCharsets.UTF_16BE), 0);
        String latin1 = "À éÿ\r\nX";
        mapping(latin1, M3FileTextCoordinates.Encoding.LATIN1,
                latin1.getBytes(StandardCharsets.ISO_8859_1), 0);
        mapping("", M3FileTextCoordinates.Encoding.UTF8, new byte[0], 0);
        var two = new M3FileTextCoordinates(M3FileSource.fromBytes("q\r\ns".getBytes(StandardCharsets.UTF_8)),
                M3FileTextCoordinates.Encoding.UTF8);
        ok(two.lines() == 2 && two.lineStartByte(1) == 3, "CRLF is one logical newline");
        refuse(IndexOutOfBoundsException.class,
                () -> two.byteAtPoint(new M3FileTextCoordinates.Point(0, 3)));
        refuse(IndexOutOfBoundsException.class,
                () -> two.byteAtUtf16Point(0, 3));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xc0,(byte) 0xaf}), M3FileTextCoordinates.Encoding.UTF8));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xed,(byte) 0xa0,(byte) 0x80}), M3FileTextCoordinates.Encoding.UTF8));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xff,(byte) 0xfe, (byte) 0, (byte) 0xdc}),
                M3FileTextCoordinates.Encoding.UTF16_LE));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xff,(byte) 0xfe, (byte) 0}),
                M3FileTextCoordinates.Encoding.UTF16_LE));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xfe,(byte) 0xff, 0, 65}),
                M3FileTextCoordinates.Encoding.UTF16_LE));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes("a".getBytes(StandardCharsets.UTF_8)),
                M3FileTextCoordinates.Encoding.UTF8_BOM));
        refuse(IllegalArgumentException.class, () -> new M3FileTextCoordinates(
                M3FileSource.fromBytes(new byte[] {(byte) 0xef,(byte) 0xbb,(byte) 0xbf, 65}),
                M3FileTextCoordinates.Encoding.UTF8));
        // Stable 4K checkpoints and binary-search line points across large multibyte input.
        Random seed = new Random(0x4d33464c);
        StringBuilder large = new StringBuilder();
        int[] cps = {'a', 'é', 0x1f600, '\n', '\r', 'z', 0x10437};
        for (int i = 0; i < 9000; i++) large.appendCodePoint(cps[seed.nextInt(cps.length)]);
        var crosswalk = new M3FileTextCoordinates(M3FileSource.fromBytes(
                large.toString().getBytes(StandardCharsets.UTF_8)), M3FileTextCoordinates.Encoding.UTF8);
        for (int u = 0; u <= large.length(); u += 59) {
            if (u > 0 && u < large.length() && Character.isHighSurrogate(large.charAt(u - 1))
                    && Character.isLowSurrogate(large.charAt(u))) continue;
            ok(crosswalk.utf16AtByte(crosswalk.byteAtUtf16(u)) == u,
                    "multi-page sparse exact UTF16 mapping");
        }
        System.out.println("M3_FILE_TEXT_COORDINATES_PASS checks=" + checks);
    }
}
