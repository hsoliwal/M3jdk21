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

import org.eclipse.collections.api.LongIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.LongToByteFunction;
import org.eclipse.collections.api.block.function.primitive.LongToCharFunction;
import org.eclipse.collections.api.block.function.primitive.LongToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.utility.internal.primitive.LongIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyLongIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyLongIterable implements LazyLongIterable
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
        return LongIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return LongIterableIterate.notEmpty(this);
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
        LongIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(long value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.containsAll(LongSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.allSatisfy((long value) -> AbstractLazyLongIterable.this.contains(value));
    }

    @Override
    public LazyLongIterable select(LongPredicate predicate)
    {
        return LazyLongIterate.select(this, predicate);
    }

    @Override
    public LazyLongIterable reject(LongPredicate predicate)
    {
        return LazyLongIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyLongIterable tap(LongProcedure procedure)
    {
        return LazyLongIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(LongToObjectFunction<? extends V> function)
    {
        return LazyLongIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(LongToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyLongIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(LongToBooleanFunction function)
    {
        return new CollectLongToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(LongToByteFunction function)
    {
        return new CollectLongToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(LongToCharFunction function)
    {
        return new CollectLongToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(LongToShortFunction function)
    {
        return new CollectLongToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(LongToIntFunction function)
    {
        return new CollectLongToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(LongToFloatFunction function)
    {
        return new CollectLongToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(LongToLongFunction function)
    {
        return new CollectLongToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(LongToDoubleFunction function)
    {
        return new CollectLongToDoubleIterable(this, function);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return LongIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return LongIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return LongIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return LongIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return LongIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return LongIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return new ChunkLongIterable(this, size);
    }

    @Override
    public long[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableLongList toList()
    {
        final MutableLongList list = LongLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableLongSet toSet()
    {
        final MutableLongSet set = LongSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableLongBag toBag()
    {
        final MutableLongBag bag = LongBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public long sum()
    {
        LongSumProcedure procedure = new LongSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        LongMaxProcedure procedure = new LongMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public long maxIfEmpty(long ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public long min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        LongMinProcedure procedure = new LongMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public long minIfEmpty(long ifEmpty)
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
        long[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            long first = sortedArray[middleIndex];
            long second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public long[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongLists.mutable.withAll(this).sortThis();
    }

    private static final class LongMaxProcedure implements LongProcedure
    {
        private boolean visitedOnce;
        private long max;

        @Override
        public void value(long each)
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

        public long getValue()
        {
            return this.max;
        }
    }

    private static final class LongMinProcedure implements LongProcedure
    {
        private boolean visitedOnce;
        private long min;

        @Override
        public void value(long each)
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

        public long getValue()
        {
            return this.min;
        }
    }

    private static final class LongSumProcedure implements LongProcedure
    {
        private long sum = 0;

        @Override
        public void value(long each)
        {
            this.sum += each;
        }

        public long getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return this;
    }
}
