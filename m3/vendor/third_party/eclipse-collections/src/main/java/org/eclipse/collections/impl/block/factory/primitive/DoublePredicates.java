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

import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;

/**
 * Provides a set of common predicates for double values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class DoublePredicates
{
    private static final DoublePredicate ALWAYS_TRUE = new AlwaysTrueDoublePredicate();
    private static final DoublePredicate ALWAYS_FALSE = new AlwaysFalseDoublePredicate();

    private DoublePredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static DoublePredicate equal(double expected)
    {
        return new EqualsDoublePredicate(expected);
    }

    public static DoublePredicate equal(double expected, double delta)
    {
        return new EqualsWithDeltaDoublePredicate(expected, delta);
    }

    public static DoublePredicate lessThan(double expected)
    {
        return new LessThanDoublePredicate(expected);
    }

    public static DoublePredicate greaterThan(double expected)
    {
        return new GreaterThanDoublePredicate(expected);
    }

    public static DoublePredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static DoublePredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static DoublePredicate and(DoublePredicate one, DoublePredicate two)
    {
        return new AndDoublePredicate(one, two);
    }

    public static DoublePredicate or(DoublePredicate one, DoublePredicate two)
    {
        return new OrDoublePredicate(one, two);
    }

    public static DoublePredicate not(DoublePredicate negate)
    {
        return new NotDoublePredicate(negate);
    }

    private static final class EqualsDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;
        private final double expected;

        private EqualsDoublePredicate(double expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(double actual)
        {
            return Double.compare(actual, this.expected) == 0;
        }
    }

    private static final class EqualsWithDeltaDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;
        private final double expected;
        private final double delta;

        private EqualsWithDeltaDoublePredicate(double expected, double delta)
        {
            this.expected = expected;
            this.delta = delta;
        }

        @Override
        public boolean accept(double actual)
        {
            return Math.abs(this.expected - actual) <= this.delta;
        }
    }

    private static final class LessThanDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        private final double expected;

        private LessThanDoublePredicate(double expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(double actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        private final double expected;

        private GreaterThanDoublePredicate(double expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(double actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        private final DoublePredicate one;
        private final DoublePredicate two;

        private AndDoublePredicate(DoublePredicate one, DoublePredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(double actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        private final DoublePredicate one;
        private final DoublePredicate two;

        private OrDoublePredicate(DoublePredicate one, DoublePredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(double actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        private final DoublePredicate negate;

        private NotDoublePredicate(DoublePredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(double actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class AlwaysTrueDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(double value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseDoublePredicate implements DoublePredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(double value)
        {
            return false;
        }
    }
}
