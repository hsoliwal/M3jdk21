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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.ShortIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class ShortIterableIterate
{
    private ShortIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(ShortIterable iterable)
    {
        return !iterable.shortIterator().hasNext();
    }

    public static boolean notEmpty(ShortIterable iterable)
    {
        return !ShortIterableIterate.isEmpty(iterable);
    }

    public static void forEach(ShortIterable iterable, ShortProcedure procedure)
    {
        ShortIteratorIterate.forEach(iterable.shortIterator(), procedure);
    }

    public static <R extends MutableShortCollection> R select(ShortIterable iterable, ShortPredicate predicate, R targetCollection)
    {
        return ShortIteratorIterate.select(iterable.shortIterator(), predicate, targetCollection);
    }

    public static <R extends MutableShortCollection> R reject(ShortIterable iterable, ShortPredicate predicate, R targetCollection)
    {
        return ShortIteratorIterate.reject(iterable.shortIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            ShortIterable iterable,
            ShortToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return ShortIteratorIterate.collect(iterable.shortIterator(), function, targetCollection);
    }

    public static short detectIfNone(ShortIterable iterable, ShortPredicate predicate, short ifNone)
    {
        return ShortIteratorIterate.detectIfNone(iterable.shortIterator(), predicate, ifNone);
    }

    public static int count(ShortIterable iterable, ShortPredicate predicate)
    {
        return ShortIteratorIterate.count(iterable.shortIterator(), predicate);
    }

    public static boolean anySatisfy(ShortIterable iterable, ShortPredicate predicate)
    {
        return ShortIteratorIterate.anySatisfy(iterable.shortIterator(), predicate);
    }

    public static boolean allSatisfy(ShortIterable iterable, ShortPredicate predicate)
    {
        return ShortIteratorIterate.allSatisfy(iterable.shortIterator(), predicate);
    }

    public static boolean noneSatisfy(ShortIterable iterable, ShortPredicate predicate)
    {
        return ShortIteratorIterate.noneSatisfy(iterable.shortIterator(), predicate);
    }

    public static long sum(ShortIterable iterable)
    {
        return ShortIteratorIterate.sum(iterable.shortIterator());
    }

    public static short max(ShortIterable iterable)
    {
        return ShortIteratorIterate.max(iterable.shortIterator());
    }

    public static short maxIfEmpty(ShortIterable iterable, short ifEmpty)
    {
        if (ShortIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return ShortIteratorIterate.max(iterable.shortIterator());
    }

    public static short min(ShortIterable iterable)
    {
        return ShortIteratorIterate.min(iterable.shortIterator());
    }

    public static short minIfEmpty(ShortIterable iterable, short ifEmpty)
    {
        if (ShortIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return ShortIteratorIterate.min(iterable.shortIterator());
    }

    public static void appendString(
            ShortIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            ShortIterator iterator = iterable.shortIterator();
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

    public static <T> T injectInto(ShortIterable iterable, T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return ShortIteratorIterate.injectInto(iterable.shortIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(ShortIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
