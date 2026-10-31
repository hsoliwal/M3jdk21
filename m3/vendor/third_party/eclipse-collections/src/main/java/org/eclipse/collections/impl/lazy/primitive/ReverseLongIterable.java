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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseLongIterable extends AbstractLazyLongIterable
{
    private final LongList adapted;

    public ReverseLongIterable(LongList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseLongIterable adapt(LongList longList)
    {
        return new ReverseLongIterable(longList);
    }

    @Override
    public LongIterator longIterator()
    {
        return new ReverseLongIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        LongIterator iterator = this.longIterator();
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
    public long max()
    {
        return this.adapted.max();
    }

    @Override
    public long min()
    {
        return this.adapted.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public long[] toArray()
    {
        long[] results = new long[this.adapted.size()];
        int index = 0;
        LongIterator iterator = this.longIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(long value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
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
    public MutableLongList toList()
    {
        return LongLists.mutable.withAll(this);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongSets.mutable.withAll(this);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongBags.mutable.withAll(this);
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    private class ReverseLongIterator implements LongIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseLongIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public long next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            long next = ReverseLongIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
