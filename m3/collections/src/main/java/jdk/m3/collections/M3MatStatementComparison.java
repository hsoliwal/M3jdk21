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
 * Deterministic structural comparison of two primitive-coordinate statements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatStatementComparison.java}
 * blob {@code 2189bc25aeb723422bbbc94dbf4f67aec14e00e9} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatStatementComparison {
    public static final int EXACT = 1 << 0;
    public static final int SAME_SUBJECT = 1 << 1;
    public static final int SAME_PREDICATE = 1 << 2;
    public static final int SAME_OBJECT = 1 << 3;
    public static final int SAME_CONTEXT = 1 << 4;
    public static final int INVERSE = 1 << 5;
    public static final int OPPOSITE = 1 << 6;

    private M3MatStatementComparison() { }

    public static Comparison compare(
            M3MatStatement left,
            M3MatStatement right,
            M3MatPredicateLexicon predicates) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(predicates, "predicates");
        predicates.flags(left.predicate());
        predicates.flags(right.predicate());

        int features = 0;
        int shared = 0;
        if (left.subject() == right.subject()) {
            features |= SAME_SUBJECT;
            shared++;
        }
        if (left.predicate() == right.predicate()) {
            features |= SAME_PREDICATE;
            shared++;
        }
        if (left.object() == right.object()) {
            features |= SAME_OBJECT;
            shared++;
        }
        if (left.context() == right.context()) {
            features |= SAME_CONTEXT;
            shared++;
        }
        if (left.equals(right)) {
            features |= EXACT;
        }
        if (isInverse(left, right, predicates)) {
            features |= INVERSE;
        }
        if (isOpposite(left, right, predicates)) {
            features |= OPPOSITE;
        }
        int scoreQ16 = (shared * 0xffff + 2) / 4;
        return new Comparison(features, scoreQ16);
    }

    public static boolean isInverse(
            M3MatStatement left,
            M3MatStatement right,
            M3MatPredicateLexicon predicates) {
        if (left.context() != right.context()) {
            return false;
        }
        int inverse = predicates.inverseId(left.predicate());
        return inverse >= 0
                && inverse == right.predicate()
                && left.subject() == right.object()
                && left.object() == right.subject();
    }

    public static boolean isOpposite(
            M3MatStatement left,
            M3MatStatement right,
            M3MatPredicateLexicon predicates) {
        if (left.context() != right.context()) {
            return false;
        }
        int opposite = predicates.oppositeId(left.predicate());
        if (opposite < 0) {
            return false;
        }
        if (opposite == right.predicate()
                && left.subject() == right.subject()
                && left.object() == right.object()) {
            return true;
        }
        int inverseOpposite = predicates.inverseId(opposite);
        return inverseOpposite >= 0
                && inverseOpposite == right.predicate()
                && left.subject() == right.object()
                && left.object() == right.subject();
    }

    public record Comparison(int features, int scoreQ16) {
        public Comparison {
            if (scoreQ16 < 0 || scoreQ16 > 0xffff) {
                throw new IllegalArgumentException("scoreQ16 outside unsigned-16 range");
            }
        }

        public boolean has(int feature) {
            return (features & feature) != 0;
        }
    }
}
