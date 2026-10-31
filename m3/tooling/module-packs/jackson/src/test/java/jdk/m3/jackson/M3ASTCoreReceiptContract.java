// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.jackson;

/**
 * No-framework Java 21 contract for the unchanged #1289 primitive AST core.
 * Run with: java jdk.m3.jackson.M3ASTCoreReceiptContract
 * This tests the exact original pool/builder sources, not Jackson databinding.
 */
public final class M3ASTCoreReceiptContract {
    private static int checks;

    private M3ASTCoreReceiptContract() { }

    private static void check(boolean value, String explanation) {
        checks++;
        if (!value) throw new AssertionError(explanation);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong exception " + error, error);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    public static void main(String[] args) {
        M3JsonLimits limits = new M3JsonLimits(64, 32, 64, 128, 16);
        M3ASTBuilder builder = new M3ASTBuilder(limits);
        int text = builder.leaf("STRING", null, "same");
        int repeatedText = builder.leaf("STRING", null, new String("same"));
        check(text == repeatedText, "duplicate scalar coordinates must be interned");
        int field = builder.field("key", text);
        check(field == builder.field("key", repeatedText), "field coordinates intern");
        int object = builder.atom("OBJECT", null, null, new int[]{field, field});
        M3AST ast = builder.build(object);
        check(ast.rootAtomIndex() == object, "root identity");
        check(ast.atomCount() == 3, "one scalar, one field, one object");
        check(ast.childReferenceCount() == 3, "repeated children retain occurrences");
        check(ast.childCountAt(object) == 2, "duplicate field occurrences");
        check(ast.objectFieldValueAt(object, "key", 0) == text, "first field");
        check(ast.objectFieldValueAt(object, "key", 1) == text, "second field");
        check(ast.objectFieldValueAt(object, "key", 2) == -1, "field occurrence absent");
        check(ast.objectFieldValueAt(object, "missing", 0) == -1, "field name absent");
        check(ast.childAtomIndexAt(object, 1) == field, "ordered duplicate child");
        check(ast.root().child(0).child(0).atomIndex() == text, "atom wrapper traversal");
        check(ast.root().asAST().sameStructure(ast), "same-pool root identity");
        check(ast.structuralHash64() == ast.root().structuralHash64(), "hash projection");
        rejects(IllegalStateException.class, () -> builder.leaf("STRING", null, "late"));
        rejects(IllegalStateException.class, () -> builder.build(object));
        rejects(IndexOutOfBoundsException.class, () -> ast.childAtomIndexAt(object, 2));
        rejects(IllegalArgumentException.class, () -> ast.objectFieldValueAt(object, "key", -1));
        rejects(IllegalArgumentException.class, () -> ast.arrayElementAt(object, 0));

        M3ASTBuilder independent = new M3ASTBuilder(limits);
        int independentText = independent.leaf("STRING", null, "same");
        int independentField = independent.field("key", independentText);
        M3AST other = independent.build(independent.atom("OBJECT", null, null,
                new int[]{independentField, independentField}));
        check(!ast.sameStructure(other), "equal structure is not shared identity");
        check(ast.structuralHash64() == other.structuralHash64(), "stable hash across pools");

        M3ASTBuilder small = new M3ASTBuilder(new M3JsonLimits(4, 1, 1, 16, 2));
        rejects(IllegalArgumentException.class,
                () -> small.leaf("STRING", null, "over budget".repeat(8)));
        int admitted = small.leaf("STRING", null, "a");
        rejects(IllegalArgumentException.class, () -> small.leaf("NUMBER", null, "2"));
        check(small.build(admitted).atomCount() == 1, "failed admission has no partial atom");
        System.out.println("M3 AST CORE PASS checks=" + checks);
    }
}
