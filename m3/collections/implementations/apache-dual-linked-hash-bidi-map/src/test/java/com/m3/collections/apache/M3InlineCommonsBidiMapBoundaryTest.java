/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.Get;
import org.apache.commons.collections4.IterableGet;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.Put;
import org.apache.commons.collections4.ResettableIterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Source-closed Apache 4.5 BidiMap Java21 API and M3 two-lane indexed owner. */
final class M3InlineCommonsBidiMapBoundaryTest {
    @Test void originalErasedPublicProviderTypeHierarchyIsAvailable() throws Exception {
        assertEquals("org.apache.commons.collections4.BidiMap", BidiMap.class.getName());
        assertEquals(IterableMap.class, BidiMap.class.getInterfaces()[0]);
        assertTrue(Map.class.isAssignableFrom(BidiMap.class));
        assertTrue(Put.class.isAssignableFrom(BidiMap.class));
        assertTrue(Get.class.isAssignableFrom(BidiMap.class));
        assertTrue(IterableGet.class.isAssignableFrom(BidiMap.class));
        assertEquals(Object.class, BidiMap.class.getMethod("getKey", Object.class).getReturnType());
        assertEquals(Object.class, BidiMap.class.getMethod("removeValue", Object.class).getReturnType());
        assertEquals(BidiMap.class, BidiMap.class.getMethod("inverseBidiMap").getReturnType());
        assertEquals(MapIterator.class, IterableMap.class.getMethod("mapIterator").getReturnType());
        assertEquals(Iterator.class, ResettableIterator.class.getInterfaces()[0]);
        assertEquals(Object.class, MapIterator.class.getMethod("next").getReturnType());
        assertEquals(void.class, ResettableIterator.class.getMethod("reset").getReturnType());
    }

    @Test void actualM3ArraySlotBidirectionalViewsMutateSameOwners() {
        BidiMap<String, Integer> map = new M3DualLinkedHashBidiMap<>();
        assertEquals(null, map.put("a", 1));
        assertEquals(null, map.put("b", 2));
        BidiMap<Integer, String> inverse = map.inverseBidiMap();
        assertSame(map, inverse.inverseBidiMap());
        assertEquals("a", inverse.get(1));
        assertEquals("b", map.getKey(2));
        assertEquals(2, map.size());
        assertEquals(null, map.put("c", 1));
        assertFalse(map.containsKey("a"));
        assertEquals("c", inverse.get(1));
        assertEquals(2, map.size());
        assertEquals("b", map.removeValue(2));
        assertEquals(1, map.size());
        assertFalse(map.containsKey("b"));
        MapIterator<String, Integer> iterator = map.mapIterator();
        assertTrue(iterator.hasNext());
        assertEquals("c", iterator.next());
        assertEquals(1, iterator.getValue());
        assertEquals(1, iterator.setValue(3));
        assertEquals(3, map.get("c"));
        assertEquals("c", inverse.get(3));
        assertFalse(iterator.hasNext());
        assertTrue(iterator instanceof ResettableIterator<?>);
        ((ResettableIterator<?>) iterator).reset();
        assertTrue(iterator.hasNext());
    }

    @Test void authenticCommonsLibraryIsTestOnly() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = f.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList rootNodes = root.getChildNodes();
        boolean oracle = false;
        for (int i = 0; i < rootNodes.getLength(); i++) {
            if (!(rootNodes.item(i) instanceof Element section)
                    || !"dependencies".equals(section.getTagName())) continue;
            NodeList dependencies = section.getElementsByTagName("dependency");
            for (int j = 0; j < dependencies.getLength(); j++) {
                Element dep = (Element) dependencies.item(j);
                String group = dep.getElementsByTagName("groupId").item(0).getTextContent().trim();
                String art = dep.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                assertEquals("test", dep.getElementsByTagName("scope").item(0).getTextContent().trim());
                oracle |= "org.apache.commons".equals(group) &&
                        "commons-collections4".equals(art);
            }
        }
        assertTrue(oracle);
    }
}
