/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.ListMultimap;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

/** Real JDK/Guava/Eclipse/Spring/Commons five-list family contract. */
final class M3FiveListFamiliesTest {
    @Test void allFiveRealInterfacesAndSingleReferencePayloadOwner() {
        M3FiveListFamiliesContract.run();
        Map<String,List<Integer>> jdk = M3FiveListFamilies.jdk();
        ListMultimap<String,Integer> guava = M3FiveListFamilies.guava();
        MutableListMultimap<String,Integer> eclipse = M3FiveListFamilies.eclipse();
        MultiValueMap<String,Integer> spring = M3FiveListFamilies.spring();
        ListValuedMap<String,Integer> apache = M3FiveListFamilies.apache();
        assertInstanceOf(M3LinkedHashMap.class,jdk);
        assertInstanceOf(M3GuavaListMultimap.class,guava);
        assertInstanceOf(M3EclipseListMultimap.class,eclipse);
        assertInstanceOf(M3SpringArrayMultiValueMap.class,spring);
        assertInstanceOf(M3ApacheListValuedMap.class,apache);
        jdk.put("k",new M3LinkedList<>());
        for (int value : new int[]{2,2,4}) {
            jdk.get("k").add(value);
            guava.put("k",value);
            eclipse.put("k",value);
            spring.add("k",value);
            apache.put("k",value);
        }
        assertEquals(List.of(2,2,4),jdk.get("k"));
        assertEquals(jdk.get("k"),guava.get("k"));
        assertEquals(jdk.get("k"),eclipse.get("k"));
        assertEquals(jdk.get("k"),spring.get("k"));
        assertEquals(jdk.get("k"),apache.get("k"));
        assertEquals(1,jdk.size());
        assertEquals(1,spring.size());
        assertEquals(3,guava.size());
        assertEquals(3,eclipse.size());
        assertEquals(3,apache.size());
        guava.get("k").remove(0);
        assertEquals(2,guava.size());
        assertEquals(3,spring.get("k").size());
        assertEquals(3,apache.size());
    }
}
