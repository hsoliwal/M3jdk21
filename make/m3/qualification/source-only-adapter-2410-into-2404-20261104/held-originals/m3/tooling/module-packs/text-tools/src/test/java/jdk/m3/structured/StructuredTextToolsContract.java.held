/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

/** Frozen-source differential coverage of the environment-name classifier and retained formats. */
public final class StructuredTextToolsContract {
    private StructuredTextToolsContract() { }
    private static int comparisons;

    @FunctionalInterface
    private interface Operation { Object run(); }

    private static String outcome(Operation operation) {
        try { return "OK:" + operation.run(); }
        catch (RuntimeException failure) {
            return failure.getClass().getName() + ":" + failure.getMessage();
        }
    }

    private static void same(String label, Operation old, Operation next) {
        String expected = outcome(old);
        String actual = outcome(next);
        comparisons++;
        if (!expected.equals(actual)) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    private static void env(String source, Limits limits) {
        same("env", () -> FrozenStructuredTextTools.dotenv(TextFrame.fromString(source), limits),
                () -> StructuredTextTools.dotenv(TextFrame.fromString(source), limits));
    }

    public static int runAll() {
        comparisons = 0;
        Limits limits = Limits.standard();
        // Every UTF-16 code unit in both name positions. The source constructor and
        // the original regex independently determine malformed Unicode/name outcomes.
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            char c = (char) unit;
            env(c + "Q=value\n", limits);
            env("Q" + c + "=value\n", limits);
        }
        for (String text : new String[] {"", "#comment\n", "_=ok", "9A=bad", "A9=ok",
                " export A = 'literal $HOME'\n", "A=1\nA=2", "A=\"x#y\"", "A=x #tail",
                "A=\"unclosed", "A=x#literal", "A=x\u2003#comment", "A=1\r\nB=2\rC=3\n",
                "A=\"\"", "A=''", "A=\u0000", "Ａ=wide", "A-B=x", " A = padded "}) {
            env(text, limits);
            env(text, new Limits(1, 0, 100, 4096));
            env(text, new Limits(4, 0, 0, 0));
        }
        for (String text : new String[] {"", "a,b\n", "\"a,b\",\"x\"\"y\"\r\n", "\"a\nb\",z",
                "a,\n", "\"unterminated", "a\"b", "\"a\"x", "a\tb\r\n", "\n", "\r\n"}) {
            for (char delimiter : new char[] {',', '\t', ';', '"', '\r', '\0', '\ud800'}) {
                same("delimited", () -> FrozenStructuredTextTools.delimited(TextFrame.fromString(text), delimiter, limits),
                        () -> StructuredTextTools.delimited(TextFrame.fromString(text), delimiter, limits));
                same("projection", () -> FrozenStructuredTextTools.selectDelimited(TextFrame.fromString(text), delimiter, new int[] {0}, limits).text(),
                        () -> StructuredTextTools.selectDelimited(TextFrame.fromString(text), delimiter, new int[] {0}, limits).text());
            }
        }
        for (String text : new String[] {"", "a=b", "[s]\na=b\n", "[s]\na=b\na=c", "[]=x", "[]", "bad", "#x\n;k\na = b"}) {
            same("ini", () -> FrozenStructuredTextTools.ini(TextFrame.fromString(text), limits),
                    () -> StructuredTextTools.ini(TextFrame.fromString(text), limits));
        }
        same("null input", () -> FrozenStructuredTextTools.dotenv(null, limits), () -> StructuredTextTools.dotenv(null, limits));
        same("null limits", () -> FrozenStructuredTextTools.dotenv(TextFrame.fromString("A=1"), null),
                () -> StructuredTextTools.dotenv(TextFrame.fromString("A=1"), null));
        if (!StructuredTextTools.dotenv(TextFrame.fromString("export A='${HOME}'\nB=x # comment"), limits)
                .equals(java.util.Map.of("A", "${HOME}", "B", "x"))) throw new AssertionError("literal value contract");
        System.out.println("StructuredTextToolsContract comparisons=" + comparisons);
        return comparisons;
    }

    public static void main(String[] args) { runAll(); }
}
