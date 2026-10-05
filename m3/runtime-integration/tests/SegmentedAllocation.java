/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

public class SegmentedAllocation {
    static volatile String[] escape;
    static String plus(String a,String b) {return a+b;}
    public static void main(String[] args)throws Exception{
        boolean expectedM3=Boolean.parseBoolean(args[0]);
        int n=2000;char[] a=new char[8192],b=new char[8192];
        java.util.Arrays.fill(a,'x');java.util.Arrays.fill(b,'\u0100');
        String left=new String(a),right=new String(b);String[] output=new String[n];
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        Field value=String.class.getDeclaredField("value");value.setAccessible(true);
        Field body=String.class.getDeclaredField("m3");body.setAccessible(true);
        Field owner=M3StringInvariant.M3_OWNER;
        for(int i=0;i<200;i++)output[i]=plus(left,right);escape=output;
        long thread=Thread.currentThread().threadId();long bytes=bean.getThreadAllocatedBytes(thread);
        long start=System.nanoTime();
        for(int i=0;i<n;i++)output[i]=plus(left,right);
        long elapsed=System.nanoTime()-start;long allocated=bean.getThreadAllocatedBytes(thread)-bytes;escape=output;

        IdentityHashMap<Object,Boolean> canonicalOwners=new IdentityHashMap<>();
        IdentityHashMap<byte[],Boolean> shadows=new IdentityHashMap<>();
        long shadowBytes=0;
        for(String s:output){
            if(s.length()!=16384||s.charAt(8191)!='x'||s.charAt(8192)!='\u0100')throw new AssertionError();
            Object m3=body.get(s);
            if((m3!=null)!=expectedM3)throw new AssertionError("wrong M3 representation");
            if(m3!=null){
                M3StringInvariant.assertNoArrayInstanceFields(m3.getClass());
                Object canonical=owner.get(m3);
                M3StringInvariant.assertNoArrayInstanceFields(canonical.getClass());
                canonicalOwners.put(canonical,Boolean.TRUE);
            }
            byte[] shadow=(byte[])value.get(s);
            if(shadows.put(shadow,Boolean.TRUE)==null)shadowBytes+=shadow.length;
        }
        if(expectedM3&&canonicalOwners.size()!=1)
            throw new AssertionError("equal joins did not converge to one canonical tuple owner: "+canonicalOwners.size());
        System.out.printf("{\"m3\":%s,\"joins\":%d,\"allocated_bytes\":%d,\"operation_ns\":%d,\"canonical_join_owners\":%d,\"compatibility_shadow_arrays\":%d,\"compatibility_shadow_bytes\":%d}%n",
            expectedM3,n,allocated,elapsed,canonicalOwners.size(),shadows.size(),shadowBytes);
    }
}
