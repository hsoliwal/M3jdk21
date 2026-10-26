// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Objects;

/** Minimal transient byte-offset bridge to original M3ASTIndex; no second tree or byte owner. */
public final class M3ASTByteIngress {
    private M3ASTByteIngress() { }

    public interface ByteNodes<N> extends M3ASTExternalIngress.Nodes<N> {
        /** Half-open UTF-8 byte offsets or the unknown pair (-1,-1). */
        int startByte(N node);
        int endByte(N node);
    }

    /** Original source identity is checked; external parser source remains UNVERIFIED. */
    public static <N> M3ASTIndex project(M3File source, M3Utf8SourceMap map,
                                         N root, ByteNodes<N> adapter,
                                         M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(adapter, "adapter");
        if (map.source() != source)
            throw new IllegalArgumentException("UTF-8 map is from a different source generation");
        return M3ASTExternalIngress.project(source, root, new M3ASTExternalIngress.Nodes<>() {
            @Override public String parserId() { return adapter.parserId(); }
            @Override public String kind(N node) { return adapter.kind(node); }
            @Override public int childCount(N node) { return adapter.childCount(node); }
            @Override public N child(N node, int i) { return adapter.child(node, i); }
            @Override public String childRole(N node, int i) { return adapter.childRole(node, i); }
            @Override public int portableFlags(N node) { return adapter.portableFlags(node); }
            @Override public boolean recovered(N node) { return adapter.recovered(node); }
            @Override public int startUtf16(N node) {
                return convert(adapter.startByte(node), adapter.endByte(node), true);
            }
            @Override public int endUtf16(N node) {
                return convert(adapter.startByte(node), adapter.endByte(node), false);
            }
            private int convert(int start, int end, boolean first) {
                if (start == -1 && end == -1) return -1;
                if (start < 0 || end < start || end > map.byteLength())
                    throw new IllegalArgumentException("invalid external byte range");
                return map.utf16Offset(first ? start : end);
            }
        }, limits);
    }
}
