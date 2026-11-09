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

import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;

/**
 * Provides a set of common predicates for long values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class LongPredicates
{
    private static final LongIsEvenPredicate IS_EVEN = new LongIsEvenPredicate();
    private static final LongIsOddPredicate IS_ODD = new LongIsOddPredicate();
    private static final LongPredicate ALWAYS_TRUE = new AlwaysTrueLongPredicate();
    private static final LongPredicate ALWAYS_FALSE = new AlwaysFalseLongPredicate();

    private LongPredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static LongPredicate equal(long expected)
    {
        return new EqualsLongPredicate(expected);
    }

    public static LongPredicate lessThan(long expected)
    {
        return new LessThanLongPredicate(expected);
    }

    public static LongPredicate greaterThan(long expected)
    {
        return new GreaterThanLongPredicate(expected);
    }

    public static LongPredicate isEven()
    {
        return IS_EVEN;
    }

    public static LongPredicate isOdd()
    {
        return IS_ODD;
    }

    public static LongPredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static LongPredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static LongPredicate and(LongPredicate one, LongPredicate two)
    {
        return new AndLongPredicate(one, two);
    }

    public static LongPredicate or(LongPredicate one, LongPredicate two)
    {
        return new OrLongPredicate(one, two);
    }

    public static LongPredicate not(LongPredicate negate)
    {
        return new NotLongPredicate(negate);
    }

    private static final class EqualsLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;
        private final long expected;

        private EqualsLongPredicate(long expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(long actual)
        {
            return actual == this.expected;
        }
    }

    private static final class LessThanLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        private final long expected;

        private LessThanLongPredicate(long expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(long actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        private final long expected;

        private GreaterThanLongPredicate(long expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(long actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        private final LongPredicate one;
        private final LongPredicate two;

        private AndLongPredicate(LongPredicate one, LongPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(long actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        private final LongPredicate one;
        private final LongPredicate two;

        private OrLongPredicate(LongPredicate one, LongPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(long actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        private final LongPredicate negate;

        private NotLongPredicate(LongPredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(long actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class LongIsEvenPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(long integral)
        {
            return integral % 2 == 0;
        }
    }

    private static final class LongIsOddPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(long integral)
        {
            return integral % 2 != 0;
        }
    }

    private static final class AlwaysTrueLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(long value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseLongPredicate implements LongPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(long value)
        {
            return false;
        }
    }
}
