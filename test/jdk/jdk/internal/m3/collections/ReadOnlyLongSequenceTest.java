// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongSequence;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

final class ReadOnlyLongSequenceTest {
    private static final Set<String> WRITES = Set.of(
            "add", "addIf", "addAll", "insert", "set", "removeAt", "removeValue",
            "filterInPlace", "trimToSize", "clear");

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
            LongArrayList owner = empty ? new LongArrayList() : LongArrayList.copyOf(new long[] {7, 0, -1});
            LongSequence sentinel = (LongSequence) Proxy.newProxyInstance(
                    LongSequence.class.getClassLoader(), new Class<?>[] {LongSequence.class},
                    (proxy, target, arguments) -> { throw new AssertionError("unexpected owner dispatch: " + target); });
            var readOnly = new ReadOnlyLongSequence(sentinel);
            int refused = 0;
            for (Method method : LongSequence.class.getMethods()) {
                if (!WRITES.contains(method.getName())) continue;
                refused++;
                var failure = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(readOnly, attempts(method, empty)));
                assertInstanceOf(UnsupportedOperationException.class, failure.getCause(), method.toString());
                var real = new ReadOnlyLongSequence(owner);
                long[] before = owner.toArray();
                assertThrows(InvocationTargetException.class, () -> method.invoke(real, attempts(method, empty)));
                assertArrayEquals(before, owner.toArray());
            }
            assertEquals(10, refused);
        }
    }

    @Test void liveViewTracksOwnerIncludingGrowthAndKeepsPrimitiveTraversal() {
        var owner = LongArrayList.copyOf(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE});
        var readOnly = new ReadOnlyLongSequence(owner);
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
        assertThrows(NullPointerException.class, () -> new ReadOnlyLongSequence(null));
    }
}
