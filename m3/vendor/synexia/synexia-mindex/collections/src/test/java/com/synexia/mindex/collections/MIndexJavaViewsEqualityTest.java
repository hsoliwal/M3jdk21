// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MIndexJavaViewsEqualityTest {
    private record Value(int number) { }

    @Test
    void globalStringSpaceHasStandardJavaCollectionEquality() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexString a = MIndexString.literal("view-a");
        MIndexString b = MIndexString.literal("view-b");
        assertTrue(strings.javaEqualityCompatible());

        MIndexList<MIndexString> list = new MIndexList<>(strings);
        List<MIndexString> listView = MIndexJavaViews.mutableList(list);
        listView.add(a);
        listView.add(b);
        assertEquals(List.of(a, b), listView);

        MIndexSet<MIndexString> set = new MIndexSet<>(strings);
        Set<MIndexString> setView = MIndexJavaViews.mutableSet(set);
        setView.add(a);
        setView.add(b);
        assertEquals(Set.of(a, b), setView);

        MIndexMap<MIndexString, MIndexString> map =
                new MIndexMap<>(strings, strings);
        Map<MIndexString, MIndexString> mapView =
                MIndexJavaViews.mutableMap(map);
        mapView.put(a, b);
        assertEquals(Map.of(a, b), mapView);

        MIndexCompositeIndex index = new MIndexCompositeIndex();
        assertEquals(List.of(a, b),
                MIndexJavaViews.readOnlyList(list.freeze(index)));
        assertEquals(Set.of(a, b),
                MIndexJavaViews.readOnlySet(set.freeze(index)));
        assertEquals(Map.of(a, b),
                MIndexJavaViews.readOnlyMap(map.freeze(index)));
    }

    @Test
    void identityAndStructuralDomainsCannotClaimJavaCollectionSemantics() {
        MIndexIdentitySpace<String> identities = MIndexSpaces.identities();
        MIndexSet<String> identitySet = new MIndexSet<>(identities);
        identitySet.add(new String("equal"));
        identitySet.add(new String("equal"));
        assertEquals(2, identitySet.size());
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.mutableSet(identitySet));

        MIndexObjectSpace<Value> objects = MIndexSpaces.values();
        MIndexList<Value> objectList = new MIndexList<>(objects);
        objectList.add(new Value(1));
        MIndexCompositeIndex index = new MIndexCompositeIndex();
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.mutableList(objectList));
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.readOnlyList(objectList.freeze(index)));
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.readOnlySet(identitySet.freeze(index)));

        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexMap<MIndexString, Value> unsafeValue =
                new MIndexMap<>(strings, objects);
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.mutableMap(unsafeValue));
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.readOnlyMap(unsafeValue.freeze(index)));

        MIndexMap<String, MIndexString> unsafeKey =
                new MIndexMap<>(identities, strings);
        assertThrows(IllegalArgumentException.class,
                () -> MIndexJavaViews.mutableMap(unsafeKey));
    }
}
