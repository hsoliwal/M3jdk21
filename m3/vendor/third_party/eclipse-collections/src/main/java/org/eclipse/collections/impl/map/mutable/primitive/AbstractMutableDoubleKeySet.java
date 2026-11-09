/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectStreamException;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedDoubleSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveKeySet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableDoubleKeySet implements MutableDoubleSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final double EMPTY_KEY = 0.0;
    private static final double REMOVED_KEY = 1.0;

    private static boolean isEmptyKey(double key)
    {
        return Double.compare(key, EMPTY_KEY) == 0;
    }

    private static boolean isRemovedKey(double key)
    {
        return Double.compare(key, REMOVED_KEY) == 0;
    }

    private static boolean isNonSentinel(double key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    protected abstract double getKeyAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract MutableDoubleKeysMap getOuter();

    protected abstract AbstractSentinelValues getSentinelValues();

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.getOuter().forEachKey(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        int count = 0;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                count++;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                count++;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                return true;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                return true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
            {
                return false;
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
            {
                return false;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && !predicate.accept(this.getKeyAtIndex(i)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(double element)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(double... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleSet select(DoublePredicate predicate)
    {
        MutableDoubleSet result = new DoubleHashSet();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                result.add(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                result.add(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                result.add(this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public MutableDoubleSet reject(DoublePredicate predicate)
    {
        MutableDoubleSet result = new DoubleHashSet();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
            {
                result.add(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
            {
                result.add(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && !predicate.accept(this.getKeyAtIndex(i)))
            {
                result.add(this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public MutableDoubleSet with(double element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleSet without(double element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleSet withAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleSet withoutAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public <V> MutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        MutableSet<V> result = Sets.mutable.with();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result.add(function.valueOf(EMPTY_KEY));
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result.add(function.valueOf(REMOVED_KEY));
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result.add(function.valueOf(this.getKeyAtIndex(i)));
            }
        }
        return result;
    }

    @Override
    public boolean remove(double key)
    {
        int oldSize = this.getOuter().size();
        this.getOuter().removeKey(key);
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        int oldSize = this.getOuter().size();
        DoubleIterator iterator = source.doubleIterator();
        while (iterator.hasNext())
        {
            this.getOuter().removeKey(iterator.next());
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(double... source)
    {
        int oldSize = this.getOuter().size();
        for (double item : source)
        {
            this.getOuter().removeKey(item);
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public void clear()
    {
        this.getOuter().clear();
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                return EMPTY_KEY;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                return REMOVED_KEY;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                return this.getKeyAtIndex(i);
            }
        }
        return ifNone;
    }

    @Override
    public MutableDoubleSet asUnmodifiable()
    {
        return UnmodifiableDoubleSet.of(this);
    }

    @Override
    public MutableDoubleSet asSynchronized()
    {
        return SynchronizedDoubleSet.of(this);
    }

    @Override
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                double adjustedValue = EMPTY_KEY - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                double adjustedValue = REMOVED_KEY - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                double adjustedValue = this.getKeyAtIndex(i) - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
        }
        return result;
    }

    @Override
    public double max()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        double max = 0;
        boolean isMaxSet = false;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                max = EMPTY_KEY;
                isMaxSet = true;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                max = REMOVED_KEY;
                isMaxSet = true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMaxSet || Double.compare(max, this.getKeyAtIndex(i)) < 0))
            {
                max = this.getKeyAtIndex(i);
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.max();
    }

    @Override
    public double min()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        double min = 0;
        boolean isMinSet = false;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                min = EMPTY_KEY;
                isMinSet = true;
            }
            if (this.getSentinelValues().containsOneKey && !isMinSet)
            {
                min = REMOVED_KEY;
                isMinSet = true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMinSet || Double.compare(this.getKeyAtIndex(i), min) < 0))
            {
                min = this.getKeyAtIndex(i);
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.min();
    }

    @Override
    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        double[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            double first = sortedArray[middleIndex];
            double second = sortedArray[middleIndex - 1];
            return (first + second) / 2.0;
        }
        return sortedArray[middleIndex];
    }

    @Override
    public double[] toSortedArray()
    {
        double[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newList(this).sortThis();
    }

    @Override
    public double[] toArray()
    {
        int size = this.getOuter().size();
        final double[] result = new double[size];
        this.getOuter().forEachKey(new DoubleProcedure()
        {
            private int index;

            public void value(double each)
            {
                result[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public double[] toArray(double[] result)
    {
        int size = this.getOuter().size();
        if (result.length < size)
        {
            result = new double[size];
        }

        double[] finalBypass = result;

        this.getOuter().forEachKey(new DoubleProcedure()
        {
            private int index;

            public void value(double each)
            {
                finalBypass[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public boolean contains(double value)
    {
        return this.getOuter().containsKey(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double item : source)
        {
            if (!this.getOuter().containsKey(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        DoubleIterator iterator = source.doubleIterator();
        while (iterator.hasNext())
        {
            if (!this.getOuter().containsKey(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyIterable<DoubleDoublePair> cartesianProduct(DoubleSet set)
    {
        return DoubleSets.cartesianProduct(this, set);
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newList(this);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSet(this);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBag(this);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result = function.valueOf(result, EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result = function.valueOf(result, REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result = function.valueOf(result, this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public ImmutableDoubleSet toImmutable()
    {
        return DoubleSets.immutable.withAll(this);
    }

    @Override
    public int size()
    {
        return this.getOuter().size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.getOuter().isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.getOuter().notEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof DoubleSet))
        {
            return false;
        }

        DoubleSet other = (DoubleSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @Override
    public int hashCode()
    {
        int result = 0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result += (int) (Double.doubleToLongBits(EMPTY_KEY) ^ Double.doubleToLongBits(EMPTY_KEY) >>> 32);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += (int) (Double.doubleToLongBits(REMOVED_KEY) ^ Double.doubleToLongBits(REMOVED_KEY) >>> 32);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result += (int) (Double.doubleToLongBits(this.getKeyAtIndex(i)) ^ Double.doubleToLongBits(this.getKeyAtIndex(i)) >>> 32);
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
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
            boolean first = true;
            if (this.getSentinelValues() != null)
            {
                if (this.getSentinelValues().containsZeroKey)
                {
                    appendable.append(String.valueOf(EMPTY_KEY));
                    first = false;
                }
                if (this.getSentinelValues().containsOneKey)
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(REMOVED_KEY));
                    first = false;
                }
            }
            for (int i = 0; i < this.getTableSize(); i++)
            {
                if (isNonSentinel(this.getKeyAtIndex(i)))
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.getKeyAtIndex(i)));
                    first = false;
                }
            }
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
                result.add(DoubleSets.mutable.withAll(this));
            }
            else
            {
                DoubleIterator iterator = this.doubleIterator();
                while (iterator.hasNext())
                {
                    MutableDoubleSet batch = DoubleSets.mutable.empty();
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

    public Object writeReplace() throws ObjectStreamException
    {
        return new SerRep(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
    }

    private static class SerRep implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private transient AbstractMutableDoubleKeySet original;
        private transient DoubleHashSet deserialized;

        @SuppressWarnings("RedundantModifier")
        public SerRep()
        {
            // for Externalizable
        }

        private SerRep(AbstractMutableDoubleKeySet original)
        {
            this.original = original;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.original.size());
            DoubleIterator it = this.original.doubleIterator();
            while (it.hasNext())
            {
                out.writeDouble(it.next());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int unread = in.readInt();
            this.deserialized = new DoubleHashSet(unread);
            while (unread > 0)
            {
                this.deserialized.add(in.readDouble());
                unread--;
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            return this.deserialized;
        }
    }
}
