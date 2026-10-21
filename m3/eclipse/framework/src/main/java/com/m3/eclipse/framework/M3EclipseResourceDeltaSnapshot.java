// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3Collections;
import com.m3.collections.M3LongArrayList;
import com.m3.collections.M3ObjectIdentitySet;
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
    /** Full 32-bit Eclipse IResourceDelta flag words in physical BFS order. */
    private final M3LongArrayList flags = M3Collections.longArrayList();

    private M3EclipseResourceDeltaSnapshot() {}

    public static M3EclipseResourceDeltaSnapshot capture(IResourceDelta root, int maxNodes) {
        Objects.requireNonNull(root, "root");
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        M3EclipseResourceDeltaSnapshot snapshot = new M3EclipseResourceDeltaSnapshot();
        M3ObjectIdentitySet<IResourceDelta> seen = new M3ObjectIdentitySet<>();
        snapshot.rows.add(root, kindMask(root));
        snapshot.parents.add(-1L);
        snapshot.flags.add(Integer.toUnsignedLong(root.getFlags()));
        seen.add(root);
        for (int ordinal = 0; ordinal < snapshot.rows.size(); ordinal++) {
            IResourceDelta[] children = snapshot.rows.at(ordinal).getAffectedChildren();
            if (children == null) {
                throw new IllegalStateException("Eclipse returned null delta children");
            }
            for (IResourceDelta child : children) {
                Objects.requireNonNull(child, "child resource delta");
                if (seen.contains(child)) {
                    throw new IllegalStateException("cyclic/shared Eclipse resource delta");
                }
                if (snapshot.rows.size() >= maxNodes) {
                    throw new IllegalStateException("M3 resource delta snapshot budget exceeded");
                }
                snapshot.rows.add(child, kindMask(child));
                snapshot.parents.add(ordinal);
                snapshot.flags.add(Integer.toUnsignedLong(child.getFlags()));
                seen.add(child);
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

    /** Full flags at capture time, including high bits; no lossy byte truncation. */
    public int capturedFlags(int ordinal) { return (int) flags.get(ordinal); }

    /**
     * Bitwise required/excluded selection against saved full-width flag words.
     * This is an indexed projection only. It does not re-open workspace resources
     * or replace Eclipse's original delta flags, identity or notification order.
     */
    public List<IResourceDelta> selectByFlags(int requiredMask, int excludedMask) {
        List<IResourceDelta> selected = M3Collections.arrayList();
        for (int ordinal = 0; ordinal < rows.size(); ordinal++) {
            int observed = (int) flags.get(ordinal);
            if ((observed & requiredMask) == requiredMask
                    && (observed & excludedMask) == 0) {
                selected.add(rows.at(ordinal));
            }
        }
        return List.copyOf(selected);
    }

    /** Primitive-mask selected original delta references in snapshot breadth-first order. */
    public List<IResourceDelta> select(int requiredKindMask, int excludedKindMask) {
        return rows.selected(requiredKindMask, excludedKindMask);
    }

    public List<IResourceDelta> all() { return rows.all(); }
}
