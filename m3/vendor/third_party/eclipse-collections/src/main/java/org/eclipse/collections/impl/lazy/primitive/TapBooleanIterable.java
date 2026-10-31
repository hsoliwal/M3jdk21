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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;

/**
 * This file was automatically generated from template file tapPrimitiveIterable.stg.
 */
public class TapBooleanIterable
        extends AbstractLazyBooleanIterable
{
    private final BooleanIterable adapted;
    private final BooleanProcedure procedure;

    public TapBooleanIterable(BooleanIterable delegate, BooleanProcedure procedure)
    {
        this.adapted = delegate;
        this.procedure = procedure;
    }

    @Override
    public void each(BooleanProcedure procedure)
    {
        this.adapted.forEach(each ->
        {
            this.procedure.value(each);
            procedure.value(each);
        });
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.adapted.anySatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.adapted.allSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.adapted.noneSatisfy(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        });
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.adapted.detectIfNone(each ->
        {
            this.procedure.value(each);
            return predicate.accept(each);
        }, ifNone);
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new TapBooleanIterator(this.adapted, this.procedure);
    }

    public static class TapBooleanIterator implements BooleanIterator
    {
        private final BooleanIterator iterator;
        private final BooleanProcedure procedure;

        public TapBooleanIterator(BooleanIterable iterable, BooleanProcedure procedure)
        {
            this(iterable.booleanIterator(), procedure);
        }

        public TapBooleanIterator(BooleanIterator iterator, BooleanProcedure procedure)
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
        public boolean next()
        {
            boolean next = this.iterator.next();
            this.procedure.value(next);
            return next;
        }
    }
}
