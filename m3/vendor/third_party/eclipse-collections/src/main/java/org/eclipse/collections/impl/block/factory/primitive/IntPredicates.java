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

import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;

/**
 * Provides a set of common predicates for int values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class IntPredicates
{
    private static final IntIsEvenPredicate IS_EVEN = new IntIsEvenPredicate();
    private static final IntIsOddPredicate IS_ODD = new IntIsOddPredicate();
    private static final IntPredicate ALWAYS_TRUE = new AlwaysTrueIntPredicate();
    private static final IntPredicate ALWAYS_FALSE = new AlwaysFalseIntPredicate();

    private IntPredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static IntPredicate equal(int expected)
    {
        return new EqualsIntPredicate(expected);
    }

    public static IntPredicate lessThan(int expected)
    {
        return new LessThanIntPredicate(expected);
    }

    public static IntPredicate greaterThan(int expected)
    {
        return new GreaterThanIntPredicate(expected);
    }

    public static IntPredicate isEven()
    {
        return IS_EVEN;
    }

    public static IntPredicate isOdd()
    {
        return IS_ODD;
    }

    public static IntPredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static IntPredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static IntPredicate and(IntPredicate one, IntPredicate two)
    {
        return new AndIntPredicate(one, two);
    }

    public static IntPredicate or(IntPredicate one, IntPredicate two)
    {
        return new OrIntPredicate(one, two);
    }

    public static IntPredicate not(IntPredicate negate)
    {
        return new NotIntPredicate(negate);
    }

    private static final class EqualsIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;
        private final int expected;

        private EqualsIntPredicate(int expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(int actual)
        {
            return actual == this.expected;
        }
    }

    private static final class LessThanIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        private final int expected;

        private LessThanIntPredicate(int expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(int actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        private final int expected;

        private GreaterThanIntPredicate(int expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(int actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        private final IntPredicate one;
        private final IntPredicate two;

        private AndIntPredicate(IntPredicate one, IntPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(int actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        private final IntPredicate one;
        private final IntPredicate two;

        private OrIntPredicate(IntPredicate one, IntPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(int actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        private final IntPredicate negate;

        private NotIntPredicate(IntPredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(int actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class IntIsEvenPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(int integral)
        {
            return integral % 2 == 0;
        }
    }

    private static final class IntIsOddPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(int integral)
        {
            return integral % 2 != 0;
        }
    }

    private static final class AlwaysTrueIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(int value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseIntPredicate implements IntPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(int value)
        {
            return false;
        }
    }
}
