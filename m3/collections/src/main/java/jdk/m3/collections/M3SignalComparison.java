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

/**
 * An immutable observation of one domain-signal comparison, not an identity or exact-match proof.
 * Order always follows Double.compare; a metric exists only when both coordinates and their
 * subtraction are finite. Preserving the source coordinates permits later policy-specific
 * refinement without pretending that scalar separation is a universal domain distance.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/SignalComparison.java}
 * blob {@code 31d467098889375d202acab93b9306fad31d5ebb} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3SignalComparison(
        double left,
        double right,
        int order,
        double absoluteSeparation,
        boolean metricAvailable) {

    public M3SignalComparison {
        int actual = Double.compare(left, right);
        if (order != Integer.signum(actual)) {
            throw new IllegalArgumentException("order disagrees with Double.compare");
        }
        boolean available = Double.isFinite(left) && Double.isFinite(right)
                && Double.isFinite(left - right);
        if (available != metricAvailable) {
            throw new IllegalArgumentException("metric availability mismatch");
        }
        if (available && (Double.compare(absoluteSeparation, Math.abs(left - right)) != 0)) {
            throw new IllegalArgumentException("absolute separation mismatch");
        }
        if (!available && !Double.isNaN(absoluteSeparation)) {
            throw new IllegalArgumentException("unavailable distance must be NaN");
        }
    }

    public static M3SignalComparison of(double left, double right) {
        boolean available = Double.isFinite(left) && Double.isFinite(right)
                && Double.isFinite(left - right);
        return new M3SignalComparison(
                left, right, Integer.signum(Double.compare(left, right)),
                available ? Math.abs(left - right) : Double.NaN, available);
    }

    /** Each operand is evaluated exactly once. */
    public static M3SignalComparison of(M3ISignal left, M3ISignal right) {
        return of(Objects.requireNonNull(left, "left").signal(),
                Objects.requireNonNull(right, "right").signal());
    }

    /** Symmetric signal distance, not a guarantee of semantic/domain distance. */
    public double distanceOr(double fallback) {
        return metricAvailable ? absoluteSeparation : fallback;
    }
}
