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

import org.eclipse.collections.api.IntIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.IntToByteFunction;
import org.eclipse.collections.api.block.function.primitive.IntToCharFunction;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.IntToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.factory.primitive.IntBags;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.utility.internal.primitive.IntIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyIntIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyIntIterable implements LazyIntIterable
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
        return IntIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return IntIterableIterate.notEmpty(this);
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
        IntIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(int value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.containsAll(IntSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return source.allSatisfy((int value) -> AbstractLazyIntIterable.this.contains(value));
    }

    @Override
    public LazyIntIterable select(IntPredicate predicate)
    {
        return LazyIntIterate.select(this, predicate);
    }

    @Override
    public LazyIntIterable reject(IntPredicate predicate)
    {
        return LazyIntIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyIntIterable tap(IntProcedure procedure)
    {
        return LazyIntIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(IntToObjectFunction<? extends V> function)
    {
        return LazyIntIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(IntToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyIntIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(IntToBooleanFunction function)
    {
        return new CollectIntToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(IntToByteFunction function)
    {
        return new CollectIntToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(IntToCharFunction function)
    {
        return new CollectIntToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(IntToShortFunction function)
    {
        return new CollectIntToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(IntToIntFunction function)
    {
        return new CollectIntToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(IntToFloatFunction function)
    {
        return new CollectIntToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(IntToLongFunction function)
    {
        return new CollectIntToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(IntToDoubleFunction function)
    {
        return new CollectIntToDoubleIterable(this, function);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return IntIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return IntIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return IntIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return IntIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return IntIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return IntIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return new ChunkIntIterable(this, size);
    }

    @Override
    public int[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableIntList toList()
    {
        final MutableIntList list = IntLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableIntSet toSet()
    {
        final MutableIntSet set = IntSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableIntBag toBag()
    {
        final MutableIntBag bag = IntBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public long sum()
    {
        IntSumProcedure procedure = new IntSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public int max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        IntMaxProcedure procedure = new IntMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public int maxIfEmpty(int ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public int min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        IntMinProcedure procedure = new IntMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public int minIfEmpty(int ifEmpty)
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
        int[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            int first = sortedArray[middleIndex];
            int second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public int[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntLists.mutable.withAll(this).sortThis();
    }

    private static final class IntMaxProcedure implements IntProcedure
    {
        private boolean visitedOnce;
        private int max;

        @Override
        public void value(int each)
        {
            if (this.visitedOnce)
            {
                if (this.max < each)
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

        public int getValue()
        {
            return this.max;
        }
    }

    private static final class IntMinProcedure implements IntProcedure
    {
        private boolean visitedOnce;
        private int min;

        @Override
        public void value(int each)
        {
            if (this.visitedOnce)
            {
                if (each < this.min)
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

        public int getValue()
        {
            return this.min;
        }
    }

    private static final class IntSumProcedure implements IntProcedure
    {
        private int sum = 0;

        @Override
        public void value(int each)
        {
            this.sum += each;
        }

        public int getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return this;
    }
}
