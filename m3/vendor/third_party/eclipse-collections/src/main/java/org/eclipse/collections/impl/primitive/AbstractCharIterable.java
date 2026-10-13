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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;

/**
 * This file was automatically generated from template file abstractPrimitiveIterable.stg.
 * @since 6.0
 */
public abstract class AbstractCharIterable implements CharIterable
{
    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
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
        char[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            char first = sortedArray[middleIndex];
            char second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public char[] toSortedArray()
    {
        char[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableCharList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public MutableCharList toList()
    {
        return CharLists.mutable.withAll(this);
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharSets.mutable.withAll(this);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharBags.mutable.withAll(this);
    }
}
