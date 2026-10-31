/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Multiset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Exact erased API and M3 primitive-lane owner; original Guava is test-only. */
final class M3InlineGuavaMultisetBoundaryTest {
    @Test void originalInterfaceSignaturesArePresent() throws Exception {
        assertEquals("com.google.common.collect.Multiset", Multiset.class.getName());
        assertEquals(Collection.class, Multiset.class.getInterfaces()[0]);
        assertEquals(int.class, Multiset.class.getMethod("count", Object.class).getReturnType());
        assertEquals(int.class, Multiset.class.getMethod("add", Object.class, int.class).getReturnType());
        assertEquals(int.class, Multiset.class.getMethod("remove", Object.class, int.class).getReturnType());
        assertEquals(int.class, Multiset.class.getMethod("setCount", Object.class, int.class).getReturnType());
        assertEquals(boolean.class, Multiset.class.getMethod(
                "setCount", Object.class, int.class, int.class).getReturnType());
        assertEquals(Set.class, Multiset.class.getMethod("elementSet").getReturnType());
        assertEquals(Set.class, Multiset.class.getMethod("entrySet").getReturnType());
        assertEquals(Spliterator.class, Multiset.class.getMethod("spliterator").getReturnType());
        assertEquals("com.google.common.collect.Multiset$Entry",
                Multiset.Entry.class.getName());
    }

    @Test void ownedM3DenseLanesPreserveCountsAndViews() {
        Multiset<String> bag = M3HashMultiset.create();
        assertTrue(bag.add("x"));
        assertTrue(bag.add("x"));
        assertEquals(2, bag.size());
        assertEquals(2, bag.count("x"));
        assertEquals(2, bag.add("x", 3));
        assertEquals(5, bag.count("x"));
        assertEquals(5, bag.setCount("x", 2));
        assertFalse(bag.setCount("x", 4, 9));
        assertTrue(bag.setCount("x", 2, 4));
        assertEquals(4, bag.count("x"));
        AtomicInteger total = new AtomicInteger();
        bag.forEachEntry((element, count) -> total.addAndGet(count));
        assertEquals(4, total.get());
        Collection<String> captured = new ArrayList<>();
        bag.forEach(captured::add);
        assertEquals(4, captured.size());
        assertEquals(4L, bag.spliterator().getExactSizeIfKnown());
        assertEquals(1, bag.elementSet().size());
        assertEquals(1, bag.entrySet().size());
        assertEquals(4, bag.remove("x", 3));
        assertEquals(1, bag.count("x"));
        assertTrue(bag.remove("x"));
        assertTrue(bag.isEmpty());
        assertEquals(List.of(), new ArrayList<>(bag));
    }

    @Test void onlyAuthenticGuavaOracleRemainsInTestScope() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element p = f.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList direct = p.getChildNodes();
        boolean guavaOracle = false;
        for (int i = 0; i < direct.getLength(); i++) {
            if (!(direct.item(i) instanceof Element section)
                    || !"dependencies".equals(section.getTagName())) continue;
            NodeList ds = section.getElementsByTagName("dependency");
            for (int j = 0; j < ds.getLength(); j++) {
                Element d = (Element) ds.item(j);
                String group = d.getElementsByTagName("groupId").item(0).getTextContent().trim();
                String art = d.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                String scope = d.getElementsByTagName("scope").item(0).getTextContent().trim();
                assertEquals("test", scope, group + ":" + art);
                guavaOracle |= "com.google.guava".equals(group) && "guava".equals(art);
            }
        }
        assertTrue(guavaOracle);
    }
}
