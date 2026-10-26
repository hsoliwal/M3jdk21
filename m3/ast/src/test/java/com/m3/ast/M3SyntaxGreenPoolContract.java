// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Contract for exact, source-full, immutable and cross-generation green DAG reuse. */
public final class M3SyntaxGreenPoolContract {
    private static int checks;
    private static void ok(boolean actual, String reason) {
        if (!actual) throw new AssertionError(reason);
        checks++;
    }
    private static void reject(Class<? extends Throwable> kind, Runnable call) {
        try { call.run(); }
        catch (Throwable error) {
            if (!kind.isInstance(error)) throw new AssertionError("wrong refusal", error);
            checks++;
            return;
        }
        throw new AssertionError("invalid green admitted");
    }
    private static M3SyntaxTree parse(String source, String grammar, int a, int b,
                                      String firstKind, String firstField) {
        var out = M3SyntaxTree.builder(grammar, source.getBytes(StandardCharsets.UTF_8));
        out.open("file", "", 0, M3SyntaxTree.NAMED);
        out.leaf(firstKind, firstField, a, a + 1, M3SyntaxTree.TOKEN);
        out.leaf("name", "right", b, b + 1, M3SyntaxTree.TOKEN);
        out.close(source.getBytes(StandardCharsets.UTF_8).length);
        return out.build();
    }
    private static M3SyntaxTree deepBuilderAgain() {
        var tree = M3SyntaxTree.builder("deep", new byte[] {7});
        for (int i = 0; i < 5000; i++) tree.open("node", "", 0, 0);
        for (int i = 0; i < 5000; i++) tree.close(1);
        return tree.build();
    }
    public static void main(String[] args) {
        var pool = new M3SyntaxGreenPool(10_000, 2_000_000);
        var a = parse("a + b", "java/21", 0, 4, "name", "left");
        var b = parse("\n a + b", "java/21", 2, 6, "name", "left");
        var first = pool.intern(a);
        int original = pool.uniqueGreens();
        var shifted = pool.intern(b);
        ok(pool.uniqueGreens() == original + 1, "only changed root green is added");
        ok(first.greenAt(1) == shifted.greenAt(1) && first.greenAt(2) == shifted.greenAt(2),
                "unchanged tokens share exact retained green IDs despite offset shifts");
        ok(first.rootGreen() != shifted.rootGreen(), "changed gap text changes ancestor");
        ok(Arrays.equals(pool.render(first.rootGreen(), 100), a.sourceBytes()), "full fidelity first");
        ok(Arrays.equals(pool.render(shifted.rootGreen(), 100), b.sourceBytes()), "full fidelity moved");
        ok(pool.intern(a).rootGreen() == first.rootGreen()
                && pool.uniqueGreens() == original + 1, "exact reread is no-op");
        ok(first.occurrences() == a.size() && first.sourceSha256().equals(a.sourceSha256()),
                "projection binds original source and shape");
        first.requireTree(a);
        reject(IllegalStateException.class, () -> first.requireTree(b));
        int[] handles = first.greenHandles();
        handles[0] = -1;
        ok(first.rootGreen() >= 0, "defensive occurrence handle exposure");
        var kind = pool.intern(parse("a + b", "java/21", 0, 4, "identifier", "left"));
        ok(kind.greenAt(1) != first.greenAt(1), "provider syntax kind is respected");
        var field = pool.intern(parse("a + b", "java/21", 0, 4, "name", "parameter"));
        ok(field.greenAt(1) != first.greenAt(1), "named grammar fields remain distinct");
        var grammar = pool.intern(parse("a + b", "rust/2026", 0, 4, "name", "left"));
        ok(grammar.greenAt(1) != first.greenAt(1), "cross-grammar hashes cannot be conflated");
        var punct = pool.intern(parse("a ; b", "java/21", 0, 4, "name", "left"));
        ok(punct.greenAt(1) == first.greenAt(1) && punct.rootGreen() != first.rootGreen(),
                "unparsed punctuation affects only owning ancestor");
        ok(pool.retainedLiteralBytes() <= 64, "gap text stored once per unique green");
        var error = M3SyntaxTree.builder("java/21", " x ".getBytes(StandardCharsets.UTF_8));
        error.open("file", "", 0, M3SyntaxTree.NAMED);
        error.leaf("ERROR", "recovery", 1, 2, M3SyntaxTree.ERROR | M3SyntaxTree.TOKEN);
        error.leaf("MISSING_SEMI", "expected", 2, 2, M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN);
        error.close(3);
        var damaged = error.build();
        var recovered = pool.intern(damaged);
        ok(Arrays.equals(pool.render(recovered.rootGreen(), 3), damaged.sourceBytes()),
                "missing, errors, trivia gaps faithfully reconstruct original bytes");
        ok(pool.childCount(recovered.rootGreen()) == 2,
                "missing token retains zero-width child position");
        ok(pool.width(recovered.greenAt(2)) == 0, "missing token has zero width");
        reject(IllegalArgumentException.class, () -> pool.render(first.rootGreen(), 2));
        reject(IndexOutOfBoundsException.class, () -> pool.childGreen(first.rootGreen(), 4));
        var tiny = new M3SyntaxGreenPool(2, 100);
        reject(IllegalStateException.class, () -> tiny.intern(a));
        ok(tiny.uniqueGreens() == 0 && tiny.childReferences() == 0
                && tiny.retainedLiteralBytes() == 0,
                "node budget refusal rolls back every partial green");
        var byteLimited = new M3SyntaxGreenPool(100, 0);
        reject(IllegalStateException.class, () -> byteLimited.intern(a));
        ok(byteLimited.uniqueGreens() == 0 && byteLimited.retainedLiteralBytes() == 0,
                "literal budget refusal leaves no retained syntax");
        // Two leaves can be admitted but an ancestor exceeds literal capacity;
        // the next import must start from an empty canonical pool.
        var partial = new M3SyntaxGreenPool(100, 2);
        reject(IllegalStateException.class, () -> partial.intern(a));
        ok(partial.uniqueGreens() == 0 && partial.childReferences() == 0
                && partial.retainedLiteralBytes() == 0,
                "ancestor capacity rejection rolls back already-interned leaves");
        var simple = M3SyntaxTree.builder("tiny", new byte[] {'x'});
        simple.open("token", "", 0, M3SyntaxTree.TOKEN);
        simple.close(1);
        var recoveredTiny = partial.intern(simple.build());
        ok(recoveredTiny.rootGreen() == 0 && partial.uniqueGreens() == 1
                && Arrays.equals(partial.render(0, 1), new byte[] {'x'}),
                "a previously refused pool remains reusable without corrupted hash slots");
        byte[] utf8 = "é😀".getBytes(StandardCharsets.UTF_8);
        var mono = M3SyntaxTree.builder("unicode", utf8);
        mono.open("file", "", 0, 0);
        mono.close(utf8.length);
        var unicode = pool.intern(mono.build());
        ok(Arrays.equals(pool.render(unicode.rootGreen(), utf8.length), utf8),
                "non-ASCII bytes survive exact green reconstruction");
        // 5,000-deep proof: bounded primitive lanes and iterative writer, no stack overflow.
        var deepBuilder = M3SyntaxTree.builder("deep", new byte[] {7});
        for (int i = 0; i < 5000; i++) deepBuilder.open("node", "", 0, 0);
        for (int i = 0; i < 5000; i++) deepBuilder.close(1);
        var deep = pool.intern(deepBuilder.build());
        ok(Arrays.equals(pool.render(deep.rootGreen(), 1), new byte[] {7}), "deep green DAG renders");
        int unique = pool.uniqueGreens();
        pool.intern(deepBuilderAgain());
        ok(pool.uniqueGreens() == unique, "repeated deep green shapes interned once");
        // Fixed-seed differential source reconstruction and interned-token stability.
        Random random = new Random(0x4d334153);
        for (int iteration = 0; iteration < 800; iteration++) {
            String prefix = " ".repeat(random.nextInt(4));
            String join = new String[] {" + ", " /* x */ ", ";", "\r\n", "="}[random.nextInt(5)];
            String suffix = "\t".repeat(random.nextInt(3));
            String text = prefix + "a" + join + "b" + suffix;
            int start = prefix.length(), other = start + 1 + join.length();
            M3SyntaxTree projected = parse(text, "java/21", start, other, "name", "left");
            M3SyntaxGreenPool.Projection proof = pool.intern(projected);
            ok(Arrays.equals(pool.render(proof.rootGreen(), projected.sourceLength()),
                    projected.sourceBytes()), "fixed-seed lossless green reconstruction");
            ok(pool.intern(projected).rootGreen() == proof.rootGreen(),
                    "fixed-seed fixed-point structural admission");
            ok(proof.greenAt(1) == first.greenAt(1) && proof.greenAt(2) == first.greenAt(2),
                    "fixed-seed stable leaf reuse across changed gaps");
        }
        System.out.println("M3_SYNTAX_GREEN_POOL_PASS checks=" + checks);
    }
}
