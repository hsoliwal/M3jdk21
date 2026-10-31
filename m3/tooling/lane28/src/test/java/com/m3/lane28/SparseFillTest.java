// SPDX-License-Identifier: Apache-2.0
package com.m3.lane28;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class SparseFillTest {
    private static final Path CRATE=Path.of(System.getProperty("m3.crate"));
    private static final Path OUT=CRATE.resolve("target/sparse");
    private static final String JAVA=Path.of(System.getProperty("java.home"),"bin/java").toString();
    private static final String JAVAC=Path.of(System.getProperty("java.home"),"bin/javac").toString();
    private static final String PRODUCT="src/java.base/share/classes/jdk/internal/mindex/";
    private static final String PROBE="test/jdk/jdk/internal/mindex/M3SparseFillTest.java";
    private static String probe,address;
    private static final Map<String,String> candidate=new TreeMap<>();
    private static void run(List<String> cmd,String log,boolean success) throws Exception {
        Process p=new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(OUT.resolve(log).toFile()).start();
        if(!p.waitFor(90,TimeUnit.SECONDS)){p.destroyForcibly();fail("timeout "+cmd);}
        if(success)assertEquals(0,p.exitValue(),Files.readString(OUT.resolve(log)));
        else {assertNotEquals(0,p.exitValue());assertTrue(Files.readString(OUT.resolve(log)).contains("AssertionError"));}
    }
    private static void compile(String state,Map<String,String> sources) throws Exception {
        Path dir=OUT.resolve(state),classes=dir.resolve("classes"),tests=dir.resolve("tests");Files.createDirectories(classes);Files.createDirectories(tests);
        var cmd=new ArrayList<>(List.of(JAVAC,"-source","21","-target","21","-proc:none","-Xlint:all","-Werror","--patch-module","java.base="+dir,"-d",classes.toString()));
        var all=new TreeMap<>(sources);all.put("M3Address28",address);
        for(var e:all.entrySet()){Path p=dir.resolve(e.getKey()+".java");Files.writeString(p,e.getValue());cmd.add(p.toString());}
        run(cmd,state+"-compile.log",true);
        Path p=dir.resolve("M3SparseFillTest.java");Files.writeString(p,probe);
        run(List.of(JAVAC,"-source","21","-target","21","-proc:none","-Xlint:all","-Werror","--patch-module","java.base="+classes,
            "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED","-d",tests.toString(),p.toString()),state+"-probe.log",true);
    }
    private static List<String> command(String state){return new ArrayList<>(List.of(JAVA,"-Xmx256m","-ea","-Xcheck:jni","--patch-module","java.base="+OUT.resolve(state+"/classes"),
        "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED","-cp",OUT.resolve(state+"/tests").toString(),"M3SparseFillTest"));}
    @BeforeAll static void prepare() throws Exception {
        Files.createDirectories(OUT);
        var ctx=new InMemoryExecutionContext(e->{throw new IllegalStateException(e);});
        var files=new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-lane28").run(new InMemoryLargeSourceSet(List.of()),ctx,1).getChangeset().getAllResults();
        for(var result:files){var f=result.getAfter();String p=f.getSourcePath().toString();
            if(p.equals(PROBE))probe=f.printAll();
            if(p.equals(PRODUCT+"M3Address28.java"))address=f.printAll();
            for(String kind:List.of("Int","Long"))if(p.equals(PRODUCT+"M3"+kind+"Lane28.java"))candidate.put("M3"+kind+"Lane28",f.printAll());}
        assertNotNull(probe);assertNotNull(address);assertEquals(2,candidate.size());
        var before=new TreeMap<String,String>();
        for(String kind:List.of("Int","Long"))before.put("M3"+kind+"Lane28",Files.readString(CRATE.resolve("history/9af35b3/M3"+kind+"Lane28.java.txt")));
        compile("before",before);compile("after",candidate);
    }
    @TestFactory Stream<DynamicTest> runtimeModes(){return Stream.of("-Xint","-XX:TieredStopAtLevel=1","-XX:-TieredCompilation","").map(mode->DynamicTest.dynamicTest(mode.isEmpty()?"mixed":mode,()->{
        String tag=mode.isEmpty()?"mixed":mode.replaceAll("[^a-zA-Z0-9]","");
        for(String state:List.of("before","after")){
            var cmd=command(state);if(!mode.isEmpty())cmd.add(1,mode);
            if(mode.contains("Tiered"))cmd.addAll(1,List.of("-Xbatch","-XX:CompileThreshold=100","-XX:+UnlockDiagnosticVMOptions","-XX:+LogCompilation",
                "-XX:LogFile="+OUT.resolve(state+tag+"-compilation.xml"),
                "-XX:CompileCommand=dontinline,jdk.internal.mindex.M3IntLane28::fill",
                "-XX:CompileCommand=dontinline,jdk.internal.mindex.M3LongLane28::fill"));
            run(cmd,state+tag+".log",true);
            if(mode.contains("Tiered")) {
                String compiler=mode.contains("StopAtLevel")?"c1":"c2";
                String log=Files.readString(OUT.resolve(state+tag+"-compilation.xml"));
                for(String kind:List.of("Int","Long"))assertTrue(log.lines().anyMatch(l->l.contains("<nmethod")&&l.contains("compiler='"+compiler+"'")
                    &&l.contains("method='jdk.internal.mindex.M3"+kind+"Lane28 fill ")),state+kind+" fill compiled by "+compiler);
            }
        }
        assertEquals(Files.readString(OUT.resolve("before"+tag+".log")),Files.readString(OUT.resolve("after"+tag+".log")));
    }));}
    @Test void exactContributionAndTargetNames() throws Exception {
        for(String kind:List.of("Int","Long")) {
            String donor=Files.readString(CRATE.resolve("donor/szf/Segmented"+kind+"Lane28.java.txt"));
            donor=donor.substring(donor.indexOf("package "))
                    .replace("package com.synexia.common.collections;","package jdk.internal.mindex;")
                    .replace("Segmented"+kind+"Lane28","M3"+kind+"Lane28")
                    .replace("SegmentedAddress28","M3Address28");
            String target=candidate.get("M3"+kind+"Lane28");
            assertEquals(donor,target.substring(target.indexOf("package ")));
        }
    }
    @Test void publicProtectedDescriptors() throws Exception {
        String javap=Path.of(System.getProperty("java.home"),"bin/javap").toString();
        for(String kind:List.of("Int","Long")){
            for(String state:List.of("before","after"))run(List.of(javap,"-protected","-s","-classpath",OUT.resolve(state+"/classes").toString(),"jdk.internal.mindex.M3"+kind+"Lane28"),state+kind+"-api.log",true);
            assertEquals(Files.readString(OUT.resolve("before"+kind+"-api.log")),Files.readString(OUT.resolve("after"+kind+"-api.log")));
        }
    }
    @Test void rejectsBoundsShortcutAndRegionSkipMutants() throws Exception {
        for(String mutant:List.of("bounds","skip")) {
            var sources=new TreeMap<>(candidate);
            for(var e:candidate.entrySet()) {
                String text=e.getValue();
                if(mutant.equals("bounds")) {
                    int from=text.indexOf("public void fill(");
                    text=text.substring(0,from)+text.substring(from).replaceFirst("M3Address28.checkRange\\(from, to\\);","if (value == 0 && allocatedBlocks == 0) return; M3Address28.checkRange(from, to);");
                } else {assertTrue(text.contains("from = regionEnd;"));text=text.replace("from = regionEnd;","from = regionEnd + 1;");}
                sources.put(e.getKey(),text);
            }
            compile(mutant,sources);run(command(mutant),mutant+"-refusal.log",false);
        }
    }
}
