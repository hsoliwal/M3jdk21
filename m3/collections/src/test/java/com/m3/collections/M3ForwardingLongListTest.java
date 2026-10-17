// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

final class M3ForwardingLongListTest {
    private static M3LongList wrap(M3LongList owner) {
        return new M3ForwardingLongList() {
            @Override protected M3LongList delegate() { return owner; }
        };
    }

    private static Object[] arguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] values = new Object[types.length];
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            if (type == int.class) values[index] = 0;
            else if (type == long.class) values[index] = Long.MIN_VALUE;
            else if (type == long[].class) values[index] = new long[] {0, Long.MAX_VALUE};
            else if (type == LongPredicate.class) values[index] = (LongPredicate) value -> value >= 0;
            else if (type == LongConsumer.class) values[index] = (LongConsumer) value -> { };
            else throw new AssertionError("unbound method input: " + method);
        }
        return values;
    }

    private static String result(Object value) {
        if (value instanceof long[] array) return Arrays.toString(array);
        if (value instanceof M3LongIterator iterator) {
            var elements = new ArrayList<Long>();
            iterator.forEachRemaining(elements::add);
            return elements.toString();
        }
        if (value instanceof Spliterator.OfLong iterator) {
            var elements = new ArrayList<Long>();
            iterator.forEachRemaining((long element) -> elements.add(element));
            return elements.toString();
        }
        if (value instanceof LongStream stream) {
            try (stream) { return Arrays.toString(stream.toArray()); }
        }
        return String.valueOf(value);
    }

    @Test void everyInterfaceMethodUsesExactlyOneNativeDispatch() throws Exception {
        Method[] methods = M3LongList.class.getMethods();
        assertEquals(29, methods.length);
        for (Method method : methods) {
            M3LongArrayList owner = M3LongArrayList.copyOf(new long[] {-1, 0, Long.MAX_VALUE});
            M3LongArrayList reference = M3LongArrayList.copyOf(owner.toArray());
            var calls = new ArrayList<String>();
            M3LongList traced = (M3LongList) Proxy.newProxyInstance(
                    M3LongList.class.getClassLoader(), new Class<?>[] {M3LongList.class},
                    (proxy, target, parameters) -> {
                        calls.add(target.getName());
                        try { return target.invoke(owner, parameters); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            String expected = result(method.invoke(reference, arguments(method)));
            String actual = result(method.invoke(wrap(traced), arguments(method)));
            assertEquals(expected, actual, method.toString());
            assertEquals(List.of(method.getName()), calls, method.toString());
            assertArrayEquals(reference.toArray(), owner.toArray(), method.toString());
        }
    }

    @Test void partialFilterCommitsAndOriginalFailureSurvive() {
        for (int throwingIndex = 0; throwingIndex < 16; throwingIndex++) {
            long[] input = new long[16];
            for (int index = 0; index < input.length; index++) input[index] = index - 8;
            M3LongArrayList owner = M3LongArrayList.copyOf(input);
            M3LongList decorated = wrap(owner);
            RuntimeException expected = new IllegalArgumentException("predicate sentinel");
            int stop = throwingIndex;
            var calls = new int[1];
            RuntimeException actual = assertThrows(RuntimeException.class,
                    () -> decorated.filterInPlace(value -> {
                        if (calls[0]++ == stop) throw expected;
                        return (value & 1) == 0;
                    }));
            assertSame(expected, actual);
            long[] expectedState = LongStream.concat(
                    Arrays.stream(input, 0, stop).filter(value -> (value & 1) == 0),
                    Arrays.stream(input, stop, input.length)).toArray();
            assertArrayEquals(expectedState, decorated.toArray());
            assertEquals(stop + 1, calls[0]);
        }
    }

    @Test void spliteratorKeepsNativeSplittingAndOwnerAfterGrowth() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[] {1, 2, 3, 4});
        M3LongList decorated = wrap(owner);
        Spliterator.OfLong right = decorated.longSpliterator();
        Spliterator.OfLong left = right.trySplit();
        assertNotNull(left);
        assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
                right.characteristics());
        owner.ensureCapacity(8192);
        owner.set(0, 99);
        var values = new ArrayList<Long>();
        left.forEachRemaining((long value) -> values.add(value));
        right.forEachRemaining((long value) -> values.add(value));
        assertEquals(List.of(99L, 2L, 3L, 4L), values);
        assertArrayEquals(new long[] {99, 2, 3, 4}, decorated.parallelLongStream().toArray());
    }

    @Test void bulkAndReturnedArraysRetainNativeOwnership() {
        M3LongList decorated = wrap(new M3LongArrayList());
        long[] input = {Long.MIN_VALUE, 0, Long.MAX_VALUE};
        decorated.addAll(input);
        input[0] = 7;
        long[] copy = decorated.toArray();
        copy[1] = 8;
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, decorated.toArray());
        assertThrows(NullPointerException.class, () -> decorated.addAll(null));
        assertEquals(3, decorated.size());
    }
}
