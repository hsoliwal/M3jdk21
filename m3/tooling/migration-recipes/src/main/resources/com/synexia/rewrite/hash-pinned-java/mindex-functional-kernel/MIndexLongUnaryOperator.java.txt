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
import java.util.function.LongUnaryOperator;

/** Immutable primitive long operation plan implementing the standard JDK functional contract. */
public final class MIndexLongUnaryOperator implements LongUnaryOperator {
    private final MIndexPrimitiveProgram program;

    private MIndexLongUnaryOperator(MIndexPrimitiveProgram program) { this.program = program; }

    public static MIndexLongUnaryOperator identity() { return new MIndexLongUnaryOperator(MIndexPrimitiveProgram.identity(MIndexPrimitiveProgram.LONG)); }
    public static MIndexLongUnaryOperator add(long value) { return op(MIndexPrimitiveProgram.ADD, value); }
    public static MIndexLongUnaryOperator subtract(long value) { return op(MIndexPrimitiveProgram.SUBTRACT, value); }
    public static MIndexLongUnaryOperator multiply(long value) { return op(MIndexPrimitiveProgram.MULTIPLY, value); }
    public static MIndexLongUnaryOperator divide(long value) { return op(MIndexPrimitiveProgram.DIVIDE, value); }
    public static MIndexLongUnaryOperator remainder(long value) { return op(MIndexPrimitiveProgram.REMAINDER, value); }
    public static MIndexLongUnaryOperator negateValue() { return op(MIndexPrimitiveProgram.NEGATE, 0L); }
    public static MIndexLongUnaryOperator abs() { return op(MIndexPrimitiveProgram.ABS, 0L); }
    public static MIndexLongUnaryOperator bitwiseNot() { return op(MIndexPrimitiveProgram.BIT_NOT, 0L); }
    public static MIndexLongUnaryOperator shiftLeft(int distance) { return op(MIndexPrimitiveProgram.SHIFT_LEFT, distance); }
    public static MIndexLongUnaryOperator shiftRight(int distance) { return op(MIndexPrimitiveProgram.SHIFT_RIGHT, distance); }
    public static MIndexLongUnaryOperator unsignedShiftRight(int distance) { return op(MIndexPrimitiveProgram.UNSIGNED_SHIFT_RIGHT, distance); }
    public static MIndexLongUnaryOperator min(long value) { return op(MIndexPrimitiveProgram.MIN, value); }
    public static MIndexLongUnaryOperator max(long value) { return op(MIndexPrimitiveProgram.MAX, value); }

    private static MIndexLongUnaryOperator op(byte opcode, long operand) {
        return new MIndexLongUnaryOperator(MIndexPrimitiveProgram.of(MIndexPrimitiveProgram.LONG, opcode, operand));
    }

    @Override
    public long applyAsLong(long operand) { return program.applyRaw(operand); }

    @Override
    public LongUnaryOperator compose(LongUnaryOperator before) {
        Objects.requireNonNull(before);
        if (before instanceof MIndexLongUnaryOperator plan) {
            return new MIndexLongUnaryOperator(plan.program.then(program));
        }
        return value -> applyAsLong(before.applyAsLong(value));
    }

    @Override
    public LongUnaryOperator andThen(LongUnaryOperator after) {
        Objects.requireNonNull(after);
        if (after instanceof MIndexLongUnaryOperator plan) {
            return new MIndexLongUnaryOperator(program.then(plan.program));
        }
        return value -> after.applyAsLong(applyAsLong(value));
    }

    public int operationCount() { return program.operationCount(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
