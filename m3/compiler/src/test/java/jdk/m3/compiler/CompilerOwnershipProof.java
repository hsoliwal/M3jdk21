// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import javax.tools.ToolProvider;

/** Executable proof over actual Java21 compiler trees and exact production classes. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/compiler-ownership-20261014/CompilerOwnershipProof.java blob fbc492e8c899; recipe m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1
public final class CompilerOwnershipProof {
    private static int assertions;
    private static int compilations;
    private CompilerOwnershipProof() { }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void refuses(Runnable action, Class<? extends Throwable> kind) {
        try { action.run(); }
        catch (Throwable error) { check(kind.isInstance(error), "wrong failure: " + error); return; }
        throw new AssertionError("missing refusal " + kind);
    }
    public static void main(String[] args) throws Exception {
        loops(); ownership(); processors(Path.of(args[0])); mutationCounterexamples(Path.of(args[0]));
        System.out.println("CompilerOwnershipProof assertions=" + assertions + " compilerInvocations=" + compilations + " PASS");
    }
    private static boolean condition(long value, int bound, StaticUsePlan.Relation relation) {
        return switch (relation) {
            case LT -> value < bound; case LE -> value <= bound;
            case GT -> value > bound; case GE -> value >= bound;
        };
    }
    private static void loops() {
        for (int start=-24; start<=24; start++) for (int bound=-24; bound<=24; bound++)
            for (int step=-7; step<=7; step++) if (step!=0)
                for (var relation : StaticUsePlan.Relation.values()) {
                    if ((step>0) != (relation==StaticUsePlan.Relation.LT || relation==StaticUsePlan.Relation.LE)) continue;
                    int value=start; long count=0;
                    while (condition(value,bound,relation)) { count++; value+=step; }
                    check(new StaticUsePlan.IntLoop(start,step,relation).conditionBound(bound)==count,"small independent loop oracle");
                }
        var random=new Random(0x3c0ffeeL);
        for (int sample=0; sample<20_000; sample++) {
            int start=random.nextInt(), bound=random.nextInt(), step=random.nextInt();
            if (step==0) step=1;
            var relation=StaticUsePlan.Relation.values()[step>0 ? random.nextInt(2) : 2+random.nextInt(2)];
            var loop=new StaticUsePlan.IntLoop(start,step,relation);
            BigInteger first=BigInteger.valueOf(start), limit=BigInteger.valueOf(bound), increment=BigInteger.valueOf(step);
            BigInteger count;
            if (!condition(start,bound,relation)) count=BigInteger.ZERO;
            else {
                BigInteger distance=limit.subtract(first).abs();
                boolean inclusive=relation==StaticUsePlan.Relation.LE || relation==StaticUsePlan.Relation.GE;
                if (!inclusive) distance=distance.subtract(BigInteger.ONE);
                count=distance.divide(increment.abs()).add(BigInteger.ONE);
            }
            BigInteger exit=first.add(count.multiply(increment));
            if (exit.bitLength()>31) refuses(() -> loop.conditionBound(bound), ArithmeticException.class);
            else check(loop.conditionBound(bound)==count.longValueExact(),"big-integer boundary oracle");
        }
        check(new StaticUsePlan.IntLoop(Integer.MIN_VALUE,1,StaticUsePlan.Relation.LT).conditionBound(Integer.MAX_VALUE)==4_294_967_295L,"long multiplicity");
        refuses(() -> new StaticUsePlan.IntLoop(0,1,StaticUsePlan.Relation.LE).conditionBound(Integer.MAX_VALUE),ArithmeticException.class);
        refuses(() -> new StaticUsePlan.IntLoop(0,-1,StaticUsePlan.Relation.GE).conditionBound(Integer.MIN_VALUE),ArithmeticException.class);
        refuses(() -> new StaticUsePlan.IntLoop(0,0,StaticUsePlan.Relation.LT),IllegalArgumentException.class);
        refuses(() -> new StaticUsePlan.IntLoop(0,-1,StaticUsePlan.Relation.LT),IllegalArgumentException.class);
    }
    private static void ownership() {
        for (var escape:StaticUsePlan.Escape.values()) for (var effect:StaticUsePlan.Effect.values())
            for (int flags=0;flags<512;flags++) {
                var facts=new StaticUsePlan.Facts(17,escape,effect,
                        bit(flags,0),bit(flags,1),bit(flags,2),bit(flags,3),bit(flags,4),bit(flags,5),bit(flags,6),bit(flags,7),bit(flags,8));
                var expected=!bit(flags,7)||!bit(flags,8)||escape==StaticUsePlan.Escape.UNKNOWN||effect==StaticUsePlan.Effect.UNKNOWN
                        ? StaticUsePlan.Decision.REFUSE
                        : (flags&127)!=0||escape==StaticUsePlan.Escape.PUBLISHED ? StaticUsePlan.Decision.MATERIALIZE
                        : effect==StaticUsePlan.Effect.PARENT_BITS_ONLY ? StaticUsePlan.Decision.DIRECT_PARENT
                        : effect==StaticUsePlan.Effect.READONLY_PARENT ? StaticUsePlan.Decision.LAZY_HANDLE : StaticUsePlan.Decision.MATERIALIZE;
                check(StaticUsePlan.evaluate(facts).decision()==expected,"existing decision truth table");
                for(boolean shared: new boolean[]{false,true}) {
                    var mode=expected==StaticUsePlan.Decision.REFUSE ? StaticUsePlan.ReferenceMode.UNKNOWN
                            : expected==StaticUsePlan.Decision.MATERIALIZE ? StaticUsePlan.ReferenceMode.KEEP_JVM_REACHABILITY
                            : shared ? StaticUsePlan.ReferenceMode.OWNER_EDGE_COUNT_REQUIRED : StaticUsePlan.ReferenceMode.NO_WRAPPER_COUNT;
                    check(StaticUsePlan.referenceMode(facts,shared)==mode,"backing counts not wrapper sites");
                }
            }
        check(StaticUsePlan.CompilerSummary.class.getAnnotation(Retention.class).value()==RetentionPolicy.CLASS,"class retention");
    }
    private static boolean bit(int value,int bit) { return (value&(1<<bit))!=0; }
    private record Result(boolean success,String diagnostics,Path root,Map<String,String> generated) {
        String text() { return String.join("\n",generated.values()); }
    }
    private static Result compile(Path root,String source,Runnable progress) throws IOException {
        compilations++;
        Files.createDirectories(root);
        Path input=root.resolve("Fixture.java"), out=root.resolve("classes"), gen=root.resolve("generated");
        Files.writeString(input,source); Files.createDirectories(out); Files.createDirectories(gen);
        var compiler=ToolProvider.getSystemJavaCompiler();
        var diagnostics=new StringWriter();
        try(var manager=compiler.getStandardFileManager(null,null,StandardCharsets.UTF_8)) {
            var task=compiler.getTask(diagnostics,manager,null,List.of("--release","21","-Xlint:all","-Werror","-classpath",System.getProperty("java.class.path"),"-d",out.toString(),"-s",gen.toString()),null,manager.getJavaFileObjects(input));
            task.setProcessors(List.of(new StaticFactsProcessor(progress)));
            boolean success=task.call();
            Map<String,String> generated=new TreeMap<>();
            try(var paths=Files.walk(gen)) {
                for(Path path:paths.filter(Files::isRegularFile).sorted().toList()) generated.put(gen.relativize(path).toString(),Files.readString(path));
            }
            return new Result(success,diagnostics.toString(),root,generated);
        }
    }
    private static String fixture(String body) {
        return "package fixture;\nimport jdk.m3.compiler.StaticUsePlan.Analyze;\npublic final class Fixture {\n @Analyze public static int f(int n,int[] values) { int total=0; " + body + " return total; }\n}\n";
    }
    private static void processors(Path work) throws Exception {
        List<String[]> cases=List.of(
                new String[]{"for(int i=0;i<n;i++) { (i)--; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { (i)+=2; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { (i)=0; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { (n)++; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { (n)=8; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { (values)=new int[2]; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((i))--; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((i))+=2; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((i))=0; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((n))++; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((n))=8; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { ((values))=new int[2]; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((i))))--; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((i))))+=2; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((i))))=0; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((n))))++; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((n))))=8; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { ((((values))))=new int[2]; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((((((i))))))))--; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((((((i))))))))+=2; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((((((i))))))))=0; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((((((n))))))))++; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { ((((((((n))))))))=8; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { ((((((((values))))))))=new int[2]; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<10;i++) total+=i;","conditionBound=10","1"},
                new String[]{"for(int i=0;i<n;i+=2) total+=i;","PARAM(n)","1"},
                new String[]{"for(int i=0;i<values.length;i++) total+=values[i];","ARRAY_LENGTH(values)","1"},
                new String[]{"for(int i=10;i>=0;i--) total+=i;","conditionBound=11","1"},
                new String[]{"for(int i=0;i<10;i++) { if(n==i) break; total++; }","NOT_EXACT_EXECUTIONS","1"},
                new String[]{"for(int i=0;i<n;i++) { n--; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<n;i++) { i+=2; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { values=new int[0]; }",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<values.length;i++) { values[i]=7; }","ARRAY_LENGTH(values)","1"},
                new String[]{"while(total<n) total++;",":UNKNOWN","1"},
                new String[]{"for(int x:values) total+=x;",":UNKNOWN","1"},
                new String[]{"for(long i=0;i<n;i++) total++;",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<=2147483647;i++) total++;",":UNKNOWN","1"},
                new String[]{"String s=\"for(int i=0;i<10;i++)\"; // for(int i=0;i<10;i++)\n total=s.length();","loopBounds={}","0"},
                new String[]{"Runnable r=() -> { for(int i=0;i<10;i++) System.out.print(i); }; total=r.hashCode();","DEFERRED_LAMBDA_BODY","0"},
                new String[]{"for(int i=0;i<3;i++) for(int j=0;j<n;j++) total++;","PARAM(n)","2"},
                new String[]{"for(int i=0;i<10;i*=2) total++;",":UNKNOWN","1"},
                new String[]{"for(int i=0;i<10;i++) { if(i==n) throw new IllegalArgumentException(); }","NOT_EXACT_EXECUTIONS","1"},
                new String[]{"for(int i=0;i<n;i++) { if(i==2) continue; total++; }","PARAM(n)","1"},
                new String[]{"int limit=n; for(int i=0;i<limit;i++) total++;",":UNKNOWN","1"}
        );
        int number=0;
        for(var test:cases) {
            String source=fixture(test[0]);
            Result result=compile(work.resolve("case-"+number++),source,null);
            check(result.success,result.diagnostics);
            check(result.generated.size()==1,"one companion per annotated method");
            check(result.text().contains(test[1]),"expected metadata "+test[1]+" in "+result.text());
            check(result.text().contains("RUNTIME_REFERENCE_COUNTS_UNPROVEN"),"never trust source sites as counts");
            String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
            check(result.text().contains(sha),"exact decoded-source hash");
            String sourceText=result.text();
            int loopCount=sourceText.split("(?:CONDITION_BOUND;|:UNKNOWN)",-1).length-1;
            check(loopCount==Integer.parseInt(test[2]),"all admitted and unknown loops counted");
            check(Files.exists(result.root.resolve("classes/META-INF/m3/compiler-facts.tsv")),"searchable ledger");
        }
        String same=fixture("for(int i=0;i<n;i+=2) total+=i;");
        Result first=compile(work.resolve("replay-first"),same,null),second=compile(work.resolve("replay-second"),same,null);
        check(first.success&&second.success,"deterministic compiles");
        check(first.generated.equals(second.generated),"path-independent generated source");
        check(Arrays.equals(Files.readAllBytes(first.root.resolve("classes/META-INF/m3/compiler-facts.tsv")),Files.readAllBytes(second.root.resolve("classes/META-INF/m3/compiler-facts.tsv"))),"deterministic ledger");
        String generated=first.generated.keySet().iterator().next();
        String classFile=generated.substring(0,generated.length()-5)+".class";
        byte[] bytecode=Files.readAllBytes(first.root.resolve("classes").resolve(classFile));
        check(Arrays.equals(bytecode,Files.readAllBytes(second.root.resolve("classes").resolve(classFile))),"deterministic companion class");
        String className=classFile.substring(0,classFile.length()-6).replace('/','.').replace('\\','.');
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","javap").toString(),"-v","-classpath",first.root.resolve("classes").toString(),className).redirectErrorStream(true).start();
        String javap=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        check(process.waitFor()==0&&javap.contains("RuntimeInvisibleAnnotations:")&&javap.contains("CompilerSummary("),"javap confirms CLASS annotation");
        Result changed=compile(work.resolve("changed"),same.replace("total+=i","total+=i+1"),null);
        check(changed.success&&!changed.generated.equals(first.generated),"source mutation invalidates summary hash");
        String overloaded="package fixture; import jdk.m3.compiler.StaticUsePlan.Analyze; public final class Fixture { @Analyze static void f(int n){} @Analyze static void f(long n){} }";
        Result overload=compile(work.resolve("overloads"),overloaded,null);
        check(overload.success&&overload.generated.size()==2,"overload-specific method keys");
        Result budget=compile(work.resolve("budget"),fixture("for(int i=0;i<1;i++) total++;".repeat(129)),null);
        check(!budget.success&&budget.diagnostics.contains("loop metadata budget"),"excess metadata refused");
        try { compile(work.resolve("cancel"),same,() -> { throw new CancellationException("fixture"); }); throw new AssertionError("cancel"); }
        catch(RuntimeException expected) { check(expected instanceof CancellationException||expected.getCause() instanceof CancellationException,"progress cancellation"); }
    }

    /** Actual compiled executions disprove the old condition bound; loops are capped. */
    private static void mutationCounterexamples(Path work) throws Exception {
        int ordinal=0;
        for (String mutation : List.of("((i))--;", "((n))++;")) {
            String body="for(int i=0;i<n;i++) { total++; if(total==8) break; " + mutation + " }";
            Result result=compile(work.resolve("mutation-runtime-"+ordinal++),fixture(body),null);
            check(result.success,result.diagnostics);
            check(result.text().contains(":UNKNOWN"),"M3-PAREN-WRITE: mutated loop was admitted");
            try (var loader=new java.net.URLClassLoader(new java.net.URL[]{result.root.resolve("classes").toUri().toURL()}, CompilerOwnershipProof.class.getClassLoader())) {
                Object observed=loader.loadClass("fixture.Fixture").getMethod("f",int.class,int[].class).invoke(null,3,new int[0]);
                check(observed.equals(8),"bounded counterexample execution");
                check((Integer)observed > new StaticUsePlan.IntLoop(0,1,StaticUsePlan.Relation.LT).conditionBound(3),"old bound is false, not merely imprecise");
            }
        }
    }
}
