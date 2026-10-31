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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ShortBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.tuple.primitive.ShortIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.map.mutable.primitive.ShortIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractShortIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.ShortSets;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * ShortHashBag is similar to {@link HashBag}, and is memory-optimized for short primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class ShortHashBag
        extends AbstractShortIterable
        implements MutableShortBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private ShortIntHashMap items;
    private int size;

    public ShortHashBag()
    {
        this.items = new ShortIntHashMap();
    }

    public ShortHashBag(int size)
    {
        this.items = new ShortIntHashMap(size);
    }

    public ShortHashBag(ShortIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public ShortHashBag(short... elements)
    {
        this();
        this.addAll(elements);
    }

    public ShortHashBag(ShortHashBag bag)
    {
        this.items = new ShortIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static ShortHashBag newBag(int size)
    {
        return new ShortHashBag(size);
    }

    public static ShortHashBag newBagWith(short... source)
    {
        return new ShortHashBag(source);
    }

    public static ShortHashBag newBag(ShortIterable source)
    {
        if (source instanceof ShortHashBag)
        {
            return new ShortHashBag((ShortHashBag) source);
        }
        return new ShortHashBag(source);
    }

    public static ShortHashBag newBag(ShortBag source)
    {
        return new ShortHashBag(source);
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
    public ShortHashBag with(short element)
    {
        this.add(element);
        return this;
    }

    public ShortHashBag with(short element1, short element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public ShortHashBag with(short element1, short element2, short element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public ShortHashBag withAll(ShortIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public ShortHashBag without(short element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public ShortHashBag withoutAll(ShortIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(short value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(short item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(ShortIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public ShortHashBag selectByOccurrences(IntPredicate predicate)
    {
        ShortHashBag result = new ShortHashBag();
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
    public MutableShortSet selectUnique()
    {
        MutableShortSet result = ShortSets.mutable.empty();
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
    public MutableList<ShortIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<ShortIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, ShortIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<ShortIntPair> occurrencesSortingBy(
            int n,
            IntFunction<ShortIntPair> function,
            MutableList<ShortIntPair> returnWhenEmpty)
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
        MutableList<ShortIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<ShortIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<ShortIntPair> toListWithOccurrences()
    {
        MutableList<ShortIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(short item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(short item)
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
    public boolean removeIf(ShortPredicate predicate)
    {
        boolean changed = false;
        for (MutableShortIterator iterator = this.items.keySet().shortIterator(); iterator.hasNext(); )
        {
            short key = iterator.next();
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
    public boolean addAll(short... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (short each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(ShortIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof ShortBag)
        {
            ShortBag otherBag = (ShortBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            ShortIterator iterator = source.shortIterator();
            while (iterator.hasNext())
            {
                short each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(short... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (short each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(ShortIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof ShortBag)
        {
            ShortBag otherBag = (ShortBag) source;
            otherBag.forEachWithOccurrences((short each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                ShortHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            ShortIterator iterator = source.shortIterator();
            while (iterator.hasNext())
            {
                short each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(ShortIterable source)
    {
        int oldSize = this.size();
        ShortSet sourceSet = source instanceof ShortSet ? (ShortSet) source : source.toSet();
        ShortHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(short... source)
    {
        return this.retainAll(ShortHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(short item, int occurrences)
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
    public boolean removeOccurrences(short item, int occurrences)
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
    public void each(ShortProcedure procedure)
    {
        this.items.forEachKeyValue((short key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public ShortHashBag select(ShortPredicate predicate)
    {
        ShortHashBag result = new ShortHashBag();
        this.forEachWithOccurrences((short each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public ShortHashBag reject(ShortPredicate predicate)
    {
        ShortHashBag result = new ShortHashBag();
        this.forEachWithOccurrences((short each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((short each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(ShortBags.mutable.withAll(this));
            }
            else
            {
                ShortIterator iterator = this.shortIterator();
                while (iterator.hasNext())
                {
                    MutableShortBag batch = ShortBags.mutable.empty();
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
        if (!(otherBag instanceof ShortBag))
        {
            return false;
        }
        ShortBag bag = (ShortBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((short key) ->
                ShortHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((short eachItem, int occurrences) ->
                result.add((int) eachItem ^ occurrences));
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
            this.items.forEachKeyValue((short each, int occurrences) ->
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
    public int count(ShortPredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((short each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((short each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public short max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public short min()
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
        this.forEachWithOccurrences((short each, int occurrences) ->
                result[0] += (long) each * occurrences);
        return result[0];
    }

    @Override
    public short[] toArray()
    {
        short[] array = new short[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((short each, int occurrences) ->
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
    public short[] toArray(short[] array)
    {
        if (array.length < this.size())
        {
            array = new short[this.size()];
        }

        int[] index = {0};

        short[] finalBypass = array;
        this.forEachWithOccurrences((short each, int occurrences) ->
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
    public MutableShortBag asUnmodifiable()
    {
        return new UnmodifiableShortBag(this);
    }

    @Override
    public MutableShortBag asSynchronized()
    {
        return new SynchronizedShortBag(this);
    }

    @Override
    public ImmutableShortBag toImmutable()
    {
        return ShortBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty ShortHashBag.
     *
     * @since 9.2.
     */
    @Override
    public ShortHashBag newEmpty()
    {
        return new ShortHashBag();
    }

    @Override
    public MutableShortIterator shortIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((short each, int occurrences) ->
            {
                try
                {
                    out.writeShort(each);
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
        this.items = new ShortIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readShort(), in.readInt());
        }
    }

    private class InternalIterator implements MutableShortIterator
    {
        private MutableShortIterator shortIterator = ShortHashBag.this.items.keySet().shortIterator();

        private short currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.shortIterator.hasNext();
        }

        @Override
        public short next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.shortIterator.next();
                this.occurrences = ShortHashBag.this.occurrencesOf(this.currentItem);
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
                this.shortIterator.remove();
                ShortHashBag.this.size--;
            }
            else
            {
                ShortHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
