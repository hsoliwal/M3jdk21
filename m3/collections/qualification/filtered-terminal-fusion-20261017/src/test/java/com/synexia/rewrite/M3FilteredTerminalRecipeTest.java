/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Serial per-file proof of actual OpenRewrite SourceFile output and public/callback equivalence. */
final class M3FilteredTerminalRecipeTest {
    private static final String RESOURCE =
            "/com/synexia/rewrite/hash-pinned-java/filtered-terminal-fusion/";
    private static final String SOURCE =
            "m3/collections/src/main/java/com/m3/collections/M3LongFilteredView.java";
    private static final String PROOF =
            "m3/collections/src/test/java/com/m3/collections/M3FilteredTerminalProof.java";
    private record Target(String path,String before,String after,String template) { }
    private static String read(String path) throws Exception {
        try(var in=M3FilteredTerminalRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in,path);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static List<Target> targets() throws Exception {
        List<Target> result=new ArrayList<>();
        for(String line:read(RESOURCE+"manifest.tsv").lines().toList()) {
            if(line.isBlank()||line.startsWith("#"))continue;
            String[] f=line.split("\t",-1);assertEquals(4,f.length);
            assertEquals(f[2],hash(read(RESOURCE+f[3]).getBytes(StandardCharsets.UTF_8)));
            result.add(new Target(f[0],f[1],f[2],f[3]));
        }
        assertEquals(List.of(SOURCE,PROOF),result.stream().map(Target::path).toList());
        assertEquals("ABSENT",result.get(1).before);
        assertEquals(result.get(0).before,hash(read("/preimage/M3LongFilteredView.java.txt").getBytes(StandardCharsets.UTF_8)));
        return result;
    }
    private static Recipe named() throws Exception {
        String path="/META-INF/rewrite/m3-filtered-terminal-fusion-20261017.yml";
        try(var in=M3FilteredTerminalRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in);
            return Environment.builder().load(new YamlResourceLoader(in,java.net.URI.create("classpath:"+path),
                    new Properties(),M3FilteredTerminalRecipeTest.class.getClassLoader())).build()
                    .activateRecipes("com.synexia.m3.FilteredTerminalFusion20261017");
        }
    }
    private static Map<String,String> run(Map<String,String> installed) throws Exception {
        List<SourceFile> files=new ArrayList<>();
        installed.forEach((p,t)->files.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));
        List<Throwable> errors=new ArrayList<>();
        List<Result> changes=named().run(new InMemoryLargeSourceSet(files),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->"OpenRewrite SDK errors "+errors);
        Map<String,String> after=new TreeMap<>();
        for(Result result:changes) {
            assertInstanceOf(PlainText.class,result.getAfter());
            assertNull(after.put(result.getAfter().getSourcePath().toString(),result.getAfter().printAll()));
        }
        return after;
    }
    private static String baseline() throws Exception {return read("/preimage/M3LongFilteredView.java.txt");}
    @Test void fourSerialPartialStatesTwoOrdersAndIdempotence() throws Exception {
        List<Target> t=targets();
        for(int mask=0;mask<4;mask++)for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            input.put(SOURCE,(mask&1)!=0?read(RESOURCE+t.get(0).template):baseline());
            if((mask&2)!=0)input.put(PROOF,read(RESOURCE+t.get(1).template));
            if(reverse) {
                var keys=new ArrayList<>(input.keySet());Collections.reverse(keys);
                var reversed=new LinkedHashMap<String,String>();for(String k:keys)reversed.put(k,input.get(k));
                input=reversed;
            }
            input.put("unrelated.txt","unchanged\n");
            var changed=run(input);assertEquals(2-Integer.bitCount(mask),changed.size());
            input.putAll(changed);assertTrue(run(input).isEmpty());
            for(Target target:t)assertEquals(target.after,hash(input.get(target.path).getBytes(StandardCharsets.UTF_8)));
            assertEquals("unchanged\n",input.get("unrelated.txt"));
        }
    }
    @Test void staleMissingDuplicateAndLateEditsFailClosed() throws Exception {
        var core=new M3HashPinnedJavaSnapshotRecipe("filtered-terminal-fusion",false);
        var ctx=new InMemoryExecutionContext();
        var missing=core.getInitialValue(ctx);
        assertThrows(IllegalStateException.class,()->core.generate(missing,ctx));
        var drift=core.getInitialValue(ctx);
        core.getScanner(drift).visit(PlainText.builder().sourcePath(Path.of(SOURCE)).text("drift").build(),ctx);
        assertThrows(IllegalStateException.class,()->core.generate(drift,ctx));
        var duplicate=core.getInitialValue(ctx);
        var old=PlainText.builder().sourcePath(Path.of(SOURCE)).text(baseline()).build();
        core.getScanner(duplicate).visit(old,ctx);core.getScanner(duplicate).visit(old,ctx);
        assertThrows(IllegalStateException.class,()->core.generate(duplicate,ctx));
        var late=core.getInitialValue(ctx);core.getScanner(late).visit(old,ctx);core.generate(late,ctx);
        var error=assertThrows(org.openrewrite.internal.RecipeRunException.class,
                ()->core.getVisitor(late).visit(old.withText("drift after scan"),ctx));
        assertInstanceOf(IllegalStateException.class,error.getCause());
    }
    @Test void upstreamOwnerSourcesRemainIdentical() throws Exception {
        Path repo=Path.of(System.getProperty("m3.receiver.source"));int count=0;
        for(String line:Files.readAllLines(Path.of("M3_OWNER_SHA256.tsv"))) {
            if(line.startsWith("#")||line.isBlank())continue;
            String[] parts=line.split("\t",-1);assertEquals(3,parts.length);
            byte[] bytes=Files.readAllBytes(repo.resolve(parts[0]));
            assertEquals(parts[1],hash(bytes));
            MessageDigest sha1=MessageDigest.getInstance("SHA-1");
            sha1.update(("blob "+bytes.length+"\0").getBytes(StandardCharsets.UTF_8));sha1.update(bytes);
            assertEquals(parts[2],HexFormat.of().formatHex(sha1.digest()));count++;
        }
        assertEquals(3,count);
    }
    @Test void actualSdkResultCompilesAndMatchesBaselineTraceAndPublicApi() throws Exception {
        var changed=run(Map.of(SOURCE,baseline()));assertEquals(2,changed.size());
        var t=targets();Path stage=Path.of("target/sdk-filtered-terminal-fusion").toAbsolutePath();
        assertFalse(Files.exists(stage),"run in clean target folder");Files.createDirectories(stage);
        Path core=Path.of(System.getProperty("m3.receiver.source"));
        for(Target file:t) {
            Path out=stage.resolve(file.path);Files.createDirectories(out.getParent());
            Files.writeString(out,changed.get(file.path));
            assertEquals(file.after,hash(Files.readAllBytes(out)));
        }
        String baselineOut=compileAndRun(stage.resolve("baseline"),core,baseline(),changed.get(PROOF));
        String candidateOut=compileAndRun(stage.resolve("candidate"),core,changed.get(SOURCE),changed.get(PROOF));
        assertEquals(baselineOut,candidateOut,"callback/exception or public descriptor drift");
        assertTrue(candidateOut.contains("assertions=642"),candidateOut);
        assertTrue(candidateOut.contains("a482d01252a7a235c73734ad9319b0b9c43c34b3bbb603437b23498e6322c008"));
        assertTrue(run(changed).isEmpty());
        StringBuilder receipt=new StringBuilder("# Qualified actual Maven/OpenRewrite SourceFile results\n");
        for(Target file:t)receipt.append(file.path).append('\t').append(file.after).append('\n');
        Files.writeString(stage.resolve("QUALIFIED.tsv"),receipt);
    }
    private static String compileAndRun(Path dir,Path owner,String cls,String proof) throws Exception {
        Path source=dir.resolve("src/com/m3/collections/M3LongFilteredView.java");
        Path test=dir.resolve("src/com/m3/collections/M3FilteredTerminalProof.java");
        Files.createDirectories(source.getParent());Files.writeString(source,cls);Files.writeString(test,proof);
        Path output=dir.resolve("classes");Files.createDirectories(output);
        runProcess(dir.resolve("javac.log"),List.of("javac","--release","21","-proc:none",
                "-Xlint:all","-Werror","-d",output.toString(),
                owner.resolve("m3/collections/src/main/java/com/m3/collections/M3LongCollection.java").toString(),
                owner.resolve("m3/collections/src/main/java/com/m3/collections/M3LongIterator.java").toString(),
                owner.resolve("m3/collections/src/main/java/com/m3/collections/M3LongPredicates.java").toString(),
                source.toString(),test.toString()));
        String trace=runProcess(dir.resolve("runtime.log"),List.of("java","-Xmx256m","-cp",output.toString(),
                "com.m3.collections.M3FilteredTerminalProof"));
        String descriptors=runProcess(dir.resolve("javap.log"),List.of("javap","-public","-s","-cp",output.toString(),
                "com.m3.collections.M3LongFilteredView"));
        return trace+descriptors;
    }
    private static String runProcess(Path log,List<String> command) throws Exception {
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();fail("timeout "+command);}
        String text=Files.readString(log);assertEquals(0,process.exitValue(),text);return text;
    }
}
