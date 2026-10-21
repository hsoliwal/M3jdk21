/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** Exact existing-source transition; this receiving proof is not an iterator behavior proof. */
final class M3CountedBagIteratorRemoveRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/counted-bag-iterator-remove/";
    private static final String YAML="/META-INF/rewrite/m3-counted-bag-iterator-remove.yml";
    private static final String NAME="com.synexia.m3.CountedBagIteratorRemove20261018";
    private static final String TARGET="m3/collections/src/main/java/com/m3/collections/M3CountedBag.java";
    private static final String BEFORE_SHA="c04ed48de6b24bb4a86f97c4aa6187e2ce11e43b082e05ef3189c618ddf1143b";
    private static final String AFTER_SHA="020b91ae24ab2bb991146861dbaecf6e7fa9836811804645722190b972c4e311";
    private static final int BEFORE_BYTES=13854;
    private static final int AFTER_BYTES=13962;
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";

    private record Images(String before,String after) { }

    private static String resource(String path) throws Exception {
        try(var stream=M3CountedBagIteratorRemoveRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static Images images() throws Exception {
        var rows=resource(ROOT+"manifest.tsv").lines().filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var receipts=resource(ROOT+"source-receipt.tsv").lines().filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());assertEquals(1,receipts.size());
        assertArrayEquals(new String[]{TARGET,BEFORE_SHA,AFTER_SHA,"M3CountedBag.java.txt"},rows.getFirst().split("\t",-1));
        assertArrayEquals(new String[]{TARGET,AFTER_SHA,Integer.toString(AFTER_BYTES)},receipts.getFirst().split("\t",-1));
        String before=resource(ROOT+"M3CountedBag.java.before.txt"),after=resource(ROOT+"M3CountedBag.java.txt");
        assertEquals(BEFORE_SHA,sha(before));assertEquals(AFTER_SHA,sha(after));
        assertEquals(BEFORE_BYTES,before.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(AFTER_BYTES,after.getBytes(StandardCharsets.UTF_8).length);
        assertNotEquals(before,after);return new Images(before,after);
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3CountedBagIteratorRemoveRecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3CountedBagIteratorRemoveRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe recipe=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3CountedBagIteratorRemoveRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,recipe.getRecipeList().size());var delivered=recipe.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("counted-bag-iterator-remove",engine.getCrateName());assertFalse(engine.isLst());
            assertTrue(recipe.validateAll().stream().allMatch(validation->validation.isValid()));return recipe;
        }
    }
    private static List<SourceFile> freshSources(Map<String,String> texts) {
        var errors=new ArrayList<Throwable>();
        var inputs=texts.entrySet().stream()
                .map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());assertEquals(texts.size(),parsed.size());
        for(var file:parsed) {
            assertInstanceOf(PlainText.class,file);assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(texts.get(path(file)),file.printAll());
        }
        return parsed;
    }
    private static List<Result> run(List<SourceFile> source) throws Exception {
        var errors=new ArrayList<Throwable>();
        var changes=activate().run(new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());return changes;
    }
    private static Map<String,String> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new TreeMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNotNull(change.getBefore());assertNotNull(change.getAfter());
            assertEquals(TARGET,path(change.getBefore()));assertEquals(TARGET,path(change.getAfter()));
            assertInstanceOf(PlainText.class,change.getAfter());
            var previous=files.put(path(change.getAfter()),change.getAfter());assertNotNull(previous);
            assertEquals(previous.printAll(),change.getBefore().printAll());
        }
        var rendered=new TreeMap<String,String>();files.forEach((name,file)->rendered.put(name,file.printAll()));
        return rendered;
    }
    private static Map<String,String> state(String target,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        if(reverse) {input.put(UNRELATED,UNTOUCHED);input.put(TARGET,target);}
        else {input.put(TARGET,target);input.put(UNRELATED,UNTOUCHED);}
        return input;
    }
    private static void refuses(List<SourceFile> source,String reason) throws Exception {
        var printed=source.stream().map(SourceFile::printAll).toList();
        Recipe recipe=activate();var errors=new ArrayList<Throwable>();
        var failure=assertThrows(IllegalStateException.class,()->recipe.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(failure.getMessage().contains(reason+": "+TARGET),failure::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(printed,source.stream().map(SourceFile::printAll).toList());
    }
    @Test void exactBeforeAfterBothOrdersAndFreshMarkerFreeFixedPoints() throws Exception {
        var texts=images();int states=0;
        for(boolean alreadyAfter:List.of(false,true))for(boolean reverse:List.of(false,true)) {
            String initial=alreadyAfter?texts.after:texts.before;
            var source=freshSources(state(initial,reverse));var changes=run(source);
            assertEquals(alreadyAfter?0:1,changes.size());
            if(!alreadyAfter) {
                assertEquals(texts.before,changes.getFirst().getBefore().printAll());
                assertEquals(texts.after,changes.getFirst().getAfter().printAll());
            }
            var rendered=renderAll(source,changes);assertEquals(2,rendered.size());
            assertEquals(texts.after,rendered.get(TARGET));assertEquals(AFTER_SHA,sha(rendered.get(TARGET)));
            assertEquals(AFTER_BYTES,rendered.get(TARGET).getBytes(StandardCharsets.UTF_8).length);
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));
            var reparsed=freshSources(rendered);assertTrue(run(reparsed).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(4,states);
    }
    @Test void missingDriftAndThreeDuplicateRepresentationsRefuse() throws Exception {
        var texts=images();int refusals=0;
        refuses(freshSources(Map.of(UNRELATED,UNTOUCHED)),"required source missing");refusals++;
        refuses(freshSources(state("drift\n",false)),"source drift");refusals++;
        for(var pair:List.of(List.of(texts.before,texts.before),List.of(texts.before,texts.after),List.of(texts.after,texts.after))) {
            var duplicate=new ArrayList<>(freshSources(state(pair.getFirst(),false)));
            duplicate.addAll(freshSources(Map.of(TARGET,pair.getLast())));
            refuses(duplicate,"duplicate target");refusals++;
        }
        assertEquals(5,refusals);
    }
}
