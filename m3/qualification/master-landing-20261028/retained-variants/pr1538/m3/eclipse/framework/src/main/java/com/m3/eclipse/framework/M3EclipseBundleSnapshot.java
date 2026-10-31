// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3Collections;
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongArrayList;
import com.m3.collections.M3LongIndexProjection;
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
    private final M3LongArrayList capturedIds = M3Collections.longArrayList();

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
            result.capturedIds.add(id);
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

    /** Detached signed-long bundle ID order, never changing framework bundle references. */
    public List<Bundle> sortedById(boolean descending) {
        return sortedById(0, 0, descending, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** State masks select snapshot occurrences; sort permutes only M3 primitive indexes. */
    public List<Bundle> sortedById(int requiredStateMask, int excludedStateMask,
            boolean descending, M3IndexMaskCompactor.Backend backend) {
        return materialize(sortedIndex(requiredStateMask, excludedStateMask, descending, backend));
    }

    /** Logical half-open window after filtering and sorting, with only window references copied. */
    public List<Bundle> sortedWindowById(int requiredStateMask, int excludedStateMask,
            boolean descending, int offset, int length, M3IndexMaskCompactor.Backend backend) {
        M3LongIndexProjection sorted = sortedIndex(
                requiredStateMask, excludedStateMask, descending, backend);
        return materialize(sorted.slice(offset, length));
    }

    private M3LongIndexProjection sortedIndex(int requiredStateMask, int excludedStateMask,
            boolean descending, M3IndexMaskCompactor.Backend backend) {
        byte[] states = new byte[rows.size()];
        for (int i = 0; i < states.length; i++) states[i] = (byte) rows.state(i);
        // Keep the existing required/excluded selector and projection owners. The
        // selector returns capture ordinals; only primitive IDs enter the sort lane.
        int[] positions = M3IndexMaskCompactor.selectPositions(
                states, requiredStateMask, excludedStateMask, backend);
        long[] selectedIds = new long[positions.length];
        for (int i = 0; i < positions.length; i++) selectedIds[i] = capturedIds.get(positions[i]);
        M3LongIndexProjection filtered = M3LongIndexProjection.snapshot(selectedIds);
        return descending ? filtered.sortedDescending() : filtered.sortedAscending();
    }

    private List<Bundle> materialize(M3LongIndexProjection projection) {
        List<Bundle> result = M3Collections.arrayList();
        for (int i = 0; i < projection.size(); i++) {
            result.add(byId(projection.get(i)));
        }
        return List.copyOf(result);
    }

    public List<Bundle> all() { return rows.all(); }
}
