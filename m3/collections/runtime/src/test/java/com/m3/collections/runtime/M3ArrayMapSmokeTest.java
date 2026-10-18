// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.runtime;

import java.util.AbstractMap;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

/** Standalone assertions; run with Java 21 after compiling the runtime sources. */
public final class M3ArrayMapSmokeTest {
    private M3ArrayMapSmokeTest() {}
    public static void main(String[] args) {
        M3ArrayMap<String,Integer> map = new M3ArrayMap<>();
        require(map.put(null, 7) == null);
        require(map.containsKey(null) && map.get(null) == 7);
        require(map.put("a", null) == null && map.containsKey("a"));
        require(map.put("a", 2) == null && map.get("a") == 2);
        for (int i = 0; i < 1000; i++) map.put("key"+i, i);
        require(map.size() == 1002);
        for (int i = 0; i < 1000; i++) require(map.get("key"+i) == i);
        Iterator<Map.Entry<String,Integer>> it = map.entrySet().iterator();
        Map.Entry<String,Integer> first = it.next();
        first.setValue(44);
        require(map.get(first.getKey()) == 44);
        it.remove();
        require(map.size() == 1001);
        require(map.entrySet().size() == 1001);
        Iterator<Map.Entry<String,Integer>> failFast = map.entrySet().iterator();
        map.put("new", 9);
        boolean caught = false;
        try { failFast.next(); } catch (ConcurrentModificationException expected) { caught = true; }
        require(caught);
        for (int i=0;i<1000;i++) map.remove("key"+i);
        map.clear();
        require(map.isEmpty());
        System.out.println("M3ArrayMapSmokeTest PASSED");
    }
    private static void require(boolean ok) {
        if (!ok) throw new AssertionError("M3ArrayMap contract violation");
    }
}
