/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

/**
 * A measure with a size that never decreases as a prefix grows, so a rope can be searched by it.
 *
 * <p>{@link M3MeasuredRope#seek(M3RopeMetric, long)} returns the smallest UTF-16 offset {@code p} with
 * {@code size(prefix(p)) > target}, descending one path and scanning at most one leaf (Swift Collections
 * {@code RopeMetric} and {@code Rope.find(at:in:preferEnd:false)}, pattern only).</p>
 */
public interface M3RopeMetric extends M3RopeMeasure {
    /** Size of the summary at {@code summary[at ..]} in this metric. */
    long size(long[] summary, int at);
}
