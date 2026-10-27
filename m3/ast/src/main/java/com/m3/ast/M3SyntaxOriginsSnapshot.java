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
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Source-external, byte-bounded wire representation of the M3AST multi-origin
 * provenance sidecar. The existing M3SyntaxOrigins primitive columns remain the
 * sole live origin owner. The packet holds source IDs, source SHA-256 digests and
 * sorted range tuples; it contains neither source payloads nor AST node objects.
 *
 * <p>Separate identity of each file is preserved. UTF-16 source IDs are written
 * as exact Java code units rather than normalized/decoded through the platform
 * charset; sources are ordered by their first encountered row. Byte ranges
 * remain absolute half-open physical byte offsets, independent of the grammar.
 *
 * <p>SHA-256 of the packet detects accidental corruption, NOT an authenticated
 * signature. A decoded origin remains metadata: callers MUST use
 * M3SyntaxOrigins.verify(tree, sourceLoader, ...) before relying on exact byte
 * coordinates. Its projection hash alone cannot prove external source content.
 */
public final class M3SyntaxOriginsSnapshot {
    private static final long MAGIC = 0x4d334f524947494eL; // M3ORIGIN
    private static final int VERSION = 1;
    private static final int HASH_BYTES = 32;
    private static final int ROW_BYTES = 6 * Integer.BYTES;
    private static final int MAX_SOURCE_ID_UNITS = 2048;
    private static final int MIN_PACKET_BYTES = Long.BYTES + 3 * Integer.BYTES
            + HASH_BYTES * 2; // header plus digest; variable parts follow

    private M3SyntaxOriginsSnapshot() { }

    /** Serialize canonical first-use dictionaries and six primitive range lanes. */
    public static byte[] encode(M3SyntaxOrigins origins, int maximumPacketBytes) {
        Objects.requireNonNull(origins, "origins");
        if (maximumPacketBytes < MIN_PACKET_BYTES)
            throw new IllegalArgumentException("origin snapshot byte budget too small");
        Map<String, Integer> sources = new LinkedHashMap<>();
        Map<String, String> hashes = new LinkedHashMap<>();
        for (int row = 0; row < origins.count(); row++) {
            M3SyntaxOrigins.Origin origin = origins.origin(row);
            sources.computeIfAbsent(origin.sourceId(), ignored -> sources.size());
            String previous = hashes.putIfAbsent(origin.sourceId(), origin.sha256());
            if (previous != null && !previous.equals(origin.sha256()))
                throw new IllegalArgumentException("conflicting provenance generation");
        }
        try {
            BoundedBuffer buffer = new BoundedBuffer(maximumPacketBytes);
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeLong(MAGIC);
            out.writeInt(VERSION);
            out.write(HexFormat.of().parseHex(origins.projectionSha256()));
            out.writeInt(origins.count());
            out.writeInt(sources.size());
            for (String id : sources.keySet()) {
                writeText(out, id);
                out.write(HexFormat.of().parseHex(hashes.get(id)));
            }
            for (int i = 0; i < origins.count(); i++) {
                M3SyntaxOrigins.Origin origin = origins.origin(i);
                out.writeInt(origins.generatedStart(i));
                out.writeInt(origins.generatedEnd(i));
                out.writeInt(sources.get(origin.sourceId()));
                out.writeInt(origin.startByte());
                out.writeInt(origin.endByte());
                out.writeInt(origin.relation().ordinal());
            }
            out.flush();
            byte[] content = buffer.toByteArray();
            out.write(digest(content, 0, content.length));
            out.flush();
            return buffer.toByteArray();
        } catch (IOException cannotHappen) {
            throw new AssertionError("in-memory origin serialization failed", cannotHappen);
        }
    }

    /**
     * Parse untrusted bytes only under explicit row, dictionary and packet
     * budgets, and with the caller's existing immutable syntax generation.
     * Both the SHA digest and a byte-for-byte canonical re-encoding are checked.
     * No network fetch or source owner is created by this decoder.
     */
    public static M3SyntaxOrigins decode(byte[] packet, M3SyntaxTree tree,
            int maximumRows, int maximumSources, int maximumPacketBytes) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(tree, "tree");
        if (maximumRows < 0 || maximumSources < 0 || maximumPacketBytes < MIN_PACKET_BYTES
                || packet.length > maximumPacketBytes || packet.length < MIN_PACKET_BYTES)
            throw new IllegalArgumentException("origin snapshot budget exceeded");
        byte[] immutablePacket = packet.clone();
        int bodyLength = immutablePacket.length - HASH_BYTES;
        if (bodyLength < Long.BYTES + 3 * Integer.BYTES + HASH_BYTES)
            throw new IllegalArgumentException("short origin packet");
        byte[] expectedDigest = Arrays.copyOfRange(immutablePacket, bodyLength,
                immutablePacket.length);
        if (!MessageDigest.isEqual(expectedDigest, digest(immutablePacket, 0, bodyLength)))
            throw new IllegalArgumentException("origin packet checksum mismatch");
        try {
            DataInputStream in = new DataInputStream(
                    new ByteArrayInputStream(immutablePacket, 0, bodyLength));
            if (in.readLong() != MAGIC || in.readInt() != VERSION)
                throw new IllegalArgumentException("wrong M3AST origin packet version");
            byte[] seal = in.readNBytes(HASH_BYTES);
            if (seal.length != HASH_BYTES || !MessageDigest.isEqual(seal,
                    HexFormat.of().parseHex(tree.projectionSha256())))
                throw new IllegalArgumentException("stale syntax/source generation");
            int rows = in.readInt(), sourceCount = in.readInt();
            if (rows < 0 || rows > maximumRows || sourceCount < 0
                    || sourceCount > maximumSources || sourceCount > rows
                    || (long) rows * ROW_BYTES > in.available())
                throw new IllegalArgumentException("origin row or source budget exceeded");

            String[] sourceIds = new String[sourceCount];
            String[] sourceSha = new String[sourceCount];
            Set<String> uniqueSources = new HashSet<>();
            for (int i = 0; i < sourceCount; i++) {
                String name = readText(in);
                if (name.isBlank() || name.indexOf('\0') >= 0 || !uniqueSources.add(name))
                    throw new IllegalArgumentException("invalid/duplicate origin source ID");
                byte[] hash = in.readNBytes(HASH_BYTES);
                if (hash.length != HASH_BYTES)
                    throw new IllegalArgumentException("truncated origin source hash");
                sourceIds[i] = name;
                sourceSha[i] = HexFormat.of().formatHex(hash);
            }
            if ((long) rows * ROW_BYTES != in.available())
                throw new IllegalArgumentException("noncanonical origin packet length");

            M3SyntaxOrigins.Builder builder = M3SyntaxOrigins.builder(tree);
            int usedSources = 0;
            M3SyntaxOrigins.Relation[] relations = M3SyntaxOrigins.Relation.values();
            for (int row = 0; row < rows; row++) {
                int from = in.readInt(), to = in.readInt();
                int sourceId = in.readInt(), sourceStart = in.readInt();
                int sourceEnd = in.readInt(), rel = in.readInt();
                if (sourceId < 0 || sourceId >= sourceCount || sourceId > usedSources
                        || rel < 0 || rel >= relations.length)
                    throw new IllegalArgumentException("noncanonical origin row/source ID");
                if (sourceId == usedSources) usedSources++;
                builder.add(from, to, sourceIds[sourceId], sourceSha[sourceId],
                        sourceStart, sourceEnd, relations[rel]);
            }
            if (usedSources != sourceCount || in.available() != 0)
                throw new IllegalArgumentException("unused dictionary or trailing origin data");
            M3SyntaxOrigins result = builder.freeze();
            if (!Arrays.equals(encode(result, maximumPacketBytes), immutablePacket))
                throw new IllegalArgumentException("noncanonical origin packet representation");
            return result;
        } catch (IOException invalid) {
            throw new IllegalArgumentException("truncated origin packet", invalid);
        }
    }

    private static String readText(DataInputStream in) throws IOException {
        int units = in.readInt();
        if (units < 0 || units > MAX_SOURCE_ID_UNITS
                || (long) units * Character.BYTES > in.available())
            throw new IllegalArgumentException("invalid source ID UTF-16 length");
        char[] data = new char[units];
        for (int i = 0; i < units; i++) data[i] = in.readChar();
        return new String(data);
    }
    private static void writeText(DataOutputStream out, String s) throws IOException {
        if (s.length() > MAX_SOURCE_ID_UNITS)
            throw new IllegalArgumentException("overlong origin source ID");
        out.writeInt(s.length());
        for (int i = 0; i < s.length(); i++) out.writeChar(s.charAt(i));
    }
    private static byte[] digest(byte[] bytes, int offset, int length) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(bytes, offset, length);
            return sha.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
    private static final class BoundedBuffer extends ByteArrayOutputStream {
        private final int maximum;
        BoundedBuffer(int maximum) { super(Math.min(maximum, 256)); this.maximum = maximum; }
        @Override public synchronized void write(int value) {
            if (size() >= maximum)
                throw new IllegalArgumentException("origin packet exceeds byte budget");
            super.write(value);
        }
        @Override public synchronized void write(byte[] value, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, value.length);
            if (length > maximum - size())
                throw new IllegalArgumentException("origin packet exceeds byte budget");
            super.write(value, offset, length);
        }
    }
}
