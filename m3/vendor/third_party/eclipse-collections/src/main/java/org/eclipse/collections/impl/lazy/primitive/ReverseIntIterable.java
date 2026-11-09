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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.factory.primitive.IntBags;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseIntIterable extends AbstractLazyIntIterable
{
    private final IntList adapted;

    public ReverseIntIterable(IntList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseIntIterable adapt(IntList intList)
    {
        return new ReverseIntIterable(intList);
    }

    @Override
    public IntIterator intIterator()
    {
        return new ReverseIntIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        IntIterator iterator = this.intIterator();
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    @Override
    public long sum()
    {
        return this.adapted.sum();
    }

    @Override
    public int max()
    {
        return this.adapted.max();
    }

    @Override
    public int min()
    {
        return this.adapted.min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public int[] toArray()
    {
        int[] results = new int[this.adapted.size()];
        int index = 0;
        IntIterator iterator = this.intIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(int value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
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
    public MutableIntList toList()
    {
        return IntLists.mutable.withAll(this);
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntSets.mutable.withAll(this);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntBags.mutable.withAll(this);
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    private class ReverseIntIterator implements IntIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseIntIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public int next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            int next = ReverseIntIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
