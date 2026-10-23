// SPDX-License-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Portable M3FMT v1 frames over an immutable, ordered M3 lexicon. Word IDs are
 * CWIDs (row + 1); zero is reserved for misses. This is part of M3JDK21, not
 * an M3-to-JDK adapter. The source bytes are stored once per frozen lexicon,
 * while frames store references, slices and exact literal fallbacks.
 *
 * <p>Big-endian frame: 8-byte magic, lexicon SHA-256, decoded length (int),
 * decoded SHA-256, operation count (int), operations. Tags: 0=word/CWID,
 * 1=word/CWID/offset/length, 2=literal/length/bytes. Integers are signed
 * 32-bit nonnegative values. Decoding rejects noncanonical operation shapes.
 */
public final class M3LexiconWire {
    private static final byte[] MAGIC = "M3FMT001".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final byte[] DOMAIN = "M3DICT01".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int FULL = 0;
    private static final int SLICE = 1;
    private static final int LITERAL = 2;
    private static final int HEADER = 8 + 32 + 4 + 32 + 4;
    private static final int MAX_DICTIONARY_BYTES = 128 * 1024 * 1024;
    private final byte[] arena;
    private final int[] offsets;
    private final int[] lengths;
    private final byte[] identity;
    private final int maximumContent;
    private final int maximumFrame;

    /** Construct one immutable byte-level lexicon snapshot, preserving order. */
    public M3LexiconWire(List<byte[]> atoms) {
        this(atoms, 32 * 1024 * 1024, 64 * 1024 * 1024);
    }

    /** Bounded constructor for untrusted transport inputs. */
    public M3LexiconWire(List<byte[]> atoms, int maximumContent, int maximumFrame) {
        Objects.requireNonNull(atoms, "atoms");
        if (maximumContent < 0 || maximumFrame < HEADER || atoms.size() > 1_000_000) {
            throw new IllegalArgumentException("invalid M3FMT bounds");
        }
        this.maximumContent = maximumContent;
        this.maximumFrame = maximumFrame;
        this.offsets = new int[atoms.size()];
        this.lengths = new int[atoms.size()];
        long total = 0;
        for (int i = 0; i < atoms.size(); i++) {
            byte[] atom = Objects.requireNonNull(atoms.get(i), "atom");
            if (atom.length == 0 || (total += atom.length) > MAX_DICTIONARY_BYTES) {
                throw new IllegalArgumentException("invalid lexicon atom or capacity");
            }
            offsets[i] = (int) total - atom.length;
            lengths[i] = atom.length;
        }
        arena = new byte[(int) total];
        for (int i = 0; i < atoms.size(); i++) {
            System.arraycopy(atoms.get(i), 0, arena, offsets[i], lengths[i]);
        }
        MessageDigest sha = sha();
        sha.update(DOMAIN);
        digestInt(sha, atoms.size());
        for (int i = 0; i < atoms.size(); i++) {
            digestInt(sha, lengths[i]);
            sha.update(arena, offsets[i], lengths[i]);
        }
        identity = sha.digest();
        // Open-addressed primitive slots; no per-word Entry objects or quadratic sweep.
        int slotsCount = 1;
        while (slotsCount < Math.max(2L, 2L * atoms.size())) {
            slotsCount = Math.multiplyExact(slotsCount, 2);
        }
        int[] slots = new int[slotsCount];
        for (int i = 0; i < atoms.size(); i++) {
            int position = hash(arena, offsets[i], lengths[i]) & (slots.length - 1);
            while (slots[position] != 0) {
                int previous = slots[position] - 1;
                if (lengths[i] == lengths[previous]
                        && Arrays.equals(arena, offsets[i], offsets[i] + lengths[i],
                                arena, offsets[previous], offsets[previous] + lengths[previous])) {
                    throw new IllegalArgumentException("duplicate canonical atom");
                }
                position = (position + 1) & (slots.length - 1);
            }
            slots[position] = i + 1;
        }
    }

    public byte[] fingerprint() { return identity.clone(); }
    public int size() { return lengths.length; }
    public byte[] arenaCopy() { return arena.clone(); }
    public int[] offsetsCopy() { return offsets.clone(); }
    public int[] lengthsCopy() { return lengths.clone(); }

    /** Encode a token sequence directly from existing canonical CWID lanes. */
    public byte[] encodeCwids(int[] cwids) {
        Objects.requireNonNull(cwids, "cwids");
        return encode(new byte[cwids.length], cwids, new int[cwids.length],
                new int[cwids.length], new byte[0]);
    }

    /**
     * Encode structural primitive lanes. Full rows need tag=0 and CWID; slices
     * use tag=1 and row byte-offset/size; literal tag=2 addresses a subrange
     * of literalBytes by start/length. No per-token objects are retained.
     */
    public byte[] encode(byte[] tags, int[] cwids, int[] starts, int[] sizes, byte[] literalBytes) {
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(cwids, "cwids");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(sizes, "sizes");
        Objects.requireNonNull(literalBytes, "literalBytes");
        if (tags.length != cwids.length || tags.length != starts.length
                || tags.length != sizes.length || tags.length > maximumFrame) {
            throw new IllegalArgumentException("lane length mismatch");
        }
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        MessageDigest outputSha = sha();
        int decoded = 0;
        for (int i = 0; i < tags.length; i++) {
            int tag = tags[i] & 0xff;
            int id = cwids[i];
            int start = starts[i];
            int size;
            if (tag == FULL || tag == SLICE) {
                int row = row(id);
                size = tag == FULL ? lengths[row] : sizes[i];
                if (tag == SLICE && (size <= 0 || start < 0 || start > lengths[row] - size)) {
                    throw new IllegalArgumentException("invalid lexicon slice");
                }
                if (tag == FULL && (start != 0 || sizes[i] != 0)) {
                    throw new IllegalArgumentException("noncanonical full row");
                }
                decoded = checkLength(decoded, size);
                body.write(tag);
                putInt(body, id);
                if (tag == SLICE) {
                    putInt(body, start);
                    putInt(body, size);
                }
                outputSha.update(arena, offsets[row] + (tag == FULL ? 0 : start), size);
            } else if (tag == LITERAL) {
                size = sizes[i];
                if (id != 0 || size <= 0 || start < 0 || start > literalBytes.length - size) {
                    throw new IllegalArgumentException("invalid literal range");
                }
                decoded = checkLength(decoded, size);
                body.write(LITERAL);
                putInt(body, size);
                body.write(literalBytes, start, size);
                outputSha.update(literalBytes, start, size);
            } else {
                throw new IllegalArgumentException("unknown M3FMT operation");
            }
            if (body.size() > maximumFrame - HEADER) {
                throw new IllegalArgumentException("frame limit exceeded");
            }
        }
        ByteArrayOutputStream frame = new ByteArrayOutputStream(HEADER + body.size());
        frame.writeBytes(MAGIC);
        frame.writeBytes(identity);
        putInt(frame, decoded);
        frame.writeBytes(outputSha.digest());
        putInt(frame, tags.length);
        frame.writeBytes(body.toByteArray());
        return frame.toByteArray();
    }

    /** Exact raw-byte escape, including arbitrary binary and malformed UTF-16. */
    public byte[] encodeLiteral(byte[] literalBytes) {
        Objects.requireNonNull(literalBytes, "literalBytes");
        if (literalBytes.length == 0) return encodeCwids(new int[0]);
        return encode(new byte[] { LITERAL }, new int[] { 0 }, new int[] { 0 },
                new int[] { literalBytes.length }, literalBytes);
    }

    /** Validate identity, all geometry and raw-content SHA-256 before returning data. */
    public byte[] decode(byte[] frame) {
        Objects.requireNonNull(frame, "frame");
        if (frame.length < HEADER || frame.length > maximumFrame) {
            throw new IllegalArgumentException("invalid frame length");
        }
        ByteBuffer in = ByteBuffer.wrap(frame);
        byte[] magic = new byte[MAGIC.length];
        in.get(magic);
        if (!Arrays.equals(MAGIC, magic)) throw new IllegalArgumentException("frame version mismatch");
        byte[] dictionary = new byte[32];
        in.get(dictionary);
        if (!MessageDigest.isEqual(identity, dictionary)) {
            throw new IllegalArgumentException("dictionary fingerprint mismatch");
        }
        int decoded = in.getInt();
        if (decoded < 0 || decoded > maximumContent) throw new IllegalArgumentException("decoded length bound");
        byte[] expected = new byte[32];
        in.get(expected);
        int count = in.getInt();
        if (count < 0 || count > in.remaining()) throw new IllegalArgumentException("operation count bound");
        byte[] out = new byte[decoded];
        int written = 0;
        for (int i = 0; i < count; i++) {
            require(in.hasRemaining(), "truncated operation tag");
            int tag = Byte.toUnsignedInt(in.get());
            int id;
            int start;
            int size;
            if (tag == FULL || tag == SLICE) {
                require(in.remaining() >= (tag == FULL ? 4 : 12), "truncated row");
                id = in.getInt();
                int row = row(id);
                start = tag == FULL ? 0 : in.getInt();
                size = tag == FULL ? lengths[row] : in.getInt();
                require(size > 0 && start >= 0 && start <= lengths[row] - size,
                        "out-of-range word reference");
                require(size <= decoded - written, "decoded length mismatch");
                System.arraycopy(arena, offsets[row] + start, out, written, size);
            } else if (tag == LITERAL) {
                require(in.remaining() >= 4, "truncated literal");
                size = in.getInt();
                require(size > 0 && size <= in.remaining() && size <= decoded - written,
                        "invalid literal geometry");
                in.get(out, written, size);
            } else {
                throw new IllegalArgumentException("invalid operation tag");
            }
            written += size;
        }
        require(written == decoded && !in.hasRemaining(), "trailing or missing frame bytes");
        require(MessageDigest.isEqual(expected, sha().digest(out)), "decoded SHA-256 mismatch");
        return out;
    }

    private int row(int cwid) {
        if (cwid <= 0 || cwid > lengths.length) throw new IllegalArgumentException("CWID not present");
        return cwid - 1;
    }

    private int checkLength(int current, int added) {
        if (added <= 0 || added > maximumContent - current) {
            throw new IllegalArgumentException("output exceeds content limit");
        }
        return current + added;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static void putInt(ByteArrayOutputStream out, int value) {
        out.write(value >>> 24);
        out.write(value >>> 16);
        out.write(value >>> 8);
        out.write(value);
    }

    private static void digestInt(MessageDigest sha, int value) {
        sha.update((byte) (value >>> 24));
        sha.update((byte) (value >>> 16));
        sha.update((byte) (value >>> 8));
        sha.update((byte) value);
    }

    private static int hash(byte[] bytes, int start, int length) {
        int value = 0x811c9dc5;
        for (int i = 0; i < length; i++) {
            value = (value ^ (bytes[start + i] & 0xff)) * 0x01000193;
        }
        return value;
    }

    private static MessageDigest sha() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
