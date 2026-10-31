// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import org.eclipse.core.resources.IResourceDelta;

/** OSGi-published read-only Eclipse-to-M3 index entrypoints; Eclipse owns all live services. */
public interface M3EclipseRuntimeServices {
    M3EclipseBundleSnapshot bundles();
    M3EclipseExtensionSnapshot extensions();
    M3EclipseResourceDeltaSnapshot resourceDelta(IResourceDelta root, int maxNodes);
}
