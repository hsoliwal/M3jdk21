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

import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;

/**
 * Provides a set of common predicates for char values.
 * This file was automatically generated from template file primitivePredicates.stg.
 */
public final class CharPredicates
{
    private static final IsUpperCaseCharPredicate IS_UPPER_CASE = new IsUpperCaseCharPredicate();
    private static final IsLowerCaseCharPredicate IS_LOWER_CASE = new IsLowerCaseCharPredicate();
    private static final IsDigitCharPredicate IS_DIGIT = new IsDigitCharPredicate();
    private static final IsDigitOrDotCharPredicate IS_DIGIT_OR_DOT = new IsDigitOrDotCharPredicate();
    private static final IsLetterCharPredicate IS_LETTER = new IsLetterCharPredicate();
    private static final IsLetterOrDigitCharPredicate IS_LETTER_OR_DIGIT = new IsLetterOrDigitCharPredicate();
    private static final IsWhiteSpaceCharPredicate IS_WHITESPACE = new IsWhiteSpaceCharPredicate();
    private static final IsUndefinedCharPredicate IS_UNDEFINED = new IsUndefinedCharPredicate();

    private static final CharPredicate ALWAYS_TRUE = new AlwaysTrueCharPredicate();
    private static final CharPredicate ALWAYS_FALSE = new AlwaysFalseCharPredicate();

    private CharPredicates()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @SuppressWarnings("MisspelledEquals")
    public static CharPredicate equal(char expected)
    {
        return new EqualsCharPredicate(expected);
    }

    public static CharPredicate lessThan(char expected)
    {
        return new LessThanCharPredicate(expected);
    }

    public static CharPredicate greaterThan(char expected)
    {
        return new GreaterThanCharPredicate(expected);
    }

    public static CharPredicate isUpperCase()
    {
        return IS_UPPER_CASE;
    }

    public static CharPredicate isLowerCase()
    {
        return IS_LOWER_CASE;
    }

    public static CharPredicate isDigit()
    {
        return IS_DIGIT;
    }

    public static CharPredicate isDigitOrDot()
    {
        return IS_DIGIT_OR_DOT;
    }

    public static CharPredicate isLetter()
    {
        return IS_LETTER;
    }

    public static CharPredicate isLetterOrDigit()
    {
        return IS_LETTER_OR_DIGIT;
    }

    public static CharPredicate isWhitespace()
    {
        return IS_WHITESPACE;
    }

    public static CharPredicate isUndefined()
    {
        return IS_UNDEFINED;
    }

    public static CharPredicate alwaysTrue()
    {
        return ALWAYS_TRUE;
    }

    public static CharPredicate alwaysFalse()
    {
        return ALWAYS_FALSE;
    }

    public static CharPredicate and(CharPredicate one, CharPredicate two)
    {
        return new AndCharPredicate(one, two);
    }

    public static CharPredicate or(CharPredicate one, CharPredicate two)
    {
        return new OrCharPredicate(one, two);
    }

    public static CharPredicate not(CharPredicate negate)
    {
        return new NotCharPredicate(negate);
    }

    private static final class EqualsCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;
        private final char expected;

        private EqualsCharPredicate(char expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(char actual)
        {
            return actual == this.expected;
        }
    }

    private static final class LessThanCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        private final char expected;

        private LessThanCharPredicate(char expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(char actual)
        {
            return actual < this.expected;
        }
    }

    private static final class GreaterThanCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        private final char expected;

        private GreaterThanCharPredicate(char expected)
        {
            this.expected = expected;
        }

        @Override
        public boolean accept(char actual)
        {
            return actual > this.expected;
        }
    }

    private static final class AndCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        private final CharPredicate one;
        private final CharPredicate two;

        private AndCharPredicate(CharPredicate one, CharPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(char actual)
        {
            return this.one.accept(actual) && this.two.accept(actual);
        }
    }

    private static final class OrCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        private final CharPredicate one;
        private final CharPredicate two;

        private OrCharPredicate(CharPredicate one, CharPredicate two)
        {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean accept(char actual)
        {
            return this.one.accept(actual) || this.two.accept(actual);
        }
    }

    private static final class NotCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        private final CharPredicate negate;

        private NotCharPredicate(CharPredicate negate)
        {
            this.negate = negate;
        }

        @Override
        public boolean accept(char actual)
        {
            return !this.negate.accept(actual);
        }
    }

    private static class IsUpperCaseCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isUpperCase(value);
        }
    }

    private static class IsLowerCaseCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isLowerCase(value);
        }
    }

    private static class IsDigitCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isDigit(value);
        }
    }

    private static class IsDigitOrDotCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isDigit(value) || value == '.';
        }
    }

    private static class IsLetterCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isLetter(value);
        }
    }

    private static class IsLetterOrDigitCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isLetterOrDigit(value);
        }
    }

    private static class IsWhiteSpaceCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return Character.isWhitespace(value);
        }
    }

    private static class IsUndefinedCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return !Character.isDefined(value);
        }
    }

    private static final class AlwaysTrueCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return true;
        }
    }

    private static final class AlwaysFalseCharPredicate implements CharPredicate
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(char value)
        {
            return false;
        }
    }
}
