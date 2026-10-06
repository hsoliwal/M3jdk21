/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

 /*
 * @test
 * @run main/othervm/native -Xcheck:jni StringPlatformChars
 * @run main/othervm/native -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage StringPlatformChars
 */
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

public class StringPlatformChars {

    private static final String JNU_ENCODING = System.getProperty("sun.jnu.encoding");

    public static void main(String... args) throws Exception {
        System.out.println("sun.jnu.encoding: " + JNU_ENCODING);
        System.loadLibrary("stringPlatformChars");

        // Test varying lengths, provoking different allocation paths
        StringBuilder unicodeSb = new StringBuilder();
        StringBuilder asciiSb = new StringBuilder();
        StringBuilder latinSb = new StringBuilder();

        for (int i = 0; i < 2000; i++) {
            unicodeSb.append('\uFEFE');
            testString(unicodeSb.toString());

            asciiSb.append('x');
            testString(asciiSb.toString());

            latinSb.append('\u00FE');
            testString(latinSb.toString());

            testString(latinSb.toString() + asciiSb.toString() + unicodeSb.toString());
        }

        // Exhaustively test simple Strings made up of all possible chars:
        for (char c = '\u0001'; c < Character.MAX_VALUE; c++) {
            testString(String.valueOf(c));
        }
        // Special case: \u0000 is treated as end-of-string in the native code,
        // so strings with it should be truncated:
        if (getBytes("\u0000abcdef").length != 0 ||
            getBytes("a\u0000bcdef").length != 1) {
            System.out.println("Mismatching values for strings including \\u0000");
            throw new AssertionError();
        }

        for (String value : List.of(
                "",
                "\u0000",
                "ASCII",
                "\u00ff\u0100",
                "\uD800",
                "\uDC00",
                "\uD83D\uDE42",
                String.join("", "left-".repeat(80), "\uD83D", "\uDE42", "-right".repeat(80)),
                String.join("", "xx", "range-", "\u0100", "-tail", "yy").substring(2, 15))) {
            testJniViews(value);
        }
    }

    private static void testJniViews(String s) throws Exception {
        char[] utf16 = getUtf16(s);
        if (!Arrays.equals(utf16, s.toCharArray())) {
            throw new AssertionError("GetStringChars mismatch: " + Arrays.toString(s.toCharArray()));
        }

        int nativeUtfLength = getUtf8Length(s);
        int oracleUtfLength = modifiedUtf8Length(s);
        if (nativeUtfLength != oracleUtfLength) {
            throw new AssertionError(
                    "GetStringUTFLength mismatch native=" + nativeUtfLength
                            + " oracle=" + oracleUtfLength
                            + " units=" + Arrays.toString(s.chars().toArray()));
        }

        byte[] nativeUtf = getUtf8(s);
        byte[] oracleUtf = modifiedUtf8(s);
        if (!Arrays.equals(nativeUtf, oracleUtf)) {
            System.out.println("GetStringUTFChars mismatch for "
                    + Arrays.toString(s.chars().toArray()));
            System.out.println("Native modified UTF8: " + Arrays.toString(nativeUtf));
            System.out.println("Oracle modified UTF8: " + Arrays.toString(oracleUtf));
            throw new AssertionError(s);
        }

        int length = s.length();
        int[][] ranges = {
                {0, 0},
                {0, length},
                {Math.min(1, length), Math.max(0, length - Math.min(1, length))},
                {length / 2, Math.min(length - length / 2, 3)},
                {Math.max(0, length - Math.min(3, length)), Math.min(3, length)}
        };
        for (int[] range : ranges) {
            int start = range[0];
            int count = range[1];
            String expected = s.substring(start, start + count);

            char[] nativeRegion = getUtf16Region(s, start, count);
            if (!Arrays.equals(nativeRegion, expected.toCharArray())) {
                throw new AssertionError(
                        "GetStringRegion mismatch start=" + start + " count=" + count
                                + " units=" + Arrays.toString(s.chars().toArray()));
            }

            byte[] nativeUtfRegion = getUtf8Region(s, start, count);
            byte[] expectedUtfRegion = modifiedUtf8(expected);
            if (!Arrays.equals(nativeUtfRegion, expectedUtfRegion)) {
                throw new AssertionError(
                        "GetStringUTFRegion mismatch start=" + start + " count=" + count
                                + " native=" + Arrays.toString(nativeUtfRegion)
                                + " expected=" + Arrays.toString(expectedUtfRegion));
            }
        }
    }

    private static int modifiedUtf8Length(String value) {
        int length = 0;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            length += unit >= 0x0001 && unit <= 0x007f
                    ? 1
                    : unit <= 0x07ff ? 2 : 3;
        }
        return length;
    }

    private static byte[] modifiedUtf8(String value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeUTF(value);
        }
        byte[] framed = bytes.toByteArray();
        return Arrays.copyOfRange(framed, 2, framed.length);
    }

    private static void testString(String s) throws Exception {
        byte[] nativeBytes = getBytes(s);
        byte[] stringBytes = s.getBytes(JNU_ENCODING);

        if (!Arrays.equals(nativeBytes, stringBytes)) {
            System.out.println("Mismatching values for: '" + s + "' " + Arrays.toString(s.chars().toArray()));
            System.out.println("Native: " + Arrays.toString(nativeBytes));
            System.out.println("String: " + Arrays.toString(stringBytes));
            throw new AssertionError(s);
        }

        String javaNewS = new String(nativeBytes, JNU_ENCODING);
        String nativeNewS = newString(nativeBytes);
        if (!javaNewS.equals(nativeNewS)) {
            System.out.println("New string via native doesn't match via java: '" + javaNewS + "' and '" + nativeNewS + "'");
            throw new AssertionError(s);
        }
    }

    static native byte[] getBytes(String string);

    static native char[] getUtf16(String string);

    static native byte[] getUtf8(String string);

    static native char[] getUtf16Region(String string, int start, int length);

    static native byte[] getUtf8Region(String string, int start, int length);

    static native int getUtf8Length(String string);

    static native String newString(byte[] bytes);
}
