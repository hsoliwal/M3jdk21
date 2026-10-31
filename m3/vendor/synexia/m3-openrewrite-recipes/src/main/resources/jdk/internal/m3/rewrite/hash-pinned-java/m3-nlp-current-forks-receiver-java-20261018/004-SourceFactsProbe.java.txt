/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search.test;

import com.m3.compiletime.ast.M3FileSourceFacts;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/** A compatibility oracle for the existing source digest and the explicit immutable ingress. */
public final class SourceFactsProbe {
    private SourceFactsProbe() { }

    public static void main(String[] args) throws Exception {
        final Method immutable;
        try {
            immutable = M3FileSourceFacts.class.getMethod(
                    "sourceSha256ImmutableView", CharSequence.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Missing immutable source digest ingress", missing);
        }
        Random random = new Random(0x4d33444947455354L);
        for (int iteration = 0; iteration < 2_000; iteration++) {
            char[] chars = new char[random.nextInt(512)];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(65_536);
            String expected = new String(chars);
            CharSequence guarded = new CharSequence() {
                @Override public int length() { return chars.length; }
                @Override public char charAt(int index) { return chars[index]; }
                @Override public CharSequence subSequence(int start, int end) {
                    throw new AssertionError("Digest must not slice or flatten its source");
                }
                @Override public String toString() {
                    throw new AssertionError("Digest must not flatten its source");
                }
            };
            Object actual = immutable.invoke(null, guarded);
            if (!M3FileSourceFacts.sourceSha256(expected).equals(actual)) {
                throw new AssertionError("UTF-16 digest changed at seeded input " + iteration);
            }
        }
        String snapshot = "legacy snapshot";
        CharSequence snapshotOnly = new CharSequence() {
            @Override public int length() { throw new AssertionError("Legacy ingress snapshots first"); }
            @Override public char charAt(int index) { throw new AssertionError("Legacy ingress snapshots first"); }
            @Override public CharSequence subSequence(int start, int end) { throw new AssertionError(); }
            @Override public String toString() { return snapshot; }
        };
        if (!M3FileSourceFacts.sourceSha256(snapshot).equals(
                M3FileSourceFacts.sourceSha256(snapshotOnly))) {
            throw new AssertionError("Legacy snapshot contract changed");
        }
        try {
            immutable.invoke(null, new Object[] {null});
            throw new AssertionError("Null immutable source accepted");
        } catch (InvocationTargetException expected) {
            if (!(expected.getCause() instanceof NullPointerException)) throw expected;
        }
        System.out.println("SourceFactsProbe PASS: 2000 raw UTF-16 digest oracles; snapshot/null contracts");
    }
}
