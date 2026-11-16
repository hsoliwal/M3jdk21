/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableCharList;
import jdk.m3.collections.eclipse.api.list.primitive.CharList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableCharStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharCharPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.CharHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharStacks;
import jdk.m3.collections.eclipse.impl.iterator.SingletonCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseCharIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableCharSingletonList is an optimization for {@link ImmutableCharList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableCharSingletonList implements ImmutableCharList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final char element1;

    ImmutableCharSingletonList(char element)
    {
        this.element1 = element;
    }

    @Override
    public char get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public char getFirst()
    {
        return this.element1;
    }

    @Override
    public char getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(char value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public int lastIndexOf(char value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public CharIterator charIterator()
    {
        return new SingletonCharIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableCharList select(CharPredicate predicate)
    {
        return predicate.accept(this.element1) ? CharArrayList.newListWith(this.element1).toImmutable()
                : new CharArrayList().toImmutable();
    }

    @Override
    public ImmutableCharList reject(CharPredicate predicate)
    {
        return predicate.accept(this.element1) ? new CharArrayList().toImmutable()
                : CharArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public char max()
    {
        return this.element1;
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return this.element1;
    }

    @Override
    public char min()
    {
        return this.element1;
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public char[] toSortedArray()
    {
        return new char[]{this.element1};
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(char value)
    {
        if (this.element1 == value)
        {
            return 0;
        }
        if (this.element1 < value)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public long dotProduct(CharList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return (long) this.element1 * list.getFirst();
    }

    @Override
    public char[] toArray()
    {
        return new char[]{this.element1};
    }

    @Override
    public char[] toArray(char[] target)
    {
        if (target.length < 1)
        {
            target = new char[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(char value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(char... source)
    {
        for (char value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        for (CharIterator iterator = source.charIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyCharIterable asReversed()
    {
        return ReverseCharIterable.adapt(this);
    }

    @Override
    public MutableCharList toList()
    {
        return CharArrayList.newListWith(this.element1);
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public ImmutableCharList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableCharSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableCharList newWith(char element)
    {
        return CharLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableCharList newWithout(char element)
    {
        return this.element1 == element ? CharLists.immutable.with() : this;
    }

    @Override
    public ImmutableCharList newWithAll(CharIterable elements)
    {
        CharArrayList arrayList = CharArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableCharList newWithoutAll(CharIterable elements)
    {
        return elements.contains(this.element1) ? CharLists.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(this);
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof CharList))
        {
            return false;
        }
        CharList list = (CharList) otherList;
        if (list.size() != 1)
        {
            return false;
        }
        return this.element1 == list.get(0);
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) this.element1;
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
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
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.element1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public ImmutableCharList distinct()
    {
        return this;
    }

    @Override
    public ImmutableCharList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<CharCharPair> zipChar(CharIterable iterable)
    {
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.charIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<CharObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public MutableCharStack toStack()
    {
        return CharStacks.mutable.with(this.element1);
    }
}
