// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Isolated-process ContractProbe; not a substitute JUnit implementation. */
public final class CodecProbe {
    private CodecProbe() {}

    /** Probe baseline defects or emit deterministic valid V1 snapshots. */
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "count" -> {
                byte[] bytes = ByteBuffer.allocate(24).putLong(0x4d3349444253454dL)
                        .putInt(1).putInt(0).putInt(0).putInt(0).array();
                ByteBuffer.wrap(bytes).putInt(Integer.parseInt(args[1]), 50_000_000);
                try {
                    M3IndexDbSemanticIndex.decode(bytes);
                    throw new AssertionError("fabricated count accepted");
                } catch (IllegalArgumentException expected) {
                    System.out.println("COUNT_REJECTED");
                }
            }
            case "malformed" -> {
                byte[] bytes = sample("src/ZZZZ.java").encode();
                int at = locate(bytes, "src/ZZZZ.java".getBytes(StandardCharsets.UTF_8));
                bytes[at] = (byte) 0xff;
                try {
                    var value = M3IndexDbSemanticIndex.decode(bytes);
                    System.out.println("MALFORMED_ACCEPTED byteStable="
                            + Arrays.equals(bytes, value.encode()));
                } catch (IllegalArgumentException expected) {
                    System.out.println("MALFORMED_REJECTED");
                }
            }
            case "surrogate" -> {
                String path = "src/" + (char) 0xd800 + ".java";
                try {
                    var before = sample(path);
                    var after = M3IndexDbSemanticIndex.decode(before.encode());
                    System.out.println("SURROGATE_ACCEPTED roundTrip="
                            + before.nodes().equals(after.nodes()));
                } catch (IllegalArgumentException expected) {
                    System.out.println("SURROGATE_REJECTED");
                }
            }
            case "fixtures" -> {
                Path root = Path.of(args[1]);
                Files.createDirectory(root);
                Random random = new Random(672493L);
                for (int n = 0; n < 128; n++) {
                    StringBuilder text = new StringBuilder("src/");
                    for (int i = 0; i < n; i++) {
                        int codePoint;
                        do {
                            codePoint = random.nextInt(0x110000);
                        } while (codePoint >= 0xd800 && codePoint <= 0xdfff);
                        text.appendCodePoint(codePoint);
                    }
                    text.append(".java");
                    var index = sample(text.toString());
                    byte[] bytes = index.encode();
                    if (!Arrays.equals(bytes, M3IndexDbSemanticIndex.decode(bytes).encode())) {
                        throw new AssertionError("valid snapshot round-trip drift");
                    }
                    Files.write(root.resolve(n + ".bin"), bytes);
                }
                System.out.println("VALID_FIXTURES=128");
            }
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    static M3IndexDbSemanticIndex sample(String sourcePath) {
        var kind = M3IndexDbSemanticKind.ATOM;
        String key = "codec";
        var node = new M3IndexDbSemanticNode(M3IndexDbSemanticIndex.nodeId(kind, key), kind,
                key, sourcePath, "symbol", "ContractProbe",
                M3IndexDbSemanticFingerprint.leaf("ATOM", List.of("node"), List.of(key), key));
        return M3IndexDbSemanticIndex.of(List.of(node), List.of());
    }

    static int locate(byte[] bytes, byte[] text) {
        for (int i = 0; i <= bytes.length - text.length; i++) {
            if (Arrays.equals(bytes, i, i + text.length, text, 0, text.length)) return i;
        }
        throw new AssertionError("fixture bytes not found");
    }
}
