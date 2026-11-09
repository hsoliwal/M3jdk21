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

import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.iterator.IntIterator;

/**
 * The IntIteratorIterate class provides implementations of the various iteration patterns for use with the {@link IntIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class IntIteratorIterate
{
    private IntIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(IntIterator iterator, IntProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableIntCollection> R select(
            IntIterator iterator,
            IntPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            int item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableIntCollection> R reject(
            IntIterator iterator,
            IntPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            int item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            IntIterator iterator,
            IntToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            int item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static int detectIfNone(IntIterator iterator, IntPredicate predicate, int ifNone)
    {
        while (iterator.hasNext())
        {
            int item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(IntIterator iterator, IntPredicate predicate)
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

    public static boolean anySatisfy(IntIterator iterator, IntPredicate predicate)
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

    public static boolean allSatisfy(IntIterator iterator, IntPredicate predicate)
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

    public static boolean noneSatisfy(IntIterator iterator, IntPredicate predicate)
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

    public static <T> T injectInto(IntIterator iterator, T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static long sum(IntIterator iterator)
    {
        long sum = 0L;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static int max(IntIterator iterator)
    {
        int max = iterator.next();
        while (iterator.hasNext())
        {
            int next = iterator.next();
            if (max < next)
            {
                max = next;
            }
        }
        return max;
    }

    public static int min(IntIterator iterator)
    {
        int min = iterator.next();
        while (iterator.hasNext())
        {
            int next = iterator.next();
            if (next < min)
            {
                min = next;
            }
        }
        return min;
    }
}
