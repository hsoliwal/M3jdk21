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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.EmptyIterator;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.impl.block.procedure.AdaptObjectIntProcedureToProcedure;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * This file was automatically generated from template file flatCollectPrimitiveToObjectIterable.stg.
 */
public class FlatCollectBooleanToObjectIterable<V>
        extends AbstractLazyIterable<V>
{
    private final BooleanIterable iterable;
    private final BooleanToObjectFunction<? extends Iterable<V>> function;

    public FlatCollectBooleanToObjectIterable(
            BooleanIterable iterable,
            BooleanToObjectFunction<? extends Iterable<V>> function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(Procedure<? super V> procedure)
    {
        this.iterable.forEach((boolean each) -> Iterate.forEach(this.function.valueOf(each), procedure));
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super V> objectIntProcedure)
    {
        Procedure<V> innerProcedure = new AdaptObjectIntProcedureToProcedure<>(objectIntProcedure);
        this.iterable.forEach(each ->
        {
            Iterable<V> iterable = this.function.valueOf(each);
            Iterate.forEach(iterable, innerProcedure);
        });
    }

    @Override
    public <P> void forEachWith(Procedure2<? super V, ? super P> procedure, P parameter)
    {
        this.iterable.forEach((boolean each) -> Iterate.forEachWith(this.function.valueOf(each), procedure, parameter));
    }

    @Override
    public Iterator<V> iterator()
    {
        return new FlatCollectBooleanIterator<>(this.iterable, this.function);
    }

    @Override
    public V detect(Predicate<? super V> predicate)
    {
        V[] result = (V[]) new Object[1];
        this.iterable.anySatisfy(each ->
        {
            Iterable<V> it = this.function.valueOf(each);
            return Iterate.anySatisfy(it, each1 ->
            {
                if (predicate.accept(each1))
                {
                    result[0] = each1;
                    return true;
                }
                return false;
            });
        });
        return result[0];
    }

    @Override
    public Optional<V> detectOptional(Predicate<? super V> predicate)
    {
        V[] result = (V[]) new Object[1];
        this.iterable.anySatisfy(each ->
        {
            Iterable<V> it = this.function.valueOf(each);
            if (iterable == null)
            {
                throw new NullPointerException();
            }
            return Iterate.anySatisfy(it, each1 ->
            {
                if (predicate.accept(each1))
                {
                    if (each1 == null)
                    {
                        throw new NullPointerException();
                    }
                    result[0] = each1;
                    return true;
                }
                return false;
            });
        });
        return Optional.ofNullable(result[0]);
    }

    @Override
    public boolean anySatisfy(Predicate<? super V> predicate)
    {
        return this.iterable.anySatisfy(each -> Iterate.anySatisfy(this.function.valueOf(each), predicate));
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.anySatisfy(Predicates.bind(predicate, parameter));
    }

    @Override
    public boolean allSatisfy(Predicate<? super V> predicate)
    {
        return this.iterable.allSatisfy(each -> Iterate.allSatisfy(this.function.valueOf(each), predicate));
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.allSatisfy(Predicates.bind(predicate, parameter));
    }

    @Override
    public boolean noneSatisfy(Predicate<? super V> predicate)
    {
        return this.iterable.noneSatisfy(each -> Iterate.anySatisfy(this.function.valueOf(each), predicate));
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.noneSatisfy(Predicates.bind(predicate, parameter));
    }

    private final class FlatCollectBooleanIterator<V> implements Iterator<V>
    {
        private final BooleanIterator iterator;
        private final BooleanToObjectFunction<? extends Iterable<V>> function;
        private Iterator<V> innerIterator = EmptyIterator.getInstance();

        public FlatCollectBooleanIterator(
                BooleanIterable iterable,
                BooleanToObjectFunction<? extends Iterable<V>> newFunction)
        {
            this(iterable.booleanIterator(), newFunction);
        }

        public FlatCollectBooleanIterator(
                BooleanIterator newIterator,
                BooleanToObjectFunction<? extends Iterable<V>> newFunction)
        {
            this.iterator = newIterator;
            this.function = newFunction;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Cannot remove from a flatCollectBoolean iterator");
        }

        @Override
        public boolean hasNext()
        {
            while (true)
            {
                if (this.innerIterator.hasNext())
                {
                    return true;
                }
                if (!this.iterator.hasNext())
                {
                    return false;
                }
                this.innerIterator = this.function.valueOf(this.iterator.next()).iterator();
            }
        }

        @Override
        public V next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            return this.innerIterator.next();
        }
    }
}
