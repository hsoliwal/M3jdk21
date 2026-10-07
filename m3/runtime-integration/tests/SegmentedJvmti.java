/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class SegmentedJvmti {
    static native int walk(String target);
    public static void main(String[] args)throws Exception{
        System.load(System.getProperty("m3.jvmti"));
        int count=0;
        for(String left:new String[]{"ascii","\u0100","\ud83d","\0"}){
            String target=left.concat(new String(new char[]{'z','\ude00'}));
            Object before=M3StringIntegration.body(target);
            Object owner=M3StringInvariant.owner(before);
            long coordinate=M3StringInvariant.M3_VALUE.getLong(before);
            if(walk(target)!=1)throw new AssertionError("heap callback mismatch");
            Object after=M3StringIntegration.body(target);
            if(M3StringInvariant.owner(after)!=owner
                    || M3StringInvariant.M3_VALUE.getLong(after)!=coordinate) {
                throw new AssertionError("JVMTI walk changed canonical M3 coordinate");
            }
            count++;
        }
        System.out.println("M3_JVMTI_PASS callbacks="+count+" canonical_unchanged=true");
    }
}
