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

import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;

/**
 * Provides a set of common predicates for short values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class ShortPredicates
{
    private static final ShortIsEvenPredicate IS_EVEN = new ShortIsEvenPredicate();
    private static final ShortIsOddPredicate IS_ODD = new ShortIsOddPredicate();
    private static final ShortPredicate ALWAYS_TRUE = new AlwaysTrueShortPredicate();
    private static final ShortPredicate ALWAYS_FALSE = new AlwaysFalseShortPredicate();

    private ShortPredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static ShortPredicate equal(short expected)
    {
        return new EqualsShortPredicate(expected);
    }

    public static ShortPredicate lessThan(short expected)
    {
        return new LessThanShortPredicate(expected);
    }

    public static ShortPredicate greaterThan(short expected)
    {
        return new GreaterThanShortPredicate(expected);
    }

    public static ShortPredicate isEven()
    {
        return IS_EVEN;
    }

    public static ShortPredicate isOdd()
    {
        return IS_ODD;
    }

    public static ShortPredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static ShortPredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static ShortPredicate and(ShortPredicate one, ShortPredicate two)
    {
        return new AndShortPredicate(one, two);
    }

    public static ShortPredicate or(ShortPredicate one, ShortPredicate two)
    {
        return new OrShortPredicate(one, two);
    }

    public static ShortPredicate not(ShortPredicate negate)
    {
        return new NotShortPredicate(negate);
    }

    private static final class EqualsShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;
        private final short expected;

        private EqualsShortPredicate(short expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(short actual)
        {
            return actual == this.expected;
        }
    }

    private static final class LessThanShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        private final short expected;

        private LessThanShortPredicate(short expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(short actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        private final short expected;

        private GreaterThanShortPredicate(short expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(short actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        private final ShortPredicate one;
        private final ShortPredicate two;

        private AndShortPredicate(ShortPredicate one, ShortPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(short actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        private final ShortPredicate one;
        private final ShortPredicate two;

        private OrShortPredicate(ShortPredicate one, ShortPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(short actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        private final ShortPredicate negate;

        private NotShortPredicate(ShortPredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(short actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class ShortIsEvenPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(short integral)
        {
            return integral % 2 == 0;
        }
    }

    private static final class ShortIsOddPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(short integral)
        {
            return integral % 2 != 0;
        }
    }

    private static final class AlwaysTrueShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(short value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseShortPredicate implements ShortPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(short value)
        {
            return false;
        }
    }
}
