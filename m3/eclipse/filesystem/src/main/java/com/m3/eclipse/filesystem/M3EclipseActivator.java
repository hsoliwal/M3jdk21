// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import java.util.Arrays;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** Closes provider-owned images when the bundle stops. */
public final class M3EclipseActivator implements BundleActivator {
    private static M3EclipseFileSystem[] systems = new M3EclipseFileSystem[4];
    private static int size;

    static synchronized void track(M3EclipseFileSystem system) {
        if (size == systems.length) systems = Arrays.copyOf(systems, Math.multiplyExact(size, 2));
        systems[size++] = system;
    }

    static synchronized void untrack(M3EclipseFileSystem system) {
        for (int i = 0; i < size; i++) {
            if (systems[i] == system) {
                System.arraycopy(systems, i + 1, systems, i, size - i - 1);
                systems[--size] = null;
                return;
            }
        }
    }

    @Override public void start(BundleContext context) { }

    @Override public void stop(BundleContext context) {
        M3EclipseFileSystem[] closing;
        synchronized (M3EclipseActivator.class) { closing = Arrays.copyOf(systems, size); }
        for (M3EclipseFileSystem system : closing) system.close();
    }
}
