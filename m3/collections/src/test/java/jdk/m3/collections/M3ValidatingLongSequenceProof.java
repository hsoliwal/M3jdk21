// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.concurrent.atomic.AtomicInteger;
import static jdk.m3.collections.M3PolicyAssertions.*;

//FLLDCIM: RELOCATE - donor blob fcb332079031 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3ValidatingLongSequenceProof {
    public static void main(String[] args) {
        fails(NullPointerException.class,()->new M3ValidatingLongSequence(null,v->true));
        fails(NullPointerException.class,
                ()->new M3ValidatingLongSequence(new M3PolicyProbe(),null));
        M3PolicyProbe existing=new M3PolicyProbe(1,-2);
        fails(IllegalArgumentException.class,
                ()->new M3ValidatingLongSequence(existing,v->v>=0));
        values(existing,1,-2);
        M3PolicyProbe probe=new M3PolicyProbe();
        long[] caller={4,5};
        AtomicInteger checks=new AtomicInteger();
        M3ValidatingLongSequence validate=new M3ValidatingLongSequence(probe,v->{
            checks.incrementAndGet();
            if(v==4) caller[1]=-100;
            return v>=0;
        });
        validate.addAll(caller);
        values(probe,4,5);
        eq(1,probe.bulks);
        check(probe.lastBulk!=caller,"bulk must snapshot");
        eq(5,probe.lastBulk[1]);
        int before=checks.get();
        AtomicInteger predicates=new AtomicInteger();
        check(!validate.addIf(-1,v->{predicates.incrementAndGet();return false;}),
                "false skips validator");
        eq(1,predicates.get());
        eq(before,checks.get());
        eq(0,probe.addIfs);
        fails(NullPointerException.class,()->validate.addIf(8,null));
        fails(IllegalArgumentException.class,()->validate.addIf(-1,v->true));
        validate.addIf(6,v->true);
        eq(1,probe.adds);
        int bulks=probe.bulks;
        fails(IllegalArgumentException.class,()->validate.addAll(new long[]{7,-8}));
        eq(bulks,probe.bulks);
        values(probe,4,5,6);
        fails(IndexOutOfBoundsException.class,()->validate.insert(99,-9));
        fails(IndexOutOfBoundsException.class,()->validate.set(99,-9));
        fails(IllegalArgumentException.class,()->validate.set(0,-9));
        eq(4,validate.set(0,8));
        validate.insert(1,9);
        values(probe,8,9,5,6);
        RuntimeException fault=new IllegalStateException("validator");
        AssertionError error=new AssertionError("validator");
        M3PolicyProbe other=new M3PolicyProbe();
        M3ValidatingLongSequence throwing=new M3ValidatingLongSequence(other,v->{
            if(v==11)throw fault;
            if(v==12)throw error;
            return true;
        });
        same(fault,fails(RuntimeException.class,()->throwing.addAll(new long[]{3,11})));
        same(error,fails(AssertionError.class,()->throwing.addAll(new long[]{3,12})));
        eq(0,other.bulks);
        values(other);
    }
}
