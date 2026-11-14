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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
public class CollectShortIterable<T>
        extends AbstractLazyShortIterable
{
    private final LazyIterable<T> iterable;
    private final ShortFunction<? super T> function;
    private final ShortFunctionToProcedure<T> shortFunctionToProcedure;

    public CollectShortIterable(LazyIterable<T> adapted, ShortFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.shortFunctionToProcedure = new ShortFunctionToProcedure<>(function);
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new ShortIterator()
        {
            private final Iterator<T> iterator = CollectShortIterable.this.iterable.iterator();

            @Override
            public short next()
            {
                return CollectShortIterable.this.function.shortValueOf(this.iterator.next());
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
    public void each(ShortProcedure procedure)
    {
        this.iterable.forEachWith(this.shortFunctionToProcedure, procedure);
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
    public int count(final ShortPredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.shortValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final ShortPredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.shortValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final ShortPredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.shortValueOf(each)));
    }

    @Override
    public short[] toArray()
    {
        final short[] array = new short[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.shortValueOf(each));
        return array;
    }

    @Override
    public short[] toSortedArray()
    {
        short[] array = this.toArray();
        Arrays.sort(array);
        return array;
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

    private static final class ShortFunctionToProcedure<T> implements Procedure2<T, ShortProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final ShortFunction<? super T> function;

        private ShortFunctionToProcedure(ShortFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, ShortProcedure procedure)
        {
            procedure.value(this.function.shortValueOf(each));
        }
    }
}
