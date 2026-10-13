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
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.factory.primitive.BooleanBags;
import org.eclipse.collections.api.factory.primitive.BooleanLists;
import org.eclipse.collections.api.factory.primitive.BooleanSets;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectBooleanIterable
        extends AbstractLazyBooleanIterable
{
    private final BooleanIterable delegate;
    private final BooleanPredicate predicate;

    public SelectBooleanIterable(BooleanIterable delegate, BooleanPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new SelectBooleanIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.delegate.forEach(new IfBooleanProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.booleanIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.booleanIterator().hasNext();
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        CountBooleanProcedure countBooleanProcedure = new CountBooleanProcedure(predicate);
        this.forEach(countBooleanProcedure);
        return countBooleanProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public boolean[] toArray()
    {
        final boolean[] array = new boolean[this.size()];
        this.forEach(new BooleanProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(boolean each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        for (boolean value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        for (BooleanIterator iterator = source.booleanIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountBooleanProcedure implements BooleanProcedure
    {
        private static final long serialVersionUID = 1L;
        private final BooleanPredicate predicate;
        private int counter = 0;

        private CountBooleanProcedure(BooleanPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(boolean each)
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

    private final class IfBooleanProcedure implements BooleanProcedure
    {
        private static final long serialVersionUID = 1L;
        private final BooleanProcedure procedure;

        private IfBooleanProcedure(BooleanProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(boolean each)
        {
            if (SelectBooleanIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectBooleanIterator
            implements BooleanIterator
    {
        private final BooleanIterator iterator;
        private final BooleanPredicate predicate;
        private boolean next;
        private boolean verifiedHasNext = false;

        private SelectBooleanIterator(BooleanIterable iterable, BooleanPredicate predicate)
        {
            this(iterable.booleanIterator(), predicate);
        }

        private SelectBooleanIterator(BooleanIterator iterator, BooleanPredicate predicate)
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
                boolean temp = this.iterator.next();
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
        public boolean next()
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
