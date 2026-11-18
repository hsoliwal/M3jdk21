/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Canonical M3 String owner byte destinations reject overflowed UTF16 offsets
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3StringOwnerByteDestinationBoundsTest
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.Arrays;

public class M3StringOwnerByteDestinationBoundsTest {
    private static final Class<?> M3;
    private static final Method ADMIT;
    private static final Method ADMIT_ARRAYS;
    private static final Method OWNER;
    private static final Method GET_BYTES;
    private static final Method PACK_IMMEDIATE;
    private static final Method INTERN_IMMEDIATE;

    static {
        try {
            M3 = Class.forName("java.lang.M3String");
            ADMIT = accessible(M3.getDeclaredMethod("admit", char[].class, int.class, int.class));
            ADMIT_ARRAYS = accessible(M3.getDeclaredMethod("admitArrays", char[][].class));
            OWNER = accessible(M3.getDeclaredMethod("owner"));
            GET_BYTES = accessible(Class.forName("java.lang.M3StringOwner")
                    .getDeclaredMethod("getBytes", int.class, int.class, byte[].class,
                            int.class, byte.class));
            Class<?> immediate = Class.forName("java.lang.M3StringImmediate");
            PACK_IMMEDIATE = accessible(immediate.getDeclaredMethod("packChars",
                    char[].class, int.class, int.class));
            INTERN_IMMEDIATE = accessible(immediate.getDeclaredMethod("of", long.class));
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static Object admit(char[] chars) throws Throwable {
        return OWNER.invoke(ADMIT.invoke(null, chars, 0, chars.length));
    }

    private static Object admitTuple(char[] first, char[] second) throws Throwable {
        return OWNER.invoke(ADMIT_ARRAYS.invoke(null, (Object) new char[][] {first, second}));
    }

    private static Object immediate(char[] chars) throws Throwable {
        long packed = (long) PACK_IMMEDIATE.invoke(null, chars, 0, chars.length);
        return INTERN_IMMEDIATE.invoke(null, packed);
    }

    private static void copy(Object owner, int begin, int end, byte[] output,
            int destBegin, byte coder) throws Throwable {
        try {
            GET_BYTES.invoke(owner, begin, end, output, destBegin, coder);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @FunctionalInterface
    interface CheckedAction {
        void run() throws Throwable;
    }

    private static void expect(Class<? extends Throwable> type, CheckedAction action)
            throws Throwable {
        try {
            action.run();
        } catch (Throwable observed) {
            if (type.isInstance(observed)) return;
            throw new AssertionError("Expected " + type + ", saw " + observed, observed);
        }
        throw new AssertionError("Expected " + type + ", operation unexpectedly succeeded");
    }

    private static void verify(Object owner, char[] source, String expectedOwner)
            throws Throwable {
        if (!owner.getClass().getSimpleName().equals(expectedOwner)) {
            throw new AssertionError("Expected " + expectedOwner + " but got " + owner.getClass());
        }
        for (byte coder = 0; coder <= 1; coder++) {
            for (int begin = 0; begin <= source.length; begin++) {
                for (int end = begin; end <= source.length; end++) {
                    int count = end - begin;
                    int offset = 2;
                    byte[] output = new byte[(count + 5) << coder];
                    Arrays.fill(output, (byte) 0x5a);
                    copy(owner, begin, end, output, offset, coder);
                    byte[] expected = new byte[output.length];
                    Arrays.fill(expected, (byte) 0x5a);
                    for (int index = 0; index < count; index++) {
                        char unit = source[begin + index];
                        if (coder == 0) {
                            expected[offset + index] = (byte) unit;
                        } else {
                            int at = (offset + index) * 2;
                            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                                expected[at] = (byte) (unit >>> 8);
                                expected[at + 1] = (byte) unit;
                            } else {
                                expected[at] = (byte) unit;
                                expected[at + 1] = (byte) (unit >>> 8);
                            }
                        }
                    }
                    if (!Arrays.equals(expected, output)) {
                        throw new AssertionError(expectedOwner + " coder=" + coder
                                + " from=" + begin + " to=" + end);
                    }
                }
            }
        }

        // Every bad offset is checked in BYTE coordinates, widened before shifting.
        for (int destinationStart : new int[] {-1, Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1, 1 << 30, Integer.MAX_VALUE}) {
            expect(IndexOutOfBoundsException.class,
                    () -> copy(owner, 0, 1, new byte[8], destinationStart, (byte) 1));
            expect(IndexOutOfBoundsException.class,
                    () -> copy(owner, 0, 0, new byte[8], destinationStart, (byte) 1));
        }
        expect(IndexOutOfBoundsException.class,
                () -> copy(owner, 0, 1, new byte[0], 0, (byte) 0));
        expect(IndexOutOfBoundsException.class,
                () -> copy(owner, 0, 1, new byte[1], 0, (byte) 1));
        expect(IndexOutOfBoundsException.class,
                () -> copy(owner, 0, source.length + 1, new byte[16], 0, (byte) 0));
        expect(IllegalArgumentException.class,
                () -> copy(owner, 0, 1, new byte[8], 0, (byte) -1));
        expect(IllegalArgumentException.class,
                () -> copy(owner, 0, 1, new byte[8], 0, (byte) 2));
        expect(NullPointerException.class,
                () -> copy(owner, 0, 1, null, 0, (byte) 1));
    }

    public static void main(String[] args) throws Throwable {
        char[] shortText = {'a', 'B', '3'};
        char[] longText = {'H', 'i', '\uD800', '\uDC00', '!', 'A', 'B', 'C', 'D'};
        char[] first = {'x', 'y'};
        char[] second = {'\uD800', '\uDC00', 'z'};
        verify(immediate(shortText), shortText, "M3StringImmediate");
        verify(admit(longText), longText, "M3StringAtom");
        verify(admitTuple(first, second), new char[] {'x', 'y', '\uD800', '\uDC00', 'z'},
                "M3StringTuple");
        System.out.println("M3 owner byte destination bounds verified for all three kinds");
    }
}
