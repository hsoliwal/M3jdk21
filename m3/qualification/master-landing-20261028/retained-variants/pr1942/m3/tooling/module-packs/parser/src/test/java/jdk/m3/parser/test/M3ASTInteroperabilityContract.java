/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Objects;
import jdk.m3.parser.M3ASTFieldRoles;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTProjectionCapture;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3FileLineIndex;

/** Real M3ASTIndex.Builder and M3File source contract; no API-shaped stub. */
public final class M3ASTInteroperabilityContract {
    private static int checks;
    private M3ASTInteroperabilityContract() { }

    private static void same(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("expected " + expected + " but found " + actual);
        checks++;
    }
    private static void refused(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException expected) {
            checks++; return;
        }
        throw new AssertionError("expected refusal");
    }

    private record Native(String kind, int start, int end, Native[] children,
                          String[] roles, int[] positions) { }
    private static final M3ASTProjectionCapture.Access<Native> NATIVE = new M3ASTProjectionCapture.Access<>() {
        @Override public String kind(Native node) { return node.kind(); }
        @Override public int childCount(Native node) { return node.children().length; }
        @Override public Native child(Native node, int index) { return node.children()[index]; }
        @Override public String fieldName(Native node, int index) { return node.roles()[index]; }
        @Override public int fieldPosition(Native node, int index) { return node.positions()[index]; }
        @Override public int startUtf16(Native node) { return node.start(); }
        @Override public int endUtf16(Native node) { return node.end(); }
    };
    public static void main(String[] unused) {
        M3File source = M3File.of(URI.create("memory:/Expression.java"), "a+b");
        Native empty = new Native("JDT_Identifier", 0, 1, new Native[0], new String[0], new int[0]);
        Native right = new Native("JDT_Identifier", 2, 3, new Native[0], new String[0], new int[0]);
        Native root = new Native("JDT_InfixExpression", 0, 3, new Native[]{empty, right},
                new String[]{"leftOperand", "rightOperand"}, new int[]{-1, -1});
        var capture = M3ASTProjectionCapture.capture(source, M3ASTIndex.Domain.ECLIPSE_JDT,
                root, NATIVE, 10, 3);
        M3ASTIndex index = capture.index();
        M3ASTFieldRoles roles = capture.roles();
        same(3, index.nodeCount());
        same(index, roles.index());
        same(2, index.childCount(0));
        same("JDT_Identifier", index.kind(1));
        same(0, index.parent(1));
        same(0, index.parent(2));
        same(-1, roles.roleId(0));
        same("leftOperand", roles.role(1).orElseThrow());
        same("rightOperand", roles.role(2).orElseThrow());
        same(-1, roles.positionInRole(1));
        same(1, roles.firstChildWithRole(0, "leftOperand"));
        same(2, roles.firstChildWithRole(0, "rightOperand"));
        same(-1, roles.firstChildWithRole(0, "missing"));
        same(true, (index.flags(1) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
        same(true, index.hasExactRange(1));
        refused(() -> M3ASTProjectionCapture.capture(source, M3ASTIndex.Domain.ECLIPSE_JDT,
                root, NATIVE, 2, 3));
        refused(() -> M3ASTProjectionCapture.capture(source, M3ASTIndex.Domain.ECLIPSE_JDT,
                root, NATIVE, 10, 1));

        var lineSource = M3File.of(URI.create("memory:/crlf"), "a\r\n\ud83e\uddea\rb\n");
        var lines = M3FileLineIndex.capture(lineSource, 8);
        same(4, lines.lineCount());
        same(0, lines.lineStart(1));
        same(3, lines.lineStart(2));
        same(6, lines.lineStart(3));
        same(8, lines.lineStart(4));
        for (int i = 0; i <= lineSource.length(); i++) {
            same(i, lines.offsetAt(lines.lineAtOffset(i), lines.columnAtOffset(i)));
        }
        refused(() -> M3FileLineIndex.capture(lineSource, 3));
        refused(() -> lines.offsetAt(1, 3));
        refused(() -> lines.lineAtOffset(-1));
        refused(() -> lines.lineAtOffset(lineSource.length() + 1));
        var recorder = M3ASTFieldRoles.recorder(3);
        recorder.record(0, null, -1);
        recorder.record(1, "leftOperand", -1);
        recorder.record(2, "rightOperand", -1);
        var fields = recorder.seal(index);
        same(3, fields.nodeCount());
        refused(() -> recorder.seal(index));
        refused(() -> recorder.record(3, "late", -1));
        System.out.println("M3_AST_INTEROP_PASS checks=" + checks);
    }
}
