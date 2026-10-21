/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3JDK21 JNI known-slice concatenation over heap and OS-mapped interned arrays
 * @modules java.base/java.lang:+open
 * @run main/othervm M3StringKnownSliceConcatJNI
 */

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class M3StringKnownSliceConcatJNI {
    private static MethodHandle charSlices;
    private static MethodHandle byteSlices;
    private static MethodHandle mappedChars;
    private static MethodHandle mappedBytes;
    private static long assertions;

    private static void require(boolean condition, String context) {
        assertions++;
        if (!condition) throw new AssertionError(context);
    }

    @FunctionalInterface
    private interface CheckedCall { void run() throws Throwable; }

    private static void expects(Class<? extends Throwable> expected,
            CheckedCall callback, String context) throws Throwable {
        assertions++;
        try {
            callback.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError(context + ": unexpected " + error, error);
        }
        throw new AssertionError(context + ": expected " + expected.getSimpleName());
    }

    private static void initialize() throws Exception {
        Class<?> m3 = Class.forName("java.lang.M3String");
        Set<String> fields = Arrays.stream(m3.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(field -> field.getName()).collect(Collectors.toSet());
        require(fields.equals(Set.of("owner", "value")),
                "canonical M3String must remain owner + packed coordinate");
        MethodHandles.Lookup access = MethodHandles.privateLookupIn(m3, MethodHandles.lookup());
        charSlices = access.findStatic(m3, "concatKnownChars", MethodType.methodType(
                char[].class, char[].class, int.class, int.class,
                char[].class, int.class, int.class));
        byteSlices = access.findStatic(m3, "concatKnownBytes", MethodType.methodType(
                byte[].class, byte[].class, int.class, int.class,
                byte[].class, int.class, int.class));
        mappedChars = access.findStatic(m3, "concatMappedUtf16", MethodType.methodType(
                char[].class, ByteBuffer.class, int.class, int.class, boolean.class,
                ByteBuffer.class, int.class, int.class, boolean.class));
        mappedBytes = access.findStatic(m3, "concatMappedBytes", MethodType.methodType(
                byte[].class, ByteBuffer.class, int.class, int.class,
                ByteBuffer.class, int.class, int.class));
    }

    private static char[] join(char[] a, int at, int count,
            char[] b, int from, int length) throws Throwable {
        return (char[]) charSlices.invoke(a, at, count, b, from, length);
    }

    private static byte[] join(byte[] a, int at, int count,
            byte[] b, int from, int length) throws Throwable {
        return (byte[]) byteSlices.invoke(a, at, count, b, from, length);
    }

    private static void randomizedHeapSliceParity() throws Throwable {
        Random random = new Random(0x4d330021L);
        for (int round = 0; round < 2048; round++) {
            char[] left = new char[random.nextInt(256)];
            char[] right = new char[random.nextInt(256)];
            for (int i = 0; i < left.length; i++) left[i] = (char) random.nextInt(65536);
            for (int i = 0; i < right.length; i++) right[i] = (char) random.nextInt(65536);
            int leftFrom = random.nextInt(left.length + 1);
            int rightFrom = random.nextInt(right.length + 1);
            int l = random.nextInt(left.length - leftFrom + 1);
            int r = random.nextInt(right.length - rightFrom + 1);
            char[] expected = new char[l + r];
            System.arraycopy(left, leftFrom, expected, 0, l);
            System.arraycopy(right, rightFrom, expected, l, r);
            char[] output = join(left, leftFrom, l, right, rightFrom, r);
            require(Arrays.equals(output, expected), "heap chars round " + round);
            require(output != left && output != right, "fresh char array " + round);
            if (output.length != 0) output[0] ^= 1;
            require(Arrays.equals(
                    join(left, leftFrom, l, right, rightFrom, r), expected),
                    "source immutable under result mutation " + round);

            byte[] leftBytes = new byte[left.length];
            byte[] rightBytes = new byte[right.length];
            random.nextBytes(leftBytes);
            random.nextBytes(rightBytes);
            byte[] expectedBytes = new byte[l + r];
            System.arraycopy(leftBytes, leftFrom, expectedBytes, 0, l);
            System.arraycopy(rightBytes, rightFrom, expectedBytes, l, r);
            byte[] bytes = join(leftBytes, leftFrom, l, rightBytes, rightFrom, r);
            require(Arrays.equals(bytes, expectedBytes), "heap bytes round " + round);
            require(bytes != leftBytes && bytes != rightBytes, "fresh byte array " + round);
        }
        char[] joined = join(new char[] {'!', '\ud83d'}, 1, 1,
                new char[] {'\ude00', '?', '\u0000'}, 0, 3);
        require(Arrays.equals(joined, new char[] {'\ud83d', '\ude00', '?', '\u0000'}),
                "split surrogate and NUL seams");
        char[] longText = new char[8193];
        Arrays.fill(longText, '\u0141');
        require(join(longText, 0, longText.length, longText, 0, 7).length == 8200,
                "4096+ code units cross repeated JNI transfer blocks");
    }

    private static ByteBuffer directChars(String spelling, ByteOrder order) {
        ByteBuffer result = ByteBuffer.allocateDirect(spelling.length() * 2).order(order);
        for (int index = 0; index < spelling.length(); index++)
            result.putChar(spelling.charAt(index));
        result.clear();
        return result.asReadOnlyBuffer();
    }

    private static void mappedSliceParity() throws Throwable {
        String a = "x\ud83d";
        String b = "\ude00\u0000\u0141z";
        ByteBuffer left = directChars(a, ByteOrder.BIG_ENDIAN);
        ByteBuffer right = directChars(b, ByteOrder.LITTLE_ENDIAN);
        char[] codeUnits = (char[]) mappedChars.invoke(
                left, 1, 1, true, right, 0, 3, false);
        require(Arrays.equals(codeUnits, new char[] {'\ud83d', '\ude00', '\u0000', '\u0141'}),
                "mixed endian source and split surrogate");
        require(left.isReadOnly() && right.isReadOnly(), "no direct-source mutation");
        require(left.position() == 0 && right.position() == 0, "no direct position mutation");

        byte[] raw = new byte[] {(byte) 0x81, 0, (byte) 0xff, 17, 0, 23};
        ByteBuffer bytes = ByteBuffer.allocateDirect(raw.length);
        bytes.put(raw).clear();
        ByteBuffer immutable = bytes.asReadOnlyBuffer();
        byte[] output = (byte[]) mappedBytes.invoke(immutable, 1, 2, immutable, 3, 3);
        require(Arrays.equals(output, new byte[] {0, (byte) 0xff, 17, 0, 23}),
                "raw known-address byte ranges");

        Path file = Files.createTempFile("m3jni-interned-utf16-", ".bin");
        file.toFile().deleteOnExit();
        ByteBuffer encoded = ByteBuffer.allocate((a + b).length() * 2)
                .order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < (a + b).length(); i++)
            encoded.putChar((a + b).charAt(i));
        Files.write(file, encoded.array());
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            MappedByteBuffer mapping = channel.map(FileChannel.MapMode.READ_ONLY, 0, encoded.capacity());
            char[] flat = (char[]) mappedChars.invoke(
                    mapping, 0, a.length(), true, mapping, a.length(), b.length(), true);
            require(Arrays.equals(flat, (a + b).toCharArray()),
                    "one read-only OS mapping, two known address windows");
            require(mapping.isDirect() && mapping.isReadOnly(),
                    "input remains direct read-only");
        }
    }

    private static void invalidSources() throws Throwable {
        expects(NullPointerException.class, () ->
                join((char[]) null, 0, 0, new char[0], 0, 0), "null heap chars");
        expects(IndexOutOfBoundsException.class, () ->
                join(new char[] {'x'}, -1, 1, new char[0], 0, 0), "negative char start");
        expects(IndexOutOfBoundsException.class, () ->
                join(new byte[] {1}, 0, 2, new byte[0], 0, 0), "byte overrun");
        expects(IllegalArgumentException.class, () ->
                mappedChars.invoke(ByteBuffer.allocateDirect(3), 0, 1, true,
                        ByteBuffer.allocateDirect(0), 0, 0, true),
                "odd-byte UTF16 direct buffer");
        expects(IllegalArgumentException.class, () ->
                mappedBytes.invoke(ByteBuffer.wrap(new byte[] {1}), 0, 1,
                        ByteBuffer.allocateDirect(0), 0, 0), "non-direct source");
        expects(IndexOutOfBoundsException.class, () ->
                mappedBytes.invoke(ByteBuffer.allocateDirect(1), 0, 2,
                        ByteBuffer.allocateDirect(0), 0, 0), "direct byte overrun");
    }

    public static void main(String[] args) throws Throwable {
        initialize();
        randomizedHeapSliceParity();
        mappedSliceParity();
        invalidSources();
        System.out.println("M3StringKnownSliceConcatJNI PASS checks=" + assertions);
    }
}
