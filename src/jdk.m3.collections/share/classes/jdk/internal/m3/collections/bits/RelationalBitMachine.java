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
package jdk.internal.m3.collections.bits;

/**
 * Interface-first contract for a positional relational-bit state machine.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/relationbits/RelationalBitMachine.java}
 * blob {@code ba5dc6f8181cd2683ad391868a2e4f5b90e7e300} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface RelationalBitMachine {
    int featureCount();

    int wordCount();

    long primitivePayloadBytes();

    boolean test(int plane, int feature);

    void set(int plane, int feature);

    void clear(int plane, int feature);

    void set(int plane, int feature, boolean value);

    void setRange(int plane, int firstFeature, int count, boolean value);

    int count(int plane);

    void clearState();

    void clearScalars();

    void clearRelation(int relation);

    void clearRelations();

    void clearAll();

    void relate(int relation, int fromFeature, int toFeature);

    void unrelate(int relation, int fromFeature, int toFeature);

    void relateSymmetric(int relation, int leftFeature, int rightFeature);

    void relateRange(int relation, int fromFeature, int firstTarget, int count);

    boolean relationContains(int relation, int fromFeature, int toFeature);

    int targetCount(int relation, int fromFeature);

    void compileTransitiveClosure(int relation);

    void applyChange(int changedFeature);

    void applyChanges(long[] changedFeatures);

    boolean hasActiveConflicts();

    boolean transitionAllowed(int fromFeature, int toFeature);

    boolean transitionActive(int fromFeature, int toFeature);

    int scalar(int scalarPlane, int feature);

    void scalar(int scalarPlane, int feature, int unsignedValue);

    int effectiveStrength(int feature);

    int decay(int scalarPlane, int feature);
}
