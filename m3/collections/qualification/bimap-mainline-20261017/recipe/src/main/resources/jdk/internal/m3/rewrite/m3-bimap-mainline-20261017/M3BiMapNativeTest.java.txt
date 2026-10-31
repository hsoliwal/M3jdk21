/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A real JNI client of the Java BiMap interface; not a native acceleration implementation. */
final class M3BiMapNativeTest {
    private static native Object call(BiMap<?, ?> map, Object key, Object value, int operation);
    @BeforeAll static void load() { System.load(Objects.requireNonNull(System.getProperty("m3.bimap.native"))); }
    @Test void nativePutForceRemoveAndInverseGetMatchDonor() {
        BiMap<Integer, Integer> map = M3BiMap.create(), donor = HashBiMap.create();
        for (int i = 0; i < 2048; i++) assertEquals(call(donor, i, i + 10000, 0), call(map, i, i + 10000, 0));
        for (int i = 0; i < 2048; i++) assertEquals(i, call(map, i + 10000, null, 3));
        for (int i = 0; i < 1024; i++) assertEquals(call(donor, i, i + 11024, 1), call(map, i, i + 11024, 1));
        for (int i = 0; i < 1024; i += 2) assertEquals(call(donor, i, null, 2), call(map, i, null, 2));
        assertEquals(donor, map); assertEquals(donor.inverse(), map.inverse());
    }
    @Test void nativeNullsAndDuplicateRefusal() {
        var map = M3BiMap.<Object, Object>create(); call(map, null, "null-key", 0); call(map, "null-value", null, 0);
        assertNull(call(map, "null-key", null, 3));
        assertEquals("null-value", call(map, null, null, 3));
        assertThrows(IllegalArgumentException.class, () -> call(map, "conflict", null, 0));
        assertEquals(2, map.size()); assertEquals("null-key", call(map, null, null, 2));
        assertFalse(map.inverse().containsKey("null-key"));
    }
    @Test void nativeCallbackExceptionIdentityIsPreserved() {
        var map = M3BiMap.<Object, Object>create(); RuntimeException marker = new RuntimeException("hash callback");
        Object key = new Object() { @Override public int hashCode() { throw marker; } };
        assertSame(marker, assertThrows(RuntimeException.class, () -> call(map, key, 1, 0)));
        assertTrue(map.isEmpty()); assertTrue(map.inverse().isEmpty());
    }
    @Test void nativeValidationAndInverseReceiver() {
        assertThrows(NullPointerException.class, () -> call(null, 1, 2, 0));
        var map = M3BiMap.<Integer, String>create();
        assertThrows(IllegalArgumentException.class, () -> call(map, 1, "x", 7));
        call(map.inverse(), "x", 1, 0); assertEquals(Map.of(1, "x"), map);
    }
}
