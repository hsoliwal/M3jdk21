/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.FloatBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.map.mutable.primitive.FloatIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractFloatIterable;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.FloatSets;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * FloatHashBag is similar to {@link HashBag}, and is memory-optimized for float primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class FloatHashBag
        extends AbstractFloatIterable
        implements MutableFloatBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private FloatIntHashMap items;
    private int size;

    public FloatHashBag()
    {
        this.items = new FloatIntHashMap();
    }

    public FloatHashBag(int size)
    {
        this.items = new FloatIntHashMap(size);
    }

    public FloatHashBag(FloatIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public FloatHashBag(float... elements)
    {
        this();
        this.addAll(elements);
    }

    public FloatHashBag(FloatHashBag bag)
    {
        this.items = new FloatIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static FloatHashBag newBag(int size)
    {
        return new FloatHashBag(size);
    }

    public static FloatHashBag newBagWith(float... source)
    {
        return new FloatHashBag(source);
    }

    public static FloatHashBag newBag(FloatIterable source)
    {
        if (source instanceof FloatHashBag)
        {
            return new FloatHashBag((FloatHashBag) source);
        }
        return new FloatHashBag(source);
    }

    public static FloatHashBag newBag(FloatBag source)
    {
        return new FloatHashBag(source);
    }

    @Override
    public boolean isEmpty()
    {
        return this.items.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.items.notEmpty();
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public int sizeDistinct()
    {
        return this.items.size();
    }

    @Override
    public void clear()
    {
        this.items.clear();
        this.size = 0;
    }

    @Override
    public FloatHashBag with(float element)
    {
        this.add(element);
        return this;
    }

    public FloatHashBag with(float element1, float element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public FloatHashBag with(float element1, float element2, float element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public FloatHashBag withAll(FloatIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public FloatHashBag without(float element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public FloatHashBag withoutAll(FloatIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(float value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(float item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public FloatHashBag selectByOccurrences(IntPredicate predicate)
    {
        FloatHashBag result = new FloatHashBag();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (predicate.accept(occurrences))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public MutableFloatSet selectUnique()
    {
        MutableFloatSet result = FloatSets.mutable.empty();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (occurrences == 1)
            {
                result.add(each);
            }
        });
        return result;
    }

    @Override
    public MutableList<FloatIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<FloatIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, FloatIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<FloatIntPair> occurrencesSortingBy(
            int n,
            IntFunction<FloatIntPair> function,
            MutableList<FloatIntPair> returnWhenEmpty)
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of n < 0");
        }
        if (n == 0)
        {
            return returnWhenEmpty;
        }
        int keySize = Math.min(n, this.sizeDistinct());
        MutableList<FloatIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<FloatIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<FloatIntPair> toListWithOccurrences()
    {
        MutableList<FloatIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(float item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(float item)
    {
        int newValue = this.items.updateValue(item, 0, IntToIntFunctions.decrement());
        if (newValue <= 0)
        {
            this.items.removeKey(item);
            if (newValue == 0)
            {
                this.size--;
            }
            return newValue == 0;
        }
        this.size--;
        return true;
    }

    @Override
    public boolean removeIf(FloatPredicate predicate)
    {
        boolean changed = false;
        for (MutableFloatIterator iterator = this.items.keySet().floatIterator(); iterator.hasNext(); )
        {
            float key = iterator.next();
            if (predicate.accept(key))
            {
                this.size -= this.items.get(key);
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean addAll(float... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (float each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(FloatIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof FloatBag)
        {
            FloatBag otherBag = (FloatBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                float each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(float... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (float each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(FloatIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof FloatBag)
        {
            FloatBag otherBag = (FloatBag) source;
            otherBag.forEachWithOccurrences((float each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                FloatHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                float each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(FloatIterable source)
    {
        int oldSize = this.size();
        FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
        FloatHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(float... source)
    {
        return this.retainAll(FloatHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(float item, int occurrences)
    {
        if (occurrences < 0)
        {
            throw new IllegalArgumentException("Cannot add a negative number of occurrences");
        }
        if (occurrences > 0)
        {
            this.items.updateValue(item, 0, (int intParameter) -> intParameter + occurrences);
            this.size += occurrences;
        }
    }

    @Override
    public boolean removeOccurrences(float item, int occurrences)
    {
        if (occurrences < 0)
        {
            throw new IllegalArgumentException("Cannot remove a negative number of occurrences");
        }

        if (occurrences == 0)
        {
            return false;
        }

        int newValue = this.items.updateValue(item, 0, (int intParameter) -> intParameter - occurrences);

        if (newValue <= 0)
        {
            this.size -= occurrences - newValue;
            this.items.removeKey(item);
            return newValue + occurrences != 0;
        }

        this.size -= occurrences;
        return true;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.items.forEachKeyValue((float key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public FloatHashBag select(FloatPredicate predicate)
    {
        FloatHashBag result = new FloatHashBag();
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public FloatHashBag reject(FloatPredicate predicate)
    {
        FloatHashBag result = new FloatHashBag();
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(FloatBags.mutable.withAll(this));
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatBag batch = FloatBags.mutable.empty();
                    for (int i = 0; i < size && iterator.hasNext(); i++)
                    {
                        batch.add(iterator.next());
                    }
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object otherBag)
    {
        if (otherBag == this)
        {
            return true;
        }
        if (!(otherBag instanceof FloatBag))
        {
            return false;
        }
        FloatBag bag = (FloatBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((float key) ->
                FloatHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((float eachItem, int occurrences) ->
                result.add(Float.floatToIntBits(eachItem) ^ occurrences));
        return result.getCount();
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        boolean[] firstItem = {true};
        try
        {
            appendable.append(start);
            this.items.forEachKeyValue((float each, int occurrences) ->
            {
                try
                {
                    for (int i = 0; i < occurrences; i++)
                    {
                        if (!firstItem[0])
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(each));
                        firstItem[0] = false;
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((float each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public float max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public float min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().min();
    }

    @Override
    public double sum()
    {
        double[] result = {0.0};
        double[] compensation = {0.0};
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                double adjustedValue = (double) each - compensation[0];
                double nextSum = result[0] + adjustedValue;
                compensation[0] = nextSum - result[0] - adjustedValue;
                result[0] = nextSum;
            }
        });
        return result[0];
    }

    @Override
    public float[] toArray()
    {
        float[] array = new float[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                array[index[0]] = each;
                index[0]++;
            }
        });
        return array;
    }

    @Override
    public float[] toArray(float[] array)
    {
        if (array.length < this.size())
        {
            array = new float[this.size()];
        }

        int[] index = {0};

        float[] finalBypass = array;
        this.forEachWithOccurrences((float each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                finalBypass[index[0]] = each;
                index[0]++;
            }
        });
        return array;
    }

    @Override
    public MutableFloatBag asUnmodifiable()
    {
        return new UnmodifiableFloatBag(this);
    }

    @Override
    public MutableFloatBag asSynchronized()
    {
        return new SynchronizedFloatBag(this);
    }

    @Override
    public ImmutableFloatBag toImmutable()
    {
        return FloatBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty FloatHashBag.
     *
     * @since 9.2.
     */
    @Override
    public FloatHashBag newEmpty()
    {
        return new FloatHashBag();
    }

    @Override
    public MutableFloatIterator floatIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((float each, int occurrences) ->
            {
                try
                {
                    out.writeFloat(each);
                    out.writeInt(occurrences);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
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
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        this.items = new FloatIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readFloat(), in.readInt());
        }
    }

    private class InternalIterator implements MutableFloatIterator
    {
        private MutableFloatIterator floatIterator = FloatHashBag.this.items.keySet().floatIterator();

        private float currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.floatIterator.hasNext();
        }

        @Override
        public float next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.floatIterator.next();
                this.occurrences = FloatHashBag.this.occurrencesOf(this.currentItem);
            }
            this.occurrences--;
            this.canRemove = true;
            return this.currentItem;
        }

        @Override
        public void remove()
        {
            if (!this.canRemove)
            {
                throw new IllegalStateException();
            }
            if (this.occurrences == 0)
            {
                this.floatIterator.remove();
                FloatHashBag.this.size--;
            }
            else
            {
                FloatHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
