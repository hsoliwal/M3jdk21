// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static jdk.m3.collections.M3PolicyAssertions.*;
import jdk.internal.m3.collections.M3BoundedLongSequence;
import jdk.internal.m3.collections.M3LongSequenceMetrics;
import jdk.internal.m3.collections.M3ObservedLongSequence;
import jdk.internal.m3.collections.M3ValidatingLongSequence;

//FLLDCIM: RELOCATE - donor blob 70f63c4dac4b at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3LongSequencePolicyFailureProof {
    public static void main(String[] args) {
        M3PolicyProbe probe=new M3PolicyProbe();
        RuntimeException callerError=new IllegalStateException("caller");
        RuntimeException validatorError=new IllegalArgumentException("validator");
        M3LongSequence validator=new M3ValidatingLongSequence(probe,v->{
            if(v==9)throw validatorError;
            return v>=0;
        });
        M3LongSequence bounded=new M3BoundedLongSequence(validator,1);
        same(callerError,fails(RuntimeException.class,
                ()->bounded.addIf(3,v->{throw callerError;})));
        same(validatorError,fails(RuntimeException.class,
                ()->bounded.addIf(9,v->true)));
        values(probe);
        eq(0,probe.adds);
        bounded.add(1);
        eq(1,probe.adds);
        fails(IllegalStateException.class,()->bounded.addAll(new long[]{2,3}));
        eq(0,probe.bulks);
        values(probe,1);
        M3LongSequenceMetrics metrics=new M3LongSequenceMetrics();
        M3ObservedLongSequence observed=new M3ObservedLongSequence(bounded,metrics);
        observed.addAll(new long[0]);
        eq(1,metrics.invocations());
        eq(1,probe.bulks);
        eq(1,metrics.successes());
    }
}
