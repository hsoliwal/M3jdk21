/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.eclipse.impl.list.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.m3.collections.eclipse.api.list.MutableList;

/**
 * The patched CompositeFastList and its legacy reference driven in lockstep: each operation runs
 * on both with identically seeded callbacks, then outcomes, traces and state must agree.
 */
final class OffsetsTwin {

    /** One operation on one twin; side 0 is the patched list, side 1 the legacy reference. */
    @FunctionalInterface
    interface TwinOp {
        Object run(OffsetsTwin twin, int side);
    }

    /** An operation with its index (or NONE) and flags. */
    record NamedOp(String name, TwinOp op, int index, int flags) {
    }

    /** A caller-held lane added to both twins; the caller may still resize it. */
    record LanePair(FastList<Object> patched, FastList<Object> legacy) {
        FastList<Object> lane(int side) {
            return side == 0 ? this.patched : this.legacy;
        }
    }

    static final int NONE = Integer.MIN_VALUE;
    static final int QUIET = 1;
    static final int INDEXED = 2;
    static final int INSERT = 4;
    static final int REBIND = 8;
    private static final int BUILD_STEPS = 60;

    final int seed;
    final Random master;
    final boolean foreign;
    final OffsetsState.Coverage cov;
    final List<LanePair> handles = new ArrayList<>();
    MutableList<Object> patched = new CompositeFastList<>();
    MutableList<Object> legacy = new LegacyCompositeFastList<>();
    int step;
    int probes;

    OffsetsTwin(int seed, OffsetsState.Coverage cov) {
        this.seed = seed;
        this.master = new Random(seed * 0x9E3779B97F4A7C15L + 17);
        this.foreign = seed % 3 == 0;
        this.cov = cov;
    }

    MutableList<Object> list(int side) {
        return side == 0 ? this.patched : this.legacy;
    }

    void replace(int side, MutableList<Object> list) {
        if (side == 0) {
            this.patched = list;
        } else {
            this.legacy = list;
        }
    }

    /** Index of the caller-held pair whose lane on this side is that very lane, else -1. */
    int handleOf(FastList<Object> lane, int side) {
        for (int h = 0; h < this.handles.size(); h++) {
            if (this.handles.get(h).lane(side) == lane) {
                return h;
            }
        }
        return -1;
    }

    /** Builds 9..40 lanes, then runs the random operation mix. */
    void run(int steps) {
        int lanes = 9 + this.master.nextInt(32);
        for (int k = 0; k < BUILD_STEPS && OffsetsState.laneCount(this.patched) < lanes; k++) {
            this.apply(OffsetsOps.lane(this));
        }
        for (this.step = 0; this.step < steps; this.step++) {
            this.apply(OffsetsOps.pick(this));
        }
    }

    void apply(NamedOp op) {
        int throwAt = this.master.nextInt(4) == 0 ? 1 + this.master.nextInt(40) : -1;
        int mutations = (op.flags() & QUIET) == 0 && this.master.nextInt(3) == 0 ? 1 : 0;
        this.apply(op, throwAt, mutations);
    }

    void apply(NamedOp op, int throwAt, int mutations) {
        long callbackSeed = this.master.nextLong();
        int sizeBefore = this.patched.size();
        String mine = this.side(op, 0, callbackSeed, throwAt, mutations);
        this.countIndexed(op, sizeBefore);
        String theirs = this.side(op, 1, callbackSeed, throwAt, mutations);
        String where = "seed " + this.seed + " step " + this.step + " " + op.name();
        assertEquals(theirs, mine, where);
        if ((op.flags() & REBIND) != 0) {
            this.handles.clear();
        }
        OffsetsState.compare(this, where);
    }

    private String side(NamedOp op, int side, long callbackSeed, int throwAt, int mutations) {
        OffsetsCallbacks.CTX.side = side;
        OffsetsCallbacks.CTX.op = op.name();
        OffsetsCallbacks.CTX.cov = this.cov;
        OffsetsCallbacks.begin(this.list(side), callbackSeed, throwAt, mutations);
        try {
            String result = OffsetsCallbacks.outcome(() -> op.op().run(this, side));
            return result + " | " + OffsetsCallbacks.end();
        } finally {
            OffsetsCallbacks.CTX.active = false;
        }
    }

    private void countIndexed(NamedOp op, int sizeBefore) {
        int i = op.index();
        boolean insert = (op.flags() & INSERT) != 0;
        if ((op.flags() & (INDEXED | INSERT)) == 0 || i < 0 || i > sizeBefore
                || i == sizeBefore && !insert) {
            return;
        }
        OffsetsState.countServed(this.cov, OffsetsState.tree(this.patched).isPresent());
    }
}
