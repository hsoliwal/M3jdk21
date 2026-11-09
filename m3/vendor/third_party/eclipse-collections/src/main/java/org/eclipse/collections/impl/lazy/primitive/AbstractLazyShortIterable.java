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

import org.eclipse.collections.api.ShortIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.utility.internal.primitive.ShortIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyShortIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
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
