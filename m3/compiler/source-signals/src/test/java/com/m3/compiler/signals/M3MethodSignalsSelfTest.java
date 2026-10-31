/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.compiler.signals;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Deterministic actual javac ANALYZE oracle; source strings/comments are never calls. */
public final class M3MethodSignalsSelfTest {
    private static long checks;
    private M3MethodSignalsSelfTest() { }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-method-signals-");
        try {
            String a = scan(sample(1), root.resolve("a.tsv"));
            String b = scan(sample(2), root.resolve("b.tsv"));
            check(a.equals(b), "declaration permutation changes method facts");
            check(a.startsWith("# M3JDK21-METHOD-SIGNALS/1\n"), "schema");
            int trailer = a.lastIndexOf("# sha256\t");
            check(trailer > 0 && trailer + 74 == a.length(), "trailer geometry");
            String body = a.substring(0, trailer);
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
            check(a.substring(trailer+9, a.length()-1).equals(digest), "trailer digest");
            check(a.contains("call:DIRECT:test.Example#twice(int;):int"), "direct static call");
            check(a.contains("call:DISPATCH_CANDIDATE:test.Worker#go():void"), "virtual candidate call");
            check(a.contains("new:DIRECT:test.Worker#<init>():void"), "new call");
            check(a.contains("M\ttest.Example#outer(int;):void\t"), "attributed method identity");
            check(!a.contains("fakeCall"), "code-looking string/comment must be masked by AST");
            check(a.contains("# sha256\t"), "integrity trailer");
            String first = a;
            scan(sample(1), root.resolve("a.tsv"));
            check(first.equals(Files.readString(root.resolve("a.tsv"))), "idempotent replay");
            Random random = new Random(33456);
            for (int round=0; round<100; round++) {
                String s=scan(sample(random.nextBoolean()?1:2), root.resolve("r"+round+".tsv"));
                check(s.equals(first), "fixed source forms must converge");
            }
            System.out.println("PASS M3MethodSignalsSelfTest checks="+checks);
        } finally {
            try(var files=Files.walk(root)){
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}
                    catch(Exception e){throw new IllegalStateException(e);}
                });
            }
        }
    }
    private static String scan(String source, Path target) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> errors=new DiagnosticCollector<>();
        JavaFileObject code=new SimpleJavaFileObject(URI.create("string:///test/Example.java"),JavaFileObject.Kind.SOURCE){
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors){return source;}
        };
        try(var fm=compiler.getStandardFileManager(errors,null,StandardCharsets.UTF_8)){
            JavacTask task=(JavacTask)compiler.getTask(null,fm,errors,
                    List.of("--release","21","-proc:none"),null,List.of(code));
            task.parse(); task.analyze();
            if(errors.getDiagnostics().stream().anyMatch(d->
                    d.getKind()==javax.tools.Diagnostic.Kind.ERROR)){
                throw new AssertionError(errors.getDiagnostics().toString());
            }
            Trees trees=Trees.instance(task);
            TypeElement owner=task.getElements().getTypeElement("test.Example");
            Objects.requireNonNull(owner);
            TreePath path=trees.getPath(owner);
            Objects.requireNonNull(path);
            M3MethodSignals signals=new M3MethodSignals(trees,target,M3MethodSignalsSelfTest::signature);
            signals.collect(path);signals.publish();
        }
        return Files.readString(target);
    }
    private static String signature(ExecutableElement method){
        StringBuilder b=new StringBuilder();
        b.append(method.getEnclosingElement()).append('#').append(method.getSimpleName()).append('(');
        for(VariableElement p:method.getParameters())b.append(p.asType()).append(';');
        return b.append("):").append(method.getReturnType()).toString();
    }
    private static String sample(int order){
        String twice="    static int twice(int value) { return value * 2; }\n";
        String outer="    void outer(int n) { int x = count; this.count += n; count++; if (n > 0) count = twice(n); for(int i=0;i<n;i++){ new Worker().go(); } Runnable r = () -> twice(n); String fake = \"fakeCall()\"; /* fakeCall(); */ if (r == null) throw new IllegalStateException(fake + x); }\n";
        return "package test; class Example { int count; " + (order==1?twice+outer:outer+twice)
                +"  private void helper(){} } class Worker { void go(){} }\n";
    }
    private static void check(boolean valid,String name){
        checks++;
        if(!valid)throw new AssertionError(name);
    }
}
