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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.BooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableBooleanStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanBooleanPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.BooleanHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanStacks;
import jdk.m3.collections.eclipse.impl.iterator.SingletonBooleanIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseBooleanIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.BooleanArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.BooleanHashSet;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableBooleanSingletonList is an optimization for {@link ImmutableBooleanList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableBooleanSingletonList implements ImmutableBooleanList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final boolean element1;

    ImmutableBooleanSingletonList(boolean element)
    {
        this.element1 = element;
    }

    @Override
    public boolean get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public boolean getFirst()
    {
        return this.element1;
    }

    @Override
    public boolean getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(boolean value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public int lastIndexOf(boolean value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new SingletonBooleanIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableBooleanList select(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? BooleanArrayList.newListWith(this.element1).toImmutable()
                : new BooleanArrayList().toImmutable();
    }

    @Override
    public ImmutableBooleanList reject(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? new BooleanArrayList().toImmutable()
                : BooleanArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[]{this.element1};
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        if (target.length < 1)
        {
            target = new boolean[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        for (boolean value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        for (BooleanIterator iterator = source.booleanIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyBooleanIterable asReversed()
    {
        return ReverseBooleanIterable.adapt(this);
    }

    @Override
    public MutableBooleanList toList()
    {
        return BooleanArrayList.newListWith(this.element1);
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return BooleanHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return BooleanHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public ImmutableBooleanList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableBooleanSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableBooleanList newWith(boolean element)
    {
        return BooleanLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableBooleanList newWithout(boolean element)
    {
        return this.element1 == element ? BooleanLists.immutable.with() : this;
    }

    @Override
    public ImmutableBooleanList newWithAll(BooleanIterable elements)
    {
        BooleanArrayList arrayList = BooleanArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableBooleanList newWithoutAll(BooleanIterable elements)
    {
        return elements.contains(this.element1) ? BooleanLists.immutable.with() : this;
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
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
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
        if (!(otherList instanceof BooleanList))
        {
            return false;
        }
        BooleanList list = (BooleanList) otherList;
        if (list.size() != 1)
        {
            return false;
        }
        return this.element1 == list.get(0);
    }

    @Override
    public int hashCode()
    {
        return 31 + (this.element1 ? 1231 : 1237);
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
    public ImmutableBooleanList distinct()
    {
        return this;
    }

    @Override
    public ImmutableBooleanList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<BooleanBooleanPair> zipBoolean(BooleanIterable iterable)
    {
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.booleanIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<BooleanObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public MutableBooleanStack toStack()
    {
        return BooleanStacks.mutable.with(this.element1);
    }
}
