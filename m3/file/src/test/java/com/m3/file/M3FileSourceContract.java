// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/** Standalone source-coordinate and admitted M3File observation contract. */
public final class M3FileSourceContract {
    public M3FileSourceContract() { }
    private static int checks;
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
        checks++;
    }
    private static void rejects(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("expected rejection");
    }
    public static void main(String[] args) throws Exception {
        byte[] source = "α\r\n😀x\ny".getBytes(StandardCharsets.UTF_8);
        var image = M3FileSource.fromBytes(source);
        check(Arrays.equals(image.bytes(), source) && image.length() == 11, "lossless bytes");
        check(image.lines() == 3 && image.lineStartByte(1) == 4
                && image.lineStartByte(2) == 10, "mixed newline index");
        check(image.lineAtByte(9) == 1 && image.lineAtByte(10) == 2, "line search");
        check(image.utf16AtByte(4) == 3 && image.utf16AtByte(8) == 5, "byte to UTF16");
        check(image.byteAtUtf16(3) == 4 && image.byteAtUtf16(5) == 8, "UTF16 to byte");
        rejects(() -> image.utf16AtByte(6));
        rejects(() -> image.byteAtUtf16(4));
        rejects(() -> M3FileSource.fromBytes(new byte[] {(byte) 0xc0, (byte) 0xaf}).utf16Length());
        var changed = image.replace(4, 8, new byte[] {'Q'});
        check(Arrays.equals(changed.source().bytes(), "α\r\nQx\ny".getBytes(StandardCharsets.UTF_8)),
                "source delta");
        check(changed.edit().oldToNew(5) == -1 && changed.edit().oldToNew(9) == 6,
                "old-coordinate delta map");
        check(!changed.source().sha256().equals(image.sha256()), "changed hash");
        byte[] escape = image.bytes(); escape[0] = 0;
        check(Arrays.equals(image.bytes(), source), "defensive ownership");
        check(M3FileSource.fromBytes(new byte[0]).lines() == 1, "empty line");
        var file = Files.createTempFile("m3-source-", ".java");
        try {
            Files.write(file, source);
            var capture = M3FileSource.capture(M3File.of(file), source.length);
            check(capture.source().sha256().equals(image.sha256())
                    && capture.observation().fingerprint().size() == source.length,
                    "single M3File observation");
            try {
                M3FileSource.capture(M3File.of(file), source.length - 1);
                throw new AssertionError("accepted oversized source");
            } catch (java.io.IOException expected) { checks++; }
        } finally { Files.deleteIfExists(file); }
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 6000; i++) value.append(i % 3 == 0 ? 'α' : 'x');
        var longImage = M3FileSource.fromBytes(value.toString().getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i <= value.length(); i += 31) {
            int atByte = longImage.byteAtUtf16(i);
            check(longImage.utf16AtByte(atByte) == i, "sparse checkpoint");
        }
        System.out.println("M3_FILE_SOURCE_PASS checks=" + checks);
    }
}
