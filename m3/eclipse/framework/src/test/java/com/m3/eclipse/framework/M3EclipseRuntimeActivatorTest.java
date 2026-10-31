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
            case "getFlags" -> 0;
            case "getAffectedChildren" -> new IResourceDelta[0];
            default -> throw new AssertionError(method.getName());
        });
        M3EclipseResourceDeltaSnapshot deltas = activator.resourceDelta(root, 1);
        assertEquals(1, deltas.size());
        assertEquals(0, deltas.capturedFlags(0));
        activator.stop(context);
        activator.stop(context);
        assertEquals(1, unregistered.get());
        assertThrows(IllegalStateException.class, activator::bundles);
        assertThrows(IllegalStateException.class, () -> activator.resourceDelta(root, 1));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
