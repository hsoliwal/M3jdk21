// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import java.util.Arrays;

/** Immutable primitive row. All arrays originate inside the pool and are never exposed. */
final class MIndexASTNode {
    final int kind;
    final int[] attributes;
    final int[] roles;
    final int[] children;
    final int depth;
    final long expandedSize;
    final String fingerprint;
    final int hash;

    MIndexASTNode(int kind, int[] attributes, int[] roles, int[] children,
            int depth, long expandedSize, String fingerprint) {
        this.kind = kind;
        this.attributes = attributes;
        this.roles = roles;
        this.children = children;
        this.depth = depth;
        this.expandedSize = expandedSize;
        this.fingerprint = fingerprint;
        this.hash = keyHash(kind, attributes, roles, children);
    }

    static int keyHash(int kind, int[] attributes, int[] roles, int[] children) {
        int hash = 31 * (31 * (31 + kind) + Arrays.hashCode(attributes)) + Arrays.hashCode(roles);
        return 31 * hash + Arrays.hashCode(children);
    }

    boolean matches(int candidateKind, int[] candidateAttributes, int[] candidateRoles, int[] candidateChildren) {
        return kind == candidateKind && Arrays.equals(attributes, candidateAttributes)
                && Arrays.equals(roles, candidateRoles) && Arrays.equals(children, candidateChildren);
    }
}
