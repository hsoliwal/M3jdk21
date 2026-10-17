/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ListMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.springframework.util.MultiValueMap;
import org.apache.commons.collections4.ListValuedMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Actual five-provider public API checks using real dependency JARs; not a name-only test. */
final class M3FiveProviderBagContractTest {
    @Test void distinctContractsOverCanonicalM3Owners() {
        Map<String,Integer> jdk=M3LibraryCollections.jdkMap();
        ListMultimap<String,Integer> guava=M3LibraryCollections.guavaListMultimap();
        MutableBagMultimap<String,Integer> eclipse=new M3EclipseBagMultimap<>();
        MultiValueMap<String,Integer> spring=M3LibraryCollections.springMultiValueMap();
        ListValuedMap<String,Integer> apache=M3LibraryCollections.apacheListValuedMap();

        jdk.put("x",1);jdk.put("x",2);
        guava.put("x",1);guava.put("x",1);
        eclipse.put("x",1);eclipse.put("x",1);
        spring.add("x",1);spring.add("x",1);
        apache.put("x",1);apache.put("x",1);

        assertEquals(1,jdk.size());
        assertEquals(2,guava.size());
        assertEquals(2,eclipse.size());
        assertEquals(1,spring.size()); // Spring size is distinct map keys
        assertEquals(2,apache.size());
        assertEquals(2,eclipse.get("x").occurrencesOf(1));
        assertEquals(2,guava.get("x").size());
        assertEquals(2,spring.get("x").size());
        assertEquals(2,apache.get("x").size());
        assertNull(spring.get("missing"));
        assertTrue(guava.get("missing").isEmpty());
        assertTrue(eclipse.get("missing").isEmpty());

        ClassToInstanceMap<Object> classes=M3GuavaClassToInstanceMaps.create();
        classes.putInstance(Integer.class,42);
        assertEquals(42,classes.getInstance(Integer.class));
        assertEquals(1,classes.size());
    }
}
