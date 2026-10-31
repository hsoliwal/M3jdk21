/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Build-time producer of one immutable UTF-16 literal pool for M3-GVAS strict-address mapping.
 *
 * <p>There is only one payload per distinct spelling. The fixed metadata lanes contain no
 * objects or pointers; all addresses are offsets into the same image, never process-local
 * pointers baked into a class file. Emission is independent of source enumeration order.
 * The consumer verifies layout and exact values before publishing native addresses.</p>
 *
 * <pre>
 * little-endian image: magic[8] = M3GVAS01; header[64]; rows[count][32]; padding;
 * payload: UTF-16LE empty-value sentinel followed by deduplicated UTF-16LE strings.
 * row: byteOffset, utf16Length, javaHash, hash31Power, codePointCount, flags,
 *      firstCodeUnit, lastCodeUnit (eight int32 values).
 * </pre>
 */
public final class M3GvasLiteralImageWriter {
    public static final int MAX_DEFAULT_BYTES = 64 * 1024 * 1024;
    public static final int HEADER_BYTES = 64;
    public static final int ROW_BYTES = 32;
    public static final int ASCII = 1;
    public static final int LATIN1 = 2;
    public static final int SURROGATE = 4;
    private static final byte[] MAGIC = "M3GVAS01".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private M3GvasLiteralImageWriter() { }

    /** An ephemeral build-time row, not a runtime String payload owner. */
    public record LiteralRow(String text, int byteOffset, int hash, int hash31Power,
            int codePoints, int flags) { }

    /** Deterministic, content-addressable complete image. */
    public record Result(byte[] bytes, List<LiteralRow> rows) {
        public Result {
            Objects.requireNonNull(bytes);
            rows = List.copyOf(rows);
        }
        public String sha256() { return sha256Hex(bytes); }
    }

    public static Result build(Collection<String> literals) {
        return build(literals, MAX_DEFAULT_BYTES);
    }

    /** Refuse rather than silently truncate an image when the producer exceeds its byte budget. */
    public static Result build(Collection<String> literals, int maximumBytes) {
        Objects.requireNonNull(literals, "literals");
        if (maximumBytes < HEADER_BYTES + 2 || maximumBytes > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("invalid bounded image capacity");
        }
        TreeSet<String> unique = new TreeSet<>();
        for (String literal : literals) unique.add(Objects.requireNonNull(literal, "literal"));
        long rowsEnd = HEADER_BYTES + (long) unique.size() * ROW_BYTES;
        long payloadStart = align16(rowsEnd);
        long total = payloadStart + 2L; // one immutable UTF-16 NUL sentinel for the empty literal
        for (String literal : unique) total += (long) literal.length() * Character.BYTES;
        if (total > maximumBytes || total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("M3-GVAS literal image exceeds bounded capacity");
        }
        ByteBuffer b = ByteBuffer.allocate((int) total).order(ByteOrder.LITTLE_ENDIAN);
        b.put(MAGIC).putInt(1).putInt(HEADER_BYTES).putInt(unique.size())
                .putInt(ROW_BYTES).putInt(HEADER_BYTES).putInt((int) payloadStart)
                .putInt((int) total);
        // All reserved header/padding bytes remain zero. UTF-16LE payload begins after the sentinel.
        b.putChar((int) payloadStart, '\0');
        int rowOffset = HEADER_BYTES;
        int payloadOffset = (int) payloadStart + 2;
        List<LiteralRow> rows = new ArrayList<>(unique.size());
        for (String literal : unique) {
            int length = literal.length();
            int offset = length == 0 ? (int) payloadStart : payloadOffset;
            int flags = flags(literal);
            int power = power31(length);
            int cps = literal.codePointCount(0, length);
            b.putInt(rowOffset, offset).putInt(rowOffset + 4, length)
                    .putInt(rowOffset + 8, literal.hashCode()).putInt(rowOffset + 12, power)
                    .putInt(rowOffset + 16, cps).putInt(rowOffset + 20, flags)
                    .putInt(rowOffset + 24, length == 0 ? -1 : literal.charAt(0))
                    .putInt(rowOffset + 28, length == 0 ? -1 : literal.charAt(length - 1));
            for (int i = 0; i < length; i++) b.putChar(payloadOffset + (i << 1), literal.charAt(i));
            if (length != 0) payloadOffset += length << 1;
            rows.add(new LiteralRow(literal, offset, literal.hashCode(), power, cps, flags));
            rowOffset += ROW_BYTES;
        }
        if (payloadOffset != total) throw new IllegalStateException("image geometry mismatch");
        return new Result(b.array(), rows);
    }

    private static long align16(long n) { return (n + 15L) & ~15L; }

    private static int power31(int exponent) {
        int power = 1;
        int base = 31;
        for (int n = exponent; n != 0; n >>>= 1) {
            if ((n & 1) != 0) power *= base;
            base *= base;
        }
        return power;
    }

    private static int flags(String value) {
        boolean ascii = true;
        boolean latin1 = true;
        boolean surrogate = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            ascii &= c <= 0x7f;
            latin1 &= c <= 0xff;
            surrogate |= Character.isSurrogate(c);
        }
        return (ascii ? ASCII : 0) | (latin1 ? LATIN1 : 0)
                | (surrogate ? SURROGATE : 0);
    }

    public static String sha256Hex(byte[] image) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));
        } catch (NoSuchAlgorithmException missing) {
            throw new AssertionError(missing);
        }
    }

    /** Write once to a build-owned output, not to an active shared-memory inode. */
    public static void writeNew(Path target, Result result) throws IOException {
        Files.write(target, result.bytes(), java.nio.file.StandardOpenOption.CREATE_NEW);
    }
}
