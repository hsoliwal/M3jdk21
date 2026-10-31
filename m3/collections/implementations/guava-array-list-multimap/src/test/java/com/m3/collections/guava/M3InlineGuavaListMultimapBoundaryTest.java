/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Genuine Guava list-multimap public signatures and independently owned M3 storage. */
final class M3InlineGuavaListMultimapBoundaryTest {
    @Test void providerApiClosureIsOriginalFqcnAndErasedDescriptors() throws Exception {
        assertEquals("com.google.common.collect.Multimap", Multimap.class.getName());
        assertEquals("com.google.common.collect.ListMultimap", ListMultimap.class.getName());
        assertEquals("com.google.common.collect.Multiset", Multiset.class.getName());
        assertEquals(Multimap.class, ListMultimap.class.getInterfaces()[0]);
        assertEquals(boolean.class, Multimap.class.getMethod("put", Object.class, Object.class).getReturnType());
        assertEquals(List.class, ListMultimap.class.getMethod("get", Object.class).getReturnType());
        assertEquals(List.class, ListMultimap.class.getMethod("removeAll", Object.class).getReturnType());
        assertEquals(List.class, ListMultimap.class.getMethod(
                "replaceValues", Object.class, Iterable.class).getReturnType());
        assertEquals(Map.class, ListMultimap.class.getMethod("asMap").getReturnType());
        assertEquals(Multiset.class, Multimap.class.getMethod("keys").getReturnType());
    }

    @Test void m3IndexedMultimapRetainsOriginalGuavaOccurrenceAndLiveViews() {
        ListMultimap<String, Integer> m = M3ArrayListMultimap.create();
        assertTrue(m.put("a", 1));
        assertTrue(m.put("a", 1));
        assertTrue(m.put("b", 2));
        assertEquals(3, m.size());
        assertEquals(2, m.keys().count("a"));
        assertEquals(2, m.keySet().size());
        assertEquals(List.of(1, 1), m.get("a"));
        assertEquals(3, m.values().size());
        m.get("new").add(5);
        assertEquals(List.of(5), m.get("new"));
        assertEquals(4, m.size());
        m.remove("a", 1);
        assertEquals(List.of(1), m.get("a"));
        assertEquals(3, m.size());
        assertEquals(List.of(1), m.removeAll("a"));
        assertFalse(m.containsKey("a"));
        assertEquals(2, m.size());
        Collection<Integer> asMapNew = m.asMap().get("new");
        assertEquals(List.of(5), asMapNew);
    }

    @Test void testOnlyRealGuavaOracleNotAProductionDependency() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = f.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList sections = root.getChildNodes();
        boolean guavaOracle = false;
        for (int i = 0; i < sections.getLength(); i++) {
            if (!(sections.item(i) instanceof Element sec)
                    || !"dependencies".equals(sec.getTagName())) continue;
            NodeList deps = sec.getElementsByTagName("dependency");
            for (int j = 0; j < deps.getLength(); j++) {
                Element dep = (Element) deps.item(j);
                assertEquals("test", dep.getElementsByTagName("scope").item(0).getTextContent().trim());
                String group = dep.getElementsByTagName("groupId").item(0).getTextContent().trim();
                String artifact = dep.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                guavaOracle |= "com.google.guava".equals(group) && "guava".equals(artifact);
            }
        }
        assertTrue(guavaOracle);
    }
}
