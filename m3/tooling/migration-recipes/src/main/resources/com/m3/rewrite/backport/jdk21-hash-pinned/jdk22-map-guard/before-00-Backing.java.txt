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

package jdk.internal.mindex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Read-only JDK-side view of a Synexia MIndex OS-mapped canonical String store.
 *
 * <p>This class intentionally contains no JNI dependency. It reads the same file-backed UTF-16
 * and UTF-8 payloads used by MIndex runtimes and addresses them through stable logical IDs rather
 * than process virtual addresses.</p>
 *
 * <p>The current implementation is a backing kernel, not a replacement for {@code String.value}.
 * It exists so a later java.lang.String/HotSpot patch can consume the canonical storage contract
 * without coupling String semantics to one transport.</p>
 */
public final class MIndexMappedStringBacking implements MIndexStringBacking {
    private static final int TEXT_MAGIC = 0x4d49584d; // MIXM
    private static final int TEXT_VERSION = 3;
    private static final int TEXT_HEADER_BYTES = 64;
    private static final int TEXT_RECORD_MAGIC = 0x4d495852; // MIXR
    private static final int TEXT_RECORD_HEADER_BYTES = 120;

    private static final int BYTE_MAGIC = 0x4d49424d; // MIBM
    private static final int BYTE_VERSION = 1;
    private static final int BYTE_HEADER_BYTES = 64;
    private static final int BYTE_RECORD_MAGIC = 0x4d494252; // MIBR
    private static final int BYTE_RECORD_HEADER_BYTES = 32;

    private static final int COMMIT_SLOT_0 = 16;
    private static final int COMMIT_SLOT_1 = 40;
    private static final int MAP_PAGE_BYTES = 256 * 1024 * 1024;

    private final Path textPath;
    private final FileChannel textChannel;
    private final ByteStore byteStore;
    private final Pages textPages;

    private final long[] charOffsets;
    private final long[] utf8Handles;
    private final int[] languages;
    private final int[] utf16Lengths;
    private final int[] utf8Lengths;
    private final int[] codePointCounts;
    private final int[] unpairedSurrogates;
    private final int[] javaHashes;
    private final int[] hash31Powers;
    private final int[] firstUnits;
    private final int[] lastUnits;
    private final int[] payloadRefs;
    private volatile boolean closed;

    private MIndexMappedStringBacking(Path textPath) {
        try {
            this.textPath = Objects.requireNonNull(textPath, "textPath")
                    .toAbsolutePath().normalize();
            textChannel = FileChannel.open(this.textPath, StandardOpenOption.READ);
            Commit textCommit =
                    readCommit(textChannel, TEXT_MAGIC, TEXT_VERSION, TEXT_HEADER_BYTES, "text");
            textPages = Pages.map(textChannel, textCommit.committedLength());

            byteStore = new ByteStore(Path.of(this.textPath.toString() + ".bytes"));

            int rows = textCommit.rowCount();
            charOffsets = new long[rows];
            utf8Handles = new long[rows];
            languages = new int[rows];
            utf16Lengths = new int[rows];
            utf8Lengths = new int[rows];
            codePointCounts = new int[rows];
            unpairedSurrogates = new int[rows];
            javaHashes = new int[rows];
            hash31Powers = new int[rows];
            firstUnits = new int[rows];
            lastUnits = new int[rows];
            payloadRefs = new int[rows];

            parseTextRows(textCommit);
        } catch (IOException failure) {
            throw new UncheckedIOException("open MIndex mapped String backing: " + textPath, failure);
        } catch (RuntimeException failure) {
            throw failure;
        }
    }

    /** Opens one read-only snapshot of the mapped text store and its sibling byte store. */
    public static MIndexMappedStringBacking open(Path textPath) {
        return new MIndexMappedStringBacking(textPath);
    }

    public Path path() {
        return textPath;
    }

    public int size() {
        return languages.length;
    }

    /** Returns the language coordinate encoded in a validated MIndex text ID. */
    public int languageId(long id) {
        return languages[row(id)];
    }

    /** UTF-16 code-unit length, exactly matching java.lang.String indexing semantics. */
    public int length(long id) {
        return utf16Lengths[row(id)];
    }

    public int utf8Length(long id) {
        return utf8Lengths[row(id)];
    }

    public int codePointCount(long id) {
        return codePointCounts[row(id)];
    }

    public int unpairedSurrogateCount(long id) {
        return unpairedSurrogates[row(id)];
    }

    /** Precomputed java.lang.String-compatible UTF-16 hash code. */
    public int hashCode(long id) {
        return javaHashes[row(id)];
    }

    public int hash31Power(long id) {
        return hash31Powers[row(id)];
    }

    public int firstUtf16Unit(long id) {
        return firstUnits[row(id)];
    }

    public int lastUtf16Unit(long id) {
        return lastUnits[row(id)];
    }

    /** Stable canonical handle in the sibling MIndex byte store. */
    public long utf8Handle(long id) {
        return utf8Handles[row(id)];
    }

    public char charAt(long id, int index) {
        int row = row(id);
        int checked = Objects.checkIndex(index, utf16Lengths[row]);
        long offset = charOffsets[row] + ((long) checked << 1);
        return (char) (((textPages.getByte(offset) & 0xff) << 8)
                | (textPages.getByte(offset + 1L) & 0xff));
    }

    /** Fresh read-only direct UTF-16 cursor over the canonical mapped payload. */
    public CharBuffer utf16View(long id) {
        int row = row(id);
        ByteBuffer bytes =
                textPages.slice(
                        charOffsets[row],
                        Math.multiplyExact(utf16Lengths[row], Character.BYTES));
        return bytes.order(ByteOrder.BIG_ENDIAN).asCharBuffer().asReadOnlyBuffer();
    }

    /** Fresh read-only direct UTF-8 cursor over the canonical sibling byte payload. */
    public ByteBuffer utf8View(long id) {
        return byteStore.view(utf8Handles[row(id)]);
    }

    /** String-compatible UTF-16 range view. The range may split a surrogate pair. */
    public View view(long id, int start, int end) {
        int length = length(id);
        Objects.checkFromToIndex(start, end, length);
        return new View(this, id, start, end - start);
    }

    public View view(long id) {
        return view(id, 0, length(id));
    }

    public String materialize(long id) {
        return utf16View(id).toString();
    }

    String materialize(long id, int start, int length) {
        CharBuffer source = utf16View(id);
        source.position(start);
        source.limit(start + length);
        return source.slice().toString();
    }

    public int compare(long leftId, long rightId) {
        int leftLength = length(leftId);
        int rightLength = length(rightId);
        int common = Math.min(leftLength, rightLength);
        for (int index = 0; index < common; index++) {
            int result = Character.compare(charAt(leftId, index), charAt(rightId, index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(leftLength, rightLength);
    }

    public boolean contentEquals(long id, CharSequence other) {
        Objects.requireNonNull(other, "other");
        int length = length(id);
        if (length != other.length()) {
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (charAt(id, index) != other.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private void parseTextRows(Commit commit) {
        long cursor = TEXT_HEADER_BYTES;
        int row = 0;
        while (row < commit.rowCount()) {
            int pageRemaining = MAP_PAGE_BYTES - (int) (cursor % MAP_PAGE_BYTES);
            if (pageRemaining < TEXT_RECORD_HEADER_BYTES) {
                cursor = nextPage(cursor);
                continue;
            }
            int magic = textPages.getInt(cursor);
            if (magic == 0) {
                cursor = nextPage(cursor);
                continue;
            }
            if (magic != TEXT_RECORD_MAGIC) {
                throw corrupt("text record magic at " + cursor);
            }

            int recordBytes = textPages.getInt(cursor + 4L);
            int language = textPages.getInt(cursor + 8L);
            int utf16Length = textPages.getInt(cursor + 12L);
            int utf8Length = textPages.getInt(cursor + 16L);
            int payloadRef = textPages.getInt(cursor + 44L);
            long utf8Handle = textPages.getLong(cursor + 112L);

            if (utf16Length < 0
                    || utf8Length < 0
                    || recordBytes < TEXT_RECORD_HEADER_BYTES
                    || recordBytes > pageRemaining
                    || cursor + recordBytes > commit.committedLength()) {
                throw corrupt("text record geometry at " + cursor);
            }
            if (byteStore.length(utf8Handle) != utf8Length) {
                throw corrupt("text UTF-8 handle at " + cursor);
            }

            long charOffset;
            if (payloadRef == 0) {
                int expected =
                        align8(Math.addExact(
                                TEXT_RECORD_HEADER_BYTES,
                                Math.multiplyExact(utf16Length, Character.BYTES)));
                if (recordBytes != expected) {
                    throw corrupt("text record size at " + cursor);
                }
                charOffset = cursor + TEXT_RECORD_HEADER_BYTES;
                int expectedCrc = textPages.getInt(cursor + 56L);
                if (charCrc(textPages, charOffset, utf16Length) != expectedCrc) {
                    throw corrupt("text record CRC at " + cursor);
                }
            } else {
                int owner = payloadRef - 1;
                if (owner < 0 || owner >= row || recordBytes != TEXT_RECORD_HEADER_BYTES) {
                    throw corrupt("text payload alias at " + cursor);
                }
                if (utf16Lengths[owner] != utf16Length
                        || utf8Lengths[owner] != utf8Length
                        || javaHashes[owner] != textPages.getInt(cursor + 28L)
                        || utf8Handles[owner] != utf8Handle) {
                    throw corrupt("text payload alias facts at " + cursor);
                }
                charOffset = charOffsets[owner];
            }

            charOffsets[row] = charOffset;
            utf8Handles[row] = utf8Handle;
            languages[row] = language;
            utf16Lengths[row] = utf16Length;
            utf8Lengths[row] = utf8Length;
            codePointCounts[row] = textPages.getInt(cursor + 20L);
            unpairedSurrogates[row] = textPages.getInt(cursor + 24L);
            javaHashes[row] = textPages.getInt(cursor + 28L);
            hash31Powers[row] = textPages.getInt(cursor + 32L);
            firstUnits[row] = textPages.getInt(cursor + 36L);
            lastUnits[row] = textPages.getInt(cursor + 40L);
            payloadRefs[row] = payloadRef;

            row++;
            cursor += recordBytes;
        }
    }

    private int row(long id) {
        requireOpen();
        long coordinate = Integer.toUnsignedLong((int) id);
        if (coordinate == 0L || coordinate > languages.length) {
            throw new IllegalArgumentException("unknown MIndex String id: "
                    + Long.toUnsignedString(id));
        }
        int row = Math.toIntExact(coordinate - 1L);
        if (languages[row] != (int) (id >>> 32)) {
            throw new IllegalArgumentException("MIndex String language mismatch: "
                    + Long.toUnsignedString(id));
        }
        return row;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("MIndex mapped String backing is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            textChannel.close();
        } catch (IOException problem) {
            failure = problem;
        }
        try {
            byteStore.close();
        } catch (UncheckedIOException problem) {
            if (failure == null) {
                failure = problem.getCause();
            } else {
                failure.addSuppressed(problem.getCause());
            }
        }
        if (failure != null) {
            throw new UncheckedIOException("close MIndex mapped String backing", failure);
        }
    }

    /**
     * Lightweight JDK-internal String-shaped view over a canonical mapped MIndex payload.
     *
     * <p>Coordinates are UTF-16 code units. Like String.substring, subSequence may split a
     * surrogate pair.</p>
     */
    public static final class View implements CharSequence {
        private final MIndexMappedStringBacking backing;
        private final long id;
        private final int start;
        private final int length;

        private View(MIndexMappedStringBacking backing, long id, int start, int length) {
            this.backing = backing;
            this.id = id;
            this.start = start;
            this.length = length;
        }

        public long id() {
            return id;
        }

        public int start() {
            return start;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            return backing.charAt(id, start + Objects.checkIndex(index, length));
        }

        @Override
        public View subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return new View(backing, id, start + from, to - from);
        }

        public int codePointCount() {
            return Character.codePointCount(this, 0, length);
        }

        @Override
        public int hashCode() {
            if (start == 0 && length == backing.length(id)) {
                return backing.hashCode(id);
            }
            int hash = 0;
            for (int index = 0; index < length; index++) {
                hash = 31 * hash + charAt(index);
            }
            return hash;
        }

        @Override
        public String toString() {
            return backing.materialize(id, start, length);
        }
    }

    private static final class ByteStore implements AutoCloseable {
        private final FileChannel channel;
        private final Pages pages;
        private final long[] payloadOffsets;
        private final int[] lengths;
        private final int[] hashes;
        private boolean closed;

        ByteStore(Path path) throws IOException {
            channel = FileChannel.open(path, StandardOpenOption.READ);
            Commit commit =
                    readCommit(channel, BYTE_MAGIC, BYTE_VERSION, BYTE_HEADER_BYTES, "byte");
            pages = Pages.map(channel, commit.committedLength());
            payloadOffsets = new long[commit.rowCount()];
            lengths = new int[commit.rowCount()];
            hashes = new int[commit.rowCount()];
            parse(commit);
        }

        int length(long handle) {
            return lengths[row(handle)];
        }

        ByteBuffer view(long handle) {
            int row = row(handle);
            return pages.slice(payloadOffsets[row], lengths[row]).asReadOnlyBuffer();
        }

        private void parse(Commit commit) {
            long cursor = BYTE_HEADER_BYTES;
            int row = 0;
            while (row < commit.rowCount()) {
                int pageRemaining = MAP_PAGE_BYTES - (int) (cursor % MAP_PAGE_BYTES);
                if (pageRemaining < BYTE_RECORD_HEADER_BYTES) {
                    cursor = nextPage(cursor);
                    continue;
                }
                int magic = pages.getInt(cursor);
                if (magic == 0) {
                    cursor = nextPage(cursor);
                    continue;
                }
                if (magic != BYTE_RECORD_MAGIC) {
                    throw corrupt("byte record magic at " + cursor);
                }

                int recordBytes = pages.getInt(cursor + 4L);
                int length = pages.getInt(cursor + 8L);
                int hash = pages.getInt(cursor + 12L);
                int expectedCrc = pages.getInt(cursor + 16L);
                int expected = align8(Math.addExact(BYTE_RECORD_HEADER_BYTES, length));
                if (length < 0
                        || recordBytes != expected
                        || recordBytes > pageRemaining
                        || cursor + recordBytes > commit.committedLength()) {
                    throw corrupt("byte record geometry at " + cursor);
                }

                long payload = cursor + BYTE_RECORD_HEADER_BYTES;
                if (byteCrc(pages, payload, length) != expectedCrc) {
                    throw corrupt("byte record CRC at " + cursor);
                }

                payloadOffsets[row] = payload;
                lengths[row] = length;
                hashes[row] = hash;
                row++;
                cursor += recordBytes;
            }
        }

        private int row(long handle) {
            if (closed || handle <= 0L || handle > lengths.length) {
                throw new IllegalArgumentException("unknown MIndex byte handle: " + handle);
            }
            return Math.toIntExact(handle - 1L);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                channel.close();
            } catch (IOException failure) {
                throw new UncheckedIOException("close MIndex mapped byte backing", failure);
            }
        }
    }

    private static Commit readCommit(
            FileChannel channel, int magic, int version, int headerBytes, String kind)
            throws IOException {
        if (channel.size() < headerBytes) {
            throw corrupt("short " + kind + " header");
        }
        ByteBuffer header = ByteBuffer.allocate(headerBytes).order(ByteOrder.BIG_ENDIAN);
        readFully(channel, header, 0L);
        header.flip();
        if (header.getInt(0) != magic
                || header.getInt(4) != version
                || header.getInt(8) != headerBytes) {
            throw corrupt("unsupported " + kind + " header");
        }

        Commit first = readCommitSlot(header, COMMIT_SLOT_0, headerBytes);
        Commit second = readCommitSlot(header, COMMIT_SLOT_1, headerBytes);
        Commit commit;
        if (first == null && second == null) {
            throw corrupt("no valid " + kind + " commit slot");
        } else if (first == null) {
            commit = second;
        } else if (second == null) {
            commit = first;
        } else {
            commit = first.generation() >= second.generation() ? first : second;
        }

        if (commit.committedLength() < headerBytes
                || commit.committedLength() > channel.size()
                || commit.rowCount() < 0) {
            throw corrupt("invalid " + kind + " commit");
        }
        return commit;
    }

    private static Commit readCommitSlot(ByteBuffer header, int offset, int headerBytes) {
        long generation = header.getLong(offset);
        long committed = header.getLong(offset + 8);
        int rows = header.getInt(offset + 16);
        int crc = header.getInt(offset + 20);
        if (generation <= 0L || committed < headerBytes || rows < 0) {
            return null;
        }
        Commit commit = new Commit(generation, committed, rows);
        return commitCrc(commit) == crc ? commit : null;
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

    private static int byteCrc(Pages pages, long offset, int length) {
        CRC32 crc = new CRC32();
        for (int index = 0; index < length; index++) {
            crc.update(pages.getByte(offset + index));
        }
        return (int) crc.getValue();
    }

    private static int charCrc(Pages pages, long offset, int utf16Length) {
        CRC32 crc = new CRC32();
        long bytes = Math.multiplyExact((long) utf16Length, Character.BYTES);
        for (long index = 0; index < bytes; index++) {
            crc.update(pages.getByte(offset + index));
        }
        return (int) crc.getValue();
    }

    private static void readFully(FileChannel channel, ByteBuffer target, long offset)
            throws IOException {
        long position = offset;
        while (target.hasRemaining()) {
            int read = channel.read(target, position);
            if (read < 0) {
                throw new IOException("unexpected EOF in MIndex backing");
            }
            if (read == 0) {
                continue;
            }
            position += read;
        }
    }

    private static long nextPage(long offset) {
        long page = offset / MAP_PAGE_BYTES;
        return Math.multiplyExact(page + 1L, MAP_PAGE_BYTES);
    }

    private static int align8(int value) {
        return Math.addExact(value, 7) & ~7;
    }

    private static IllegalStateException corrupt(String detail) {
        return new IllegalStateException("corrupt MIndex mapped backing: " + detail);
    }

    private record Commit(long generation, long committedLength, int rowCount) {}

    private static final class Pages {
        private final MappedByteBuffer[] pages;
        private final int[] capacities;
        private final long length;

        private Pages(MappedByteBuffer[] pages, int[] capacities, long length) {
            this.pages = pages;
            this.capacities = capacities;
            this.length = length;
        }

        static Pages map(FileChannel channel, long length) throws IOException {
            int count = Math.toIntExact((length + MAP_PAGE_BYTES - 1L) / MAP_PAGE_BYTES);
            MappedByteBuffer[] pages = new MappedByteBuffer[count];
            int[] capacities = new int[count];
            for (int index = 0; index < count; index++) {
                long offset = (long) index * MAP_PAGE_BYTES;
                int capacity = (int) Math.min((long) MAP_PAGE_BYTES, length - offset);
                capacities[index] = capacity;
                pages[index] = channel.map(FileChannel.MapMode.READ_ONLY, offset, capacity);
            }
            return new Pages(pages, capacities, length);
        }

        byte getByte(long offset) {
            check(offset, 1L);
            int page = (int) (offset / MAP_PAGE_BYTES);
            int inPage = (int) (offset % MAP_PAGE_BYTES);
            return pages[page].get(inPage);
        }

        int getInt(long offset) {
            return slice(offset, Integer.BYTES).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        long getLong(long offset) {
            return slice(offset, Long.BYTES).order(ByteOrder.BIG_ENDIAN).getLong();
        }

        ByteBuffer slice(long offset, int bytes) {
            check(offset, bytes);
            if (bytes == 0) {
                return ByteBuffer.allocate(0).asReadOnlyBuffer();
            }
            int first = (int) (offset / MAP_PAGE_BYTES);
            int last = (int) ((offset + bytes - 1L) / MAP_PAGE_BYTES);
            if (first != last) {
                throw corrupt("record crossed a mapping page boundary");
            }
            int inPage = (int) (offset % MAP_PAGE_BYTES);
            ByteBuffer duplicate = pages[first].asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
            duplicate.position(inPage);
            duplicate.limit(inPage + bytes);
            return duplicate.slice().order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer();
        }

        private void check(long offset, long bytes) {
            if (offset < 0L || bytes < 0L || offset > length - bytes) {
                throw new IndexOutOfBoundsException(
                        "mapped range offset=" + offset + " bytes=" + bytes + " length=" + length);
            }
        }
    }
}
