/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.eclipse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Source-inlined Eclipse API, JDK21 erased interface and live callback semantics. */
final class M3EclipseFunctionalAdaptersTest {
    @Test void originalEclipseInterfacesHaveTheirPublicFunctionalDescriptors() throws Exception {
        assertEquals("org.eclipse.collections.api.block.function.Function", Function.class.getName());
        assertEquals("org.eclipse.collections.api.block.function.Function2", Function2.class.getName());
        assertEquals("org.eclipse.collections.api.block.predicate.Predicate", Predicate.class.getName());
        assertEquals("org.eclipse.collections.api.block.predicate.Predicate2", Predicate2.class.getName());
        assertEquals("org.eclipse.collections.api.block.procedure.Procedure", Procedure.class.getName());
        assertEquals(Object.class, Function.class.getMethod("valueOf", Object.class).getReturnType());
        assertEquals(Object.class, Function2.class.getMethod(
                "value", Object.class, Object.class).getReturnType());
        assertEquals(boolean.class, Predicate.class.getMethod("accept", Object.class).getReturnType());
        assertEquals(boolean.class, Predicate2.class.getMethod(
                "accept", Object.class, Object.class).getReturnType());
        assertEquals(void.class, Procedure.class.getMethod("value", Object.class).getReturnType());
        for (Class<?> api : List.of(Function.class, Function2.class, Predicate.class,
                Predicate2.class, Procedure.class)) {
            assertTrue(Serializable.class.isAssignableFrom(api));
            assertTrue(api.isAnnotationPresent(FunctionalInterface.class));
        }
    }

    @Test void reusesTheJavaFunctionAtomsAndRunsCallbacksExactlyAtInvocation() {
        AtomicInteger invoked = new AtomicInteger();
        Function<Integer, String> f = M3EclipseFunctionalAdapters.function(n -> {
            invoked.incrementAndGet();
            return "m3-" + n;
        });
        assertEquals(0, invoked.get());
        assertEquals("m3-7", f.valueOf(7));
        assertEquals("m3-9", f.apply(9));
        assertEquals(2, invoked.get());

        Function2<Integer, Integer, Integer> combine = M3EclipseFunctionalAdapters
                .function2(Integer::sum);
        assertEquals(11, combine.value(5, 6));
        assertEquals(15, combine.apply(7, 8));

        Predicate<Integer> even = M3EclipseFunctionalAdapters.predicate(n -> n % 2 == 0);
        Predicate2<Integer, Integer> greater =
                M3EclipseFunctionalAdapters.predicate2((n, limit) -> n > limit);
        assertTrue(even.accept(8));
        assertFalse(even.test(7));
        assertTrue(greater.accept(9, 3));
        assertFalse(greater.test(1, 3));

        List<Integer> values = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(values.removeIf(even)); // existing JDK/M3 indexed collection consumer protocol
        assertEquals(List.of(1, 3, 5), values);
        AtomicInteger sum = new AtomicInteger();
        Procedure<Integer> procedure = M3EclipseFunctionalAdapters.procedure(sum::addAndGet);
        values.forEach(procedure);
        procedure.value(2);
        assertEquals(11, sum.get());
        assertThrows(NullPointerException.class, () -> M3EclipseFunctionalAdapters.function(null));
        assertThrows(NullPointerException.class, () -> M3EclipseFunctionalAdapters.predicate2(null));
    }

    @Test void providerSourceIsInlinedNotADownstreamRuntimeDependency() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom));
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = f.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        NodeList sections = root.getChildNodes();
        for (int i = 0; i < sections.getLength(); i++) {
            if (!(sections.item(i) instanceof Element section)
                    || !"dependencies".equals(section.getTagName())) continue;
            NodeList deps = section.getElementsByTagName("dependency");
            for (int d = 0; d < deps.getLength(); d++) {
                Element dependency = (Element) deps.item(d);
                assertEquals("test", dependency.getElementsByTagName(
                        "scope").item(0).getTextContent().trim());
            }
        }
        assertTrue(Files.isRegularFile(Path.of("LICENSE-EPL-1.0.txt")));
        assertTrue(Files.isRegularFile(Path.of("LICENSE-EDL-1.0.txt")));
    }
}
