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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapByteIterable
        extends AbstractLazyByteIterable
{
    private final ByteIterable adapted;
    private final ByteProcedure procedure;

    public TapByteIterable(ByteIterable delegate, ByteProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(ByteProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new TapByteIterator(this.adapted, this.procedure);
    }

    public static class TapByteIterator implements ByteIterator
    {
        private final ByteIterator iterator;
        private final ByteProcedure procedure;

        public TapByteIterator(ByteIterable iterable, ByteProcedure procedure)
        {
            this(iterable.byteIterator(), procedure);
        }

        public TapByteIterator(ByteIterator iterator, ByteProcedure procedure)
        {
            this.iterator = iterator;
            this.procedure = procedure;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public byte next()
        {
            byte next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
