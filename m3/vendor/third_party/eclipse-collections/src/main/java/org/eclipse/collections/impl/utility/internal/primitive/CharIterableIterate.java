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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.CharIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class CharIterableIterate
{
    private CharIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(CharIterable iterable)
    {
        return !iterable.charIterator().hasNext();
    }

    public static boolean notEmpty(CharIterable iterable)
    {
        return !CharIterableIterate.isEmpty(iterable);
    }

    public static void forEach(CharIterable iterable, CharProcedure procedure)
    {
        CharIteratorIterate.forEach(iterable.charIterator(), procedure);
    }

    public static <R extends MutableCharCollection> R select(CharIterable iterable, CharPredicate predicate, R targetCollection)
    {
        return CharIteratorIterate.select(iterable.charIterator(), predicate, targetCollection);
    }

    public static <R extends MutableCharCollection> R reject(CharIterable iterable, CharPredicate predicate, R targetCollection)
    {
        return CharIteratorIterate.reject(iterable.charIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            CharIterable iterable,
            CharToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return CharIteratorIterate.collect(iterable.charIterator(), function, targetCollection);
    }

    public static char detectIfNone(CharIterable iterable, CharPredicate predicate, char ifNone)
    {
        return CharIteratorIterate.detectIfNone(iterable.charIterator(), predicate, ifNone);
    }

    public static int count(CharIterable iterable, CharPredicate predicate)
    {
        return CharIteratorIterate.count(iterable.charIterator(), predicate);
    }

    public static boolean anySatisfy(CharIterable iterable, CharPredicate predicate)
    {
        return CharIteratorIterate.anySatisfy(iterable.charIterator(), predicate);
    }

    public static boolean allSatisfy(CharIterable iterable, CharPredicate predicate)
    {
        return CharIteratorIterate.allSatisfy(iterable.charIterator(), predicate);
    }

    public static boolean noneSatisfy(CharIterable iterable, CharPredicate predicate)
    {
        return CharIteratorIterate.noneSatisfy(iterable.charIterator(), predicate);
    }

    public static long sum(CharIterable iterable)
    {
        return CharIteratorIterate.sum(iterable.charIterator());
    }

    public static char max(CharIterable iterable)
    {
        return CharIteratorIterate.max(iterable.charIterator());
    }

    public static char maxIfEmpty(CharIterable iterable, char ifEmpty)
    {
        if (CharIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return CharIteratorIterate.max(iterable.charIterator());
    }

    public static char min(CharIterable iterable)
    {
        return CharIteratorIterate.min(iterable.charIterator());
    }

    public static char minIfEmpty(CharIterable iterable, char ifEmpty)
    {
        if (CharIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return CharIteratorIterate.min(iterable.charIterator());
    }

    public static void appendString(
            CharIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            CharIterator iterator = iterable.charIterator();
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

    public static <T> T injectInto(CharIterable iterable, T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return CharIteratorIterate.injectInto(iterable.charIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(CharIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
