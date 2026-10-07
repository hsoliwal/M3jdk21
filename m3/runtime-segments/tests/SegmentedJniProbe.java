/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
public class SegmentedJniProbe {
    public static void main(String[] args) throws Exception {
        System.load(System.getProperty("m3.jni"));
        Field value = String.class.getDeclaredField("value"); value.setAccessible(true);
        Field cache = String.class.getDeclaredField("m3Flat"); cache.setAccessible(true);
        int calls = 0;
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String left = new String(new char[]{'\0', (char)unit});
            String right = new String(new char[]{(char)(Character.MAX_VALUE-unit), 'Z'});
            String joined = left.concat(right);
            String sliced = joined.substring(1,3);
            String expected = new String(new char[]{(char)unit,(char)(Character.MAX_VALUE-unit)});
            if (value.get(sliced) != null || cache.get(sliced) != null) throw new AssertionError("not segmented");
            for (int mode = 0; mode < 5; mode++) {
                if (!expected.equals(JoinJni.roundTrip(sliced,mode))) throw new AssertionError(unit+":"+mode);
                calls++;
            }
            if (value.get(sliced) != null || cache.get(sliced) != null) throw new AssertionError("JNI flattened Java storage");
        }
        System.out.println("SEGMENTED_JNI_PASS calls="+calls);
    }
}
