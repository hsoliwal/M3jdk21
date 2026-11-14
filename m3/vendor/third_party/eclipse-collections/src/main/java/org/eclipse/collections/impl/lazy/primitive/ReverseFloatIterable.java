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
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseFloatIterable extends AbstractLazyFloatIterable
{
    private final FloatList adapted;

    public ReverseFloatIterable(FloatList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseFloatIterable adapt(FloatList floatList)
    {
        return new ReverseFloatIterable(floatList);
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new ReverseFloatIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        FloatIterator iterator = this.floatIterator();
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    @Override
    public double sum()
    {
        return this.adapted.sum();
    }

    @Override
    public float max()
    {
        return this.adapted.max();
    }

    @Override
    public float min()
    {
        return this.adapted.min();
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.max();
    }

    @Override
    public double average()
    {
        return this.adapted.average();
    }

    @Override
    public double median()
    {
        return this.adapted.median();
    }

    @Override
    public float[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public float[] toArray()
    {
        float[] results = new float[this.adapted.size()];
        int index = 0;
        FloatIterator iterator = this.floatIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(float value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public int size()
    {
        return this.adapted.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.adapted.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.adapted.notEmpty();
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

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    private class ReverseFloatIterator implements FloatIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseFloatIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            float next = ReverseFloatIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
