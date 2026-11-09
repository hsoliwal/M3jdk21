// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jackson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class M3SourceFactsContract {
    private M3SourceFactsContract() { }

    public static void main(String[] args) throws Exception {
        require(M3JsonLimits.DEFAULT.maxTokens() == 1_000_000L);
        require(M3JsonLimits.for100MiB().maxTextUnits() == 160_000_000L);
        require(M3JsonLimits.for100MiB().maxChildReferences() < Integer.MAX_VALUE - 8L);
        mustThrow(() -> new M3JsonLimits(0L, 1, 1L, 1L, 1));
        mustThrow(() -> new M3JsonLimits(1L, 0, 1L, 1L, 1));
        mustThrow(() -> new M3JsonLimits(1L, 1, Integer.MAX_VALUE, 1L, 1));
        mustThrow(() -> new M3JsonLimits(1L, 1, 1L, 0L, 1));
        mustThrow(() -> new M3JsonLimits(1L, 1, 1L, 1L, 0));
        String empty = M3SourceHash.utf16("");
        require(empty.length() == 64);
        require(!empty.equals(M3SourceHash.bytes(new byte[0])));
        require(!M3SourceHash.utf16("a").equals(M3SourceHash.utf16("A")));
        require(!M3SourceHash.utf16("a").equals(M3SourceHash.utf16("a\u0000")));
        require(!M3SourceHash.utf16("\uD83D\uDE00").equals(M3SourceHash.utf16("\uFFFD")));
        require(!M3SourceHash.utf16("\uD800").equals(M3SourceHash.utf16("\uFFFD")));
        require(M3SourceHash.utf16("abc").equals(M3SourceHash.utf16("abc")));
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("abc".getBytes(StandardCharsets.UTF_8)));
        require(expected.equals(M3SourceHash.bytes("abc".getBytes(StandardCharsets.UTF_8))));
        System.out.println("M3SourceFactsContract: PASS (16 assertions)");
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("M3 source facts assertion failed");
    }

    private static void mustThrow(Runnable code) {
        try { code.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
}
