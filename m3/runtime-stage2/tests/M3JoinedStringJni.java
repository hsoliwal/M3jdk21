/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class M3JoinedStringJni {
    static native String roundTrip(String value, int mode);

    public static void main(String[] args) {
        System.load(System.getProperty("m3.jni"));
        String[] values = {
            String.join("", new String(new char[] {'a', '\ud83d'}), new String(new char[] {'\ude00', 'z'})),
            String.join("|", "latin", "日本語", "tail"),
            String.join("", new String(new char[] {'\ud800'}), new String(new char[] {'x', '\udc00'})),
            String.join("", "\0", "middle", "\0")
        };
        int calls = 0;
        for (String value : values) {
            for (int mode = 0; mode < 5; mode++) {
                String actual = roundTrip(value, mode);
                if (!value.equals(actual)) {
                    throw new AssertionError("mode=" + mode + " expected=" + printable(value)
                                             + " actual=" + printable(actual));
                }
                calls++;
            }
        }
        try {
            roundTrip(values[0], 5);
            throw new AssertionError("bounds");
        } catch (StringIndexOutOfBoundsException expected) {
            calls++;
        }
        System.out.println("M3_STRING_JNI_PASS calls=" + calls);
    }

    private static String printable(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            result.append(String.format("\\u%04x", (int)value.charAt(i)));
        }
        return result.toString();
    }
}
