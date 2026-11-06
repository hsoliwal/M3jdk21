// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded, digest-verified *container* for M3InternSpace's existing archive.
 * Does not alter its original wire format or grant OS-wide interned identities.
 * SHA-256 detects accidental/tampered bytes, not authorship or authentication.
 * Immutable archives are intentionally materialized in bounded Java memory.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/main/java/com/synexia/common/intern/M3InternArchiveGuard.java blob 9bb73f2f1d70; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class M3InternArchiveGuard {
    private static final int MAGIC = 0x4d334947; // M3IG
    private static final int VERSION = 1;
    private static final int CORE_MAGIC = 0x4d334953; // M3IS
    private static final int CORE_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES = 12 + DIGEST_BYTES;
    private M3InternArchiveGuard() { }

    /** Caller chooses maximum inner archive bytes (including metadata). */
    public static byte[] write(M3InternSpace space, int maxArchiveBytes) throws IOException {
        Objects.requireNonNull(space, "space");
        requireBudget(maxArchiveBytes);
        if (!space.isFrozen()) throw new IllegalStateException("freeze before archiving");
        LimitedStream data = new LimitedStream(maxArchiveBytes);
        space.writeTo(new DataOutputStream(data));
        byte[] payload = data.toByteArray();
        ByteArrayOutputStream framed = new ByteArrayOutputStream(HEADER_BYTES + payload.length);
        DataOutputStream out = new DataOutputStream(framed);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(payload.length);
        out.write(sha256(payload));
        out.write(payload);
        out.flush();
        return framed.toByteArray();
    }

    /**
     * Fails closed before invoking the original reader on oversized records,
     * malformed tuple arities, bad DAG references or trailing bytes.
     * Record budget is the sum of all four table counts, not a per-table limit.
     */
    public static M3InternSpace read(byte[] framed, int maxArchiveBytes, int maxRecords)
            throws IOException {
        Objects.requireNonNull(framed, "framed");
        requireBudget(maxArchiveBytes);
        if (maxRecords < 0 || maxRecords > (1 << 24)) {
            throw new IllegalArgumentException("invalid record budget");
        }
        if (framed.length < HEADER_BYTES || (long) framed.length > (long) maxArchiveBytes + HEADER_BYTES) {
            throw new IOException("archive frame too large or truncated");
        }
        byte[] safe = framed.clone(); // caller modifications cannot race validation and parse
        ByteBuffer buffer = ByteBuffer.wrap(safe);
        if (buffer.getInt() != MAGIC || buffer.getInt() != VERSION) {
            throw new IOException("invalid archive frame header");
        }
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || payloadLength > maxArchiveBytes
                || payloadLength != safe.length - HEADER_BYTES) {
            throw new IOException("invalid archive payload length");
        }
        byte[] expectedHash = new byte[DIGEST_BYTES];
        buffer.get(expectedHash);
        byte[] payload = Arrays.copyOfRange(safe, HEADER_BYTES, safe.length);
        if (!MessageDigest.isEqual(expectedHash, sha256(payload))) {
            throw new IOException("archive SHA-256 mismatch");
        }
        scan(payload, maxRecords);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            M3InternSpace result = M3InternSpace.readFrom(in);
            if (in.read() != -1) throw new IOException("trailing M3 archive records");
            return result;
        } catch (RuntimeException malformed) {
            throw new IOException("invalid M3 archive contents", malformed);
        }
    }

    private static void requireBudget(int maxArchiveBytes) {
        if (maxArchiveBytes < 16 || maxArchiveBytes > (1 << 27)) {
            throw new IllegalArgumentException("archive byte budget must be 16..134217728");
        }
    }

    /** Allocation-free structural preflight (the outer payload is already bounded). */
    private static void scan(byte[] bytes, int maxRecords) throws IOException {
        ByteBuffer data = ByteBuffer.wrap(bytes);
        if (integer(data) != CORE_MAGIC || integer(data) != CORE_VERSION) {
            throw new IOException("invalid inner M3 archive header");
        }
        long total = 0;
        for (int table = 0; table < 4; table++) {
            int count = integer(data);
            if (count < 0 || count > maxRecords || (total += count) > maxRecords) {
                throw new IOException("intern entry count budget exceeded");
            }
            for (int i = 1; i <= count; i++) {
                if (table < 2) {
                    int n = integer(data);
                    skip(data, n);
                } else {
                    integer(data); // tuple kind or graph opcode
                    int entries = integer(data);
                    if (entries < (table == 3 ? 1 : 0)) {
                        throw new IOException("invalid tuple/graph arity");
                    }
                    if ((long) entries * Long.BYTES > data.remaining()) {
                        throw new IOException("truncated tuple/graph record");
                    }
                    if (table == 3) {
                        data.getLong(); // opaque graph payload
                        for (int child = 1; child < entries; child++) {
                            long predecessor = data.getLong();
                            if (predecessor <= 0 || predecessor >= i) {
                                throw new IOException("forward or invalid DAG edge");
                            }
                        }
                    } else {
                        data.position(data.position() + entries * Long.BYTES);
                    }
                }
            }
        }
        if (data.hasRemaining()) throw new IOException("trailing M3 archive payload");
    }

    private static int integer(ByteBuffer data) throws IOException {
        if (data.remaining() < Integer.BYTES) throw new IOException("truncated M3 metadata");
        return data.getInt();
    }

    private static void skip(ByteBuffer data, int bytes) throws IOException {
        if (bytes < 0 || bytes > data.remaining()) throw new IOException("oversized/truncated byte atom");
        data.position(data.position() + bytes);
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static final class LimitedStream extends OutputStream {
        private final int budget;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private LimitedStream(int budget) { this.budget = budget; }
        @Override public void write(int value) throws IOException {
            if (bytes.size() >= budget) throw new IOException("archive byte budget exceeded");
            bytes.write(value);
        }
        @Override public void write(byte[] source, int offset, int count) throws IOException {
            Objects.checkFromIndexSize(offset, count, source.length);
            if ((long) bytes.size() + count > budget) throw new IOException("archive byte budget exceeded");
            bytes.write(source, offset, count);
        }
        private byte[] toByteArray() { return bytes.toByteArray(); }
    }
}
