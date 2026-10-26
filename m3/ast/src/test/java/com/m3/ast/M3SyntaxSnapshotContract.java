// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Deterministic Java21 lossless parser-interchange and corruption/refusal contract. */
public final class M3SyntaxSnapshotContract {
    private M3SyntaxSnapshotContract() { }
    private static int checks;
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("invalid snapshot accepted");
    }
    private static M3SyntaxTree build(byte[] source) {
        var b = M3SyntaxTree.builder("tree-sitter/java@1", source);
        b.open("file", "", 0, M3SyntaxTree.NAMED);
        b.leaf("id", "name", 0, 1, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        b.leaf("missing", "expected", 1, 1, M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN);
        b.leaf("ERROR", "", 1, 2, M3SyntaxTree.ERROR | M3SyntaxTree.TOKEN);
        b.close(source.length);
        return b.build();
    }
    public static void main(String[] args) {
        byte[] bytes = new byte[] {'a', (byte) 0xff, 0, 13, 10, (byte) 0xc0};
        M3SyntaxTree initial = build(bytes);
        byte[] packet = M3SyntaxSnapshot.encode(initial, 1024);
        M3SyntaxTree restored = M3SyntaxSnapshot.decode(packet, bytes, 64, 1024);
        check(initial.projectionSha256().equals(restored.projectionSha256()), "exact projection identity");
        check(initial.sourceSha256().equals(restored.sourceSha256()), "external source generation");
        check(Arrays.equals(bytes, restored.sourceBytes()), "binary/invalid UTF8 source preserved");
        check(restored.size() == 4 && restored.parent(3) == 0
                && restored.flags(2) == (M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN),
                "recovery and parent edge contract");
        check(Arrays.equals(packet, M3SyntaxSnapshot.encode(restored, packet.length)),
                "binary fixed point");
        check(packet.length < 384, "small interned dictionaries and primitive rows");
        reject(() -> M3SyntaxSnapshot.encode(initial, packet.length - 1));
        reject(() -> M3SyntaxSnapshot.decode(packet, bytes, 3, packet.length));
        reject(() -> M3SyntaxSnapshot.decode(packet, bytes, 10, packet.length - 1));
        reject(() -> M3SyntaxSnapshot.decode(packet, new byte[bytes.length], 10, packet.length));
        reject(() -> M3SyntaxSnapshot.decode(packet, new byte[bytes.length - 1], 10, packet.length));
        for (int at = 0; at < packet.length; at++) {
            byte[] truncated = Arrays.copyOf(packet, at);
            reject(() -> M3SyntaxSnapshot.decode(truncated, bytes, 64, packet.length));
        }
        byte[] trailing = Arrays.copyOf(packet, packet.length + 1);
        reject(() -> M3SyntaxSnapshot.decode(trailing, bytes, 64, trailing.length));
        byte[] wrongVersion = packet.clone(); wrongVersion[11]++;
        reject(() -> M3SyntaxSnapshot.decode(wrongVersion, bytes, 64, packet.length));
        byte[] wrongMagic = packet.clone(); wrongMagic[0]++;
        reject(() -> M3SyntaxSnapshot.decode(wrongMagic, bytes, 64, packet.length));
        byte[] wrongSource = packet.clone(); wrongSource[16]++;
        reject(() -> M3SyntaxSnapshot.decode(wrongSource, bytes, 64, packet.length));
        byte[] wrongProjection = packet.clone(); wrongProjection[48]++;
        reject(() -> M3SyntaxSnapshot.decode(wrongProjection, bytes, 64, packet.length));
        byte[] wrongRows = packet.clone(); ByteBuffer.wrap(wrongRows).putInt(80, 500);
        reject(() -> M3SyntaxSnapshot.decode(wrongRows, bytes, 64, packet.length));
        byte[] wrongParent = packet.clone(); ByteBuffer.wrap(wrongParent).putInt(packet.length - 3 * 4, 9999);
        reject(() -> M3SyntaxSnapshot.decode(wrongParent, bytes, 64, packet.length));

        var deep = M3SyntaxTree.builder("rowan/rust", new byte[] {1});
        for (int i = 0; i < 5000; i++) deep.open("nest", "", 0, 0);
        deep.leaf("leaf", "field", 0, 1, M3SyntaxTree.TOKEN);
        for (int i = 0; i < 5000; i++) deep.close(1);
        M3SyntaxTree deepTree = deep.build();
        byte[] deepPacket = M3SyntaxSnapshot.encode(deepTree, 200_000);
        M3SyntaxTree deepRestored = M3SyntaxSnapshot.decode(deepPacket,
                deepTree.sourceBytes(), 5001, 200_000);
        check(deepRestored.projectionSha256().equals(deepTree.projectionSha256())
                && deepRestored.size() == 5001, "5001-depth stackless codec");

        Random random = new Random(0x4d33415354L);
        for (int iteration = 0; iteration < 500; iteration++) {
            byte[] source = new byte[1 + random.nextInt(120)];
            random.nextBytes(source);
            var tree = M3SyntaxTree.builder("compiler/" + (char) 0xd800 + "raw", source);
            tree.open("root", "", 0, M3SyntaxTree.NAMED);
            int count = random.nextInt(source.length + 1);
            for (int p = 0; p < count; p++) {
                int flags = random.nextBoolean() ? M3SyntaxTree.TOKEN : M3SyntaxTree.TRIVIA | M3SyntaxTree.TOKEN;
                tree.leaf("kind" + (p % 7), p % 2 == 0 ? "" : "f" + (char) 0xdc00, p, p + 1, flags);
            }
            tree.close(source.length);
            var t = tree.build();
            byte[] encoded = M3SyntaxSnapshot.encode(t, 64_000);
            var read = M3SyntaxSnapshot.decode(encoded, source, t.size(), encoded.length);
            check(t.projectionSha256().equals(read.projectionSha256())
                    && Arrays.equals(encoded, M3SyntaxSnapshot.encode(read, encoded.length)),
                    "random exact source, native names, recovery and deterministic roundtrip");
        }
        System.out.println("M3_SYNTAX_SNAPSHOT_PASS checks=" + checks);
    }
}
