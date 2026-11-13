// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Canonical algorithm families of M3JDK (package jdk.m3.algorithm): array, interval and greedy, dynamic
 * programming, string, stack, heap, linked list, tree, matrix, backtracking and bit shapes, each loop observing the
 * M3ProgressMonitor cancellation contract, plus the catalog that names them. Relocated by recipe from the m3scale
 * donor (recipe/families-20261106). Java 21; requires jdk.m3.collections, the progress-monitor owner the
 * M3ProgressMonitor adapter extends (make/m3/qualification/progress-monitor-unify-20261106).
 */
module org.m3jdk.algorithms {
    requires transitive jdk.m3.collections;
    exports jdk.m3.algorithm;
}
