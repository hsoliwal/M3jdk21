// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTSyntaxCoverage;
import jdk.m3.parser.M3File;

/** Original M3ASTIndex detached leaf topology and source-stream exactness. */
public final class M3ASTSyntaxCoverageContract {
    private static int checks;
    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label);
    }
    private static void reject(Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (IllegalArgumentException failure) { return; }
        throw new AssertionError("invalid syntax coverage was accepted");
    }
    private static M3ASTIndex index(String text, int[] start, int[] end, int[] tags,
                                   boolean recovered) {
        M3File file = M3File.of(URI.create("memory:/syntax"), text);
        var builder = M3ASTIndex.externalBuilder(file, "syntax-fixture", 500);
        int root = builder.add(-1, "document", 0, text.length(), M3ASTIndex.EXACT_RANGE
                | (recovered ? M3ASTIndex.RECOVERED : 0));
        builder.portable(root, null, 0);
        for (int i = 0; i < start.length; i++) {
            int row = builder.add(root, "leaf", start[i], end[i], M3ASTIndex.EXACT_RANGE);
            builder.portable(row, null, tags[i]);
        }
        return builder.seal();
    }
    public static void main(String[] args) {
        M3ASTIndex full = index("a + b",
                new int[] {0,1,2,3,4}, new int[] {1,2,3,4,5},
                new int[] {1,2,1,2,1}, false);
        var proof = M3ASTSyntaxCoverage.verify(full, 10, 10, false);
        check(proof.tokens() == 3 && proof.trivia() == 2 && proof.inserted() == 0
                && proof.coveredUtf16() == 5, "complete token and trivia coverage");
        check(proof.utf16Sha256().equals(full.source().utf16Sha256()), "UTF-16BE identity");
        reject(() -> M3ASTSyntaxCoverage.verify(full, 5, 4, false));
        reject(() -> M3ASTSyntaxCoverage.verify(full, 4, 5, false));
        reject(() -> M3ASTSyntaxCoverage.verify(index("a b",
                new int[]{0,2}, new int[]{1,3}, new int[]{1,1}, false), 10,10,false));
        reject(() -> M3ASTSyntaxCoverage.verify(index("a b",
                new int[]{0,1,2}, new int[]{1,3,3}, new int[]{1,2,1}, false), 10,10,false));
        reject(() -> M3ASTSyntaxCoverage.verify(index("abc",
                new int[]{0,1}, new int[]{2,3}, new int[]{1,1}, false), 10,10,false));
        reject(() -> M3ASTSyntaxCoverage.verify(index("abc",
                new int[]{0,1}, new int[]{1,3}, new int[]{1,0}, false), 10,10,false));
        reject(() -> M3ASTSyntaxCoverage.verify(index("abc",
                new int[]{0,1}, new int[]{1,3}, new int[]{1,3}, false), 10,10,false));
        M3ASTIndex recovered = index("a+b", new int[]{0,1,1,2},
                new int[]{1,1,2,3}, new int[]{1,9,1,1}, true);
        reject(() -> M3ASTSyntaxCoverage.verify(recovered,10,10,false));
        check(M3ASTSyntaxCoverage.verify(recovered,10,10,true).inserted() == 1,
                "only explicit zero-width missing token permitted");
        M3ASTIndex eofInsertion = index("a+b", new int[]{0,1,2,3},
                new int[]{1,2,3,3}, new int[]{1,1,1,8}, true);
        check(M3ASTSyntaxCoverage.verify(eofInsertion,10,10,true).inserted() == 1,
                "missing EOF token permitted when recovering");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 150; i++) text.append(i%3==0?'\n':i%3==1?'a':'€');
        int[] starts = new int[text.length()];
        int[] ends = new int[text.length()];
        int[] flags = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            starts[i] = i;
            ends[i] = i + 1;
            flags[i] = i % 3 == 0 ? M3ASTIndex.TRIVIA : M3ASTIndex.TOKEN;
        }
        var many = index(text.toString(), starts, ends, flags, false);
        var report = M3ASTSyntaxCoverage.verify(many, 200, 200, false);
        check(report.coveredUtf16() == 150 && report.trivia() == 50
                && report.tokens() == 100, "bounded exact linear source coverage");
        System.out.println("M3_AST_SYNTAX_COVERAGE_PASS checks=" + checks);
    }
}
