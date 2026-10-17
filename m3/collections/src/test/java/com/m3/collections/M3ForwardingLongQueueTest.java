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

final class M3ForwardingLongQueueTest {
    private static M3LongQueue owner(long[] values) {
        M3LongQueue owner = new M3LongArrayDeque(8);
        for (long value : values) if (!owner.offer(value)) throw new AssertionError("fixture capacity");
        return owner;
    }

    private static M3LongQueue wrap(M3LongQueue owner) {
        return new M3ForwardingLongQueue() {
            @Override protected M3LongQueue delegate() { return owner; }
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
        Method[] methods = M3LongQueue.class.getMethods();
        assertEquals(18, methods.length);
        for (Method method : methods) {
            M3LongQueue owner = owner(new long[] {-1, 0, Long.MAX_VALUE});
            M3LongQueue reference = owner(owner.toArray());
            var calls = new ArrayList<String>();
            M3LongQueue traced = (M3LongQueue) Proxy.newProxyInstance(
                    M3LongQueue.class.getClassLoader(), new Class<?>[] {M3LongQueue.class},
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


    @Test void everyMethodPreservesRuntimeAndErrorIdentity() throws Exception {
        for (boolean error : new boolean[] {false, true}) for (Method method : M3LongQueue.class.getMethods()) {
            Throwable marker = error ? new AssertionError("native error") : new IllegalArgumentException("native runtime");
            int[] calls = {0};
            M3LongQueue owner = (M3LongQueue) Proxy.newProxyInstance(M3LongQueue.class.getClassLoader(),
                    new Class<?>[] {M3LongQueue.class}, (proxy, target, parameters) -> {
                assertEquals(method, target); calls[0]++; throw marker;
            });
            var thrown = assertThrows(InvocationTargetException.class,
                    () -> method.invoke(wrap(owner), arguments(method)));
            assertSame(marker, thrown.getCause()); assertEquals(1, calls[0]);
        }
    }
}
