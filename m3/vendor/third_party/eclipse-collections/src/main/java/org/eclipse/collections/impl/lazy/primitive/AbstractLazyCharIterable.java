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

import org.eclipse.collections.api.CharIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.CharToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.CharToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.CharToIntFunction;
import org.eclipse.collections.api.block.function.primitive.CharToLongFunction;
import org.eclipse.collections.api.block.function.primitive.CharToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.utility.internal.primitive.CharIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyCharIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyCharIterable implements LazyCharIterable
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
        return CharIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return CharIterableIterate.notEmpty(this);
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
        CharIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(char value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        return this.containsAll(CharSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return source.allSatisfy((char value) -> AbstractLazyCharIterable.this.contains(value));
    }

    @Override
    public LazyCharIterable select(CharPredicate predicate)
    {
        return LazyCharIterate.select(this, predicate);
    }

    @Override
    public LazyCharIterable reject(CharPredicate predicate)
    {
        return LazyCharIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyCharIterable tap(CharProcedure procedure)
    {
        return LazyCharIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(CharToObjectFunction<? extends V> function)
    {
        return LazyCharIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(CharToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyCharIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(CharToBooleanFunction function)
    {
        return new CollectCharToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(CharToByteFunction function)
    {
        return new CollectCharToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(CharToCharFunction function)
    {
        return new CollectCharToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(CharToShortFunction function)
    {
        return new CollectCharToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(CharToIntFunction function)
    {
        return new CollectCharToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(CharToFloatFunction function)
    {
        return new CollectCharToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(CharToLongFunction function)
    {
        return new CollectCharToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(CharToDoubleFunction function)
    {
        return new CollectCharToDoubleIterable(this, function);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return CharIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return CharIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return CharIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return CharIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return CharIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return CharIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        return new ChunkCharIterable(this, size);
    }

    @Override
    public char[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableCharList toList()
    {
        final MutableCharList list = CharLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableCharSet toSet()
    {
        final MutableCharSet set = CharSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableCharBag toBag()
    {
        final MutableCharBag bag = CharBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public long sum()
    {
        CharSumProcedure procedure = new CharSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        CharMaxProcedure procedure = new CharMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public char maxIfEmpty(char ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public char min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        CharMinProcedure procedure = new CharMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public char minIfEmpty(char ifEmpty)
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
        char[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            char first = sortedArray[middleIndex];
            char second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public char[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharLists.mutable.withAll(this).sortThis();
    }

    private static final class CharMaxProcedure implements CharProcedure
    {
        private boolean visitedOnce;
        private char max;

        @Override
        public void value(char each)
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

        public char getValue()
        {
            return this.max;
        }
    }

    private static final class CharMinProcedure implements CharProcedure
    {
        private boolean visitedOnce;
        private char min;

        @Override
        public void value(char each)
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

        public char getValue()
        {
            return this.min;
        }
    }

    private static final class CharSumProcedure implements CharProcedure
    {
        private char sum = 0;

        @Override
        public void value(char each)
        {
            this.sum += each;
        }

        public char getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return this;
    }
}
