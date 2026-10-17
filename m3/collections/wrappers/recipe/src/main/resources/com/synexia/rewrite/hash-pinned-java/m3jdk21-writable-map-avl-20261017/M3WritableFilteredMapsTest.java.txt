// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** JUnit provider lane for owning Maven module; standalone proof runs separately. */
class M3WritableFilteredMapsTest {
    @Test void rejectedMappingsNeverAffectHiddenEntries() {
        var source = new LinkedHashMap<Integer,String>();
        source.put(1,"hidden");source.put(2,"two");
        Map<Integer,String> view=M3WritableFilteredMaps.filtered(source,(key,value)->key%2==0);
        assertEquals("two",view.put(2,"new"));
        assertEquals("new",source.get(2));
        assertThrows(IllegalArgumentException.class,()->view.put(1,"bad"));
        assertFalse(view.containsKey(1));
        assertEquals("hidden",source.get(1));
        assertEquals(null,view.remove(1));
        assertEquals("hidden",source.get(1));
        assertTrue(view.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(2,"new")));
        assertFalse(source.containsKey(2));
    }
    @Test void writableNavigationAndReadOnlyPredecessorRemainSeparated() {
        var source=new TreeMap<Integer,Integer>();
        for(int i=0;i<9;i++)source.put(i,i);
        var writable=M3WritableFilteredMaps.filtered(source,(key,value)->key%2==0);
        assertEquals(6,writable.floorKey(7));
        assertEquals(2,writable.subMap(2,true,7,false).pollFirstEntry().getKey());
        assertFalse(source.containsKey(2));
        // Existing M3JDK21 owner admits guarded visible writes; Synexia's
        // read-only predecessor does not. Preserve the JDK receiver contract.
        var existing=M3FilteredMaps.filtered(source,(key,value)->key%2==0);
        assertEquals(null, existing.put(10,10));
        assertEquals(10, source.get(10));
        assertThrows(IllegalArgumentException.class,()->existing.put(11,11));
    }
}
