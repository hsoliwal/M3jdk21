/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Proof of the bounded vendor-free M3-native collection slice, not vendor ABI parity. */
final class M3NativeFiveLibrarySdkNoVendorTest {
    @Test void nativeFamiliesPreserveDistinctCardinality() {
        var jdk = M3NativeFiveLibrarySdk.<String, Integer>map();
        jdk.put("k", 1);
        jdk.put("k", 2);
        assertEquals(1, jdk.size());
        assertEquals(2, jdk.get("k"));

        var springShaped = M3NativeFiveLibrarySdk.<String, Integer>mapOfLists();
        springShaped.add("k", 1);
        springShaped.add("k", 1);
        assertEquals(1, springShaped.size());
        assertEquals(List.of(1, 1), springShaped.get("k"));

        var pairCounted = M3NativeFiveLibrarySdk.<String, Integer>listMultimap();
        pairCounted.add("k", 1);
        pairCounted.add("k", 1);
        assertEquals(2, pairCounted.size());
        assertEquals(1, pairCounted.keyCount());

        var uniquePairs = M3NativeFiveLibrarySdk.<String, Integer>setMultimap();
        assertTrue(uniquePairs.put("k", 1));
        assertFalse(uniquePairs.put("k", 1));
        assertEquals(1L, uniquePairs.size());
        assertTrue(uniquePairs.get("k").remove(1));
        assertEquals(0L, uniquePairs.size());
    }

    @Test void liveListCollectionAndDequeFiltersReuseSingleOwners() {
        var list = M3NativeFiveLibrarySdk.<Integer>list();
        list.addAll(List.of(1, 2, 3, 4));
        List<Integer> selected = M3NativeFiveLibrarySdk.filteredList(list, n -> n % 2 == 0);
        assertEquals(List.of(2, 4), selected);
        list.add(6);
        assertEquals(List.of(2, 4, 6), selected);
        assertThrows(IllegalArgumentException.class, () -> selected.add(7));
        selected.add(8);
        assertTrue(list.contains(8));

        Collection<Integer> collection = M3NativeFiveLibrarySdk.filteredCollection(
                list, n -> n > 3);
        assertEquals(List.of(4, 6, 8), new ArrayList<>(collection));
        assertThrows(IllegalArgumentException.class, () -> collection.add(2));

        Collection<Integer> readOnly = M3NativeFiveLibrarySdk.readOnlyFilteredCollection(
                list, n -> n > 3);
        assertThrows(UnsupportedOperationException.class, () -> readOnly.add(10));

        Deque<Integer> deque = M3NativeFiveLibrarySdk.deque();
        deque.addAll(List.of(1, 2, 3));
        Deque<Integer> evens = M3NativeFiveLibrarySdk.filteredDeque(deque, n -> n % 2 == 0);
        assertEquals(2, evens.peekFirst());
        evens.addLast(4);
        assertThrows(IllegalArgumentException.class, () -> evens.addLast(5));
        assertTrue(deque.contains(1));
        assertTrue(deque.contains(4));
    }

    @Test void sortedLiveViewsReuseComparatorAndKeepHiddenMembers() {
        NavigableSet<Integer> set = M3NativeFiveLibrarySdk.sortedSet(Comparator.naturalOrder());
        set.addAll(List.of(1, 2, 3, 4));
        NavigableSet<Integer> evens = M3NativeFiveLibrarySdk.filteredNavigableSet(
                set, n -> n % 2 == 0);
        assertEquals(2, evens.first());
        assertEquals(4, evens.last());
        assertThrows(IllegalArgumentException.class, () -> evens.add(5));
        assertTrue(evens.add(6));
        assertTrue(set.contains(1));
        assertTrue(set.contains(6));
        assertEquals(4, evens.headSet(6, false).last());

        NavigableMap<String, Integer> map = M3NativeFiveLibrarySdk.sortedMap(
                Comparator.naturalOrder());
        map.put("a", 1);
        map.put("b", 2);
        NavigableMap<String, Integer> visible = M3NativeFiveLibrarySdk
                .filteredNavigableMapEntries(map, (k, v) -> v % 2 == 0);
        assertEquals(Set.of("b"), visible.keySet());
        assertThrows(IllegalArgumentException.class, () -> visible.put("c", 3));
        visible.put("d", 4);
        assertEquals(1, map.get("a"));
        assertEquals(4, map.get("d"));
    }

    @Test void noVendorJavaImportsOrRuntimeDependenciesInNativeCore() throws Exception {
        Pattern forbiddenImport = Pattern.compile(
                "(?m)^\\s*import\\s+(?:static\\s+)?(?:com\\.google\\.common"
                + "|org\\.eclipse\\.collections|org\\.springframework"
                + "|org\\.apache\\.commons)\\.");
        Path src = Path.of("src/main/java/com/m3/collections");
        assertTrue(Files.isDirectory(src), "run from the m3/collections Maven module");
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(forbiddenImport.matcher(Files.readString(file)).find(),
                        "vendor runtime import in native M3 core: " + file);
            }
        }
        assertNativePomHasNoVendorRuntimeDependencies(Path.of("pom.xml"));
        assertNativePomHasNoVendorRuntimeDependencies(Path.of("../arrays/pom.xml"));
    }

    private static void assertNativePomHasNoVendorRuntimeDependencies(Path pom) throws Exception {
        assertTrue(Files.isRegularFile(pom), "missing build descriptor " + pom);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList roots = project.getChildNodes();
        for (int i = 0; i < roots.getLength(); i++) {
            Node node = roots.item(i);
            if (!(node instanceof Element dependencies)
                    || !"dependencies".equals(dependencies.getTagName())) continue;
            NodeList children = dependencies.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element dep) || !"dependency".equals(dep.getTagName()))
                    continue;
                String group = directText(dep, "groupId");
                String scope = directText(dep, "scope");
                assertTrue("com.m3".equals(group) || "test".equals(scope),
                        "non-M3 production Maven dependency in " + pom + ": " + group);
            }
        }
    }

    private static String directText(Element source, String name) {
        NodeList nodes = source.getElementsByTagName(name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
