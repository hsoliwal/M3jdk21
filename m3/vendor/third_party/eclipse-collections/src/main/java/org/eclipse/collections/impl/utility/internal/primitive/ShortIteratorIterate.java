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

import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.ShortIterator;

/**
 * The ShortIteratorIterate class provides implementations of the various iteration patterns for use with the {@link ShortIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class ShortIteratorIterate
{
    private ShortIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(ShortIterator iterator, ShortProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableShortCollection> R select(
            ShortIterator iterator,
            ShortPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            short item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableShortCollection> R reject(
            ShortIterator iterator,
            ShortPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            short item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            ShortIterator iterator,
            ShortToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            short item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static short detectIfNone(ShortIterator iterator, ShortPredicate predicate, short ifNone)
    {
        while (iterator.hasNext())
        {
            short item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(ShortIterator iterator, ShortPredicate predicate)
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

    public static boolean anySatisfy(ShortIterator iterator, ShortPredicate predicate)
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

    public static boolean allSatisfy(ShortIterator iterator, ShortPredicate predicate)
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

    public static boolean noneSatisfy(ShortIterator iterator, ShortPredicate predicate)
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

    public static <T> T injectInto(ShortIterator iterator, T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static long sum(ShortIterator iterator)
    {
        long sum = 0L;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static short max(ShortIterator iterator)
    {
        short max = iterator.next();
        while (iterator.hasNext())
        {
            short next = iterator.next();
            if (max < next)
            {
                max = next;
            }
        }
        return max;
    }

    public static short min(ShortIterator iterator)
    {
        short min = iterator.next();
        while (iterator.hasNext())
        {
            short next = iterator.next();
            if (next < min)
            {
                min = next;
            }
        }
        return min;
    }
}
