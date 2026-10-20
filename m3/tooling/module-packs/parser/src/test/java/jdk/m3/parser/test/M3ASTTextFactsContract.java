/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Stock-JDK-compatible contract for M3AST source-range lexical facts.
 */
public final class M3ASTTextFactsContract {
    private static int checks;

    public static void main(String[] args) throws Exception {
        String content = "class Facts { String value = \"beta Gamma42 reason\"; }";
        var source = new SimpleJavaFileObject(
                URI.create("memory:/Facts.java"),
                javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) {
                return content;
            }
        };

        try (var files = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(
                    null, files, null, List.of("-proc:none"), null, List.of(source));
            try (M3AST ast = M3AST.open(task)) {
                ast.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        M3AST.Source retained = unit.source();
                        int begin = retained.text().indexOf("beta");
                        int end = begin + "beta".length();
                        M3AST.TextFacts facts = unit.textFacts(begin, end);
                        String expected = retained.text().substring(begin, end);

                        check(facts.utf16Length() == expected.length(), "length");
                        check(facts.javaHash() == expected.hashCode(), "hash");
                        check(facts.codePointCount()
                                == expected.codePointCount(0, expected.length()), "code points");
                        check(facts.ascii(), "beta is ASCII");
                        check(facts.latin1(), "beta is Latin-1");
                        if (facts.signalsAvailable()) {
                            check(facts.m3Backed(), "signals require M3 storage");
                            check(facts.mayContainUnits(facts.bitSignal64()), "own unit signal");
                            check(facts.mayContainBigrams(facts.bigramSignal64()), "own bigram signal");
                            check(facts.mayContainTrigrams(facts.trigramSignal64()), "own trigram signal");
                        } else {
                            check(facts.mayContainUnits(~0L), "fallback unit filter is fail-open");
                            check(facts.mayContainBigrams(~0L), "fallback bigram filter is fail-open");
                            check(facts.mayContainTrigrams(~0L), "fallback trigram filter is fail-open");
                        }
                    }
                });
                task.parse();
            }
        }
        System.out.println("M3_AST_STRING_FACTS_CONTRACT_PASS checks=" + checks);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
