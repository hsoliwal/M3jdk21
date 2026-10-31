// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.synexia.mindex.compiler.ast.MIndexASTDocument;
import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;
import com.synexia.mindex.compiler.ast.MIndexJavaAST;

import com.synexia.mindex.swap.TieredSwapCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable derived facts. AST rows remain in the existing scoped/partial AST owners. */
public record M3ASTHierarchyFacts(
        M3ASTHierarchy.Level level, String identitySha256, String contextSha256,
        String providerId, int schemaVersion, long[] values) {
    public M3ASTHierarchyFacts {
        Objects.requireNonNull(level, "level");
        identitySha256 = M3FileSourceFacts.requireSha256(identitySha256, "identitySha256");
        contextSha256 = M3FileSourceFacts.requireSha256(contextSha256, "contextSha256");
        Objects.requireNonNull(providerId, "providerId");
        if (providerId.isBlank() || providerId.length() > 1024 || providerId.indexOf('\0') >= 0
                || schemaVersion < 1) throw new IllegalArgumentException("hierarchy provider identity");
        values = Objects.requireNonNull(values, "values").clone();
        if (values.length == 0 || values.length > 256) throw new IllegalArgumentException("fact arity");
        for (long value : values) if (value < 0) throw new IllegalArgumentException("negative fact");
    }

    @Override public long[] values() { return values.clone(); }
    public int valueCount() { return values.length; }
    public long value(int index) { return values[Objects.checkIndex(index, values.length)]; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof M3ASTHierarchyFacts that
                && level == that.level && schemaVersion == that.schemaVersion
                && identitySha256.equals(that.identitySha256) && contextSha256.equals(that.contextSha256)
                && providerId.equals(that.providerId) && Arrays.equals(values, that.values);
    }
    @Override public int hashCode() {
        return 31 * Objects.hash(level, identitySha256, contextSha256, providerId, schemaVersion)
                + Arrays.hashCode(values);
    }

    /** Bounded codec for the existing workspace swap frame; corruption never becomes facts. */
    public static final class Codec implements TieredSwapCache.Codec<M3ASTHierarchyFacts> {
        private static final int MAGIC = 0x4d334846;
        private static final int VERSION = 1;
        private static final int FIXED_BYTES = 120;
        private final int maxValues;
        private final int maxBytes;

        public Codec(int maxValues) {
            if (maxValues < 1 || maxValues > 256) throw new IllegalArgumentException("fact codec arity");
            this.maxValues = maxValues;
            maxBytes = FIXED_BYTES + 2048 + 8 * maxValues;
        }

        @Override public byte[] encode(M3ASTHierarchyFacts facts) throws IOException {
            Objects.requireNonNull(facts, "facts");
            if (facts.values.length > maxValues) throw new IOException("hierarchy fact arity limit");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(facts.level.ordinal());
                out.writeInt(facts.schemaVersion);
                out.write(HexFormat.of().parseHex(facts.identitySha256));
                out.write(HexFormat.of().parseHex(facts.contextSha256));
                out.writeInt(facts.providerId.length());
                for (int i = 0; i < facts.providerId.length(); i++) out.writeChar(facts.providerId.charAt(i));
                out.writeInt(facts.values.length);
                for (long value : facts.values) out.writeLong(value);
                out.flush();
                out.write(digest(bytes.toByteArray(), bytes.size()));
            }
            byte[] result = bytes.toByteArray();
            if (result.length > maxBytes) throw new IOException("hierarchy fact byte limit");
            return result;
        }

        @Override public M3ASTHierarchyFacts decode(byte[] bytes) throws IOException {
            Objects.requireNonNull(bytes, "bytes");
            if (bytes.length < FIXED_BYTES + 10 || bytes.length > maxBytes)
                throw new IOException("hierarchy fact length");
            int body = bytes.length - 32;
            if (!MessageDigest.isEqual(digest(bytes, body), Arrays.copyOfRange(bytes, body, bytes.length)))
                throw new IOException("hierarchy fact checksum");
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, body))) {
                if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IOException("hierarchy fact format");
                int level = in.readInt(), schema = in.readInt();
                if (level < 0 || level >= M3ASTHierarchy.Level.values().length)
                    throw new IOException("hierarchy fact level");
                String identity = HexFormat.of().formatHex(in.readNBytes(32));
                String context = HexFormat.of().formatHex(in.readNBytes(32));
                int chars = in.readInt();
                if (chars < 1 || chars > 1024 || 2L * chars + 4 > in.available())
                    throw new IOException("hierarchy provider length");
                char[] provider = new char[chars];
                for (int i = 0; i < chars; i++) provider[i] = in.readChar();
                int count = in.readInt();
                if (count < 1 || count > maxValues || 8L * count != in.available())
                    throw new IOException("hierarchy fact arity");
                long[] values = new long[count];
                for (int i = 0; i < count; i++) values[i] = in.readLong();
                return new M3ASTHierarchyFacts(M3ASTHierarchy.Level.values()[level], identity,
                        context, new String(provider), schema, values);
            } catch (IllegalArgumentException | NullPointerException invalid) {
                throw new IOException("invalid hierarchy facts", invalid);
            }
        }

        /** Declared retained-size estimate, not a heap or process-memory measurement. */
        @Override public long weight(M3ASTHierarchyFacts facts) {
            return 256L + 2L * facts.providerId.length() + 8L * facts.values.length;
        }

        private static byte[] digest(byte[] bytes, int length) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(bytes, 0, length);
                return digest.digest();
            } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        }
    }
}
