// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.m3.arrays.M3ArrayNative;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/** Actual JNI array materialization into the existing immutable compiler source owner. */
public final class M3NativeSourceSnapshotContract {
    private static int checks;
    private static int snapshots;

    private M3NativeSourceSnapshotContract() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("required native library path");
        M3ArrayNative.load(Path.of(args[0]));
        check(M3ArrayNative.isLoaded(), "required native owner loaded");
        Random random = new Random(0x4d33534f55524345L);
        for (int length : new int[] {0, 1, 2, 3, 7, 31, 4095, 4096, 4097, 8191, 8192, 8193}) {
            char[] expected = new char[length];
            for (int at = 0; at < length; at++) expected[at] = (char) random.nextInt(65536);
            if (length > 0) expected[0] = '\u0000';
            if (length > 1) expected[length - 1] = '\uffff';
            if (length > 4) {
                expected[length / 2 - 1] = '\ud83d';
                expected[length / 2] = '\ude00';
                expected[length / 2 + 1] = '\ud800';
            }
            List<Integer> splits = new ArrayList<>();
            for (int split : new int[] {0, Math.min(1, length), length / 2,
                    Math.max(0, length - 1), length}) {
                if (!splits.contains(split)) splits.add(split);
            }
            for (int split : splits) snapshot(expected, split);
        }
        // ASCII Java source and non-ASCII comment characters follow the same exact native path.
        snapshot("class Example { /* \ud83d\ude00 */ String s = \"M3\"; }".toCharArray(), 22);
        System.out.println("M3_NATIVE_SOURCE_SNAPSHOT_PASS snapshots=" + snapshots
                + " checks=" + checks + " native=true raw_utf16=true");
    }

    private static void snapshot(char[] expected, int split) throws Exception {
        char[][] segments = {new char[0], Arrays.copyOfRange(expected, 0, split),
                new char[0], Arrays.copyOfRange(expected, split, expected.length), new char[0]};
        char[][] segmentCopies = Arrays.stream(segments).map(char[]::clone).toArray(char[][]::new);
        char[] result = M3ArrayNative.concatenateUtf16(segments);
        check(Arrays.equals(expected, result), "native concatenation preserves all code units");
        check(M3ArrayNative.compareUtf16(expected, 0, result, 0, expected.length) == 0,
                "native comparison agrees on the exact materialized range");
        for (int at = 0; at < segments.length; at++) {
            check(Arrays.equals(segments[at], segmentCopies[at]), "JNI leaves source segments intact");
            check(result != segments[at], "result has independent array identity");
        }
        char[] padded = new char[result.length + 6];
        System.arraycopy(result, 0, padded, 3, result.length);
        check(M3ArrayNative.compareUtf16(result, 0, padded, 3, result.length) == 0,
                "native source comparison respects independent offsets");
        String source = new String(result);
        M3File file = M3File.of(URI.create("mem:///NativeSource" + snapshots + ".java"), source);
        check(file.source() == source, "M3File reuses the immutable String payload");
        check(file.getCharContent(false) == source, "compiler receives the admitted String identity");
        check(file.byteSha256() == null, "character input asserts no original byte encoding");
        String digest = file.sourceSha256();
        check(digest.equals(oracle(expected)), "independent framed UTF-16 digest oracle");
        for (int at = 0; at < 8; at++) {
            check(file.sourceSha256() == digest, "repeated consumers share the owned immutable digest");
        }
        String context = M3FileSourceFacts.contextSha256(List.of("javac21", "--release", "21"));
        M3FileSourceFacts facts = M3FileSourceFacts.capture(file.toUri().toString(), file.source(),
                context, "native-source-snapshot-proof", 1, "PARSE");
        check(facts.sourceSha256().equals(digest), "standalone and owner source facts agree");
        check(facts.utf16Length() == expected.length, "source length remains UTF-16 units");
        check(facts.contextSha256().equals(context), "compiler context remains a separate seal");
        for (char[] segment : segments) if (segment.length != 0) segment[0] ^= 0xffff;
        if (result.length != 0) result[0] ^= 0xffff;
        check(Arrays.equals(file.source().toCharArray(), expected),
                "later mutable array changes cannot alter admitted compiler source");
        check(file.sourceSha256() == digest, "immutable source seal survives array-owner changes");
        snapshots++;
    }

    // DataOutputStream is intentionally independent of the product's batched digest framing.
    private static String oracle(char[] source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.write("M3_FILE_SOURCE_UTF16_V1".getBytes(StandardCharsets.US_ASCII));
            output.writeInt(source.length);
            for (char unit : source) output.writeChar(unit);
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }

    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
}
