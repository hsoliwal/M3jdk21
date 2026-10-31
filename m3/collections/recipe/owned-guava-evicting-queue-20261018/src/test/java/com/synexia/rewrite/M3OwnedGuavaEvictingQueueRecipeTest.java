/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** One exact absent-before production source; queue runtime behavior is qualified separately. */
final class M3OwnedGuavaEvictingQueueRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/owned-guava-evicting-queue/";
    private static final String YAML="/META-INF/rewrite/m3-owned-guava-evicting-queue.yml";
    private static final String NAME="com.synexia.m3.OwnedGuavaEvictingQueue20261018";
    private static final String TARGET="m3/collections/implementations/guava-evicting-queue/src/main/java/com/m3/collections/guava/M3EvictingQueue.java";
    private static final String AFTER_SHA="8afe356324bdaa51953933f2213b573175b64e2921df4c0df7c4a16161274840";
    private static final int AFTER_BYTES=16979;
    private static final String TEMPLATE="M3EvictingQueue.java.txt";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";

    private record Execution(Recipe recipe,List<Result> changes,Observation observation) { }
    private static final class Observation {
        private int edited;
        private int generated;
    }
    /** Observes this supplied installer batch, not arbitrary composed-recipe transactionality. */
    private static final class ObservedSources extends InMemoryLargeSourceSet {
        private final Observation observation;
        private ObservedSources(List<SourceFile> files,Observation observation) {
            super(files);
            this.observation=observation;
        }
        @Override public LargeSourceSet edit(UnaryOperator<SourceFile> map) {
            return super.edit(before -> {
                SourceFile after=map.apply(before);
                if(after!=before)observation.edited++;
                return after;
            });
        }
        @Override public LargeSourceSet generate(Collection<? extends SourceFile> additions) {
            if(additions!=null)observation.generated+=additions.size();
            return super.generate(additions);
        }
    }
    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedGuavaEvictingQueueRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static String image() throws Exception {
        var rows=resource(ROOT+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var receipts=resource(ROOT+"source-receipt.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());
        assertEquals(1,receipts.size());
        assertArrayEquals(new String[]{TARGET,"ABSENT",AFTER_SHA,TEMPLATE},
                rows.getFirst().split("\t",-1));
        assertArrayEquals(new String[]{TARGET,AFTER_SHA,Integer.toString(AFTER_BYTES)},
                receipts.getFirst().split("\t",-1));
        String after=resource(ROOT+TEMPLATE);
        assertEquals(AFTER_SHA,sha(after));
        assertEquals(AFTER_BYTES,after.getBytes(StandardCharsets.UTF_8).length);
        return after;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe engine(Recipe recipe) {
        return recipe.getRecipeList().getFirst().getRecipeList().getFirst();
    }
    private static Recipe activate() throws Exception {
        var location=M3OwnedGuavaEvictingQueueRecipeTest.class.getResource(YAML);
        assertNotNull(location,YAML);
        try(var stream=M3OwnedGuavaEvictingQueueRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe recipe=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedGuavaEvictingQueueRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,recipe.getRecipeList().size());
            var delivered=recipe.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());
            assertEquals(1,delivered.getRecipeList().size());
            var selected=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,engine(recipe));
            assertEquals("owned-guava-evicting-queue",selected.getCrateName());
            assertFalse(selected.isLst());
            assertTrue(recipe.validateAll().stream().allMatch(validation->validation.isValid()));
            return recipe;
        }
    }
    private static List<SourceFile> freshSources(Map<String,String> texts) {
        var errors=new ArrayList<Throwable>();
        var inputs=texts.entrySet().stream()
                .map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(texts.size(),parsed.size());
        var expectedOrder=new ArrayList<>(texts.keySet());
        for(int i=0;i<parsed.size();i++) {
            var file=parsed.get(i);
            assertInstanceOf(PlainText.class,file);
            assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(expectedOrder.get(i),path(file));
            assertEquals(texts.get(path(file)),file.printAll());
        }
        return parsed;
    }
    private static Map<String,String> state(String after,boolean present,
            boolean reverse,boolean unrelated) {
        var texts=new LinkedHashMap<String,String>();
        if(present)texts.put(TARGET,after);
        if(unrelated)texts.put(UNRELATED,UNTOUCHED);
        if(!reverse)return texts;
        var reversed=new LinkedHashMap<String,String>();
        var names=new ArrayList<>(texts.keySet());
        java.util.Collections.reverse(names);
        for(String name:names)reversed.put(name,texts.get(name));
        return reversed;
    }
    private static Execution run(List<SourceFile> source) throws Exception {
        Recipe recipe=activate();
        var errors=new ArrayList<Throwable>();
        var observation=new Observation();
        var changes=recipe.run(new ObservedSources(source,observation),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        return new Execution(recipe,changes,observation);
    }
    private static Map<String,SourceFile> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new LinkedHashMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNull(change.getBefore());
            assertNotNull(change.getAfter());
            assertEquals(TARGET,path(change.getAfter()));
            assertInstanceOf(PlainText.class,change.getAfter());
            assertNull(files.put(TARGET,change.getAfter()));
        }
        return files;
    }
    private static Map<String,String> printed(Map<String,SourceFile> source) {
        var texts=new LinkedHashMap<String,String>();
        source.forEach((name,file)->texts.put(name,file.printAll()));
        return texts;
    }
    private static void refuses(List<SourceFile> source,String reason) throws Exception {
        var originals=List.copyOf(source);
        var originalText=source.stream().map(SourceFile::printAll).toList();
        var originalPaths=source.stream().map(M3OwnedGuavaEvictingQueueRecipeTest::path).toList();
        var originalIds=source.stream().map(SourceFile::getId).toList();
        var originalMarkers=source.stream().map(SourceFile::getMarkers).toList();
        var observation=new Observation();
        var supplied=new ObservedSources(source,observation);
        var errors=new ArrayList<Throwable>();
        Recipe recipe=activate();
        var failure=assertThrows(IllegalStateException.class,()->recipe.run(supplied,
                new InMemoryExecutionContext(errors::add),1));
        assertTrue(failure.getMessage().contains(reason+": "+TARGET),failure::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(0,observation.generated,"no source additions before refusal");
        assertEquals(0,observation.edited,"no replacements/deletions before refusal");
        assertTrue(supplied.getChangeset().getAllResults().isEmpty(),"no supplied-batch outputs");
        assertEquals(originals.size(),source.size());
        for(int i=0;i<source.size();i++) {
            assertSame(originals.get(i),source.get(i));
            assertEquals(originalPaths.get(i),path(source.get(i)));
            assertEquals(originalText.get(i),source.get(i).printAll());
            assertEquals(originalIds.get(i),source.get(i).getId());
            assertSame(originalMarkers.get(i),source.get(i).getMarkers());
        }
    }
    @Test void absentOrPresentBothOrdersOptionalUnrelatedAndFreshReactivationFixedPoints() throws Exception {
        String after=image();
        int executions=0;
        for(boolean present:List.of(false,true))
            for(boolean reverse:List.of(false,true))
                for(boolean unrelated:List.of(false,true)) {
                    var source=freshSources(state(after,present,reverse,unrelated));
                    var result=run(source);
                    assertEquals(present?0:1,result.changes.size());
                    assertEquals(present?0:1,result.observation.generated);
                    if(present)assertEquals(0,result.observation.edited);
                    var delivered=renderAll(source,result.changes);
                    assertEquals(unrelated?2:1,delivered.size());
                    var target=delivered.get(TARGET);
                    assertNotNull(target);
                    assertEquals(after,target.printAll());
                    assertEquals(AFTER_SHA,sha(target.printAll()));
                    assertEquals(AFTER_BYTES,target.printAll().getBytes(StandardCharsets.UTF_8).length);
                    for(var file:source)assertSame(file,delivered.get(path(file)),
                            "existing exact postimage/unrelated source retains identity");
                    if(unrelated)assertEquals(UNTOUCHED,delivered.get(UNRELATED).printAll());
                    var reparsed=freshSources(printed(delivered));
                    var fixed=run(reparsed);
                    assertNotSame(result.recipe,fixed.recipe);
                    assertNotSame(engine(result.recipe),engine(fixed.recipe));
                    assertTrue(fixed.changes.isEmpty());
                    assertEquals(0,fixed.observation.generated);
                    assertEquals(0,fixed.observation.edited);
                    var fixedDelivered=renderAll(reparsed,fixed.changes);
                    assertEquals(printed(delivered),printed(fixedDelivered));
                    for(var file:reparsed)assertSame(file,fixedDelivered.get(path(file)));
                    executions++;
                }
        assertEquals(8,executions); // Eight executions; empty/singleton orders coincide.
    }
    @Test void driftIdenticalDuplicateAndBothMixedDuplicateOrdersRefuseBeforeOutput() throws Exception {
        String after=image(),drift=after+"\n// unqualified source drift\n";
        int refusals=0;
        refuses(freshSources(state(drift,true,false,true)),"source drift");
        refusals++;
        var identical=new ArrayList<>(freshSources(state(after,true,false,true)));
        identical.addAll(freshSources(Map.of(TARGET,after)));
        refuses(identical,"duplicate target");
        refusals++;
        for(boolean reverse:List.of(false,true)) {
            var mixed=new ArrayList<>(freshSources(Map.of(TARGET,reverse?drift:after)));
            mixed.addAll(freshSources(Map.of(UNRELATED,UNTOUCHED)));
            mixed.addAll(freshSources(Map.of(TARGET,reverse?after:drift)));
            refuses(mixed,"duplicate target");
            refusals++;
        }
        assertEquals(4,refusals);
    }
}
