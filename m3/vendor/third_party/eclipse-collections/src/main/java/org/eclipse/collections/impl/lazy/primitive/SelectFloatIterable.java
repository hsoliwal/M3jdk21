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

import java.util.NoSuchElementException;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectFloatIterable
        extends AbstractLazyFloatIterable
{
    private final FloatIterable delegate;
    private final FloatPredicate predicate;

    public SelectFloatIterable(FloatIterable delegate, FloatPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new SelectFloatIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.delegate.forEach(new IfFloatProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.floatIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.floatIterator().hasNext();
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        CountFloatProcedure countFloatProcedure = new CountFloatProcedure(predicate);
        this.forEach(countFloatProcedure);
        return countFloatProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public float[] toArray()
    {
        final float[] array = new float[this.size()];
        this.forEach(new FloatProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(float each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatLists.mutable.withAll(this);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatSets.mutable.withAll(this);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatBags.mutable.withAll(this);
    }

    private static final class CountFloatProcedure implements FloatProcedure
    {
        private static final long serialVersionUID = 1L;
        private final FloatPredicate predicate;
        private int counter = 0;

        private CountFloatProcedure(FloatPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(float each)
        {
            if (this.predicate.accept(each))
            {
                this.counter++;
            }
        }

        public int getCount()
        {
            return this.counter;
        }
    }

    private final class IfFloatProcedure implements FloatProcedure
    {
        private static final long serialVersionUID = 1L;
        private final FloatProcedure procedure;

        private IfFloatProcedure(FloatProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(float each)
        {
            if (SelectFloatIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectFloatIterator
            implements FloatIterator
    {
        private final FloatIterator iterator;
        private final FloatPredicate predicate;
        private float next;
        private boolean verifiedHasNext = false;

        private SelectFloatIterator(FloatIterable iterable, FloatPredicate predicate)
        {
            this(iterable.floatIterator(), predicate);
        }

        private SelectFloatIterator(FloatIterator iterator, FloatPredicate predicate)
        {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext()
        {
            if (this.verifiedHasNext)
            {
                return true;
            }
            while (this.iterator.hasNext())
            {
                float temp = this.iterator.next();
                if (this.predicate.accept(temp))
                {
                    this.next = temp;
                    this.verifiedHasNext = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public float next()
        {
            if (this.verifiedHasNext || this.hasNext())
            {
                this.verifiedHasNext = false;
                return this.next;
            }
            throw new NoSuchElementException();
        }
    }
}
