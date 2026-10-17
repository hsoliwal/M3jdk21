// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

final class M3ReadOnlyLongListTest {
    private static final Set<String> WRITES = Set.of(
            "add", "addIf", "addAll", "insert", "set", "removeAt", "removeValue",
            "filterInPlace", "trimToSize", "clear", "remove", "sort", "ensureCapacity", "deduplicateSorted");

    private static Object[] attempts(Method method, boolean empty) {
        Object[] arguments = new Object[method.getParameterCount()];
        Class<?>[] types = method.getParameterTypes();
        for (int index = 0; index < types.length; index++) {
            if (types[index] == int.class) arguments[index] = empty ? -1 : 0;
            else if (types[index] == long.class) arguments[index] = Long.MIN_VALUE;
            else if (types[index] == long[].class) arguments[index] = empty ? new long[0] : null;
            else if (types[index] == LongPredicate.class) arguments[index] = (LongPredicate) value -> {
                throw new AssertionError("read-only must refuse before callback");
            };
            else throw new AssertionError("unbound mutator: " + method);
        }
        return arguments;
    }

    @Test void refusesEveryMutationEvenNoopsAndInvalidArgumentsBeforeDispatch() throws Exception {
        for (boolean empty : new boolean[] {true, false}) {
            M3LongArrayList owner = empty ? new M3LongArrayList() : M3LongArrayList.copyOf(new long[] {7, 0, -1});
            M3LongList sentinel = (M3LongList) Proxy.newProxyInstance(
                    M3LongList.class.getClassLoader(), new Class<?>[] {M3LongList.class},
                    (proxy, target, arguments) -> { throw new AssertionError("unexpected owner dispatch: " + target); });
            var readOnly = new M3ReadOnlyLongList(sentinel);
            int refused = 0;
            for (Method method : M3LongList.class.getMethods()) {
                if (!WRITES.contains(method.getName())) continue;
                refused++;
                var failure = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(readOnly, attempts(method, empty)));
                assertInstanceOf(UnsupportedOperationException.class, failure.getCause(), method.toString());
                var real = new M3ReadOnlyLongList(owner);
                long[] before = owner.toArray();
                assertThrows(InvocationTargetException.class, () -> method.invoke(real, attempts(method, empty)));
                assertArrayEquals(before, owner.toArray());
            }
            assertEquals(15, refused);
        }
    }

    @Test void liveViewTracksOwnerIncludingGrowthAndKeepsPrimitiveTraversal() {
        var owner = M3LongArrayList.copyOf(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE});
        var readOnly = new M3ReadOnlyLongList(owner);
        assertArrayEquals(owner.toArray(), readOnly.toArray());
        long[] copy = readOnly.toArray();
        copy[0] = 4;
        assertEquals(Long.MIN_VALUE, owner.get(0));
        Spliterator.OfLong traversal = readOnly.longSpliterator();
        assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED, traversal.characteristics());
        owner.ensureCapacity(8192);
        owner.set(0, 42);
        long[] traversed = new long[3];
        int[] offset = {0};
        traversal.forEachRemaining((long value) -> traversed[offset[0]++] = value);
        assertArrayEquals(owner.toArray(), traversed);
        owner.add(-9);
        assertEquals(4, readOnly.size());
        assertArrayEquals(owner.toArray(), readOnly.parallelLongStream().toArray());
        owner.clear();
        assertTrue(readOnly.isEmpty());
        assertThrows(NullPointerException.class, () -> new M3ReadOnlyLongList(null));
    }
}
