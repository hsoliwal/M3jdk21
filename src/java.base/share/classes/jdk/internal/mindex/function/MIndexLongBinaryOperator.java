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

import java.util.function.LongBinaryOperator;

/** Immutable primitive long binary operator plan. */
public final class MIndexLongBinaryOperator implements LongBinaryOperator {
    private static final byte ADD = 1;
    private static final byte SUBTRACT = 2;
    private static final byte MULTIPLY = 3;
    private static final byte DIVIDE = 4;
    private static final byte REMAINDER = 5;
    private static final byte MIN = 6;
    private static final byte MAX = 7;
    private static final byte BIT_AND = 8;
    private static final byte BIT_OR = 9;
    private static final byte BIT_XOR = 10;
    private final byte opcode;

    private MIndexLongBinaryOperator(byte opcode) {
        this.opcode = opcode;
    }

    public static MIndexLongBinaryOperator add() { return new MIndexLongBinaryOperator(ADD); }
    public static MIndexLongBinaryOperator subtract() { return new MIndexLongBinaryOperator(SUBTRACT); }
    public static MIndexLongBinaryOperator multiply() { return new MIndexLongBinaryOperator(MULTIPLY); }
    public static MIndexLongBinaryOperator divide() { return new MIndexLongBinaryOperator(DIVIDE); }
    public static MIndexLongBinaryOperator remainder() { return new MIndexLongBinaryOperator(REMAINDER); }
    public static MIndexLongBinaryOperator min() { return new MIndexLongBinaryOperator(MIN); }
    public static MIndexLongBinaryOperator max() { return new MIndexLongBinaryOperator(MAX); }
    public static MIndexLongBinaryOperator bitAnd() { return new MIndexLongBinaryOperator(BIT_AND); }
    public static MIndexLongBinaryOperator bitOr() { return new MIndexLongBinaryOperator(BIT_OR); }
    public static MIndexLongBinaryOperator bitXor() { return new MIndexLongBinaryOperator(BIT_XOR); }

    @Override
    public long applyAsLong(long left, long right) {
        return switch (opcode) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case REMAINDER -> left % right;
            case MIN -> Math.min(left, right);
            case MAX -> Math.max(left, right);
            case BIT_AND -> left & right;
            case BIT_OR -> left | right;
            case BIT_XOR -> left ^ right;
            default -> throw new AssertionError(opcode);
        };
    }
}
