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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.LongIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapLongIterable
        extends AbstractLazyLongIterable
{
    private final LongIterable adapted;
    private final LongProcedure procedure;

    public TapLongIterable(LongIterable delegate, LongProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(LongProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public LongIterator longIterator()
    {
        return new TapLongIterator(this.adapted, this.procedure);
    }

    public static class TapLongIterator implements LongIterator
    {
        private final LongIterator iterator;
        private final LongProcedure procedure;

        public TapLongIterator(LongIterable iterable, LongProcedure procedure)
        {
            this(iterable.longIterator(), procedure);
        }

        public TapLongIterator(LongIterator iterator, LongProcedure procedure)
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
        public long next()
        {
            long next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
