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

import java.util.Collection;

import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;

/**
 * The LongIteratorIterate class provides implementations of the various iteration patterns for use with the {@link LongIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class LongIteratorIterate
{
    private LongIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(LongIterator iterator, LongProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableLongCollection> R select(
            LongIterator iterator,
            LongPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            long item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableLongCollection> R reject(
            LongIterator iterator,
            LongPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            long item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            LongIterator iterator,
            LongToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            long item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static long detectIfNone(LongIterator iterator, LongPredicate predicate, long ifNone)
    {
        while (iterator.hasNext())
        {
            long item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(LongIterator iterator, LongPredicate predicate)
    {
        int count = 0;
        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                count++;
            }
        }
        return count;
    }

    public static boolean anySatisfy(LongIterator iterator, LongPredicate predicate)
    {
        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean allSatisfy(LongIterator iterator, LongPredicate predicate)
    {
        while (iterator.hasNext())
        {
            if (!predicate.accept(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean noneSatisfy(LongIterator iterator, LongPredicate predicate)
    {
        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    public static <T> T injectInto(LongIterator iterator, T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static long sum(LongIterator iterator)
    {
        long sum = 0L;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static long max(LongIterator iterator)
    {
        long max = iterator.next();
        while (iterator.hasNext())
        {
            long next = iterator.next();
            if (max < next)
            {
                max = next;
            }
        }
        return max;
    }

    public static long min(LongIterator iterator)
    {
        long min = iterator.next();
        while (iterator.hasNext())
        {
            long next = iterator.next();
            if (next < min)
            {
                min = next;
            }
        }
        return min;
    }
}
