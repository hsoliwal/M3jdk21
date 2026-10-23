// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21: opt-in full flags and compact work nominations; legacy capture retained.
package com.m3.eclipse.framework;

import com.m3.collections.M3Collections;
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongArrayList;
import com.m3.collections.M3ObjectIdentitySet;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

/**
 * Bounded breadth-first, index-parented projection of one original Eclipse resource delta.
 *
 * <p>Retains the original delta nodes, without making another workspace, mutating a
 * resource, using JNI references, or allocating a per-child tree/Entry node. M3 primitive
 * ordinal lanes record ancestry and M3 byte state masks store Eclipse delta kinds.
 * The snapshot follows Eclipse's default getAffectedChildren visibility policy;
 * request a new snapshot after later workspace changes. The original capture operation
 * reads only kinds and children. {@link #captureChanges} additionally captures all
 * flag bits and conservative build-work nominations; resource paths and operations
 * remain on the original Eclipse handles.</p>
 */
public final class M3EclipseResourceDeltaSnapshot {
    /** Case-sensitive Java source filename, including module-info.java. */
    public static final int JAVA_SOURCE = 1;
    /** Case-sensitive .class filename. */
    public static final int CLASS_FILE = 2;
    /** A recognized build or project descriptor filename. */
    public static final int BUILD_DESCRIPTOR = 4;
    /** Workspace root, project, or folder in the captured resource handle. */
    public static final int CONTAINER = 8;
    /** Content must be reconsidered; this does not prove that bytes changed. */
    public static final int CONTENT_DIRTY = 16;
    /** Removed resource or removed phantom. */
    public static final int REMOVED = 32;
    /** Build context must be reconsidered. */
    public static final int CONTEXT_DIRTY = 64;
    /** Descendant context may be invalid, including descendants absent from this delta. */
    public static final int DESCENDANTS_DIRTY = 128;

    private final M3EclipseIndexedSnapshot<IResourceDelta> rows =
            new M3EclipseIndexedSnapshot<>();
    private final M3LongArrayList parents = M3Collections.longArrayList();
    private int[] flags;
    private byte[] workStates;

    private M3EclipseResourceDeltaSnapshot() {}

    public static M3EclipseResourceDeltaSnapshot capture(IResourceDelta root, int maxNodes) {
        Objects.requireNonNull(root, "root");
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        M3EclipseResourceDeltaSnapshot snapshot = new M3EclipseResourceDeltaSnapshot();
        M3ObjectIdentitySet<IResourceDelta> seen = new M3ObjectIdentitySet<>();
        snapshot.rows.add(root, kindMask(root));
        snapshot.parents.add(-1L);
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
                seen.add(child);
            }
        }
        return snapshot;
    }

    /**
     * Captures the existing bounded breadth-first projection, then reads each node's
     * flags and resource handle once in ordinal order. Resource type is read once;
     * filename is read once for files only. Existing {@link #capture} callers do not
     * incur these callbacks or the additional five payload bytes per row.
     *
     * <p>Call while Eclipse supplies a stable delta. Flags retain all 32 bits. Work
     * state is a conservative nomination, not a dependency graph, content digest,
     * workspace mutation, or proof that a compiler/build must execute. A descriptor
     * content change nominates its parent subtree; inherited context propagates only
     * through captured parent ordinals. Existing compiler and build owners resolve
     * dependencies beyond this delta. No resource handle is passed to JNI.</p>
     *
     * <p>Descriptor names are case-sensitive: pom.xml, .classpath, .project,
     * module-info.java, MANIFEST.MF, build.xml, build.gradle, build.gradle.kts,
     * settings.gradle, and settings.gradle.kts. This list is a nomination aid and
     * is not an exhaustive classification of supported build inputs.</p>
     */
    public static M3EclipseResourceDeltaSnapshot captureChanges(
            IResourceDelta root, int maxNodes) {
        M3EclipseResourceDeltaSnapshot snapshot = capture(root, maxNodes);
        int count = snapshot.size();
        int[] capturedFlags = new int[count];
        byte[] states = new byte[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            IResourceDelta delta = snapshot.at(ordinal);
            int value = delta.getFlags();
            IResource resource = Objects.requireNonNull(delta.getResource(), "delta resource");
            capturedFlags[ordinal] = value;
            states[ordinal] = (byte) workState(resource, snapshot.capturedKind(ordinal), value);
        }
        // Descriptor changes affect sibling inputs too. Nominate the owning parent
        // subtree before the single forward ancestry pass, independent of child order.
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int state = Byte.toUnsignedInt(states[ordinal]);
            if ((state & (BUILD_DESCRIPTOR | CONTENT_DIRTY))
                    == (BUILD_DESCRIPTOR | CONTENT_DIRTY)) {
                int parent = snapshot.parentOrdinal(ordinal);
                if (parent >= 0) {
                    states[parent] |= (byte) (CONTEXT_DIRTY | DESCENDANTS_DIRTY);
                }
            }
        }
        for (int ordinal = 1; ordinal < count; ordinal++) {
            int parent = snapshot.parentOrdinal(ordinal);
            if ((states[parent] & DESCENDANTS_DIRTY) != 0) {
                states[ordinal] |= (byte) CONTEXT_DIRTY;
                if ((states[ordinal] & CONTAINER) != 0) {
                    states[ordinal] |= (byte) DESCENDANTS_DIRTY;
                }
            }
        }
        snapshot.flags = capturedFlags;
        snapshot.workStates = states;
        return snapshot;
    }

    private static int workState(IResource resource, int kind, int flags) {
        int type = resource.getType();
        int state = 0;
        if (type == IResource.FILE) {
            String name = Objects.requireNonNull(resource.getName(), "resource name");
            if (name.endsWith(".java")) state |= JAVA_SOURCE;
            if (name.endsWith(".class")) state |= CLASS_FILE;
            if (isBuildDescriptor(name)) state |= BUILD_DESCRIPTOR;
        } else if (type == IResource.ROOT || type == IResource.PROJECT || type == IResource.FOLDER) {
            state |= CONTAINER;
        }
        boolean added = (kind & (IResourceDelta.ADDED | IResourceDelta.ADDED_PHANTOM)) != 0;
        boolean removed = (kind & (IResourceDelta.REMOVED | IResourceDelta.REMOVED_PHANTOM)) != 0;
        if (removed) state |= REMOVED;
        int contentFlags = IResourceDelta.CONTENT | IResourceDelta.ENCODING
                | IResourceDelta.REPLACED | IResourceDelta.TYPE | IResourceDelta.MOVED_FROM
                | IResourceDelta.MOVED_TO | IResourceDelta.COPIED_FROM | IResourceDelta.LOCAL_CHANGED;
        if (added || removed || (flags & contentFlags) != 0) state |= CONTENT_DIRTY;
        int contextFlags = IResourceDelta.DESCRIPTION | IResourceDelta.OPEN | IResourceDelta.TYPE
                | IResourceDelta.REPLACED | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
                | IResourceDelta.COPIED_FROM | IResourceDelta.ENCODING
                | IResourceDelta.LOCAL_CHANGED | IResourceDelta.DERIVED_CHANGED;
        if ((flags & contextFlags) != 0
                || (state & (BUILD_DESCRIPTOR | CONTENT_DIRTY)) == (BUILD_DESCRIPTOR | CONTENT_DIRTY)
                || ((state & CONTAINER) != 0 && (added || removed))) {
            state |= CONTEXT_DIRTY;
        }
        if (((state & CONTAINER) != 0 && (state & (CONTENT_DIRTY | CONTEXT_DIRTY)) != 0)
                || (flags & IResourceDelta.TYPE) != 0) {
            // TYPE also covers folder -> file: the former descendants need invalidation.
            state |= DESCENDANTS_DIRTY;
        }
        return state;
    }

    private static boolean isBuildDescriptor(String name) {
        return switch (name) {
            case "pom.xml", ".classpath", ".project", "module-info.java", "MANIFEST.MF",
                    "build.xml", "build.gradle", "build.gradle.kts", "settings.gradle",
                    "settings.gradle.kts" -> true;
            default -> false;
        };
    }

    /** Whether full flag and work-state lanes were requested during capture. */
    public boolean hasCapturedChanges() { return flags != null; }

    /** All captured Eclipse flag bits, without narrowing to the kind/work byte. */
    public int capturedFlags(int ordinal) {
        requireChanges();
        return flags[Objects.checkIndex(ordinal, size())];
    }

    /** Unsigned work-state byte; see {@link #captureChanges} for its limits. */
    public int capturedWorkState(int ordinal) {
        requireChanges();
        return Byte.toUnsignedInt(workStates[Objects.checkIndex(ordinal, size())]);
    }

    /** Full-width flag selection, preserving original references and breadth-first order. */
    public List<IResourceDelta> selectByFlags(int requiredFlags, int excludedFlags) {
        requireChanges();
        if ((requiredFlags & excludedFlags) != 0) return List.of();
        List<IResourceDelta> selected = M3Collections.arrayList();
        for (int ordinal = 0; ordinal < flags.length; ordinal++) {
            int value = flags[ordinal];
            if ((value & requiredFlags) == requiredFlags && (value & excludedFlags) == 0) {
                selected.add(at(ordinal));
            }
        }
        return List.copyOf(selected);
    }

    /**
     * Detached ascending ordinals selected through the existing primitive mask owner.
     * Masks must fit an unsigned byte. NATIVE_REQUIRED preserves that owner's explicit
     * failure when its compatible JNI library is unavailable; it never falls back to Java.
     */
    public int[] selectWorkOrdinals(int requiredState, int excludedState,
            M3IndexMaskCompactor.Backend backend) {
        requireChanges();
        return M3IndexMaskCompactor.selectPositions(workStates, requiredState, excludedState, backend);
    }

    private void requireChanges() {
        if (flags == null) {
            throw new IllegalStateException("flags and work state require captureChanges");
        }
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
