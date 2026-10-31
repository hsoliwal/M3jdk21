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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.ByteIterator;

/**
 * This file was automatically generated from template file primitiveIterableIterate.stg.
 *
 * @since 5.0
 */
public final class ByteIterableIterate
{
    private ByteIterableIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static boolean isEmpty(ByteIterable iterable)
    {
        return !iterable.byteIterator().hasNext();
    }

    public static boolean notEmpty(ByteIterable iterable)
    {
        return !ByteIterableIterate.isEmpty(iterable);
    }

    public static void forEach(ByteIterable iterable, ByteProcedure procedure)
    {
        ByteIteratorIterate.forEach(iterable.byteIterator(), procedure);
    }

    public static <R extends MutableByteCollection> R select(ByteIterable iterable, BytePredicate predicate, R targetCollection)
    {
        return ByteIteratorIterate.select(iterable.byteIterator(), predicate, targetCollection);
    }

    public static <R extends MutableByteCollection> R reject(ByteIterable iterable, BytePredicate predicate, R targetCollection)
    {
        return ByteIteratorIterate.reject(iterable.byteIterator(), predicate, targetCollection);
    }

    public static <V, R extends Collection<V>> R collect(
            ByteIterable iterable,
            ByteToObjectFunction<? extends V> function,
            R targetCollection)
    {
        return ByteIteratorIterate.collect(iterable.byteIterator(), function, targetCollection);
    }

    public static byte detectIfNone(ByteIterable iterable, BytePredicate predicate, byte ifNone)
    {
        return ByteIteratorIterate.detectIfNone(iterable.byteIterator(), predicate, ifNone);
    }

    public static int count(ByteIterable iterable, BytePredicate predicate)
    {
        return ByteIteratorIterate.count(iterable.byteIterator(), predicate);
    }

    public static boolean anySatisfy(ByteIterable iterable, BytePredicate predicate)
    {
        return ByteIteratorIterate.anySatisfy(iterable.byteIterator(), predicate);
    }

    public static boolean allSatisfy(ByteIterable iterable, BytePredicate predicate)
    {
        return ByteIteratorIterate.allSatisfy(iterable.byteIterator(), predicate);
    }

    public static boolean noneSatisfy(ByteIterable iterable, BytePredicate predicate)
    {
        return ByteIteratorIterate.noneSatisfy(iterable.byteIterator(), predicate);
    }

    public static long sum(ByteIterable iterable)
    {
        return ByteIteratorIterate.sum(iterable.byteIterator());
    }

    public static byte max(ByteIterable iterable)
    {
        return ByteIteratorIterate.max(iterable.byteIterator());
    }

    public static byte maxIfEmpty(ByteIterable iterable, byte ifEmpty)
    {
        if (ByteIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return ByteIteratorIterate.max(iterable.byteIterator());
    }

    public static byte min(ByteIterable iterable)
    {
        return ByteIteratorIterate.min(iterable.byteIterator());
    }

    public static byte minIfEmpty(ByteIterable iterable, byte ifEmpty)
    {
        if (ByteIterableIterate.isEmpty(iterable))
        {
            return ifEmpty;
        }
        return ByteIteratorIterate.min(iterable.byteIterator());
    }

    public static void appendString(
            ByteIterable iterable,
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);

            ByteIterator iterator = iterable.byteIterator();
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

    public static <T> T injectInto(ByteIterable iterable, T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return ByteIteratorIterate.injectInto(iterable.byteIterator(), injectedValue, function);
    }

    private static <T> String stringValueOfItem(ByteIterable iterable, T item)
    {
        return item == iterable
                ? "(this " + iterable.getClass().getSimpleName() + ')'
                : String.valueOf(item);
    }
}
