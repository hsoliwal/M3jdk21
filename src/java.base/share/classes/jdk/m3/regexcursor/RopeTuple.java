// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

/**
 * Proving-ground mirror of {@code java.lang.M3StringTuple}: a persistent composition node that
 * keeps child coordinates, never copied text. Repeated composition forms a shared DAG.
 */
public final class RopeTuple extends RopeOwner {
    final RopeString left;
    final RopeString right;
    final int height;

    RopeTuple(RopeString left, RopeString right) {
        super(Math.addExact(left.length(), right.length()), left.presence() | right.presence());
        this.left = left;
        this.right = right;
        this.height = 1 + Math.max(left.height(), right.height());
    }
}
