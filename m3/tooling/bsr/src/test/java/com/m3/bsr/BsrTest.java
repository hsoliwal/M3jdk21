// SPDX-License-Identifier: Apache-2.0
package com.m3.bsr;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class BsrTest {
    private static final Path CRATE=Path.of(System.getProperty("m3.crate"));
    private static final Path ROOT=Path.of(System.getProperty("m3.root")).normalize();
    private static final Path RES=CRATE.resolve("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-bsr");
    private static final Path OUT=CRATE.resolve("target");
    private static final Path JDK=Path.of(System.getProperty("java.home"));
    private static final String OWNER="src/java.base/share/classes/jdk/internal/mindex/";
    private static List<SourceFile> before,after;
    private static final String PROBE="test/jdk/jdk/internal/mindex/M3BitScanTest.java";
    private static SourceFile kernel(List<SourceFile> files) {
        return files.stream().filter(f->f.getSourcePath().toString().equals(OWNER+"M3BitLane28.java")).findFirst().orElseThrow();
    }
    private static String probe() {
        return after.stream().filter(f->f.getSourcePath().toString().equals(PROBE)).findFirst().orElseThrow().printAll();
    }
    private static InMemoryExecutionContext context(){return new InMemoryExecutionContext(e->{throw new IllegalStateException(e);});}
    private static List<SourceFile> apply(List<SourceFile> files){return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-bsr")
        .run(new InMemoryLargeSourceSet(files),context(),1).getChangeset().getAllResults().stream().map(r->r.getAfter()).toList();}
    private static List<SourceFile> parse() throws Exception {
        var files=new ArrayList<SourceFile>();
        for(String line:Files.readAllLines(RES.resolve("manifest.tsv"))) {
            if(line.isBlank()||line.startsWith("#"))continue;String[] row=line.split("\t");
            if(row[1].equals("ABSENT"))continue;
            files.add(PlainText.builder().sourcePath(Path.of(row[0])).text(Files.readString(RES.resolve(row[3]+".before"))).build());
        }
        return files;
    }
    private static void run(List<String> cmd,String log,boolean success) throws Exception {
        Process p=new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(OUT.resolve(log).toFile()).start();
        if(!p.waitFor(90,TimeUnit.SECONDS)){p.destroyForcibly();fail("timeout "+cmd);}
        if(success)assertEquals(0,p.exitValue(),Files.readString(OUT.resolve(log)));
        else {assertNotEquals(0,p.exitValue());assertTrue(Files.readString(OUT.resolve(log)).contains("AssertionError"));}
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static void compile(String state,String source) throws Exception {
        Path dir=OUT.resolve(state),classes=dir.resolve("classes"),test=dir.resolve("tests");
        Path isolated=dir.resolve("sources"),owner=isolated.resolve("jdk/internal/mindex");
        Files.createDirectories(owner);Files.createDirectories(classes);Files.createDirectories(test);Files.createDirectories(OUT.resolve("headers"));
        Files.writeString(owner.resolve("M3BitLane28.java"),source);
        for(String name:List.of("M3Address28","M3Bits"))Files.copy(ROOT.resolve(OWNER+name+".java"),owner.resolve(name+".java"),StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(owner.resolve("Lane28Loader.java"),"package jdk.internal.mindex; public final class Lane28Loader { private Lane28Loader() {} public static void load(String path) { System.load(path); } }");
        var cmd=new ArrayList<>(List.of(JDK.resolve("bin/javac").toString(),"-source","21","-target","21","-proc:none","-implicit:none","-Xlint:all","-Werror",
                "--patch-module","java.base="+isolated,"-h",OUT.resolve("headers").toString(),"-d",classes.toString()));
        for(String name:List.of("M3BitLane28","M3Address28","M3Bits","Lane28Loader"))cmd.add(owner.resolve(name+".java").toString());
        run(cmd,state+"-compile.log",true);
        Path p=dir.resolve("M3BitScanTest.java");Files.writeString(p,probe());
        run(List.of(JDK.resolve("bin/javac").toString(),"-source","21","-target","21","-Xlint:all","-Werror","--patch-module","java.base="+classes,
                "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED","-d",test.toString(),p.toString()),state+"-probe-compile.log",true);
    }
    @BeforeAll static void prepare() throws Exception {
        Files.createDirectories(OUT);
        for(String line:Files.readAllLines(CRATE.resolve("inputs.tsv"))) {
            if(line.startsWith("#")||line.isBlank())continue;String[] row=line.split("\t");
            assertEquals(row[1],hash(Files.readAllBytes(ROOT.resolve(row[0]))),row[0]);
        }
        before=parse();after=apply(before);assertEquals(17,after.size());
        compile("before",kernel(before).printAll());compile("after",kernel(after).printAll());
        for(SourceFile file:after) {
            Path generated=OUT.resolve("generated").resolve(file.getSourcePath());
            Files.createDirectories(generated.getParent());Files.writeString(generated,file.printAll());
        }
        var command=new ArrayList<>(List.of("gcc","-std=c11","-O2","-Wall","-Wextra","-Werror","-Wconversion","-Wshadow","-Wpedantic",
                "-fPIC","-shared","-I"+JDK.resolve("include"),"-I"+JDK.resolve("include/linux"),"-include",
                OUT.resolve("headers/jdk_internal_mindex_M3BitLane28.h").toString(),"-I"+OUT.resolve("headers")));
        if(Boolean.getBoolean("bsr.sanitize"))command.addAll(List.of("-fsanitize=undefined","-fno-sanitize-recover=all"));
        command.addAll(List.of(ROOT.resolve("src/java.base/share/native/libjava/M3BitLane28.c").toString(),"-o",OUT.resolve("libbsr.so").toString()));
        run(command,"native-compile.log",true);
    }
    @Test void exactAndFixedPoint() throws Exception {
        assertTrue(apply(after).isEmpty());
        for(String line:Files.readAllLines(RES.resolve("manifest.tsv"))) {
            if(line.startsWith("#")||line.isBlank())continue;String[] row=line.split("\t");
            assertEquals(row[2],hash(Files.readAllBytes(OUT.resolve("generated").resolve(row[0]))));
        }
    }
    @Test void refusesDriftMissingAndDuplicate() throws Exception {
        var missing=parse();missing.removeFirst();assertThrows(RuntimeException.class,()->apply(missing));
        var duplicate=parse();duplicate.add(duplicate.getFirst());assertThrows(RuntimeException.class,()->apply(duplicate));
        var drift=parse();SourceFile f=drift.getFirst();drift.set(0,PlainText.builder().sourcePath(f.getSourcePath()).text(f.printAll()+"drift").build());
        assertThrows(RuntimeException.class,()->apply(drift));
    }
    @Test void retainsUnrelatedAndRejectsTemplateTampering() throws Exception {
        var files=parse();var unrelated=PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("retained").build();
        files.add(unrelated);var changes=apply(files);assertEquals(17,changes.size());
        assertTrue(changes.stream().noneMatch(f->f.getSourcePath().equals(unrelated.getSourcePath())));
        Path bad=OUT.resolve("test-classes/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-bsr-bad");Files.createDirectories(bad);
        try(var list=Files.list(RES)) {for(Path p:list.toList())Files.writeString(bad.resolve(p.getFileName()),Files.readString(p)+(p.getFileName().toString().equals("manifest.tsv")?"":"drift"));}
        assertThrows(IllegalStateException.class,()->new M3Jdk21HashPinnedTextSnapshotRecipe("m3-bsr-bad").getInitialValue(context()));
    }
    @Test void apiDescriptors() throws Exception {
        for(String state:List.of("before","after"))run(List.of(JDK.resolve("bin/javap").toString(),"-protected","-s","-classpath",OUT.resolve(state+"/classes").toString(),"jdk.internal.mindex.M3BitLane28"),state+"-api.log",true);
        assertEquals(Files.readString(OUT.resolve("before-api.log")),Files.readString(OUT.resolve("after-api.log")));
    }
    private static List<String> command(String state) {
        return new ArrayList<>(List.of(JDK.resolve("bin/java").toString(),"-Xmx256m","-ea","-Xcheck:jni",
                "--patch-module","java.base="+OUT.resolve(state+"/classes"),
                "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED","--add-opens","java.base/jdk.internal.mindex=ALL-UNNAMED",
                "-Dbsr.native="+OUT.resolve("libbsr.so"),"-cp",OUT.resolve(state+"/tests").toString(),"M3BitScanTest"));
    }
    @TestFactory Stream<DynamicTest> runtimeModes(){return Stream.of("interpreter","mixed","c1","c2").map(mode->DynamicTest.dynamicTest(mode,()->{
        for(String state:List.of("before","after")){
            var cmd=command(state);if(mode.equals("interpreter"))cmd.add(1,"-Xint");
            if(mode.equals("c1")||mode.equals("c2")) {
                cmd.addAll(1,List.of("-Xbatch","-XX:CompileThreshold=100","-XX:+UnlockDiagnosticVMOptions","-XX:+LogCompilation",
                        "-XX:LogFile="+OUT.resolve(state+"-"+mode+"-compilation.xml"),
                        "-XX:CompileCommand=dontinline,jdk.internal.mindex.M3BitLane28::nextSetBit",
                        "-XX:CompileCommand=dontinline,jdk.internal.mindex.M3BitLane28::previousSetBit",
                        mode.equals("c1")?"-XX:TieredStopAtLevel=1":"-XX:-TieredCompilation"));
            }
            run(cmd,state+"-"+mode+".log",true);
            if(mode.equals("c1")||mode.equals("c2"))for(String method:List.of("nextSetBit","previousSetBit")){
                String log=Files.readString(OUT.resolve(state+"-"+mode+"-compilation.xml"));
                assertTrue(log.lines().anyMatch(l->l.contains("<nmethod")&&l.contains("compiler='"+mode+"'")&&l.contains("M3BitLane28 "+method+" ")),state+mode+method);
            }
        }
        assertEquals(Files.readString(OUT.resolve("before-"+mode+".log")),Files.readString(OUT.resolve("after-"+mode+".log")));
    }));}
    @Test void rejectsInvalidSummaryAndBoundsMutants() throws Exception {
        String source=kernel(after).printAll();
        Map<String,String> mutants=Map.of(
            "region",source.replace("regionCounts[from >>> 20] == 0","regionCounts[from >>> 20] != 0"),
            "page",source.replace("pageCounts[from >>> 20][(from >>> 12) & 255] != 0","pageCounts[from >>> 20][(from >>> 12) & 255] == 0"),
            "bounds",source.replace("M3Address28.checkRange(from, M3Address28.MAX_SLOTS);","if (cardinality == 0) return -1;\n        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);"));
        for(var entry:mutants.entrySet()){compile(entry.getKey(),entry.getValue());run(command(entry.getKey()),entry.getKey()+"-refusal.log",false);}
    }
    @Test void measuredBitmapReads() throws Exception {
        for(String state:List.of("before","after")) {
            String source=kernel(state.equals("before")?before:after).printAll();
            source=source.replace("public final class M3BitLane28 {","public final class M3BitLane28 { public static long visits, wordReads;");
            int start=source.indexOf("    public int nextSetBit("),end=source.indexOf("    /** Counts set bits",start);
            String methods=source.substring(start,end).replace("while (from < M3Address28.MAX_SLOTS) {","while (from < M3Address28.MAX_SLOTS) { visits++;")
                .replace("while (from >= 0) {","while (from >= 0) { visits++;")
                .replace("long word = page[wordIndex]", "wordReads++; long word = page[wordIndex]")
                .replace("word = page[wordIndex];","wordReads++; word = page[wordIndex];");
            compile("work-"+state,source.substring(0,start)+methods+source.substring(end));
            var cmd=command("work-"+state);cmd.add("work");run(cmd,"work-"+state+".log",true);
        }
        List<String> first=Files.readAllLines(OUT.resolve("work-before.log")),last=Files.readAllLines(OUT.resolve("work-after.log"));
        assertEquals("empty visits=0 wordReads=0",last.get(0));
        assertEquals(first.get(1),last.get(1),"cold traversal work unchanged; no eager preparation");
        assertTrue(last.get(2).endsWith("wordReads=256"),last.toString());
        long oldReads=Long.parseLong(first.get(2).split("wordReads=")[1]);assertTrue(oldReads>256);
    }
    @Test void qualifiedSourceAtoms() throws Exception {
        assertEquals("1e5e926a4f039e393e63a70cc111a922ed0a81bad2460f578173859d924ade06",
                hash(Files.readAllBytes(CRATE.resolve("donor/SegmentedBitLane28.java.txt"))));
        String donor=Files.readString(CRATE.resolve("donor/SegmentedBitLane28.java.txt"))
                .replace("SegmentedBitLane28","M3BitLane28").replace("SegmentedAddress28","M3Address28").replace("PackedBitAtoms","M3Bits");
        String product=kernel(after).printAll();
        int da=donor.indexOf("    public int nextSetBit("),db=donor.indexOf("    /** Counts set bits",da);
        int pa=product.indexOf("    public int nextSetBit("),pb=product.indexOf("    /** Counts set bits",pa);
        assertEquals(donor.substring(da,db),product.substring(pa,pb));
    }

}
