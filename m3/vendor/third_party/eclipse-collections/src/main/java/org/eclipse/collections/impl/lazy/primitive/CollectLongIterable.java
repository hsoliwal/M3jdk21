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

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
public class CollectLongIterable<T>
        extends AbstractLazyLongIterable
{
    private final LazyIterable<T> iterable;
    private final LongFunction<? super T> function;
    private final LongFunctionToProcedure<T> longFunctionToProcedure;

    public CollectLongIterable(LazyIterable<T> adapted, LongFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.longFunctionToProcedure = new LongFunctionToProcedure<>(function);
    }

    @Override
    public LongIterator longIterator()
    {
        return new LongIterator()
        {
            private final Iterator<T> iterator = CollectLongIterable.this.iterable.iterator();

            @Override
            public long next()
            {
                return CollectLongIterable.this.function.longValueOf(this.iterator.next());
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
    public void each(LongProcedure procedure)
    {
        this.iterable.forEachWith(this.longFunctionToProcedure, procedure);
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
    public int count(final LongPredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.longValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final LongPredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.longValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final LongPredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.longValueOf(each)));
    }

    @Override
    public long[] toArray()
    {
        final long[] array = new long[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.longValueOf(each));
        return array;
    }

    @Override
    public long[] toSortedArray()
    {
        long[] array = this.toArray();
        Arrays.sort(array);
        return array;
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

    private static final class LongFunctionToProcedure<T> implements Procedure2<T, LongProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final LongFunction<? super T> function;

        private LongFunctionToProcedure(LongFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, LongProcedure procedure)
        {
            procedure.value(this.function.longValueOf(each));
        }
    }
}
