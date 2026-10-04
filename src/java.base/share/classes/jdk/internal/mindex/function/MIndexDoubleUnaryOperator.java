/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/** Immutable primitive double operation plan implementing the standard JDK functional contract. */
public final class MIndexDoubleUnaryOperator implements DoubleUnaryOperator {
    private final MIndexPrimitiveProgram program;

    private MIndexDoubleUnaryOperator(MIndexPrimitiveProgram program) { this.program = program; }

    public static MIndexDoubleUnaryOperator identity() { return new MIndexDoubleUnaryOperator(MIndexPrimitiveProgram.identity(MIndexPrimitiveProgram.DOUBLE)); }
    public static MIndexDoubleUnaryOperator add(double value) { return op(MIndexPrimitiveProgram.ADD, value); }
    public static MIndexDoubleUnaryOperator subtract(double value) { return op(MIndexPrimitiveProgram.SUBTRACT, value); }
    public static MIndexDoubleUnaryOperator multiply(double value) { return op(MIndexPrimitiveProgram.MULTIPLY, value); }
    public static MIndexDoubleUnaryOperator divide(double value) { return op(MIndexPrimitiveProgram.DIVIDE, value); }
    public static MIndexDoubleUnaryOperator remainder(double value) { return op(MIndexPrimitiveProgram.REMAINDER, value); }
    public static MIndexDoubleUnaryOperator negateValue() { return op(MIndexPrimitiveProgram.NEGATE, 0.0); }
    public static MIndexDoubleUnaryOperator abs() { return op(MIndexPrimitiveProgram.ABS, 0.0); }
    public static MIndexDoubleUnaryOperator min(double value) { return op(MIndexPrimitiveProgram.MIN, value); }
    public static MIndexDoubleUnaryOperator max(double value) { return op(MIndexPrimitiveProgram.MAX, value); }

    private static MIndexDoubleUnaryOperator op(byte opcode, double operand) {
        return new MIndexDoubleUnaryOperator(MIndexPrimitiveProgram.of(
                MIndexPrimitiveProgram.DOUBLE, opcode, Double.doubleToRawLongBits(operand)));
    }

    @Override
    public double applyAsDouble(double operand) {
        return Double.longBitsToDouble(program.applyRaw(Double.doubleToRawLongBits(operand)));
    }

    @Override
    public DoubleUnaryOperator compose(DoubleUnaryOperator before) {
        Objects.requireNonNull(before);
        if (before instanceof MIndexDoubleUnaryOperator plan) {
            return new MIndexDoubleUnaryOperator(plan.program.then(program));
        }
        return value -> applyAsDouble(before.applyAsDouble(value));
    }

    @Override
    public DoubleUnaryOperator andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        if (after instanceof MIndexDoubleUnaryOperator plan) {
            return new MIndexDoubleUnaryOperator(program.then(plan.program));
        }
        return value -> after.applyAsDouble(applyAsDouble(value));
    }

    public int operationCount() { return program.operationCount(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
