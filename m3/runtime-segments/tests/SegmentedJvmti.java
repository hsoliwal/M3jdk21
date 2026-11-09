/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
public class SegmentedJvmti {
    static native int walk(String target);
    public static void main(String[] args)throws Exception{
        System.load(System.getProperty("m3.jvmti"));
        Field cache=String.class.getDeclaredField("m3Flat");cache.setAccessible(true);
        Field value=String.class.getDeclaredField("value");value.setAccessible(true);
        int count=0;
        for(String left:new String[]{"ascii","\u0100","\ud83d","\0"}){
            String target=left.concat(new String(new char[]{'z','\ude00'}));
            if(value.get(target)!=null||cache.get(target)!=null)throw new AssertionError("flat before walk");
            if(walk(target)!=1)throw new AssertionError("heap callback mismatch");
            if(value.get(target)!=null||cache.get(target)!=null)throw new AssertionError("heap walk materialized Java cache");
            count++;
        }
        System.out.println("SEGMENTED_JVMTI_PASS callbacks="+count);
    }
}
