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
import java.util.function.IntPredicate;

/** Immutable int predicate bytecode implementing the standard JDK predicate contract. */
public final class MIndexIntPredicate implements IntPredicate {
    private final MIndexPredicateProgram program;

    private MIndexIntPredicate(MIndexPredicateProgram program) { this.program = program; }

    public static MIndexIntPredicate equalTo(int value) { return compare(MIndexPredicateProgram.EQ, value); }
    public static MIndexIntPredicate notEqualTo(int value) { return compare(MIndexPredicateProgram.NOT_EQ, value); }
    public static MIndexIntPredicate lessThan(int value) { return compare(MIndexPredicateProgram.LESS, value); }
    public static MIndexIntPredicate lessOrEqual(int value) { return compare(MIndexPredicateProgram.LESS_OR_EQUAL, value); }
    public static MIndexIntPredicate greaterThan(int value) { return compare(MIndexPredicateProgram.GREATER, value); }
    public static MIndexIntPredicate greaterOrEqual(int value) { return compare(MIndexPredicateProgram.GREATER_OR_EQUAL, value); }

    private static MIndexIntPredicate compare(byte opcode, long operand) {
        return new MIndexIntPredicate(MIndexPredicateProgram.compare(
                MIndexPrimitiveProgram.INT, opcode, operand));
    }

    @Override
    public boolean test(int value) {
        return program.testRaw(value);
    }

    @Override
    public IntPredicate and(IntPredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexIntPredicate plan) {
            return new MIndexIntPredicate(program.and(plan.program));
        }
        return value -> test(value) && other.test(value);
    }

    @Override
    public IntPredicate or(IntPredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexIntPredicate plan) {
            return new MIndexIntPredicate(program.or(plan.program));
        }
        return value -> test(value) || other.test(value);
    }

    @Override
    public IntPredicate negate() {
        return new MIndexIntPredicate(program.negate());
    }

    public int instructionCount() { return program.instructionCount(); }
    public int maximumStackDepth() { return program.maxStack(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
