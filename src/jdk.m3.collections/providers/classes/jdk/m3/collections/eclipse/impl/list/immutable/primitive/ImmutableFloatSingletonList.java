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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.FloatList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableFloatStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.FloatHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatStacks;
import jdk.m3.collections.eclipse.impl.iterator.SingletonFloatIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseFloatIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableFloatSingletonList is an optimization for {@link ImmutableFloatList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableFloatSingletonList implements ImmutableFloatList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final float element1;

    ImmutableFloatSingletonList(float element)
    {
        this.element1 = element;
    }

    @Override
    public float get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public float getFirst()
    {
        return this.element1;
    }

    @Override
    public float getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(float value)
    {
        return Float.compare(this.element1, value) == 0 ? 0 : -1;
    }

    @Override
    public int lastIndexOf(float value)
    {
        return Float.compare(this.element1, value) == 0 ? 0 : -1;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new SingletonFloatIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableFloatList select(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? FloatArrayList.newListWith(this.element1).toImmutable()
                : new FloatArrayList().toImmutable();
    }

    @Override
    public ImmutableFloatList reject(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? new FloatArrayList().toImmutable()
                : FloatArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public double sum()
    {
        return this.element1;
    }

    @Override
    public float max()
    {
        return this.element1;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.element1;
    }

    @Override
    public float min()
    {
        return this.element1;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float[] toSortedArray()
    {
        return new float[]{this.element1};
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(float value)
    {
        if (Float.compare(this.element1, value) == 0)
        {
            return 0;
        }
        if (Float.compare(this.element1, value) < 0)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public double dotProduct(FloatList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return (double) this.element1 * list.getFirst();
    }

    @Override
    public float[] toArray()
    {
        return new float[]{this.element1};
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < 1)
        {
            target = new float[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(float value)
    {
        return Float.compare(this.element1, value) == 0;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (Float.compare(this.element1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (Float.compare(this.element1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newListWith(this.element1);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableFloatSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableFloatList newWith(float element)
    {
        return FloatLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableFloatList newWithout(float element)
    {
        return Float.compare(this.element1, element) == 0 ? FloatLists.immutable.with() : this;
    }

    @Override
    public ImmutableFloatList newWithAll(FloatIterable elements)
    {
        FloatArrayList arrayList = FloatArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableFloatList newWithoutAll(FloatIterable elements)
    {
        return elements.contains(this.element1) ? FloatLists.immutable.with() : this;
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
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
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
        if (!(otherList instanceof FloatList))
        {
            return false;
        }
        FloatList list = (FloatList) otherList;
        if (list.size() != 1)
        {
            return false;
        }
        return Float.compare(this.element1, list.get(0)) == 0;
    }

    @Override
    public int hashCode()
    {
        return 31 + Float.floatToIntBits(this.element1);
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
    public ImmutableFloatList distinct()
    {
        return this;
    }

    @Override
    public ImmutableFloatList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.floatIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public MutableFloatStack toStack()
    {
        return FloatStacks.mutable.with(this.element1);
    }
}
