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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.IntBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractIntIterable;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * IntHashBag is similar to {@link HashBag}, and is memory-optimized for int primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class IntHashBag
        extends AbstractIntIterable
        implements MutableIntBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private IntIntHashMap items;
    private int size;

    public IntHashBag()
    {
        this.items = new IntIntHashMap();
    }

    public IntHashBag(int size)
    {
        this.items = new IntIntHashMap(size);
    }

    public IntHashBag(IntIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public IntHashBag(int... elements)
    {
        this();
        this.addAll(elements);
    }

    public IntHashBag(IntHashBag bag)
    {
        this.items = new IntIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static IntHashBag newBag(int size)
    {
        return new IntHashBag(size);
    }

    public static IntHashBag newBagWith(int... source)
    {
        return new IntHashBag(source);
    }

    public static IntHashBag newBag(IntIterable source)
    {
        if (source instanceof IntHashBag)
        {
            return new IntHashBag((IntHashBag) source);
        }
        return new IntHashBag(source);
    }

    public static IntHashBag newBag(IntBag source)
    {
        return new IntHashBag(source);
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
    public IntHashBag with(int element)
    {
        this.add(element);
        return this;
    }

    public IntHashBag with(int element1, int element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public IntHashBag with(int element1, int element2, int element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public IntHashBag withAll(IntIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public IntHashBag without(int element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public IntHashBag withoutAll(IntIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(int value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(int item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(IntIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public IntHashBag selectByOccurrences(IntPredicate predicate)
    {
        IntHashBag result = new IntHashBag();
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
    public MutableIntSet selectUnique()
    {
        MutableIntSet result = IntSets.mutable.empty();
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
    public MutableList<IntIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<IntIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, IntIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<IntIntPair> occurrencesSortingBy(
            int n,
            IntFunction<IntIntPair> function,
            MutableList<IntIntPair> returnWhenEmpty)
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
        MutableList<IntIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<IntIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<IntIntPair> toListWithOccurrences()
    {
        MutableList<IntIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(int item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(int item)
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
    public boolean removeIf(IntPredicate predicate)
    {
        boolean changed = false;
        for (MutableIntIterator iterator = this.items.keySet().intIterator(); iterator.hasNext(); )
        {
            int key = iterator.next();
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
    public boolean addAll(int... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (int each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(IntIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof IntBag)
        {
            IntBag otherBag = (IntBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                int each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(int... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (int each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(IntIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof IntBag)
        {
            IntBag otherBag = (IntBag) source;
            otherBag.forEachWithOccurrences((int each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                IntHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                int each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(IntIterable source)
    {
        int oldSize = this.size();
        IntSet sourceSet = source instanceof IntSet ? (IntSet) source : source.toSet();
        IntHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(int... source)
    {
        return this.retainAll(IntHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(int item, int occurrences)
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
    public boolean removeOccurrences(int item, int occurrences)
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
    public void each(IntProcedure procedure)
    {
        this.items.forEachKeyValue((int key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public IntHashBag select(IntPredicate predicate)
    {
        IntHashBag result = new IntHashBag();
        this.forEachWithOccurrences((int each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public IntHashBag reject(IntPredicate predicate)
    {
        IntHashBag result = new IntHashBag();
        this.forEachWithOccurrences((int each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((int each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
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
                result.add(IntBags.mutable.withAll(this));
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
        if (!(otherBag instanceof IntBag))
        {
            return false;
        }
        IntBag bag = (IntBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((int key) ->
                IntHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((int eachItem, int occurrences) ->
                result.add(eachItem ^ occurrences));
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
            this.items.forEachKeyValue((int each, int occurrences) ->
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
    public int count(IntPredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((int each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((int each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public int max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public int min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().min();
    }

    @Override
    public long sum()
    {
        long[] result = {0L};
        this.forEachWithOccurrences((int each, int occurrences) ->
                result[0] += (long) each * occurrences);
        return result[0];
    }

    @Override
    public int[] toArray()
    {
        int[] array = new int[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((int each, int occurrences) ->
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
    public int[] toArray(int[] array)
    {
        if (array.length < this.size())
        {
            array = new int[this.size()];
        }

        int[] index = {0};

        int[] finalBypass = array;
        this.forEachWithOccurrences((int each, int occurrences) ->
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
    public MutableIntBag asUnmodifiable()
    {
        return new UnmodifiableIntBag(this);
    }

    @Override
    public MutableIntBag asSynchronized()
    {
        return new SynchronizedIntBag(this);
    }

    @Override
    public ImmutableIntBag toImmutable()
    {
        return IntBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty IntHashBag.
     *
     * @since 9.2.
     */
    @Override
    public IntHashBag newEmpty()
    {
        return new IntHashBag();
    }

    @Override
    public MutableIntIterator intIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((int each, int occurrences) ->
            {
                try
                {
                    out.writeInt(each);
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
        this.items = new IntIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readInt(), in.readInt());
        }
    }

    private class InternalIterator implements MutableIntIterator
    {
        private MutableIntIterator intIterator = IntHashBag.this.items.keySet().intIterator();

        private int currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.intIterator.hasNext();
        }

        @Override
        public int next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.intIterator.next();
                this.occurrences = IntHashBag.this.occurrencesOf(this.currentItem);
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
                this.intIterator.remove();
                IntHashBag.this.size--;
            }
            else
            {
                IntHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
