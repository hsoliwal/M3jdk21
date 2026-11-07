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

import java.util.Arrays;
import java.util.Objects;

/**
 * Dense O(1) pair dispatch: {@code operator[leftClass * classCount + rightClass]}.
 *
 * <p>Useful when a semantic/type pair can select a precompiled integer operator without a map,
 * virtual dispatch or repeated branching.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatOperatorTable.java}
 * blob {@code 1a25ef96db13865844c6f7290ec93f17d07b7e11} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatOperatorTable {
    private final int classCount;
    private final int[] operators;

    private M3MatOperatorTable(int classCount, int[] operators) {
        this.classCount = classCount;
        this.operators = operators;
    }

    public static Builder builder(int classCount, int defaultOperator) {
        return new Builder(classCount, defaultOperator);
    }

    public int classCount() { return classCount; }
    public long payloadBytes() { return 4L * operators.length; }

    public int operator(int leftClass, int rightClass) {
        Objects.checkIndex(leftClass, classCount);
        Objects.checkIndex(rightClass, classCount);
        return operators[leftClass * classCount + rightClass];
    }

    public int flatIndex(int leftClass, int rightClass) {
        Objects.checkIndex(leftClass, classCount);
        Objects.checkIndex(rightClass, classCount);
        return leftClass * classCount + rightClass;
    }

    public int operatorAtFlatIndex(int index) {
        return operators[Objects.checkIndex(index, operators.length)];
    }

    public int[] toArray() { return operators.clone(); }

    public static final class Builder {
        private final int classCount;
        private final int[] operators;

        private Builder(int classCount, int defaultOperator) {
            if (classCount <= 0) throw new IllegalArgumentException("classCount must be positive");
            long cells = Math.multiplyExact((long) classCount, (long) classCount);
            if (cells > Integer.MAX_VALUE) throw new IllegalArgumentException("operator table too large");
            this.classCount = classCount;
            this.operators = new int[(int) cells];
            Arrays.fill(operators, defaultOperator);
        }

        public Builder set(int leftClass, int rightClass, int operator) {
            Objects.checkIndex(leftClass, classCount);
            Objects.checkIndex(rightClass, classCount);
            operators[leftClass * classCount + rightClass] = operator;
            return this;
        }

        public Builder setSymmetric(int firstClass, int secondClass, int operator) {
            set(firstClass, secondClass, operator);
            set(secondClass, firstClass, operator);
            return this;
        }

        public M3MatOperatorTable build() {
            return new M3MatOperatorTable(classCount, operators.clone());
        }
    }
}
