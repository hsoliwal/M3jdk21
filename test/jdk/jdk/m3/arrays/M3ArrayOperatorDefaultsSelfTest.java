// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic Java 21 oracle for opt-in named array-plus defaults.
 * Run directly as a main class or through the adjacent JUnit wrapper.
 */
public final class M3ArrayOperatorDefaultsSelfTest {
    private static long checks;

    private M3ArrayOperatorDefaultsSelfTest() {}

    public static void main(String[] args) {
        boundaryAndOwnership();
        surrogateFactsAndSlicing();
        overridesAndFailures();
        randomizedDifferentials();
        System.out.println("M3ArrayOperatorDefaultsSelfTest PASS checks=" + checks);
    }

    private static void boundaryAndOwnership() {
        byte[] originalBytes = {1, 2, 3};
        ByteArrayView bytes = M3Arrays.bytes(originalBytes);
        originalBytes[0] = 99;
        ByteArrayView withEmpty = M3Arrays.asByteSequenceOperator(bytes)
                .plus(M3Arrays.bytes(new byte[0]));
        ByteArrayView joinedBytes = M3Arrays.asByteSequenceOperator(withEmpty)
                .plus(M3Arrays.bytes(new byte[] {4, -5}));
        equal(new byte[] {1, 2, 3, 4, -5}, joinedBytes.copy(), "byte joins");
        check(joinedBytes.segmentCount() == 2, "byte segments excluding empty");
        equal(new byte[] {3, 4}, joinedBytes.slice(2, 4).copy(), "byte seam slice");
        check(joinedBytes.asReadOnlyBuffers().length == 2, "byte descriptors");
        check(joinedBytes.asReadOnlyBuffers()[0].isReadOnly(), "byte descriptor readonly");
        throwsType(ReadOnlyBufferException.class,
                () -> joinedBytes.asReadOnlyBuffers()[0].put(0, (byte) 1), "byte readonly write");
        byte[] byteCopy = joinedBytes.copy();
        byteCopy[0] = 42;
        check(joinedBytes.byteAt(0) == 1, "byte copy isolation");
        throwsType(IndexOutOfBoundsException.class, () -> joinedBytes.byteAt(5),
                "byte bounds");

        int[] originalInts = {10, 20};
        IntArrayView ints = M3Arrays.ints(originalInts);
        originalInts[0] = 9000;
        IntArrayView joinedInts = M3Arrays.asIntSequenceOperator(ints)
                .plus(M3Arrays.ints(new int[] {30, -40}));
        equal(new int[] {10, 20, 30, -40}, joinedInts.copy(), "int joins");
        equal(new int[] {20, 30}, joinedInts.slice(1, 3).copy(), "int seam slice");
        int[] intCopy = joinedInts.copy();
        intCopy[0] = 999;
        check(joinedInts.intAt(0) == 10, "int copy isolation");
        int[] intTarget = new int[6];
        joinedInts.copyTo(1, intTarget, 2, 3);
        equal(new int[] {0, 0, 20, 30, -40, 0}, intTarget, "int offset copy");
        throwsType(IndexOutOfBoundsException.class, () -> joinedInts.slice(-1, 3),
                "int slice bounds");

        long[] originalLongs = {Long.MIN_VALUE, 0};
        LongArrayView longs = M3Arrays.longs(originalLongs);
        originalLongs[0] = 4;
        LongArrayView joinedLongs = M3Arrays.asLongSequenceOperator(longs)
                .plus(M3Arrays.longs(new long[] {Long.MAX_VALUE, -7}));
        equal(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE, -7},
                joinedLongs.copy(), "long joins");
        equal(new long[] {0, Long.MAX_VALUE}, joinedLongs.slice(1, 3).copy(),
                "long seam slice");
        long[] longTarget = new long[5];
        joinedLongs.copyTo(2, longTarget, 1, 2);
        equal(new long[] {0, Long.MAX_VALUE, -7, 0, 0}, longTarget,
                "long offset copy");
        throwsType(IndexOutOfBoundsException.class, () -> joinedLongs.longAt(-1),
                "long bounds");

        check(M3Arrays.asByteSequenceOperator(bytes).source() == bytes, "byte source identity");
        check(M3Arrays.asIntSequenceOperator(ints).source() == ints, "int source identity");
        check(M3Arrays.asLongSequenceOperator(longs).source() == longs, "long source identity");
    }

    private static void surrogateFactsAndSlicing() {
        char[] caller = {'A', '\uD83D'};
        Utf16ArrayView left = M3Arrays.chars(caller);
        caller[0] = 'Z';
        Utf16ArrayView right = M3Arrays.stringView("\uDE00B");
        Utf16ArrayView joined = M3Arrays.asUtf16SequenceOperator(left).plus(right);
        check("A\uD83D\uDE00B".equals(joined.toString()), "UTF-16 code-unit composition");
        check(joined.length() == 4, "UTF-16 length");
        check(joined.codePointCount() == 3, "surrogate seam count");
        check(joined.codePointAt(1) == 0x1F600, "surrogate seam code point");
        check(joined.javaHashCode() == "A\uD83D\uDE00B".hashCode(), "hash algebra");
        check(joined.facts().length() == 4, "facts length");
        check(joined.segmentCount() == 2, "UTF-16 segment identity");
        check(joined.asReadOnlyCharBuffers().length == 2, "UTF-16 buffer descriptors");
        check(joined.asReadOnlyCharBuffers()[0].isReadOnly(), "UTF-16 readonly descriptor");
        throwsType(ReadOnlyBufferException.class,
                () -> joined.asReadOnlyCharBuffers()[0].put(0, 'Q'), "UTF-16 readonly write");
        check("\uD83D\uDE00".equals(joined.subSequence(1, 3).toString()),
                "UTF-16 seam slice");
        check(joined.subSequence(1, 3).codePointCount() == 1,
                "UTF-16 seam slice facts");
        char[] exported = joined.copy();
        exported[0] = 'X';
        check(joined.charAt(0) == 'A', "UTF-16 copy isolation");
        check(M3Arrays.asUtf16SequenceOperator(left).source() == left,
                "UTF-16 source identity");
        throwsType(IndexOutOfBoundsException.class, () -> joined.charAt(4), "UTF-16 bounds");
    }

    private static void overridesAndFailures() {
        ByteArrayView bytes = M3Arrays.bytes(new byte[] {1});
        IntArrayView ints = M3Arrays.ints(new int[] {2});
        LongArrayView longs = M3Arrays.longs(new long[] {3});
        Utf16ArrayView chars = M3Arrays.chars(new char[] {'a'});
        throwsType(NullPointerException.class,
                () -> M3Arrays.asByteSequenceOperator(null), "null byte factory");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asUtf16SequenceOperator(null), "null UTF-16 factory");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asIntSequenceOperator(null), "null int factory");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asLongSequenceOperator(null), "null long factory");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asByteSequenceOperator(bytes).plus(null), "null byte right");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asUtf16SequenceOperator(chars).plus(null), "null char right");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asIntSequenceOperator(ints).plus(null), "null int right");
        throwsType(NullPointerException.class,
                () -> M3Arrays.asLongSequenceOperator(longs).plus(null), "null long right");
        throwsType(NullPointerException.class,
                () -> ((M3Arrays.IntSequenceOperator) () -> null).plus(ints),
                "dynamic null int source");

        AtomicInteger overrideCalls = new AtomicInteger();
        M3Arrays.IntSequenceOperator override = new M3Arrays.IntSequenceOperator() {
            @Override public IntArrayView source() { return ints; }
            @Override public IntArrayView plus(IntArrayView right) {
                overrideCalls.incrementAndGet();
                return source();
            }
        };
        check(override.plus(M3Arrays.ints(new int[] {4})) == ints,
                "default is overridable");
        check(overrideCalls.get() == 1, "override invoked exactly once");

        AtomicInteger sourceCalls = new AtomicInteger();
        M3Arrays.LongSequenceOperator dynamic = () -> {
            sourceCalls.incrementAndGet();
            return longs;
        };
        equal(new long[] {3, 7}, dynamic.plus(M3Arrays.longs(new long[] {7})).copy(),
                "dynamic functional source");
        check(sourceCalls.get() == 1, "source evaluated once");

        IntArrayView giant = new IntArrayView() {
            @Override public int length() { return Integer.MAX_VALUE; }
            @Override public int intAt(int index) { return index; }
            @Override public IntArrayView slice(int start, int end) {
                throw new AssertionError("slice not needed");
            }
        };
        throwsType(ArithmeticException.class,
                () -> M3Arrays.asIntSequenceOperator(giant).plus(ints), "length overflow");
        equal(new byte[0], M3Arrays.asByteSequenceOperator(
                M3Arrays.bytes(new byte[0])).plus(M3Arrays.bytes(new byte[0])).copy(),
                "empty byte operands");
        equal(new int[0], M3Arrays.asIntSequenceOperator(
                M3Arrays.ints(new int[0])).plus(M3Arrays.ints(new int[0])).copy(),
                "empty int operands");
        equal(new long[0], M3Arrays.asLongSequenceOperator(
                M3Arrays.longs(new long[0])).plus(M3Arrays.longs(new long[0])).copy(),
                "empty long operands");
        check(M3Arrays.asUtf16SequenceOperator(M3Arrays.chars(new char[0]))
                .plus(M3Arrays.chars(new char[0])).length() == 0, "empty char operands");
    }

    private static void randomizedDifferentials() {
        Random random = new Random(0x4D33415252415953L);
        for (int iteration = 0; iteration < 400; iteration++) {
            int leftLength = random.nextInt(18);
            int rightLength = random.nextInt(18);
            byte[] bytesLeft = new byte[leftLength], bytesRight = new byte[rightLength];
            random.nextBytes(bytesLeft);
            random.nextBytes(bytesRight);
            ByteArrayView byteResult = M3Arrays.asByteSequenceOperator(
                    M3Arrays.bytes(bytesLeft)).plus(M3Arrays.bytes(bytesRight));
            byte[] expectedBytes = Arrays.copyOf(bytesLeft, leftLength + rightLength);
            System.arraycopy(bytesRight, 0, expectedBytes, leftLength, rightLength);
            equal(expectedBytes, byteResult.copy(), "random byte concat");
            check(byteResult.length() == expectedBytes.length, "random byte length");

            char[] charsLeft = new char[leftLength], charsRight = new char[rightLength];
            for (int i = 0; i < leftLength; i++) charsLeft[i] = randomChar(random);
            for (int i = 0; i < rightLength; i++) charsRight[i] = randomChar(random);
            Utf16ArrayView charResult = M3Arrays.asUtf16SequenceOperator(
                    M3Arrays.chars(charsLeft)).plus(M3Arrays.chars(charsRight));
            char[] expectedChars = Arrays.copyOf(charsLeft, leftLength + rightLength);
            System.arraycopy(charsRight, 0, expectedChars, leftLength, rightLength);
            equal(expectedChars, charResult.copy(), "random UTF-16 concat");
            String expectedString = new String(expectedChars);
            check(charResult.javaHashCode() == expectedString.hashCode(), "random UTF-16 hash");
            check(charResult.codePointCount() ==
                    expectedString.codePointCount(0, expectedString.length()),
                    "random UTF-16 code points");

            int[] intsLeft = new int[leftLength], intsRight = new int[rightLength];
            long[] longsLeft = new long[leftLength], longsRight = new long[rightLength];
            for (int i = 0; i < leftLength; i++) {
                intsLeft[i] = random.nextInt();
                longsLeft[i] = random.nextLong();
            }
            for (int i = 0; i < rightLength; i++) {
                intsRight[i] = random.nextInt();
                longsRight[i] = random.nextLong();
            }
            IntArrayView intResult = M3Arrays.asIntSequenceOperator(
                    M3Arrays.ints(intsLeft)).plus(M3Arrays.ints(intsRight));
            int[] expectedInts = Arrays.copyOf(intsLeft, leftLength + rightLength);
            System.arraycopy(intsRight, 0, expectedInts, leftLength, rightLength);
            equal(expectedInts, intResult.copy(), "random int concat");
            LongArrayView longResult = M3Arrays.asLongSequenceOperator(
                    M3Arrays.longs(longsLeft)).plus(M3Arrays.longs(longsRight));
            long[] expectedLongs = Arrays.copyOf(longsLeft, leftLength + rightLength);
            System.arraycopy(longsRight, 0, expectedLongs, leftLength, rightLength);
            equal(expectedLongs, longResult.copy(), "random long concat");

            int start = random.nextInt(leftLength + rightLength + 1);
            int end = start + random.nextInt(leftLength + rightLength - start + 1);
            equal(Arrays.copyOfRange(expectedBytes, start, end),
                    byteResult.slice(start, end).copy(), "random byte slice");
            equal(Arrays.copyOfRange(expectedChars, start, end),
                    charResult.subSequence(start, end).copy(), "random UTF-16 slice");
            equal(Arrays.copyOfRange(expectedInts, start, end),
                    intResult.slice(start, end).copy(), "random int slice");
            equal(Arrays.copyOfRange(expectedLongs, start, end),
                    longResult.slice(start, end).copy(), "random long slice");
        }
        for (int count : new int[] {63, 64, 65, 127, 128, 129}) {
            IntArrayView[] segments = new IntArrayView[count];
            for (int i = 0; i < count; i++) {
                segments[i] = M3Arrays.ints(new int[] {i});
            }
            IntArrayView joined = M3Arrays.ints(new int[0]);
            for (IntArrayView segment : segments) {
                joined = M3Arrays.asIntSequenceOperator(joined).plus(segment);
            }
            check(joined.length() == count, "many offset segments length");
            for (int i = 0; i < count; i++) {
                check(joined.intAt(i) == i, "many offset segments index");
            }
            int[] copied = joined.copy();
            for (int i = 0; i < count; i++) check(copied[i] == i, "many offsets copy");
        }
    }

    private static char randomChar(Random random) {
        return switch (random.nextInt(6)) {
            case 0 -> (char) (0xD800 + random.nextInt(0x400));
            case 1 -> (char) random.nextInt(128);
            case 2 -> (char) (0xDC00 + random.nextInt(0x400));
            default -> (char) random.nextInt(0x10000);
        };
    }

    private static void equal(byte[] expected, byte[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
    }

    private static void equal(char[] expected, char[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
    }

    private static void equal(int[] expected, int[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
    }

    private static void equal(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action, String label) {
        boolean expected = false;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) {
                throw new AssertionError(label + ": unexpected " + failure, failure);
            }
            expected = true;
        }
        check(expected, label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " (check " + checks + ")");
    }
}
