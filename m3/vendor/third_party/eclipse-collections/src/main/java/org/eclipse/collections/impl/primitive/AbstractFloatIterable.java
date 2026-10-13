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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;

/**
 * This file was automatically generated from template file abstractPrimitiveIterable.stg.
 * @since 6.0
 */
public abstract class AbstractFloatIterable implements FloatIterable
{
    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
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
        float[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            float first = sortedArray[middleIndex];
            float second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public float[] toSortedArray()
    {
        float[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
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
}
