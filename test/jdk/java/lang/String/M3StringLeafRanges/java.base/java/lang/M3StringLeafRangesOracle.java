/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;

import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.ADDRESS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.BIG_ENDIAN;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.DESCRIPTOR;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_HEAD_UNITS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_MAX_UNITS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_TAIL_SHIFT;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.NOT_M3;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.STRIDE;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.UNITS_MASK;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.WIDTH_MASK;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.WIDTH_SHIFT;

/**
 * Differential oracle for {@code JavaLangAccess.stringM3LeafRanges}.
 *
 * <p>Modes: {@code component} builds M3Strings through the package-private owner factories and
 * the pool, and runs on a stock JDK 21 with {@code --patch-module java.base}; {@code m3} adds the
 * JavaLangAccess path over admitted Strings (M3 image, {@code -XX:+UseM3StringStorage});
 * {@code stock} adds the JavaLangAccess path where every String must be NOT_M3 (M3 image without
 * M3 storage). Every returned leaf is decoded independently of the producer, exactly as the
 * native kernel will (bytes at the address, or inline lane bytes), and compared with
 * {@code charAt} at every index. Lane mutants (start + 1 unit, flipped byte order) must fail.</p>
 */
public final class M3StringLeafRangesOracle {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final long SENTINEL = 0x5a5a_5a5a_5a5a_5a5aL;
    /** Descriptor bits that no leaf may set: 42..47 always, 48..63 unless INLINE. */
    private static final long RESERVED = 0x0000_fc00_0000_0000L;
    private static final long TAIL = 0xffff_0000_0000_0000L;
    private static final List<Long> BLOCKS = new ArrayList<>();
    private static final Object MAPPING = new Object();

    private static long checks;
    private static long leavesSeen;
    private static int atomLeaves;
    private static int inlineLeaves;
    private static int startMutants;
    private static int startKills;
    private static int orderMutants;
    private static int orderKills;
    private static long nextId = 1L << 50;

    private M3StringLeafRangesOracle() {}

    public static void main(String[] args) {
        String mode = args.length == 0 ? "component" : args[0];
        try {
            component();
            switch (mode) {
                case "component" -> { }
                case "m3" -> jla(true);
                case "stock" -> jla(false);
                default -> throw new IllegalArgumentException("mode " + mode);
            }
        } finally {
            BLOCKS.forEach(UNSAFE::freeMemory);
            BLOCKS.clear();
        }
        require(startKills == startMutants && startMutants > 0, "start mutants survived");
        require(orderKills == orderMutants && orderMutants > 0, "byte-order mutants survived");
        System.out.println("M3_LEAF_RANGES_PASS mode=" + mode + " stringLayout=" + stringLayout()
                + " checks=" + checks
                + " leaves=" + leavesSeen + " atomLeaves=" + atomLeaves
                + " inlineLeaves=" + inlineLeaves
                + " startMutantsKilled=" + startKills + "/" + startMutants
                + " byteOrderMutantsKilled=" + orderKills + "/" + orderMutants);
    }

    // ---------------------------------------------------------------- component cases

    private static void component() {
        Random random = new Random(0x1b_2026_1029L);
        notM3AndNulls();
        check(M3String.empty(), 0, "empty");

        char[] latin = latin1Units(random, 37);
        char[] wide = utf16Units(random, 29);
        M3String latinAtom = M3String.whole(M3StringAtom.localChars(
                latin, 0, latin.length, String.LATIN1, id(), id()));
        M3String wideAtom = M3String.whole(M3StringAtom.localChars(
                wide, 0, wide.length, String.UTF16, id(), id()));
        check(latinAtom, 1, "latin1-local-atom");
        check(wideAtom, 1, "utf16-local-atom-big-endian");

        char[] little = utf16Units(random, 23);
        M3String littleAtom = mapped(little, 2, false, 0);
        M3String oddAtom = mapped(utf16Units(random, 19), 2, true, 1);
        M3String latinMappedBe = mapped(latin1Units(random, 11), 1, true, 3);
        M3String latinMappedLe = mapped(latin1Units(random, 13), 1, false, 0);
        check(littleAtom, 1, "utf16-mapped-little-endian");
        check(oddAtom, 1, "utf16-mapped-big-endian-unaligned");
        check(latinMappedBe, 1, "latin1-mapped-flag-be");
        check(latinMappedLe, 1, "latin1-mapped-flag-le");

        M3String shortLatin = immediate("a" + (char) 0xe9 + "Z9~");
        M3String ident = immediate("abcDEF_$09");
        check(shortLatin, 1, "immediate-latin1");
        check(ident, 1, "immediate-ident-10");
        check(ident.slice(3, 9), 1, "immediate-ident-slice");
        check(ident.slice(8, 10), 1, "immediate-ident-tail-only");

        M3String pair = tuple(latinAtom, wideAtom);
        check(pair, 2, "tuple-latin1-utf16");
        check(tuple(latinAtom.slice(0, 5), latinAtom.slice(5, 12)), 1, "tuple-coalesced");
        check(tuple(latinAtom.slice(5, 9), latinAtom.slice(0, 5)), 2, "tuple-same-owner-gap");

        M3String[] parts = {latinAtom, ident, littleAtom, shortLatin, oddAtom, wideAtom};
        M3String composite = tuple(tuple(tuple(parts[0], parts[1]), parts[2]),
                tuple(parts[3], tuple(parts[4], parts[5])));
        long[] whole = check(composite, parts.length, "composite-6");
        capacity(composite, whole, parts.length);
        slices(composite, parts);
        pool(random);
        Reference.reachabilityFence(MAPPING);
    }

    private static void notM3AndNulls() {
        long[] lanes = new long[4];
        Arrays.fill(lanes, SENTINEL);
        require(M3StringLeafRanges.fill(null, lanes) == NOT_M3, "null storage is NOT_M3");
        require(untouched(lanes, 0), "NOT_M3 touched lanes");
        expectNpe(() -> M3StringLeafRanges.fill(null, null), "null lanes, null storage");
        expectNpe(() -> M3StringLeafRanges.fill(M3String.empty(), null), "null lanes");
        checks++;
    }

    /** A stock-layout String has no m3 field; its JavaLangAccess result is NOT_M3. */
    private static String stringLayout() {
        try {
            String.class.getDeclaredField("m3");
            return "m3";
        } catch (NoSuchFieldException stock) {
            return "stock";
        }
    }

    private static void capacity(M3String text, long[] full, int leaves) {
        for (int slots = 0; slots <= leaves + 1; slots++) {
            long[] lanes = new long[slots * STRIDE + 1];
            Arrays.fill(lanes, SENTINEL);
            require(M3StringLeafRanges.fill(text, lanes) == leaves, "capacity count " + slots);
            int written = Math.min(slots, leaves) * STRIDE;
            for (int at = 0; at < lanes.length; at++) {
                boolean same = at < written ? lanes[at] == full[at] : lanes[at] == SENTINEL;
                require(same, "capacity " + slots + " lane " + at);
            }
            checks++;
        }
    }

    /** Every slice [b, e) of the composite: expected leaf count from the known leaf extents. */
    private static void slices(M3String composite, M3String[] parts) {
        int[] ends = new int[parts.length];
        for (int i = 0, sum = 0; i < parts.length; i++) ends[i] = sum += parts[i].length();
        int length = composite.length();
        for (int begin = 0; begin < length; begin++) {
            for (int end = begin + 1; end <= length; end++) {
                int expected = 0;
                for (int i = 0, from = 0; i < parts.length; from = ends[i++]) {
                    if (from < end && begin < ends[i]) expected++;
                }
                check(composite.slice(begin, end), expected, "slice " + begin + ".." + end);
            }
        }
    }

    /** Pool admission and balanced concatenation: immediates, atoms and tuples as produced. */
    private static void pool(Random random) {
        for (int trial = 0; trial < 300; trial++) {
            M3String text = M3String.empty();
            int pieces = 1 + random.nextInt(12);
            for (int piece = 0; piece < pieces; piece++) {
                int size = 1 + random.nextInt(random.nextBoolean() ? 9 : 48);
                char[] units = random.nextInt(3) == 0
                        ? utf16Units(random, size) : latin1Units(random, size);
                text = M3StringPool.concat(text, M3StringPool.internChars(units, 0, size));
            }
            check(text, -1, "pool-" + trial);
            int begin = random.nextInt(text.length());
            int end = begin + 1 + random.nextInt(text.length() - begin);
            check(text.slice(begin, end), -1, "pool-slice-" + trial);
        }
    }

    // ---------------------------------------------------------------- JavaLangAccess path

    private static void jla(boolean expectM3) {
        JavaLangAccess access = SharedSecrets.getJavaLangAccess();
        expectNpe(() -> access.stringM3LeafRanges(null, new long[0]), "jla null text");
        expectNpe(() -> access.stringM3LeafRanges("x", null), "jla null lanes");
        Random random = new Random(0x2b_2026_1029L);
        for (int trial = 0; trial < 200; trial++) {
            String text = "";
            int pieces = 1 + random.nextInt(10);
            for (int piece = 0; piece < pieces; piece++) {
                int size = 1 + random.nextInt(40);
                text = text.concat(new String(random.nextInt(3) == 0
                        ? utf16Units(random, size) : latin1Units(random, size)));
            }
            long[] lanes = new long[2];
            Arrays.fill(lanes, SENTINEL);
            int leaves = access.stringM3LeafRanges(text, lanes);
            if (!expectM3) {
                require(leaves == NOT_M3 && untouched(lanes, 0), "stock String is NOT_M3");
                checks++;
                continue;
            }
            M3String storage = text.m3();
            require(storage != null && leaves >= 1, "M3 admission " + trial);
            lanes = new long[leaves * STRIDE];
            require(access.stringM3LeafRanges(text, lanes) == leaves, "jla count");
            long[] direct = new long[leaves * STRIDE];
            require(M3StringLeafRanges.fill(storage, direct) == leaves, "direct count");
            require(Arrays.equals(lanes, direct), "jla lanes equal direct lanes");
            require(firstMismatch(text, lanes, leaves) < 0, "jla decode " + trial);
            checks += text.length();
            Reference.reachabilityFence(text);
        }
    }

    // ---------------------------------------------------------------- check and decode

    private static long[] check(M3String text, int expectedLeaves, String label) {
        int leaves = M3StringLeafRanges.fill(text, new long[0]);
        require(leaves >= 0, label + " NOT_M3 for M3 storage");
        if (expectedLeaves >= 0) {
            require(leaves == expectedLeaves, label + " leaves " + leaves + " != " + expectedLeaves);
        }
        long[] lanes = new long[leaves * STRIDE + STRIDE];
        Arrays.fill(lanes, SENTINEL);
        require(M3StringLeafRanges.fill(text, lanes) == leaves, label + " count replay");
        require(untouched(lanes, leaves * STRIDE), label + " wrote past the leaf count");
        descriptors(lanes, leaves, label);
        int mismatch = firstMismatch(text, lanes, leaves);
        require(mismatch < 0, label + " unit mismatch at " + mismatch);
        mutants(text, lanes, leaves);
        Reference.reachabilityFence(text);
        checks += text.length();
        leavesSeen += leaves;
        return Arrays.copyOf(lanes, leaves * STRIDE);
    }

    private static void descriptors(long[] lanes, int leaves, String label) {
        for (int leaf = 0; leaf < leaves; leaf++) {
            long address = lanes[leaf * STRIDE + ADDRESS];
            long descriptor = lanes[leaf * STRIDE + DESCRIPTOR];
            int units = (int) (descriptor & UNITS_MASK);
            int width = (int) ((descriptor >>> WIDTH_SHIFT) & WIDTH_MASK);
            boolean inline = (descriptor & INLINE) != 0;
            require(units >= 1 && (width == 1 || width == 2), label + " geometry");
            require((descriptor & RESERVED) == 0, label + " reserved bits");
            require((descriptor & BIG_ENDIAN) == 0 || (width == 2 && !inline), label + " byte order");
            if (inline) {
                inlineLeaves++;
                require(width == 1 && units <= INLINE_MAX_UNITS, label + " inline geometry");
                int head = Math.min(units, INLINE_HEAD_UNITS);
                require(head == INLINE_HEAD_UNITS || address >>> (head * Byte.SIZE) == 0,
                        label + " inline head padding");
                int tailUnits = units - head;
                long used = tailUnits == 0 ? 0L : (-1L >>> (Long.SIZE - tailUnits * Byte.SIZE))
                        << INLINE_TAIL_SHIFT;
                require((descriptor & TAIL & ~used) == 0, label + " inline tail padding");
            } else {
                atomLeaves++;
                require(address != 0L && (descriptor & TAIL) == 0, label + " atom fields");
            }
        }
    }

    /** Independent decode; returns the first mismatching logical index, or -1. */
    private static int firstMismatch(CharSequence text, long[] lanes, int leaves) {
        int position = 0;
        for (int leaf = 0; leaf < leaves; leaf++) {
            long address = lanes[leaf * STRIDE + ADDRESS];
            long descriptor = lanes[leaf * STRIDE + DESCRIPTOR];
            int units = (int) (descriptor & UNITS_MASK);
            for (int k = 0; k < units; k++, position++) {
                if (position >= text.length() || unit(address, descriptor, k) != text.charAt(position)) {
                    return position;
                }
            }
        }
        return position == text.length() ? -1 : position;
    }

    private static char unit(long address, long descriptor, int k) {
        if ((descriptor & INLINE) != 0) {
            long bits = k < INLINE_HEAD_UNITS ? address >>> (k * Byte.SIZE)
                    : descriptor >>> (INLINE_TAIL_SHIFT + (k - INLINE_HEAD_UNITS) * Byte.SIZE);
            return (char) (bits & 0xff);
        }
        if (((descriptor >>> WIDTH_SHIFT) & WIDTH_MASK) == 1) {
            return (char) (UNSAFE.getByte(address + k) & 0xff);
        }
        int first = UNSAFE.getByte(address + 2L * k) & 0xff;
        int second = UNSAFE.getByte(address + 2L * k + 1) & 0xff;
        return (descriptor & BIG_ENDIAN) != 0
                ? (char) (first << 8 | second) : (char) (second << 8 | first);
    }

    /**
     * Off-by-one start and flipped byte order of each atom leaf must change some unit. The start
     * mutant needs two units: unit 1 differs from unit 0 by construction and stays in the leaf.
     */
    private static void mutants(M3String text, long[] lanes, int leaves) {
        for (int leaf = 0; leaf < leaves; leaf++) {
            long descriptor = lanes[leaf * STRIDE + DESCRIPTOR];
            if ((descriptor & INLINE) != 0) continue;
            long width = (descriptor >>> WIDTH_SHIFT) & WIDTH_MASK;
            if ((descriptor & UNITS_MASK) >= 2) {
                long[] shifted = lanes.clone();
                shifted[leaf * STRIDE + ADDRESS] += width;
                startMutants++;
                if (firstMismatch(text, shifted, leaves) >= 0) startKills++;
            }
            if (width == 2) {
                long[] swapped = lanes.clone();
                swapped[leaf * STRIDE + DESCRIPTOR] ^= BIG_ENDIAN;
                orderMutants++;
                if (firstMismatch(text, swapped, leaves) >= 0) orderKills++;
            }
        }
    }

    // ---------------------------------------------------------------- builders

    /** Latin-1 units with no two equal neighbours, so a shifted start always differs. */
    private static char[] latin1Units(Random random, int count) {
        char[] units = new char[count];
        for (int i = 0; i < count; i++) {
            do {
                units[i] = (char) (0x21 + random.nextInt(0xff - 0x21 + 1));
            } while (i > 0 && units[i] == units[i - 1]);
        }
        return units;
    }

    /** UTF-16 units above Latin-1 whose two bytes differ, so a swapped order always differs. */
    private static char[] utf16Units(Random random, int count) {
        char[] units = new char[count];
        for (int i = 0; i < count; i++) {
            char unit;
            do {
                unit = (char) (0x100 + random.nextInt(0xfff0 - 0x100));
            } while ((unit >>> 8) == (unit & 0xff) || (i > 0 && unit == units[i - 1]));
            units[i] = unit;
        }
        return units;
    }

    private static M3String mapped(char[] units, int width, boolean bigEndian, int misalign) {
        long block = UNSAFE.allocateMemory((long) units.length * width + misalign + 16);
        BLOCKS.add(block);
        long base = block + misalign;
        int hash = 0;
        for (int i = 0; i < units.length; i++) {
            char unit = units[i];
            hash = 31 * hash + unit;
            if (width == 1) {
                UNSAFE.putByte(base + i, (byte) unit);
            } else {
                UNSAFE.putByte(base + 2L * i, (byte) (bigEndian ? unit >>> 8 : unit));
                UNSAFE.putByte(base + 2L * i + 1, (byte) (bigEndian ? unit : unit >>> 8));
            }
        }
        byte coder = width == 1 ? String.LATIN1 : String.UTF16;
        return M3String.whole(M3StringAtom.mapped(MAPPING, base, (byte) width, bigEndian,
                units.length, coder, hash, id(), id()));
    }

    private static M3String immediate(String text) {
        char[] units = text.toCharArray();
        long packed = M3StringImmediate.packChars(units, 0, units.length);
        require(packed != M3StringImmediate.NONE, "immediate profile for " + text);
        return M3String.whole(M3StringImmediate.of(packed));
    }

    private static M3String tuple(M3String left, M3String right) {
        return M3String.whole(new M3StringTuple(left, right, id(), id()));
    }

    private static long id() {
        return nextId++;
    }

    private static boolean untouched(long[] lanes, int from) {
        for (int at = from; at < lanes.length; at++) {
            if (lanes[at] != SENTINEL) return false;
        }
        return true;
    }

    private static void expectNpe(Runnable action, String label) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }
        throw new AssertionError(label + ": expected NullPointerException");
    }

    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
