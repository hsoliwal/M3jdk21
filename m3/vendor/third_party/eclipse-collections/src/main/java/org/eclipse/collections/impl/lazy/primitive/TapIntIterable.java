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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapIntIterable
        extends AbstractLazyIntIterable
{
    private final IntIterable adapted;
    private final IntProcedure procedure;

    public TapIntIterable(IntIterable delegate, IntProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(IntProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public IntIterator intIterator()
    {
        return new TapIntIterator(this.adapted, this.procedure);
    }

    public static class TapIntIterator implements IntIterator
    {
        private final IntIterator iterator;
        private final IntProcedure procedure;

        public TapIntIterator(IntIterable iterable, IntProcedure procedure)
        {
            this(iterable.intIterator(), procedure);
        }

        public TapIntIterator(IntIterator iterator, IntProcedure procedure)
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
        public int next()
        {
            int next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
