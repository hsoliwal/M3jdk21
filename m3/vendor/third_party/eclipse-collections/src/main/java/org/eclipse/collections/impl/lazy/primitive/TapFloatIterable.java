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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapFloatIterable
        extends AbstractLazyFloatIterable
{
    private final FloatIterable adapted;
    private final FloatProcedure procedure;

    public TapFloatIterable(FloatIterable delegate, FloatProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(FloatProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new TapFloatIterator(this.adapted, this.procedure);
    }

    public static class TapFloatIterator implements FloatIterator
    {
        private final FloatIterator iterator;
        private final FloatProcedure procedure;

        public TapFloatIterator(FloatIterable iterable, FloatProcedure procedure)
        {
            this(iterable.floatIterator(), procedure);
        }

        public TapFloatIterator(FloatIterator iterator, FloatProcedure procedure)
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
        public float next()
        {
            float next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
