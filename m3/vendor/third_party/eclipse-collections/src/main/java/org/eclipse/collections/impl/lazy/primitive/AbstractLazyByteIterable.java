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

import org.eclipse.collections.api.ByteIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.factory.primitive.ByteBags;
import org.eclipse.collections.api.factory.primitive.ByteLists;
import org.eclipse.collections.api.factory.primitive.ByteSets;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.utility.internal.primitive.ByteIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyByteIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyByteIterable implements LazyByteIterable
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
        return ByteIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return ByteIterableIterate.notEmpty(this);
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
        ByteIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(byte value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.containsAll(ByteSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.allSatisfy((byte value) -> AbstractLazyByteIterable.this.contains(value));
    }

    @Override
    public LazyByteIterable select(BytePredicate predicate)
    {
        return LazyByteIterate.select(this, predicate);
    }

    @Override
    public LazyByteIterable reject(BytePredicate predicate)
    {
        return LazyByteIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyByteIterable tap(ByteProcedure procedure)
    {
        return LazyByteIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return LazyByteIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(ByteToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyByteIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(ByteToBooleanFunction function)
    {
        return new CollectByteToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(ByteToByteFunction function)
    {
        return new CollectByteToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(ByteToCharFunction function)
    {
        return new CollectByteToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(ByteToShortFunction function)
    {
        return new CollectByteToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(ByteToIntFunction function)
    {
        return new CollectByteToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(ByteToFloatFunction function)
    {
        return new CollectByteToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(ByteToLongFunction function)
    {
        return new CollectByteToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(ByteToDoubleFunction function)
    {
        return new CollectByteToDoubleIterable(this, function);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return ByteIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return ByteIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return ByteIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return ByteIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return ByteIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return ByteIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return new ChunkByteIterable(this, size);
    }

    @Override
    public byte[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableByteList toList()
    {
        final MutableByteList list = ByteLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableByteSet toSet()
    {
        final MutableByteSet set = ByteSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableByteBag toBag()
    {
        final MutableByteBag bag = ByteBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public long sum()
    {
        ByteSumProcedure procedure = new ByteSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ByteMaxProcedure procedure = new ByteMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public byte maxIfEmpty(byte ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public byte min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ByteMinProcedure procedure = new ByteMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public byte minIfEmpty(byte ifEmpty)
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
        byte[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            byte first = sortedArray[middleIndex];
            byte second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteLists.mutable.withAll(this).sortThis();
    }

    private static final class ByteMaxProcedure implements ByteProcedure
    {
        private boolean visitedOnce;
        private byte max;

        @Override
        public void value(byte each)
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

        public byte getValue()
        {
            return this.max;
        }
    }

    private static final class ByteMinProcedure implements ByteProcedure
    {
        private boolean visitedOnce;
        private byte min;

        @Override
        public void value(byte each)
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

        public byte getValue()
        {
            return this.min;
        }
    }

    private static final class ByteSumProcedure implements ByteProcedure
    {
        private byte sum = 0;

        @Override
        public void value(byte each)
        {
            this.sum += each;
        }

        public byte getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return this;
    }
}
