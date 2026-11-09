/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Actual M3 owner cursor preserves UTF-16 at arbitrary boundaries and access orders
 * @modules java.base/jdk.internal.misc
 * @compile --patch-module java.base=${test.src} M3StringPathCursorTest.java
 * @run main/othervm -Xmx128m --patch-module java.base=${test.classes} java.lang.M3StringPathCursorTest
 * @run main/othervm -Xmx128m -Xint --patch-module java.base=${test.classes} java.lang.M3StringPathCursorTest
 */
package java.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import jdk.internal.misc.Unsafe;

/**
 * Uses the real M3String, M3StringAtom, and M3StringTuple classes. Test-owned native
 * memory is released after all readers finish. The oracle is an independently
 * assembled char array, never M3String.charAt or another cursor implementation.
 * This test does not activate String admission or stand in for HotSpot/JNI tests.
 */
public final class M3StringPathCursorTest {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static long scenarios;

    private M3StringPathCursorTest() { }

    private record View(M3String text, char[] units) {
        View slice(int from, int to) {
            return new View(text.slice(from, to), Arrays.copyOfRange(units, from, to));
        }
    }

    private static final class NativeFixture implements AutoCloseable {
        private long[] addresses = new long[16];
        private int addressCount;
        private long nextId;
        private long nativeBytes;

        View atom(char[] units, int width, boolean bigEndian) {
            if (units.length == 0) return new View(M3String.empty(), units.clone());
            boolean latin1 = true;
            int hash = 0;
            for (char unit : units) {
                latin1 &= unit <= 0xff;
                hash = 31 * hash + unit;
            }
            if (width == 1 && !latin1) throw new IllegalArgumentException("test width");
            if (addressCount == addresses.length) {
                addresses = Arrays.copyOf(addresses, addresses.length * 2);
            }
            long bytes = Math.multiplyExact((long) units.length, width);
            long address = UNSAFE.allocateMemory(bytes);
            addresses[addressCount++] = address;
            nativeBytes += bytes;
            for (int index = 0; index < units.length; index++) {
                long at = address + (long) index * width;
                char unit = units[index];
                if (width == 1) {
                    UNSAFE.putByte(at, (byte) unit);
                } else {
                    UNSAFE.putByte(at, (byte) (bigEndian ? unit >>> 8 : unit));
                    UNSAFE.putByte(at + 1, (byte) (bigEndian ? unit : unit >>> 8));
                }
            }
            byte coder = String.COMPACT_STRINGS && latin1 ? String.LATIN1 : String.UTF16;
            long id = ++nextId;
            M3StringAtom owner = new M3StringAtom(this, address, (byte) width,
                    bigEndian, units.length, coder, hash, id, id * 0x9e3779b97f4a7c15L);
            return new View(M3String.whole(owner), units.clone());
        }

        View immediate(char[] units) {
            // Padding is deliberately outside both admitted immediate alphabets.
            // The actual packer's offset/length must exclude it.
            char[] padded = new char[units.length + 4];
            Arrays.fill(padded, '\uffff');
            System.arraycopy(units, 0, padded, 2, units.length);
            long allocatedBefore = nativeBytes;
            long packed = M3StringImmediate.packChars(padded, 2, units.length);
            require(packed != M3StringImmediate.NONE, "test immediate must be admitted");
            M3StringImmediate owner = M3StringImmediate.of(packed);
            require(nativeBytes == allocatedBefore, "immediate fixture adds no native payload");
            return new View(M3String.whole(owner), units.clone());
        }

        View join(View left, View right) {
            if (left.units.length == 0) return right;
            if (right.units.length == 0) return left;
            return rawJoin(left, right);
        }

        View rawJoin(View left, View right) {
            char[] units = Arrays.copyOf(left.units,
                    Math.addExact(left.units.length, right.units.length));
            System.arraycopy(right.units, 0, units, left.units.length, right.units.length);
            long id = ++nextId;
            M3StringTuple owner = new M3StringTuple(left.text, right.text, id,
                    id * 0xd6e8feb86659fd93L);
            return new View(M3String.whole(owner), units);
        }

        M3String joinOwners(M3String left, M3String right) {
            long id = ++nextId;
            return M3String.whole(new M3StringTuple(left, right, id,
                    id * 0xd6e8feb86659fd93L));
        }

        View balanced(List<View> leaves, int from, int to) {
            if (from == to) return new View(M3String.empty(), new char[0]);
            if (to - from == 1) return leaves.get(from);
            int middle = (from + to) >>> 1;
            return join(balanced(leaves, from, middle), balanced(leaves, middle, to));
        }

        @Override
        public void close() {
            for (int index = 0; index < addressCount; index++) {
                UNSAFE.freeMemory(addresses[index]);
                addresses[index] = 0;
            }
            addressCount = 0;
            nativeBytes = 0;
        }
    }

    public static void main(String[] arguments) {
        basicBoundaries();
        immediateLeafBoundaries();
        exhaustiveShortText();
        randomSharedSlices();
        deepTrees();
        largeCoordinates();
        System.out.println("M3_PATH_CURSOR_PASS checks=" + checks
                + " scenarios=" + scenarios + " actual_owners=true simulated_owners=false"
                + " string_admission_tested=false hotspot_jni_integration_tested=false");
    }

    private static void basicBoundaries() {
        try (NativeFixture fixture = new NativeFixture()) {
            char[][] spellings = {
                {}, {'\0'}, {'\u00ff'}, {'\ud800'}, {'\udfff'},
                {'a', '\ud83d', '\ude42', 'z'}, {'\ude42', '\ud83d'},
                {'\r', '\n'}, {'e', '\u0301'}, {'\uffff', '\u0000', '\u0100'}
            };
            for (char[] spelling : spellings) {
                View big = fixture.atom(spelling, 2, true);
                View little = fixture.atom(spelling, 2, false);
                verifyAccess(big, 1);
                verifyAccess(little, 2);
                verifyStreams(big);
                for (int from = 0; from <= spelling.length; from++) {
                    for (int to = from; to <= spelling.length; to++) {
                        verifyAccess(big.slice(from, to), from * 31L + to);
                        verifyStreams(little.slice(from, to));
                    }
                }
            }
            View left = fixture.atom(new char[] {'x', '\ud83d'}, 2, true);
            View right = fixture.atom(new char[] {'\ude42', 'y'}, 2, false);
            View middle = fixture.join(left, right);
            View prefix = fixture.atom(new char[] {'\0', '\u00ff'}, 1, true);
            View mixed = fixture.join(prefix, middle);
            verifyAccess(mixed, 3);
            verifyStreams(mixed);
            verifyStreams(mixed.slice(3, 5));
            verifyStreams(mixed.slice(3, 4));
            verifyStreams(mixed.slice(4, 5));
            View empty = new View(M3String.empty(), new char[0]);
            View emptyChildren = fixture.rawJoin(fixture.rawJoin(empty, mixed.slice(1, 5)),
                    fixture.rawJoin(middle.slice(1, 3), empty));
            verifyAccess(emptyChildren, 4);
            verifyAccess(emptyChildren.slice(1, emptyChildren.units.length - 1), 5);
            verifyStreams(emptyChildren);
        }
    }

    private static void immediateLeafBoundaries() {
        long checksBefore = checks;
        long scenariosBefore = scenarios;
        char[][] spellings = {
            {'\0'}, {'\u00ff', 'A', '\0'},
            {'a', 'B', '0', '_', '$', '\u00ff', 'z'},
            "aB09_$Zx".toCharArray(), "aB09_$Zxy".toCharArray(),
            "aB09_$Zxyz".toCharArray()
        };
        try (NativeFixture fixture = new NativeFixture()) {
            View nativeWide = fixture.atom(new char[] {'x', '\ud83d', '\ude42', '\u0100'}, 2, true);
            View nativeLittle = fixture.atom(new char[] {'\uffff', '\ud800', 'y'}, 2, false);
            View empty = new View(M3String.empty(), new char[0]);
            long seed = 0x494d4d4544494154L;
            for (char[] spelling : spellings) {
                View immediate = fixture.immediate(spelling);
                require(immediate.text.owner() instanceof M3StringImmediate, "actual immediate owner");
                verifyAccess(immediate, seed++);
                verifyStreams(immediate);
                for (int from = 0; from <= spelling.length; from++) {
                    for (int to = from; to <= spelling.length; to++) {
                        View slice = immediate.slice(from, to);
                        verifyAccess(slice, seed++);
                        verifyStreams(slice);
                        View mixed = fixture.rawJoin(
                                fixture.rawJoin(nativeWide.slice(1, 4), slice),
                                fixture.rawJoin(slice, nativeLittle.slice(0, 2)));
                        verifyAccess(mixed, seed++);
                        verifyAccess(mixed.slice(1, mixed.units.length - 1), seed++);
                        verifyStreams(mixed);
                    }
                }
                View adjacent = fixture.immediate("0123AB_$xy".toCharArray()).slice(2, 9);
                View shared = fixture.rawJoin(immediate, immediate);
                View nested = fixture.rawJoin(
                        fixture.rawJoin(empty, shared.slice(1, shared.units.length)),
                        fixture.rawJoin(adjacent, empty));
                verifyAccess(nested, seed++);
                verifyStreams(nested);
                List<View> pieces = List.of(immediate, nativeWide, adjacent, nativeLittle,
                        immediate, adjacent.slice(1, adjacent.units.length));
                View balanced = fixture.balanced(pieces, 0, pieces.size());
                verifyAccess(balanced, seed++);
                verifyAccess(balanced.slice(1, balanced.units.length - 1), seed++);
                verifyStreams(balanced);
            }
        }
        System.out.println("M3_IMMEDIATE_CURSOR_SEAMS_PASS checks=" + (checks - checksBefore)
                + " scenarios=" + (scenarios - scenariosBefore)
                + " actual_immediate_native_tuple_owners=true");
    }

    private static void exhaustiveShortText() {
        char[] alphabet = {'a', '\0', '\ud800', '\udc00'};
        for (int length = 0; length <= 5; length++) {
            int combinations = 1 << (2 * length);
            for (int code = 0; code < combinations; code++) {
                char[] units = new char[length];
                int remaining = code;
                for (int at = 0; at < length; at++, remaining >>>= 2) {
                    units[at] = alphabet[remaining & 3];
                }
                try (NativeFixture fixture = new NativeFixture()) {
                    View source = fixture.atom(units, 2, (code & 1) == 0);
                    List<View> leaves = new ArrayList<>();
                    for (int at = 0; at < length; at++) leaves.add(source.slice(at, at + 1));
                    View balanced = fixture.balanced(leaves, 0, leaves.size());
                    View left = new View(M3String.empty(), new char[0]);
                    View right = left;
                    for (int at = 0; at < length; at++) {
                        left = fixture.join(left, leaves.get(at));
                        right = fixture.join(leaves.get(length - 1 - at), right);
                    }
                    verifyAccess(left, code);
                    verifyAccess(right, code + 1L);
                    for (int from = 0; from <= length; from++) {
                        for (int to = from; to <= length; to++) {
                            verifyAccess(balanced.slice(from, to), code + from * 31L + to);
                        }
                    }
                    verifyStreams(balanced);
                }
            }
        }
    }

    private static void randomSharedSlices() {
        Random random = new Random(0x4d33524f50453231L);
        char[] alphabet = {'a', 'b', '\0', '\u00ff', '\u0100', '\uffff',
                '\ud800', '\udbff', '\udc00', '\udfff', '\r', '\n'};
        for (int trial = 0; trial < 350; trial++) {
            try (NativeFixture fixture = new NativeFixture()) {
                List<View> known = new ArrayList<>();
                for (int atom = 0; atom < 5; atom++) {
                    char[] units = new char[1 + random.nextInt(24)];
                    for (int at = 0; at < units.length; at++) {
                        units[at] = alphabet[random.nextInt(alphabet.length)];
                    }
                    known.add(fixture.atom(units, 2, random.nextBoolean()));
                }
                for (int operation = 0; operation < 20; operation++) {
                    View source = known.get(random.nextInt(known.size()));
                    View next;
                    if ((operation % 3) == 0 && source.units.length > 0
                            && source.units.length < 2048) {
                        next = fixture.join(source, source);
                        M3StringTuple shared = (M3StringTuple) next.text.owner();
                        require(shared.left == shared.right, "shared physical child");
                    } else if ((operation % 3) == 1) {
                        int from = random.nextInt(source.units.length + 1);
                        int to = from + random.nextInt(source.units.length - from + 1);
                        next = source.slice(from, to);
                    } else {
                        View other = known.get(random.nextInt(known.size()));
                        next = fixture.join(source, other);
                    }
                    if (next.units.length <= 8192) known.add(next);
                    verifyAccess(next, trial * 97L + operation);
                    if (operation % 5 == 0) verifyStreams(next);
                }
            }
        }
    }

    private static void deepTrees() {
        try (NativeFixture fixture = new NativeFixture()) {
            View atom = fixture.atom(new char[] {'A', '\ud83d', '\ude42', '\u00ff'}, 2, true);
            View shared = atom;
            for (int depth = 0; depth < 12; depth++) shared = fixture.join(shared, shared);
            verifyAccess(shared, 7);
            verifyAccess(shared.slice(1, shared.units.length - 1), 8);
            verifyStreams(shared.slice(1, shared.units.length - 1));

            View left = atom;
            View right = atom;
            for (int depth = 0; depth < 256; depth++) {
                View unit = atom.slice(depth & 3, (depth & 3) + 1);
                left = fixture.join(left, unit);
                right = fixture.join(unit, right);
            }
            verifyAccess(left, 9);
            verifyAccess(right, 10);
            verifyAccess(left.slice(1, left.units.length - 1), 11);
            verifyAccess(right.slice(1, right.units.length - 1), 12);
        }
    }

    private static void largeCoordinates() {
        try (NativeFixture fixture = new NativeFixture()) {
            char[] period = {'A', '\ud800', '\udc00', '\uffff'};
            M3String seed = fixture.atom(period, 2, true).text;
            M3String[] powers = new M3String[29];
            powers[0] = seed;
            for (int power = 1; power < powers.length; power++) {
                powers[power] = fixture.joinOwners(powers[power - 1], powers[power - 1]);
            }
            M3String huge = powers[powers.length - 1];
            for (int power = powers.length - 2; power >= 0; power--) {
                huge = fixture.joinOwners(huge, powers[power]);
            }
            huge = fixture.joinOwners(huge, seed.slice(0, 3));
            require(huge.length() == Integer.MAX_VALUE, "maximum logical length");
            require(fixture.nativeBytes == 8, "maximum coordinate uses only eight native bytes");

            // The oracle is periodic arithmetic. No maximum-sized array, scan, or flattening.
            verifyPeriodic(huge, 0, period);
            verifyPeriodic(huge.slice(1, Integer.MAX_VALUE - 1), 1, period);
            verifyPeriodic(huge.slice(2, Integer.MAX_VALUE), 2, period);
            int high = 1 << 30;
            verifyPeriodic(M3String.range(huge.owner(), high, high - 1), high, period);
            verifyPeriodic(huge.slice(Integer.MAX_VALUE - 257, Integer.MAX_VALUE),
                    Integer.MAX_VALUE - 257L, period);

            // Both child coordinates start near the int limit, while their tuple is small.
            M3String left = huge.slice(Integer.MAX_VALUE - 65, Integer.MAX_VALUE - 1);
            M3String right = huge.slice(Integer.MAX_VALUE - 257, Integer.MAX_VALUE - 128);
            verifyPeriodic(fixture.joinOwners(left, right), Integer.MAX_VALUE - 65L, period);
        }
    }

    private static void verifyPeriodic(M3String source, long origin, char[] period) {
        scenarios++;
        int length = source.length();
        M3String.CodeUnitCursor cursor = new M3String.CodeUnitCursor(source);
        int[] anchors = {0, 1, 2, 3, length / 2, length - 130, length - 65,
                length - 3, length - 2, length - 1};
        Arrays.sort(anchors);
        for (int anchor : anchors) {
            int from = Math.max(0, anchor - 2);
            int to = (int) Math.min(length - 1L, anchor + 2L);
            for (int at = from; at <= to; at++) equalPeriodic(cursor, origin, period, at);
        }
        for (int position = anchors.length - 1; position >= 0; position--) {
            int anchor = anchors[position];
            int from = Math.max(0, anchor - 2);
            int to = (int) Math.min(length - 1L, anchor + 2L);
            for (int at = to; at >= from; at--) equalPeriodic(cursor, origin, period, at);
        }
        for (int at = 0; at < 64; at++) {
            equalPeriodic(cursor, origin, period, at);
            equalPeriodic(cursor, origin, period, length - 1 - at);
        }
        Random random = new Random(0x4d4158434f4f5244L);
        for (int probe = 0; probe < 512; probe++) {
            equalPeriodic(cursor, origin, period, random.nextInt(length));
        }
        for (int invalid : new int[] {-1, length, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            boolean failed = false;
            try {
                cursor.at(invalid);
            } catch (IndexOutOfBoundsException expected) {
                failed = true;
            }
            require(failed, "maximum coordinate invalid index must fail");
            equalPeriodic(cursor, origin, period, length - 1);
        }
    }

    private static void equalPeriodic(
            M3String.CodeUnitCursor cursor, long origin, char[] period, int at) {
        char expected = period[(int) ((origin + at) % period.length)];
        char actual = cursor.at(at);
        checks++;
        if (actual != expected) {
            throw new AssertionError("maximum coordinate index=" + at + " origin=" + origin
                    + " expected=" + (int) expected + " actual=" + (int) actual);
        }
    }

    private static void verifyAccess(View view, long seed) {
        scenarios++;
        require(view.text.length() == view.units.length, "length");
        M3String.CodeUnitCursor cursor = new M3String.CodeUnitCursor(view.text);
        for (int at = 0; at < view.units.length; at++) {
            equalUnit(cursor, view.units, at);
            if ((at & 3) == 0) equalUnit(cursor, view.units, at);
        }
        for (int at = view.units.length - 1; at >= 0; at--) equalUnit(cursor, view.units, at);
        for (int lo = 0, hi = view.units.length - 1; lo <= hi; lo++, hi--) {
            equalUnit(cursor, view.units, lo);
            equalUnit(cursor, view.units, hi);
        }
        if (view.units.length != 0) {
            Random random = new Random(seed);
            int probes = Math.min(64, view.units.length * 2 + 8);
            for (int at = 0; at < probes; at++) {
                equalUnit(cursor, view.units, random.nextInt(view.units.length));
            }
        }
        for (int invalid : new int[] {-1, view.units.length, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            boolean failed = false;
            try {
                cursor.at(invalid);
            } catch (IndexOutOfBoundsException expected) {
                failed = true;
            }
            require(failed, "invalid index must fail");
            if (view.units.length != 0) equalUnit(cursor, view.units, view.units.length / 2);
        }
    }

    private static void equalUnit(M3String.CodeUnitCursor cursor, char[] expected, int at) {
        char actual = cursor.at(at);
        checks++;
        if (actual != expected[at]) {
            throw new AssertionError("cursor unit index=" + at + " length=" + expected.length
                    + " expected=" + (int) expected[at] + " actual=" + (int) actual);
        }
    }

    private static void verifyStreams(View view) {
        int[] units = new int[view.units.length];
        int[] codePoints = new int[view.units.length];
        int count = 0;
        for (int at = 0; at < units.length; at++) units[at] = view.units[at];
        for (int at = 0; at < view.units.length;) {
            int cp = Character.codePointAt(view.units, at, view.units.length);
            codePoints[count++] = cp;
            at += Character.charCount(cp);
        }
        codePoints = Arrays.copyOf(codePoints, count);
        require(Arrays.equals(units, view.text.charsStream().toArray()), "chars stream");
        require(Arrays.equals(codePoints, view.text.codePointsStream().toArray()), "code point stream");
        require(Arrays.equals(units, splitValues(view.text.charsStream().spliterator())), "split chars");
        require(Arrays.equals(codePoints, splitValues(view.text.codePointsStream().spliterator())),
                "split code points");
    }

    private static int[] splitValues(Spliterator.OfInt source) {
        List<Integer> values = new ArrayList<>();
        collectSplits(source, values, 0);
        int[] result = new int[values.size()];
        for (int at = 0; at < result.length; at++) result[at] = values.get(at);
        return result;
    }

    private static void collectSplits(Spliterator.OfInt source, List<Integer> values, int depth) {
        Spliterator.OfInt prefix = depth < 6 ? source.trySplit() : null;
        if (prefix != null) {
            collectSplits(prefix, values, depth + 1);
            collectSplits(source, values, depth + 1);
        } else {
            source.forEachRemaining((IntConsumer) values::add);
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Workload construction stays outside measured cursor traversal. */
    public static BenchmarkFixture benchmarkFixture(String kind) {
        return new BenchmarkFixture(kind);
    }

    public static final class BenchmarkFixture implements AutoCloseable {
        private final NativeFixture fixture = new NativeFixture();
        private final View view;
        private final int[] randomPositions;
        private final long expectedSum;

        private BenchmarkFixture(String kind) {
            char[] units = new char[16_384];
            for (int at = 0; at < units.length; at++) units[at] = (char) (32 + at % 91);
            View atom = fixture.atom(units, 1, true);
            View built;
            if (kind.equals("atom-16384")) {
                built = atom;
            } else if (kind.equals("shared-16384") || kind.equals("shared-slice-16382")) {
                built = atom.slice(0, 4);
                for (int depth = 0; depth < 12; depth++) built = fixture.join(built, built);
                if (kind.equals("shared-slice-16382")) built = built.slice(1, built.units.length - 1);
            } else if (kind.equals("balanced-unit-16384") || kind.equals("balanced-8-16384")
                    || kind.equals("balanced-64-16384")) {
                int width = kind.equals("balanced-unit-16384") ? 1
                        : kind.equals("balanced-8-16384") ? 8 : 64;
                List<View> leaves = new ArrayList<>();
                for (int from = 0; from < units.length; from += width) {
                    leaves.add(atom.slice(from, Math.min(units.length, from + width)));
                }
                built = fixture.balanced(leaves, 0, leaves.size());
            } else {
                fixture.close();
                throw new IllegalArgumentException("unknown workload " + kind);
            }
            view = built;
            randomPositions = new int[view.units.length];
            Random random = new Random(0x435552534f524cL);
            long sum = 0;
            for (int at = 0; at < randomPositions.length; at++) {
                randomPositions[at] = random.nextInt(view.units.length);
                sum += view.units[at];
            }
            expectedSum = sum;
        }

        public int length() { return view.units.length; }
        public long nativeBytes() { return fixture.nativeBytes; }
        public void verify() { verifyAccess(view, 17); }

        public long traverse(int mode, int repetitions, boolean reuseCursor) {
            long sum = 0;
            M3String.CodeUnitCursor reused = reuseCursor
                    ? new M3String.CodeUnitCursor(view.text) : null;
            for (int repetition = 0; repetition < repetitions; repetition++) {
                M3String.CodeUnitCursor cursor = reuseCursor ? reused
                        : new M3String.CodeUnitCursor(view.text);
                if (mode == 0) {
                    for (int at = 0; at < view.units.length; at++) sum += cursor.at(at);
                } else if (mode == 1) {
                    for (int at = view.units.length - 1; at >= 0; at--) sum += cursor.at(at);
                } else {
                    for (int at : randomPositions) sum += cursor.at(at);
                }
            }
            return sum;
        }

        public long firstRead(int repetitions) {
            long sum = 0;
            for (int repetition = 0; repetition < repetitions; repetition++) {
                M3String.CodeUnitCursor cursor = new M3String.CodeUnitCursor(view.text);
                sum += cursor.at(randomPositions[repetition % randomPositions.length]);
            }
            return sum;
        }

        public long firstReadExpected(int repetitions) {
            long sum = 0;
            for (int repetition = 0; repetition < repetitions; repetition++) {
                sum += view.units[randomPositions[repetition % randomPositions.length]];
            }
            return sum;
        }

        public long expected(int mode, int repetitions) {
            if (mode != 2) return expectedSum * repetitions;
            long sum = 0;
            for (int at : randomPositions) sum += view.units[at];
            return sum * repetitions;
        }

        @Override
        public void close() { fixture.close(); }
    }
}
