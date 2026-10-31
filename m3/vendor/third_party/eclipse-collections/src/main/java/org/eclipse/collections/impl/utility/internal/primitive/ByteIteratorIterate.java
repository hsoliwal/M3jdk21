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

import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.ByteIterator;

/**
 * The ByteIteratorIterate class provides implementations of the various iteration patterns for use with the {@link ByteIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class ByteIteratorIterate
{
    private ByteIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(ByteIterator iterator, ByteProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableByteCollection> R select(
            ByteIterator iterator,
            BytePredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            byte item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableByteCollection> R reject(
            ByteIterator iterator,
            BytePredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            byte item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            ByteIterator iterator,
            ByteToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            byte item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static byte detectIfNone(ByteIterator iterator, BytePredicate predicate, byte ifNone)
    {
        while (iterator.hasNext())
        {
            byte item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(ByteIterator iterator, BytePredicate predicate)
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

    public static boolean anySatisfy(ByteIterator iterator, BytePredicate predicate)
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

    public static boolean allSatisfy(ByteIterator iterator, BytePredicate predicate)
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

    public static boolean noneSatisfy(ByteIterator iterator, BytePredicate predicate)
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

    public static <T> T injectInto(ByteIterator iterator, T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static long sum(ByteIterator iterator)
    {
        long sum = 0L;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static byte max(ByteIterator iterator)
    {
        byte max = iterator.next();
        while (iterator.hasNext())
        {
            byte next = iterator.next();
            if (max < next)
            {
                max = next;
            }
        }
        return max;
    }

    public static byte min(ByteIterator iterator)
    {
        byte min = iterator.next();
        while (iterator.hasNext())
        {
            byte next = iterator.next();
            if (next < min)
            {
                min = next;
            }
        }
        return min;
    }
}
