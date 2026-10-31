// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.Objects;

/**
 * Deterministic interaction classifier over immutable indexed domains.
 *
 * <p>The participating values never gain mutable semantic state. Meaning belongs to the
 * relationship: the same indexed String/atom can be evidence in one composition, a reason in
 * another, and discussion/context material in another.</p>
 */
public final class M3Interaction {
    private M3Interaction() { }

    public static M3InteractionKind classify(
            M3AtomDomain left,
            M3AtomDomain right,
            M3InteractionMode mode) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(mode, "mode");

        if (left == M3AtomDomain.AST || right == M3AtomDomain.AST) {
            return M3InteractionKind.STRUCTURAL;
        }
        if (left == M3AtomDomain.DAG || right == M3AtomDomain.DAG) {
            return M3InteractionKind.DEPENDENCY;
        }
        if (left == M3AtomDomain.EMOTION || right == M3AtomDomain.EMOTION) {
            return M3InteractionKind.EMOTIONAL_CONTEXT;
        }
        if (right == M3AtomDomain.FACT || mode == M3InteractionMode.SUPPORT) {
            return M3InteractionKind.EVIDENCE;
        }
        if (right == M3AtomDomain.LOGIC || mode == M3InteractionMode.DERIVE) {
            return M3InteractionKind.LOGIC;
        }
        if (right == M3AtomDomain.REASON) {
            return M3InteractionKind.REASON;
        }
        if (right == M3AtomDomain.CONTEXT || mode == M3InteractionMode.APPLY) {
            return M3InteractionKind.CONTEXT_ACTION;
        }
        if (mode == M3InteractionMode.OPPOSE) {
            return M3InteractionKind.ARGUMENT;
        }
        if (mode == M3InteractionMode.EXPLORE) {
            return M3InteractionKind.BRAINSTORM;
        }
        return M3InteractionKind.DISCUSSION;
    }
}
