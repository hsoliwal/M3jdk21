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
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.factory.primitive.IntBags;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectIntIterable
        extends AbstractLazyIntIterable
{
    private final IntIterable delegate;
    private final IntPredicate predicate;

    public SelectIntIterable(IntIterable delegate, IntPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public IntIterator intIterator()
    {
        return new SelectIntIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.delegate.forEach(new IfIntProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.intIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.intIterator().hasNext();
    }

    @Override
    public int count(IntPredicate predicate)
    {
        CountIntProcedure countIntProcedure = new CountIntProcedure(predicate);
        this.forEach(countIntProcedure);
        return countIntProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public int[] toArray()
    {
        final int[] array = new int[this.size()];
        this.forEach(new IntProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(int each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        for (IntIterator iterator = source.intIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountIntProcedure implements IntProcedure
    {
        private static final long serialVersionUID = 1L;
        private final IntPredicate predicate;
        private int counter = 0;

        private CountIntProcedure(IntPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(int each)
        {
            if (this.predicate.accept(each))
            {
                this.counter++;
            }
        }

        public int getCount()
        {
            return this.counter;
        }
    }

    private final class IfIntProcedure implements IntProcedure
    {
        private static final long serialVersionUID = 1L;
        private final IntProcedure procedure;

        private IfIntProcedure(IntProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(int each)
        {
            if (SelectIntIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectIntIterator
            implements IntIterator
    {
        private final IntIterator iterator;
        private final IntPredicate predicate;
        private int next;
        private boolean verifiedHasNext = false;

        private SelectIntIterator(IntIterable iterable, IntPredicate predicate)
        {
            this(iterable.intIterator(), predicate);
        }

        private SelectIntIterator(IntIterator iterator, IntPredicate predicate)
        {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext()
        {
            if (this.verifiedHasNext)
            {
                return true;
            }
            while (this.iterator.hasNext())
            {
                int temp = this.iterator.next();
                if (this.predicate.accept(temp))
                {
                    this.next = temp;
                    this.verifiedHasNext = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public int next()
        {
            if (this.verifiedHasNext || this.hasNext())
            {
                this.verifiedHasNext = false;
                return this.next;
            }
            throw new NoSuchElementException();
        }
    }
}
