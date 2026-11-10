/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * White-box decoder contract for published word-row images (format 3).
 *
 * <p>Each corrupt image differs from one valid fixture in exactly one header,
 * row or relation field, and must be refused with the decoder's specific
 * reason. The valid fixture proves every scalar accessor, generation guard,
 * cursor relation and the package-private native read view. No JNI library is
 * loaded: native bulk reads need the separately built word-row library.</p>
 */
public final class M3WordRowsImageRefusalContractTest {
    private static final int COUNT = 3;
    private static final int ROWS = 128;
    private static final int MEMBERS = 512;
    private static final int CONCEPTS = 576;
    private static final int SUBJECTS = 640;
    private static final int EXPANSIONS = 704;
    private static final int END = 728;
    private static final int LANGUAGE = 42;
    private static final long FINGERPRINT = 0x1122334455667788L;
    private static final String MAGIC_OR_STRIDE = "unsupported M3 word-row magic, format, or stride";
    private static final String HEADER = "negative or unqualified M3 word-row header field";
    private static final String GEOMETRY = "corrupt M3 word-row section geometry";
    private static final String NEGATIVE_ROW = "negative M3 word-row scalar at local ID 0";
    private static final String RANKS = "invalid rank/range geometry at local ID 0";
    private static final String RELATION = "invalid M3 word-row relation offset/count";
    private static long checks;
    private static final List<Path> written = new ArrayList<>();

    private M3WordRowsImageRefusalContractTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private interface Action { void run() throws Exception; }

    private static Throwable refused(Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage()))) {
                return thrown;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " '" + reason + "'", thrown);
        }
        throw new AssertionError("accepted, expected refusal: " + reason);
    }

    /** Three words "a", "ab", "b" with one membership/concept/subject/expansion each. */
    private static ByteBuffer fixture() {
        ByteBuffer image = ByteBuffer.allocate(END).order(ByteOrder.BIG_ENDIAN);
        image.putLong(0, M3WordRows.MAGIC).putInt(8, M3WordRows.FORMAT).putInt(12, LANGUAGE)
                .putInt(16, COUNT).putInt(20, M3WordRows.ROW_BYTES).putLong(24, FINGERPRINT)
                .putLong(32, 0x8877665544332211L).putLong(40, 60L).putLong(48, 3L)
                .putInt(56, 2).putInt(60, 2).putInt(64, ROWS).putInt(68, MEMBERS)
                .putInt(72, COUNT).putInt(76, CONCEPTS).putInt(80, COUNT).putInt(84, SUBJECTS)
                .putInt(88, COUNT).putInt(92, EXPANSIONS).putInt(96, COUNT).putInt(100, END);
        image.put(104, (byte) 'e').put(105, (byte) 'n').put(106, (byte) 'v').put(107, (byte) '3');
        String[] values = {"a", "ab", "b"};
        int[] reverseRanks = {0, 2, 1};
        int[] prefixEnds = {2, 2, 3};
        int[] suffixEnds = {1, 3, 3};
        for (int i = 0; i < COUNT; i++) {
            int p = ROWS + (i << 7);
            int length = values[i].length();
            image.putInt(p, i * 5 + 5).putInt(p + 4, length).putInt(p + 8, length)
                    .putInt(p + 12, length).putInt(p + 16, values[i].hashCode())
                    .putInt(p + 20, 0x80 + i).putLong(p + 24, 0x100000000L + i)
                    .putLong(p + 32, 10L * (i + 1)).putInt(p + 40, i + 1).putInt(p + 44, i)
                    .putInt(p + 48, reverseRanks[i]).putInt(p + 52, COUNT - i - 1)
                    .putInt(p + 56, prefixEnds[i]).putInt(p + 60, suffixEnds[i])
                    .putInt(p + 72, MEMBERS + i * Integer.BYTES).putInt(p + 76, 1)
                    .putInt(p + 80, CONCEPTS + i * Long.BYTES).putInt(p + 84, 1)
                    .putInt(p + 88, SUBJECTS + i * Long.BYTES).putInt(p + 92, 1)
                    .putInt(p + 96, EXPANSIONS + i * Long.BYTES).putInt(p + 100, 1)
                    .putInt(p + 116, length == 1 ? 31 : 961);
            image.putInt(MEMBERS + i * 4, 10 + i).putLong(CONCEPTS + i * 8, 100 + i)
                    .putLong(SUBJECTS + i * 8, 200 + i).putLong(EXPANSIONS + i * 8, 300 + i);
        }
        return image;
    }

    private static Path write(Path dir, String name, byte[] bytes) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        written.add(file);
        return file;
    }

    private static void badInt(Path dir, int offset, int value, String reason) throws IOException {
        ByteBuffer image = fixture();
        image.putInt(offset, value);
        Path file = write(dir, "int-" + offset + "-" + Integer.toHexString(value) + ".m3rows", image.array());
        refused(IOException.class, reason, () -> M3WordRows.open(file));
    }

    private static void badLong(Path dir, int offset, long value, String reason) throws IOException {
        ByteBuffer image = fixture();
        image.putLong(offset, value);
        Path file = write(dir, "long-" + offset + "-" + Long.toHexString(value) + ".m3rows", image.array());
        refused(IOException.class, reason, () -> M3WordRows.open(file));
    }

    private static void header(Path dir) throws IOException {
        badLong(dir, 0, 0L, MAGIC_OR_STRIDE);
        badInt(dir, 8, 2, MAGIC_OR_STRIDE);
        badInt(dir, 20, 64, MAGIC_OR_STRIDE);
        badInt(dir, 12, -1, HEADER);
        badInt(dir, 12, M3WordRows.MAX_LANGUAGE_ID + 1, HEADER);
        badInt(dir, 16, -1, HEADER);
        for (int field : new int[] {56, 60, 72, 80, 88, 96}) badInt(dir, field, -1, HEADER);
        badLong(dir, 40, -1L, HEADER);
        badLong(dir, 48, -1L, HEADER);
        badInt(dir, 16, Integer.MAX_VALUE, "invalid M3 word-row arithmetic");
        for (int field : new int[] {64, 68, 76, 84, 92, 100}) badInt(dir, field, END + 64, GEOMETRY);
        byte[] longer = Arrays.copyOf(fixture().array(), END + 64);
        Path padded = write(dir, "padded.m3rows", longer);
        refused(IOException.class, GEOMETRY, () -> M3WordRows.open(padded));
        ByteBuffer tag = fixture();
        tag.put(104, (byte) 0xff);
        Path utf8 = write(dir, "tag-utf8.m3rows", tag.array());
        Throwable invalid = refused(IOException.class, "invalid M3 word-row UTF-8 metadata",
                () -> M3WordRows.open(utf8));
        check(invalid.getCause() instanceof java.nio.charset.CharacterCodingException, "UTF-8 cause");
        Path tiny = write(dir, "tiny.m3rows", new byte[M3WordRows.HEADER_BYTES - 1]);
        refused(IOException.class, "invalid M3 word-row image length", () -> M3WordRows.open(tiny));
        refused(java.nio.file.NoSuchFileException.class, null, () -> M3WordRows.open(dir.resolve("absent")));
        refused(NullPointerException.class, "source", () -> M3WordRows.open(null));
    }

    /** A sparse image beyond int range is refused before any mapping is attempted. */
    private static void oversized(Path dir) throws IOException {
        if (Files.getFileStore(dir).getUsableSpace() < (4L << 30)) {
            System.out.println("M3_WORDROWS_OVERSIZE_SKIPPED reason=temp-store-below-4GiB");
            return;
        }
        Path file = dir.resolve("oversized.m3rows");
        written.add(file);
        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, StandardOpenOption.SPARSE)) {
            channel.position(Integer.MAX_VALUE).write(ByteBuffer.wrap(new byte[1]));
        }
        check(Files.size(file) == Integer.MAX_VALUE + 1L, "sparse image length");
        refused(IOException.class, "invalid M3 word-row image length", () -> M3WordRows.open(file));
        Files.delete(file);
        written.remove(file);
    }

    private static void rows(Path dir) throws IOException {
        int row = ROWS;
        for (int field : new int[] {4, 8, 12, 40}) badInt(dir, row + field, -1, NEGATIVE_ROW);
        badLong(dir, row + 32, -1L, NEGATIVE_ROW);
        badInt(dir, row + 12, 5, "invalid UTF-16/code-point relationship");
        badInt(dir, row + 44, -1, RANKS);
        badInt(dir, row + 44, COUNT, RANKS);
        badInt(dir, row + 48, -1, RANKS);
        badInt(dir, row + 48, COUNT, RANKS);
        badInt(dir, row + 52, -1, RANKS);
        badInt(dir, row + 52, COUNT, RANKS);
        badInt(dir, row + 56, 0, RANKS);
        badInt(dir, row + 56, COUNT + 1, RANKS);
        badInt(dir, row + 60, 0, RANKS);
        badInt(dir, row + 60, COUNT + 1, RANKS);
        badInt(dir, row + 76, -1, RELATION);
        badInt(dir, row + 72, MEMBERS - 4, RELATION);
        badInt(dir, row + 72, CONCEPTS + 4, RELATION);
        badInt(dir, row + 72, MEMBERS + 2, RELATION);
        badInt(dir, row + 76, 10_000, RELATION);
        badInt(dir, row + 80, CONCEPTS + 4, RELATION);
        badInt(dir, row + 92, 9, RELATION);
        badInt(dir, row + 96, EXPANSIONS - 8, RELATION);
    }

    private static void accessors(M3WordRows rows) {
        check(rows.languageId() == LANGUAGE && rows.wordCount() == COUNT, "header identity");
        check(rows.lexiconFingerprint() == FINGERPRINT, "lexicon fingerprint");
        check(rows.languageTag().equals("en") && rows.dictionaryVersion().equals("v3"), "UTF-8 metadata");
        String[] values = {"a", "ab", "b"};
        int[] prefixEnds = {2, 2, 3};
        int[] suffixEnds = {1, 3, 3};
        for (int i = 0; i < COUNT; i++) {
            check(rows.javaHash(i) == values[i].hashCode(), "Java String hash lane " + i);
            check(rows.characterFlags(i) == 0x80 + i, "character flags lane " + i);
            check(rows.bitSignal(i) == 0x100000000L + i, "bit signal lane " + i);
            check(rows.prefixEndExclusive(i) == prefixEnds[i], "prefix range end " + i);
            check(rows.suffixEndExclusive(i) == suffixEnds[i], "suffix range end " + i);
            check(rows.hash31Power(i) == (values[i].length() == 1 ? 31 : 961), "31^length lane " + i);
        }
        refused(IndexOutOfBoundsException.class, null, () -> rows.javaHash(COUNT));
        refused(IndexOutOfBoundsException.class, null, () -> rows.hash31Power(-1));
        check(rows.isPrefixOf(0, 1) && !rows.isPrefixOf(1, 0) && !rows.isPrefixOf(0, 2), "prefix ranges");
        check(rows.isSuffixOf(2, 1) && !rows.isSuffixOf(0, 1) && !rows.isSuffixOf(1, 0), "suffix ranges");
        ByteBuffer view = rows.nativeImage();
        check(view.isReadOnly() && view.order() == ByteOrder.BIG_ENDIAN, "native view is read-only big-endian");
        check(view.capacity() == rows.mappedImageBytes() && view.getLong(0) == M3WordRows.MAGIC,
                "native view covers the whole image");
        check(rows.nativeRowsBase() == rows.rowByteOffset(0) && rows.nativeRowsBase() == ROWS,
                "native rows base");
    }

    private static void generation(M3WordRows rows) {
        rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "en", "v3");
        String other = "M3 word rows belong to another lexicon generation";
        refused(IllegalArgumentException.class, other,
                () -> rows.requireGeneration(LANGUAGE + 1, COUNT, FINGERPRINT, "en", "v3"));
        refused(IllegalArgumentException.class, other,
                () -> rows.requireGeneration(LANGUAGE, COUNT + 1, FINGERPRINT, "en", "v3"));
        refused(IllegalArgumentException.class, other,
                () -> rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT + 1, "en", "v3"));
        refused(IllegalArgumentException.class, other,
                () -> rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "de", "v3"));
        refused(IllegalArgumentException.class, other,
                () -> rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "en", "v4"));
        refused(NullPointerException.class, "tag",
                () -> rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, null, "v3"));
        refused(NullPointerException.class, "version",
                () -> rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "en", null));
    }

    private static void cursors(M3WordRows rows, M3WordRows foreign) {
        M3WordRows.Cursor unbound = rows.cursor();
        String notBound = "M3 word cursor not bound";
        refused(IllegalStateException.class, notBound, unbound::localId);
        refused(IllegalStateException.class, notBound, unbound::rowByteOffset);
        refused(IllegalStateException.class, notBound, unbound::flags);
        refused(IllegalStateException.class, notBound, unbound::corpusCount);
        M3WordRows.Cursor a = rows.cursor().reset(0);
        M3WordRows.Cursor ab = rows.cursor().reset(1);
        M3WordRows.Cursor b = rows.cursor().reset(2);
        check(a.localId() == 0 && b.localId() == 2, "bound local IDs");
        check(ab.rowByteOffset() == rows.rowByteOffset(1), "cursor row address");
        check(a.isPrefixOf(ab) && !ab.isPrefixOf(a) && !a.isPrefixOf(b), "cursor prefix relation");
        check(b.isSuffixOf(ab) && !a.isSuffixOf(ab) && !ab.isSuffixOf(a), "cursor suffix relation");
        refused(IllegalStateException.class, notBound, () -> a.sameWord(rows.cursor()));
        refused(IllegalStateException.class, notBound, () -> rows.cursor().isPrefixOf(a));
        refused(NullPointerException.class, "other", () -> a.isSuffixOf(null));
        refused(IllegalArgumentException.class, "foreign word generation",
                () -> a.isPrefixOf(foreign.cursor().reset(1)));
        check(a.reset(2).localId() == 2 && a.sameWord(b), "cursor rebinding reuses scratch");
        refused(IndexOutOfBoundsException.class, null, () -> a.reset(COUNT));
    }

    private static void javaBatch(M3WordRows rows, Path notALibrary) {
        check(!M3WordRowsNative.available(), "no JNI library loaded by default");
        refused(NullPointerException.class, "library", () -> M3WordRowsNative.load(null));
        refused(UnsatisfiedLinkError.class, null, () -> M3WordRowsNative.load(notALibrary));
        check(!M3WordRowsNative.available(), "failed load does not enable the JNI path");
        refused(NullPointerException.class, "rows", () -> M3WordRowsNative.flagsBatch(null, new int[0]));
        refused(NullPointerException.class, "localIds", () -> M3WordRowsNative.flagsBatch(rows, null));
        check(M3WordRowsNative.flagsBatch(rows, new int[0]).length == 0, "empty batch");
        check(Arrays.equals(new int[] {15, 5, 10, 5}, M3WordRowsNative.flagsBatch(rows, new int[] {2, 0, 1, 0})),
                "Java batch flags in request order");
        refused(IndexOutOfBoundsException.class, null,
                () -> M3WordRowsNative.flagsBatch(rows, new int[] {0, COUNT}));
    }

    private static void deleteAll(Path dir) throws Exception {
        for (int attempt = 0; ; attempt++) {
            List<Path> remaining = new ArrayList<>();
            for (Path file : written) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException mappedOnWindows) {
                    remaining.add(file);
                }
            }
            written.clear();
            written.addAll(remaining);
            if (remaining.isEmpty()) break;
            if (attempt == 100) throw new IOException("mapped fixtures were not released: " + remaining);
            System.gc();
            Thread.sleep(20);
        }
        Files.delete(dir);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-word-row-refusal-");
        try {
            Path valid = write(dir, "valid.m3rows", fixture().array());
            M3WordRows rows = M3WordRows.open(valid);
            M3WordRows foreign = M3WordRows.open(valid);
            accessors(rows);
            generation(rows);
            cursors(rows, foreign);
            javaBatch(rows, valid);
            header(dir);
            oversized(dir);
            rows(dir);
        } finally {
            deleteAll(dir);
        }
        System.out.println("M3_WORDROWS_IMAGE_REFUSAL_PASS checks=" + checks);
    }
}
