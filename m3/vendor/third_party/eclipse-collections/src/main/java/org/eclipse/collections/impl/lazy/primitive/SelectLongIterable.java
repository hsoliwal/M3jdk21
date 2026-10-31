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
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectLongIterable
        extends AbstractLazyLongIterable
{
    private final LongIterable delegate;
    private final LongPredicate predicate;

    public SelectLongIterable(LongIterable delegate, LongPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public LongIterator longIterator()
    {
        return new SelectLongIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.delegate.forEach(new IfLongProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.longIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.longIterator().hasNext();
    }

    @Override
    public int count(LongPredicate predicate)
    {
        CountLongProcedure countLongProcedure = new CountLongProcedure(predicate);
        this.forEach(countLongProcedure);
        return countLongProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public long[] toArray()
    {
        final long[] array = new long[this.size()];
        this.forEach(new LongProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(long each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountLongProcedure implements LongProcedure
    {
        private static final long serialVersionUID = 1L;
        private final LongPredicate predicate;
        private int counter = 0;

        private CountLongProcedure(LongPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(long each)
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

    private final class IfLongProcedure implements LongProcedure
    {
        private static final long serialVersionUID = 1L;
        private final LongProcedure procedure;

        private IfLongProcedure(LongProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(long each)
        {
            if (SelectLongIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectLongIterator
            implements LongIterator
    {
        private final LongIterator iterator;
        private final LongPredicate predicate;
        private long next;
        private boolean verifiedHasNext = false;

        private SelectLongIterator(LongIterable iterable, LongPredicate predicate)
        {
            this(iterable.longIterator(), predicate);
        }

        private SelectLongIterator(LongIterator iterator, LongPredicate predicate)
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
                long temp = this.iterator.next();
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
        public long next()
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
