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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapShortIterable
        extends AbstractLazyShortIterable
{
    private final ShortIterable adapted;
    private final ShortProcedure procedure;

    public TapShortIterable(ShortIterable delegate, ShortProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(ShortProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new TapShortIterator(this.adapted, this.procedure);
    }

    public static class TapShortIterator implements ShortIterator
    {
        private final ShortIterator iterator;
        private final ShortProcedure procedure;

        public TapShortIterator(ShortIterable iterable, ShortProcedure procedure)
        {
            this(iterable.shortIterator(), procedure);
        }

        public TapShortIterator(ShortIterator iterator, ShortProcedure procedure)
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
        public short next()
        {
            short next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
