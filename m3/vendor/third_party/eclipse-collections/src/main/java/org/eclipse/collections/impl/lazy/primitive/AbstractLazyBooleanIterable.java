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

import org.eclipse.collections.api.BooleanIterable;
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
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToByteFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToCharFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToIntFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToLongFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.factory.primitive.BooleanBags;
import org.eclipse.collections.api.factory.primitive.BooleanLists;
import org.eclipse.collections.api.factory.primitive.BooleanSets;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.utility.internal.primitive.BooleanIterableIterate;
import org.eclipse.collections.impl.utility.primitive.LazyBooleanIterate;

/**
 * This file was automatically generated from template file abstractLazyPrimitiveIterable.stg.
 *
 * @since 5.0
 */
public abstract class AbstractLazyBooleanIterable implements LazyBooleanIterable
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
        return BooleanIterableIterate.isEmpty(this);
    }

    @Override
    public boolean notEmpty()
    {
        return BooleanIterableIterate.notEmpty(this);
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
        BooleanIterableIterate.appendString(this, appendable, start, separator, end);
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.anySatisfy(each -> each == value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.containsAll(BooleanSets.immutable.of(source));
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return source.allSatisfy((boolean value) -> AbstractLazyBooleanIterable.this.contains(value));
    }

    @Override
    public LazyBooleanIterable select(BooleanPredicate predicate)
    {
        return LazyBooleanIterate.select(this, predicate);
    }

    @Override
    public LazyBooleanIterable reject(BooleanPredicate predicate)
    {
        return LazyBooleanIterate.select(this, value -> !predicate.accept(value));
    }

    @Override
    public LazyBooleanIterable tap(BooleanProcedure procedure)
    {
        return LazyBooleanIterate.tap(this, procedure);
    }

    @Override
    public <V> LazyIterable<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return LazyBooleanIterate.collect(this, function);
    }

    public <V> LazyIterable<V> flatCollect(BooleanToObjectFunction<? extends Iterable<V>> function)
    {
        return LazyBooleanIterate.flatCollect(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyBooleanIterable collectBoolean(BooleanToBooleanFunction function)
    {
        return new CollectBooleanToBooleanIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyByteIterable collectByte(BooleanToByteFunction function)
    {
        return new CollectBooleanToByteIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyCharIterable collectChar(BooleanToCharFunction function)
    {
        return new CollectBooleanToCharIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyShortIterable collectShort(BooleanToShortFunction function)
    {
        return new CollectBooleanToShortIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyIntIterable collectInt(BooleanToIntFunction function)
    {
        return new CollectBooleanToIntIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyFloatIterable collectFloat(BooleanToFloatFunction function)
    {
        return new CollectBooleanToFloatIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyLongIterable collectLong(BooleanToLongFunction function)
    {
        return new CollectBooleanToLongIterable(this, function);
    }

    /**
     * @since 7.0
     */
    @Override
    public LazyDoubleIterable collectDouble(BooleanToDoubleFunction function)
    {
        return new CollectBooleanToDoubleIterable(this, function);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return BooleanIterableIterate.detectIfNone(this, predicate, ifNone);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return BooleanIterableIterate.count(this, predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return BooleanIterableIterate.anySatisfy(this, predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return BooleanIterableIterate.allSatisfy(this, predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return BooleanIterableIterate.noneSatisfy(this, predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return BooleanIterableIterate.injectInto(this, injectedValue, function);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return new ChunkBooleanIterable(this, size);
    }

    @Override
    public boolean[] toArray()
    {
        return this.toList().toArray();
    }

    @Override
    public MutableBooleanList toList()
    {
        final MutableBooleanList list = BooleanLists.mutable.empty();
        this.forEach(list::add);
        return list;
    }

    @Override
    public MutableBooleanSet toSet()
    {
        final MutableBooleanSet set = BooleanSets.mutable.empty();
        this.forEach(set::add);
        return set;
    }

    @Override
    public MutableBooleanBag toBag()
    {
        final MutableBooleanBag bag = BooleanBags.mutable.empty();
        this.forEach(bag::add);
        return bag;
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return this;
    }
}
