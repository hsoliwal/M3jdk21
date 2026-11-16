/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import java.util.Arrays;
import java.util.Iterator;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure2;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.factory.primitive.CharBags;
import jdk.m3.collections.eclipse.api.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.api.factory.primitive.CharSets;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
@SuppressWarnings("deprecation")
public class CollectCharIterable<T>
        extends AbstractLazyCharIterable
{
    private final RichIterable<T> iterable;
    private final CharFunction<? super T> function;
    private final CharFunctionToProcedure<T> charFunctionToProcedure;

    public CollectCharIterable(RichIterable<T> adapted, CharFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.charFunctionToProcedure = new CharFunctionToProcedure<>(function);
    }

    @Override
    public CharIterator charIterator()
    {
        return new CharIterator()
        {
            private final Iterator<T> iterator = CollectCharIterable.this.iterable.iterator();

            @Override
            public char next()
            {
                return CollectCharIterable.this.function.charValueOf(this.iterator.next());
            }

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }
        };
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.iterable.forEachWith(this.charFunctionToProcedure, procedure);
    }

    @Override
    public int size()
    {
        return this.iterable.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.iterable.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.iterable.notEmpty();
    }

    @Override
    public int count(final CharPredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.charValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final CharPredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.charValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final CharPredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.charValueOf(each)));
    }

    @Override
    public char[] toArray()
    {
        final char[] array = new char[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.charValueOf(each));
        return array;
    }

    @Override
    public char[] toSortedArray()
    {
        char[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableCharList toList()
    {
        return CharLists.mutable.withAll(this);
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharSets.mutable.withAll(this);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharBags.mutable.withAll(this);
    }

    @Override
    public boolean containsAll(char... source)
    {
        for (char value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        for (CharIterator iterator = source.charIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    private static final class CharFunctionToProcedure<T> implements Procedure2<T, CharProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final CharFunction<? super T> function;

        private CharFunctionToProcedure(CharFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, CharProcedure procedure)
        {
            procedure.value(this.function.charValueOf(each));
        }
    }
}
