/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3WordRows;
import com.m3.text.M3WordRowsNative;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Independent, deterministic format-3 direct-address/JNI contract proof. */
public final class M3WordRowsContractTest {
    private static long checks;
    private static final int COUNT = 3;
    private static final int ROWS_BASE = 128;
    private static final int MEMBERS = 512;
    private static final int CONCEPTS = 576;
    private static final int SUBJECTS = 640;
    private static final int EXPANSIONS = 704;
    private static final int END = 728;
    private static final int LANGUAGE = 42;
    private static final long FINGERPRINT = 0x1122334455667788L;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void fails(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected exception " + type.getName());
        } catch (Throwable found) {
            check(type.isInstance(found), "wrong exception: " + found);
        }
    }

    private static byte[] fixture() {
        ByteBuffer image = ByteBuffer.allocate(END).order(ByteOrder.BIG_ENDIAN);
        image.putLong(0, M3WordRows.MAGIC);
        image.putInt(8, M3WordRows.FORMAT);
        image.putInt(12, LANGUAGE);
        image.putInt(16, COUNT);
        image.putInt(20, M3WordRows.ROW_BYTES);
        image.putLong(24, FINGERPRINT);
        image.putLong(32, 0x8877665544332211L);
        image.putLong(40, 60L);
        image.putLong(48, 3L);
        image.putInt(56, 2);
        image.putInt(60, 2);
        image.putInt(64, ROWS_BASE);
        image.putInt(68, MEMBERS);
        image.putInt(72, COUNT);
        image.putInt(76, CONCEPTS);
        image.putInt(80, COUNT);
        image.putInt(84, SUBJECTS);
        image.putInt(88, COUNT);
        image.putInt(92, EXPANSIONS);
        image.putInt(96, COUNT);
        image.putInt(100, END);
        image.put(104, (byte) 'e');
        image.put(105, (byte) 'n');
        image.put(106, (byte) 'v');
        image.put(107, (byte) '3');
        String[] values = {"a", "ab", "b"};
        int[] reverseRanks = {0, 2, 1};
        int[] prefixEnds = {2, 2, 3};
        int[] suffixEnds = {1, 3, 3};
        for (int i = 0; i < COUNT; i++) {
            int p = ROWS_BASE + (i << 7);
            image.putInt(p, i * 5 + 5);
            image.putInt(p + 4, values[i].length());
            image.putInt(p + 8, values[i].length());
            image.putInt(p + 12, values[i].length());
            image.putInt(p + 16, values[i].hashCode());
            image.putInt(p + 20, 0x80 + i);
            image.putLong(p + 24, 0x100000000L + i);
            image.putLong(p + 32, 10L * (i + 1));
            image.putInt(p + 40, i + 1);
            image.putInt(p + 44, i);
            image.putInt(p + 48, reverseRanks[i]);
            image.putInt(p + 52, COUNT - i - 1);
            image.putInt(p + 56, prefixEnds[i]);
            image.putInt(p + 60, suffixEnds[i]);
            image.putInt(p + 72, MEMBERS + i * Integer.BYTES);
            image.putInt(p + 76, 1);
            image.putInt(p + 80, CONCEPTS + i * Long.BYTES);
            image.putInt(p + 84, 1);
            image.putInt(p + 88, SUBJECTS + i * Long.BYTES);
            image.putInt(p + 92, 1);
            image.putInt(p + 96, EXPANSIONS + i * Long.BYTES);
            image.putInt(p + 100, 1);
            image.putInt(p + 116, values[i].length() == 1 ? 31 : 961);
            image.putInt(MEMBERS + i * 4, 10 + i);
            image.putLong(CONCEPTS + i * 8, 100 + i);
            image.putLong(SUBJECTS + i * 8, 200 + i);
            image.putLong(EXPANSIONS + i * 8, 300 + i);
        }
        return image.array();
    }

    private static void bad(Path file, byte[] good, int offset, int value) throws Exception {
        byte[] broken = good.clone();
        ByteBuffer.wrap(broken).putInt(offset, value);
        Files.write(file, broken);
        try {
            M3WordRows.open(file);
            throw new AssertionError("expected corrupt image refusal at byte " + offset);
        } catch (java.io.IOException expected) {
            checks++;
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-word-row-proof-");
        Path file = dir.resolve("fixture.m3rows");
        try {
            byte[] source = fixture();
            Files.write(file, source);
            M3WordRows rows = M3WordRows.open(file);
            rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "en", "v3");
            check(rows.hotCoreBytes() == 64, "hot-core bytes");
            check(rows.mappedImageBytes() == END, "single mapped image");
            check(rows.totalCorpusTokens() == 60, "corpus total");
            check(rows.totalDocuments() == 3, "document total");
            for (int i = 0; i < COUNT; i++) {
                check(rows.rowByteOffset(i) == ROWS_BASE + i * 128, "row pointer arithmetic");
                check(rows.wordId(i) == ((long) LANGUAGE << 48) + i, "packed word ID");
                check(rows.localId(rows.wordId(i)) == i, "local id roundtrip");
                check(rows.flags(i) == i * 5 + 5, "flags");
                check(rows.utf16Length(i) == (i == 1 ? 2 : 1), "utf16");
                check(rows.codePointCount(i) == rows.utf16Length(i), "code point count");
                check(rows.utf8Length(i) == rows.utf16Length(i), "utf8");
                check(rows.corpusCount(i) == 10L * (i + 1), "corpus count");
                check(rows.documentFrequency(i) == i + 1, "document frequency");
                check(rows.membershipCount(i) == 1 && rows.membershipAt(i, 0) == i + 10,
                        "membership CSR");
                check(rows.conceptCount(i) == 1 && rows.conceptAt(i, 0) == i + 100,
                        "concept CSR");
                check(rows.subjectCount(i) == 1 && rows.subjectAt(i, 0) == i + 200,
                        "subject CSR");
                check(rows.expansionCount(i) == 1 && rows.expansionWordIdAt(i, 0) == i + 300,
                        "expansion CSR");
                check(rows.frequencyRank(i) == COUNT - i - 1, "frequency rank");
            }
            check(rows.isPrefixOf(0, 1), "a prefix of ab");
            check(!rows.isPrefixOf(1, 0), "ab not prefix of a");
            check(rows.isSuffixOf(2, 1), "b suffix of ab");
            check(!rows.isSuffixOf(0, 1), "a not suffix of ab");
            M3WordRows.Cursor a = rows.cursor().reset(0);
            M3WordRows.Cursor b = rows.cursor().reset(1);
            check(a.isPrefixOf(b) && !a.isSuffixOf(b) && !a.sameWord(b), "cursor relations");
            check(a.corpusCount() == 10L && b.flags() == 10, "cursor hot facts");
            check(a.sameWord(rows.cursor().reset(0)), "cursor word equality");
            fails(IllegalArgumentException.class, () -> rows.localId(rows.wordId(0) | M3WordRows.OVERLAY_BIT));
            fails(IllegalArgumentException.class, () -> rows.localId((43L << 48)));
            fails(IllegalArgumentException.class, () -> rows.localId((42L << 48) | 1000L));
            fails(IllegalArgumentException.class, () -> rows.requireGeneration(43, COUNT, FINGERPRINT, "en", "v3"));
            fails(IndexOutOfBoundsException.class, () -> rows.rowByteOffset(-1));
            fails(IndexOutOfBoundsException.class, () -> rows.rowByteOffset(COUNT));
            fails(IndexOutOfBoundsException.class, () -> rows.membershipAt(0, 1));
            M3WordRows foreign = M3WordRows.open(file);
            fails(IllegalArgumentException.class, () -> a.sameWord(foreign.cursor().reset(0)));
            int[] ids = new int[10000];
            Random rnd = new Random(0x4d33524f5753L);
            for (int i = 0; i < ids.length; i++) ids[i] = rnd.nextInt(COUNT);
            int[] expected = new int[ids.length];
            for (int i = 0; i < ids.length; i++) expected[i] = rows.flags(ids[i]);
            check(Arrays.equals(expected, M3WordRowsNative.flagsBatch(rows, ids)), "Java batch parity");
            if (args.length != 0) {
                M3WordRowsNative.load(Path.of(args[0]));
                check(M3WordRowsNative.available(), "native library loaded");
                check(Arrays.equals(expected, M3WordRowsNative.flagsBatch(rows, ids)), "JNI full-batch parity");
                check(Arrays.equals(new int[0], M3WordRowsNative.flagsBatch(rows, new int[0])), "JNI empty");
            }
            fails(IndexOutOfBoundsException.class, () -> M3WordRowsNative.flagsBatch(rows, new int[] {1, -1}));
            bad(file, source, 8, 2); // unsupported historical row image
            bad(file, source, 20, 64); // wrong row stride
            bad(file, source, 64, ROWS_BASE + 64); // corrupt section geometry
            bad(file, source, ROWS_BASE + 76, 10000); // row relation overflow
            bad(file, source, ROWS_BASE + 56, 0); // invalid prefix range
            bad(file, source, 12, -1); // invalid language
            System.out.println("M3_WORDROWS_PASS checks=" + checks
                    + " JNI=" + M3WordRowsNative.available()
                    + " imageFormat=3 scope=word-id-arithmetic-no-string-replacement");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }
}
