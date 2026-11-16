/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.primitive;

import java.util.Arrays;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleBags;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleSets;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyDoubleIterableAdapter;

/**
 * This file was automatically generated from template file abstractPrimitiveIterable.stg.
 * @since 6.0
 */
@SuppressWarnings({"cast", "missing-explicit-ctor"})
public abstract class AbstractDoubleIterable implements DoubleIterable
{
    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
        double[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            double first = sortedArray[middleIndex];
            double second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public double[] toSortedArray()
    {
        double[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleLists.mutable.withAll(this);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleSets.mutable.withAll(this);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleBags.mutable.withAll(this);
    }
}
