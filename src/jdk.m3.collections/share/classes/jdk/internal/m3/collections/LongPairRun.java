/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Create-only run codec: 16-byte header, then key/payload/source-ordinal triples, big endian.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/LongPairRun.java}
 * blob {@code 015ad7a0608b20a1e28105fd66cf1dfad7fa671d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class LongPairRun {
    private static final long MAGIC = 0x53584c5052554e31L; // SXLPRUN1
    private LongPairRun() { }
    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    static Path seal(Path data) { return data.resolveSibling(data.getFileName() + ".ok"); }
    static long bytes(long count) { return Math.addExact(16, Math.multiplyExact(24, count)); }
    static String metadata(long count, String hash) { return "LPR1\n" + count + "\n" + hash + "\n"; }
    static boolean before(long key, long ordinal, long oldKey, long oldOrdinal, boolean unsigned) {
        int c = unsigned ? Long.compareUnsigned(key, oldKey) : Long.compare(key, oldKey);
        return c < 0 || (c == 0 && ordinal <= oldOrdinal);
    }
    static void regular(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("not a regular file: " + path.getFileName());
    }
    static final class Reader implements AutoCloseable {
        private final DataInputStream input;
        private final MessageDigest digest;
        private final String expectedHash;
        private final boolean unsigned;
        private final long sourceCount;
        final long count;
        private long remaining;
        private boolean seen;
        private boolean verified;
        long key;
        long payload;
        long ordinal;
        Reader(Path file, int buffer, boolean unsigned, long sourceCount) throws IOException {
            regular(file); Path meta = seal(file); regular(meta);
            if (Files.size(meta) > 128) throw new IOException("run metadata length");
            String text = Files.readString(meta, StandardCharsets.US_ASCII);
            String[] parts = text.split("\n", -1);
            if (parts.length != 4 || !parts[0].equals("LPR1") || !parts[3].isEmpty()
                    || !parts[2].matches("[0-9a-f]{64}")) throw new IOException("run metadata syntax");
            try { count = Long.parseLong(parts[1]); }
            catch (NumberFormatException e) { throw new IOException("run count", e); }
            if (count < 0 || count > sourceCount || !parts[1].equals(Long.toString(count))
                    || count > (Long.MAX_VALUE - 16) / 24 || Files.size(file) != bytes(count))
                throw new IOException("run count/length mismatch");
            expectedHash = parts[2]; this.unsigned = unsigned; this.sourceCount = sourceCount;
            digest = digest(); remaining = count;
            input = new DataInputStream(new BufferedInputStream(
                    new DigestInputStream(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS), digest), buffer));
            try {
                if (input.readLong() != MAGIC || input.readLong() != count) throw new IOException("run header");
            } catch (IOException | RuntimeException | Error failure) {
                try { input.close(); } catch (IOException close) { failure.addSuppressed(close); }
                throw failure;
            }
        }
        boolean next() throws IOException {
            if (remaining == 0) {
                if (!verified) {
                    if (input.read() != -1 || !HexFormat.of().formatHex(digest.digest()).equals(expectedHash))
                        throw new IOException("run digest/EOF mismatch");
                    verified = true;
                }
                return false;
            }
            long k = input.readLong(), p = input.readLong(), o = input.readLong();
            if (o < 0 || o >= sourceCount || (seen && before(k, o, key, ordinal, unsigned)))
                throw new IOException("run ordering/ordinal violation");
            key = k; payload = p; ordinal = o; seen = true; remaining--;
            return true;
        }
        String hash() { return expectedHash; }
        @Override public void close() throws IOException { input.close(); }
    }
    static final class Writer implements AutoCloseable {
        private final Path file;
        private final DataOutputStream output;
        private final MessageDigest digest = digest();
        private final long expected;
        private final boolean unsigned;
        private long count;
        private long oldKey;
        private long oldOrdinal;
        private boolean finished;
        Writer(Path file, long expected, int buffer, boolean unsigned) throws IOException {
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) || Files.exists(seal(file), LinkOption.NOFOLLOW_LINKS))
                throw new java.nio.file.FileAlreadyExistsException(file.toString());
            this.file = file; this.expected = expected; this.unsigned = unsigned;
            output = new DataOutputStream(new BufferedOutputStream(new DigestOutputStream(
                    Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), digest), buffer));
            try { output.writeLong(MAGIC); output.writeLong(expected); }
            catch (IOException failure) { try { output.close(); } catch (IOException close) { failure.addSuppressed(close); } throw failure; }
        }
        void write(long key, long payload, long ordinal) throws IOException {
            if (finished || count == expected || ordinal < 0
                    || (count != 0 && before(key, ordinal, oldKey, oldOrdinal, unsigned)))
                throw new IOException("run writer order/count");
            output.writeLong(key); output.writeLong(payload); output.writeLong(ordinal);
            oldKey = key; oldOrdinal = ordinal; count++;
        }
        void finish() throws IOException {
            if (finished || count != expected) throw new IOException("incomplete run");
            output.close();
            String hash = HexFormat.of().formatHex(digest.digest());
            Files.writeString(seal(file), metadata(count, hash), StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            finished = true;
        }
        @Override public void close() throws IOException { output.close(); }
    }
}
