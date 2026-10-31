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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.CharBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.map.mutable.primitive.CharIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractCharIterable;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * CharHashBag is similar to {@link HashBag}, and is memory-optimized for char primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class CharHashBag
        extends AbstractCharIterable
        implements MutableCharBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private CharIntHashMap items;
    private int size;

    public CharHashBag()
    {
        this.items = new CharIntHashMap();
    }

    public CharHashBag(int size)
    {
        this.items = new CharIntHashMap(size);
    }

    public CharHashBag(CharIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public CharHashBag(char... elements)
    {
        this();
        this.addAll(elements);
    }

    public CharHashBag(CharHashBag bag)
    {
        this.items = new CharIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static CharHashBag newBag(int size)
    {
        return new CharHashBag(size);
    }

    public static CharHashBag newBagWith(char... source)
    {
        return new CharHashBag(source);
    }

    public static CharHashBag newBag(CharIterable source)
    {
        if (source instanceof CharHashBag)
        {
            return new CharHashBag((CharHashBag) source);
        }
        return new CharHashBag(source);
    }

    public static CharHashBag newBag(CharBag source)
    {
        return new CharHashBag(source);
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
    public CharHashBag with(char element)
    {
        this.add(element);
        return this;
    }

    public CharHashBag with(char element1, char element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public CharHashBag with(char element1, char element2, char element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public CharHashBag withAll(CharIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public CharHashBag without(char element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public CharHashBag withoutAll(CharIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(char value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(char item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(CharIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public CharHashBag selectByOccurrences(IntPredicate predicate)
    {
        CharHashBag result = new CharHashBag();
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
    public MutableCharSet selectUnique()
    {
        MutableCharSet result = CharSets.mutable.empty();
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
    public MutableList<CharIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<CharIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, CharIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<CharIntPair> occurrencesSortingBy(
            int n,
            IntFunction<CharIntPair> function,
            MutableList<CharIntPair> returnWhenEmpty)
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
        MutableList<CharIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<CharIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<CharIntPair> toListWithOccurrences()
    {
        MutableList<CharIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(char item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(char item)
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
    public boolean removeIf(CharPredicate predicate)
    {
        boolean changed = false;
        for (MutableCharIterator iterator = this.items.keySet().charIterator(); iterator.hasNext(); )
        {
            char key = iterator.next();
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
    public boolean addAll(char... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (char each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(CharIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof CharBag)
        {
            CharBag otherBag = (CharBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                char each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(char... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (char each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(CharIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof CharBag)
        {
            CharBag otherBag = (CharBag) source;
            otherBag.forEachWithOccurrences((char each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                CharHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                char each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(CharIterable source)
    {
        int oldSize = this.size();
        CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
        CharHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(char... source)
    {
        return this.retainAll(CharHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(char item, int occurrences)
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
    public boolean removeOccurrences(char item, int occurrences)
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
    public void each(CharProcedure procedure)
    {
        this.items.forEachKeyValue((char key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public CharHashBag select(CharPredicate predicate)
    {
        CharHashBag result = new CharHashBag();
        this.forEachWithOccurrences((char each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public CharHashBag reject(CharPredicate predicate)
    {
        CharHashBag result = new CharHashBag();
        this.forEachWithOccurrences((char each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((char each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(CharBags.mutable.withAll(this));
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharBag batch = CharBags.mutable.empty();
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
        if (!(otherBag instanceof CharBag))
        {
            return false;
        }
        CharBag bag = (CharBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((char key) ->
                CharHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((char eachItem, int occurrences) ->
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
            this.items.forEachKeyValue((char each, int occurrences) ->
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
    public int count(CharPredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((char each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((char each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public char min()
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
        this.forEachWithOccurrences((char each, int occurrences) ->
                result[0] += (long) each * occurrences);
        return result[0];
    }

    @Override
    public char[] toArray()
    {
        char[] array = new char[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((char each, int occurrences) ->
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
    public char[] toArray(char[] array)
    {
        if (array.length < this.size())
        {
            array = new char[this.size()];
        }

        int[] index = {0};

        char[] finalBypass = array;
        this.forEachWithOccurrences((char each, int occurrences) ->
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
    public MutableCharBag asUnmodifiable()
    {
        return new UnmodifiableCharBag(this);
    }

    @Override
    public MutableCharBag asSynchronized()
    {
        return new SynchronizedCharBag(this);
    }

    @Override
    public ImmutableCharBag toImmutable()
    {
        return CharBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty CharHashBag.
     *
     * @since 9.2.
     */
    @Override
    public CharHashBag newEmpty()
    {
        return new CharHashBag();
    }

    @Override
    public MutableCharIterator charIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((char each, int occurrences) ->
            {
                try
                {
                    out.writeChar(each);
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
        this.items = new CharIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readChar(), in.readInt());
        }
    }

    private class InternalIterator implements MutableCharIterator
    {
        private MutableCharIterator charIterator = CharHashBag.this.items.keySet().charIterator();

        private char currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.charIterator.hasNext();
        }

        @Override
        public char next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.charIterator.next();
                this.occurrences = CharHashBag.this.occurrencesOf(this.currentItem);
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
                this.charIterator.remove();
                CharHashBag.this.size--;
            }
            else
            {
                CharHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
