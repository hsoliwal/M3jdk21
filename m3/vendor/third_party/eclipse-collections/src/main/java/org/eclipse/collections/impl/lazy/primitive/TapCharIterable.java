/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.CharIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapCharIterable
        extends AbstractLazyCharIterable
{
    private final CharIterable adapted;
    private final CharProcedure procedure;

    public TapCharIterable(CharIterable delegate, CharProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(CharProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public CharIterator charIterator()
    {
        return new TapCharIterator(this.adapted, this.procedure);
    }

    public static class TapCharIterator implements CharIterator
    {
        private final CharIterator iterator;
        private final CharProcedure procedure;

        public TapCharIterator(CharIterable iterable, CharProcedure procedure)
        {
            this(iterable.charIterator(), procedure);
        }

        public TapCharIterator(CharIterator iterator, CharProcedure procedure)
        {
            this.iterator = iterator;
            this.procedure = procedure;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public char next()
        {
            char next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
