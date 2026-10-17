// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3Collections;
import com.m3.collections.M3LongArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.resources.IResourceDelta;

/**
 * Bounded breadth-first, index-parented projection of one original Eclipse resource delta.
 *
 * <p>Retains the original delta nodes, without making another workspace, mutating a
 * resource, using JNI references, or allocating a per-child tree/Entry node. M3 primitive
 * ordinal lanes record ancestry and M3 byte state masks store Eclipse delta kinds.
 * The snapshot follows Eclipse's default getAffectedChildren visibility policy;
 * request a new snapshot after later workspace changes. Path/flag behavior remains
 * on each original IResourceDelta, rather than a lossy M3 reimplementation.</p>
 */
public final class M3EclipseResourceDeltaSnapshot {
    private final M3EclipseIndexedSnapshot<IResourceDelta> rows =
            new M3EclipseIndexedSnapshot<>();
    private final M3LongArrayList parents = M3Collections.longArrayList();

    private M3EclipseResourceDeltaSnapshot() {}

    public static M3EclipseResourceDeltaSnapshot capture(IResourceDelta root, int maxNodes) {
        Objects.requireNonNull(root, "root");
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        M3EclipseResourceDeltaSnapshot snapshot = new M3EclipseResourceDeltaSnapshot();
        IdentityHashMap<IResourceDelta, Boolean> seen = M3Collections.identityMap();
        snapshot.rows.add(root, kindMask(root));
        snapshot.parents.add(-1L);
        seen.put(root, Boolean.TRUE);
        for (int ordinal = 0; ordinal < snapshot.rows.size(); ordinal++) {
            IResourceDelta[] children = snapshot.rows.at(ordinal).getAffectedChildren();
            if (children == null) {
                throw new IllegalStateException("Eclipse returned null delta children");
            }
            for (IResourceDelta child : children) {
                Objects.requireNonNull(child, "child resource delta");
                if (seen.containsKey(child)) {
                    throw new IllegalStateException("cyclic/shared Eclipse resource delta");
                }
                if (snapshot.rows.size() >= maxNodes) {
                    throw new IllegalStateException("M3 resource delta snapshot budget exceeded");
                }
                snapshot.rows.add(child, kindMask(child));
                snapshot.parents.add(ordinal);
                seen.put(child, Boolean.TRUE);
            }
        }
        return snapshot;
    }

    private static int kindMask(IResourceDelta delta) {
        int kind = delta.getKind();
        if ((kind & ~0xff) != 0 || kind < 0) {
            throw new IllegalArgumentException("Eclipse delta kind outside M3 state byte: " + kind);
        }
        return kind;
    }

    public int size() { return rows.size(); }

    public IResourceDelta at(int ordinal) { return rows.at(ordinal); }

    public int parentOrdinal(int ordinal) { return Math.toIntExact(parents.get(ordinal)); }

    public int capturedKind(int ordinal) { return rows.state(ordinal); }

    /** Primitive-mask selected original delta references in snapshot breadth-first order. */
    public List<IResourceDelta> select(int requiredKindMask, int excludedKindMask) {
        return rows.selected(requiredKindMask, excludedKindMask);
    }

    public List<IResourceDelta> all() { return rows.all(); }
}
