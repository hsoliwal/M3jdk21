/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * GVAS literal-image admission and lookup through the shipped exact-address JNI mapper.
 *
 * <p>Images are produced by an independent test-side writer of the documented M3GVAS01 layout
 * (64-byte header, 32-byte rows, NUL code unit, UTF-16LE payload). Every corrupt image differs from
 * a valid one in one field and must be refused with the specific reason. Valid images prove each
 * precomputed fact, the mapped payload address, the optional primitive hash directory (bounded
 * collisions, probe exhaustion, overflow fallback) and the binary-search path of rows outside the
 * directory budget.
 *
 * <p>Usage: {@code <mapper-library> <fixture-dir>}. Mappings are process-lifetime, so fixtures stay
 * in the build directory.
 */
public final class M3GvasLiteralImageJniContractTest {
    private static final long STRIDE = 0x10_0000L;
    private static final long HUGE_BASE = M3FixedAddressMapping.DEFAULT_BASE + 0x1000_0000L;
    private static final int HEADER = 64;
    private static final int ROW = 32;
    private static final String GEOMETRY = "M3-GVAS literal geometry";
    private static final String PAYLOAD = "M3-GVAS overlapping/out-of-range payload";
    private static final String FACTS = "M3-GVAS literal precompute fact mismatch";
    private static long checks;
    private static int nextBase;
    private static Path library;
    private static Path dir;

    private M3GvasLiteralImageJniContractTest() {}

    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(
                    type.isInstance(thrown)
                            && (reason == null || reason.equals(thrown.getMessage())),
                    "wrong refusal for <" + reason + ">: " + thrown);
            return;
        }
        throw new AssertionError("accepted: " + reason);
    }

    private static int power31(int exponent) {
        int power = 1;
        for (int i = 0; i < exponent; i++) power *= 31;
        return power;
    }

    private static int flags(String text) {
        int flags = 0;
        if (text.chars().allMatch(c -> c <= 0x7f)) flags |= M3GvasLiteralImage.FLAG_ASCII;
        if (text.chars().allMatch(c -> c <= 0xff)) flags |= M3GvasLiteralImage.FLAG_LATIN1;
        if (text.chars().anyMatch(c -> Character.isSurrogate((char) c)))
            flags |= M3GvasLiteralImage.FLAG_SURROGATE;
        return flags;
    }

    /** Writes the rows in the given order; admission, not the writer, enforces sorting. */
    private static ByteBuffer write(List<String> rows) {
        int offset = HEADER + rows.size() * ROW;
        long units = rows.stream().mapToLong(String::length).sum();
        int capacity = Math.toIntExact(offset + 2 + 2 * units);
        ByteBuffer b = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        b.put(0, "M3GVAS01".getBytes(StandardCharsets.US_ASCII));
        b.putInt(8, 1)
                .putInt(12, HEADER)
                .putInt(16, rows.size())
                .putInt(20, ROW)
                .putInt(24, HEADER)
                .putInt(28, offset)
                .putInt(32, capacity);
        int expected = offset + 2;
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            int length = text.length();
            int at = HEADER + row * ROW;
            int begin = length == 0 ? offset : expected;
            b.putInt(at, begin)
                    .putInt(at + 4, length)
                    .putInt(at + 8, text.hashCode())
                    .putInt(at + 12, power31(length))
                    .putInt(at + 16, text.codePointCount(0, length))
                    .putInt(at + 20, flags(text))
                    .putInt(at + 24, length == 0 ? -1 : text.charAt(0))
                    .putInt(at + 28, length == 0 ? -1 : text.charAt(length - 1));
            for (int i = 0; i < length; i++) b.putChar(begin + 2 * i, text.charAt(i));
            expected += 2 * length;
        }
        return b;
    }

    private static M3FixedAddressMapping.Image map(String name, ByteBuffer image, long base)
            throws IOException, NoSuchAlgorithmException {
        byte[] bytes = image.array();
        Path file = Files.write(dir.resolve(name + ".m3gvas"), bytes);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return M3FixedAddressMapping.open(file, base, sha, library);
    }

    private static M3FixedAddressMapping.Image map(String name, ByteBuffer image) throws Exception {
        return map(name, image, M3FixedAddressMapping.DEFAULT_BASE + STRIDE * nextBase++);
    }

    private static void corrupt(String name, String reason, ByteBuffer image) throws Exception {
        M3FixedAddressMapping.Image mapped = map(name, image);
        refused(IllegalArgumentException.class, reason, () -> M3GvasLiteralImage.open(mapped));
    }

    private static void corrupt(String name, String reason, Consumer<ByteBuffer> edit)
            throws Exception {
        ByteBuffer image = write(List.of("", "a", "ab", "bé"));
        edit.accept(image);
        corrupt(name, reason, image);
    }

    private static void refusals() throws Exception {
        refused(NullPointerException.class, "image", () -> M3GvasLiteralImage.open(null));
        ByteBuffer truncated =
                ByteBuffer.wrap(Arrays.copyOf(write(List.of("a")).array(), HEADER + 1));
        corrupt("truncated", "truncated M3-GVAS literal header", truncated);
        corrupt("magic", "M3-GVAS literal magic", b -> b.put(3, (byte) 'X'));
        corrupt("version", GEOMETRY, b -> b.putInt(8, 2));
        corrupt("header-bytes", GEOMETRY, b -> b.putInt(12, HEADER + 1));
        corrupt("negative-count", GEOMETRY, b -> b.putInt(16, -1));
        corrupt("row-bytes", GEOMETRY, b -> b.putInt(20, 16));
        corrupt("rows-offset", GEOMETRY, b -> b.putInt(24, 0));
        corrupt("payload-before-header", GEOMETRY, b -> b.putInt(28, 32));
        corrupt("capacity-field", GEOMETRY, b -> b.putInt(32, b.capacity() + 1));
        corrupt("unaligned-payload", GEOMETRY, b -> b.putInt(28, b.getInt(28) + 16));
        corrupt("payload-sentinel", GEOMETRY, b -> b.putChar(b.getInt(28), 'x'));
        ByteBuffer noSentinelRoom =
                ByteBuffer.allocate(HEADER + ROW + 1).order(ByteOrder.LITTLE_ENDIAN);
        noSentinelRoom.put(0, write(List.of()).array(), 0, HEADER);
        noSentinelRoom.putInt(16, 1).putInt(28, HEADER + ROW).putInt(32, HEADER + ROW + 1);
        corrupt("no-sentinel-room", GEOMETRY, noSentinelRoom);
        ByteBuffer overflow = ByteBuffer.allocate(2 * HEADER).order(ByteOrder.LITTLE_ENDIAN);
        overflow.put(0, write(List.of()).array(), 0, HEADER);
        overflow.putInt(16, 1 << 26).putInt(28, HEADER).putInt(32, 2 * HEADER);
        corrupt("row-directory-overflow", "M3-GVAS capacity overflow", overflow);
        corrupt("header-flags", "M3-GVAS unexpected header flags", b -> b.put(40, (byte) 1));
        int row1 = HEADER + ROW;
        int row2 = HEADER + 2 * ROW;
        corrupt("negative-length", PAYLOAD, b -> b.putInt(row1 + 4, -1));
        corrupt("oversized-length", PAYLOAD, b -> b.putInt(row1 + 4, 1000));
        corrupt("moved-payload", PAYLOAD, b -> b.putInt(row1, b.getInt(row1) + 2));
        corrupt("moved-empty-payload", PAYLOAD, b -> b.putInt(HEADER, b.getInt(HEADER) + 2));
        corrupt("descending", "M3-GVAS literal rows not strictly sorted", write(List.of("b", "a")));
        corrupt("duplicate", "M3-GVAS literal rows not strictly sorted", write(List.of("a", "a")));
        corrupt(
                "longer-first",
                "M3-GVAS literal rows not strictly sorted",
                write(List.of("ab", "a")));
        corrupt("hash", FACTS, b -> b.putInt(row1 + 8, b.getInt(row1 + 8) + 1));
        corrupt("power", FACTS, b -> b.putInt(row1 + 12, b.getInt(row1 + 12) + 1));
        corrupt("code-points", FACTS, b -> b.putInt(row1 + 16, 2));
        corrupt("flags", FACTS, b -> b.putInt(row1 + 20, 0));
        corrupt("first-unit", FACTS, b -> b.putInt(row1 + 24, 'b'));
        corrupt("last-unit", FACTS, b -> b.putInt(row2 + 28, 'a'));
        corrupt("empty-first-unit", FACTS, b -> b.putInt(HEADER + 24, 0));
        ByteBuffer valid = write(List.of("a"));
        ByteBuffer trailing =
                ByteBuffer.allocate(valid.capacity() + 2).order(ByteOrder.LITTLE_ENDIAN);
        trailing.put(0, valid.array()).putInt(32, valid.capacity() + 2);
        corrupt("trailing-bytes", "trailing/omitted payload bytes", trailing);
    }

    private static void facts(
            M3GvasLiteralImage image, M3FixedAddressMapping.Image mapped, List<String> rows) {
        check(image.size() == rows.size(), "row count");
        check(image.baseAddress() == mapped.address(), "mapped base");
        ByteBuffer bytes = mapped.readOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            check(image.findRow(text) == row, "exact row of " + row);
            check(image.utf16LengthAt(row) == text.length(), "length fact");
            check(image.hashCodeAt(row) == text.hashCode(), "String hash fact");
            check(image.hash31PowerAt(row) == power31(text.length()), "31^length fact");
            check(
                    image.codePointCountAt(row) == text.codePointCount(0, text.length()),
                    "code point fact");
            check(image.flagsAt(row) == flags(text), "character class fact");
            long address = image.addressOf(text);
            check(
                    address == image.addressAt(row) && address >= mapped.address(),
                    "canonical payload address");
            int relative = Math.toIntExact(address - mapped.address());
            for (int i = 0; i < text.length(); i++) {
                check(bytes.getChar(relative + 2 * i) == text.charAt(i), "mapped UTF-16 payload");
            }
        }
    }

    private static void smallImage() throws Exception {
        List<String> rows =
                List.copyOf(
                        new TreeSet<>(
                                List.of(
                                        "",
                                        "a",
                                        "ab",
                                        "abc",
                                        "bé",
                                        "café",
                                        "你好",
                                        "🚀",
                                        "\uD800",
                                        "\uDC00",
                                        "a\uDC00",
                                        "é",
                                        "\uD800\uD800",
                                        "zz")));
        M3FixedAddressMapping.Image mapped = map("small", write(rows));
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapped);
        check(!image.hashSlotIndexPrepared(), "fewer than 64 rows use binary search only");
        facts(image, mapped, rows);
        for (String miss : List.of("0", "aa", "abd", "b", "\uD83D", "￿", "zzz")) {
            check(image.findRow(miss) == -1 && image.addressOf(miss) == 0L, "miss " + miss);
        }
        refused(NullPointerException.class, "spelling", () -> image.findRow(null));
        refused(IndexOutOfBoundsException.class, null, () -> image.addressAt(rows.size()));
        refused(IndexOutOfBoundsException.class, null, () -> image.flagsAt(-1));
    }

    private static void hashedImage() throws Exception {
        TreeSet<String> unique = new TreeSet<>();
        for (int i = 0; i < 200; i++)
            unique.add("w" + Integer.toString(i * 7919, 36) + (char) ('a' + i % 26));
        List<String> rows = List.copyOf(unique);
        M3FixedAddressMapping.Image mapped = map("hashed", write(rows));
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapped);
        check(image.hashSlotIndexPrepared(), "bounded rows build the primitive directory");
        facts(image, mapped, rows);
        for (int miss = 0; miss < 4096; miss++) {
            check(image.findRow("miss-" + miss) == -1, "hash directory miss");
        }
    }

    /** Same FNV-1a spelling hash and 64-bit finalizer as the published hash directory. */
    private static int slot(String text, int mask) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
        hash ^= text.length();
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return (int) (hash ^ (hash >>> 33)) & mask;
    }

    private static List<String> colliding(String prefix, int count, int mask, int target) {
        List<String> found = new ArrayList<>();
        for (int i = 0; found.size() < count; i++) {
            String candidate = prefix + String.format("%07d", i);
            if (slot(candidate, mask) == target) found.add(candidate);
        }
        return found;
    }

    private static void collisionImages() throws Exception {
        // 64 rows in 128 slots, all in one probe run: the 64th row needs 63 probes.
        int target = slot("c0000000", 127);
        List<String> cluster = colliding("c", 66, 127, target);
        List<String> rows = List.copyOf(new TreeSet<>(cluster.subList(0, 64)));
        M3FixedAddressMapping.Image mapped = map("cluster-64", write(rows));
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapped);
        check(image.hashSlotIndexPrepared(), "a 63-probe cluster is still admitted");
        facts(image, mapped, rows);
        // A same-length absent spelling in that slot exhausts 64 probes and falls back to search.
        check(
                image.findRow(cluster.get(64)) == -1 && image.findRow(cluster.get(65)) == -1,
                "probe exhaustion falls back to exact binary search");
        String longer = colliding("dd", 1, 127, target).get(0);
        check(image.findRow(longer) == -1, "length mismatch in the probe run");
        // 65 rows colliding in 256 slots exceed the 64-probe budget: no directory is built.
        int wide = slot("e0000000", 255);
        List<String> overflow = List.copyOf(new TreeSet<>(colliding("e", 65, 255, wide)));
        M3FixedAddressMapping.Image overflowMapped = map("cluster-65", write(overflow));
        M3GvasLiteralImage unindexed = M3GvasLiteralImage.open(overflowMapped);
        check(!unindexed.hashSlotIndexPrepared(), "probe budget overflow keeps binary search");
        facts(unindexed, overflowMapped, overflow);
    }

    private static void hugeImage() throws Exception {
        int rows = (1 << 20) + 1;
        int offset = HEADER + rows * ROW;
        int capacity = offset + 2 + rows * 4;
        ByteBuffer b = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        b.put(0, "M3GVAS01".getBytes(StandardCharsets.US_ASCII));
        b.putInt(8, 1)
                .putInt(12, HEADER)
                .putInt(16, rows)
                .putInt(20, ROW)
                .putInt(24, HEADER)
                .putInt(28, offset)
                .putInt(32, capacity);
        for (int row = 0; row < rows; row++) {
            char first = (char) (0x100 + (row >>> 10));
            char last = (char) (0x100 + (row & 1023));
            int at = HEADER + row * ROW;
            int begin = offset + 2 + row * 4;
            b.putInt(at, begin)
                    .putInt(at + 4, 2)
                    .putInt(at + 8, 31 * first + last)
                    .putInt(at + 12, 961)
                    .putInt(at + 16, 2)
                    .putInt(at + 20, 0)
                    .putInt(at + 24, first)
                    .putInt(at + 28, last);
            b.putChar(begin, first).putChar(begin + 2, last);
        }
        M3FixedAddressMapping.Image mapped = map("over-directory-budget", b, HUGE_BASE);
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapped);
        check(image.size() == rows, "all rows admitted");
        check(!image.hashSlotIndexPrepared(), "rows above 2^20 skip the optional directory");
        for (int row : new int[] {0, 1, 1023, 1024, rows / 2, rows - 2, rows - 1}) {
            String text =
                    new String(
                            new char[] {
                                (char) (0x100 + (row >>> 10)), (char) (0x100 + (row & 1023))
                            });
            check(
                    image.findRow(text) == row && image.addressOf(text) == image.addressAt(row),
                    "binary row " + row);
        }
        check(image.findRow("Ā") == -1 && image.findRow("؀Ā") == -1, "binary misses");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("mapper-library fixture-dir");
        library = Path.of(args[0]);
        dir = Files.createDirectories(Path.of(args[1]));
        refusals();
        smallImage();
        hashedImage();
        collisionImages();
        hugeImage();
        System.out.println(
                "M3_GVAS_LITERAL_JNI_CONTRACT_PASS checks=" + checks + " images=" + nextBase);
    }
}
