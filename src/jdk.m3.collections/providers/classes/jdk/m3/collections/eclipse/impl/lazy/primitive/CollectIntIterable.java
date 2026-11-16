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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure2;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.factory.primitive.IntBags;
import jdk.m3.collections.eclipse.api.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.api.factory.primitive.IntSets;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
@SuppressWarnings("deprecation")
public class CollectIntIterable<T>
        extends AbstractLazyIntIterable
{
    private final RichIterable<T> iterable;
    private final IntFunction<? super T> function;
    private final IntFunctionToProcedure<T> intFunctionToProcedure;

    public CollectIntIterable(RichIterable<T> adapted, IntFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.intFunctionToProcedure = new IntFunctionToProcedure<>(function);
    }

    @Override
    public IntIterator intIterator()
    {
        return new IntIterator()
        {
            private final Iterator<T> iterator = CollectIntIterable.this.iterable.iterator();

            @Override
            public int next()
            {
                return CollectIntIterable.this.function.intValueOf(this.iterator.next());
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
    public void each(IntProcedure procedure)
    {
        this.iterable.forEachWith(this.intFunctionToProcedure, procedure);
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
    public int count(final IntPredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.intValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final IntPredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.intValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final IntPredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.intValueOf(each)));
    }

    @Override
    public int[] toArray()
    {
        final int[] array = new int[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.intValueOf(each));
        return array;
    }

    @Override
    public int[] toSortedArray()
    {
        int[] array = this.toArray();
        Arrays.sort(array);
        return array;
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

    private static final class IntFunctionToProcedure<T> implements Procedure2<T, IntProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final IntFunction<? super T> function;

        private IntFunctionToProcedure(IntFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, IntProcedure procedure)
        {
            procedure.value(this.function.intValueOf(each));
        }
    }
}
