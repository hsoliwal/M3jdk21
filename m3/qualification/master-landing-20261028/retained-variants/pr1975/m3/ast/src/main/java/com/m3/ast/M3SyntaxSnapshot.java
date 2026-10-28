// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned, bounded binary interchange for the existing lossless M3SyntaxTree.
 * The source bytes are NOT embedded: an independent M3File source generation (or
 * other immutable source owner) supplies them and is verified by SHA-256 before
 * reconstruction. No foreign parser objects, Java object serialization, JNI
 * pointers or duplicate runtime syntax graph survive the handoff.
 *
 * <p>Every row retains grammar-native kind, incoming field, flags, parent and
 * original source-byte range. Tables encode strings as exact UTF-16 units so
 * even unusual provider names are not silently repaired by a charset encoder.
 * The node rows remain primitive, preorder and parent-indexed. Errors, trivia,
 * missing zero-width syntax and unparsed gaps are preserved by the source owner.
 * Import rejects corrupted, ambiguous, stale or over-budget snapshots.
 */
public final class M3SyntaxSnapshot {
    private static final long MAGIC = 0x4d3353594e415354L; // M3SYNAST
    private static final int VERSION = 1;
    private static final int HASH_LENGTH = 32;
    private static final int ROW_BYTES = 6 * Integer.BYTES;
    private M3SyntaxSnapshot() { }

    public static byte[] encode(M3SyntaxTree tree, int maximumPacketBytes) {
        Objects.requireNonNull(tree, "tree");
        if (maximumPacketBytes < 1) throw new IllegalArgumentException("empty packet budget");
        Map<String, Integer> kinds = new LinkedHashMap<>();
        Map<String, Integer> fields = new LinkedHashMap<>();
        for (int node = 0; node < tree.size(); node++) {
            kinds.computeIfAbsent(tree.kind(node), ignored -> kinds.size());
            fields.computeIfAbsent(tree.field(node), ignored -> fields.size());
        }
        try {
            BoundedBuffer buffer = new BoundedBuffer(maximumPacketBytes);
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeLong(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(tree.sourceLength());
            out.write(HexFormat.of().parseHex(tree.sourceSha256()));
            out.write(HexFormat.of().parseHex(tree.projectionSha256()));
            out.writeInt(tree.size());
            writeExactText(out, tree.grammar());
            out.writeInt(kinds.size());
            for (String kind : kinds.keySet()) writeExactText(out, kind);
            out.writeInt(fields.size());
            for (String field : fields.keySet()) writeExactText(out, field);
            for (int node = 0; node < tree.size(); node++) {
                out.writeInt(kinds.get(tree.kind(node)));
                out.writeInt(fields.get(tree.field(node)));
                out.writeInt(tree.flags(node));
                out.writeInt(tree.parent(node));
                out.writeInt(tree.startByte(node));
                out.writeInt(tree.endByte(node));
            }
            out.flush();
            return buffer.toByteArray();
        } catch (IOException unexpected) {
            throw new AssertionError("in-memory snapshot I/O cannot fail", unexpected);
        }
    }

    /**
     * Consume an untrusted packet only with explicit packet/node budgets and an
     * independently obtained, immutable source image. No source bytes are read
     * or inferred from the packet. The caller continues to own its M3File data.
     */
    public static M3SyntaxTree decode(byte[] packet, byte[] source,
            int maximumNodes, int maximumPacketBytes) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(source, "source");
        if (maximumNodes < 1 || maximumPacketBytes < 1
                || packet.length > maximumPacketBytes)
            throw new IllegalArgumentException("snapshot budget exceeded");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet));
            if (in.readLong() != MAGIC || in.readInt() != VERSION)
                throw new IllegalArgumentException("wrong M3AST interchange format/version");
            int length = in.readInt();
            if (length != source.length) throw new IllegalArgumentException("source length drift");
            byte[] expectedSourceSha = in.readNBytes(HASH_LENGTH);
            byte[] expectedProjectionSha = in.readNBytes(HASH_LENGTH);
            if (expectedSourceSha.length != HASH_LENGTH
                    || expectedProjectionSha.length != HASH_LENGTH
                    || !MessageDigest.isEqual(expectedSourceSha, sha256(source)))
                throw new IllegalArgumentException("source generation drift");
            int nodes = in.readInt();
            if (nodes < 1 || nodes > maximumNodes || (long) nodes * ROW_BYTES > in.available())
                throw new IllegalArgumentException("invalid syntax node count");
            String grammar = readExactText(in);
            String[] kinds = readDictionary(in, nodes, false);
            String[] fields = readDictionary(in, nodes, true);
            if ((long) nodes * ROW_BYTES != in.available())
                throw new IllegalArgumentException("noncanonical syntax payload length");

            M3SyntaxTree.Builder builder = M3SyntaxTree.builder(grammar, source);
            // Wire-table IDs are defined by first preorder occurrence in encode().
            // Refuse reordered or unused entries rather than accepting multiple
            // binary encodings of the same source-sealed syntax projection.
            int firstUnusedKind = 0, firstUnusedField = 0;
            int[] open = new int[nodes];
            int[] closingByte = new int[nodes];
            int depth = 0;
            for (int node = 0; node < nodes; node++) {
                int kindId = in.readInt(), fieldId = in.readInt(), flags = in.readInt();
                int parent = in.readInt(), start = in.readInt(), end = in.readInt();
                if (kindId < 0 || kindId >= kinds.length || fieldId < 0
                        || fieldId >= fields.length || start < 0 || end < start || end > source.length)
                    throw new IllegalArgumentException("invalid syntax row");
                if (kindId > firstUnusedKind || fieldId > firstUnusedField)
                    throw new IllegalArgumentException("noncanonical first-use syntax dictionary");
                if (kindId == firstUnusedKind) firstUnusedKind++;
                if (fieldId == firstUnusedField) firstUnusedField++;
                while (depth > 0 && open[depth - 1] != parent) {
                    builder.close(closingByte[depth - 1]);
                    depth--;
                }
                if ((depth == 0 && node != 0) || (node == 0 && parent != -1)
                        || (depth > 0 && (parent != open[depth - 1]
                        || end > closingByte[depth - 1])))
                    throw new IllegalArgumentException("nonpreorder syntax parent/span");
                if (builder.open(kinds[kindId], fields[fieldId], start, flags) != node)
                    throw new IllegalArgumentException("noncanonical syntax order");
                open[depth] = node;
                closingByte[depth] = end;
                depth++;
            }
            if (firstUnusedKind != kinds.length || firstUnusedField != fields.length)
                throw new IllegalArgumentException("unused syntax dictionary entries");
            while (depth > 0) {
                builder.close(closingByte[depth - 1]);
                depth--;
            }
            if (in.available() != 0) throw new IllegalArgumentException("trailing snapshot payload");
            M3SyntaxTree result = builder.build();
            byte[] hash = HexFormat.of().parseHex(result.projectionSha256());
            if (!MessageDigest.isEqual(expectedProjectionSha, hash))
                throw new IllegalArgumentException("syntax projection drift");
            return result;
        } catch (IOException malformed) {
            throw new IllegalArgumentException("truncated or invalid M3AST snapshot", malformed);
        }
    }

    private static String[] readDictionary(DataInputStream in, int maximum, boolean allowEmpty)
            throws IOException {
        int count = in.readInt();
        if (count < 1 || count > maximum || (long) count * Integer.BYTES > in.available())
            throw new IllegalArgumentException("invalid syntax dictionary length");
        String[] values = new String[count];
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            String value = readExactText(in);
            if ((!allowEmpty && value.isBlank()) || !seen.add(value))
                throw new IllegalArgumentException("invalid/duplicate syntax name");
            values[i] = value;
        }
        return values;
    }
    private static String readExactText(DataInputStream in) throws IOException {
        int units = in.readInt();
        if (units < 0 || (long) units * Character.BYTES > in.available())
            throw new IllegalArgumentException("invalid UTF-16 table length");
        char[] chars = new char[units];
        for (int i = 0; i < units; i++) chars[i] = in.readChar();
        return new String(chars);
    }
    private static void writeExactText(DataOutputStream out, String value) throws IOException {
        out.writeInt(value.length());
        for (int i = 0; i < value.length(); i++) out.writeChar(value.charAt(i));
    }
    private static byte[] sha256(byte[] source) {
        try { return MessageDigest.getInstance("SHA-256").digest(source); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static final class BoundedBuffer extends ByteArrayOutputStream {
        private final int limit;
        BoundedBuffer(int budget) { super(Math.min(64, budget)); limit = budget; }
        @Override public synchronized void write(int value) {
            if (size() >= limit) throw new IllegalArgumentException("snapshot byte budget exceeded");
            super.write(value);
        }
        @Override public synchronized void write(byte[] bytes, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length > limit - size())
                throw new IllegalArgumentException("snapshot byte budget exceeded");
            super.write(bytes, offset, length);
        }
    }
}
