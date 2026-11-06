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
    private ServiceRegistration<M3EclipseRuntimeServices> service;
    private enum Lifecycle { STOPPED, STARTING, ACTIVE, STOPPING }
    private Lifecycle lifecycle = Lifecycle.STOPPED;
    private boolean stopRequested;

    public M3EclipseRuntimeActivator() { }

    @Override
    public void start(BundleContext newContext) {
        Objects.requireNonNull(newContext, "context");
        synchronized (this) {
            if (lifecycle != Lifecycle.STOPPED) {
                throw new IllegalStateException("M3 Eclipse forwarder already started");
            }
            lifecycle = Lifecycle.STARTING;
            context = newContext;
        }
        ServiceRegistration<M3EclipseRuntimeServices> registered;
        try {
            // REGISTERED is synchronous: publish usable context before the framework call.
            // Never hold our monitor across framework callbacks.
            registered = newContext.registerService(M3EclipseRuntimeServices.class, this, null);
            if (registered == null) {
                throw new IllegalStateException("OSGi returned no service registration");
            }
        } catch (RuntimeException | Error failure) {
            synchronized (this) {
                context = null;
                stopRequested = false;
                lifecycle = Lifecycle.STOPPED;
            }
            throw failure;
        }
        boolean stopAfterRegistration;
        synchronized (this) {
            service = registered;
            stopAfterRegistration = stopRequested;
            lifecycle = stopAfterRegistration ? Lifecycle.STOPPING : Lifecycle.ACTIVE;
            stopRequested = false;
        }
        // A REGISTERED callback may stop us before registerService returns its handle.
        if (stopAfterRegistration) unregister(registered);
    }

    @Override
    public void stop(BundleContext stoppedContext) {
        ServiceRegistration<M3EclipseRuntimeServices> registered;
        synchronized (this) {
            if (lifecycle == Lifecycle.STARTING) {
                stopRequested = true;
                return;
            }
            if (lifecycle != Lifecycle.ACTIVE) return;
            lifecycle = Lifecycle.STOPPING;
            registered = service;
        }
        unregister(registered);
    }

    private void unregister(ServiceRegistration<M3EclipseRuntimeServices> registered) {
        try {
            // UNREGISTERING callbacks may still obtain and use this service.
            registered.unregister();
        } finally {
            synchronized (this) {
                service = null;
                context = null;
                lifecycle = Lifecycle.STOPPED;
            }
        }
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
