// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.primitives.Primitives;
import com.m3.collections.M3ArrayList;
import com.m3.collections.M3LinkedHashMap;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Contract discriminators missing from the preserved seeded factory oracle. */
final class M3ClassToInstanceFocusedTest {
    @Test void codeSourcesAndAuthoritativeArrayOwner() throws Exception {
        Path owner = configured("m3.classInstance.ownerClasses");
        Path artifact = configured("m3.classInstance.guavaJar");
        source(M3GuavaClassToInstanceMaps.class, owner);
        source(M3GuavaClassToInstanceMapsSelfTest.class, owner);
        for (Class<?> type : List.of(ClassToInstanceMap.class, MutableClassToInstanceMap.class,
                Primitives.class, M3LinkedHashMap.class, M3ArrayList.class)) source(type, artifact);
        var map = M3GuavaClassToInstanceMaps.<Object>create();
        Object value = new StringBuilder("payload");
        map.putInstance(StringBuilder.class, (StringBuilder) value);
        Object storage = backing(map);
        assertInstanceOf(M3LinkedHashMap.class, storage);
        Object table = field(storage, "table");
        assertEquals(Object[].class, table.getClass().getDeclaredField("keys").getType());
        assertEquals(Object[].class, table.getClass().getDeclaredField("values").getType());
        assertEquals(int[].class, table.getClass().getDeclaredField("index").getType());
        assertTrue(Arrays.stream((Object[]) field(table, "keys")).anyMatch(k -> k == StringBuilder.class));
        assertTrue(Arrays.stream((Object[]) field(table, "values")).anyMatch(v -> v == value));
    }

    @Test void exactPrimitiveWrapperAndVoidTokensRemainIndependent() {
        var map = M3GuavaClassToInstanceMaps.<Object>create();
        map.putInstance(Object.class, "object");
        map.putInstance(String.class, "string");
        map.putInstance(int.class, 7);
        map.putInstance(Integer.class, 9);
        map.putInstance(void.class, null);
        assertEquals("object", map.getInstance(Object.class));
        assertEquals("string", map.getInstance(String.class));
        assertEquals(7, map.getInstance(int.class));
        assertEquals(9, map.getInstance(Integer.class));
        assertTrue(map.containsKey(void.class));
        assertNull(map.getInstance(void.class));
        assertNull(map.getInstance(Number.class));
        assertEquals(5, map.size());
        assertEquals(7, map.putInstance(int.class, null));
        assertTrue(map.containsKey(int.class));
        assertNull(map.getInstance(int.class));
        assertEquals(9, map.getInstance(Integer.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void nullClassAndWrongTypeWritesRefuseBeforeEffect() {
        var map = M3GuavaClassToInstanceMaps.<Number>create();
        map.putInstance(Integer.class, 1);
        Map<Class<? extends Number>, Number> before = new HashMap<>(map);
        Map raw = map;
        assertThrows(ClassCastException.class, () -> raw.put(Double.class, 2L));
        assertThrows(ClassCastException.class, () -> raw.put("not-a-Class", 3));
        assertThrows(NullPointerException.class, () -> raw.put(null, 4));
        assertThrows(NullPointerException.class, () -> map.getInstance(null));
        assertEquals(before, map);
        assertNull(map.get(null));
        assertFalse(map.containsKey(null));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void everyEntryProjectionChecksSetValue() {
        var map = M3GuavaClassToInstanceMaps.<Number>create();
        map.putInstance(Integer.class, 1);
        List<Map.Entry<?, ?>> projected = new ArrayList<>();
        projected.add(map.entrySet().iterator().next());
        projected.add((Map.Entry<?, ?>) map.entrySet().toArray()[0]);
        projected.add(map.entrySet().toArray(new Map.Entry[0])[0]);
        projected.add(map.entrySet().stream().findFirst().orElseThrow());
        assertTrue(map.entrySet().spliterator().tryAdvance(projected::add));
        for (Map.Entry<?, ?> entry : projected) {
            assertThrows(ClassCastException.class, () -> ((Map.Entry) entry).setValue(2L));
            assertEquals(Map.of(Integer.class, 1), map);
        }
        Map.Entry<Class<? extends Number>, Number> live = map.entrySet().iterator().next();
        assertEquals(1, live.setValue(5));
        assertEquals(5, map.getInstance(Integer.class));
    }

    @Test void putAllValidatesAllTypesBeforePublishing() {
        var map = M3GuavaClassToInstanceMaps.<Number>create();
        map.putInstance(Integer.class, 1);
        Map<Class<? extends Number>, Number> incoming = new LinkedHashMap<>();
        incoming.put(Long.class, 2L);
        incoming.put(Double.class, 3L);
        assertThrows(ClassCastException.class, () -> map.putAll(incoming));
        assertEquals(Map.of(Integer.class, 1), map);
        incoming.remove(Double.class);
        incoming.put(null, 4);
        assertThrows(NullPointerException.class, () -> map.putAll(incoming));
        assertEquals(Map.of(Integer.class, 1), map);
        map.putAll(map);
        assertEquals(Map.of(Integer.class, 1), map);
    }

    @Test void defaultsRemainCheckedAndRemovalViewsStayLive() {
        var map = M3GuavaClassToInstanceMaps.<Number>create();
        var keys = map.keySet();
        var values = map.values();
        map.putInstance(Integer.class, 1);
        assertThrows(ClassCastException.class, () -> map.compute(Integer.class, (k, v) -> 2L));
        assertThrows(ClassCastException.class, () -> map.merge(Integer.class, 2L, (a, b) -> b));
        assertThrows(ClassCastException.class, () -> map.replaceAll((k, v) -> 2L));
        assertEquals(Map.of(Integer.class, 1), map);
        map.putInstance(Long.class, null);
        assertEquals(2, keys.size());
        assertTrue(values.remove(null));
        assertFalse(map.containsKey(Long.class));
        var iterator = map.entrySet().iterator();
        iterator.next();
        iterator.remove();
        assertTrue(map.isEmpty());
        assertTrue(keys.isEmpty());
        assertTrue(values.isEmpty());
    }

    @Test void factoryCallsOwnIndependentMaps() throws Exception {
        var first = M3GuavaClassToInstanceMaps.<Object>create();
        var second = M3GuavaClassToInstanceMaps.<Object>create();
        assertNotSame(first, second);
        assertNotSame(backing(first), backing(second));
        assertNotSame(field(backing(first), "table"), field(backing(second), "table"));
        first.putInstance(String.class, "first");
        assertTrue(second.isEmpty());
    }

    private static Path configured(String name) throws Exception {
        String value = System.getProperty(name);
        assertNotNull(value, name);
        return Path.of(value).toRealPath();
    }
    private static void source(Class<?> type, Path expected) throws Exception {
        var code = type.getProtectionDomain().getCodeSource();
        assertNotNull(code, type.getName());
        assertEquals(expected, Path.of(code.getLocation().toURI()).toRealPath(), type.getName());
    }
    private static Object backing(ClassToInstanceMap<?> map) throws Exception {
        assertInstanceOf(MutableClassToInstanceMap.class, map);
        return field(map, "delegate");
    }
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
