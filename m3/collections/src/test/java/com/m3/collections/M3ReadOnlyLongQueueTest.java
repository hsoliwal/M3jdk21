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

final class M3ReadOnlyLongQueueTest {
    private static final Set<String> WRITES = Set.of(
            "offer", "poll", "tryPoll", "pollReserved", "offerBatch", "drainTo", "clear");

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
            M3LongQueue owner = new M3LongArrayDeque(8);
            if (!empty) for (long value : new long[] {7, 0, -1}) owner.offer(value);
            M3LongQueue sentinel = (M3LongQueue) Proxy.newProxyInstance(
                    M3LongQueue.class.getClassLoader(), new Class<?>[] {M3LongQueue.class},
                    (proxy, target, arguments) -> { throw new AssertionError("unexpected owner dispatch: " + target); });
            var readOnly = new M3ReadOnlyLongQueue(sentinel);
            int refused = 0;
            for (Method method : M3LongQueue.class.getMethods()) {
                if (!WRITES.contains(method.getName())) continue;
                refused++;
                var failure = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(readOnly, attempts(method, empty)));
                assertInstanceOf(UnsupportedOperationException.class, failure.getCause(), method.toString());
                var real = new M3ReadOnlyLongQueue(owner);
                long[] before = owner.toArray();
                assertThrows(InvocationTargetException.class, () -> method.invoke(real, attempts(method, empty)));
                assertArrayEquals(before, owner.toArray());
            }
            assertEquals(7, refused);
        }
    }

    @Test void liveFacadeTracksOwnerAndArrayResultsKeepNativeOwnership() {
        M3LongQueue owner = new M3LongArrayDeque(8);
        owner.offer(Long.MIN_VALUE); owner.offer(0); owner.offer(Long.MAX_VALUE);
        var readOnly = new M3ReadOnlyLongQueue(owner);
        assertArrayEquals(owner.toArray(), readOnly.toArray());
        long[] copy = readOnly.toArray(); copy[0] = 7;
        assertEquals(Long.MIN_VALUE, owner.peek());
        assertEquals(Long.MIN_VALUE, owner.poll()); owner.offer(-9);
        assertArrayEquals(owner.toArray(), readOnly.parallelLongStream().toArray());
        owner.clear(); assertTrue(readOnly.isEmpty());
        assertThrows(NullPointerException.class, () -> new M3ReadOnlyLongQueue(null));
    }
}
