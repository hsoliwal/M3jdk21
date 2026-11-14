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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.factory.primitive.BooleanBags;
import org.eclipse.collections.api.factory.primitive.BooleanLists;
import org.eclipse.collections.api.factory.primitive.BooleanSets;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseBooleanIterable extends AbstractLazyBooleanIterable
{
    private final BooleanList adapted;

    public ReverseBooleanIterable(BooleanList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseBooleanIterable adapt(BooleanList booleanList)
    {
        return new ReverseBooleanIterable(booleanList);
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new ReverseBooleanIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        BooleanIterator iterator = this.booleanIterator();
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    @Override
    public boolean[] toArray()
    {
        boolean[] results = new boolean[this.adapted.size()];
        int index = 0;
        BooleanIterator iterator = this.booleanIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
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
    public MutableBooleanList toList()
    {
        return BooleanLists.mutable.withAll(this);
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return BooleanSets.mutable.withAll(this);
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return BooleanBags.mutable.withAll(this);
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    private class ReverseBooleanIterator implements BooleanIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseBooleanIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public boolean next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            boolean next = ReverseBooleanIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
