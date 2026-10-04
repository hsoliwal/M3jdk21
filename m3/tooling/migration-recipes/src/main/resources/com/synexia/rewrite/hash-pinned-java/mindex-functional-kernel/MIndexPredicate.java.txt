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

/**
 * Flat short-circuit Predicate composition.
 *
 * <p>Homogeneous AND or OR chains are flattened into one atom array. Mixed/negated compositions
 * fall back to one ordinary predicate atom so Java Predicate short-circuit and exception behavior
 * remains exact.</p>
 */
public final class MIndexPredicate<T> implements Predicate<T> {
    private static final byte SINGLE = 0;
    private static final byte AND = 1;
    private static final byte OR = 2;

    private final Predicate<Object>[] atoms;
    private final byte mode;
    private final boolean negated;

    @SuppressWarnings("unchecked")
    private MIndexPredicate(Predicate<?>[] atoms, byte mode, boolean negated) {
        this.atoms = (Predicate<Object>[]) atoms;
        this.mode = mode;
        this.negated = negated;
    }

    public static <T> MIndexPredicate<T> of(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate instanceof MIndexPredicate<?> plan) {
            @SuppressWarnings("unchecked")
            MIndexPredicate<T> typed = (MIndexPredicate<T>) plan;
            return typed;
        }
        return new MIndexPredicate<>(
                new Predicate<?>[] {predicate}, SINGLE, false);
    }

    @Override
    public boolean test(T value) {
        boolean result;
        if (mode == AND) {
            result = true;
            for (Predicate<Object> atom : atoms) {
                if (!atom.test(value)) {
                    result = false;
                    break;
                }
            }
        } else if (mode == OR) {
            result = false;
            for (Predicate<Object> atom : atoms) {
                if (atom.test(value)) {
                    result = true;
                    break;
                }
            }
        } else {
            result = atoms[0].test(value);
        }
        return negated ? !result : result;
    }

    @Override
    public MIndexPredicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        MIndexPredicate<T> right = MIndexPredicate.of(other);
        if (mergeable(AND) && right.mergeable(AND)) {
            return new MIndexPredicate<>(
                    concat(atoms, right.atoms), AND, false);
        }
        return MIndexPredicate.of(
                value -> this.test(value) && other.test(value));
    }

    @Override
    public MIndexPredicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        MIndexPredicate<T> right = MIndexPredicate.of(other);
        if (mergeable(OR) && right.mergeable(OR)) {
            return new MIndexPredicate<>(
                    concat(atoms, right.atoms), OR, false);
        }
        return MIndexPredicate.of(
                value -> this.test(value) || other.test(value));
    }

    @Override
    public MIndexPredicate<T> negate() {
        return new MIndexPredicate<>(atoms, mode, !negated);
    }

    public int atomCount() {
        return atoms.length;
    }

    public int instructionCount() {
        return atoms.length
                + (atoms.length > 1 ? atoms.length - 1 : 0)
                + (negated ? 1 : 0);
    }

    public int maximumStackDepth() {
        return 1;
    }

    private boolean mergeable(byte requestedMode) {
        return !negated && (mode == SINGLE || mode == requestedMode);
    }

    private static Predicate<?>[] concat(
            Predicate<?>[] left, Predicate<?>[] right) {
        Predicate<?>[] result =
                Arrays.copyOf(left, Math.addExact(left.length, right.length));
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }
}
