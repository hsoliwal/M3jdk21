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
package jdk.m3.collections;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Semantic metadata for a compact M3 signal lane. This is evidence, not a claim that arbitrary
 * domain objects have an exact order, a metric, or a proven pruning bound.
 *
 * <p>Rows and numeric values remain in existing MAT primitive owners. A descriptor is shared
 * schema-level metadata, never a retained structural object per element.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSignalSemantics.java}
 * blob {@code 9450ae632b891f200668b227e4425731163d9257} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSignalSemantics {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*");

    private M3MatSignalSemantics() {}

    public enum Scope {
        PACKAGE, TYPE, FIELD, METHOD, EDGE, GRAPH, OBJECT, COLLECTION, QUERY
    }

    public enum Stage {
        SOURCE_PARSE, COMPILER_ATTRIBUTED, BYTECODE, RUNTIME_OBSERVED,
        QUERY_RELATIVE, DOMAIN_DECLARED
    }

    public enum Role {
        COUNT, ORDINAL, WEIGHT, DISTANCE, LOWER_BOUND, FLAG, CONFIDENCE, COST, SKETCH
    }

    public enum Evidence {
        EXACT_DECLARATION, STATIC_POSSIBLE, HEURISTIC, OBSERVED_SAMPLE, DOMAIN_ASSERTED
    }

    public enum Invalidation {
        SOURCE_ROOT, CLASSPATH_AND_SOURCE, BINARY_ROOT, OBJECT_VERSION,
        RUNTIME_EPOCH, QUERY_AND_SOURCE
    }

    /**
     * Explicit provenance and admissibility declarations. Exact order/pruning are opt-in claims
     * requiring separate semantic oracle tests; this metadata alone cannot prove them.
     */
    public record Descriptor(
            String id, Scope scope, Stage stage, Role role, Evidence evidence,
            Invalidation invalidation, String unit, boolean exactOrderClaim,
            boolean admissibleBoundClaim) {

        public Descriptor {
            id = Objects.requireNonNull(id, "id");
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException("invalid signal id: " + id);
            }
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(stage, "stage");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(evidence, "evidence");
            Objects.requireNonNull(invalidation, "invalidation");
            unit = Objects.requireNonNull(unit, "unit");
            if (unit.isBlank() || unit.length() > 64) {
                throw new IllegalArgumentException("invalid unit");
            }
            if (exactOrderClaim && role != Role.ORDINAL) {
                throw new IllegalArgumentException("only ordinals may claim exact ordering");
            }
            if (admissibleBoundClaim && role != Role.LOWER_BOUND) {
                throw new IllegalArgumentException("only lower bounds may claim admissible pruning");
            }
            if ((exactOrderClaim || admissibleBoundClaim)
                    && evidence != Evidence.EXACT_DECLARATION
                    && evidence != Evidence.DOMAIN_ASSERTED) {
                throw new IllegalArgumentException("heuristic/observed facts cannot certify order");
            }
            if (stage == Stage.QUERY_RELATIVE
                    && invalidation != Invalidation.QUERY_AND_SOURCE) {
                throw new IllegalArgumentException("query-relative facts must expire with query");
            }
            if (stage == Stage.RUNTIME_OBSERVED
                    && invalidation != Invalidation.RUNTIME_EPOCH) {
                throw new IllegalArgumentException("runtime samples require epoch invalidation");
            }
            if (stage == Stage.COMPILER_ATTRIBUTED
                    && invalidation != Invalidation.CLASSPATH_AND_SOURCE) {
                throw new IllegalArgumentException("attributed facts require classpath invalidation");
            }
            if (stage == Stage.SOURCE_PARSE
                    && invalidation != Invalidation.SOURCE_ROOT) {
                throw new IllegalArgumentException("parsed facts require source invalidation");
            }
        }

        /** A metadata declaration alone never authorizes eliminating an exact verifier. */
        public boolean permitsSkippingExactVerifier() {
            return false;
        }

        /** Scalar comparison is not automatically a domain distance, even for ordinal signals. */
        public boolean distanceMeaningDeclared() {
            return role == Role.DISTANCE || role == Role.LOWER_BOUND;
        }
    }
}
