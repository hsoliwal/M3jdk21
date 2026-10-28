/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** SDK registration of an existing M3 array/index owner, not another casefold engine. */
final class M3SpringCaseInsensitiveSdkFactoryTest {
    @Test void correctSpringMappedContractAndExistingM3Owner() {
        Map<String, Integer> map = M3LibraryCollections.springCaseInsensitiveMap(Locale.ROOT);
        assertInstanceOf(M3SpringCaseInsensitiveMap.class, map);
        map.put("Header", null);
        assertTrue(map.containsKey("hEaDeR"));
        assertFalse(map.keySet().remove("HEADER")); // Spring 6.2 null-valued removal result
        assertFalse(map.containsKey("Header"));
        map.put("A", 1);
        map.putIfAbsent("a", 2);
        assertEquals(1, map.get("A"));
        assertEquals("A", map.keySet().iterator().next());
        map.put("a", 3);
        assertEquals(3, map.get("A"));
        assertEquals("a", map.keySet().iterator().next());
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test void exactSdkDiscoveryAndFactoryDescriptor() throws Exception {
        var owners = M3LibraryBindings.forFamily(M3LibraryBinding.Family.SPRING_FRAMEWORK);
        assertEquals(4, owners.size());
        var binding = owners.stream()
                .filter(b -> b.factoryName().equals("springCaseInsensitiveMap"))
                .findFirst().orElseThrow();
        assertEquals("java.util.Map", binding.contractType());
        assertEquals("com.m3.collections.interop.M3SpringCaseInsensitiveMap", binding.adapterType());
        assertEquals("springCaseInsensitiveMap(java.util.Locale)", binding.descriptor());
        assertEquals(M3LibraryBindings.all().size(),
                M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));
    }
}
