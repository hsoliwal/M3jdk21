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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class BooleanIterableIterate
{
    private BooleanIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(BooleanIterable iterable)
    {
        return !iterable.booleanIterator().hasNext();
    }

    public static boolean notEmpty(BooleanIterable iterable)
    {
        return !BooleanIterableIterate.isEmpty(iterable);
    }

    public static void forEach(BooleanIterable iterable, BooleanProcedure procedure)
    {
        BooleanIteratorIterate.forEach(iterable.booleanIterator(), procedure);
    }

    public static <R extends MutableBooleanCollection> R select(BooleanIterable iterable, BooleanPredicate predicate, R targetCollection)
    {
        return BooleanIteratorIterate.select(iterable.booleanIterator(), predicate, targetCollection);
    }

    public static <R extends MutableBooleanCollection> R reject(BooleanIterable iterable, BooleanPredicate predicate, R targetCollection)
    {
        return BooleanIteratorIterate.reject(iterable.booleanIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            BooleanIterable iterable,
            BooleanToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return BooleanIteratorIterate.collect(iterable.booleanIterator(), function, targetCollection);
    }

    public static boolean detectIfNone(BooleanIterable iterable, BooleanPredicate predicate, boolean ifNone)
    {
        return BooleanIteratorIterate.detectIfNone(iterable.booleanIterator(), predicate, ifNone);
    }

    public static int count(BooleanIterable iterable, BooleanPredicate predicate)
    {
        return BooleanIteratorIterate.count(iterable.booleanIterator(), predicate);
    }

    public static boolean anySatisfy(BooleanIterable iterable, BooleanPredicate predicate)
    {
        return BooleanIteratorIterate.anySatisfy(iterable.booleanIterator(), predicate);
    }

    public static boolean allSatisfy(BooleanIterable iterable, BooleanPredicate predicate)
    {
        return BooleanIteratorIterate.allSatisfy(iterable.booleanIterator(), predicate);
    }

    public static boolean noneSatisfy(BooleanIterable iterable, BooleanPredicate predicate)
    {
        return BooleanIteratorIterate.noneSatisfy(iterable.booleanIterator(), predicate);
    }

    public static void appendString(
            BooleanIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            BooleanIterator iterator = iterable.booleanIterator();
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

    public static <T> T injectInto(BooleanIterable iterable, T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return BooleanIteratorIterate.injectInto(iterable.booleanIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(BooleanIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
