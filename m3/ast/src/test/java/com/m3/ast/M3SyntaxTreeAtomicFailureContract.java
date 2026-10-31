// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/** Java21 fault oracle: lossless retry after each of ten grow allocations. */
public final class M3SyntaxTreeAtomicFailureContract {
    private static final String[] NAMES = {
            "kind", "field", "flags", "parent", "firstChild", "nextSibling",
            "lastChild", "lastEnd", "start", "end"
    };
    private static int checks;
    private M3SyntaxTreeAtomicFailureContract() { }
    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
    private static Field[] fields() throws ReflectiveOperationException {
        Field[] fields = new Field[NAMES.length];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = M3SyntaxTree.Builder.class.getDeclaredField(NAMES[i]);
            fields[i].setAccessible(true);
        }
        return fields;
    }
    private static void caseAt(int site, Field[] fields) throws ReflectiveOperationException {
        byte[] source = "x".getBytes(StandardCharsets.UTF_8);
        M3SyntaxTree.Builder builder = M3SyntaxTree.builder("failure-atomic", source);
        builder.open("root", "", 0, M3SyntaxTree.NAMED);
        for (int i = 0; i < 15; i++) builder.leaf("leaf", "item", 0, 0, M3SyntaxTree.TOKEN);
        Object[] original = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) original[i] = fields[i].get(builder);
        M3SyntaxGrowthFault.arm(site);
        try {
            builder.leaf("leaf", "item", 0, 0, M3SyntaxTree.TOKEN);
            throw new AssertionError("expected injected failure at " + site);
        } catch (OutOfMemoryError failure) {
            check(failure == M3SyntaxGrowthFault.FAILURE, "exact injected exception");
            check(M3SyntaxGrowthFault.visits() == site, "same failure site");
        } finally {
            M3SyntaxGrowthFault.disarm();
        }
        for (int i = 0; i < fields.length; i++) {
            check(fields[i].get(builder) == original[i], "unpublished original plane " + NAMES[i] + " at site " + site);
        }
        builder.leaf("leaf", "item", 0, 0, M3SyntaxTree.TOKEN);
        builder.close(source.length);
        M3SyntaxTree tree = builder.build();
        check(tree.size() == 17, "retry inserted one row");
        check(tree.parent(16) == 0, "retry parent");
        check(tree.nextSibling(16) == -1, "last sibling after retry");
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        Field[] fields = fields();
        for (int site = 1; site <= 10; site++) caseAt(site, fields);
        check(M3SyntaxGrowthAgent.SITES.get() == 10, "ten original source array copies instrumented");
        System.out.println("M3_SYNTAX_ATOMIC_FAILURE_PASS checks=" + checks);
    }
}
