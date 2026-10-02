/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
public class SegmentedAllocation {
    static volatile String[] escape;
    static String plus(String a,String b) {return a+b;}
    public static void main(String[] args)throws Exception{
        boolean expectedSegments=Boolean.parseBoolean(args[0]);
        int n=2000;char[] a=new char[8192],b=new char[8192];java.util.Arrays.fill(a,'x');java.util.Arrays.fill(b,'\u0100');
        String left=new String(a),right=new String(b);String[] output=new String[n];
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        Field value=String.class.getDeclaredField("value");value.setAccessible(true);
        Field body=String.class.getDeclaredField("mindex");body.setAccessible(true);
        Class<?> type=Class.forName("java.lang.MIndexString");
        Field parts=type.getDeclaredField("segments");parts.setAccessible(true);
        Field local=type.getDeclaredField("localValue");local.setAccessible(true);
        Field cache=type.getDeclaredField("materialized");cache.setAccessible(true);
        for(int i=0;i<200;i++)output[i]=plus(left,right);escape=output;
        long thread=Thread.currentThread().threadId();long bytes=bean.getThreadAllocatedBytes(thread);long start=System.nanoTime();
        for(int i=0;i<n;i++)output[i]=plus(left,right);
        long elapsed=System.nanoTime()-start;long allocated=bean.getThreadAllocatedBytes(thread)-bytes;escape=output;
        IdentityHashMap<byte[],Boolean> retained=new IdentityHashMap<>();long payload=0;
        for(String s:output){
            if(s.length()!=16384||s.charAt(8191)!='x'||s.charAt(8192)!='\u0100')throw new AssertionError();
            Object storage=body.get(s);Object[] leaves=storage==null?null:(Object[])parts.get(storage);
            if((leaves!=null)!=expectedSegments)throw new AssertionError("wrong representation");
            if(leaves!=null){
                if(((byte[])value.get(s)).length!=0||cache.get(storage)!=null)throw new AssertionError("hidden flattening");
                for(Object leaf:leaves){byte[] raw=(byte[])local.get(leaf);if(retained.put(raw,true)==null)payload+=raw.length;}
            }else{byte[]raw=(byte[])value.get(s);if(retained.put(raw,true)==null)payload+=raw.length;}
        }
        System.out.printf("{\"segmented\":%s,\"joins\":%d,\"allocated_bytes\":%d,\"operation_ns\":%d,\"retained_leaf_arrays\":%d,\"leaf_payload_bytes\":%d}%n",expectedSegments,n,allocated,elapsed,retained.size(),payload);
    }
}
