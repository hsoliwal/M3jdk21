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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ByteBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.map.mutable.primitive.ByteIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractByteIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * ByteHashBag is similar to {@link HashBag}, and is memory-optimized for byte primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class ByteHashBag
        extends AbstractByteIterable
        implements MutableByteBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private ByteIntHashMap items;
    private int size;

    public ByteHashBag()
    {
        this.items = new ByteIntHashMap();
    }

    public ByteHashBag(int size)
    {
        this.items = new ByteIntHashMap(size);
    }

    public ByteHashBag(ByteIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public ByteHashBag(byte... elements)
    {
        this();
        this.addAll(elements);
    }

    public ByteHashBag(ByteHashBag bag)
    {
        this.items = new ByteIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static ByteHashBag newBag(int size)
    {
        return new ByteHashBag(size);
    }

    public static ByteHashBag newBagWith(byte... source)
    {
        return new ByteHashBag(source);
    }

    public static ByteHashBag newBag(ByteIterable source)
    {
        if (source instanceof ByteHashBag)
        {
            return new ByteHashBag((ByteHashBag) source);
        }
        return new ByteHashBag(source);
    }

    public static ByteHashBag newBag(ByteBag source)
    {
        return new ByteHashBag(source);
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
    public ByteHashBag with(byte element)
    {
        this.add(element);
        return this;
    }

    public ByteHashBag with(byte element1, byte element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public ByteHashBag with(byte element1, byte element2, byte element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public ByteHashBag withAll(ByteIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public ByteHashBag without(byte element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public ByteHashBag withoutAll(ByteIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(byte item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(ByteIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public ByteHashBag selectByOccurrences(IntPredicate predicate)
    {
        ByteHashBag result = new ByteHashBag();
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
    public MutableByteSet selectUnique()
    {
        MutableByteSet result = ByteSets.mutable.empty();
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
    public MutableList<ByteIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<ByteIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, ByteIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<ByteIntPair> occurrencesSortingBy(
            int n,
            IntFunction<ByteIntPair> function,
            MutableList<ByteIntPair> returnWhenEmpty)
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
        MutableList<ByteIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<ByteIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<ByteIntPair> toListWithOccurrences()
    {
        MutableList<ByteIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(byte item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(byte item)
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
    public boolean removeIf(BytePredicate predicate)
    {
        boolean changed = false;
        for (MutableByteIterator iterator = this.items.keySet().byteIterator(); iterator.hasNext(); )
        {
            byte key = iterator.next();
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
    public boolean addAll(byte... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (byte each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof ByteBag)
        {
            ByteBag otherBag = (ByteBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(byte... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (byte each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof ByteBag)
        {
            ByteBag otherBag = (ByteBag) source;
            otherBag.forEachWithOccurrences((byte each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                ByteHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(ByteIterable source)
    {
        int oldSize = this.size();
        ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
        ByteHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(byte... source)
    {
        return this.retainAll(ByteHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(byte item, int occurrences)
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
    public boolean removeOccurrences(byte item, int occurrences)
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
    public void each(ByteProcedure procedure)
    {
        this.items.forEachKeyValue((byte key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public ByteHashBag select(BytePredicate predicate)
    {
        ByteHashBag result = new ByteHashBag();
        this.forEachWithOccurrences((byte each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public ByteHashBag reject(BytePredicate predicate)
    {
        ByteHashBag result = new ByteHashBag();
        this.forEachWithOccurrences((byte each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((byte each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ByteIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(ByteBags.mutable.withAll(this));
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
                while (iterator.hasNext())
                {
                    MutableByteBag batch = ByteBags.mutable.empty();
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
        if (!(otherBag instanceof ByteBag))
        {
            return false;
        }
        ByteBag bag = (ByteBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((byte key) ->
                ByteHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((byte eachItem, int occurrences) ->
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
            this.items.forEachKeyValue((byte each, int occurrences) ->
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
    public int count(BytePredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((byte each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((byte each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public byte min()
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
        this.forEachWithOccurrences((byte each, int occurrences) ->
                result[0] += (long) each * occurrences);
        return result[0];
    }

    @Override
    public byte[] toArray()
    {
        byte[] array = new byte[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((byte each, int occurrences) ->
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
    public byte[] toArray(byte[] array)
    {
        if (array.length < this.size())
        {
            array = new byte[this.size()];
        }

        int[] index = {0};

        byte[] finalBypass = array;
        this.forEachWithOccurrences((byte each, int occurrences) ->
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
    public MutableByteBag asUnmodifiable()
    {
        return new UnmodifiableByteBag(this);
    }

    @Override
    public MutableByteBag asSynchronized()
    {
        return new SynchronizedByteBag(this);
    }

    @Override
    public ImmutableByteBag toImmutable()
    {
        return ByteBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty ByteHashBag.
     *
     * @since 9.2.
     */
    @Override
    public ByteHashBag newEmpty()
    {
        return new ByteHashBag();
    }

    @Override
    public MutableByteIterator byteIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((byte each, int occurrences) ->
            {
                try
                {
                    out.writeByte(each);
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
        this.items = new ByteIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readByte(), in.readInt());
        }
    }

    private class InternalIterator implements MutableByteIterator
    {
        private MutableByteIterator byteIterator = ByteHashBag.this.items.keySet().byteIterator();

        private byte currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.byteIterator.hasNext();
        }

        @Override
        public byte next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.byteIterator.next();
                this.occurrences = ByteHashBag.this.occurrencesOf(this.currentItem);
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
                this.byteIterator.remove();
                ByteHashBag.this.size--;
            }
            else
            {
                ByteHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
