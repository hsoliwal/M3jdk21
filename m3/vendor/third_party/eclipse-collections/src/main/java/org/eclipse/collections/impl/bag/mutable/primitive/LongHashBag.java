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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.LongBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractLongIterable;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * LongHashBag is similar to {@link HashBag}, and is memory-optimized for long primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class LongHashBag
        extends AbstractLongIterable
        implements MutableLongBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private LongIntHashMap items;
    private int size;

    public LongHashBag()
    {
        this.items = new LongIntHashMap();
    }

    public LongHashBag(int size)
    {
        this.items = new LongIntHashMap(size);
    }

    public LongHashBag(LongIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public LongHashBag(long... elements)
    {
        this();
        this.addAll(elements);
    }

    public LongHashBag(LongHashBag bag)
    {
        this.items = new LongIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static LongHashBag newBag(int size)
    {
        return new LongHashBag(size);
    }

    public static LongHashBag newBagWith(long... source)
    {
        return new LongHashBag(source);
    }

    public static LongHashBag newBag(LongIterable source)
    {
        if (source instanceof LongHashBag)
        {
            return new LongHashBag((LongHashBag) source);
        }
        return new LongHashBag(source);
    }

    public static LongHashBag newBag(LongBag source)
    {
        return new LongHashBag(source);
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
    public LongHashBag with(long element)
    {
        this.add(element);
        return this;
    }

    public LongHashBag with(long element1, long element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public LongHashBag with(long element1, long element2, long element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public LongHashBag withAll(LongIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public LongHashBag without(long element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public LongHashBag withoutAll(LongIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(long value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(long item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(LongIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public LongHashBag selectByOccurrences(IntPredicate predicate)
    {
        LongHashBag result = new LongHashBag();
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
    public MutableLongSet selectUnique()
    {
        MutableLongSet result = LongSets.mutable.empty();
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
    public MutableList<LongIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<LongIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, LongIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<LongIntPair> occurrencesSortingBy(
            int n,
            IntFunction<LongIntPair> function,
            MutableList<LongIntPair> returnWhenEmpty)
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
        MutableList<LongIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<LongIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<LongIntPair> toListWithOccurrences()
    {
        MutableList<LongIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(long item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(long item)
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
    public boolean removeIf(LongPredicate predicate)
    {
        boolean changed = false;
        for (MutableLongIterator iterator = this.items.keySet().longIterator(); iterator.hasNext(); )
        {
            long key = iterator.next();
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
    public boolean addAll(long... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (long each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(LongIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof LongBag)
        {
            LongBag otherBag = (LongBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                long each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(long... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (long each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(LongIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof LongBag)
        {
            LongBag otherBag = (LongBag) source;
            otherBag.forEachWithOccurrences((long each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                LongHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                long each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(LongIterable source)
    {
        int oldSize = this.size();
        LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
        LongHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(long... source)
    {
        return this.retainAll(LongHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(long item, int occurrences)
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
    public boolean removeOccurrences(long item, int occurrences)
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
    public void each(LongProcedure procedure)
    {
        this.items.forEachKeyValue((long key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public LongHashBag select(LongPredicate predicate)
    {
        LongHashBag result = new LongHashBag();
        this.forEachWithOccurrences((long each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public LongHashBag reject(LongPredicate predicate)
    {
        LongHashBag result = new LongHashBag();
        this.forEachWithOccurrences((long each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((long each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(LongBags.mutable.withAll(this));
            }
            else
            {
                LongIterator iterator = this.longIterator();
                while (iterator.hasNext())
                {
                    MutableLongBag batch = LongBags.mutable.empty();
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
        if (!(otherBag instanceof LongBag))
        {
            return false;
        }
        LongBag bag = (LongBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((long key) ->
                LongHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((long eachItem, int occurrences) ->
                result.add((int) (eachItem ^ eachItem >>> 32) ^ occurrences));
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
            this.items.forEachKeyValue((long each, int occurrences) ->
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
    public int count(LongPredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((long each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((long each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public long min()
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
        this.forEachWithOccurrences((long each, int occurrences) ->
                result[0] += (long) each * occurrences);
        return result[0];
    }

    @Override
    public long[] toArray()
    {
        long[] array = new long[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((long each, int occurrences) ->
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
    public long[] toArray(long[] array)
    {
        if (array.length < this.size())
        {
            array = new long[this.size()];
        }

        int[] index = {0};

        long[] finalBypass = array;
        this.forEachWithOccurrences((long each, int occurrences) ->
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
    public MutableLongBag asUnmodifiable()
    {
        return new UnmodifiableLongBag(this);
    }

    @Override
    public MutableLongBag asSynchronized()
    {
        return new SynchronizedLongBag(this);
    }

    @Override
    public ImmutableLongBag toImmutable()
    {
        return LongBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty LongHashBag.
     *
     * @since 9.2.
     */
    @Override
    public LongHashBag newEmpty()
    {
        return new LongHashBag();
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((long each, int occurrences) ->
            {
                try
                {
                    out.writeLong(each);
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
        this.items = new LongIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readLong(), in.readInt());
        }
    }

    private class InternalIterator implements MutableLongIterator
    {
        private MutableLongIterator longIterator = LongHashBag.this.items.keySet().longIterator();

        private long currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.longIterator.hasNext();
        }

        @Override
        public long next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.longIterator.next();
                this.occurrences = LongHashBag.this.occurrencesOf(this.currentItem);
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
                this.longIterator.remove();
                LongHashBag.this.size--;
            }
            else
            {
                LongHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
