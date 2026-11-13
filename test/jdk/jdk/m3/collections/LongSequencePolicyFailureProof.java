// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static jdk.m3.collections.PolicyAssertions.*;
import jdk.internal.m3.collections.BoundedLongSequence;
import jdk.internal.m3.collections.LongSequenceMetrics;
import jdk.internal.m3.collections.ObservedLongSequence;
import jdk.internal.m3.collections.ValidatingLongSequence;

//FLLDCIM: RELOCATE - donor blob 70f63c4dac4b at d5ad5090d87e; recipe make/m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class LongSequencePolicyFailureProof {
    public static void main(String[] args) {
        PolicyProbe probe=new PolicyProbe();
        RuntimeException callerError=new IllegalStateException("caller");
        RuntimeException validatorError=new IllegalArgumentException("validator");
        LongSequence validator=new ValidatingLongSequence(probe,v->{
            if(v==9)throw validatorError;
            return v>=0;
        });
        LongSequence bounded=new BoundedLongSequence(validator,1);
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
        LongSequenceMetrics metrics=new LongSequenceMetrics();
        ObservedLongSequence observed=new ObservedLongSequence(bounded,metrics);
        observed.addAll(new long[0]);
        eq(1,metrics.invocations());
        eq(1,probe.bulks);
        eq(1,metrics.successes());
    }
}
