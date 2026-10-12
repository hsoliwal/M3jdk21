/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import java.util.NoSuchElementException;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectCharIterable
        extends AbstractLazyCharIterable
{
    private final CharIterable delegate;
    private final CharPredicate predicate;

    public SelectCharIterable(CharIterable delegate, CharPredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public CharIterator charIterator()
    {
        return new SelectCharIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.delegate.forEach(new IfCharProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.charIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.charIterator().hasNext();
    }

    @Override
    public int count(CharPredicate predicate)
    {
        CountCharProcedure countCharProcedure = new CountCharProcedure(predicate);
        this.forEach(countCharProcedure);
        return countCharProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public char[] toArray()
    {
        final char[] array = new char[this.size()];
        this.forEach(new CharProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(char each)
            {
                array[this.index++] = each;
            }
        });
        return array;
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

    private static final class CountCharProcedure implements CharProcedure
    {
        private static final long serialVersionUID = 1L;
        private final CharPredicate predicate;
        private int counter = 0;

        private CountCharProcedure(CharPredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(char each)
        {
            if (this.predicate.accept(each))
            {
                this.counter++;
            }
        }

        public int getCount()
        {
            return this.counter;
        }
    }

    private final class IfCharProcedure implements CharProcedure
    {
        private static final long serialVersionUID = 1L;
        private final CharProcedure procedure;

        private IfCharProcedure(CharProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(char each)
        {
            if (SelectCharIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectCharIterator
            implements CharIterator
    {
        private final CharIterator iterator;
        private final CharPredicate predicate;
        private char next;
        private boolean verifiedHasNext = false;

        private SelectCharIterator(CharIterable iterable, CharPredicate predicate)
        {
            this(iterable.charIterator(), predicate);
        }

        private SelectCharIterator(CharIterator iterator, CharPredicate predicate)
        {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext()
        {
            if (this.verifiedHasNext)
            {
                return true;
            }
            while (this.iterator.hasNext())
            {
                char temp = this.iterator.next();
                if (this.predicate.accept(temp))
                {
                    this.next = temp;
                    this.verifiedHasNext = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public char next()
        {
            if (this.verifiedHasNext || this.hasNext())
            {
                this.verifiedHasNext = false;
                return this.next;
            }
            throw new NoSuchElementException();
        }
    }
}
