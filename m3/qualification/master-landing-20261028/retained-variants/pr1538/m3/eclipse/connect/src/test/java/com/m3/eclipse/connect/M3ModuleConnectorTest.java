/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.eclipse.connect;

import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.runtime.test.ModuleFixture;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import static org.junit.jupiter.api.Assertions.*;

class M3ModuleConnectorTest {
    @TempDir Path temporary;
    private static Backend backend() {
        return Boolean.getBoolean("m3.module.resources.native-required") ? Backend.NATIVE_REQUIRED : Backend.JAVA;
    }

    private Path bundle(String version, String value) throws Exception {
        return ModuleFixture.compileJar(temporary.resolve("v" + version), "m3-bundle-fixture.jar",
                Map.of("bundlefixture/Activator.java", """
                    package bundlefixture;
                    public class Activator implements org.osgi.framework.BundleActivator, java.util.function.Supplier<String> {
                        private org.osgi.framework.ServiceRegistration<java.util.function.Supplier> registration;
                        public String get() { return "%s"; }
                        public void start(org.osgi.framework.BundleContext context) {
                            registration = context.registerService(java.util.function.Supplier.class, this, null);
                        }
                        public void stop(org.osgi.framework.BundleContext context) { registration.unregister(); }
                    }
                    """.formatted(value)),
                Map.of("Automatic-Module-Name", "m3.bundle.fixture", "Bundle-ManifestVersion", "2",
                        "Bundle-SymbolicName", "m3.bundle.fixture", "Bundle-Version", version,
                        "Bundle-Activator", "bundlefixture.Activator",
                        "Import-Package", "org.osgi.framework;version=\"[1.10,2)\""),
                Map.of("fixture/message.txt", value.getBytes(StandardCharsets.UTF_8),
                        "assets/", new byte[0], "dual", "file".getBytes(StandardCharsets.UTF_8),
                        "dual/", new byte[0]));
    }

    @Test
    void directoryAliasUsesCapturedCanonicalNameAndPreservesExactFile() throws Exception {
        var connector = new M3ModuleConnector(backend());
        connector.register("m3:directories", ModuleFixture.reference(bundle("1.0.0", "directory fixture")));
        ConnectContent content = connector.connect("m3:directories").orElseThrow().getContent();
        content.open();
        try {
            var alias = content.getEntry("assets").orElseThrow();
            assertEquals("assets/", alias.getName());
            assertEquals("assets/", content.getEntry("assets/").orElseThrow().getName());
            assertArrayEquals(new byte[0], alias.getBytes());
            var exact = content.getEntry("dual").orElseThrow();
            assertEquals("dual", exact.getName(), "an exact file wins over its directory alias");
            assertEquals("file", new String(exact.getBytes(), StandardCharsets.UTF_8));
            assertEquals("dual/", content.getEntry("dual/").orElseThrow().getName());
            assertTrue(content.getEntry("missing").isEmpty());
            assertTrue(content.getEntry("/assets").isEmpty());
        }
        finally { content.close(); }
    }

    @Test
    void contentUsesOriginalManifestPayloadAndCanReopenSamePinnedRevision() throws Exception {
        Path jar = bundle("1.0.0", "first revision");
        var reference = ModuleFixture.reference(jar);
        var connector = new M3ModuleConnector(backend());
        connector.register("m3:fixture", reference);
        ConnectContent content = connector.connect("m3:fixture").orElseThrow().getContent();
        assertThrows(IllegalStateException.class, content::getHeaders);
        content.open();
        var owned = ((M3ModuleConnector.Content) content).resources();
        assertSame(reference, owned.reference());
        assertEquals("m3.bundle.fixture", content.getHeaders().orElseThrow().get(Constants.BUNDLE_SYMBOLICNAME));
        assertTrue(content.getClassLoader().isEmpty(), "Equinox must own class loading, weaving and wiring");
        List<String> names = new ArrayList<>();
        content.getEntries().forEach(names::add);
        assertEquals(List.of(owned.names()), names);
        // The exact same opened owner supplies Connect entries and Spring's real scanner.
        try (var loader = new java.net.URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, getClass().getClassLoader());
                var context = new org.springframework.context.support.GenericApplicationContext()) {
            context.setClassLoader(loader);
            var resolver = new com.m3.runtime.spring.M3ResourcePatternResolver(List.of(owned), loader, backend());
            var scanner = new org.springframework.context.annotation.ClassPathBeanDefinitionScanner(
                    context, false, context.getEnvironment(), resolver);
            scanner.addIncludeFilter((metadata, factory) ->
                    metadata.getClassMetadata().getClassName().equals("bundlefixture.Activator"));
            scanner.scan("bundlefixture");
            context.refresh();
            assertEquals("first revision", context.getBean(Supplier.class).get());
            assertSame(owned, ((M3ModuleConnector.Content) content).resources());
        }
        var oldEntry = content.getEntry("fixture/message.txt").orElseThrow();
        var stream = oldEntry.getInputStream();
        assertEquals('f', stream.read());
        assertTrue(content.getEntry("/fixture/message.txt").isEmpty());
        content.close();
        assertFalse(owned.isOpen());
        assertThrows(ClosedChannelException.class, stream::read);
        assertThrows(IllegalStateException.class, content::getEntries);
        content.open();
        assertNotSame(owned, ((M3ModuleConnector.Content) content).resources());
        assertThrows(ClosedChannelException.class, oldEntry::getInputStream);
        assertEquals("first revision", new String(content.getEntry("fixture/message.txt").orElseThrow().getBytes(), StandardCharsets.UTF_8));
        content.close();
        stream.close();
    }

    @Test
    void realEquinoxLoadsIndexedBundleRegistersServiceAndUpdatesRevision() throws Exception {
        var connector = new M3ModuleConnector(backend());
        connector.register("m3:fixture", ModuleFixture.reference(bundle("1.0.0", "first revision")));
        var actualContent = new java.util.concurrent.atomic.AtomicReference<M3ModuleConnector.Content>();
        var observed = new org.osgi.framework.connect.ModuleConnector() {
            @Override public void initialize(java.io.File storage, Map<String, String> configuration) {
                connector.initialize(storage, configuration);
            }
            @Override public java.util.Optional<org.osgi.framework.BundleActivator> newBundleActivator() {
                return connector.newBundleActivator();
            }
            @Override public java.util.Optional<org.osgi.framework.connect.ConnectModule> connect(String location)
                    throws org.osgi.framework.BundleException {
                return connector.connect(location).map(module -> () -> {
                    var content = (M3ModuleConnector.Content) module.getContent();
                    actualContent.set(content);
                    return content;
                });
            }
        };
        ConnectFrameworkFactory factory = ServiceLoader.load(ConnectFrameworkFactory.class).findFirst().orElseThrow();
        Framework framework = factory.newFramework(Map.of(
                Constants.FRAMEWORK_STORAGE, temporary.resolve("framework").toString(),
                Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT), observed);
        try (var firstContext = new org.springframework.context.support.GenericApplicationContext();
                var secondContext = new org.springframework.context.support.GenericApplicationContext()) {
            framework.init();
            framework.start();
            Bundle bundle = framework.getBundleContext().installBundle("m3:fixture");
            bundle.start();
            assertEquals(Bundle.ACTIVE, bundle.getState());
            Class<?> firstClass = bundle.loadClass("bundlefixture.Activator");
            assertNotSame(M3ModuleConnectorTest.class.getClassLoader(), firstClass.getClassLoader());
            var firstReference = framework.getBundleContext().getServiceReference(Supplier.class);
            assertNotNull(firstReference);
            Supplier<?> first = framework.getBundleContext().getService(firstReference);
            assertEquals("first revision", first.get());
            var firstRows = actualContent.get().resources();
            scanConnectedRevision(firstContext, actualContent.get(),
                    bundle.adapt(org.osgi.framework.wiring.BundleWiring.class).getClassLoader());
            Supplier<?> firstBean = firstContext.getBean(Supplier.class);
            assertSame(firstClass, firstBean.getClass(), "Spring must use actual Equinox class identity");
            assertSame(firstRows, actualContent.get().resources(), "both frameworks borrow the same opened owner");
            assertEquals("first revision", firstBean.get());
            try (var stream = bundle.getEntry("fixture/message.txt").openStream()) {
                assertEquals("first revision", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            connector.register("m3:fixture", ModuleFixture.reference(bundle("2.0.0", "second revision")));
            bundle.update();
            assertEquals(Bundle.ACTIVE, bundle.getState());
            assertEquals("2.0.0", bundle.getVersion().toString());
            Class<?> secondClass = bundle.loadClass("bundlefixture.Activator");
            assertNotSame(firstClass, secondClass, "new Connect content must get a new Equinox wiring");
            var secondReference = framework.getBundleContext().getServiceReference(Supplier.class);
            assertNotNull(secondReference);
            Supplier<?> second = framework.getBundleContext().getService(secondReference);
            assertEquals("second revision", second.get());
            var secondRows = actualContent.get().resources();
            assertNotSame(firstRows, secondRows);
            scanConnectedRevision(secondContext, actualContent.get(),
                    bundle.adapt(org.osgi.framework.wiring.BundleWiring.class).getClassLoader());
            Supplier<?> secondBean = secondContext.getBean(Supplier.class);
            assertSame(secondClass, secondBean.getClass());
            assertSame(secondRows, actualContent.get().resources());
            assertEquals("second revision", secondBean.get());
            assertEquals("first revision", firstBean.get(), "an existing Spring bean keeps its original wiring");
            assertEquals("first revision", first.get(), "existing revision objects must never retarget");
            framework.getBundleContext().ungetService(firstReference);
            framework.getBundleContext().ungetService(secondReference);
            bundle.uninstall();
            assertNull(framework.getBundleContext().getServiceReference(Supplier.class));
        }
        finally {
            framework.stop();
            assertNotEquals(org.osgi.framework.FrameworkEvent.WAIT_TIMEDOUT, framework.waitForStop(15_000).getType());
        }
    }

    private static void scanConnectedRevision(org.springframework.context.support.GenericApplicationContext context,
            M3ModuleConnector.Content content, ClassLoader wiringLoader) {
        context.setClassLoader(wiringLoader);
        var resolver = new com.m3.runtime.spring.M3ResourcePatternResolver(
                List.of(content.resources()), wiringLoader, backend());
        var scanner = new org.springframework.context.annotation.ClassPathBeanDefinitionScanner(
                context, false, context.getEnvironment(), resolver);
        scanner.addIncludeFilter((metadata, factory) ->
                metadata.getClassMetadata().getClassName().equals("bundlefixture.Activator"));
        scanner.scan("bundlefixture");
        context.refresh();
    }
}
