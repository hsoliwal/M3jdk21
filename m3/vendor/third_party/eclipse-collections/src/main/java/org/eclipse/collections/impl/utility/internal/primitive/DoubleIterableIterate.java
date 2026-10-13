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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class DoubleIterableIterate
{
    private DoubleIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(DoubleIterable iterable)
    {
        return !iterable.doubleIterator().hasNext();
    }

    public static boolean notEmpty(DoubleIterable iterable)
    {
        return !DoubleIterableIterate.isEmpty(iterable);
    }

    public static void forEach(DoubleIterable iterable, DoubleProcedure procedure)
    {
        DoubleIteratorIterate.forEach(iterable.doubleIterator(), procedure);
    }

    public static <R extends MutableDoubleCollection> R select(DoubleIterable iterable, DoublePredicate predicate, R targetCollection)
    {
        return DoubleIteratorIterate.select(iterable.doubleIterator(), predicate, targetCollection);
    }

    public static <R extends MutableDoubleCollection> R reject(DoubleIterable iterable, DoublePredicate predicate, R targetCollection)
    {
        return DoubleIteratorIterate.reject(iterable.doubleIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            DoubleIterable iterable,
            DoubleToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return DoubleIteratorIterate.collect(iterable.doubleIterator(), function, targetCollection);
    }

    public static double detectIfNone(DoubleIterable iterable, DoublePredicate predicate, double ifNone)
    {
        return DoubleIteratorIterate.detectIfNone(iterable.doubleIterator(), predicate, ifNone);
    }

    public static int count(DoubleIterable iterable, DoublePredicate predicate)
    {
        return DoubleIteratorIterate.count(iterable.doubleIterator(), predicate);
    }

    public static boolean anySatisfy(DoubleIterable iterable, DoublePredicate predicate)
    {
        return DoubleIteratorIterate.anySatisfy(iterable.doubleIterator(), predicate);
    }

    public static boolean allSatisfy(DoubleIterable iterable, DoublePredicate predicate)
    {
        return DoubleIteratorIterate.allSatisfy(iterable.doubleIterator(), predicate);
    }

    public static boolean noneSatisfy(DoubleIterable iterable, DoublePredicate predicate)
    {
        return DoubleIteratorIterate.noneSatisfy(iterable.doubleIterator(), predicate);
    }

    public static double sum(DoubleIterable iterable)
    {
        return DoubleIteratorIterate.sum(iterable.doubleIterator());
    }

    public static double max(DoubleIterable iterable)
    {
        return DoubleIteratorIterate.max(iterable.doubleIterator());
    }

    public static double maxIfEmpty(DoubleIterable iterable, double ifEmpty)
    {
        if (DoubleIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return DoubleIteratorIterate.max(iterable.doubleIterator());
    }

    public static double min(DoubleIterable iterable)
    {
        return DoubleIteratorIterate.min(iterable.doubleIterator());
    }

    public static double minIfEmpty(DoubleIterable iterable, double ifEmpty)
    {
        if (DoubleIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return DoubleIteratorIterate.min(iterable.doubleIterator());
    }

    public static void appendString(
            DoubleIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            DoubleIterator iterator = iterable.doubleIterator();
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

    public static <T> T injectInto(DoubleIterable iterable, T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return DoubleIteratorIterate.injectInto(iterable.doubleIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(DoubleIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
