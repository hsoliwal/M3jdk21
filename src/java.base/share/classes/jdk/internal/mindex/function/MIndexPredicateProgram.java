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

final class MIndexPredicateProgram {
    static final byte EQ = 1;
    static final byte NOT_EQ = 2;
    static final byte LESS = 3;
    static final byte LESS_OR_EQUAL = 4;
    static final byte GREATER = 5;
    static final byte GREATER_OR_EQUAL = 6;
    static final byte IS_NAN = 7;
    static final byte IS_FINITE = 8;
    static final byte IS_INFINITE = 9;
    static final byte AND = 20;
    static final byte OR = 21;
    static final byte NOT = 22;

    private final byte kind;
    private final byte[] code;
    private final long[] operand;
    private final int maxStack;

    private MIndexPredicateProgram(byte kind, byte[] code, long[] operand) {
        this.kind = kind;
        this.code = code;
        this.operand = operand;
        this.maxStack = validate(code);
    }

    static MIndexPredicateProgram compare(byte kind, byte opcode, long operand) {
        if (opcode < EQ || opcode > GREATER_OR_EQUAL) {
            throw new IllegalArgumentException("not a comparison opcode");
        }
        return new MIndexPredicateProgram(kind, new byte[] {opcode}, new long[] {operand});
    }

    static MIndexPredicateProgram doubleTest(byte opcode) {
        if (opcode != IS_NAN && opcode != IS_FINITE && opcode != IS_INFINITE) {
            throw new IllegalArgumentException("not a double predicate opcode");
        }
        return new MIndexPredicateProgram(
                MIndexPrimitiveProgram.DOUBLE, new byte[] {opcode}, new long[] {0L});
    }

    MIndexPredicateProgram and(MIndexPredicateProgram other) {
        return combine(other, AND);
    }

    MIndexPredicateProgram or(MIndexPredicateProgram other) {
        return combine(other, OR);
    }

    MIndexPredicateProgram negate() {
        int length = Math.addExact(code.length, 1);
        byte[] resultCode = Arrays.copyOf(code, length);
        long[] resultOperand = Arrays.copyOf(operand, length);
        resultCode[length - 1] = NOT;
        return new MIndexPredicateProgram(kind, resultCode, resultOperand);
    }

    boolean testRaw(long value) {
        long stack = 0L;
        int depth = 0;
        for (int index = 0; index < code.length; index++) {
            byte opcode = code[index];
            if (opcode == AND || opcode == OR) {
                boolean right = ((stack >>> (depth - 1)) & 1L) != 0L;
                boolean left = ((stack >>> (depth - 2)) & 1L) != 0L;
                depth -= 2;
                stack = lowBits(stack, depth);
                boolean result = opcode == AND ? left && right : left || right;
                if (result) {
                    stack |= 1L << depth;
                }
                depth++;
            } else if (opcode == NOT) {
                stack ^= 1L << (depth - 1);
            } else {
                boolean result = comparison(kind, opcode, value, operand[index]);
                if (result) {
                    stack |= 1L << depth;
                } else {
                    stack &= ~(1L << depth);
                }
                depth++;
            }
        }
        return (stack & 1L) != 0L;
    }

    int instructionCount() {
        return code.length;
    }

    int maxStack() {
        return maxStack;
    }

    long retainedBytes() {
        return code.length + (long) Long.BYTES * operand.length;
    }

    private MIndexPredicateProgram combine(MIndexPredicateProgram other, byte logical) {
        if (kind != other.kind) {
            throw new IllegalArgumentException("predicate kind mismatch");
        }
        int length = Math.addExact(Math.addExact(code.length, other.code.length), 1);
        byte[] resultCode = Arrays.copyOf(code, length);
        long[] resultOperand = Arrays.copyOf(operand, length);
        System.arraycopy(other.code, 0, resultCode, code.length, other.code.length);
        System.arraycopy(other.operand, 0, resultOperand, operand.length, other.operand.length);
        resultCode[length - 1] = logical;
        return new MIndexPredicateProgram(kind, resultCode, resultOperand);
    }

    private static int validate(byte[] code) {
        int depth = 0;
        int maximum = 0;
        for (byte opcode : code) {
            if (opcode == AND || opcode == OR) {
                if (depth < 2) {
                    throw new IllegalArgumentException("malformed predicate program");
                }
                depth--;
            } else if (opcode == NOT) {
                if (depth < 1) {
                    throw new IllegalArgumentException("malformed predicate program");
                }
            } else {
                depth++;
                maximum = Math.max(maximum, depth);
                if (maximum > 63) {
                    throw new IllegalArgumentException("predicate stack exceeds 63 entries");
                }
            }
        }
        if (depth != 1) {
            throw new IllegalArgumentException("malformed predicate program");
        }
        return maximum;
    }

    private static boolean comparison(byte kind, byte opcode, long leftRaw, long rightRaw) {
        if (kind == MIndexPrimitiveProgram.INT) {
            int left = (int) leftRaw;
            int right = (int) rightRaw;
            return switch (opcode) {
                case EQ -> left == right;
                case NOT_EQ -> left != right;
                case LESS -> left < right;
                case LESS_OR_EQUAL -> left <= right;
                case GREATER -> left > right;
                case GREATER_OR_EQUAL -> left >= right;
                default -> throw new AssertionError(opcode);
            };
        }
        if (kind == MIndexPrimitiveProgram.LONG) {
            return switch (opcode) {
                case EQ -> leftRaw == rightRaw;
                case NOT_EQ -> leftRaw != rightRaw;
                case LESS -> leftRaw < rightRaw;
                case LESS_OR_EQUAL -> leftRaw <= rightRaw;
                case GREATER -> leftRaw > rightRaw;
                case GREATER_OR_EQUAL -> leftRaw >= rightRaw;
                default -> throw new AssertionError(opcode);
            };
        }
        double left = Double.longBitsToDouble(leftRaw);
        double right = Double.longBitsToDouble(rightRaw);
        return switch (opcode) {
            case EQ -> left == right;
            case NOT_EQ -> left != right;
            case LESS -> left < right;
            case LESS_OR_EQUAL -> left <= right;
            case GREATER -> left > right;
            case GREATER_OR_EQUAL -> left >= right;
            case IS_NAN -> Double.isNaN(left);
            case IS_FINITE -> Double.isFinite(left);
            case IS_INFINITE -> Double.isInfinite(left);
            default -> throw new AssertionError(opcode);
        };
    }

    private static long lowBits(long value, int count) {
        if (count == 0) {
            return 0L;
        }
        return value & ((1L << count) - 1L);
    }
}
