// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ContextImmutablePoolTest {

    @Test
    void canonicalizesEqualExplicitImmutableValues() {
        ContextImmutablePool<String> pool =
                new ContextImmutablePool<>(
                        ImmutableCodec.of(
                                String.class,
                                "test/string-v1",
                                value -> value.getBytes(
                                        StandardCharsets.UTF_8)),
                        16);

        String first = new String("context");
        String second = new String("context");

        assertNotSame(first, second);
        assertSame(first, pool.intern(first));
        assertSame(first, pool.intern(second));
        assertEquals(1, pool.size());
        assertEquals(1L, pool.stats().hits());
    }

    @Test
    void fullPoolDoesNotEvictExistingCanonicalIdentity() {
        ContextImmutablePool<String> pool =
                new ContextImmutablePool<>(
                        ImmutableCodec.of(
                                String.class,
                                "test/bounded-v1",
                                value -> value.getBytes(
                                        StandardCharsets.UTF_8)),
                        1);

        String first = new String("first");
        String second = new String("second");

        assertSame(first, pool.intern(first));
        assertSame(second, pool.intern(second));
        assertSame(first, pool.intern(
                new String("first")));
        assertEquals(1, pool.size());
        assertEquals(1L, pool.stats().rejected());
    }

    @Test
    void unknownMutableTypePassesThroughButStrictPathFails() {
        Mutable value = new Mutable();

        assertSame(value, ImmutablePools.intern(value));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImmutablePools.requireIntern(value));
    }

    @Test
    void automaticallyPoolsDeeplyImmutableRecords() {
        ImmutablePoint first =
                new ImmutablePoint(
                        7,
                        new String("north"));
        ImmutablePoint second =
                new ImmutablePoint(
                        7,
                        new String("north"));

        assertNotSame(first, second);
        assertSame(first, ImmutablePools.intern(first));
        assertSame(first, ImmutablePools.intern(second));
    }

    @Test
    void refusesRecordsThatContainMutableState() {
        MutableRecord value =
                new MutableRecord(
                        new java.util.ArrayList<>());

        assertSame(value, ImmutablePools.intern(value));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImmutablePools.requireIntern(value));
    }

    @Test
    void recordEncodingUsesStateWithoutExecutingCustomAccessors() {
        AccessorRecord first = new AccessorRecord(1);
        AccessorRecord second = new AccessorRecord(2);
        assertSame(first, ImmutablePools.intern(first));
        assertSame(second, ImmutablePools.intern(second));
        assertSame(first, ImmutablePools.intern(new AccessorRecord(1)));
        assertNotSame(first, second);
    }

    private record AccessorRecord(int value) {
        @Override public int value() { throw new AssertionError("accessor must not execute"); }
    }

    private record ImmutablePoint(
            int x,
            String name) {
    }

    private record MutableRecord(
            java.util.List<String> values) {
    }

    private static final class Mutable {
    }
}
