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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.ShortList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseShortIterable extends AbstractLazyShortIterable
{
    private final ShortList adapted;

    public ReverseShortIterable(ShortList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseShortIterable adapt(ShortList shortList)
    {
        return new ReverseShortIterable(shortList);
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new ReverseShortIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        ShortIterator iterator = this.shortIterator();
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
    public short max()
    {
        return this.adapted.max();
    }

    @Override
    public short min()
    {
        return this.adapted.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public short[] toArray()
    {
        short[] results = new short[this.adapted.size()];
        int index = 0;
        ShortIterator iterator = this.shortIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(short value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
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
    public MutableShortList toList()
    {
        return ShortLists.mutable.withAll(this);
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortSets.mutable.withAll(this);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortBags.mutable.withAll(this);
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    private class ReverseShortIterator implements ShortIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseShortIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public short next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            short next = ReverseShortIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
