/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.factory.primitive;

import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;

/**
 * Provides a set of common predicates for byte values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class BytePredicates
{
    private static final ByteIsEvenPredicate IS_EVEN = new ByteIsEvenPredicate();
    private static final ByteIsOddPredicate IS_ODD = new ByteIsOddPredicate();
    private static final BytePredicate ALWAYS_TRUE = new AlwaysTrueBytePredicate();
    private static final BytePredicate ALWAYS_FALSE = new AlwaysFalseBytePredicate();

    private BytePredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static BytePredicate equal(byte expected)
    {
        return new EqualsBytePredicate(expected);
    }

    public static BytePredicate lessThan(byte expected)
    {
        return new LessThanBytePredicate(expected);
    }

    public static BytePredicate greaterThan(byte expected)
    {
        return new GreaterThanBytePredicate(expected);
    }

    public static BytePredicate isEven()
    {
        return IS_EVEN;
    }

    public static BytePredicate isOdd()
    {
        return IS_ODD;
    }

    public static BytePredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static BytePredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static BytePredicate and(BytePredicate one, BytePredicate two)
    {
        return new AndBytePredicate(one, two);
    }

    public static BytePredicate or(BytePredicate one, BytePredicate two)
    {
        return new OrBytePredicate(one, two);
    }

    public static BytePredicate not(BytePredicate negate)
    {
        return new NotBytePredicate(negate);
    }

    private static final class EqualsBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;
        private final byte expected;

        private EqualsBytePredicate(byte expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(byte actual)
        {
            return actual == this.expected;
        }
    }

    private static final class LessThanBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        private final byte expected;

        private LessThanBytePredicate(byte expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(byte actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        private final byte expected;

        private GreaterThanBytePredicate(byte expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(byte actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        private final BytePredicate one;
        private final BytePredicate two;

        private AndBytePredicate(BytePredicate one, BytePredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(byte actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        private final BytePredicate one;
        private final BytePredicate two;

        private OrBytePredicate(BytePredicate one, BytePredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(byte actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        private final BytePredicate negate;

        private NotBytePredicate(BytePredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(byte actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class ByteIsEvenPredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(byte integral)
        {
            return integral % 2 == 0;
        }
    }

    private static final class ByteIsOddPredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(byte integral)
        {
            return integral % 2 != 0;
        }
    }

    private static final class AlwaysTrueBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(byte value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseBytePredicate implements BytePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(byte value)
        {
            return false;
        }
    }
}
