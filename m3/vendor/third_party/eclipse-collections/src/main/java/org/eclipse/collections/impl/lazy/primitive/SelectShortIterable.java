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
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectShortIterable
        extends AbstractLazyShortIterable
{
    private final ShortIterable delegate;
    private final ShortPredicate predicate;

    public SelectShortIterable(ShortIterable delegate, ShortPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new SelectShortIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.delegate.forEach(new IfShortProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.shortIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.shortIterator().hasNext();
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        CountShortProcedure countShortProcedure = new CountShortProcedure(predicate);
        this.forEach(countShortProcedure);
        return countShortProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public short[] toArray()
    {
        final short[] array = new short[this.size()];
        this.forEach(new ShortProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(short each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        for (ShortIterator iterator = source.shortIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountShortProcedure implements ShortProcedure
    {
        private static final long serialVersionUID = 1L;
        private final ShortPredicate predicate;
        private int counter = 0;

        private CountShortProcedure(ShortPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(short each)
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

    private final class IfShortProcedure implements ShortProcedure
    {
        private static final long serialVersionUID = 1L;
        private final ShortProcedure procedure;

        private IfShortProcedure(ShortProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(short each)
        {
            if (SelectShortIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectShortIterator
            implements ShortIterator
    {
        private final ShortIterator iterator;
        private final ShortPredicate predicate;
        private short next;
        private boolean verifiedHasNext = false;

        private SelectShortIterator(ShortIterable iterable, ShortPredicate predicate)
        {
            this(iterable.shortIterator(), predicate);
        }

        private SelectShortIterator(ShortIterator iterator, ShortPredicate predicate)
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
                short temp = this.iterator.next();
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
        public short next()
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
