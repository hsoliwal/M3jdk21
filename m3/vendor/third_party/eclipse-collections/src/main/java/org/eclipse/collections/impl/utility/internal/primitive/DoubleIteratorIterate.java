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

import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;

/**
 * The DoubleIteratorIterate class provides implementations of the various iteration patterns for use with the {@link DoubleIterator}.
 * This file was automatically generated from template file primitiveIteratorIterate.stg.
 *
 * @since 5.0
 */
public final class DoubleIteratorIterate
{
    private DoubleIteratorIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void forEach(DoubleIterator iterator, DoubleProcedure procedure)
    {
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    public static <R extends MutableDoubleCollection> R select(
            DoubleIterator iterator,
            DoublePredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            double item = iterator.next();
            if (predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <R extends MutableDoubleCollection> R reject(
            DoubleIterator iterator,
            DoublePredicate predicate,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            double item = iterator.next();
            if (!predicate.accept(item))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    public static <V, R extends Collection<V>> R collect(
            DoubleIterator iterator,
            DoubleToObjectFunction<? extends V> function,
            R targetCollection)
    {
        while (iterator.hasNext())
        {
            double item = iterator.next();
            targetCollection.add(function.valueOf(item));
        }
        return targetCollection;
    }

    public static double detectIfNone(DoubleIterator iterator, DoublePredicate predicate, double ifNone)
    {
        while (iterator.hasNext())
        {
            double item = iterator.next();
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    public static int count(DoubleIterator iterator, DoublePredicate predicate)
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

    public static boolean anySatisfy(DoubleIterator iterator, DoublePredicate predicate)
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

    public static boolean allSatisfy(DoubleIterator iterator, DoublePredicate predicate)
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

    public static boolean noneSatisfy(DoubleIterator iterator, DoublePredicate predicate)
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

    public static <T> T injectInto(DoubleIterator iterator, T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }
        return result;
    }

    public static double sum(DoubleIterator iterator)
    {
        double sum = 0.0;
        while (iterator.hasNext())
        {
            sum += iterator.next();
        }
        return sum;
    }

    public static double max(DoubleIterator iterator)
    {
        double max = iterator.next();
        while (iterator.hasNext())
        {
            double next = iterator.next();
            if (Double.compare(max, next) < 0)
            {
                max = next;
            }
        }
        return max;
    }

    public static double min(DoubleIterator iterator)
    {
        double min = iterator.next();
        while (iterator.hasNext())
        {
            double next = iterator.next();
            if (Double.compare(next, min) < 0)
            {
                min = next;
            }
        }
        return min;
    }
}
