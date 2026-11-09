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
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.factory.primitive.DoubleBags;
import org.eclipse.collections.api.factory.primitive.DoubleLists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseDoubleIterable extends AbstractLazyDoubleIterable
{
    private final DoubleList adapted;

    public ReverseDoubleIterable(DoubleList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseDoubleIterable adapt(DoubleList doubleList)
    {
        return new ReverseDoubleIterable(doubleList);
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new ReverseDoubleIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        DoubleIterator iterator = this.doubleIterator();
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    @Override
    public double sum()
    {
        return this.adapted.sum();
    }

    @Override
    public double max()
    {
        return this.adapted.max();
    }

    @Override
    public double min()
    {
        return this.adapted.min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public double[] toArray()
    {
        double[] results = new double[this.adapted.size()];
        int index = 0;
        DoubleIterator iterator = this.doubleIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(double value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
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

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    private class ReverseDoubleIterator implements DoubleIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseDoubleIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            double next = ReverseDoubleIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
