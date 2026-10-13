/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.primitive;

import java.util.Arrays;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;

/**
 * This file was automatically generated from template file abstractPrimitiveIterable.stg.
 * @since 6.0
 */
public abstract class AbstractShortIterable implements ShortIterable
{
    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.max();
    }

    @Override
    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        short[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            short first = sortedArray[middleIndex];
            short second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public short[] toSortedArray()
    {
        short[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableShortList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
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
}
