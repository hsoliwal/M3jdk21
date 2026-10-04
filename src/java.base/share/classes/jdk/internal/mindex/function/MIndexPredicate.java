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
import java.util.Objects;
import java.util.function.Predicate;

/** Flat postfix Predicate composition with no nested predicate-wrapper chain. */
public final class MIndexPredicate<T> implements Predicate<T> {
    private static final byte PUSH = 1;
    private static final byte AND = 2;
    private static final byte OR = 3;
    private static final byte NOT = 4;

    private final Predicate<Object>[] atoms;
    private final byte[] code;
    private final int[] atomIndex;
    private final int maxStack;

    @SuppressWarnings("unchecked")
    private MIndexPredicate(
            Predicate<?>[] atoms,
            byte[] code,
            int[] atomIndex) {
        this.atoms = (Predicate<Object>[]) atoms;
        this.code = code;
        this.atomIndex = atomIndex;
        this.maxStack = validate(code);
    }

    public static <T> MIndexPredicate<T> of(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate instanceof MIndexPredicate<?> plan) {
            @SuppressWarnings("unchecked")
            MIndexPredicate<T> typed = (MIndexPredicate<T>) plan;
            return typed;
        }
        return new MIndexPredicate<>(
                new Predicate<?>[] {predicate},
                new byte[] {PUSH},
                new int[] {0});
    }

    @Override
    public boolean test(T value) {
        long stack = 0L;
        int depth = 0;
        for (int index = 0; index < code.length; index++) {
            switch (code[index]) {
                case PUSH -> {
                    if (atoms[atomIndex[index]].test(value)) {
                        stack |= 1L << depth;
                    } else {
                        stack &= ~(1L << depth);
                    }
                    depth++;
                }
                case AND, OR -> {
                    boolean right = ((stack >>> (depth - 1)) & 1L) != 0L;
                    boolean left = ((stack >>> (depth - 2)) & 1L) != 0L;
                    depth -= 2;
                    stack = lowBits(stack, depth);
                    boolean result = code[index] == AND ? left && right : left || right;
                    if (result) {
                        stack |= 1L << depth;
                    }
                    depth++;
                }
                case NOT -> stack ^= 1L << (depth - 1);
                default -> throw new AssertionError(code[index]);
            }
        }
        return (stack & 1L) != 0L;
    }

    @Override
    public MIndexPredicate<T> and(Predicate<? super T> other) {
        return combine(other, AND);
    }

    @Override
    public MIndexPredicate<T> or(Predicate<? super T> other) {
        return combine(other, OR);
    }

    @Override
    public MIndexPredicate<T> negate() {
        byte[] joinedCode = Arrays.copyOf(code, code.length + 1);
        int[] joinedIndex = Arrays.copyOf(atomIndex, atomIndex.length + 1);
        joinedCode[code.length] = NOT;
        return new MIndexPredicate<>(atoms.clone(), joinedCode, joinedIndex);
    }

    public int atomCount() {
        return atoms.length;
    }

    public int instructionCount() {
        return code.length;
    }

    public int maximumStackDepth() {
        return maxStack;
    }

    private MIndexPredicate<T> combine(Predicate<? super T> other, byte logical) {
        MIndexPredicate<T> right = MIndexPredicate.of(other);
        int atomOffset = atoms.length;
        Predicate<?>[] joinedAtoms =
                Arrays.copyOf(atoms, Math.addExact(atoms.length, right.atoms.length));
        System.arraycopy(right.atoms, 0, joinedAtoms, atomOffset, right.atoms.length);

        int codeLength = Math.addExact(Math.addExact(code.length, right.code.length), 1);
        byte[] joinedCode = Arrays.copyOf(code, codeLength);
        int[] joinedIndex = Arrays.copyOf(atomIndex, codeLength);
        System.arraycopy(right.code, 0, joinedCode, code.length, right.code.length);
        for (int index = 0; index < right.atomIndex.length; index++) {
            joinedIndex[code.length + index] =
                    right.code[index] == PUSH
                            ? Math.addExact(atomOffset, right.atomIndex[index])
                            : 0;
        }
        joinedCode[codeLength - 1] = logical;
        return new MIndexPredicate<>(joinedAtoms, joinedCode, joinedIndex);
    }

    private static int validate(byte[] code) {
        int depth = 0;
        int maximum = 0;
        for (byte opcode : code) {
            if (opcode == PUSH) {
                maximum = Math.max(maximum, ++depth);
                if (maximum > 63) {
                    throw new IllegalArgumentException("predicate stack exceeds 63 entries");
                }
            } else if (opcode == AND || opcode == OR) {
                if (depth < 2) {
                    throw new IllegalArgumentException("malformed predicate program");
                }
                depth--;
            } else if (opcode == NOT) {
                if (depth < 1) {
                    throw new IllegalArgumentException("malformed predicate program");
                }
            } else {
                throw new IllegalArgumentException("unknown predicate opcode");
            }
        }
        if (depth != 1) {
            throw new IllegalArgumentException("malformed predicate program");
        }
        return maximum;
    }

    private static long lowBits(long value, int count) {
        return count == 0 ? 0L : value & ((1L << count) - 1L);
    }
}
