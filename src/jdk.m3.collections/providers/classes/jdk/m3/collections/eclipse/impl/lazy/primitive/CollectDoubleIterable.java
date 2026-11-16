/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import java.util.Arrays;
import java.util.Iterator;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure2;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleBags;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleSets;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
@SuppressWarnings("deprecation")
public class CollectDoubleIterable<T>
        extends AbstractLazyDoubleIterable
{
    private final RichIterable<T> iterable;
    private final DoubleFunction<? super T> function;
    private final DoubleFunctionToProcedure<T> doubleFunctionToProcedure;

    public CollectDoubleIterable(RichIterable<T> adapted, DoubleFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.doubleFunctionToProcedure = new DoubleFunctionToProcedure<>(function);
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new DoubleIterator()
        {
            private final Iterator<T> iterator = CollectDoubleIterable.this.iterable.iterator();

            @Override
            public double next()
            {
                return CollectDoubleIterable.this.function.doubleValueOf(this.iterator.next());
            }

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }
        };
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.iterable.forEachWith(this.doubleFunctionToProcedure, procedure);
    }

    @Override
    public int size()
    {
        return this.iterable.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.iterable.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.iterable.notEmpty();
    }

    @Override
    public int count(final DoublePredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.doubleValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final DoublePredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.doubleValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final DoublePredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.doubleValueOf(each)));
    }

    @Override
    public double[] toArray()
    {
        final double[] array = new double[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.doubleValueOf(each));
        return array;
    }

    @Override
    public double[] toSortedArray()
    {
        double[] array = this.toArray();
        Arrays.sort(array);
        return array;
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

    private static final class DoubleFunctionToProcedure<T> implements Procedure2<T, DoubleProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final DoubleFunction<? super T> function;

        private DoubleFunctionToProcedure(DoubleFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, DoubleProcedure procedure)
        {
            procedure.value(this.function.doubleValueOf(each));
        }
    }
}
