/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectStreamException;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableFloatSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.FloatHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatSets;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.api.set.primitive.FloatSet;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.impl.factory.Sets;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.BoxedMutableFloatSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.SynchronizedFloatSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableFloatSet;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveKeySet.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class AbstractMutableFloatKeySet implements MutableFloatSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_KEY = 0.0f;
    private static final float REMOVED_KEY = 1.0f;

    private static boolean isEmptyKey(float key)
    {
        return Float.compare(key, EMPTY_KEY) == 0;
    }

    private static boolean isRemovedKey(float key)
    {
        return Float.compare(key, REMOVED_KEY) == 0;
    }

    private static boolean isNonSentinel(float key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    @Override
    public MutableSet<Float> boxed()
    {
        return new BoxedMutableFloatSet(this);
    }

    protected abstract float getKeyAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract MutableFloatKeysMap getOuter();

    protected abstract AbstractSentinelValues getSentinelValues();

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.getOuter().forEachKey(procedure);
    }

    @Override
    public int count(FloatPredicate predicate)
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
    public boolean anySatisfy(FloatPredicate predicate)
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
    public boolean allSatisfy(FloatPredicate predicate)
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
    public boolean add(float element)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(float... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(FloatIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatSet select(FloatPredicate predicate)
    {
        MutableFloatSet result = new FloatHashSet();
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
    public MutableFloatSet reject(FloatPredicate predicate)
    {
        MutableFloatSet result = new FloatHashSet();
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
    public MutableFloatSet with(float element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatSet without(float element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatSet withAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatSet withoutAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public <V> MutableSet<V> collect(FloatToObjectFunction<? extends V> function)
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
    public boolean remove(float key)
    {
        int oldSize = this.getOuter().size();
        this.getOuter().removeKey(key);
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(FloatIterable source)
    {
        int oldSize = this.getOuter().size();
        FloatIterator iterator = source.floatIterator();
        while (iterator.hasNext())
        {
            this.getOuter().removeKey(iterator.next());
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(float... source)
    {
        int oldSize = this.getOuter().size();
        for (float item : source)
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
    public float detectIfNone(FloatPredicate predicate, float ifNone)
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
    public MutableFloatSet asUnmodifiable()
    {
        return UnmodifiableFloatSet.of(this);
    }

    @Override
    public MutableFloatSet asSynchronized()
    {
        return SynchronizedFloatSet.of(this);
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
    public float max()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        float max = 0;
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
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMaxSet || Float.compare(max, this.getKeyAtIndex(i)) < 0))
            {
                max = this.getKeyAtIndex(i);
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.max();
    }

    @Override
    public float min()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        float min = 0;
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
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMinSet || Float.compare(this.getKeyAtIndex(i), min) < 0))
            {
                min = this.getKeyAtIndex(i);
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
        float[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            float first = sortedArray[middleIndex];
            float second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public float[] toSortedArray()
    {
        float[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newList(this).sortThis();
    }

    @Override
    public float[] toArray()
    {
        int size = this.getOuter().size();
        float[] result = new float[size];
        this.getOuter().forEachKey(new FloatProcedure()
        {
            private int index;

            public void value(float each)
            {
                result[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public float[] toArray(float[] result)
    {
        int size = this.getOuter().size();
        if (result.length < size)
        {
            result = new float[size];
        }

        float[] finalBypass = result;

        this.getOuter().forEachKey(new FloatProcedure()
        {
            private int index;

            public void value(float each)
            {
                finalBypass[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public boolean contains(float value)
    {
        return this.getOuter().containsKey(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float item : source)
        {
            if (!this.getOuter().containsKey(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        FloatIterator iterator = source.floatIterator();
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
    public LazyIterable<FloatFloatPair> cartesianProduct(FloatSet set)
    {
        return FloatSets.cartesianProduct(this, set);
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newList(this);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSet(this);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBag(this);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
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
    public ImmutableFloatSet toImmutable()
    {
        return FloatSets.immutable.withAll(this);
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

        if (!(obj instanceof FloatSet))
        {
            return false;
        }

        FloatSet other = (FloatSet) obj;
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
                result += Float.floatToIntBits(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += Float.floatToIntBits(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result += Float.floatToIntBits(this.getKeyAtIndex(i));
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
                result.add(FloatSets.mutable.withAll(this));
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatSet batch = FloatSets.mutable.empty();
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

        private transient AbstractMutableFloatKeySet original;
        private transient FloatHashSet deserialized;

        @SuppressWarnings("RedundantModifier")
        public SerRep()
        {
            // for Externalizable
        }

        private SerRep(AbstractMutableFloatKeySet original)
        {
            this.original = original;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.original.size());
            FloatIterator it = this.original.floatIterator();
            while (it.hasNext())
            {
                out.writeFloat(it.next());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int unread = in.readInt();
            this.deserialized = new FloatHashSet(unread);
            while (unread > 0)
            {
                this.deserialized.add(in.readFloat());
                unread--;
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            return this.deserialized;
        }
    }
}
