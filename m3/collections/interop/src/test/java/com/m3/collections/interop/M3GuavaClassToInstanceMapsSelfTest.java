/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.*;

/** Guava 33.4.8-jre differential on real class-value casting and entry live views. */
public final class M3GuavaClassToInstanceMapsSelfTest {
    private static long checks;
    private M3GuavaClassToInstanceMapsSelfTest() { }
    private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError("check="+checks+" "+a+" != "+b);}
    private static void expectException(Class<? extends Throwable> type,Runnable f){checks++;try{f.run();}catch(Throwable x){if(type.isInstance(x))return;throw new AssertionError(x);}throw new AssertionError("missing "+type);}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void main(String[] args){
       ClassToInstanceMap<Object> m=M3GuavaClassToInstanceMaps.create();
       ClassToInstanceMap<Object> ref=MutableClassToInstanceMap.create();
       Random rnd=new Random(1787);
       for(int i=0;i<3000;i++){
         int op=rnd.nextInt(9);
         switch(op){
           case 0 -> eq(ref.putInstance(String.class,"s"+i),m.putInstance(String.class,"s"+i));
           case 1 -> eq(ref.putInstance(Integer.class,i),m.putInstance(Integer.class,i));
           case 2 -> eq(ref.putInstance(Number.class,(long)i),m.putInstance(Number.class,(long)i));
           case 3 -> eq(ref.putInstance(Boolean.class,(i&1)==0),m.putInstance(Boolean.class,(i&1)==0));
           case 4 -> eq(ref.remove(String.class),m.remove(String.class));
           case 5 -> eq(ref.remove(Number.class),m.remove(Number.class));
           case 6 -> {eq(ref.getInstance(String.class),m.getInstance(String.class));eq(ref.getInstance(Number.class),m.getInstance(Number.class));}
           case 7 -> {ref.putAll(Map.of(String.class,"X"));m.putAll(Map.of(String.class,"X"));}
           default -> eq(ref.containsKey(Integer.class),m.containsKey(Integer.class));
         }
         eq(ref,m);eq(m,ref);eq(ref.keySet(),m.keySet());
       }
       @SuppressWarnings({"rawtypes","unchecked"}) Map raw=(Map)m;
       expectException(ClassCastException.class,()->raw.put(String.class,123));
       eq(ref.size(),m.size());
       System.out.println("PASS GUAVA_CLASS_TO_INSTANCE_M3 checks="+checks+" operations=3000");
    }
}
