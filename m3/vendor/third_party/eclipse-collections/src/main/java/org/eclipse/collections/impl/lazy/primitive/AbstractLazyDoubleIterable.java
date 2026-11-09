/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToByteFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToIntFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToLongFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.factory.primitive.DoubleBags;
import org.eclipse.collections.api.factory.primitive.DoubleLists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.utility.internal.primitive.DoubleIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyDoubleIterate;

import java.util.NoSuchElementException;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyDoubleIterable implements LazyDoubleIterable
{
    @Override
    public int size()
    {
        return this.count(value -> true);
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    @Override
    public boolean isEmpty()
    {
        return DoubleIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return DoubleIterableIterate.notEmpty(this);
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
        DoubleIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(double value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.containsAll(DoubleSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return source.allSatisfy((double value) -> AbstractLazyDoubleIterable.this.contains(value));
    }

    @Override
    public LazyDoubleIterable select(DoublePredicate predicate)
    {
        return LazyDoubleIterate.select(this, predicate);
    }

    @Override
    public LazyDoubleIterable reject(DoublePredicate predicate)
    {
        return LazyDoubleIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyDoubleIterable tap(DoubleProcedure procedure)
    {
        return LazyDoubleIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return LazyDoubleIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(DoubleToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyDoubleIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(DoubleToBooleanFunction function)
    {
        return new CollectDoubleToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(DoubleToByteFunction function)
    {
        return new CollectDoubleToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(DoubleToCharFunction function)
    {
        return new CollectDoubleToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(DoubleToShortFunction function)
    {
        return new CollectDoubleToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(DoubleToIntFunction function)
    {
        return new CollectDoubleToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(DoubleToFloatFunction function)
    {
        return new CollectDoubleToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(DoubleToLongFunction function)
    {
        return new CollectDoubleToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(DoubleToDoubleFunction function)
    {
        return new CollectDoubleToDoubleIterable(this, function);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return DoubleIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return DoubleIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return DoubleIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return DoubleIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return DoubleIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return DoubleIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return new ChunkDoubleIterable(this, size);
    }

    @Override
    public double[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableDoubleList toList()
    {
        final MutableDoubleList list = DoubleLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableDoubleSet toSet()
    {
        final MutableDoubleSet set = DoubleSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableDoubleBag toBag()
    {
        final MutableDoubleBag bag = DoubleBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public double sum()
    {
        DoubleSumProcedure procedure = new DoubleSumProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public double max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        DoubleMaxProcedure procedure = new DoubleMaxProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public double maxIfEmpty(double ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
        }
        return this.max();
    }

    @Override
    public double min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        DoubleMinProcedure procedure = new DoubleMinProcedure();
        this.forEach(procedure);
        return procedure.getValue();
    }

    @Override
    public double minIfEmpty(double ifEmpty)
    {
        if (this.isEmpty())
        {
            return ifEmpty;
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
        return (double) this.sum() / (double) this.size();
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
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public double[] toSortedArray()
    {
        return this.toSortedList().toArray();
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleLists.mutable.withAll(this).sortThis();
    }

    private static final class DoubleMaxProcedure implements DoubleProcedure
    {
        private boolean visitedOnce;
        private double max;

        @Override
        public void value(double each)
        {
            if (this.visitedOnce)
            {
                if (Double.compare(this.max, each) < 0)
                {
                    this.max = each;
                }
            }
            else
            {
                this.max = each;
                this.visitedOnce = true;
            }
        }

        public double getValue()
        {
            return this.max;
        }
    }

    private static final class DoubleMinProcedure implements DoubleProcedure
    {
        private boolean visitedOnce;
        private double min;

        @Override
        public void value(double each)
        {
            if (this.visitedOnce)
            {
                if (Double.compare(each, this.min) < 0)
                {
                    this.min = each;
                }
            }
            else
            {
                this.min = each;
                this.visitedOnce = true;
            }
        }

        public double getValue()
        {
            return this.min;
        }
    }

    private static final class DoubleSumProcedure implements DoubleProcedure
    {
        private double sum = 0.0;
        private double compensation = 0.0;

        @Override
        public void value(double each)
        {
            double adjustedValue = each - this.compensation;
            double nextSum = this.sum + adjustedValue;
            this.compensation = nextSum - this.sum - adjustedValue;
            this.sum = nextSum;
        }

        public double getValue()
        {
            return this.sum;
        }
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return this;
    }
}
