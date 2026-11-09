/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Composite cursors preserve clipped UTF-16 coordinates and reuse array-backed pending ranges
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCursorContinuationTest
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCursorContinuationTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Exercises actual target atoms/tuples, rather than accepting stock-String oracle execution. */
public class M3StringCursorContinuationTest {
    private static final Class<?> VALUE;
    private static final Class<?> OWNER;
    private static final Method ADMIT;
    private static final Method WHOLE;
    private static final Method RANGE;
    private static final Method EMPTY;
    private static final Method START;
    private static final Field VALUE_OWNER;
    private static final Constructor<?> TUPLE;
    private static final Constructor<?> CURSOR;
    private static final Method AT;
    private static final Field PENDING;
    private static final Field PENDING_SIZE;
    private static final Field BEGIN;
    private static final Field END;
    private static long checks;
    private static long identity;

    static {
        try {
            VALUE = Class.forName("java.lang.M3String");
            OWNER = Class.forName("java.lang.M3StringOwner");
            ADMIT = method(VALUE, "admit", char[].class, int.class, int.class);
            WHOLE = method(VALUE, "whole", OWNER);
            RANGE = method(VALUE, "range", OWNER, int.class, int.class);
            EMPTY = method(VALUE, "empty");
            START = method(VALUE, "start");
            VALUE_OWNER = field(VALUE, "owner");
            TUPLE = Class.forName("java.lang.M3StringTuple")
                    .getDeclaredConstructor(VALUE, VALUE, long.class, long.class);
            TUPLE.setAccessible(true);
            Class<?> cursor = Class.forName("java.lang.M3String$CodeUnitCursor");
            CURSOR = cursor.getDeclaredConstructor(VALUE);
            CURSOR.setAccessible(true);
            AT = method(cursor, "at", int.class);
            PENDING = field(cursor, "pendingOwners");
            PENDING_SIZE = field(cursor, "pendingSize");
            BEGIN = field(cursor, "logicalBegin");
            END = field(cursor, "logicalEnd");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        emptyAndScalar();
        clippedIntervalsAndSeams();
        deepContinuationAndAbandonedSlots();
        randomizedCoordinates();
        publicConsumerSeams();
        System.out.println("M3_STRING_CURSOR_CONTINUATION_PASS checks=" + checks);
    }

    private static void emptyAndScalar() throws Exception {
        Object empty = cursor(new Value(EMPTY.invoke(null), new char[0]));
        expectBounds(empty, -1);
        expectBounds(empty, 0);

        Value scalar = leaf(new char[] {'x', '\u0100', '\ud800', '\0', '\udfff', 'y'});
        Value clipped = slice(scalar, 1, 5);
        Object reader = cursor(clipped);
        check(read(reader, 2) == clipped.units[2], "middle scalar access");
        check(BEGIN.getInt(reader) == 0 && END.getInt(reader) == clipped.units.length,
                "cache the complete visible scalar interval");
        check(PENDING.get(reader) == null, "scalar cursor has no stack arrays");
        verifyAll(clipped);
    }

    private static void clippedIntervalsAndSeams() throws Exception {
        Value left = leaf(new char[] {'!', 'a', '\ud83d', '?'});
        Value right = leaf(new char[] {'!', '\ude42', 'b', '?'});
        Value pair = join(slice(left, 1, 3), slice(right, 1, 3));
        Value tail = leaf(new char[] {'#', '\ud800', 'c', '\udfff', '#'});
        Value nested = slice(join(slice(pair, 1, 4), slice(tail, 1, 4)), 0, 5);
        verifyAll(pair);
        verifyAll(nested);
        Value reused = join(nested, nested);
        verifyAll(slice(reused, 1, reused.units.length - 1));
        check(pair.units[1] == '\ud83d' && pair.units[2] == '\ude42',
                "surrogate pair crosses two distinct owner ranges");

        Value wideLeft = leaf(new char[] {'!', 'a', 'b', 'c', '?'});
        Value wideRight = leaf(new char[] {'!', 'd', 'e', 'f', '?'});
        Value wide = slice(join(slice(wideLeft, 1, 4), slice(wideRight, 1, 4)), 1, 5);
        Object reader = cursor(wide);
        check(read(reader, 1) == 'c', "seek inside clipped first leaf");
        check(BEGIN.getInt(reader) == 0 && END.getInt(reader) == 2,
                "left boundary is clipped to the top-level view");
        check(read(reader, 2) == 'd', "continue into clipped right leaf");
        check(BEGIN.getInt(reader) == 2 && END.getInt(reader) == 4,
                "right boundary is clipped to the top-level view");
        noPendingOwners(reader);
        check(read(reader, 0) == 'b', "backward transition after stack exhaustion");
    }

    private static void deepContinuationAndAbandonedSlots() throws Exception {
        Value unit = leaf(new char[] {'\u0101'});
        Value deep = unit;
        for (int index = 0; index < 96; index++) {
            deep = join(deep, leaf(new char[] {(char) (0x0200 + index)}));
        }
        Object reader = cursor(deep);
        check(read(reader, 0) == deep.units[0], "deep first read");
        check(PENDING.get(reader) == null, "first lookup does not allocate continuation lanes");
        check(read(reader, 1) == deep.units[1], "second adjacent scalar establishes continuation");
        check(PENDING_SIZE.getInt(reader) > 8, "fixture forces stack growth");
        for (int index = 2; index < deep.units.length; index++) {
            check(read(reader, index) == deep.units[index], "deep sequential access");
        }
        noPendingOwners(reader);

        read(reader, 0);
        read(reader, 1);
        check(PENDING_SIZE.getInt(reader) > 0, "pending owners established again");
        read(reader, deep.units.length - 1);
        noPendingOwners(reader);
        verifyAll(deep);
    }

    private static void randomizedCoordinates() throws Exception {
        Random random = new Random(0x4d33435552534f52L);
        char[] alphabet = {'a', 'b', '\0', '\u00e9', '\u0100', '\ud83d', '\ude42', '\ud800', '\udfff'};
        for (int trial = 0; trial < 400; trial++) {
            Value current = leaf(new char[] {'z'});
            for (int piece = 0; piece < 12; piece++) {
                char[] payload = new char[3 + random.nextInt(10)];
                for (int index = 0; index < payload.length; index++) {
                    payload[index] = alphabet[random.nextInt(alphabet.length)];
                }
                Value next = leaf(payload);
                int begin = random.nextInt(payload.length);
                int end = begin + 1 + random.nextInt(payload.length - begin);
                next = slice(next, begin, end);
                current = random.nextBoolean() ? join(current, next) : join(next, current);
                if (piece % 4 == 3 && current.units.length > 2) {
                    int from = random.nextInt(current.units.length - 1);
                    int to = from + 1 + random.nextInt(current.units.length - from);
                    current = slice(current, from, to);
                }
            }
            verifyAll(current);
            Object reader = cursor(current);
            for (int index = 0; index < 120; index++) {
                int at = random.nextInt(current.units.length);
                check(read(reader, at) == current.units[at], "random coordinate");
                check(read(reader, at) == current.units[at], "repeated coordinate");
            }
            int middle = current.units.length / 2;
            read(reader, middle);
            int begin = BEGIN.getInt(reader);
            int end = END.getInt(reader);
            int pending = PENDING_SIZE.getInt(reader);
            expectBounds(reader, -1);
            expectBounds(reader, current.units.length);
            expectBounds(reader, Integer.MAX_VALUE);
            check(BEGIN.getInt(reader) == begin && END.getInt(reader) == end
                            && PENDING_SIZE.getInt(reader) == pending,
                    "invalid index leaves valid cursor state intact");
            check(read(reader, middle) == current.units[middle], "recovery after bounds rejection");
        }
    }

    private static void publicConsumerSeams() {
        String text = String.join("", "ab", "\ud83d", "\ude42", "cd", "\ud800", "ef");
        char[] expected = {'a', 'b', '\ud83d', '\ude42', 'c', 'd', '\ud800', 'e', 'f'};
        check(Arrays.equals(text.chars().toArray(), unitsAsInts(expected)), "chars consumer");
        check(Arrays.equals(text.codePoints().toArray(),
                new int[] {'a', 'b', 0x1f642, 'c', 'd', 0xd800, 'e', 'f'}), "codePoints seam");
        check(text.indexOf(String.join("", "b", "\ud83d", "\ude42", "c")) == 1,
                "literal search crosses scalar seams");
        check(text.lastIndexOf("ef") == 7, "reverse search remains supported");
    }

    private static int[] unitsAsInts(char[] source) {
        int[] output = new int[source.length];
        for (int index = 0; index < source.length; index++) output[index] = source[index];
        return output;
    }

    private static void verifyAll(Value value) throws Exception {
        Object reader = cursor(value);
        for (int index = 0; index < value.units.length; index++) {
            check(read(reader, index) == value.units[index], "forward UTF-16 coordinate");
        }
        noPendingOwners(reader);
        for (int index = value.units.length - 1; index >= 0; index--) {
            check(read(reader, index) == value.units[index], "reverse UTF-16 coordinate");
        }
        for (int index = 0; index < value.units.length; index++) {
            int other = value.units.length - index - 1;
            check(read(reader, other) == value.units[other], "alternating high coordinate");
            check(read(reader, index) == value.units[index], "alternating low coordinate");
        }
    }

    private static void noPendingOwners(Object cursor) throws Exception {
        check(PENDING_SIZE.getInt(cursor) == 0, "no pending ranges after final scalar");
        Object[] pending = (Object[]) PENDING.get(cursor);
        if (pending != null) {
            for (Object owner : pending) check(owner == null, "consumed/abandoned owner slot cleared");
        }
    }

    private static Value leaf(char[] units) throws Exception {
        Object value = ADMIT.invoke(null, units, 0, units.length);
        check(value != null, "actual canonical atom admission");
        return new Value(value, units.clone());
    }

    private static Value slice(Value source, int begin, int end) throws Exception {
        Object value = RANGE.invoke(null, VALUE_OWNER.get(source.value),
                (Integer) START.invoke(source.value) + begin, end - begin);
        return new Value(value, Arrays.copyOfRange(source.units, begin, end));
    }

    private static Value join(Value left, Value right) throws Exception {
        long id = ++identity;
        Object owner = TUPLE.newInstance(left.value, right.value, id, id);
        char[] expected = Arrays.copyOf(left.units, left.units.length + right.units.length);
        System.arraycopy(right.units, 0, expected, left.units.length, right.units.length);
        return new Value(WHOLE.invoke(null, owner), expected);
    }

    private static Object cursor(Value value) throws Exception {
        return CURSOR.newInstance(value.value);
    }

    private static char read(Object cursor, int index) throws Exception {
        return (Character) AT.invoke(cursor, index);
    }

    private static void expectBounds(Object cursor, int index) throws Exception {
        try {
            AT.invoke(cursor, index);
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IndexOutOfBoundsException, "exact bounds category");
            return;
        }
        throw new AssertionError("missing bounds rejection: " + index);
    }

    private static Method method(Class<?> owner, String name, Class<?>... arguments)
            throws ReflectiveOperationException {
        Method result = owner.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private record Value(Object value, char[] units) {}
}
