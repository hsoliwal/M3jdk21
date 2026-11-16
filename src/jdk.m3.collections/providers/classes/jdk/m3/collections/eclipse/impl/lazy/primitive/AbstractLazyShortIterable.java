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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToBooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToByteFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToCharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToFloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToIntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToLongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToShortFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortSets;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.utility.internal.primitive.ShortIterableIterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LazyShortIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
@SuppressWarnings({"missing-explicit-ctor", "serial"})
public abstract class AbstractLazyShortIterable implements LazyShortIterable
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
        return ShortIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return ShortIterableIterate.notEmpty(this);
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
        ShortIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(short value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.containsAll(ShortSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return source.allSatisfy((short value) -> AbstractLazyShortIterable.this.contains(value));
    }

    @Override
    public LazyShortIterable select(ShortPredicate predicate)
    {
        return LazyShortIterate.select(this, predicate);
    }

    @Override
    public LazyShortIterable reject(ShortPredicate predicate)
    {
        return LazyShortIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyShortIterable tap(ShortProcedure procedure)
    {
        return LazyShortIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return LazyShortIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(ShortToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyShortIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(ShortToBooleanFunction function)
    {
        return new CollectShortToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(ShortToByteFunction function)
    {
        return new CollectShortToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(ShortToCharFunction function)
    {
        return new CollectShortToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(ShortToShortFunction function)
    {
        return new CollectShortToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(ShortToIntFunction function)
    {
        return new CollectShortToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(ShortToFloatFunction function)
    {
        return new CollectShortToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(ShortToLongFunction function)
    {
        return new CollectShortToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(ShortToDoubleFunction function)
    {
        return new CollectShortToDoubleIterable(this, function);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return ShortIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return ShortIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return ShortIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return ShortIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return ShortIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return ShortIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        return new ChunkShortIterable(this, size);
    }

    @Override
    public short[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableShortList toList()
    {
        final MutableShortList list = ShortLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableShortSet toSet()
    {
        final MutableShortSet set = ShortSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableShortBag toBag()
    {
        final MutableShortBag bag = ShortBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public long sum()
    {
        ShortSumProcedure procedure = new ShortSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public short max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ShortMaxProcedure procedure = new ShortMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public short maxIfEmpty(short ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public short min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ShortMinProcedure procedure = new ShortMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public short minIfEmpty(short ifEmpty)
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
        short[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            short first = sortedArray[middleIndex];
            short second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public short[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortLists.mutable.withAll(this).sortThis();
    }

    private static final class ShortMaxProcedure implements ShortProcedure
    {
        private boolean visitedOnce;
        private short max;

        @Override
        public void value(short each)
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

        public short getValue()
        {
            return this.max;
        }
    }

    private static final class ShortMinProcedure implements ShortProcedure
    {
        private boolean visitedOnce;
        private short min;

        @Override
        public void value(short each)
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

        public short getValue()
        {
            return this.min;
        }
    }

    private static final class ShortSumProcedure implements ShortProcedure
    {
        private short sum = 0;

        @Override
        public void value(short each)
        {
            this.sum += each;
        }

        public short getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return this;
    }
}
