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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToByteFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToCharFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToIntFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToLongFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.utility.internal.primitive.FloatIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyFloatIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyFloatIterable implements LazyFloatIterable
{
    @Override
    public int size()
    {
        return this.count(value -> true);
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public boolean isEmpty()
    {
        return FloatIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return FloatIterableIterate.notEmpty(this);
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        FloatIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(float value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.containsAll(FloatSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return source.allSatisfy((float value) -> AbstractLazyFloatIterable.this.contains(value));
    }

    @Override
    public LazyFloatIterable select(FloatPredicate predicate)
    {
        return LazyFloatIterate.select(this, predicate);
    }

    @Override
    public LazyFloatIterable reject(FloatPredicate predicate)
    {
        return LazyFloatIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyFloatIterable tap(FloatProcedure procedure)
    {
        return LazyFloatIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return LazyFloatIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(FloatToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyFloatIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(FloatToBooleanFunction function)
    {
        return new CollectFloatToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(FloatToByteFunction function)
    {
        return new CollectFloatToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(FloatToCharFunction function)
    {
        return new CollectFloatToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(FloatToShortFunction function)
    {
        return new CollectFloatToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(FloatToIntFunction function)
    {
        return new CollectFloatToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(FloatToFloatFunction function)
    {
        return new CollectFloatToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(FloatToLongFunction function)
    {
        return new CollectFloatToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(FloatToDoubleFunction function)
    {
        return new CollectFloatToDoubleIterable(this, function);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return FloatIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return FloatIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return FloatIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return FloatIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return FloatIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return FloatIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        return new ChunkFloatIterable(this, size);
    }

    @Override
    public float[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableFloatList toList()
    {
        final MutableFloatList list = FloatLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableFloatSet toSet()
    {
        final MutableFloatSet set = FloatSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableFloatBag toBag()
    {
        final MutableFloatBag bag = FloatBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public double sum()
    {
        FloatSumProcedure procedure = new FloatSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public float max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        FloatMaxProcedure procedure = new FloatMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public float maxIfEmpty(float ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public float min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        FloatMinProcedure procedure = new FloatMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public float minIfEmpty(float ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.min();
    }

    @Override
    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        float[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            float first = sortedArray[middleIndex];
            float second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public float[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatLists.mutable.withAll(this).sortThis();
    }

    private static final class FloatMaxProcedure implements FloatProcedure
    {
        private boolean visitedOnce;
        private float max;

        @Override
        public void value(float each)
        {
            if (this.visitedOnce)
            {
                if (Float.compare(this.max, each) < 0)
                {
                    this.max = each;
                }
            }
            else
            {
                this.max = each;
                this.visitedOnce = true;
            }
        }

        public float getValue()
        {
            return this.max;
        }
    }

    private static final class FloatMinProcedure implements FloatProcedure
    {
        private boolean visitedOnce;
        private float min;

        @Override
        public void value(float each)
        {
            if (this.visitedOnce)
            {
                if (Float.compare(each, this.min) < 0)
                {
                    this.min = each;
                }
            }
            else
            {
                this.min = each;
                this.visitedOnce = true;
            }
        }

        public float getValue()
        {
            return this.min;
        }
    }

    private static final class FloatSumProcedure implements FloatProcedure
    {
        private double sum = 0.0;
        private double compensation = 0.0;

        @Override
        public void value(float each)
        {
            double adjustedValue = each - this.compensation;
            double nextSum = this.sum + adjustedValue;
            this.compensation = nextSum - this.sum - adjustedValue;
            this.sum = nextSum;
        }

        public double getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return this;
    }
}
