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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.FloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.iterator.SingletonFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.FloatIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.impl.factory.primitive.FloatSets;

/**
 * ImmutableFloatSingletonBag is an optimization for {@link ImmutableFloatBag} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatSingletonBag implements ImmutableFloatBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final float element1;

    ImmutableFloatSingletonBag(float element)
    {
        this.element1 = element;
    }

    @Override
    public ImmutableFloatBag newWith(float element)
    {
        return FloatBags.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableFloatBag newWithout(float element)
    {
        return Float.compare(this.element1, element) == 0 ? FloatBags.immutable.with() : this;
    }

    @Override
    public ImmutableFloatBag newWithAll(FloatIterable elements)
    {
        return FloatHashBag.newBag(elements).with(this.element1).toImmutable();
    }

    @Override
    public ImmutableFloatBag newWithoutAll(FloatIterable elements)
    {
        return elements.contains(this.element1) ? FloatBags.immutable.with() : this;
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
    public boolean contains(float value)
    {
        return Float.compare(this.element1, value) == 0;
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

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? FloatBags.immutable.with(this.element1)
            : FloatBags.immutable.empty();
    }

    @Override
    public ImmutableFloatBag selectByOccurrences(IntPredicate predicate)
    {
        return predicate.accept(1)
            ? FloatBags.immutable.with(this.element1)
            : FloatBags.immutable.empty();
    }

    @Override
    public ImmutableFloatSet selectUnique()
    {
        return FloatSets.immutable.of(this.element1);
    }

    @Override
    public ImmutableList<FloatIntPair> topOccurrences(int count)
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
    public ImmutableList<FloatIntPair> bottomOccurrences(int count)
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
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? FloatBags.immutable.empty()
            : FloatBags.immutable.with(this.element1);
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Bags.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newListWith(this.element1);
    }

    @Override
    public int sizeDistinct()
    {
        return 1;
    }

    @Override
    public int occurrencesOf(float item)
    {
        return Float.compare(this.element1, item) == 0 ? 1 : 0;
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure floatIntProcedure)
    {
        floatIntProcedure.value(this.element1, 1);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
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
    public double sum()
    {
        return this.element1;
    }

    @Override
    public float min()
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
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof FloatBag))
        {
            return false;
        }
        FloatBag bag = (FloatBag) obj;
        if (bag.size() != 1)
        {
            return false;
        }
        return this.occurrencesOf(this.element1) == bag.occurrencesOf(this.element1);
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(this.element1) ^ 1;
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
    public ImmutableFloatBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
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
    public FloatIterator floatIterator()
    {
        return new SingletonFloatIterator(element1);
    }
}
