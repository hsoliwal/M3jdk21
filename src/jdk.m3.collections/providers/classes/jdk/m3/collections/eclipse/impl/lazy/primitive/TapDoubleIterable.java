/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapDoubleIterable
        extends AbstractLazyDoubleIterable
{
    private final DoubleIterable adapted;
    private final DoubleProcedure procedure;

    public TapDoubleIterable(DoubleIterable delegate, DoubleProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(DoubleProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new TapDoubleIterator(this.adapted, this.procedure);
    }

    public static class TapDoubleIterator implements DoubleIterator
    {
        private final DoubleIterator iterator;
        private final DoubleProcedure procedure;

        public TapDoubleIterator(DoubleIterable iterable, DoubleProcedure procedure)
        {
            this(iterable.doubleIterator(), procedure);
        }

        public TapDoubleIterator(DoubleIterator iterator, DoubleProcedure procedure)
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
        public double next()
        {
            double next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
