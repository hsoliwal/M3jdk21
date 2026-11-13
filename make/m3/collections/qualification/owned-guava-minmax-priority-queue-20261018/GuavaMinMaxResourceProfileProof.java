/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Only the isolated 64 MiB child may execute the huge unbounded expected allocation.
 */
import com.google.common.collect.Ordering;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

public final class GuavaMinMaxResourceProfileProof {
    private GuavaMinMaxResourceProfileProof() { }
    private static final class Caller extends Ordering<Integer> {
        int reverseCalls;
        @Override public int compare(Integer a,Integer b){return a.compareTo(b);}
        @Override @SuppressWarnings("unchecked") public <S extends Integer> Ordering<S> reverse(){reverseCalls++;return (Ordering<S>)this;}
    }
    public static void main(String[] args) throws Exception{
        Class<?> owner=Class.forName(args[0]);Object builder=owner.getMethod("expectedSize",int.class).invoke(null,Integer.MAX_VALUE);String result;
        try{Object queue=builder.getClass().getMethod("create").invoke(builder);result="RETURN/"+((java.util.Queue<?>)queue).size();}
        catch(InvocationTargetException wrapped){Throwable cause=wrapped.getCause();result="EXCEPTION/"+cause.getClass().getName()+"/"+cause.getMessage();}
        Caller caller=new Caller();Object hooked=owner.getMethod("orderedBy",Comparator.class).invoke(null,caller);hooked.getClass().getMethod("expectedSize",int.class).invoke(hooked,Integer.MAX_VALUE);String hookResult;
        try{Object queue=hooked.getClass().getMethod("create").invoke(hooked);hookResult="RETURN/"+((java.util.Queue<?>)queue).size();}
        catch(InvocationTargetException wrapped){Throwable cause=wrapped.getCause();hookResult="EXCEPTION/"+cause.getClass().getName()+"/"+cause.getMessage();}
        System.out.println("{\"status\":\"OBSERVED\",\"partition\":\"RESOURCE_PROFILE\",\"callbackTimingPartition\":\"REQUIRED_CONCRETE\",\"heapMiB\":64,\"expectedSize\":2147483647,\"outcome\":\""+result+"\",\"callerOrderingAllocationOutcome\":\""+hookResult+"\",\"callerOrderingReverseCalls\":"+caller.reverseCalls+",\"reverseObservedBeforeConstructorCompletion\":"+(caller.reverseCalls>0)+",\"allocationPolicyParityClaim\":false}");
    }
}
