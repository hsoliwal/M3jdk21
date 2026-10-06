// SPDX-License-Identifier: Apache-2.0
package com.m3.lane28;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class Lane28Test {
    private static final Path ROOT = Path.of(System.getProperty("m3.root")).toAbsolutePath().normalize();
    private static final Path CRATE = Path.of(System.getProperty("m3.crate")).toAbsolutePath().normalize();
    private static final Path OUT = CRATE.resolve("target");
    private static final Path GENERATED = OUT.resolve("generated");
    private static final Path PATCH = OUT.resolve("patch");
    private static final Path PROBES = OUT.resolve("probes");
    private static final String PRODUCT = "src/java.base/share/classes/";
    private static final String KERNEL = PRODUCT + "jdk/internal/mindex/M3BitLane28.java";
    private static final String TEST = "test/jdk/jdk/internal/mindex/";
    private static final String JAVA_RES = "com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-lane28/";
    private static final String TEXT_RES = "com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-lane28/";
    private static final String JAVAC = Path.of(System.getProperty("java.home"),"bin/javac").toString();
    private static final String JAVA = Path.of(System.getProperty("java.home"),"bin/java").toString();
    private static List<SourceFile> javaAfter;
    private static List<SourceFile> textAfter;

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }

    private static List<SourceFile> apply(Recipe recipe, List<SourceFile> before) {
        return recipe.run(new InMemoryLargeSourceSet(before),context(),1).getChangeset()
                .getAllResults().stream().map(result -> result.getAfter()).toList();
    }

    private static List<SourceFile> textBefore() throws Exception {
        var result = new ArrayList<SourceFile>();
        for (String line : Files.readAllLines(CRATE.resolve("src/main/resources/"+TEXT_RES+"manifest.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] cells = line.split("\t");
            if (!cells[1].equals("ABSENT")) result.add(PlainText.builder().sourcePath(Path.of(cells[0]))
                    .text(Files.readString(CRATE.resolve("src/main/resources/"+TEXT_RES+cells[3]+".before"))).build());
        }
        return result;
    }

    @BeforeAll static void prepare() throws Exception {
        Files.createDirectories(OUT);
        for (String line : Files.readAllLines(CRATE.resolve("inputs.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] fields = line.split("\t");
            assertEquals(fields[1],hash(Files.readAllBytes(ROOT.resolve(fields[0]))),fields[0]);
        }
        javaAfter = apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),List.of());
        assertEquals(7,javaAfter.size());
        textAfter = apply(new M3Jdk21HashPinnedTextSnapshotRecipe("m3-lane28"),textBefore());
        for (SourceFile file : Stream.concat(javaAfter.stream(),textAfter.stream()).toList()) {
            Path target = GENERATED.resolve(file.getSourcePath()).normalize();
            assertTrue(target.startsWith(GENERATED));
            Files.createDirectories(target.getParent()); Files.writeString(target,file.printAll());
        }
        compileProduct(PATCH,GENERATED.resolve(KERNEL),"compile-product.log");
        Files.createDirectories(PROBES);
        execute(List.of(JAVAC,"-source","21","-target","21","-proc:none","-Xlint:all","-Werror",
                "--patch-module","java.base="+PATCH,"--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED",
                "-d",PROBES.toString(),GENERATED.resolve(TEST+"M3Lane28Test.java").toString(), GENERATED.resolve(TEST+"M3Lane28Cost.java").toString()),"compile-probes.log",true);
        compileNative(OUT,false,"compile-native.log");
    }

    @Test void exactRecipeAndFixedPoint() {
        assertTrue(apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),javaAfter).isEmpty());
        assertTrue(apply(new M3Jdk21HashPinnedTextSnapshotRecipe("m3-lane28"),textAfter).isEmpty());
    }

    @Test void refusesOccupiedWrongParserAndDuplicateJava() {
        var wrong = JavaParser.fromJavaVersion().build().parse("class Conflict {}")
                .findFirst().orElseThrow().withSourcePath(Path.of(KERNEL));
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),List.of(wrong)));
        var plain = PlainText.builder().sourcePath(Path.of(KERNEL)).text(javaAfter.stream()
                .filter(f->f.getSourcePath().toString().equals(KERNEL)).findFirst().orElseThrow().printAll()).build();
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),List.of(plain)));
        var duplicate = new ArrayList<>(javaAfter); duplicate.add(javaAfter.getFirst());
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),duplicate));
    }

    @Test void refusesTextDriftMissingAndDuplicate() throws Exception {
        var before = textBefore(); assertFalse(before.isEmpty());
        var missing = new ArrayList<>(before); missing.removeFirst();
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedTextSnapshotRecipe("m3-lane28"),missing));
        var duplicate = new ArrayList<>(before); duplicate.add(before.getFirst());
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedTextSnapshotRecipe("m3-lane28"),duplicate));
        var drift = new ArrayList<>(before);
        drift.set(0,PlainText.builder().sourcePath(before.getFirst().getSourcePath()).text(before.getFirst().printAll()+"drift").build());
        assertThrows(RuntimeException.class,()->apply(new M3Jdk21HashPinnedTextSnapshotRecipe("m3-lane28"),drift));
    }

    @Test void preservesUnrelatedSourceAndRejectsTemplateTampering() throws Exception {
        var unrelated = JavaParser.fromJavaVersion().build().parse("class Untouched { int value() { return 42; } }")
                .findFirst().orElseThrow().withSourcePath(Path.of("src/other/Untouched.java"));
        for (SourceFile file : apply(new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28"),List.of(unrelated)))
            assertNotEquals(unrelated.getSourcePath(),file.getSourcePath());
        Path bad = OUT.resolve("test-classes/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-lane28-bad");
        Files.createDirectories(bad);
        try (var files=Files.list(CRATE.resolve("src/main/resources/"+JAVA_RES))) {
            for(Path file:files.toList()) Files.writeString(bad.resolve(file.getFileName()),Files.readString(file)
                    +(file.toString().endsWith(".java.txt")?"// drift\n":""));
        }
        assertThrows(IllegalStateException.class,()->new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28-bad").getInitialValue(context()));
    }

    @Test void bootstrapInventoryAndModuleBoundary() throws Exception {
        Path classes=OUT.resolve("bootstrap"); Files.createDirectories(classes);
        String base=".m3/openrewrite-recipes/src/main/java/com/synexia/m3/bootstrap/";
        execute(List.of(JAVAC,"--release","21","-proc:none","-Xlint:all","-Werror","-cp",System.getProperty("java.class.path"),
                "-d",classes.toString(),ROOT.resolve(base+"M3BootstrapTaskRecipe.java").toString(),
                ROOT.resolve(base+"M3BootstrapInventoryRecipe.java").toString()),"compile-bootstrap.log",true);
        String oldFile=System.getProperty("m3.llm.taskCrateFile"),oldHash=System.getProperty("m3.llm.taskCrateRoot");
        Path manifest=CRATE.resolve("src/main/resources/"+JAVA_RES+"manifest.tsv");
        try(var loader=new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},Lane28Test.class.getClassLoader())) {
            System.setProperty("m3.llm.taskCrateFile",manifest.toString());
            System.setProperty("m3.llm.taskCrateRoot",hash(Files.readAllBytes(manifest)));
            var type=loader.loadClass("com.synexia.m3.bootstrap.M3BootstrapTaskRecipe");
            Recipe recipe=(Recipe)type.getConstructor().newInstance();
            assertEquals(false,type.getMethod("directTargetFileMutationAuthority").invoke(recipe));
            assertTrue(apply(recipe,javaAfter).isEmpty());
            System.setProperty("m3.llm.taskCrateRoot","0".repeat(64));
            assertThrows(IllegalStateException.class,recipe::getRecipeList);
        } finally {
            if(oldFile==null)System.clearProperty("m3.llm.taskCrateFile");else System.setProperty("m3.llm.taskCrateFile",oldFile);
            if(oldHash==null)System.clearProperty("m3.llm.taskCrateRoot");else System.setProperty("m3.llm.taskCrateRoot",oldHash);
        }
        execute(List.of(Path.of(System.getProperty("java.home"),"bin/jdeps").toString(),"-s",PATCH.toString()),"jdeps.log",true);
        assertEquals("patch -> java.base",Files.readString(OUT.resolve("jdeps.log")).strip());
    }

    @TestFactory Stream<DynamicTest> runtimeModes() {
        return Stream.of("interpreter","mixed","c1","c2").map(mode->DynamicTest.dynamicTest(mode,()->{
            List<String> command=runtime(PATCH,OUT,"M3Lane28Test");
            if(mode.equals("interpreter"))command.add(1,"-Xint");
            if(mode.equals("c1")||mode.equals("c2")) {
                command.addAll(1,List.of("-Xbatch","-XX:CompileThreshold=100","-XX:+UnlockDiagnosticVMOptions","-XX:+LogCompilation",
                        "-XX:LogFile="+OUT.resolve(mode+"-compilation.xml")));
                command.add(1,mode.equals("c1")?"-XX:TieredStopAtLevel=1":"-XX:-TieredCompilation");
            }
            execute(command,mode+".log",true);
            assertTrue(Files.readString(OUT.resolve(mode+".log")).contains("maintainedCounts=true"));
            if(mode.equals("c1")||mode.equals("c2")) {
                String log=Files.readString(OUT.resolve(mode+"-compilation.xml"));
                assertTrue(log.lines().anyMatch(l->l.contains("<nmethod")&&l.contains("compiler='"+mode+"'")
                        &&l.contains("method='jdk.internal.mindex.M3BitLane28 ")),"kernel compiled by "+mode);
            }
        }));
    }

    @Test void descriptiveCostProbe() throws Exception {
        var command=runtime(PATCH,OUT,"M3Lane28Cost");
        command.remove("-Xcheck:jni"); // Checker remains required in every correctness run.
        command.addAll(1,List.of("-Xbatch","-XX:-TieredCompilation","-XX:CompileThreshold=100"));
        execute(command,"cost.log",true);
        execute(command,"cost-repeat.log",true);
        assertTrue(Files.readString(OUT.resolve("cost.log")).contains("COST pages=32"));
        assertTrue(Files.readString(OUT.resolve("cost-repeat.log")).contains("COST pages=32"));
    }

    @Test void rejectsNativeMaskMutant() throws Exception {
        Path directory=OUT.resolve("native-mutant");Files.createDirectories(directory);
        compileNative(directory,true,"native-mutant-compile.log");
        execute(runtime(PATCH,directory,"M3Lane28Test"),"native-mutant.log",false);
        assertTrue(Files.readString(OUT.resolve("native-mutant.log")).contains("AssertionError: native parity"));
    }

    @Test void rejectsStaleRankMutant() throws Exception {
        Path source=OUT.resolve("rank-mutant/M3BitLane28.java");Files.createDirectories(source.getParent());
        String original=Files.readString(GENERATED.resolve(KERNEL));
        String update="regionCounts[slot >>> 20] += delta;";
        assertTrue(original.contains(update));
        Files.writeString(source,original.replace(update,"// Deliberate stale-region-count mutant"));
        Path patch=OUT.resolve("rank-mutant-patch");
        compileProduct(patch,source,"rank-mutant-compile.log");
        execute(runtime(patch,OUT,"M3Lane28Test"),"rank-mutant.log",false);
        assertTrue(Files.readString(OUT.resolve("rank-mutant.log")).contains("AssertionError:"));
        assertTrue(Files.readString(OUT.resolve("rank-mutant.log")).contains("rank")
                ||Files.readString(OUT.resolve("rank-mutant.log")).contains("region count"));
    }

    private static void compileProduct(Path output,Path kernel,String log) throws Exception {
        Files.createDirectories(output); Files.createDirectories(OUT.resolve("headers"));
        Path loader=OUT.resolve("loader/jdk/internal/mindex/Lane28Loader.java");
        Files.createDirectories(loader.getParent());
        Files.writeString(loader,"package jdk.internal.mindex; public final class Lane28Loader { "
                +"private Lane28Loader() {} public static void load(String path) { System.load(path); } }");
        var command=new ArrayList<>(List.of(JAVAC,"-source","21","-target","21","-proc:none","-implicit:none","-Xlint:all","-Werror",
                "--patch-module","java.base="+GENERATED.resolve(PRODUCT)+java.io.File.pathSeparator+loader.getParent(),
                "-h",OUT.resolve("headers").toString(),"-d",output.toString(),kernel.toString(),loader.toString()));
        for(String name:List.of("M3Address28","M3IntLane28","M3LongLane28","M3Bits"))
            command.add(GENERATED.resolve(PRODUCT+"jdk/internal/mindex/"+name+".java").toString());
        execute(command,log,true);
    }

    private static void compileNative(Path directory,boolean mutant,String log) throws Exception {
        Path javaHome=Path.of(System.getProperty("java.home"));
        Path nativeSource=GENERATED.resolve("src/java.base/share/native/libjava/M3BitLane28.c");
        if(mutant) {
            String body=Files.readString(nativeSource);
            String mask="if (w == first) word &= UINT64_MAX << (from & 63);";
            assertTrue(body.contains(mask));
            nativeSource=directory.resolve("M3BitLane28.c");
            Files.writeString(nativeSource,body.replace(mask,"/* Deliberate missing left mask mutant. */"));
        }
        var command=new ArrayList<>(List.of("gcc","-std=c11","-O2","-Wall","-Wextra","-Werror","-Wconversion","-Wshadow","-Wpedantic",
                "-fPIC","-shared","-I"+javaHome.resolve("include"),"-I"+javaHome.resolve("include/linux"),
                "-I"+OUT.resolve("headers")));
        if(Boolean.getBoolean("lane28.sanitize"))command.addAll(List.of("-fsanitize=undefined","-fno-sanitize-recover=all"));
        command.addAll(List.of(nativeSource.toString(),"-o",directory.resolve("libM3Lane28.so").toString()));
        execute(command,log,true);
    }

    private static List<String> runtime(Path patch,Path nativePath,String main) {
        return new ArrayList<>(List.of(JAVA,"-Xmx256m","-Xcheck:jni","--patch-module","java.base="+patch,
                "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED",
                "--add-opens","java.base/jdk.internal.mindex=ALL-UNNAMED",
                "-Dlane28.native="+nativePath.resolve("libM3Lane28.so"),"-cp",PROBES.toString(),main));
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void execute(List<String> command,String log,boolean success) throws Exception {
        Path output=OUT.resolve(log);
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("timeout: "+log);}
        if(success) assertEquals(0,process.exitValue(),()->{try{return Files.readString(output);}catch(Exception e){return e.toString();}});
        else assertNotEquals(0,process.exitValue(),"mutant must fail");
    }
}
