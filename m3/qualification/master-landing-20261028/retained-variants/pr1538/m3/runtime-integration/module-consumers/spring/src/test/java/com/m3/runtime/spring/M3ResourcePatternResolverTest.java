/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.file.M3ModuleResources;
import com.m3.runtime.spring.duplicate.DuplicateComponents;
import com.m3.runtime.spring.fixtures.ContractComponents;
import com.m3.runtime.spring.fixtures.ContractComponents.*;
import com.m3.runtime.test.ModuleFixture;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.AntPathMatcher;
import static org.junit.jupiter.api.Assertions.*;

class M3ResourcePatternResolverTest {
    private static Backend backend() {
        return Boolean.getBoolean("m3.module.resources.native-required") ? Backend.NATIVE_REQUIRED : Backend.JAVA;
    }
    private static ClassLoader noClasspathEnumeration() {
        return new ClassLoader(M3ResourcePatternResolverTest.class.getClassLoader()) {
            @Override public Enumeration<URL> getResources(String name) { return Collections.emptyEnumeration(); }
        };
    }

    @Test
    void realScannerUsesIndexedPayloadAndRetainsContainerSemantics() throws Exception {
        ContractComponents.INITIALIZED.set(0);
        ContractComponents.DESTROYED.set(0);
        var reference = ModuleFixture.classes("spring.fixture", Visible.class, MetaService.class,
                Prototype.class, SampleFactory.class, Inactive.class, ConditionalService.class, Excluded.class);
        try (M3ModuleResources rows = M3ModuleResources.open(reference);
                GenericApplicationContext parent = new GenericApplicationContext()) {
            parent.getBeanFactory().registerSingleton("parentOnly", new Object());
            parent.refresh();
            M3ResourcePatternResolver resolver = new M3ResourcePatternResolver(List.of(rows, rows),
                    noClasspathEnumeration(), backend());
            assertEquals(0, reference.payloadReads.get(), "enumeration must not parse class payload");
            try (GenericApplicationContext child = new GenericApplicationContext(parent)) {
                child.getEnvironment().setActiveProfiles("active");
                ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
                        child, true, child.getEnvironment(), resolver);
                scanner.setMetadataReaderFactory(new CachingMetadataReaderFactory(resolver));
                scanner.addExcludeFilter((metadata, factory) ->
                        metadata.getClassMetadata().getClassName().endsWith("$Excluded"));
                scanner.scan("com.m3.runtime.spring.fixtures");
                int definitionCount = child.getBeanDefinitionCount();
                scanner.scan("com.m3.runtime.spring.fixtures");
                assertEquals(definitionCount, child.getBeanDefinitionCount(), "repeat scan remains idempotent");
                child.registerBeanDefinition("registeredLater", new RootBeanDefinition(StringBuilder.class));
                child.refresh();
                assertEquals("indexed bean", child.getBean("visibleService", Visible.class).value());
                assertSame(child.getBean(Visible.class), child.getBean(Visible.class));
                assertNotNull(child.getBean(MetaService.class), "metaannotations use Spring's real metadata reader");
                assertNotSame(child.getBean("prototypeService"), child.getBean("prototypeService"));
                assertEquals("factory product", child.getBean("sampleFactory"));
                assertInstanceOf(SampleFactory.class, child.getBean("&sampleFactory"));
                assertSame(parent.getBean("parentOnly"), child.getBean("parentOnly"));
                assertInstanceOf(StringBuilder.class, child.getBean("registeredLater"));
                assertFalse(child.containsBean("inactiveService"));
                assertFalse(child.containsBean("conditionalService"));
                assertFalse(child.containsBean("excludedService"));
                assertEquals(1, ContractComponents.INITIALIZED.get());
                assertTrue(reference.payloadReads.get() >= rows.size(), "Spring ASM reads original indexed payload");
                assertEquals(1, reference.listCalls.get(), "scans reuse one immutable resource plane");
            }
            assertEquals(1, ContractComponents.DESTROYED.get());
            assertTrue(rows.isOpen(), "the scoped resolver borrows the explicit owner");
        }
        assertEquals(1, reference.closed.get());
    }

    @Test
    void opaqueCustomMatcherReceivesOriginalEncounterOrder() throws Exception {
        var reference = ModuleFixture.classes("matcher.fixture", Prototype.class, Visible.class, MetaService.class);
        try (M3ModuleResources rows = M3ModuleResources.open(reference)) {
            var resolver = new M3ResourcePatternResolver(List.of(rows), noClasspathEnumeration(), backend());
            List<String> visited = new ArrayList<>();
            resolver.setPathMatcher(new AntPathMatcher() {
                @Override public boolean isPattern(String path) { return true; }
                @Override public boolean match(String pattern, String path) {
                    visited.add(path);
                    return path.endsWith(".class");
                }
            });
            Resource[] resources = resolver.moduleResources("opaque pattern with no package prefix");
            assertEquals(rows.size(), resources.length);
            assertEquals(List.of(rows.names()), visited);
            try (var stream = resources[0].getInputStream()) {
                assertEquals(0xca, stream.read(), "real class file magic");
                rows.close();
                assertThrows(ClosedChannelException.class, stream::read);
            }
            assertThrows(ClosedChannelException.class, resources[0]::getInputStream);
        }
    }

    @Test
    void duplicateBeanNameStillFailsThroughSpringScanner() throws Exception {
        var reference = ModuleFixture.classes("duplicate.fixture", DuplicateComponents.First.class, DuplicateComponents.Second.class);
        try (M3ModuleResources rows = M3ModuleResources.open(reference);
                GenericApplicationContext context = new GenericApplicationContext()) {
            var resolver = new M3ResourcePatternResolver(List.of(rows), noClasspathEnumeration(), backend());
            var scanner = new ClassPathBeanDefinitionScanner(context, true, context.getEnvironment(), resolver);
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> scanner.scan("com.m3.runtime.spring.duplicate"));
            assertEquals("ConflictingBeanDefinitionException", failure.getClass().getSimpleName());
        }
    }
}
