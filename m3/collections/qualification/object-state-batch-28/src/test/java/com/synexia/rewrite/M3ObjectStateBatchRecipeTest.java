/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Executes the existing hash-pinned Maven/OpenRewrite engine, then the real Java oracle. */
final class M3ObjectStateBatchRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/object-state-batch-28/";
    private record Output(String path, String sha, String text) { }
    private static String read(String path) throws Exception {
        try(var in=M3ObjectStateBatchRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in,path);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static List<Output> targets() throws Exception {
        List<Output> out=new ArrayList<>();
        for(String row:read(ROOT+"manifest.tsv").lines().toList()) {
            if(row.isBlank()||row.startsWith("#"))continue;
            String[] p=row.split("\t");assertEquals(4,p.length);assertEquals("ABSENT",p[1]);
            String text=read(ROOT+p[3]);assertEquals(p[2],hash(text.getBytes(StandardCharsets.UTF_8)));
            out.add(new Output(p[0],p[2],text));
        }
        assertEquals(2,out.size());return out;
    }
    private static Recipe named() throws Exception {
        String path="/META-INF/rewrite/m3-object-state-batch-28.yml";
        try(var in=M3ObjectStateBatchRecipeTest.class.getResourceAsStream(path)) {
            return Environment.builder().load(new YamlResourceLoader(in,java.net.URI.create("classpath:"+path),
                    new Properties(),M3ObjectStateBatchRecipeTest.class.getClassLoader())).build()
                    .activateRecipes("com.synexia.rewrite.M3ObjectStateBatch28");
        }
    }
    private static Map<String,String> run(Map<String,String> installed) throws Exception {
        List<SourceFile> input=new ArrayList<>();
        installed.forEach((p,t)->input.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));
        List<Throwable> errors=new ArrayList<>();
        var changed=named().run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        Map<String,String> outputs=new TreeMap<>();
        for(Result value:changed) {
            assertInstanceOf(PlainText.class,value.getAfter());
            assertNull(outputs.put(value.getAfter().getSourcePath().toString(),value.getAfter().printAll()));
        }
        return outputs;
    }
    @Test void fourPartialStatesAndFixedPoint() throws Exception {
        var targets=targets();
        for(int mask=0;mask<4;mask++) for(boolean reversed:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(int i=0;i<2;i++) if((mask&(1<<i))!=0)input.put(targets.get(i).path,targets.get(i).text);
            if(reversed) { var keys=new ArrayList<>(input.keySet());Collections.reverse(keys);
                var reordered=new LinkedHashMap<String,String>();for(String p:keys)reordered.put(p,input.get(p));input=reordered; }
            input.put("unrelated.txt","unchanged\n");
            var actual=run(input);assertEquals(2-Integer.bitCount(mask),actual.size());input.putAll(actual);
            for(Output o:targets)assertEquals(o.sha,hash(input.get(o.path).getBytes(StandardCharsets.UTF_8)));
            assertTrue(run(input).isEmpty());
        }
    }
    @Test void driftDuplicateAndLateMutationRefusal() throws Exception {
        var core=new M3HashPinnedJavaSnapshotRecipe("object-state-batch-28",false);
        for(Output o:targets()) {
            var context=new InMemoryExecutionContext();var inv=core.getInitialValue(context);
            var good=PlainText.builder().sourcePath(Path.of(o.path)).text(o.text).build();
            core.getScanner(inv).visit(good.withText("drift\n"),context);
            assertThrows(IllegalStateException.class,()->core.generate(inv,context));
            var duplicate=core.getInitialValue(context);core.getScanner(duplicate).visit(good,context);
            core.getScanner(duplicate).visit(good,context);
            assertThrows(IllegalStateException.class,()->core.generate(duplicate,context));
            var ready=core.getInitialValue(context);core.getScanner(ready).visit(good,context);
            core.generate(ready,context);
            assertInstanceOf(IllegalStateException.class,assertThrows(
                    org.openrewrite.internal.RecipeRunException.class,
                    ()->core.getVisitor(ready).visit(good.withText(o.text+"drift\n"),context)).getCause());
        }
    }
    @Test void incumbentM3OwnersAreBytePinned() throws Exception {
        Path root=Path.of(System.getProperty("m3.receiver.source"));int count=0;
        for(String row:Files.readAllLines(Path.of("M3_OWNER_SHA256.tsv"))) {
            if(row.startsWith("#")||row.isBlank())continue;
            String[] p=row.split("\t");assertEquals(3,p.length);byte[] bytes=Files.readAllBytes(root.resolve(p[0]));
            assertEquals(p[1],hash(bytes));var digest=MessageDigest.getInstance("SHA-1");
            digest.update(("blob "+bytes.length+"\0").getBytes(StandardCharsets.UTF_8));digest.update(bytes);
            assertEquals(p[2],HexFormat.of().formatHex(digest.digest()));count++;
        }
        assertEquals(5,count);
    }
    @Test void actualSdkResultCompilesAndExecutes() throws Exception {
        var outputs=run(Map.of());assertEquals(2,outputs.size());var targets=targets();
        Path stage=Path.of("target/sdk-batch-28").toAbsolutePath();assertFalse(Files.exists(stage));
        Path classes=stage.resolve("classes");Files.createDirectories(classes);
        var files=new ArrayList<String>();
        for(Output o:targets) {
            Path path=stage.resolve(o.path);Files.createDirectories(path.getParent());Files.writeString(path,outputs.get(o.path));
            assertEquals(o.sha,hash(Files.readAllBytes(path)));files.add(path.toString());
        }
        Path owner=Path.of(System.getProperty("m3.receiver.source"));
        for(String name:List.of("M3Address28.java","M3BitLane28.java","M3IntLane28.java",
                "M3SlotAllocator28.java","M3ObjectStateSlots28.java"))files.add(owner.resolve(name).toString());
        var javac=new ArrayList<>(List.of("javac","--release","21","-proc:none","-Xlint:all","-Werror","-d",classes.toString()));
        javac.addAll(files);exec(stage.resolve("javac.log"),javac);
        String log=exec(stage.resolve("runtime.log"),List.of("java","-Xmx256m","-cp",classes.toString(),
                "com.m3.collections.M3ObjectStateBatch28SelfTest"));
        assertTrue(log.contains("checks=1720349"),log);
        assertTrue(log.contains("randomized_operations=49152"),log);
        assertTrue(run(outputs).isEmpty());
        StringBuilder receipt=new StringBuilder("# Actual Maven/OpenRewrite SDK generated outputs\n");
        for(Output o:targets)receipt.append(o.path).append('\t').append(o.sha).append('\n');
        Files.writeString(stage.resolve("QUALIFIED.tsv"),receipt);
    }
    private static String exec(Path log,List<String> command) throws Exception {
        var process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(100,TimeUnit.SECONDS)){process.destroyForcibly();fail("timeout "+command);}
        String actual=Files.readString(log);assertEquals(0,process.exitValue(),actual);return actual;
    }
}
