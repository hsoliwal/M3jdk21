// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ImmutablePoolTest {
    record Point(int x, int y) { }

    @Test void canonicalizesAnExplicitImmutableValueAndResolvesScopedIds() {
        try (var pool = new ImmutablePool<>(Point.class, 3, 24, value -> 8)) {
            Point first = new Point(1, 2);
            assertSame(first, pool.intern(first));
            assertSame(first, pool.intern(new Point(1, 2)));
            var id = pool.internId(new Point(1, 2));
            assertSame(first, pool.resolve(id));
            assertEquals(0, id.index());
            assertEquals(1, pool.internId(new Point(2, 3)).index());
            assertEquals(2, pool.stats().entries());
            assertEquals(16, pool.stats().retainedWeight());
        }
    }

    @Test void budgetsDoNotEvictOrReassignIds() {
        var pool = new ImmutablePool<>(String.class, 1, 3, String::length);
        var id = pool.internId("abc");
        assertThrows(IllegalStateException.class, () -> pool.intern("d"));
        assertEquals("abc", pool.resolve(id));
        assertEquals("abc", pool.intern(new String("abc")));
        var other = new ImmutablePool<>(String.class, 1, 3, String::length);
        assertThrows(IllegalArgumentException.class, () -> other.resolve(id));
        pool.clear();
        assertThrows(IllegalStateException.class, () -> pool.resolve(id));
        assertNotEquals(id, pool.internId("abc"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.intern("abc"));
        assertEquals(0, pool.stats().retainedWeight());
        pool.close();
    }

    @Test void weightAndExactTypeAreChecked() {
        var bounded = new ImmutablePool<>(String.class, 3, 2, String::length);
        assertThrows(IllegalStateException.class, () -> bounded.intern("abc"));
        assertEquals(0, bounded.stats().entries());
        var invalid = new ImmutablePool<>(String.class, 3, 2, value -> -1);
        assertThrows(IllegalArgumentException.class, () -> invalid.intern("x"));
        var objects = new ImmutablePool<>(Number.class, 3, 3, value -> 1);
        assertThrows(IllegalArgumentException.class, () -> objects.intern(1));
        assertThrows(NullPointerException.class, () -> bounded.intern(null));
    }

    @Test void concurrentAdmissionPublishesOneCanonicalReference() throws Exception {
        try (var pool = new ImmutablePool<>(Point.class, 10, 100, value -> 1);
             var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Point>> tasks = new ArrayList<>();
            for (int i = 0; i < 400; i++) tasks.add(() -> pool.intern(new Point(7, 9)));
            var futures = executor.invokeAll(tasks);
            Point canonical = futures.getFirst().get();
            for (var future : futures) assertSame(canonical, future.get());
            assertEquals(1, pool.stats().entries());
        }
    }

    @Test void registryPoolsOnlyRegisteredExactClasses() {
        ValuePools pools = ValuePools.standard(20, 1_000);
        String first = new String("same");
        assertSame(first, pools.canonicalize(first));
        assertSame(first, pools.canonicalize(new String("same")));
        List<String> mutable = new ArrayList<>(List.of("a"));
        assertSame(mutable, pools.canonicalize(mutable));
        assertFalse(pools.contains(mutable.getClass()));
        assertThrows(IllegalArgumentException.class, () -> pools.with(
                new ImmutablePool<>(String.class, 1, 1, value -> 1)));
        assertNull(pools.canonicalize(null));
    }
}
