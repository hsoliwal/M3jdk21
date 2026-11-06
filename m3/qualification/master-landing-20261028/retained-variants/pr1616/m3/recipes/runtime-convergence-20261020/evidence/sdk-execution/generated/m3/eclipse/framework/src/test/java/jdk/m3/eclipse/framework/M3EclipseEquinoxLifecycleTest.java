// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.launch.Framework;

/** Actual pinned Equinox service event delivery, independent of proxy callback fixtures. */
class M3EclipseEquinoxLifecycleTest {
    @TempDir Path storage;

    @Test void serviceRemainsUsableInsideSynchronousLifecycleEvents() throws Exception {
        Framework framework = new EquinoxFactory().newFramework(Map.of(
                Constants.FRAMEWORK_STORAGE, storage.toString(),
                Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT));
        framework.init();
        framework.start();
        try {
            BundleContext context = framework.getBundleContext();
            AtomicInteger registered = new AtomicInteger();
            AtomicInteger unregistering = new AtomicInteger();
            AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
            ServiceListener listener = event -> {
                if (event.getType() != ServiceEvent.REGISTERED
                        && event.getType() != ServiceEvent.UNREGISTERING) return;
                Object service = context.getService(event.getServiceReference());
                try {
                    M3EclipseRuntimeServices published = (M3EclipseRuntimeServices) service;
                    assertEquals(context.getBundles().length, published.bundles().size());
                    if (event.getType() == ServiceEvent.REGISTERED) registered.incrementAndGet();
                    else unregistering.incrementAndGet();
                } catch (Throwable failure) {
                    callbackFailure.compareAndSet(null, failure);
                } finally {
                    context.ungetService(event.getServiceReference());
                }
            };
            context.addServiceListener(listener, "(objectClass=" + M3EclipseRuntimeServices.class.getName() + ")");
            M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
            try {
                activator.start(context);
                assertNull(callbackFailure.get(), "REGISTERED service was unusable");
                assertEquals(1, registered.get());
                activator.stop(context);
                assertNull(callbackFailure.get(), "UNREGISTERING service was unusable");
                assertEquals(1, unregistering.get());
                assertThrows(IllegalStateException.class, activator::bundles);
            } finally {
                activator.stop(context);
                context.removeServiceListener(listener);
            }
        } finally {
            framework.stop();
            assertEquals(FrameworkEvent.STOPPED, framework.waitForStop(10000).getType());
        }
    }
}
