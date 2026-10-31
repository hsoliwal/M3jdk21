// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

/**
 * Proving-ground mirror of {@code java.lang.M3StringTuple}: a persistent composition node that
 * keeps child coordinates, never copied text. Repeated composition forms a shared DAG.
 */
public final class M3RopeTuple extends M3RopeOwner {
    final M3RopeString left;
    final M3RopeString right;
    final int height;

    M3RopeTuple(M3RopeString left, M3RopeString right) {
        super(Math.addExact(left.length(), right.length()), left.presence() | right.presence());
        this.left = left;
        this.right = right;
        this.height = 1 + Math.max(left.height(), right.height());
    }
}
