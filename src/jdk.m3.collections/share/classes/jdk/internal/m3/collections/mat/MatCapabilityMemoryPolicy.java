/*******************************************************************************
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
package jdk.internal.m3.collections.mat;

import java.util.Objects;

/**
 * Runtime-Xmx-driven eligibility for optional capability state.
 * Never destroys canonical values, assumes reference counts, or silently spills objects.
 * The container/owner performs any independently proven action.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCapabilityMemoryPolicy.java}
 * blob {@code 458a0c55c50599766608016ac738de3e1cd72666} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatCapabilityMemoryPolicy {
    public enum Action {
        KEEP_OPTIONAL,
        RECLAIM_REBUILDABLE,
        AVOID_NEW_OPTIONAL_ALLOCATION
    }

    /** Immutable observation; used = committed - free, max = Runtime.maxMemory(). */
    public record Heap(long used, long committed, long max) {
        public Heap {
            if (used < 0 || committed < 0 || max <= 0 || used > committed
                    || committed > max) {
                throw new IllegalArgumentException("invalid heap sample");
            }
        }
    }

    /** Candidate-only policy: never authorizes changing canonical payload or identity. */
    public record Decision(Action action, long capabilities, long freeHeadroom,
            boolean optionalCacheAdmission, boolean canonicalPayloadPreserved) {
        public Decision {
            Objects.requireNonNull(action, "action");
            MatCapabilityFlags.checked(capabilities);
            if (freeHeadroom < 0 || !canonicalPayloadPreserved) {
                throw new IllegalArgumentException("canonical state must survive pressure");
            }
        }
    }

    private MatCapabilityMemoryPolicy() {}

    public static Heap currentHeap() {
        Runtime runtime = Runtime.getRuntime();
        long maximum = runtime.maxMemory();
        long committed = runtime.totalMemory();
        long used = Math.max(0L, committed - runtime.freeMemory());
        return new Heap(Math.min(used, maximum), Math.min(committed, maximum), maximum);
    }

    public static Decision currentDecision(long capabilities) {
        return decide(capabilities, currentHeap());
    }

    /** Testable deterministic part. Residency transitions remain owned by existing MAT/MIndex. */
    static Decision decide(long capabilities, Heap heap) {
        long mask = MatCapabilityFlags.checked(capabilities);
        Heap sample = Objects.requireNonNull(heap, "heap");
        long headroom = sample.max() - sample.used();
        boolean relevant = MatCapabilityFlags.has(mask, MatCapabilityFlags.CACHEABLE)
                || MatCapabilityFlags.has(mask, MatCapabilityFlags.PREFETCHABLE);
        boolean high = sample.used() > sample.max() - sample.max() / 5;
        boolean critical = sample.used() > sample.max() - sample.max() / 10;
        Action action = critical ? Action.AVOID_NEW_OPTIONAL_ALLOCATION
                : high ? Action.RECLAIM_REBUILDABLE : Action.KEEP_OPTIONAL;
        return new Decision(action, mask, headroom, relevant && !high, true);
    }
}
