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
import java.util.function.LongPredicate;

/** Immutable long predicate bytecode implementing the standard JDK predicate contract. */
public final class MIndexLongPredicate implements LongPredicate {
    private final MIndexPredicateProgram program;

    private MIndexLongPredicate(MIndexPredicateProgram program) { this.program = program; }

    public static MIndexLongPredicate equalTo(long value) { return compare(MIndexPredicateProgram.EQ, value); }
    public static MIndexLongPredicate notEqualTo(long value) { return compare(MIndexPredicateProgram.NOT_EQ, value); }
    public static MIndexLongPredicate lessThan(long value) { return compare(MIndexPredicateProgram.LESS, value); }
    public static MIndexLongPredicate lessOrEqual(long value) { return compare(MIndexPredicateProgram.LESS_OR_EQUAL, value); }
    public static MIndexLongPredicate greaterThan(long value) { return compare(MIndexPredicateProgram.GREATER, value); }
    public static MIndexLongPredicate greaterOrEqual(long value) { return compare(MIndexPredicateProgram.GREATER_OR_EQUAL, value); }

    private static MIndexLongPredicate compare(byte opcode, long operand) {
        return new MIndexLongPredicate(MIndexPredicateProgram.compare(
                MIndexPrimitiveProgram.LONG, opcode, operand));
    }

    @Override
    public boolean test(long value) {
        return program.testRaw(value);
    }

    @Override
    public LongPredicate and(LongPredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexLongPredicate plan) {
            return new MIndexLongPredicate(program.and(plan.program));
        }
        return value -> test(value) && other.test(value);
    }

    @Override
    public LongPredicate or(LongPredicate other) {
        Objects.requireNonNull(other);
        if (other instanceof MIndexLongPredicate plan) {
            return new MIndexLongPredicate(program.or(plan.program));
        }
        return value -> test(value) || other.test(value);
    }

    @Override
    public LongPredicate negate() {
        return new MIndexLongPredicate(program.negate());
    }

    public int instructionCount() { return program.instructionCount(); }
    public int maximumStackDepth() { return program.maxStack(); }
    public long estimatedRetainedBytes() { return program.retainedBytes(); }
}
