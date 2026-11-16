/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.block.procedure.primitive;

import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.Pair;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.tuple.Tuples;

/**
 * This file was automatically generated from template file primitiveCaseProcedure.stg.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ShortCaseProcedure implements ShortProcedure
{
    private static final long serialVersionUID = 1L;

    private final MutableList<Pair<ShortPredicate, ShortProcedure>> predicateProcedures = Lists.mutable.empty();
    private ShortProcedure defaultProcedure;

    public ShortCaseProcedure()
    {
    }

    public ShortCaseProcedure(ShortProcedure newDefaultProcedure)
    {
        this.setDefault(newDefaultProcedure);
    }

    public ShortCaseProcedure addCase(
            ShortPredicate predicate,
            ShortProcedure procedure)
    {
        this.predicateProcedures.add(Tuples.pair(predicate, procedure));
        return this;
    }

    public ShortCaseProcedure setDefault(ShortProcedure procedure)
    {
        this.defaultProcedure = procedure;
        return this;
    }

    @Override
    public void value(short argument)
    {
        int localSize = this.predicateProcedures.size();
        for (int i = 0; i < localSize; i++)
        {
            Pair<ShortPredicate, ShortProcedure> pair = this.predicateProcedures.get(i);
            if (pair.getOne().accept(argument))
            {
                pair.getTwo().value(argument);
                return;
            }
        }

        if (this.defaultProcedure != null)
        {
            this.defaultProcedure.value(argument);
        }
    }

    @Override
    public String toString()
    {
        return "new ShortCaseProcedure(" + this.predicateProcedures + ')';
    }
}
