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

import java.util.Arrays;

final class MIndexPrimitiveProgram {
    static final byte INT = 1;
    static final byte LONG = 2;
    static final byte DOUBLE = 3;

    static final byte ADD = 1;
    static final byte SUBTRACT = 2;
    static final byte MULTIPLY = 3;
    static final byte DIVIDE = 4;
    static final byte REMAINDER = 5;
    static final byte NEGATE = 6;
    static final byte ABS = 7;
    static final byte BIT_NOT = 8;
    static final byte SHIFT_LEFT = 9;
    static final byte SHIFT_RIGHT = 10;
    static final byte UNSIGNED_SHIFT_RIGHT = 11;
    static final byte MIN = 12;
    static final byte MAX = 13;

    private final byte kind;
    private final byte[] code;
    private final long[] operand;

    private MIndexPrimitiveProgram(byte kind, byte[] code, long[] operand) {
        this.kind = kind;
        this.code = code;
        this.operand = operand;
    }

    static MIndexPrimitiveProgram identity(byte kind) {
        return new MIndexPrimitiveProgram(kind, new byte[0], new long[0]);
    }

    static MIndexPrimitiveProgram of(byte kind, byte opcode, long operand) {
        validate(kind, opcode);
        return new MIndexPrimitiveProgram(kind, new byte[] {opcode}, new long[] {operand});
    }

    byte kind() {
        return kind;
    }

    int operationCount() {
        return code.length;
    }

    long retainedBytes() {
        return code.length + (long) Long.BYTES * operand.length;
    }

    MIndexPrimitiveProgram then(MIndexPrimitiveProgram after) {
        if (kind != after.kind) {
            throw new IllegalArgumentException("primitive program kind mismatch");
        }
        if (code.length == 0) {
            return after;
        }
        if (after.code.length == 0) {
            return this;
        }
        int length = Math.addExact(code.length, after.code.length);
        byte[] joinedCode = Arrays.copyOf(code, length);
        long[] joinedOperand = Arrays.copyOf(operand, length);
        System.arraycopy(after.code, 0, joinedCode, code.length, after.code.length);
        System.arraycopy(after.operand, 0, joinedOperand, operand.length, after.operand.length);
        return new MIndexPrimitiveProgram(kind, joinedCode, joinedOperand);
    }

    long applyRaw(long input) {
        long value = input;
        for (int index = 0; index < code.length; index++) {
            value = apply(kind, code[index], value, operand[index]);
        }
        return value;
    }

    private static long apply(byte kind, byte opcode, long value, long operand) {
        if (kind == INT) {
            int left = (int) value;
            int right = (int) operand;
            return switch (opcode) {
                case ADD -> left + right;
                case SUBTRACT -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE -> left / right;
                case REMAINDER -> left % right;
                case NEGATE -> -left;
                case ABS -> Math.abs(left);
                case BIT_NOT -> ~left;
                case SHIFT_LEFT -> left << right;
                case SHIFT_RIGHT -> left >> right;
                case UNSIGNED_SHIFT_RIGHT -> left >>> right;
                case MIN -> Math.min(left, right);
                case MAX -> Math.max(left, right);
                default -> throw new AssertionError(opcode);
            };
        }
        if (kind == LONG) {
            long right = operand;
            return switch (opcode) {
                case ADD -> value + right;
                case SUBTRACT -> value - right;
                case MULTIPLY -> value * right;
                case DIVIDE -> value / right;
                case REMAINDER -> value % right;
                case NEGATE -> -value;
                case ABS -> Math.abs(value);
                case BIT_NOT -> ~value;
                case SHIFT_LEFT -> value << (int) right;
                case SHIFT_RIGHT -> value >> (int) right;
                case UNSIGNED_SHIFT_RIGHT -> value >>> (int) right;
                case MIN -> Math.min(value, right);
                case MAX -> Math.max(value, right);
                default -> throw new AssertionError(opcode);
            };
        }
        double left = Double.longBitsToDouble(value);
        double right = Double.longBitsToDouble(operand);
        double result = switch (opcode) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case REMAINDER -> left % right;
            case NEGATE -> -left;
            case ABS -> Math.abs(left);
            case MIN -> Math.min(left, right);
            case MAX -> Math.max(left, right);
            default -> throw new AssertionError(opcode);
        };
        return Double.doubleToRawLongBits(result);
    }

    private static void validate(byte kind, byte opcode) {
        if (kind != INT && kind != LONG && kind != DOUBLE) {
            throw new IllegalArgumentException("unknown primitive kind");
        }
        if (kind == DOUBLE
                && (opcode == BIT_NOT
                    || opcode == SHIFT_LEFT
                    || opcode == SHIFT_RIGHT
                    || opcode == UNSIGNED_SHIFT_RIGHT)) {
            throw new IllegalArgumentException("integral operation on double plan");
        }
        if (opcode < ADD || opcode > MAX) {
            throw new IllegalArgumentException("unknown primitive opcode");
        }
    }
}
