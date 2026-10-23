// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3ObjectStateSlots28;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;

/**
 * M3 indexed view of Eclipse's existing extension registry, not a second registry.
 *
 * <p>Only the Eclipse registry may own extension contributions and executable loading.
 * This type never implements IExtensionRegistry, which explicitly forbids client
 * implementations. Cached extension points can become invalid after registry changes;
 * {@link #livePoint(String)} forwards to the canonical registry instead.</p>
 */
public final class M3EclipseExtensionSnapshot {
    public static final int VALID_AT_CAPTURE = M3ObjectStateSlots28.VISIBLE;
    public static final int INVALID_AT_CAPTURE = M3ObjectStateSlots28.DIRTY;

    private final IExtensionRegistry registry;
    private final M3EclipseIndexedSnapshot<IExtensionPoint> rows =
            new M3EclipseIndexedSnapshot<>();

    private M3EclipseExtensionSnapshot(IExtensionRegistry registry) {
        this.registry = registry;
    }

    public static M3EclipseExtensionSnapshot capture(IExtensionRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        M3EclipseExtensionSnapshot snapshot = new M3EclipseExtensionSnapshot(registry);
        IExtensionPoint[] points = registry.getExtensionPoints();
        if (points == null) throw new IllegalStateException("Eclipse returned null extension points");
        for (IExtensionPoint point : points) {
            Objects.requireNonNull(point, "extension point");
            snapshot.rows.add(point,
                    point.isValid() ? VALID_AT_CAPTURE : INVALID_AT_CAPTURE);
        }
        return snapshot;
    }

    public int size() { return rows.size(); }

    public IExtensionPoint at(int ordinal) { return rows.at(ordinal); }

    public int capturedState(int ordinal) { return rows.state(ordinal); }

    public List<IExtensionPoint> select(int requiredMask, int excludedMask) {
        return rows.selected(requiredMask, excludedMask);
    }

    public List<IExtensionPoint> all() { return rows.all(); }

    /** Directly delegate live identifier lookup; do not cache a second registry. */
    public IExtensionPoint livePoint(String identifier) {
        return registry.getExtensionPoint(Objects.requireNonNull(identifier, "identifier"));
    }

    /** Re-enumerate the canonical registry for a fresh M3 snapshot. */
    public M3EclipseExtensionSnapshot refresh() { return capture(registry); }
}
