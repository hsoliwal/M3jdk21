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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.DoubleBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleIntPair;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.map.mutable.primitive.DoubleIntHashMap;
import org.eclipse.collections.impl.primitive.AbstractDoubleIterable;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;

/**
 * DoubleHashBag is similar to {@link HashBag}, and is memory-optimized for double primitives.
 * This file was automatically generated from template file primitiveHashBag.stg.
 *
 * @since 3.0.
 */
public class DoubleHashBag
        extends AbstractDoubleIterable
        implements MutableDoubleBag, Externalizable
{
    private static final long serialVersionUID = 1L;

    private DoubleIntHashMap items;
    private int size;

    public DoubleHashBag()
    {
        this.items = new DoubleIntHashMap();
    }

    public DoubleHashBag(int size)
    {
        this.items = new DoubleIntHashMap(size);
    }

    public DoubleHashBag(DoubleIterable iterable)
    {
        this();
        this.addAll(iterable);
    }

    public DoubleHashBag(double... elements)
    {
        this();
        this.addAll(elements);
    }

    public DoubleHashBag(DoubleHashBag bag)
    {
        this.items = new DoubleIntHashMap(bag.sizeDistinct());
        this.addAll(bag);
    }

    public static DoubleHashBag newBag(int size)
    {
        return new DoubleHashBag(size);
    }

    public static DoubleHashBag newBagWith(double... source)
    {
        return new DoubleHashBag(source);
    }

    public static DoubleHashBag newBag(DoubleIterable source)
    {
        if (source instanceof DoubleHashBag)
        {
            return new DoubleHashBag((DoubleHashBag) source);
        }
        return new DoubleHashBag(source);
    }

    public static DoubleHashBag newBag(DoubleBag source)
    {
        return new DoubleHashBag(source);
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
    public DoubleHashBag with(double element)
    {
        this.add(element);
        return this;
    }

    public DoubleHashBag with(double element1, double element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public DoubleHashBag with(double element1, double element2, double element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    @Override
    public DoubleHashBag withAll(DoubleIterable iterable)
    {
        this.addAll(iterable);
        return this;
    }

    @Override
    public DoubleHashBag without(double element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public DoubleHashBag withoutAll(DoubleIterable iterable)
    {
        this.removeAll(iterable);
        return this;
    }

    @Override
    public boolean contains(double value)
    {
        return this.items.containsKey(value);
    }

    @Override
    public int occurrencesOf(double item)
    {
        return this.items.get(item);
    }

    @Override
    public void forEachWithOccurrences(DoubleIntProcedure procedure)
    {
        this.items.forEachKeyValue(procedure);
    }

    @Override
    public DoubleHashBag selectByOccurrences(IntPredicate predicate)
    {
        DoubleHashBag result = new DoubleHashBag();
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
    public MutableDoubleSet selectUnique()
    {
        MutableDoubleSet result = DoubleSets.mutable.empty();
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
    public MutableList<DoubleIntPair> topOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, item -> -item.getTwo(), Lists.mutable.empty());
    }

    @Override
    public MutableList<DoubleIntPair> bottomOccurrences(int count)
    {
        return this.occurrencesSortingBy(count, DoubleIntPair::getTwo, Lists.mutable.empty());
    }

    protected MutableList<DoubleIntPair> occurrencesSortingBy(
            int n,
            IntFunction<DoubleIntPair> function,
            MutableList<DoubleIntPair> returnWhenEmpty)
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
        MutableList<DoubleIntPair> sorted = this.toListWithOccurrences().sortThisByInt(function);
        MutableList<DoubleIntPair> results = sorted.subList(0, keySize).toList();
        while (keySize < sorted.size() && results.getLast().getTwo() == sorted.get(keySize).getTwo())
        {
            results.add(sorted.get(keySize));
            keySize++;
        }
        return results;
    }

    protected MutableList<DoubleIntPair> toListWithOccurrences()
    {
        MutableList<DoubleIntPair> result = Lists.mutable.withInitialCapacity(this.sizeDistinct());
        this.forEachWithOccurrences((each, count) -> result.add(PrimitiveTuples.pair(each, count)));
        return result;
    }

    @Override
    public boolean add(double item)
    {
        this.items.updateValue(item, 0, IntToIntFunctions.increment());
        this.size++;
        return true;
    }

    @Override
    public boolean remove(double item)
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
    public boolean removeIf(DoublePredicate predicate)
    {
        boolean changed = false;
        for (MutableDoubleIterator iterator = this.items.keySet().doubleIterator(); iterator.hasNext(); )
        {
            double key = iterator.next();
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
    public boolean addAll(double... source)
    {
        if (source.length == 0)
        {
            return false;
        }

        for (double each : source)
        {
            this.add(each);
        }
        return true;
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        if (source instanceof DoubleBag)
        {
            DoubleBag otherBag = (DoubleBag) source;
            otherBag.forEachWithOccurrences(this::addOccurrences);
        }
        else
        {
            DoubleIterator iterator = source.doubleIterator();
            while (iterator.hasNext())
            {
                double each = iterator.next();
                this.add(each);
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(double... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (double each : source)
        {
            int occurrences = this.items.removeKeyIfAbsent(each, 0);
            this.size -= occurrences;
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof DoubleBag)
        {
            DoubleBag otherBag = (DoubleBag) source;
            otherBag.forEachWithOccurrences((double each, int occurrences) ->
            {
                int oldOccurrences = this.items.removeKeyIfAbsent(each, 0);
                DoubleHashBag.this.size -= oldOccurrences;
            });
        }
        else
        {
            DoubleIterator iterator = source.doubleIterator();
            while (iterator.hasNext())
            {
                double each = iterator.next();
                int occurrences = this.items.removeKeyIfAbsent(each, 0);
                this.size -= occurrences;
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(DoubleIterable source)
    {
        int oldSize = this.size();
        DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
        DoubleHashBag retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.items = retained.items;
            this.size = retained.size;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(double... source)
    {
        return this.retainAll(DoubleHashSet.newSetWith(source));
    }

    @Override
    public void addOccurrences(double item, int occurrences)
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
    public boolean removeOccurrences(double item, int occurrences)
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
    public void each(DoubleProcedure procedure)
    {
        this.items.forEachKeyValue((double key, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                procedure.value(key);
            }
        });
    }

    @Override
    public DoubleHashBag select(DoublePredicate predicate)
    {
        DoubleHashBag result = new DoubleHashBag();
        this.forEachWithOccurrences((double each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public DoubleHashBag reject(DoublePredicate predicate)
    {
        DoubleHashBag result = new DoubleHashBag();
        this.forEachWithOccurrences((double each, int occurrences) ->
        {
            if (!predicate.accept(each))
            {
                result.addOccurrences(each, occurrences);
            }
        });
        return result;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T[] result = (T[]) new Object[1];
        result[0] = injectedValue;
        this.forEachWithOccurrences((double each, int occurrences) ->
        {
            for (int i = 0; i < occurrences; i++)
            {
                result[0] = function.valueOf(result[0], each);
            }
        });
        return result[0];
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<DoubleIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(DoubleBags.mutable.withAll(this));
            }
            else
            {
                DoubleIterator iterator = this.doubleIterator();
                while (iterator.hasNext())
                {
                    MutableDoubleBag batch = DoubleBags.mutable.empty();
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
        if (!(otherBag instanceof DoubleBag))
        {
            return false;
        }
        DoubleBag bag = (DoubleBag) otherBag;
        if (this.sizeDistinct() != bag.sizeDistinct())
        {
            return false;
        }

        return this.items.keysView().allSatisfy((double key) ->
                DoubleHashBag.this.occurrencesOf(key) == bag.occurrencesOf(key));
    }

    @Override
    public int hashCode()
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((double eachItem, int occurrences) ->
                result.add((int) (Double.doubleToLongBits(eachItem) ^ Double.doubleToLongBits(eachItem) >>> 32) ^ occurrences));
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
            this.items.forEachKeyValue((double each, int occurrences) ->
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
    public int count(DoublePredicate predicate)
    {
        Counter result = new Counter();
        this.forEachWithOccurrences((double each, int occurrences) ->
        {
            if (predicate.accept(each))
            {
                result.add(occurrences);
            }
        });
        return result.getCount();
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.items.keysView().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.items.keysView().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.items.keysView().noneSatisfy(predicate);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.items.keysView().detectIfNone(predicate, ifNone);
    }

    @Override
    public <V> MutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        HashBag<V> result = HashBag.newBag(this.items.size());
        this.forEachWithOccurrences((double each, int occurrences) ->
                result.addOccurrences(function.valueOf(each), occurrences));
        return result;
    }

    @Override
    public double max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return this.items.keysView().max();
    }

    @Override
    public double min()
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
        this.forEachWithOccurrences((double each, int occurrences) ->
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
    public double[] toArray()
    {
        double[] array = new double[this.size()];
        int[] index = {0};

        this.forEachWithOccurrences((double each, int occurrences) ->
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
    public double[] toArray(double[] array)
    {
        if (array.length < this.size())
        {
            array = new double[this.size()];
        }

        int[] index = {0};

        double[] finalBypass = array;
        this.forEachWithOccurrences((double each, int occurrences) ->
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
    public MutableDoubleBag asUnmodifiable()
    {
        return new UnmodifiableDoubleBag(this);
    }

    @Override
    public MutableDoubleBag asSynchronized()
    {
        return new SynchronizedDoubleBag(this);
    }

    @Override
    public ImmutableDoubleBag toImmutable()
    {
        return DoubleBags.immutable.withAll(this);
    }

    /**
     * Creates a new empty DoubleHashBag.
     *
     * @since 9.2.
     */
    @Override
    public DoubleHashBag newEmpty()
    {
        return new DoubleHashBag();
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new InternalIterator();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.items.size());
        try
        {
            this.items.forEachKeyValue((double each, int occurrences) ->
            {
                try
                {
                    out.writeDouble(each);
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
        this.items = new DoubleIntHashMap(size);
        for (int i = 0; i < size; i++)
        {
            this.addOccurrences(in.readDouble(), in.readInt());
        }
    }

    private class InternalIterator implements MutableDoubleIterator
    {
        private MutableDoubleIterator doubleIterator = DoubleHashBag.this.items.keySet().doubleIterator();

        private double currentItem;
        private int occurrences;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.occurrences > 0 || this.doubleIterator.hasNext();
        }

        @Override
        public double next()
        {
            if (this.occurrences == 0)
            {
                this.currentItem = this.doubleIterator.next();
                this.occurrences = DoubleHashBag.this.occurrencesOf(this.currentItem);
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
                this.doubleIterator.remove();
                DoubleHashBag.this.size--;
            }
            else
            {
                DoubleHashBag.this.remove(this.currentItem);
            }
            this.canRemove = false;
        }
    }
}
