// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

/** Semantic meaning of an interaction between immutable indexed values. */
public enum M3InteractionKind {
    LOGIC,
    EVIDENCE,
    REASON,
    DISCUSSION,
    ARGUMENT,
    BRAINSTORM,
    CONTEXT_ACTION,
    STRUCTURAL,
    DEPENDENCY,
    EMOTIONAL_CONTEXT
}
