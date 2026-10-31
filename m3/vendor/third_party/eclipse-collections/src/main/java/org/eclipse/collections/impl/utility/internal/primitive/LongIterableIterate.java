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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class LongIterableIterate
{
    private LongIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(LongIterable iterable)
    {
        return !iterable.longIterator().hasNext();
    }

    public static boolean notEmpty(LongIterable iterable)
    {
        return !LongIterableIterate.isEmpty(iterable);
    }

    public static void forEach(LongIterable iterable, LongProcedure procedure)
    {
        LongIteratorIterate.forEach(iterable.longIterator(), procedure);
    }

    public static <R extends MutableLongCollection> R select(LongIterable iterable, LongPredicate predicate, R targetCollection)
    {
        return LongIteratorIterate.select(iterable.longIterator(), predicate, targetCollection);
    }

    public static <R extends MutableLongCollection> R reject(LongIterable iterable, LongPredicate predicate, R targetCollection)
    {
        return LongIteratorIterate.reject(iterable.longIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            LongIterable iterable,
            LongToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return LongIteratorIterate.collect(iterable.longIterator(), function, targetCollection);
    }

    public static long detectIfNone(LongIterable iterable, LongPredicate predicate, long ifNone)
    {
        return LongIteratorIterate.detectIfNone(iterable.longIterator(), predicate, ifNone);
    }

    public static int count(LongIterable iterable, LongPredicate predicate)
    {
        return LongIteratorIterate.count(iterable.longIterator(), predicate);
    }

    public static boolean anySatisfy(LongIterable iterable, LongPredicate predicate)
    {
        return LongIteratorIterate.anySatisfy(iterable.longIterator(), predicate);
    }

    public static boolean allSatisfy(LongIterable iterable, LongPredicate predicate)
    {
        return LongIteratorIterate.allSatisfy(iterable.longIterator(), predicate);
    }

    public static boolean noneSatisfy(LongIterable iterable, LongPredicate predicate)
    {
        return LongIteratorIterate.noneSatisfy(iterable.longIterator(), predicate);
    }

    public static long sum(LongIterable iterable)
    {
        return LongIteratorIterate.sum(iterable.longIterator());
    }

    public static long max(LongIterable iterable)
    {
        return LongIteratorIterate.max(iterable.longIterator());
    }

    public static long maxIfEmpty(LongIterable iterable, long ifEmpty)
    {
        if (LongIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return LongIteratorIterate.max(iterable.longIterator());
    }

    public static long min(LongIterable iterable)
    {
        return LongIteratorIterate.min(iterable.longIterator());
    }

    public static long minIfEmpty(LongIterable iterable, long ifEmpty)
    {
        if (LongIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return LongIteratorIterate.min(iterable.longIterator());
    }

    public static void appendString(
            LongIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            LongIterator iterator = iterable.longIterator();
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

    public static <T> T injectInto(LongIterable iterable, T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return LongIteratorIterate.injectInto(iterable.longIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(LongIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
