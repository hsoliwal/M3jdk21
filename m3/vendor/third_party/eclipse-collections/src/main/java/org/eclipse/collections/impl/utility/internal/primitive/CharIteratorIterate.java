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

import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.CharIterator;

/**
 * The CharIteratorIterate class provides implementations of the various iteration patterns for use with the {@link CharIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class CharIteratorIterate
{
    private CharIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(CharIterator iterator, CharProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableCharCollection> R select(
            CharIterator iterator,
            CharPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            char item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableCharCollection> R reject(
            CharIterator iterator,
            CharPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            char item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            CharIterator iterator,
            CharToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            char item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static char detectIfNone(CharIterator iterator, CharPredicate predicate, char ifNone)
    {
        while (iterator.hasNext())
        {
            char item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(CharIterator iterator, CharPredicate predicate)
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

    public static boolean anySatisfy(CharIterator iterator, CharPredicate predicate)
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

    public static boolean allSatisfy(CharIterator iterator, CharPredicate predicate)
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

    public static boolean noneSatisfy(CharIterator iterator, CharPredicate predicate)
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

    public static <T> T injectInto(CharIterator iterator, T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static long sum(CharIterator iterator)
    {
        long sum = 0L;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static char max(CharIterator iterator)
    {
        char max = iterator.next();
        while (iterator.hasNext())
        {
            char next = iterator.next();
            if (max < next)
            {
                max = next;
            }
        }
        return max;
    }

    public static char min(CharIterator iterator)
    {
        char min = iterator.next();
        while (iterator.hasNext())
        {
            char next = iterator.next();
            if (next < min)
            {
                min = next;
            }
        }
        return min;
    }
}
