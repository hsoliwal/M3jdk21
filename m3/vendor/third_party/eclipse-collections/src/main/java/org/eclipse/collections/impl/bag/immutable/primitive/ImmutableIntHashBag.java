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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.IntBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedIntIntProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;

/**
 * ImmutableIntHashBag is the non-modifiable equivalent of {@link IntHashBag}.
 * This file was automatically generated from template file immutablePrimitiveHashBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntHashBag implements ImmutableIntBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableIntBag delegate;

    private ImmutableIntHashBag(int[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = IntHashBag.newBagWith(newElements);
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use IntBags.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableIntHashBag newBagWith(int... elements)
    {
        return new ImmutableIntHashBag(elements);
    }

    @Override
    public ImmutableIntBag newWith(int element)
    {
        return IntHashBag.newBag(this.delegate).with(element).toImmutable();
    }

    @Override
    public ImmutableIntBag newWithout(int element)
    {
        IntHashBag hashBag = IntHashBag.newBag(this.delegate);
        hashBag.remove(element);
        return hashBag.toImmutable();
    }

    @Override
    public ImmutableIntBag newWithAll(IntIterable elements)
    {
        IntHashBag bag = IntHashBag.newBag(this.delegate);
        bag.addAll(elements);
        return bag.toImmutable();
    }

    @Override
    public ImmutableIntBag newWithoutAll(IntIterable elements)
    {
        IntHashBag bag = IntHashBag.newBag(this.delegate);
        bag.removeAll(elements);
        return bag.toImmutable();
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public boolean contains(int value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(int... elements)
    {
        return this.delegate.containsAll(elements);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public ImmutableIntBag select(IntPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableIntBag selectByOccurrences(IntPredicate predicate)
    {
        return this.delegate.selectByOccurrences(predicate).toImmutable();
    }

    @Override
    public ImmutableIntSet selectUnique()
    {
        MutableIntSet result = IntSets.mutable.empty();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (occurrences == 1)
            {
                result.add(each);
            }
        });
        return result.toImmutable();
    }

    @Override
    public ImmutableList<IntIntPair> topOccurrences(int count)
    {
        return this.delegate.topOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableList<IntIntPair> bottomOccurrences(int count)
    {
        return this.delegate.bottomOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableIntBag reject(IntPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public MutableIntList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public int sizeDistinct()
    {
        return this.delegate.sizeDistinct();
    }

    @Override
    public int occurrencesOf(int item)
    {
        return this.delegate.occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(IntIntProcedure intIntProcedure)
    {
        this.delegate.forEachWithOccurrences(intIntProcedure);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public int min()
    {
        return this.delegate.min();
    }

    @Override
    public int max()
    {
        return this.delegate.max();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.delegate.average();
    }

    @Override
    public double median()
    {
        return this.delegate.median();
    }

    @Override
    public int[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                IntIterator iterator = this.intIterator();
                while (iterator.hasNext())
                {
                    MutableIntBag batch = IntBags.mutable.empty();
                    for (int i = 0; i < size && iterator.hasNext(); i++)
                    {
                        batch.add(iterator.next());
                    }
                    result.add(batch.toImmutable());
                }
            }
        }
        return result.toImmutable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public MutableIntSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public ImmutableIntBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public int[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    @Override
    public IntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.delegate.intIterator());
    }

    private Object writeReplace()
    {
        return new ImmutableIntBagSerializationProxy(this);
    }

    protected static class ImmutableIntBagSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private IntBag bag;

        public ImmutableIntBagSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableIntBagSerializationProxy(IntBag bag)
        {
            this.bag = bag;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.bag.sizeDistinct());
            try
            {
                this.bag.forEachWithOccurrences(new CheckedIntIntProcedure()
                {
                    @Override
                    public void safeValue(int item, int count) throws IOException
                    {
                        out.writeInt(item);
                        out.writeInt(count);
                    }
                });
            }
            catch (RuntimeException e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            MutableIntBag deserializedBag = new IntHashBag();

            for (int i = 0; i < size; i++)
            {
                deserializedBag.addOccurrences(in.readInt(), in.readInt());
            }

            this.bag = deserializedBag;
        }

        protected Object readResolve()
        {
            return this.bag.toImmutable();
        }
    }
}
