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

import java.util.function.IntBinaryOperator;

/** Immutable primitive int binary operator plan. */
public final class MIndexIntBinaryOperator implements IntBinaryOperator {
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

    private MIndexIntBinaryOperator(byte opcode) {
        this.opcode = opcode;
    }

    public static MIndexIntBinaryOperator add() { return new MIndexIntBinaryOperator(ADD); }
    public static MIndexIntBinaryOperator subtract() { return new MIndexIntBinaryOperator(SUBTRACT); }
    public static MIndexIntBinaryOperator multiply() { return new MIndexIntBinaryOperator(MULTIPLY); }
    public static MIndexIntBinaryOperator divide() { return new MIndexIntBinaryOperator(DIVIDE); }
    public static MIndexIntBinaryOperator remainder() { return new MIndexIntBinaryOperator(REMAINDER); }
    public static MIndexIntBinaryOperator min() { return new MIndexIntBinaryOperator(MIN); }
    public static MIndexIntBinaryOperator max() { return new MIndexIntBinaryOperator(MAX); }
    public static MIndexIntBinaryOperator bitAnd() { return new MIndexIntBinaryOperator(BIT_AND); }
    public static MIndexIntBinaryOperator bitOr() { return new MIndexIntBinaryOperator(BIT_OR); }
    public static MIndexIntBinaryOperator bitXor() { return new MIndexIntBinaryOperator(BIT_XOR); }

    @Override
    public int applyAsInt(int left, int right) {
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
