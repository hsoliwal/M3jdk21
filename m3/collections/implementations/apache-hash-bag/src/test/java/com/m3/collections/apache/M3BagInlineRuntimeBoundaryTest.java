/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * ABI/source checks for unchanged Apache Commons Collections Bag 4.5.0 API.
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.collections4.Bag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class M3BagInlineRuntimeBoundaryTest {
    @Test void providerApiRemainsTheOriginalPublicBagContract() throws Exception {
        Class<?> api = Bag.class;
        assertEquals("org.apache.commons.collections4.Bag", api.getName());
        assertEquals(java.util.Collection.class, api.getInterfaces()[0]);
        assertEquals(int.class, api.getMethod("getCount", Object.class).getReturnType());
        assertEquals(boolean.class, api.getMethod("add", Object.class, int.class).getReturnType());
        assertEquals(boolean.class, api.getMethod("remove", Object.class, int.class).getReturnType());
        assertEquals(Set.class, api.getMethod("uniqueSet").getReturnType());
    }

    @Test void m3ParallelArraysRespectApacheBagCardinalityAndMutation() {
        Bag<String> bag = new M3HashBag<>();
        assertTrue(bag.add("x"));
        assertFalse(bag.add("x"));
        assertTrue(bag.add("y", 3));
        assertEquals(5, bag.size());
        assertEquals(2, bag.getCount("x"));
        assertEquals(3, bag.getCount("y"));
        assertEquals(Set.of("x", "y"), bag.uniqueSet());
        assertTrue(bag.remove("x", 1));
        assertEquals(1, bag.getCount("x"));
        assertTrue(bag.remove("x"));
        assertEquals(0, bag.getCount("x"));
        assertEquals(3, bag.size());
        assertTrue(bag.containsAll(List.of("y", "y", "y")));
        assertFalse(bag.containsAll(List.of("y", "y", "y", "y")));
    }

    @Test void originalCommonsArtifactIsTestOnlyNotAProductionDependency() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList roots = project.getChildNodes();
        boolean sawOracle = false;
        for (int i = 0; i < roots.getLength(); i++) {
            if (!(roots.item(i) instanceof Element section) ||
                    !"dependencies".equals(section.getTagName())) continue;
            NodeList dependencies = section.getChildNodes();
            for (int j = 0; j < dependencies.getLength(); j++) {
                if (!(dependencies.item(j) instanceof Element dep) ||
                        !"dependency".equals(dep.getTagName())) continue;
                String group = field(dep, "groupId");
                String artifact = field(dep, "artifactId");
                String scope = field(dep, "scope");
                assertEquals("test", scope, group + ":" + artifact + " must not be published downstream");
                if ("org.apache.commons".equals(group) &&
                        "commons-collections4".equals(artifact)) sawOracle = true;
            }
        }
        assertTrue(sawOracle, "real donor differential tests must remain enabled");
    }

    private static String field(Element parent, String name) {
        NodeList children = parent.getElementsByTagName(name);
        return children.getLength() == 0 ? "" : children.item(0).getTextContent().trim();
    }
}
