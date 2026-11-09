/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.net.URI;
import jdk.m3.parser.M3ASTFieldRoles;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3FileLineIndex;

/** Deterministic 200-fixture line-coordinate oracle and 4,096-role dictionary. */
public final class M3ASTInteroperabilityFuzzContract {
    private static int checks;
    private M3ASTInteroperabilityFuzzContract() { }
    private static void same(int expected, int actual) {
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
        checks++;
    }
    public static void main(String[] unused) {
        final char[] alphabet = {'\r','\n','a','\ud83e','\uddea','\u0000','\u2028'};
        for (int fixture = 0; fixture < 200; fixture++) {
            StringBuilder text = new StringBuilder();
            long state = fixture + 1;
            for (int i = 0, length = fixture % 64; i < length; i++) {
                state = (state * 48271L + 1) % 2147483647L;
                text.append(alphabet[(int) (state % alphabet.length)]);
            }
            M3File source = M3File.of(URI.create("memory:/" + fixture), text);
            M3FileLineIndex lines = M3FileLineIndex.capture(source, source.length() + 1);
            int count = 1;
            for (int i = 0; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '\r' || c == '\n') {
                    if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                    count++;
                }
            }
            same(count, lines.lineCount());
            for (int i = 0; i <= source.length(); i++) {
                int line = lines.lineAtOffset(i);
                same(i, lines.offsetAt(line, lines.columnAtOffset(i)));
                same(1, line >= 1 && line <= lines.lineCount() ? 1 : 0);
            }
        }

        final int count = 4097;
        M3File source = M3File.of(URI.create("memory:/roles"), "");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.JACKSON_JSON, count + 1);
        int root = builder.add(-1, "root", 0, 0, M3ASTIndex.SYNTAX_ONLY);
        var recorder = M3ASTFieldRoles.recorder(count + 1);
        recorder.record(root, null, -1);
        for (int row = 1; row < count; row++) {
            builder.add(root, "child", -1, -1, M3ASTIndex.SYNTAX_ONLY);
            recorder.record(row, "field" + row, -1);
        }
        var original = builder.seal();
        var sidecar = recorder.seal(original);
        same(count, sidecar.nodeCount());
        same(4096, sidecar.roleVocabularySize());
        same(1, sidecar.firstChildWithRole(0, "field1"));
        same(4096, sidecar.firstChildWithRole(0, "field4096"));
        same(-1, sidecar.firstChildWithRole(0, "missing"));
        try {
            recorder.record(count, "overflow", -1);
            throw new AssertionError("sealed recorder unexpectedly accepted mutation");
        } catch (IllegalStateException expected) { checks++; }
        var overflow = M3ASTFieldRoles.recorder(count + 1);
        overflow.record(0, null, -1);
        for (int row = 1; row < count; row++) overflow.record(row, "field" + row, -1);
        try {
            overflow.record(count, "extra", -1);
            throw new AssertionError("4097th role accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        System.out.println("M3_AST_INTEROP_FUZZ_PASS checks=" + checks);
    }
}
