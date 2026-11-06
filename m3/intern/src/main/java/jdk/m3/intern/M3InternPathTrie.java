// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.util.Objects;

/**
 * Interned, prefix-sharing lexical paths inside an existing {@link M3InternSpace}.
 *
 * <p>Every distinct prefix is one kind-tagged primitive tuple in the existing
 * shape store, not a linked heap object, new hash map or second string pool.
 * A path node contains (root, parent, UTF-16 atom ID, depth). Insertion and
 * canonicalization reuse the existing collision-verified tuple interner.
 * Scope is an opaque caller-assigned filesystem identity, not a class-loader
 * hashCode or an OS-global identifier. A handle is meaningful only in its
 * originating space and must not be compared across independent spaces.</p>
 *
 * <p>Segments are exact Java UTF-16 values: empty segments, slash characters,
 * dot components, unpaired surrogates and case differences are preserved.
 * Nothing resolves symlinks, case folding, normalization or filesystem I/O.
 * Once the underlying space is frozen, existing paths remain addressable but
 * new segments or paths are refused; use {@link #findExisting(String...)} for
 * an explicitly read-only lookup on a frozen space.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/main/java/com/synexia/common/intern/M3InternPathTrie.java blob a1a834294f37; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class M3InternPathTrie {
    private static final int ROOT_KIND = 0x4d335052; // M3PR
    private static final int NODE_KIND = 0x4d33504e; // M3PN
    private static final int MAX_DEPTH = 1_000_000;

    private final M3InternSpace space;
    private final long filesystemScope;
    private final long root;

    /** Create only a small facade; all entries live in the shared shape store. */
    public M3InternPathTrie(M3InternSpace space, long filesystemScope) {
        this.space = Objects.requireNonNull(space, "space");
        if (filesystemScope < 0) {
            throw new IllegalArgumentException("negative filesystem scope");
        }
        this.filesystemScope = filesystemScope;
        this.root = space.shapes().intern(ROOT_KIND, filesystemScope);
    }

    public long filesystemScope() {
        return filesystemScope;
    }

    /** Canonical empty path node for this scope (depth zero). */
    public long root() {
        return root;
    }

    /**
     * Intern all segments serially. The varargs array is copied at entry so
     * a concurrent caller reassigning its slots cannot change this operation.
     * The contained String values are immutable by Java contract.
     */
    public long intern(String... segments) {
        String[] owned = Objects.requireNonNull(segments, "segments").clone();
        if (owned.length > MAX_DEPTH) {
            throw new IllegalArgumentException("path depth exceeds budget");
        }
        long node = root;
        for (String segment : owned) {
            node = append(node, Objects.requireNonNull(segment, "segment"));
        }
        return node;
    }

    /**
     * Append one exact segment to an existing node. Insertion is O(1) expected
     * after interning the segment, subject to collision-chain verification.
     */
    public long append(long parent, String segment) {
        int parentDepth = depth(parent);
        if (parentDepth == MAX_DEPTH) {
            throw new IllegalArgumentException("path depth exceeds budget");
        }
        long atom = space.internString(Objects.requireNonNull(segment, "segment"));
        return space.shapes().intern(NODE_KIND, root, parent, atom, parentDepth + 1L);
    }

    /**
     * Lookup without mutation in a frozen space. Returns zero if any segment
     * or prefix was never interned, preserving the space's one-based handles.
     * Before freeze, refuse rather than accidentally interning unknown paths.
     */
    public long findExisting(String... segments) {
        if (!space.isFrozen()) {
            throw new IllegalStateException("freeze the space before read-only lookup");
        }
        String[] owned = Objects.requireNonNull(segments, "segments").clone();
        for (String segment : owned) Objects.requireNonNull(segment, "segment");
        try {
            return intern(owned);
        } catch (IllegalStateException notPresent) {
            // Both existing interners check collision-equal entries BEFORE
            // rejecting new entries on a frozen store.
            return 0;
        }
    }

    /** Number of lexical segments, or zero for the canonical root. */
    public int depth(long node) {
        if (node == root) return 0;
        return (int) nodeField(node, 3);
    }

    /** Return zero at root; otherwise return the canonical parent handle. */
    public long parent(long node) {
        return node == root ? 0 : nodeField(node, 1);
    }

    /** Read the last segment without producing or flattening the whole path. */
    public String lastSegment(long node) {
        if (node == root) throw new IllegalArgumentException("root has no segment");
        return space.string(nodeField(node, 2));
    }

    /** Explicit independent string-array escape; intermediate entries are not objects. */
    public String[] segments(long node) {
        int count = depth(node);
        String[] result = new String[count];
        long current = node;
        for (int i = count - 1; i >= 0; i--) {
            if (nodeField(current, 3) != i + 1L) {
                throw new IllegalStateException("path depth/parent mismatch");
            }
            result[i] = space.string(nodeField(current, 2));
            current = nodeField(current, 1);
        }
        if (current != root) {
            throw new IllegalStateException("path root/parent mismatch");
        }
        return result;
    }

    /**
     * Tests the original prefix relationship in O(depth difference), without
     * string allocation, hashing or touching immutable segment contents.
     */
    public boolean isPrefix(long candidatePrefix, long path) {
        int prefixDepth = depth(candidatePrefix);
        int pathDepth = depth(path);
        if (prefixDepth > pathDepth) return false;
        long position = path;
        for (int depth = pathDepth; depth > prefixDepth; depth--) {
            position = nodeField(position, 1);
        }
        return position == candidatePrefix;
    }

    /**
     * Number of shared leading segments (not UTF-16 characters). Handles
     * short and long path pairs by aligning depth and ascending canonical
     * parent links; no arrays or facade objects are materialized.
     */
    public int commonPrefixLength(long first, long second) {
        int leftDepth = depth(first);
        int rightDepth = depth(second);
        long a = first;
        long b = second;
        while (leftDepth > rightDepth) {
            a = nodeField(a, 1);
            leftDepth--;
        }
        while (rightDepth > leftDepth) {
            b = nodeField(b, 1);
            rightDepth--;
        }
        while (a != b) {
            a = nodeField(a, 1);
            b = nodeField(b, 1);
            leftDepth--;
        }
        return leftDepth;
    }

    /**
     * Original node validation with scalar reads instead of a new long[4]
     * allocation on every parent/prefix traversal. Scope, tuple kind,
     * arity, positive parent and bounded depth are still checked.
     */
    private long nodeField(long handle, int field) {
        M3InternSpace.TupleStore store = space.shapes();
        long parent = store.scopedMember(handle, NODE_KIND, 4, root, 1);
        long depth = store.scopedMember(handle, NODE_KIND, 4, root, 3);
        if (parent <= 0 || depth < 1 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("path node belongs to a different scope");
        }
        if (field == 1) return parent;
        if (field == 3) return depth;
        return store.scopedMember(handle, NODE_KIND, 4, root, field);
    }
}
