// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3LongLongHashMap;
import java.util.List;
import java.util.Objects;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Read-only Equinox/OSGi inventory over M3 primitive ID-to-handle and state-mask owners.
 *
 * <p>No bundle is started, resolved, registered or stopped. Captured state bits represent
 * one observation, not a live lifecycle subscription; obtain a fresh snapshot after
 * registry changes. Bundle object identity stays with the original OSGi framework.</p>
 */
public final class M3EclipseBundleSnapshot {
    private final M3EclipseIndexedSnapshot<Bundle> rows = new M3EclipseIndexedSnapshot<>();
    private final M3LongLongHashMap byId = new M3LongLongHashMap();

    private M3EclipseBundleSnapshot() {}

    public static M3EclipseBundleSnapshot capture(BundleContext context) {
        Objects.requireNonNull(context, "context");
        M3EclipseBundleSnapshot result = new M3EclipseBundleSnapshot();
        Bundle[] bundles = context.getBundles();
        if (bundles == null) return result;
        for (Bundle bundle : bundles) {
            Objects.requireNonNull(bundle, "bundle");
            long id = bundle.getBundleId();
            if (id < 0L || result.byId.containsKey(id)) {
                throw new IllegalArgumentException("invalid/duplicate OSGi bundle ID: " + id);
            }
            int state = bundle.getState();
            if (Integer.bitCount(state) != 1 || (state & ~0x3f) != 0) {
                throw new IllegalArgumentException("invalid OSGi bundle state: " + state);
            }
            long handle = result.rows.add(bundle, state);
            if (!result.byId.put(id, handle)) {
                throw new IllegalStateException("OSGi bundle duplicate after preflight: " + id);
            }
        }
        return result;
    }

    public int size() { return rows.size(); }

    public Bundle at(int ordinal) { return rows.at(ordinal); }

    public int capturedState(int ordinal) { return rows.state(ordinal); }

    /** Return the bundle reference captured for the given OSGi ID, or null if absent. */
    public Bundle byId(long bundleId) {
        if (!byId.containsKey(bundleId)) return null;
        return rows.atHandle(byId.getOrDefault(bundleId, 0L));
    }

    /** Exact M3 mask algebra over capture-time bundle states. */
    public List<Bundle> select(int requiredStateMask, int excludedStateMask) {
        return rows.selected(requiredStateMask, excludedStateMask);
    }

    public List<Bundle> all() { return rows.all(); }
}
