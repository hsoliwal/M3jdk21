/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class SegmentedJniProbe {
    public static void main(String[] args) throws Exception {
        System.load(System.getProperty("m3.jni"));
        int calls = 0;
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String left = new String(new char[]{'\0', (char)unit});
            String right = new String(new char[]{(char)(Character.MAX_VALUE-unit), 'Z'});
            String joined = left.concat(right);
            String sliced = joined.substring(1,3);
            String expected = new String(new char[]{(char)unit,(char)(Character.MAX_VALUE-unit)});

            Object before=M3StringIntegration.body(sliced);
            Object owner=M3StringInvariant.owner(before);
            long coordinate=M3StringInvariant.M3_VALUE.getLong(before);
            for (int mode = 0; mode < 5; mode++) {
                if (!expected.equals(JoinJni.roundTrip(sliced,mode))) {
                    throw new AssertionError(unit+":"+mode);
                }
                calls++;
            }
            Object after=M3StringIntegration.body(sliced);
            if (M3StringInvariant.owner(after)!=owner
                    || M3StringInvariant.M3_VALUE.getLong(after)!=coordinate) {
                throw new AssertionError("JNI changed canonical M3 coordinate");
            }

            char[] shadow=sliced.toCharArray();
            if(shadow.length!=0)shadow[0]^=1;
            if(!sliced.equals(expected))throw new AssertionError("char shadow leaked into canonical M3");
        }
        System.out.println("M3_JNI_SHADOW_PASS calls="+calls+" canonical_unchanged=true");
    }
}
