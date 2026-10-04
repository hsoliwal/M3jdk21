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
import java.util.function.DoublePredicate;

/** Immutable double predicate bytecode implementing the standard JDK predicate contract. */
public final class MIndexDoublePredicate implements DoublePredicate {
    private final MIndexPredicateProgram program;

    private MIndexDoublePredicate(MIndexPredicateProgram program) { this.program = program; }

    public static MIndexDoublePredicate equalTo(double value) { return compare(MIndexPredicateProgram.EQ, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate notEqualTo(double value) { return compare(MIndexPredicateProgram.NOT_EQ, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate lessThan(double value) { return compare(MIndexPredicateProgram.LESS, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate lessOrEqual(double value) { return compare(MIndexPredicateProgram.LESS_OR_EQUAL, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate greaterThan(double value) { return compare(MIndexPredicateProgram.GREATER, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate greaterOrEqual(double value) { return compare(MIndexPredicateProgram.GREATER_OR_EQUAL, Double.doubleToRawLongBits(value)); }
    public static MIndexDoublePredicate isNaN() {
        return new MIndexDoublePredicate(MIndexPredicateProgram.doubleTest(MIndexPredicateProgram.IS_NAN));
    }
    public static MIndexDoublePredicate isFinite() {
        return new MIndexDoublePredicate(MIndexPredicateProgram.doubleTest(MIndexPredicateProgram.IS_FINITE));
    }
    public static MIndexDoublePredicate isInfinite() {
        return new MIndexDoublePredicate(MIndexPredicateProgram.doubleTest(MIndexPredicateProgram.IS_INFINITE));
    }

    private static MIndexDoublePredicate compare(byte opcode, long operand) {
        return new MIndexDoublePredicate(MIndexPredicateProgram.compare(
                MIndexPrimitiveProgram.DOUBLE, opcode, operand));
    }

    @Override
    public boolean test(double value) {
        return program.testRaw(Double.doubleToRawLongBits(value));
    }

    @Override
    public DoublePredicate and(DoublePredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexDoublePredicate plan) {
            return new MIndexDoublePredicate(program.and(plan.program));
        }
        return value -> test(value) && other.test(value);
    }

    @Override
    public DoublePredicate or(DoublePredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexDoublePredicate plan) {
            return new MIndexDoublePredicate(program.or(plan.program));
        }
        return value -> test(value) || other.test(value);
    }

    @Override
    public DoublePredicate negate() {
        return new MIndexDoublePredicate(program.negate());
    }

    public int instructionCount() { return program.instructionCount(); }
    public int maximumStackDepth() { return program.maxStack(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
