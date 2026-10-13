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

import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;

/**
 * The FloatIteratorIterate class provides implementations of the various iteration patterns for use with the {@link FloatIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class FloatIteratorIterate
{
    private FloatIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(FloatIterator iterator, FloatProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableFloatCollection> R select(
            FloatIterator iterator,
            FloatPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            float item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableFloatCollection> R reject(
            FloatIterator iterator,
            FloatPredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            float item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            FloatIterator iterator,
            FloatToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            float item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static float detectIfNone(FloatIterator iterator, FloatPredicate predicate, float ifNone)
    {
        while (iterator.hasNext())
        {
            float item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(FloatIterator iterator, FloatPredicate predicate)
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

    public static boolean anySatisfy(FloatIterator iterator, FloatPredicate predicate)
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

    public static boolean allSatisfy(FloatIterator iterator, FloatPredicate predicate)
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

    public static boolean noneSatisfy(FloatIterator iterator, FloatPredicate predicate)
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

    public static <T> T injectInto(FloatIterator iterator, T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static double sum(FloatIterator iterator)
    {
        double sum = 0.0;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static float max(FloatIterator iterator)
    {
        float max = iterator.next();
        while (iterator.hasNext())
        {
            float next = iterator.next();
            if (Float.compare(max, next) < 0)
            {
                max = next;
            }
        }
        return max;
    }

    public static float min(FloatIterator iterator)
    {
        float min = iterator.next();
        while (iterator.hasNext())
        {
            float next = iterator.next();
            if (Float.compare(next, min) < 0)
            {
                min = next;
            }
        }
        return min;
    }
}
