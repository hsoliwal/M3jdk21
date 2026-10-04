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
import java.util.function.IntUnaryOperator;

/** Immutable primitive int operation plan implementing the standard JDK functional contract. */
public final class MIndexIntUnaryOperator implements IntUnaryOperator {
    private final MIndexPrimitiveProgram program;

    private MIndexIntUnaryOperator(MIndexPrimitiveProgram program) {
        this.program = program;
    }

    public static MIndexIntUnaryOperator identity() { return new MIndexIntUnaryOperator(MIndexPrimitiveProgram.identity(MIndexPrimitiveProgram.INT)); }
    public static MIndexIntUnaryOperator add(int value) { return op(MIndexPrimitiveProgram.ADD, value); }
    public static MIndexIntUnaryOperator subtract(int value) { return op(MIndexPrimitiveProgram.SUBTRACT, value); }
    public static MIndexIntUnaryOperator multiply(int value) { return op(MIndexPrimitiveProgram.MULTIPLY, value); }
    public static MIndexIntUnaryOperator divide(int value) { return op(MIndexPrimitiveProgram.DIVIDE, value); }
    public static MIndexIntUnaryOperator remainder(int value) { return op(MIndexPrimitiveProgram.REMAINDER, value); }
    public static MIndexIntUnaryOperator negateValue() { return op(MIndexPrimitiveProgram.NEGATE, 0); }
    public static MIndexIntUnaryOperator abs() { return op(MIndexPrimitiveProgram.ABS, 0); }
    public static MIndexIntUnaryOperator bitwiseNot() { return op(MIndexPrimitiveProgram.BIT_NOT, 0); }
    public static MIndexIntUnaryOperator shiftLeft(int distance) { return op(MIndexPrimitiveProgram.SHIFT_LEFT, distance); }
    public static MIndexIntUnaryOperator shiftRight(int distance) { return op(MIndexPrimitiveProgram.SHIFT_RIGHT, distance); }
    public static MIndexIntUnaryOperator unsignedShiftRight(int distance) { return op(MIndexPrimitiveProgram.UNSIGNED_SHIFT_RIGHT, distance); }
    public static MIndexIntUnaryOperator min(int value) { return op(MIndexPrimitiveProgram.MIN, value); }
    public static MIndexIntUnaryOperator max(int value) { return op(MIndexPrimitiveProgram.MAX, value); }

    private static MIndexIntUnaryOperator op(byte opcode, int operand) {
        return new MIndexIntUnaryOperator(MIndexPrimitiveProgram.of(MIndexPrimitiveProgram.INT, opcode, operand));
    }

    @Override
    public int applyAsInt(int operand) {
        return (int) program.applyRaw(operand);
    }

    @Override
    public IntUnaryOperator compose(IntUnaryOperator before) {
        Objects.requireNonNull(before);
        if (before instanceof MIndexIntUnaryOperator plan) {
            return new MIndexIntUnaryOperator(plan.program.then(program));
        }
        return value -> applyAsInt(before.applyAsInt(value));
    }

    @Override
    public IntUnaryOperator andThen(IntUnaryOperator after) {
        Objects.requireNonNull(after);
        if (after instanceof MIndexIntUnaryOperator plan) {
            return new MIndexIntUnaryOperator(program.then(plan.program));
        }
        return value -> after.applyAsInt(applyAsInt(value));
    }

    public int operationCount() { return program.operationCount(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
