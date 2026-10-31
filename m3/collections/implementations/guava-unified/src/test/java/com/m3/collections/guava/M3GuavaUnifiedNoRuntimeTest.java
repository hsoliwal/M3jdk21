/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * One and only one public Guava API hierarchy for two independent M3 array-backed bodies.
 * Each family's authentic real Guava 33.4.8-jre differential JUnit is also retained.
 */
final class M3GuavaUnifiedNoRuntimeTest {
    @Test void twoM3IndexedEnginesUseOneOriginalPublicApiDomain() throws Exception {
        Multiset<String> counts = M3HashMultiset.create();
        ListMultimap<String, Integer> relation = M3ArrayListMultimap.create();
        assertTrue(counts.add("a"));
        assertTrue(counts.add("a"));
        assertEquals(2, counts.count("a"));
        assertTrue(relation.put("a", 1));
        assertTrue(relation.put("a", 1));
        assertTrue(relation.put("b", 2));
        assertEquals(3, relation.size());
        Multiset<String> keys = relation.keys();
        assertTrue(keys instanceof Multiset<?>);
        assertEquals(2, keys.count("a"));
        assertEquals(1, keys.count("b"));
        assertEquals(2, relation.keySet().size());
        assertEquals(List.of(1, 1), relation.get("a"));
        assertEquals(1, counts.elementSet().size());
        relation.get("c").add(9);
        assertEquals(4, relation.size());
        assertEquals(1, relation.keys().count("c"));
        assertTrue(relation.containsEntry("c", 9));
        assertEquals(0, counts.count("c"));
        assertSame(Multiset.class, Multimap.class.getMethod("keys").getReturnType());
    }

    @Test void indexedOwnerViewsAndMultisetMutationsRemainDistinct() {
        ListMultimap<String, String> list = M3ArrayListMultimap.create();
        Multiset<String> set = M3HashMultiset.create();
        list.put("k", null);
        list.put("k", null);
        assertTrue(list.get("k").contains(null));
        assertEquals(2, list.get("k").size());
        assertEquals(2, list.keys().count("k"));
        set.add("k", 3);
        assertEquals(3, set.count("k"));
        assertTrue(list.remove("k", null));
        assertEquals(1, list.get("k").size());
        assertEquals(3, set.count("k"));
        assertEquals(java.util.Collections.singletonList(null), list.get("k"));
        assertEquals(1, list.size());
        list.clear();
        assertFalse(list.containsKey("k"));
        assertEquals(3, set.size());
    }

    @Test void onlyRealGuavaJUnitDonorIsADeclaredDependency() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory xml = DocumentBuilderFactory.newInstance();
        xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = xml.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList children = root.getChildNodes();
        boolean guavaTestOracle = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element section)
                    || !"dependencies".equals(section.getTagName())) continue;
            NodeList deps = section.getElementsByTagName("dependency");
            for (int j = 0; j < deps.getLength(); j++) {
                Element dep = (Element) deps.item(j);
                assertEquals("test", dep.getElementsByTagName("scope").item(0).getTextContent().trim());
                String group = dep.getElementsByTagName("groupId").item(0).getTextContent().trim();
                String artifact = dep.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                guavaTestOracle |= "com.google.guava".equals(group) && "guava".equals(artifact);
            }
        }
        assertTrue(guavaTestOracle);
    }
}
