/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class JoinJni {
    static native String roundTrip(String value, int mode);
    public static void main(String[] args) {
        System.load(System.getProperty("m3.jni"));
        int calls = 0;
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String expected = new String(new char[] {'\0', (char) unit, 'z'});
            String joined = String.join("", expected);
            for (int mode = 0; mode < 5; mode++) {
                if (!expected.equals(roundTrip(joined, mode))) throw new AssertionError(unit + ":" + mode);
                calls++;
            }
        }
        for (String value : new String[] {"", "\ud83d\ude00", "\ud800x\udc00", "日本語"}) {
            for (int mode = 0; mode < 5; mode++) {
                if (!value.equals(roundTrip(String.join("", value), mode))) throw new AssertionError("seam");
                calls++;
            }
        }
        try { roundTrip(String.join("", "bounds"), 5); throw new AssertionError("bounds"); }
        catch (StringIndexOutOfBoundsException expected) { calls++; }
        System.out.println("JOIN_JNI_PASS calls=" + calls);
    }
}
