// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/** Actual JDT parser, original M3AST owner; source binding remains unverified. */
@SuppressWarnings("deprecation")
public final class M3JDTRealProvenanceContract {
    private static int checks;
    private static void check(boolean valid) { checks++; if (!valid) throw new AssertionError(); }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 40; i++) {
            String code = "class C" + i + " { String s = \"😀é₹\"; int f(){return 42;} }";
            byte[] bytes = code.getBytes(StandardCharsets.UTF_8);
            M3File source = M3File.fromCapturedBytes(URI.create("memory:/jdt/C"+i+".java"),
                    bytes, StandardCharsets.UTF_8, bytes.length);
            ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
            parser.setSource(source.text().toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(false);
            CompilationUnit root = (CompilationUnit) parser.createAST(null);
            check(root != null && root.getLength() == source.length());
            var limits = new M3ASTIngress.Limits(source.length(), 1000, 100);
            M3ASTIndex ast = M3ASTIngress.eclipseJdt(source, root, limits);
            check(ast.source() == source && ast.nodeCount() > 3);
            for (int row = 0; row < ast.nodeCount(); row++) {
                check((ast.flags(row) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
                if (ast.hasExactRange(row))
                    check(ast.sourceText(row).equals(
                            source.text().substring(ast.start(row), ast.end(row))));
            }
            root.setFlags(root.getFlags() | ASTNode.RECOVERED);
            var changed = M3ASTIngress.eclipseJdt(source, root, limits);
            check((changed.flags(0) & M3ASTIndex.RECOVERED) != 0);
            check((changed.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
        }
        System.out.println("M3_JDT_REAL_SOURCE_PROVENANCE_PASS checks=" + checks);
    }
}
