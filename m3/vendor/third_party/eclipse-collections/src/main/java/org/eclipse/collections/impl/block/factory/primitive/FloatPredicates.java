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

import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;

/**
 * Provides a set of common predicates for float values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class FloatPredicates
{
    private static final FloatPredicate ALWAYS_TRUE = new AlwaysTrueFloatPredicate();
    private static final FloatPredicate ALWAYS_FALSE = new AlwaysFalseFloatPredicate();

    private FloatPredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static FloatPredicate equal(float expected)
    {
        return new EqualsFloatPredicate(expected);
    }

    public static FloatPredicate equal(float expected, float delta)
    {
        return new EqualsWithDeltaFloatPredicate(expected, delta);
    }

    public static FloatPredicate lessThan(float expected)
    {
        return new LessThanFloatPredicate(expected);
    }

    public static FloatPredicate greaterThan(float expected)
    {
        return new GreaterThanFloatPredicate(expected);
    }

    public static FloatPredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static FloatPredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static FloatPredicate and(FloatPredicate one, FloatPredicate two)
    {
        return new AndFloatPredicate(one, two);
    }

    public static FloatPredicate or(FloatPredicate one, FloatPredicate two)
    {
        return new OrFloatPredicate(one, two);
    }

    public static FloatPredicate not(FloatPredicate negate)
    {
        return new NotFloatPredicate(negate);
    }

    private static final class EqualsFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;
        private final float expected;

        private EqualsFloatPredicate(float expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(float actual)
        {
            return Float.compare(actual, this.expected) == 0;
        }
    }

    private static final class EqualsWithDeltaFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;
        private final float expected;
        private final float delta;

        private EqualsWithDeltaFloatPredicate(float expected, float delta)
        {
            this.expected = expected;
            this.delta = delta;
        }

        @Override
        public boolean accept(float actual)
        {
            return Math.abs(this.expected - actual) <= this.delta;
        }
    }

    private static final class LessThanFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        private final float expected;

        private LessThanFloatPredicate(float expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(float actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        private final float expected;

        private GreaterThanFloatPredicate(float expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(float actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        private final FloatPredicate one;
        private final FloatPredicate two;

        private AndFloatPredicate(FloatPredicate one, FloatPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(float actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        private final FloatPredicate one;
        private final FloatPredicate two;

        private OrFloatPredicate(FloatPredicate one, FloatPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(float actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        private final FloatPredicate negate;

        private NotFloatPredicate(FloatPredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(float actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static final class AlwaysTrueFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(float value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseFloatPredicate implements FloatPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(float value)
        {
            return false;
        }
    }
}
