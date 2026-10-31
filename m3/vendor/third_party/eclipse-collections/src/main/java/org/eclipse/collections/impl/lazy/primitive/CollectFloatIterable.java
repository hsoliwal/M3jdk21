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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
public class CollectFloatIterable<T>
        extends AbstractLazyFloatIterable
{
    private final LazyIterable<T> iterable;
    private final FloatFunction<? super T> function;
    private final FloatFunctionToProcedure<T> floatFunctionToProcedure;

    public CollectFloatIterable(LazyIterable<T> adapted, FloatFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.floatFunctionToProcedure = new FloatFunctionToProcedure<>(function);
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new FloatIterator()
        {
            private final Iterator<T> iterator = CollectFloatIterable.this.iterable.iterator();

            @Override
            public float next()
            {
                return CollectFloatIterable.this.function.floatValueOf(this.iterator.next());
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
    public void each(FloatProcedure procedure)
    {
        this.iterable.forEachWith(this.floatFunctionToProcedure, procedure);
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
    public int count(final FloatPredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.floatValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final FloatPredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.floatValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final FloatPredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.floatValueOf(each)));
    }

    @Override
    public float[] toArray()
    {
        final float[] array = new float[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.floatValueOf(each));
        return array;
    }

    @Override
    public float[] toSortedArray()
    {
        float[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatLists.mutable.withAll(this);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatSets.mutable.withAll(this);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatBags.mutable.withAll(this);
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    private static final class FloatFunctionToProcedure<T> implements Procedure2<T, FloatProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final FloatFunction<? super T> function;

        private FloatFunctionToProcedure(FloatFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, FloatProcedure procedure)
        {
            procedure.value(this.function.floatValueOf(each));
        }
    }
}
