/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.impl.iterator.SingletonCharIterator;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;

/**
 * ImmutableCharSingletonSet is an optimization for {@link ImmutableCharSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharSingletonSet implements ImmutableCharSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final char element;

    ImmutableCharSingletonSet(char element)
    {
        this.element = element;
    }

    @Override
    public ImmutableCharSet newWith(char element)
    {
        return CharSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableCharSet newWithout(char element)
    {
        return this.element == element ? CharSets.immutable.with() : this;
    }

    @Override
    public ImmutableCharSet newWithAll(CharIterable elements)
    {
        return CharHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableCharSet newWithoutAll(CharIterable elements)
    {
        return elements.contains(this.element) ? CharSets.immutable.with() : this;
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
        return this.element == value;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        for (CharIterator iterator = source.charIterator(); iterator.hasNext(); )
        {
            if (this.element != iterator.next())
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
            if (this.element != value)
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
        procedure.value(this.element);
    }

    @Override
    public ImmutableCharSet select(CharPredicate predicate)
    {
        return predicate.accept(this.element) ? CharHashSet.newSetWith(this.element).toImmutable()
                : new CharHashSet().toImmutable();
    }

    @Override
    public ImmutableCharSet reject(CharPredicate predicate)
    {
        return predicate.accept(this.element) ? new CharHashSet().toImmutable()
                : CharHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableCharList toList()
    {
        return CharLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(char item)
    {
        return this.element == item ? 1 : 0;
    }

    public void forEachWithOccurrences(CharIntProcedure charIntProcedure)
    {
        charIntProcedure.value(this.element, 1);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public long sum()
    {
        return this.element;
    }

    @Override
    public char min()
    {
        return this.element;
    }

    @Override
    public char max()
    {
        return this.element;
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return this.element;
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        return this.element;
    }

    @Override
    public double average()
    {
        return this.element;
    }

    @Override
    public double median()
    {
        return this.element;
    }

    @Override
    public LazyIterable<CharCharPair> cartesianProduct(CharSet set)
    {
        return CharSets.cartesianProduct(this, set);
    }

    @Override
    public char[] toSortedArray()
    {
        return new char[]{this.element};
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
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
        if (!(obj instanceof CharSet))
        {
            return false;
        }
        CharSet set = (CharSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return (int) this.element;
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharSets.mutable.with(this.element);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharBags.mutable.with(this.element);
    }

    @Override
    public CharSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableCharSet toImmutable()
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
        return new char[]{this.element};
    }

    @Override
    public char[] toArray(char[] target)
    {
        if (target.length < 1)
        {
            target = new char[]{this.element};
        }
        else
        {
            target[0] = this.element;
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
            appendable.append(String.valueOf(this.element));
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
        return new SingletonCharIterator(element);
    }
}
