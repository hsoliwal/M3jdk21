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

import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.Pair;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.tuple.Tuples;

/**
 * This file was automatically generated from template file primitiveCaseProcedure.stg.
 */
@SuppressWarnings({"serial", "this-escape"})
public class LongCaseProcedure implements LongProcedure
{
    private static final long serialVersionUID = 1L;

    private final MutableList<Pair<LongPredicate, LongProcedure>> predicateProcedures = Lists.mutable.empty();
    private LongProcedure defaultProcedure;

    public LongCaseProcedure()
    {
    }

    public LongCaseProcedure(LongProcedure newDefaultProcedure)
    {
        this.setDefault(newDefaultProcedure);
    }

    public LongCaseProcedure addCase(
            LongPredicate predicate,
            LongProcedure procedure)
    {
        this.predicateProcedures.add(Tuples.pair(predicate, procedure));
        return this;
    }

    public LongCaseProcedure setDefault(LongProcedure procedure)
    {
        this.defaultProcedure = procedure;
        return this;
    }

    @Override
    public void value(long argument)
    {
        int localSize = this.predicateProcedures.size();
        for (int i = 0; i < localSize; i++)
        {
            Pair<LongPredicate, LongProcedure> pair = this.predicateProcedures.get(i);
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
        return "new LongCaseProcedure(" + this.predicateProcedures + ')';
    }
}
