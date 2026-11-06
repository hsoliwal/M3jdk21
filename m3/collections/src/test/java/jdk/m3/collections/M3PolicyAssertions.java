// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;

//FLLDCIM: RELOCATE - donor blob e836f63cc034 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
final class M3PolicyAssertions {
    private M3PolicyAssertions(){}
    static void check(boolean ok,String reason) {
        if(!ok)throw new AssertionError(reason);
    }
    static void eq(long a,long b){check(a==b,"expected "+a+" got "+b);}
    static void same(Object a,Object b){check(a==b,"different identity");}
    static void values(M3LongSequence s,long... expected) {
        check(Arrays.equals(s.toArray(),expected),"wrong elements");
    }
    static <T extends Throwable> T fails(Class<T> type,Runnable action) {
        try {action.run();}
        catch(Throwable failure) {
            if(type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("wrong failure",failure);
        }
        throw new AssertionError("expected "+type.getSimpleName());
    }
}
