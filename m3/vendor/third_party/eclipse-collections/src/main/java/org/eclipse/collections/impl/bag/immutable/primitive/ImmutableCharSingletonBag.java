/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.CharBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.iterator.SingletonCharIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.impl.factory.primitive.CharSets;

/**
 * ImmutableCharSingletonBag is an optimization for {@link ImmutableCharBag} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharSingletonBag implements ImmutableCharBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final char element1;

    ImmutableCharSingletonBag(char element)
    {
        this.element1 = element;
    }

    @Override
    public ImmutableCharBag newWith(char element)
    {
        return CharBags.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableCharBag newWithout(char element)
    {
        return this.element1 == element ? CharBags.immutable.with() : this;
    }

    @Override
    public ImmutableCharBag newWithAll(CharIterable elements)
    {
        return CharHashBag.newBag(elements).with(this.element1).toImmutable();
    }

    @Override
    public ImmutableCharBag newWithoutAll(CharIterable elements)
    {
        return elements.contains(this.element1) ? CharBags.immutable.with() : this;
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
    public boolean contains(char value)
    {
        return this.element1 == value;
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

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public ImmutableCharBag select(CharPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? CharBags.immutable.with(this.element1)
            : CharBags.immutable.empty();
    }

    @Override
    public ImmutableCharBag selectByOccurrences(IntPredicate predicate)
    {
        return predicate.accept(1)
            ? CharBags.immutable.with(this.element1)
            : CharBags.immutable.empty();
    }

    @Override
    public ImmutableCharSet selectUnique()
    {
        return CharSets.immutable.of(this.element1);
    }

    @Override
    public ImmutableList<CharIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableList<CharIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableCharBag reject(CharPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? CharBags.immutable.empty()
            : CharBags.immutable.with(this.element1);
    }

    @Override
    public <V> ImmutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Bags.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public MutableCharList toList()
    {
        return CharArrayList.newListWith(this.element1);
    }

    @Override
    public int sizeDistinct()
    {
        return 1;
    }

    @Override
    public int occurrencesOf(char item)
    {
        return this.element1 == item ? 1 : 0;
    }

    @Override
    public void forEachWithOccurrences(CharIntProcedure charIntProcedure)
    {
        charIntProcedure.value(this.element1, 1);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
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
    public long sum()
    {
        return this.element1;
    }

    @Override
    public char min()
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
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof CharBag))
        {
            return false;
        }
        CharBag bag = (CharBag) obj;
        if (bag.size() != 1)
        {
            return false;
        }
        return this.occurrencesOf(this.element1) == bag.occurrencesOf(this.element1);
    }

    @Override
    public int hashCode()
    {
        return (int) this.element1 ^ 1;
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
    public ImmutableCharBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
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
    public String toString()
    {
        return '[' + this.makeString() + ']';
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

    @Override
    public CharIterator charIterator()
    {
        return new SingletonCharIterator(element1);
    }
}
