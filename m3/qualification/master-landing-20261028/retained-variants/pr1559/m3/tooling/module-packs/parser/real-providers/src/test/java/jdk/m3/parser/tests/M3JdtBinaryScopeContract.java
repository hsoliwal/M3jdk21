// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/** Selected JDT source generations with supplied, unchanged binary dependencies. */
public final class M3JdtBinaryScopeContract {
    private static final M3ASTIngress.Limits LIMITS = new M3ASTIngress.Limits(100_000, 10_000, 100);
    private static int checks, generations, projections;
    private M3JdtBinaryScopeContract() { }
    private static void check(boolean accepted, String message) {
        checks++;
        if (!accepted) throw new AssertionError(message);
    }
    private static byte[] digest(Path file) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
    }
    private static void generation(Path binaries, String code, Path dependency, byte[] expectedBytes) throws Exception {
        int before = M3JdtRewriteAgent.PARSE_ENTRIES.get();
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("demo/Use.java");
        parser.setSource(code.toCharArray());
        parser.setEnvironment(new String[] { binaries.toString() }, new String[0], null, true);
        parser.setResolveBindings(true);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        parser.setCompilerOptions(options);
        CompilationUnit tree = (CompilationUnit) parser.createAST(null);
        generations++;
        check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == before + 1, "one JDT parse for this selected source generation");
        for (var problem : tree.getProblems()) check(!problem.isError(), problem.toString());
        TypeDeclaration use = (TypeDeclaration) tree.types().get(0);
        check(use.resolveBinding() != null && use.resolveBinding().isFromSource(), "selected source owns its type");
        MethodInvocation[] invocation = new MethodInvocation[1];
        tree.accept(new ASTVisitor() {
            @Override public boolean visit(MethodInvocation node) {
                check(invocation[0] == null, "fixture contains exactly one external call");
                invocation[0] = node;
                return true;
            }
        });
        check(invocation[0] != null, "external method invocation parsed");
        IMethodBinding binding = invocation[0].resolveMethodBinding();
        check(binding != null && !binding.isRecovered(), "supplied method symbol resolved without dependency source");
        check(binding.getDeclaringClass().getQualifiedName().equals("external.Provided"), "given binary symbol owner");
        check(!binding.getDeclaringClass().isFromSource(), "dependency binding originates in a class file");
        check(binding.getReturnType().getName().equals("int"), "binary signature is authoritative");
        check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == before + 1, "binding reads do not create another ASTParser entry");
        M3File source = M3File.of(URI.create("memory:/demo/Use.java"), code);
        M3ASTIndex original = M3ASTAuthoritativeModels.eclipseJdt(source, tree, code, LIMITS);
        check(original.hasExactRange(0), "original selected source keeps its exact range");
        ASTRewrite plan = ASTRewrite.create(tree.getAST());
        for (int pass = 0; pass < 3; pass++) {
            int entries = M3JdtRewriteAgent.PARSE_ENTRIES.get();
            M3ASTIndex projected = M3ASTAuthoritativeModels.eclipseJdtRewrite(
                    source, tree, code, plan, tree.getAST().modificationCount(), LIMITS);
            projections++;
            check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == entries, "repeated direct projection adds no parser entry");
            check(projected.source() == source && projected.nodeCount() == original.nodeCount(), "same source and original nodes reused");
            check(!projected.hasExactRange(0), "planned positions remain unknown");
            check(invocation[0].resolveMethodBinding() == binding, "given external binding identity reused");
        }
        // A transformed name does not cause this structural API to fabricate fresh binding semantics.
        ASTNode changedName = tree.getAST().newSimpleName("different");
        plan.set(invocation[0], MethodInvocation.NAME_PROPERTY, changedName, null);
        int entries = M3JdtRewriteAgent.PARSE_ENTRIES.get();
        M3ASTAuthoritativeModels.eclipseJdtRewrite(source, tree, code, plan,
                tree.getAST().modificationCount(), LIMITS);
        projections++;
        check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == entries, "changed plan reuses the parsed owner");
        check(invocation[0].resolveMethodBinding() == binding && binding.getName().equals("answer"),
                "original binding context stays original after planned name edit");
        check(Arrays.equals(digest(dependency), expectedBytes), "dependency class bytes unchanged after generation");
    }
    public static void main(String[] args) throws Exception {
        check(M3JdtRewriteAgent.active, "actual ASTParser entry instrumentation required");
        if (args.length != 1) throw new IllegalArgumentException("one absent fixture directory required");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(root)) throw new IllegalArgumentException("fixture directory must be absent");
        Files.createDirectory(root);
        Path source = root.resolve("Provided.java");
        Path binaries = Files.createDirectory(root.resolve("classes"));
        Files.writeString(source, "package external; public class Provided { public static int answer(){ return 42; } }",
                StandardCharsets.UTF_8);
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "Java 21 compiler available to prepare binary fixture");
        check(compiler.run(null, null, null, "--release", "21", "-d", binaries.toString(), source.toString()) == 0,
                "dependency compiled once before any selected JDT source");
        Path dependency = binaries.resolve("external/Provided.class");
        byte[] initial = digest(dependency);
        Files.delete(source);
        check(!Files.exists(source), "dependency source is unavailable during selected JDT parsing");
        generation(binaries, "package demo; import external.Provided; class Use { int n=Provided.answer(); }",
                dependency, initial);
        generation(binaries, "package demo; import external.Provided; class Use { int n=Provided.answer()+1; }",
                dependency, initial);
        check(generations == 2 && M3JdtRewriteAgent.PARSE_ENTRIES.get() == 2,
                "two selected source generations use exactly two actual JDT parser entries");
        check(Arrays.equals(digest(dependency), initial) && !Files.exists(source), "binary dependency stays given and unchanged");
        System.out.println("M3_JDT_BINARY_SCOPE_PASS checks=" + checks + " sourceGenerations=" + generations
                + " parserEntries=" + M3JdtRewriteAgent.PARSE_ENTRIES.get() + " directProjections=" + projections
                + " projectionParserEntries=0 emissionParserEntries=0 dependencySourcePresent=false");
    }
}
