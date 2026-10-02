/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Verify the JDK MIndex backing kernel reads the canonical mapped String/byte format
 * @modules java.base/jdk.internal.mindex
 * @run main MIndexMappedStringBackingTest
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import jdk.internal.mindex.MIndexMappedStringBacking;

public class MIndexMappedStringBackingTest {
    private static final int TEXT_MAGIC = 0x4d49584d;
    private static final int TEXT_VERSION = 3;
    private static final int TEXT_HEADER_BYTES = 64;
    private static final int TEXT_RECORD_MAGIC = 0x4d495852;
    private static final int TEXT_RECORD_HEADER_BYTES = 120;

    private static final int BYTE_MAGIC = 0x4d49424d;
    private static final int BYTE_VERSION = 1;
    private static final int BYTE_HEADER_BYTES = 64;
    private static final int BYTE_RECORD_MAGIC = 0x4d494252;
    private static final int BYTE_RECORD_HEADER_BYTES = 32;

    private static final int COMMIT_SLOT_0 = 16;
    private static final int COMMIT_SLOT_1 = 40;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("mindex-jdk-backing");
        Path textPath = directory.resolve("strings.midx");

        String logical = new String(new char[] {
                'A', '\uD800', 'x', '\uD83D', '\uDE00', 'y', '\uDC00', 'B'
        });
        writeFixture(textPath, logical);

        long defaultId = id(0, 0);
        long languageAliasId = id(7, 1);

        try (MIndexMappedStringBacking backing = MIndexMappedStringBacking.open(textPath)) {
            check(backing.size() == 2, "row count");
            check(backing.languageId(defaultId) == 0, "default language");
            check(backing.languageId(languageAliasId) == 7, "alias language");

            for (long id : new long[] {defaultId, languageAliasId}) {
                check(backing.length(id) == logical.length(), "UTF-16 length");
                check(backing.utf8Length(id) == logical.getBytes(StandardCharsets.UTF_8).length,
                        "UTF-8 length");
                check(backing.codePointCount(id) == logical.codePointCount(0, logical.length()),
                        "code-point count");
                check(backing.unpairedSurrogateCount(id) == unpairedSurrogates(logical),
                        "unpaired-surrogate count");
                check(backing.hashCode(id) == logical.hashCode(), "String hash");
                check(backing.contentEquals(id, logical), "content equality");
                check(backing.materialize(id).equals(logical), "materialization");

                for (int index = 0; index < logical.length(); index++) {
                    check(backing.charAt(id, index) == logical.charAt(index), "charAt " + index);
                }

                var chars = backing.utf16View(id);
                check(chars.isDirect(), "UTF-16 direct");
                check(chars.isReadOnly(), "UTF-16 read-only");
                check(chars.toString().equals(logical), "UTF-16 view");

                var utf8 = backing.utf8View(id);
                check(utf8.isDirect(), "UTF-8 direct");
                check(utf8.isReadOnly(), "UTF-8 read-only");
                byte[] actualUtf8 = new byte[utf8.remaining()];
                utf8.get(actualUtf8);
                check(Arrays.equals(actualUtf8, logical.getBytes(StandardCharsets.UTF_8)),
                        "UTF-8 bytes");
            }

            check(backing.utf8Handle(defaultId) == backing.utf8Handle(languageAliasId),
                    "cross-language UTF-8 handle");

            int emoji = logical.indexOf("\uD83D\uDE00");
            MIndexMappedStringBacking.View high = backing.view(defaultId, emoji, emoji + 1);
            MIndexMappedStringBacking.View low = backing.view(defaultId, emoji + 1, emoji + 2);
            MIndexMappedStringBacking.View pair = backing.view(defaultId, emoji, emoji + 2);
            check(high.toString().equals(logical.substring(emoji, emoji + 1)),
                    "high surrogate substring");
            check(low.toString().equals(logical.substring(emoji + 1, emoji + 2)),
                    "low surrogate substring");
            check(pair.toString().equals(logical.substring(emoji, emoji + 2)),
                    "surrogate pair substring");
            check(pair.codePointCount() == 1, "view code-point count");
            check(pair.hashCode() == logical.substring(emoji, emoji + 2).hashCode(),
                    "view hash");

            check(backing.compare(defaultId, languageAliasId) == 0, "cross-language compare");

            boolean rejected = false;
            try {
                backing.length(id(8, 1));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            check(rejected, "language mismatch rejection");
        }

        System.out.println(
                "MINDEX_JDK_MAPPED_BACKING_PASS|utf16=1|utf8=1|alias=1|substring=1|jni=0");
    }

    private static void writeFixture(Path textPath, String logical) throws Exception {
        byte[] utf8 = logical.getBytes(StandardCharsets.UTF_8);
        Path bytePath = Path.of(textPath.toString() + ".bytes");
        writeByteStore(bytePath, utf8);
        writeTextStore(textPath, logical, utf8.length);
    }

    private static void writeByteStore(Path path, byte[] payload) throws Exception {
        int recordBytes = align8(BYTE_RECORD_HEADER_BYTES + payload.length);
        int committed = BYTE_HEADER_BYTES + recordBytes;
        ByteBuffer file = ByteBuffer.allocate(committed).order(ByteOrder.BIG_ENDIAN);

        writeHeader(file, BYTE_MAGIC, BYTE_VERSION, BYTE_HEADER_BYTES, committed, 1);

        file.position(BYTE_HEADER_BYTES);
        file.putInt(BYTE_RECORD_MAGIC);
        file.putInt(recordBytes);
        file.putInt(payload.length);
        file.putInt(fnv32(payload));
        file.putInt(crc(payload));
        file.putInt(0);
        file.putLong(0L);
        file.put(payload);

        Files.write(path, file.array());
    }

    private static void writeTextStore(Path path, String logical, int utf8Length)
            throws Exception {
        int firstRecordBytes =
                align8(TEXT_RECORD_HEADER_BYTES + logical.length() * Character.BYTES);
        int secondRecordBytes = TEXT_RECORD_HEADER_BYTES;
        int committed = TEXT_HEADER_BYTES + firstRecordBytes + secondRecordBytes;
        ByteBuffer file = ByteBuffer.allocate(committed).order(ByteOrder.BIG_ENDIAN);

        writeHeader(file, TEXT_MAGIC, TEXT_VERSION, TEXT_HEADER_BYTES, committed, 2);

        Metrics metrics = metrics(logical);
        putTextRecord(
                file,
                TEXT_HEADER_BYTES,
                firstRecordBytes,
                0,
                logical,
                utf8Length,
                metrics,
                0,
                charCrc(logical),
                1L,
                true);
        putTextRecord(
                file,
                TEXT_HEADER_BYTES + firstRecordBytes,
                secondRecordBytes,
                7,
                logical,
                utf8Length,
                metrics,
                1,
                0,
                1L,
                false);

        Files.write(path, file.array());
    }

    private static void putTextRecord(
            ByteBuffer file,
            int offset,
            int recordBytes,
            int language,
            String logical,
            int utf8Length,
            Metrics metrics,
            int payloadRef,
            int payloadCrc,
            long utf8Handle,
            boolean writePayload) {
        file.position(offset);
        file.putInt(TEXT_RECORD_MAGIC);
        file.putInt(recordBytes);
        file.putInt(language);
        file.putInt(logical.length());
        file.putInt(utf8Length);
        file.putInt(logical.codePointCount(0, logical.length()));
        file.putInt(unpairedSurrogates(logical));
        file.putInt(logical.hashCode());
        file.putInt(metrics.hash31Power());
        file.putInt(logical.isEmpty() ? -1 : logical.charAt(0));
        file.putInt(logical.isEmpty() ? -1 : logical.charAt(logical.length() - 1));
        file.putInt(payloadRef);
        file.putLong(0L);
        file.putInt(payloadCrc);
        file.putInt(0);
        file.putInt(0);
        file.putInt(0);
        file.putInt(0);
        file.putInt(0);
        file.putLong(0L);
        file.putLong(0L);
        file.putLong(0L);
        file.putLong(0L);
        file.putLong(utf8Handle);
        if (writePayload) {
            for (int index = 0; index < logical.length(); index++) {
                file.putChar(logical.charAt(index));
            }
        }
    }

    private static void writeHeader(
            ByteBuffer file,
            int magic,
            int version,
            int headerBytes,
            long committed,
            int rows) {
        file.putInt(0, magic);
        file.putInt(4, version);
        file.putInt(8, headerBytes);
        file.putInt(12, 0);
        putCommit(file, COMMIT_SLOT_0, new Commit(1L, headerBytes, 0));
        putCommit(file, COMMIT_SLOT_1, new Commit(2L, committed, rows));
    }

    private static void putCommit(ByteBuffer target, int offset, Commit commit) {
        target.putLong(offset, commit.generation());
        target.putLong(offset + 8, commit.committedLength());
        target.putInt(offset + 16, commit.rowCount());
        target.putInt(offset + 20, commitCrc(commit));
    }

    private static int commitCrc(Commit commit) {
        ByteBuffer bytes = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(commit.generation());
        bytes.putLong(commit.committedLength());
        bytes.putInt(commit.rowCount());
        CRC32 crc = new CRC32();
        crc.update(bytes.array());
        return (int) crc.getValue();
    }

    private static int crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private static int charCrc(String value) {
        CRC32 crc = new CRC32();
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            crc.update(unit >>> 8);
            crc.update(unit);
        }
        return (int) crc.getValue();
    }

    private static int fnv32(byte[] bytes) {
        int hash = 0x811c9dc5;
        for (byte value : bytes) {
            hash ^= value & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }

    private static Metrics metrics(String value) {
        int power = 1;
        for (int index = 0; index < value.length(); index++) {
            power *= 31;
        }
        return new Metrics(power);
    }

    private static int unpairedSurrogates(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 < value.length()
                        && Character.isLowSurrogate(value.charAt(index + 1))) {
                    index++;
                } else {
                    count++;
                }
            } else if (Character.isLowSurrogate(unit)) {
                count++;
            }
        }
        return count;
    }

    private static long id(int language, int row) {
        return ((long) language << 32) | Integer.toUnsignedLong(row + 1);
    }

    private static int align8(int value) {
        return (value + 7) & ~7;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Commit(long generation, long committedLength, int rowCount) {}

    private record Metrics(int hash31Power) {}
}
