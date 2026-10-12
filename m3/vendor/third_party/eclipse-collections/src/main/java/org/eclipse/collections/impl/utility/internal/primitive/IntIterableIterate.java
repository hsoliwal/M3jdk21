/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.utility.internal.primitive;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.iterator.IntIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class IntIterableIterate
{
    private IntIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(IntIterable iterable)
    {
        return !iterable.intIterator().hasNext();
    }

    public static boolean notEmpty(IntIterable iterable)
    {
        return !IntIterableIterate.isEmpty(iterable);
    }

    public static void forEach(IntIterable iterable, IntProcedure procedure)
    {
        IntIteratorIterate.forEach(iterable.intIterator(), procedure);
    }

    public static <R extends MutableIntCollection> R select(IntIterable iterable, IntPredicate predicate, R targetCollection)
    {
        return IntIteratorIterate.select(iterable.intIterator(), predicate, targetCollection);
    }

    public static <R extends MutableIntCollection> R reject(IntIterable iterable, IntPredicate predicate, R targetCollection)
    {
        return IntIteratorIterate.reject(iterable.intIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            IntIterable iterable,
            IntToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return IntIteratorIterate.collect(iterable.intIterator(), function, targetCollection);
    }

    public static int detectIfNone(IntIterable iterable, IntPredicate predicate, int ifNone)
    {
        return IntIteratorIterate.detectIfNone(iterable.intIterator(), predicate, ifNone);
    }

    public static int count(IntIterable iterable, IntPredicate predicate)
    {
        return IntIteratorIterate.count(iterable.intIterator(), predicate);
    }

    public static boolean anySatisfy(IntIterable iterable, IntPredicate predicate)
    {
        return IntIteratorIterate.anySatisfy(iterable.intIterator(), predicate);
    }

    public static boolean allSatisfy(IntIterable iterable, IntPredicate predicate)
    {
        return IntIteratorIterate.allSatisfy(iterable.intIterator(), predicate);
    }

    public static boolean noneSatisfy(IntIterable iterable, IntPredicate predicate)
    {
        return IntIteratorIterate.noneSatisfy(iterable.intIterator(), predicate);
    }

    public static long sum(IntIterable iterable)
    {
        return IntIteratorIterate.sum(iterable.intIterator());
    }

    public static int max(IntIterable iterable)
    {
        return IntIteratorIterate.max(iterable.intIterator());
    }

    public static int maxIfEmpty(IntIterable iterable, int ifEmpty)
    {
        if (IntIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return IntIteratorIterate.max(iterable.intIterator());
    }

    public static int min(IntIterable iterable)
    {
        return IntIteratorIterate.min(iterable.intIterator());
    }

    public static int minIfEmpty(IntIterable iterable, int ifEmpty)
    {
        if (IntIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return IntIteratorIterate.min(iterable.intIterator());
    }

    public static void appendString(
            IntIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            IntIterator iterator = iterable.intIterator();
            if (iterator.hasNext())
            {
                appendable.append(stringValueOfItem(iterable, iterator.next()));
                while (iterator.hasNext())
                {
                    appendable.append(separator);
                    appendable.append(stringValueOfItem(iterable, iterator.next()));
                }
            }

            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T injectInto(IntIterable iterable, T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return IntIteratorIterate.injectInto(iterable.intIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(IntIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
