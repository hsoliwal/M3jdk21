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

import java.util.function.DoubleBinaryOperator;

/** Immutable primitive double binary operator plan. */
public final class MIndexDoubleBinaryOperator implements DoubleBinaryOperator {
    private static final byte ADD = 1;
    private static final byte SUBTRACT = 2;
    private static final byte MULTIPLY = 3;
    private static final byte DIVIDE = 4;
    private static final byte REMAINDER = 5;
    private static final byte MIN = 6;
    private static final byte MAX = 7;
    private final byte opcode;

    private MIndexDoubleBinaryOperator(byte opcode) {
        this.opcode = opcode;
    }

    public static MIndexDoubleBinaryOperator add() { return new MIndexDoubleBinaryOperator(ADD); }
    public static MIndexDoubleBinaryOperator subtract() { return new MIndexDoubleBinaryOperator(SUBTRACT); }
    public static MIndexDoubleBinaryOperator multiply() { return new MIndexDoubleBinaryOperator(MULTIPLY); }
    public static MIndexDoubleBinaryOperator divide() { return new MIndexDoubleBinaryOperator(DIVIDE); }
    public static MIndexDoubleBinaryOperator remainder() { return new MIndexDoubleBinaryOperator(REMAINDER); }
    public static MIndexDoubleBinaryOperator min() { return new MIndexDoubleBinaryOperator(MIN); }
    public static MIndexDoubleBinaryOperator max() { return new MIndexDoubleBinaryOperator(MAX); }

    @Override
    public double applyAsDouble(double left, double right) {
        return switch (opcode) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case REMAINDER -> left % right;
            case MIN -> Math.min(left, right);
            case MAX -> Math.max(left, right);
            default -> throw new AssertionError(opcode);
        };
    }
}
