/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Provider API binary/alias invariants; authentic Spring binary differentials remain test-only. */
final class M3SpringInlineApiBoundaryTest {
    @Test void publicSurfaceAndRuntimeNullabilityMarkerAreOwned() throws Exception {
        assertEquals("org.springframework.util.MultiValueMap", MultiValueMap.class.getName());
        assertEquals(Map.class, MultiValueMap.class.getInterfaces()[0]);
        assertNotNull(MultiValueMap.class.getMethod("getFirst", Object.class)
                .getAnnotation(Nullable.class));
        assertEquals(RetentionPolicy.RUNTIME,
                Nullable.class.getAnnotation(java.lang.annotation.Retention.class).value());
        assertEquals(Map.class, MultiValueMap.class.getMethod("asSingleValueMap").getReturnType());
        assertEquals(MultiValueMap.class, MultiValueMap.class.getMethod(
                "fromMultiValue", Map.class).getReturnType());
    }

    @Test void m3ProviderAdapterReadsAndWritesTheSuppliedDirectoryWithoutCopy() {
        Map<String, List<Integer>> store = new LinkedHashMap<>();
        MultiValueMap<String, Integer> m = MultiValueMap.fromMultiValue(store);
        m.add("x", 1);
        m.add("x", 1);
        assertEquals(List.of(1, 1), store.get("x"));
        assertEquals(1, m.size());
        assertEquals(1, m.getFirst("x"));
        m.addIfAbsent("x", 10);
        assertEquals(2, store.get("x").size());
        m.addIfAbsent("y", 10);
        assertEquals(List.of(10), store.get("y"));
        Map<String, Integer> detached = m.toSingleValueMap();
        m.set("x", 9);
        assertEquals(1, detached.get("x"));
        assertEquals(9, m.asSingleValueMap().get("x"));
        m.asSingleValueMap().put("z", null);
        assertTrue(store.containsKey("z"));
        assertEquals(null, store.get("z"));
        assertThrows(NullPointerException.class,
                () -> m.asSingleValueMap().containsValue(-1));
    }

    @Test void scalarBackedProviderFactoryPreservesLiveAliasAndDuplicateRefusal() {
        Map<String, Integer> scalar = new LinkedHashMap<>();
        MultiValueMap<String, Integer> m = MultiValueMap.fromSingleValue(scalar);
        m.add("a", 4);
        assertEquals(4, scalar.get("a"));
        assertEquals(List.of(4), m.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> m.add("a", 6));
        assertThrows(UnsupportedOperationException.class, () -> m.put("a", List.of(7, 8)));
        m.put("a", List.of(7));
        assertEquals(7, scalar.get("a"));
        m.put("a", List.of());
        assertTrue(scalar.containsKey("a"));
        assertEquals(null, m.get("a"));
        assertEquals(Collections.singletonList(null), m.entrySet().iterator().next().getValue());
        assertThrows(UnsupportedOperationException.class,
                () -> m.toSingleValueMap().put("new", 3));
        scalar.put("b", 8);
        assertEquals(8, m.getFirst("b"));
        assertFalse(m.isEmpty());
    }

    @Test void onlyJunitAndAuthenticSpringDonorRemainAsTestDependencies() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        boolean realDonorTestOnly = false;
        NodeList sections = project.getChildNodes();
        for (int s = 0; s < sections.getLength(); s++) {
            if (!(sections.item(s) instanceof Element section) ||
                    !"dependencies".equals(section.getTagName())) continue;
            NodeList deps = section.getElementsByTagName("dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                String scope = dep.getElementsByTagName("scope").item(0).getTextContent().trim();
                assertEquals("test", scope);
                String group = dep.getElementsByTagName("groupId").item(0).getTextContent().trim();
                String artifact = dep.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                if ("org.springframework".equals(group) && "spring-core".equals(artifact)) {
                    realDonorTestOnly = true;
                }
            }
        }
        assertTrue(realDonorTestOnly);
    }
}
