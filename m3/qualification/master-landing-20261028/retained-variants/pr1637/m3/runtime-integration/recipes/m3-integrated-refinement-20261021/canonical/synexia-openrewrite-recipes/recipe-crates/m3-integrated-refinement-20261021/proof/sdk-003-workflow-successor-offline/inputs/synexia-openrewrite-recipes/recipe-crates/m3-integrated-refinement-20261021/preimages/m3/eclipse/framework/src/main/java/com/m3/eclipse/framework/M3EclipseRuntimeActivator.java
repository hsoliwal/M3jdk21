// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import java.util.Objects;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Actual Equinox entrypoint: publish an OSGi service that forwards to existing Eclipse
 * providers and existing M3 indexed storage. No replacement workbench, service registry,
 * thread or native SWT control is created. The framework remains responsible for load,
 * activation, service availability and classloader identity.
 */
public final class M3EclipseRuntimeActivator
        implements BundleActivator, M3EclipseRuntimeServices {
    private volatile BundleContext context;
    private volatile ServiceRegistration<M3EclipseRuntimeServices> service;

    public M3EclipseRuntimeActivator() { }

    @Override
    public synchronized void start(BundleContext newContext) {
        Objects.requireNonNull(newContext, "context");
        if (context != null || service != null) {
            throw new IllegalStateException("M3 Eclipse forwarder already started");
        }
        ServiceRegistration<M3EclipseRuntimeServices> registered =
                newContext.registerService(M3EclipseRuntimeServices.class, this, null);
        if (registered == null) {
            throw new IllegalStateException("OSGi returned no service registration");
        }
        context = newContext;
        service = registered;
    }

    @Override
    public synchronized void stop(BundleContext stoppedContext) {
        ServiceRegistration<M3EclipseRuntimeServices> registered = service;
        service = null;
        context = null;
        if (registered != null) registered.unregister();
    }

    @Override
    public M3EclipseBundleSnapshot bundles() {
        return M3EclipseBundleSnapshot.capture(requireActiveContext());
    }

    @Override
    public M3EclipseExtensionSnapshot extensions() {
        requireActiveContext();
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        if (registry == null) {
            throw new IllegalStateException("Eclipse extension registry is not available");
        }
        return M3EclipseExtensionSnapshot.capture(registry);
    }

    @Override
    public M3EclipseResourceDeltaSnapshot resourceDelta(IResourceDelta root, int maxNodes) {
        requireActiveContext();
        return M3EclipseResourceDeltaSnapshot.capture(root, maxNodes);
    }

    private BundleContext requireActiveContext() {
        BundleContext current = context;
        if (current == null) throw new IllegalStateException("M3 Eclipse forwarding bundle is stopped");
        return current;
    }
}
