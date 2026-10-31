/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary JNI concatenation of OS-mapped interned M3String views preserves backing ownership and UTF-16
 * @modules java.base/java.lang:+open java.base/sun.nio.ch
 * @run main/othervm M3StringOsMappedNativeConcatTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Random;
import sun.nio.ch.DirectBuffer;

public final class M3StringOsMappedNativeConcatTest {
    private static final String[] PIECES = {
        "ABC", "\u0000", "\u0141", "-", "123", "\ud83d", "\ude00", "xyz", "", "A", "\ud83d", "x", "\ude00"
    };
    private static long checks;
    private static Class<?> valueClass, ownerClass, atomClass, tupleClass;
    private static MethodHandle mappedAtom, whole, range, concatNative, charShadow, hash, sameCoordinate;
    private static MethodHandle ordinaryStringConstructor, compactValueProjection;
    private static Field valueOwner, atomPayloadOwner, tupleLeft, tupleRight;
    private static Field stringValueField, stringM3Field;

    public static void main(String[] args) throws Throwable {
        bind();
        String expected = String.join("", PIECES);
        byte[] data = utf16be(expected);
        Path file = Files.createTempFile("m3-os-mapped-concat-", ".bin");
        file.toFile().deleteOnExit();
        Files.write(file, data);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, data.length);
            long base = ((DirectBuffer) mapped).address();
            check(base != 0L, "read-only OS mapping address");

            Object[] values = new Object[PIECES.length];
            Object parent = mappedValue(mapped, base, expected);
            Object empty = range.invoke(parent, 0, 0);
            int start = 0;
            for (int i = 0; i < PIECES.length; i++) {
                String piece = PIECES[i];
                values[i] = piece.isEmpty() ? empty
                    : mappedValue(mapped, base + 2L * start, piece);
                start += piece.length();
            }
            check(start == expected.length(), "mapped record offsets");
            Object joined = join(values);
            check(expected.equals(text(joined)), "native multi-owner spelling");
            check(expected.hashCode() == (int) hash.invoke(joined), "Java hash parity");
            checkAllLeavesMapped(joined, mapped);
            checkOrdinaryStringDualView(joined, expected);

            for (int trial = 0; trial < 300; trial++) {
                Random random = new Random(0x4d3L + trial);
                Object[] slices = new Object[1 + random.nextInt(64)];
                int at = 0;
                for (int i = 0; i < slices.length; i++) {
                    int remaining = expected.length() - at;
                    int count = i == slices.length - 1 ? remaining : random.nextInt(remaining + 1);
                    slices[i] = range.invoke(parent, at, at + count);
                    at += count;
                }
                check(at == expected.length(), "complete fixed-map slice coverage");
                Object rejoined = join(slices);
                check(expected.equals(text(rejoined)), "native substring seam parity");
                check((boolean) sameCoordinate.invoke(parent, rejoined),
                    "adjacent ranges should recombine their original owner");
            }

            check(text(join(new Object[0])).isEmpty(), "empty array");
            check(text(join(new Object[]{empty, empty})).isEmpty(), "empty values");
            check(text(join(new Object[]{parent})).equals(expected), "one value");
            expectNullArray();
            Object[] invalid = Arrays.copyOf(values, values.length);
            invalid[2] = null;
            expectNullPart(invalid);
            Reference.reachabilityFence(mapped);
        }
        System.out.println("M3StringOsMappedNativeConcatTest PASS checks=" + checks);
    }

    /**
     * The standard java.lang.String object remains the public receiver and keeps its byte[]
     * field, coder and hash fields. Its canonical M3 view owns the actual shared spelling.
     * Compatibility byte[] and char[] are explicit fresh array snapshots, not cached copies
     * or a fabricated mutable alias of the operating system's read-only mapping.
     */
    private static void checkOrdinaryStringDualView(Object mappedView, String expected)
            throws Throwable {
        String first = (String) ordinaryStringConstructor.invoke(mappedView);
        String second = (String) ordinaryStringConstructor.invoke(mappedView);
        byte[] sentinel = (byte[]) stringValueField.get(first);
        check(sentinel != null && sentinel.length == 0, "M3 String uses empty byte[] sentinel");
        check(sentinel == stringValueField.get(second), "String wrappers share compatibility sentinel");
        check(stringM3Field.get(first) == mappedView, "first String retains canonical M3 view");
        check(stringM3Field.get(second) == mappedView, "second String retains same M3 view");

        check(first.length() == expected.length(), "ordinary String.length over M3");
        check(first.equals(expected) && expected.equals(first), "ordinary String.equals");
        check(first.hashCode() == expected.hashCode(), "ordinary String.hashCode");
        check(first.charAt(0) == expected.charAt(0), "ordinary String.charAt");

        char[] charsA = first.toCharArray();
        char[] charsB = first.toCharArray();
        check(charsA != charsB, "toCharArray must return a fresh Java array");
        check(Arrays.equals(charsA, expected.toCharArray()), "ordinary char[] contents");
        check(Arrays.equals(charsB, expected.toCharArray()), "repeat char[] contents");
        charsA[0] ^= 1;
        check(first.charAt(0) == expected.charAt(0), "mutable char[] cannot mutate interned text");

        byte[] bytesA = (byte[]) compactValueProjection.invoke(first);
        byte[] bytesB = (byte[]) compactValueProjection.invoke(first);
        check(bytesA != bytesB && bytesA != sentinel && bytesB != sentinel,
                "explicit Compact-String byte[] snapshots");
        check(bytesA.length == expected.length() * 2, "UTF-16 array byte length");
        bytesA[0] ^= 1;
        check(first.equals(expected), "mutating byte[] snapshot leaves String unchanged");

        check(Arrays.equals(first.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)), "ordinary String UTF-8 export");
        check(Arrays.equals(first.codePoints().toArray(), expected.codePoints().toArray()),
                "ordinary String code points across mapped leaves");
        check(first.substring(1, first.length() - 1)
                .equals(expected.substring(1, expected.length() - 1)), "ordinary substring view");
        check(stringValueField.get(first) == sentinel, "projections never replace String sentinel");
        check(stringM3Field.get(first) == mappedView, "canonical M3 owner remains authoritative");
    }

    private static Object mappedValue(MappedByteBuffer mapping, long address, String text) throws Throwable {
        int units = text.length();
        Object atom = mappedAtom.invoke(mapping, address, (byte) 2, true, units,
            (byte) 1, text.hashCode(), address + units, 0x4d330000L + units);
        return whole.invoke(atom);
    }

    private static byte[] utf16be(String text) {
        ByteBuffer data = ByteBuffer.allocate(text.length() * 2).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < text.length(); i++) data.putChar(text.charAt(i));
        return data.array();
    }

    private static Object join(Object[] parts) throws Throwable {
        Object typed = Array.newInstance(valueClass, parts.length);
        for (int i = 0; i < parts.length; i++) Array.set(typed, i, parts[i]);
        return concatNative.invoke(typed);
    }

    private static String text(Object view) throws Throwable {
        return new String((char[]) charShadow.invoke(view));
    }

    private static void checkAllLeavesMapped(Object view, MappedByteBuffer mapping) throws Exception {
        Object owner = valueOwner.get(view);
        if (atomClass.isInstance(owner)) {
            check(atomPayloadOwner.get(owner) == mapping, "shares original OS mapping");
        } else if (tupleClass.isInstance(owner)) {
            checkAllLeavesMapped(tupleLeft.get(owner), mapping);
            checkAllLeavesMapped(tupleRight.get(owner), mapping);
        } else {
            throw new AssertionError("unexpected non-mapped owner " + owner.getClass());
        }
    }

    private static void expectNullArray() throws Throwable {
        try {
            concatNative.invoke((Object) null);
            throw new AssertionError("expected null-array exception");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void expectNullPart(Object[] parts) throws Throwable {
        try {
            join(parts);
            throw new AssertionError("expected null-part exception");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void bind() throws Exception {
        valueClass = Class.forName("java.lang.M3String");
        ownerClass = Class.forName("java.lang.M3StringOwner");
        atomClass = Class.forName("java.lang.M3StringAtom");
        tupleClass = Class.forName("java.lang.M3StringTuple");
        Class<?> poolClass = Class.forName("java.lang.M3StringPool");
        MethodHandles.Lookup base = MethodHandles.lookup();
        MethodHandles.Lookup atomLookup = MethodHandles.privateLookupIn(atomClass, base);
        MethodHandles.Lookup valueLookup = MethodHandles.privateLookupIn(valueClass, base);
        MethodHandles.Lookup poolLookup = MethodHandles.privateLookupIn(poolClass, base);
        MethodHandles.Lookup stringLookup = MethodHandles.privateLookupIn(String.class, base);
        ordinaryStringConstructor = stringLookup.findConstructor(String.class,
                MethodType.methodType(void.class, valueClass));
        compactValueProjection = stringLookup.findVirtual(String.class, "value",
                MethodType.methodType(byte[].class));
        mappedAtom = atomLookup.findStatic(atomClass, "mapped", MethodType.methodType(atomClass,
            Object.class, long.class, byte.class, boolean.class, int.class,
            byte.class, int.class, long.class, long.class));
        whole = valueLookup.findStatic(valueClass, "whole",
            MethodType.methodType(valueClass, ownerClass));
        range = valueLookup.findVirtual(valueClass, "slice",
            MethodType.methodType(valueClass, int.class, int.class));
        charShadow = valueLookup.findVirtual(valueClass, "charShadow",
            MethodType.methodType(char[].class));
        hash = valueLookup.findVirtual(valueClass, "hashCodeValue",
            MethodType.methodType(int.class));
        sameCoordinate = valueLookup.findVirtual(valueClass, "sameCoordinate",
            MethodType.methodType(boolean.class, valueClass));
        concatNative = poolLookup.findStatic(poolClass, "concatViewsNative0",
            MethodType.methodType(valueClass, Array.newInstance(valueClass, 0).getClass()));
        valueOwner = valueClass.getDeclaredField("owner");
        atomPayloadOwner = atomClass.getDeclaredField("payloadOwner");
        tupleLeft = tupleClass.getDeclaredField("left");
        tupleRight = tupleClass.getDeclaredField("right");
        stringValueField = String.class.getDeclaredField("value");
        stringM3Field = String.class.getDeclaredField("m3");
        stringValueField.setAccessible(true);
        stringM3Field.setAccessible(true);
        valueOwner.setAccessible(true);
        atomPayloadOwner.setAccessible(true);
        tupleLeft.setAccessible(true);
        tupleRight.setAccessible(true);
    }

    private static void check(boolean success, String why) {
        checks++;
        if (!success) throw new AssertionError(why);
    }
}
