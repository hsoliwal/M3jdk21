// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

/** Semantic role of one immutable M3 backport DAG node. */
public enum M3DagKind {
    INVENTORY,
    COMPATIBILITY,
    RECIPE,
    VERIFICATION,
    PROMOTION
}
