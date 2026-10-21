// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.signals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Real JavacTask + Plugin ServiceLoader compiler tests, no javac internal mocks. */
public final class M3SourceSignalPluginSelfTest {
    private static int checks;
    private static final String ANNOTATION = """
            package com.m3.collections.sortable;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            @Retention(RetentionPolicy.SOURCE) @Target(ElementType.TYPE)
            public @interface Sortable { String signal() default ""; }
            """;
    private static final String MODEL = """
            package demo;
            import com.m3.collections.sortable.Sortable;
            import java.util.List;
            import java.util.ArrayList;
            @Sortable(signal="signal")
            public final class Model implements Runnable {
                private final List<String> names;
                private final int age;
                public Model(List<String> names, int age) {
                    this.names = names;
                    this.age = age;
                }
                public double signal() { return age + names.size(); }
                public int count(String prefix) {
                    int sum=0;
                    for (String name : names) {
                        if (name.startsWith(prefix)) sum++;
                    }
                    return sum + Math.max(0, age);
                }
                public List<String> make() { return new ArrayList<>(names); }
                @Override public void run() { count("A"); }
            }
            """;
    private static final String OTHER = """
            package demo;
            public final class Other {
                public static int only() { return 7; }
            }
            """;

    private M3SourceSignalPluginSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-source-signals-contract-");
        try {
            annotatedAndCompanionCoexist(root);
            deterministicAcrossCompilationPermutations(root);
            mutationChangesSignals(root);
            invalidOptionsReject(root);
            System.out.println("PASS M3 source-signal compiler checks=" + checks);
        } finally {
            try (var walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void annotatedAndCompanionCoexist(Path root) throws Exception {
        Path dir = root.resolve("annotated");
        Path annotation = source(dir,"com/m3/collections/sortable/Sortable.java",ANNOTATION);
        Path model = source(dir,"demo/Model.java",MODEL);
        Path other = source(dir,"demo/Other.java",OTHER);
        Path sidecar = dir.resolve("out/m3-signals.tsv");
        compile(dir,sidecar,List.of(other,annotation,model),"include=annotated");
        String output = Files.readString(sidecar, StandardCharsets.UTF_8);
        check(output.startsWith("# M3JDK21-SOURCE-SIGNALS/1\n"),"schema");
        check(output.contains("S\tdemo.Model\t"),"annotated type emitted");
        check(!output.contains("S\tdemo.Other\t"),"unannotated type excluded");
        check(output.contains("\tFIELDS\t"),"field fingerprint family");
        check(output.contains("\tMETHODS\t"),"method fingerprint family");
        check(output.contains("\tDEPENDENCIES\t"),"dependency fingerprint family");
        check(output.contains("\tINHERITANCE\t"),"inheritance fingerprint family");
        check(output.contains("\tANNOTATIONS\t"),"annotation fingerprint family");
        check(output.contains("E\tdemo.Model\tcall:DISPATCH_CANDIDATE:java.lang.String#startsWith"),
                "attributed virtual call edge is not overclaimed exact");
        check(output.contains("E\tdemo.Model\tcall:DIRECT:java.lang.Math#max"),
                "attributed static call edge is direct");
        check(output.contains("E\tdemo.Model\tnew:DIRECT:java.util.ArrayList#<init>"),
                "constructor allocation edge");
        check(output.contains("\t1\t"),"primitive counters emitted");
        verifyChecksum(output);
        Path identical=dir.resolve("out/m3-signals-identical.tsv");
        compile(dir,identical,List.of(model,other,annotation),"include=annotated");
        check(Arrays.equals(Files.readAllBytes(sidecar),Files.readAllBytes(identical)),
                "source argument ordering cannot perturb emitted facts");
    }

    private static void deterministicAcrossCompilationPermutations(Path root) throws Exception {
        for (int permutation=0;permutation<12;permutation++) {
            Path dir = root.resolve("permutation-" + permutation);
            String methods = permutation % 2 == 0
                    ? "public int left(){return 1;} public int right(){return 2;}"
                    : "public int right(){return 2;} public int left(){return 1;}";
            String fields = permutation % 3 == 0
                    ? "private int a; private long b;"
                    : "private long b; private int a;";
            String body = "package demo; public final class Shape { " + fields + methods + " }";
            Path shape = source(dir,"demo/Shape.java",body);
            Path out = dir.resolve("source-signals.tsv");
            compile(dir,out,List.of(shape),"include=all");
            String content=Files.readString(out,StandardCharsets.UTF_8);
            verifyChecksum(content);
            if(permutation==0){baseline=content;}
            check(content.equals(baseline),"field/method declaration permutation " + permutation);
        }
    }
    private static String baseline;

    private static void mutationChangesSignals(Path root) throws Exception {
        Path dir=root.resolve("mutation");
        Path source=source(dir,"demo/Shape.java",
                "package demo; public final class Shape { int alpha; void run(){} }");
        Path out=dir.resolve("metrics.tsv");
        compile(dir,out,List.of(source),"include=all");
        String before=Files.readString(out,StandardCharsets.UTF_8);
        Files.writeString(source,
                "package demo; public final class Shape { int alpha; int beta; void run(){} }");
        compile(dir,out,List.of(source),"include=all");
        String after=Files.readString(out,StandardCharsets.UTF_8);
        check(!before.equals(after),"source mutations invalidate source-signal facts");
        verifyChecksum(after);
    }

    private static void invalidOptionsReject(Path root) throws Exception {
        Path dir=root.resolve("bad-options");
        Path source=source(dir,"demo/Shape.java","package demo; public class Shape {}");
        boolean rejected=false;
        try { compile(dir,dir.resolve("signals.tsv"),List.of(source),"unknown-option"); }
        catch (IllegalArgumentException problem) { rejected=true; }
        check(rejected,"unknown options fail closed");
    }

    private static Path source(Path root,String relative,String text) throws IOException {
        Path path=root.resolve("src").resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path,text,StandardCharsets.UTF_8);
        return path;
    }

    private static void compile(Path dir,Path outfile,List<Path> sources,String mode) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler==null) throw new IllegalStateException("JDK compiler unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        Files.createDirectories(dir.resolve("classes"));
        // Maven uses target/classes, while standalone javac tests use the same ServiceLoader resource.
        Path plugin=Path.of(M3SourceSignalPlugin.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(
                diagnostics,null,StandardCharsets.UTF_8)) {
            var units=manager.getJavaFileObjectsFromPaths(sources);
            List<String> options=List.of("--release","21","-proc:none","-Xlint:all",
                    "-processorpath",plugin.toString(),
                    "-Xplugin:M3SourceSignals out=" + outfile + " " + mode,
                    "-d",dir.resolve("classes").toString());
            boolean success=compiler.getTask(null,manager,diagnostics,options,null,units).call();
            if(!success) throw new AssertionError("javac compilation failed: " + diagnostics.getDiagnostics());
        }
    }

    private static void verifyChecksum(String data) {
        int start=data.lastIndexOf("# sha256\t");
        check(start>=0,"checksum trailer available");
        String got=data.substring(start+9).trim();
        String expected;
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256")
                    .digest(data.substring(0,start).getBytes(StandardCharsets.UTF_8));
            expected=HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        check(expected.equals(got),"sidecar integrity checksum");
    }

    private static void check(boolean condition,String message) {
        checks++;
        if(!condition) throw new AssertionError(message);
    }
}
