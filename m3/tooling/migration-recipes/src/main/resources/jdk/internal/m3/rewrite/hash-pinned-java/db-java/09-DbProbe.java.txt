// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Shared corruption probe for local and real JUnit execution.
 * This is test code, not a new database or function API.
 */
public final class DbProbe {
    private static final long MAGIC = 0x4d3349444253454dL;
    private static final String PATH = "src/Db.java";

    private DbProbe() {}

    /** Each count probe runs in an isolated, bounded-heap child JVM. */
    public static void main(String[] args) {
        if (args.length == 1 && args[0].startsWith("count-")) {
            int column = Integer.parseInt(args[0].substring(6));
            int[] counts = new int[3];
            counts[column] = 50_000_000;
            reject(header(counts[0], counts[1], counts[2]));
        } else if (args.length == 1 && args[0].equals("utf8")) {
            reject(corrupt(new byte[] {(byte) 0x80}));
        } else {
            throw new IllegalArgumentException("count-0/count-1/count-2/utf8 required");
        }
        System.out.println("DB_PARSE_PASS " + args[0]);
    }

    static byte[] header(int strings, int nodes, int edges) {
        return ByteBuffer.allocate(24).putLong(MAGIC).putInt(1)
                .putInt(strings).putInt(nodes).putInt(edges).array();
    }

    static M3IndexDbSemanticIndex sample(String path) {
        var kind = M3IndexDbSemanticKind.ATOM;
        var node = new M3IndexDbSemanticNode(M3IndexDbSemanticIndex.nodeId(kind, "key"),
                kind, "key", path, "symbol", "ContractProbe",
                M3IndexDbSemanticFingerprint.leaf("ATOM", List.of("node"), List.of("leaf"), "return 1;"));
        return M3IndexDbSemanticIndex.of(List.of(node), List.of());
    }

    /** Replace only the sourcePath string's encoded body; retain all node identities and ordinals. */
    static byte[] corrupt(byte[] replacement) {
        byte[] original = sample(PATH).encode();
        ByteBuffer view = ByteBuffer.wrap(original);
        int strings = view.getInt(12);
        view.position(24);
        for (int index = 0; index < strings; index++) {
            int prefix = view.position();
            int length = view.getInt();
            byte[] text = new byte[length];
            view.get(text);
            if (Arrays.equals(text, PATH.getBytes(StandardCharsets.UTF_8))) {
                ByteBuffer out = ByteBuffer.allocate(original.length - length + replacement.length);
                out.put(original, 0, prefix).putInt(replacement.length).put(replacement);
                out.put(original, view.position(), original.length - view.position());
                return out.array();
            }
        }
        throw new AssertionError("fixture sourcePath missing");
    }

    static void reject(byte[] payload) {
        try {
            M3IndexDbSemanticIndex.decode(payload);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("corrupt frame was accepted");
    }
}
