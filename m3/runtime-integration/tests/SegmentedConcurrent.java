/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;
public class SegmentedConcurrent {
    public static void main(String[] args)throws Exception{
        String a=new String(new char[]{'A','\u0100','\ud83d'}),b=new String(new char[]{'\ude00','Z'});
        String shared=a.concat(b);String expected=new String(new char[]{'A','\u0100','\ud83d','\ude00','Z'});
        String canonical=shared.intern();byte[]encoded=expected.getBytes(StandardCharsets.UTF_8);
        AtomicReference<Throwable> failed=new AtomicReference<>();Thread[]threads=new Thread[4];
        for(int t=0;t<threads.length;t++){
            threads[t]=new Thread(()->{try{
                for(int i=0;i<10000;i++){
                    if(!shared.equals(expected)||shared.hashCode()!=expected.hashCode()||shared.intern()!=canonical)throw new AssertionError("shared");
                    if(!Arrays.equals(shared.getBytes(StandardCharsets.UTF_8),encoded))throw new AssertionError("cache publication");
                    String local=a.concat(b);String slice=local.substring(2,4);
                    if(slice.charAt(0)!='\ud83d'||slice.charAt(1)!='\ude00'||local.intern()!=canonical)throw new AssertionError("range/intern");
                    if((i&2047)==0)System.gc();
                }
            }catch(Throwable e){failed.compareAndSet(null,e);}});threads[t].start();
        }
        for(Thread t:threads)t.join();if(failed.get()!=null)throw new AssertionError(failed.get());
        System.out.println("SEGMENTED_CONCURRENT_PASS iterations=40000");
    }
}
