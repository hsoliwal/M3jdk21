// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

/**
 * Independent, strict reference decoders written from the published format descriptions only: the LZ4 block format
 * (https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md) and the Snappy raw format
 * (https://github.com/google/snappy/blob/main/format_description.txt). They share no code with the encoders under test
 * and none with the donor's decoder pair, which this module does not relocate.
 *
 * <p>Strict means: a truncated or malformed stream, an offset of 0 or before the start of the output, a length past
 * the declared decoded size, trailing bytes, and for LZ4 every violation of the end-of-block rules (the last sequence
 * carries literals only; every match starts at least {@value #LZ4_MFLIMIT} bytes before the end of the block and ends at
 * least {@value #LZ4_LAST_LITERALS} bytes before it) throw {@link IllegalArgumentException}.
 *
 * <p>{@code stats} (may be {@code null}) accumulates: [0] sequences or elements, [1] matches or copies, [2] bytes
 * produced by matches or copies.
 */
final class M3CompressionSpecDecoders {
    static final int LZ4_MIN_MATCH = 4;
    static final int LZ4_LAST_LITERALS = 5;
    static final int LZ4_MFLIMIT = 12;
    private static final int NIBBLE_MAX = 15;
    private static final int SNAPPY_LONG_LITERAL = 60;

    private M3CompressionSpecDecoders() {}

    /** Decodes one LZ4 raw block whose decoded size the caller knows (the block format does not carry it). */
    static byte[] lz4Block(byte[] block, int decodedLength, long[] stats) {
        Cursor in = new Cursor(block);
        byte[] out = new byte[decodedLength];
        int op = 0;
        while (true) {
            int token = in.u8("LZ4 token");
            long literals = lz4Length(in, token >>> 4);
            require(literals <= decodedLength - op && literals <= in.remaining(), "LZ4 literal run past the block");
            System.arraycopy(block, in.at, out, op, (int) literals);
            in.at += (int) literals;
            op += (int) literals;
            count(stats, 0, 1);
            if (in.remaining() == 0) {
                require((token & NIBBLE_MAX) == 0, "LZ4 last sequence carries a match length");
                break;
            }
            int offset = (int) in.littleEndian(2, "LZ4 offset");
            require(offset != 0 && offset <= op, "LZ4 offset 0 or before the output start");
            long length = lz4Length(in, token & NIBBLE_MAX) + LZ4_MIN_MATCH;
            require(op <= decodedLength - LZ4_MFLIMIT, "LZ4 match starts within the last 12 bytes");
            require(length <= decodedLength - LZ4_LAST_LITERALS - op, "LZ4 match reaches the last 5 bytes");
            op = copy(out, op, offset, (int) length);
            count(stats, 1, 1);
            count(stats, 2, length);
        }
        require(op == decodedLength, "LZ4 decoded size differs from the declared size");
        return out;
    }

    /** Decodes one Snappy raw (unframed) stream: varint decoded length, then literal and copy elements. */
    static byte[] snappyRaw(byte[] raw, long[] stats) {
        Cursor in = new Cursor(raw);
        long declared = in.varint32();
        require(declared <= Integer.MAX_VALUE - 8, "Snappy declared length too large for one array");
        byte[] out = new byte[(int) declared];
        int op = 0;
        while (in.remaining() > 0) {
            int tag = in.u8("Snappy tag");
            int kind = tag & 3;
            count(stats, 0, 1);
            if (kind == 0) {
                long length = literalLength(in, tag >>> 2);
                require(length <= out.length - op && length <= in.remaining(), "Snappy literal past the stream");
                System.arraycopy(raw, in.at, out, op, (int) length);
                in.at += (int) length;
                op += (int) length;
                continue;
            }
            long length;
            long offset;
            if (kind == 1) {
                length = 4 + ((tag >>> 2) & 7);
                offset = ((long) (tag >>> 5) << 8) | in.u8("Snappy copy-1 offset");
            } else {
                length = 1 + (tag >>> 2);
                offset = in.littleEndian(kind == 2 ? 2 : 4, "Snappy copy offset");
            }
            require(offset != 0 && offset <= op, "Snappy offset 0 or before the output start");
            require(length <= out.length - op, "Snappy copy past the declared length");
            op = copy(out, op, (int) offset, (int) length);
            count(stats, 1, 1);
            count(stats, 2, length);
        }
        require(op == out.length, "Snappy decoded size differs from the declared length");
        return out;
    }

    private static long lz4Length(Cursor in, int nibble) {
        long length = nibble;
        if (nibble == NIBBLE_MAX) {
            int next;
            do {
                next = in.u8("LZ4 length extension");
                length += next;
            } while (next == 255);
        }
        return length;
    }

    private static long literalLength(Cursor in, int code) {
        if (code < SNAPPY_LONG_LITERAL) {
            return code + 1L;
        }
        return in.littleEndian(code - 59, "Snappy literal length") + 1;
    }

    /** Byte-by-byte back-reference copy: an offset shorter than the length repeats the overlapped bytes. */
    private static int copy(byte[] out, int op, int offset, int length) {
        for (int i = 0; i < length; i++) {
            out[op + i] = out[op + i - offset];
        }
        return op + length;
    }

    private static void count(long[] stats, int lane, long amount) {
        if (stats != null) {
            stats[lane] += amount;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static final class Cursor {
        private final byte[] bytes;
        private int at;

        Cursor(byte[] bytes) {
            this.bytes = bytes;
        }

        int remaining() {
            return bytes.length - at;
        }

        int u8(String what) {
            require(at < bytes.length, what + " truncated");
            return bytes[at++] & 0xff;
        }

        long littleEndian(int width, String what) {
            long value = 0;
            for (int i = 0; i < width; i++) {
                value |= (long) u8(what) << (8 * i);
            }
            return value;
        }

        long varint32() {
            long value = 0;
            for (int shift = 0; shift <= 28; shift += 7) {
                int next = u8("Snappy length varint");
                value |= (long) (next & 0x7f) << shift;
                if ((next & 0x80) == 0) {
                    require(value <= 0xffffffffL, "Snappy length varint above 32 bits");
                    return value;
                }
            }
            throw new IllegalArgumentException("Snappy length varint longer than 5 bytes");
        }
    }
}
