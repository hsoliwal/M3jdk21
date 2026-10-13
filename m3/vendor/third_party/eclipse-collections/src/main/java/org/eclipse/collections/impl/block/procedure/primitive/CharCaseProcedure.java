/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.procedure.primitive;

import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

/**
 * This file was automatically generated from template file primitiveCaseProcedure.stg.
 */
public class CharCaseProcedure implements CharProcedure
{
    private static final long serialVersionUID = 1L;

    private final MutableList<Pair<CharPredicate, CharProcedure>> predicateProcedures = Lists.mutable.empty();
    private CharProcedure defaultProcedure;

    public CharCaseProcedure()
    {
    }

    public CharCaseProcedure(CharProcedure newDefaultProcedure)
    {
        this.setDefault(newDefaultProcedure);
    }

    public CharCaseProcedure addCase(
            CharPredicate predicate,
            CharProcedure procedure)
    {
        this.predicateProcedures.add(Tuples.pair(predicate, procedure));
        return this;
    }

    public CharCaseProcedure setDefault(CharProcedure procedure)
    {
        this.defaultProcedure = procedure;
        return this;
    }

    @Override
    public void value(char argument)
    {
        int localSize = this.predicateProcedures.size();
        for (int i = 0; i < localSize; i++)
        {
            Pair<CharPredicate, CharProcedure> pair = this.predicateProcedures.get(i);
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
        return "new CharCaseProcedure(" + this.predicateProcedures + ')';
    }
}
