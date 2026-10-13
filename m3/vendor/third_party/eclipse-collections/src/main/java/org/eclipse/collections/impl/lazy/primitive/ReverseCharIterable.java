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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseCharIterable extends AbstractLazyCharIterable
{
    private final CharList adapted;

    public ReverseCharIterable(CharList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseCharIterable adapt(CharList charList)
    {
        return new ReverseCharIterable(charList);
    }

    @Override
    public CharIterator charIterator()
    {
        return new ReverseCharIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        CharIterator iterator = this.charIterator();
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
    public char max()
    {
        return this.adapted.max();
    }

    @Override
    public char min()
    {
        return this.adapted.min();
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
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
    public char[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public char[] toArray()
    {
        char[] results = new char[this.adapted.size()];
        int index = 0;
        CharIterator iterator = this.charIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(char value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(CharIterable source)
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

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    private class ReverseCharIterator implements CharIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseCharIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public char next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            char next = ReverseCharIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
