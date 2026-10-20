// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.resources.IResourceDelta;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

class M3EclipseRuntimeActivatorTest {
    @Test
    void registersRealOsgiServiceAndUnregistersOnce() {
        AtomicInteger registered = new AtomicInteger();
        AtomicInteger unregistered = new AtomicInteger();
        Bundle bundle = proxy(Bundle.class, (instance, method, args) -> switch (method.getName()) {
            case "getBundleId" -> 0L;
            case "getState" -> Bundle.ACTIVE;
            default -> throw new AssertionError(method.getName());
        });
        ServiceRegistration<?> service = proxy(ServiceRegistration.class,
                (instance, method, args) -> {
                    if (method.getName().equals("unregister")) { unregistered.incrementAndGet(); return null; }
                    throw new AssertionError(method.getName());
                });
        BundleContext context = proxy(BundleContext.class, (instance, method, args) -> {
            if (method.getName().equals("registerService")) {
                assertSame(M3EclipseRuntimeServices.class, args[0]);
                registered.incrementAndGet();
                return service;
            }
            if (method.getName().equals("getBundles")) return new Bundle[]{bundle};
            throw new AssertionError(method.getName());
        });
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        assertThrows(IllegalStateException.class, activator::bundles);
        activator.start(context);
        assertEquals(1, registered.get());
        assertThrows(IllegalStateException.class, () -> activator.start(context));
        assertSame(bundle, activator.bundles().byId(0L));
        IResourceDelta root = proxy(IResourceDelta.class, (instance, method, args) -> switch (method.getName()) {
            case "getKind" -> IResourceDelta.ADDED;
            case "getAffectedChildren" -> new IResourceDelta[0];
            default -> throw new AssertionError(method.getName());
        });
        assertEquals(1, activator.resourceDelta(root, 1).size());
        activator.stop(context);
        activator.stop(context);
        assertEquals(1, unregistered.get());
        assertThrows(IllegalStateException.class, activator::bundles);
        assertThrows(IllegalStateException.class, () -> activator.resourceDelta(root, 1));
    }

    @Test void registeredCallbackCanUsePublishedService() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext context = context(activator, () -> assertEquals(0, activator.bundles().size()), () -> {});
        activator.start(context);
        activator.stop(context);
    }

    @Test void unregisteringCallbackCanUsePublishedService() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext context = context(activator, () -> {}, () -> assertEquals(0, activator.bundles().size()));
        activator.start(context);
        activator.stop(context);
        assertThrows(IllegalStateException.class, activator::bundles);
    }

    @Test void failedRegistrationCanBeRetried() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext failed = context(activator, () -> { throw new IllegalArgumentException("denied"); }, () -> {});
        assertThrows(IllegalArgumentException.class, () -> activator.start(failed));
        assertThrows(IllegalStateException.class, activator::bundles);
        BundleContext valid = context(activator, () -> {}, () -> {});
        activator.start(valid);
        activator.stop(valid);
    }

    @Test void reentrantStopDuringRegistrationUnregistersExactlyOnce() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        AtomicInteger removals = new AtomicInteger();
        BundleContext context = context(activator, () -> activator.stop(null), removals::incrementAndGet);
        activator.start(context);
        assertThrows(IllegalStateException.class, activator::bundles);
        activator.stop(context);
        assertEquals(1, removals.get());
    }

    @Test void reentrantStartDuringRegistrationIsRefused() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext nested = context(activator, () -> {}, () -> {});
        BundleContext context = context(activator,
                () -> assertThrows(IllegalStateException.class, () -> activator.start(nested)), () -> {});
        activator.start(context);
        activator.stop(context);
    }

    @Test void reentrantStopDuringUnregistrationIsIdempotent() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        AtomicInteger removals = new AtomicInteger();
        BundleContext context = context(activator, () -> {}, () -> {
            removals.incrementAndGet();
            activator.stop(null);
            assertEquals(0, activator.bundles().size());
        });
        activator.start(context);
        activator.stop(context);
        assertEquals(1, removals.get());
    }

    @Test void registrationCallbackDoesNotHoldLifecycleMonitor() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext context = context(activator, () ->
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                    java.util.concurrent.CompletableFuture.runAsync(() -> activator.stop(null))
                        .get(2, java.util.concurrent.TimeUnit.SECONDS)), () -> {});
        activator.start(context);
        assertThrows(IllegalStateException.class, activator::bundles);
    }

    @Test void nullRegistrationRollsBackAndPermitsRetry() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext missing = proxy(BundleContext.class, (instance, method, args) -> {
            if (method.getName().equals("registerService")) return null;
            throw new AssertionError(method.getName());
        });
        assertThrows(IllegalStateException.class, () -> activator.start(missing));
        assertThrows(IllegalStateException.class, activator::bundles);
        BundleContext valid = context(activator, () -> {}, () -> {});
        activator.start(valid);
        activator.stop(valid);
    }

    @Test void unregisterFailureStillClearsLocalLifecycle() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        IllegalArgumentException expected = new IllegalArgumentException("unregister failed");
        BundleContext broken = context(activator, () -> {}, () -> { throw expected; });
        activator.start(broken);
        assertSame(expected, assertThrows(IllegalArgumentException.class, () -> activator.stop(broken)));
        assertThrows(IllegalStateException.class, activator::bundles);
        activator.stop(broken);
        BundleContext valid = context(activator, () -> {}, () -> {});
        activator.start(valid);
        activator.stop(valid);
    }

    @Test void reentrantStartDuringUnregistrationIsRefused() {
        M3EclipseRuntimeActivator activator = new M3EclipseRuntimeActivator();
        BundleContext nested = context(activator, () -> {}, () -> {});
        BundleContext context = context(activator, () -> {}, () ->
                assertThrows(IllegalStateException.class, () -> activator.start(nested)));
        activator.start(context);
        activator.stop(context);
    }

    private static BundleContext context(M3EclipseRuntimeActivator activator,
            Runnable registering, Runnable unregistering) {
        ServiceRegistration<?> service = proxy(ServiceRegistration.class, (instance, method, args) -> {
            if (method.getName().equals("unregister")) { unregistering.run(); return null; }
            throw new AssertionError(method.getName());
        });
        return proxy(BundleContext.class, (instance, method, args) -> {
            if (method.getName().equals("registerService")) {
                assertSame(activator, args[1]);
                registering.run();
                return service;
            }
            if (method.getName().equals("getBundles")) return new Bundle[0];
            throw new AssertionError(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
