// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Zero-entry, offset-aware selection of already verified L0 AST occurrences.
 *
 * <p>This is a read-only query over {@link M3ASTHierarchy}, not a parser, a
 * second AST owner, a source mutation, or a promotion/verification authority.
 * One family mask describes the file's collection category; one unsigned byte
 * describes each occurrence's behavior/state. Both masks must contain every
 * requested bit. Children remain dense occurrence coordinates under L0 files.
 *
 * <p>Counting and cursor traversal allocate no element objects or intermediate
 * arrays. Snapshot projections allocate exactly one final {@code int[]} (also
 * when there are zero matches), taking O(files + occurrences) time. Immutable
 * hierarchy custody makes two-pass output sizing safe. Iteration is stable in
 * source's canonical file/occurrence order and retains duplicate atom handles.
 */
public final class M3ASTHierarchyMaskSelection {
    private M3ASTHierarchyMaskSelection() { }

    /** Count logical L0 occurrences selected by both file-family and state bits. */
    public static int count(M3ASTHierarchy source, int families, int stateMask) {
        return scan(source, families, stateMask, null, 0);
    }

    /**
     * Export globally stable occurrence coordinates into one exact-sized array.
     * The returned array never aliases the hierarchy's primitive owner lanes.
     */
    public static int[] coordinates(M3ASTHierarchy source, int families, int stateMask) {
        int n = count(source, families, stateMask);
        int[] output = new int[n];
        if (scan(source, families, stateMask, output, 0) != n) {
            throw new IllegalStateException("immutable AST hierarchy changed between scans");
        }
        return output;
    }

    /** Export selected canonical atom handles into a single exact-sized primitive array. */
    public static int[] atomHandles(M3ASTHierarchy source, int families, int stateMask) {
        int n = count(source, families, stateMask);
        int[] output = new int[n];
        if (scan(source, families, stateMask, output, 1) != n) {
            throw new IllegalStateException("immutable AST hierarchy changed between scans");
        }
        return output;
    }

    /** Visit selected coordinates without a materialized selection or boxed indices. */
    public static void forEachCoordinate(M3ASTHierarchy source, int families,
                                         int stateMask, IntConsumer action) {
        Objects.requireNonNull(action, "action");
        M3ASTHierarchy hierarchy = Objects.requireNonNull(source, "source");
        validate(families, stateMask);
        M3ASTHierarchy.Plane files = hierarchy.level(
                M3ASTHierarchy.Level.L0_FILE_FACTS);
        for (int file = 0; file < files.size(); file++) {
            if ((files.familyMask(file) & families) != families) continue;
            int start = files.firstChild(file), end = start + files.childCount(file);
            for (int index = start; index < end; index++) {
                if ((hierarchy.stateMask(index) & stateMask) == stateMask) {
                    action.accept(index);
                }
            }
        }
    }

    /** 0 selects all; larger masks must be within the sealed family/state domains. */
    private static void validate(int families, int stateMask) {
        if ((families & ~M3ASTHierarchy.ALL_COLLECTION_FAMILIES) != 0
                || (stateMask & ~0xff) != 0) {
            throw new IllegalArgumentException("invalid collection family or unsigned state mask");
        }
    }

    /** Count when destination is null; otherwise mode 0 emits coordinates, mode 1 handles. */
    private static int scan(M3ASTHierarchy hierarchy, int families,
                            int stateMask, int[] destination, int mode) {
        Objects.requireNonNull(hierarchy, "source");
        validate(families, stateMask);
        M3ASTHierarchy.Plane files = hierarchy.level(
                M3ASTHierarchy.Level.L0_FILE_FACTS);
        int matches = 0;
        for (int file = 0; file < files.size(); file++) {
            if ((files.familyMask(file) & families) != families) continue;
            int start = files.firstChild(file), end = start + files.childCount(file);
            for (int index = start; index < end; index++) {
                if ((hierarchy.stateMask(index) & stateMask) == stateMask) {
                    if (destination != null) {
                        destination[matches] = mode == 1 ? hierarchy.atomHandle(index) : index;
                    }
                    matches++;
                }
            }
        }
        return matches;
    }
}
