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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * One immutable algorithm-kernel candidate.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/AlgorithmCandidate.java}
 * blob {@code 04b5655d4a99489ebd5c984338eeaa0e4099cadc} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class AlgorithmCandidate {

    @FunctionalInterface
    public interface Matcher {
        /** Null means supported; non-null is a hard rejection reason. */
        String rejection(AlgorithmRequirements requirements);
    }

    @FunctionalInterface
    public interface Estimator {
        /** Analytical peak/retained working bytes; not a JVM heap measurement. */
        long estimatedBytes(AlgorithmRequirements requirements);
    }

    /** Higher is better; values are in [0,100]. */
    public record Traits(
            int preprocessing,
            int query,
            int update,
            int throughput,
            int allocation,
            int locality) {

        public Traits {
            check(preprocessing);
            check(query);
            check(update);
            check(throughput);
            check(allocation);
            check(locality);
        }

        private static void check(int value) {
            if (value < 0 || value > 100) throw new IllegalArgumentException("trait out of range");
        }
    }

    private final String id;
    private final AlgorithmRequirements.Purpose purpose;
    private final Class<?> implementationType;
    private final String description;
    private final Matcher matcher;
    private final Estimator estimator;
    private final Traits traits;

    public AlgorithmCandidate(
            String id,
            AlgorithmRequirements.Purpose purpose,
            Class<?> implementationType,
            String description,
            Matcher matcher,
            Estimator estimator,
            Traits traits) {
        this.id = requireId(id);
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.implementationType = Objects.requireNonNull(implementationType, "implementationType");
        this.description = Objects.requireNonNull(description, "description");
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.traits = Objects.requireNonNull(traits, "traits");
    }

    public String id() { return id; }
    public AlgorithmRequirements.Purpose purpose() { return purpose; }
    public Class<?> implementationType() { return implementationType; }
    public String description() { return description; }
    public Traits traits() { return traits; }

    String rejection(AlgorithmRequirements requirements) {
        if (requirements.purpose() != purpose) return "purpose mismatch";
        return matcher.rejection(requirements);
    }

    long estimatedBytes(AlgorithmRequirements requirements) {
        return Math.max(0L, estimator.estimatedBytes(requirements));
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("blank candidate id");
        return id;
    }
}
