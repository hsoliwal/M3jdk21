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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.factory.primitive.DoubleBags;
import org.eclipse.collections.api.factory.primitive.DoubleLists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectDoubleIterable
        extends AbstractLazyDoubleIterable
{
    private final DoubleIterable delegate;
    private final DoublePredicate predicate;

    public SelectDoubleIterable(DoubleIterable delegate, DoublePredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new SelectDoubleIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.delegate.forEach(new IfDoubleProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.doubleIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.doubleIterator().hasNext();
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        CountDoubleProcedure countDoubleProcedure = new CountDoubleProcedure(predicate);
        this.forEach(countDoubleProcedure);
        return countDoubleProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public double[] toArray()
    {
        final double[] array = new double[this.size()];
        this.forEach(new DoubleProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(double each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountDoubleProcedure implements DoubleProcedure
    {
        private static final long serialVersionUID = 1L;
        private final DoublePredicate predicate;
        private int counter = 0;

        private CountDoubleProcedure(DoublePredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(double each)
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

    private final class IfDoubleProcedure implements DoubleProcedure
    {
        private static final long serialVersionUID = 1L;
        private final DoubleProcedure procedure;

        private IfDoubleProcedure(DoubleProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(double each)
        {
            if (SelectDoubleIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectDoubleIterator
            implements DoubleIterator
    {
        private final DoubleIterator iterator;
        private final DoublePredicate predicate;
        private double next;
        private boolean verifiedHasNext = false;

        private SelectDoubleIterator(DoubleIterable iterable, DoublePredicate predicate)
        {
            this(iterable.doubleIterator(), predicate);
        }

        private SelectDoubleIterator(DoubleIterator iterator, DoublePredicate predicate)
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
                double temp = this.iterator.next();
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
        public double next()
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
