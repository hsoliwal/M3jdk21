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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class FloatIterableIterate
{
    private FloatIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(FloatIterable iterable)
    {
        return !iterable.floatIterator().hasNext();
    }

    public static boolean notEmpty(FloatIterable iterable)
    {
        return !FloatIterableIterate.isEmpty(iterable);
    }

    public static void forEach(FloatIterable iterable, FloatProcedure procedure)
    {
        FloatIteratorIterate.forEach(iterable.floatIterator(), procedure);
    }

    public static <R extends MutableFloatCollection> R select(FloatIterable iterable, FloatPredicate predicate, R targetCollection)
    {
        return FloatIteratorIterate.select(iterable.floatIterator(), predicate, targetCollection);
    }

    public static <R extends MutableFloatCollection> R reject(FloatIterable iterable, FloatPredicate predicate, R targetCollection)
    {
        return FloatIteratorIterate.reject(iterable.floatIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            FloatIterable iterable,
            FloatToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return FloatIteratorIterate.collect(iterable.floatIterator(), function, targetCollection);
    }

    public static float detectIfNone(FloatIterable iterable, FloatPredicate predicate, float ifNone)
    {
        return FloatIteratorIterate.detectIfNone(iterable.floatIterator(), predicate, ifNone);
    }

    public static int count(FloatIterable iterable, FloatPredicate predicate)
    {
        return FloatIteratorIterate.count(iterable.floatIterator(), predicate);
    }

    public static boolean anySatisfy(FloatIterable iterable, FloatPredicate predicate)
    {
        return FloatIteratorIterate.anySatisfy(iterable.floatIterator(), predicate);
    }

    public static boolean allSatisfy(FloatIterable iterable, FloatPredicate predicate)
    {
        return FloatIteratorIterate.allSatisfy(iterable.floatIterator(), predicate);
    }

    public static boolean noneSatisfy(FloatIterable iterable, FloatPredicate predicate)
    {
        return FloatIteratorIterate.noneSatisfy(iterable.floatIterator(), predicate);
    }

    public static double sum(FloatIterable iterable)
    {
        return FloatIteratorIterate.sum(iterable.floatIterator());
    }

    public static float max(FloatIterable iterable)
    {
        return FloatIteratorIterate.max(iterable.floatIterator());
    }

    public static float maxIfEmpty(FloatIterable iterable, float ifEmpty)
    {
        if (FloatIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return FloatIteratorIterate.max(iterable.floatIterator());
    }

    public static float min(FloatIterable iterable)
    {
        return FloatIteratorIterate.min(iterable.floatIterator());
    }

    public static float minIfEmpty(FloatIterable iterable, float ifEmpty)
    {
        if (FloatIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return FloatIteratorIterate.min(iterable.floatIterator());
    }

    public static void appendString(
            FloatIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            FloatIterator iterator = iterable.floatIterator();
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

    public static <T> T injectInto(FloatIterable iterable, T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return FloatIteratorIterate.injectInto(iterable.floatIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(FloatIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
