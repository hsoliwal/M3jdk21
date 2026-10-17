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

/** Custody of the exact qualified owned implementation and its independent provider oracle. */
final class M3OwnedApacheBidiMapRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/owned-apache-bidimap/";
    private static final String YAML="/META-INF/rewrite/m3-owned-apache-bidimap.yml";
    private static final String NAME="com.synexia.m3.OwnedApacheBidiMap20261017";
    private static final String BODY="m3/collections/implementations/apache/src/main/java/com/m3/collections/apache/M3BidiMap.java";
    private static final String ORACLE="m3/collections/implementations/apache/src/test/java/com/m3/collections/apache/M3BidiMapSelfTest.java";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String hash,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedApacheBidiMapRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var rows=resource(ROOT+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var receipts=resource(ROOT+"source-receipt.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(2,rows.size());assertEquals(2,receipts.size());var expected=List.of(BODY,ORACLE);
        var targets=new ArrayList<Target>();String previous="";
        for(int i=0;i<rows.size();i++) {
            String[] cells=rows.get(i).split("\t",-1);assertEquals(4,cells.length);
            String[] receipt=receipts.get(i).split("\t",-1);assertEquals(3,receipt.length);
            assertEquals(expected.get(i),cells[0]);assertEquals("ABSENT",cells[1]);
            assertEquals(cells[0],receipt[0]);assertEquals(cells[2],receipt[1]);
            assertTrue(cells[2].matches("[0-9a-f]{64}"));
            assertTrue(previous.compareTo(cells[0])<0);previous=cells[0];
            assertEquals(i==0?"M3BidiMap.java.txt":"M3BidiMapSelfTest.java.txt",cells[3]);
            String text=resource(ROOT+cells[3]);assertEquals(cells[2],sha(text));
            assertEquals(Integer.parseInt(receipt[2]),text.getBytes(StandardCharsets.UTF_8).length);
            targets.add(new Target(cells[0],cells[2],text));
        }
        return targets;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3OwnedApacheBidiMapRecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3OwnedApacheBidiMapRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedApacheBidiMapRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());var delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("owned-apache-bidimap",engine.getCrateName());assertFalse(engine.isLst());
            assertTrue(activated.validateAll().stream().allMatch(validation->validation.isValid()));return activated;
        }
    }
    private static List<SourceFile> freshSources(Map<String,String> source) {
        var errors=new ArrayList<Throwable>();
        var inputs=source.entrySet().stream()
                .map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());assertEquals(source.size(),parsed.size());
        for(var file:parsed) {
            assertInstanceOf(PlainText.class,file);assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(source.get(path(file)),file.printAll());
        }
        return parsed;
    }
    private static List<Result> run(List<SourceFile> source) throws Exception {
        var errors=new ArrayList<Throwable>();
        var changes=activate().run(new InMemoryLargeSourceSet(source),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());return changes;
    }
    private static Map<String,String> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new TreeMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNull(change.getBefore());assertNotNull(change.getAfter());assertInstanceOf(PlainText.class,change.getAfter());
            assertNull(files.put(path(change.getAfter()),change.getAfter()));
        }
        var rendered=new TreeMap<String,String>();
        files.forEach((name,file)->rendered.put(name,file.printAll()));return rendered;
    }
    private static LinkedHashMap<String,String> state(List<Target> targets,int mask,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        for(int i=0;i<targets.size();i++)if((mask & 1<<i)!=0)input.put(targets.get(i).path,targets.get(i).text);
        input.put(UNRELATED,UNTOUCHED);if(!reverse)return input;
        var inverted=new LinkedHashMap<String,String>();
        var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
        for(String key:keys)inverted.put(key,input.get(key));return inverted;
    }
    private static void refuses(List<SourceFile> source,String reason,String target) throws Exception {
        Recipe recipe=activate();var errors=new ArrayList<Throwable>();
        var refusal=assertThrows(IllegalStateException.class,()->recipe.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(refusal.getMessage().contains(reason+": "+target),refusal::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
    }
    @Test void allFourInstallationStatesBothOrdersAndFreshParseFixedPoint() throws Exception {
        var rows=targets();int states=0;
        for(int mask=0;mask<4;mask++)for(boolean reverse:List.of(false,true)) {
            var source=freshSources(state(rows,mask,reverse));var changes=run(source);
            assertEquals(2-Integer.bitCount(mask),changes.size());var rendered=renderAll(source,changes);
            assertEquals(3,rendered.size());
            for(var target:rows) {
                assertEquals(target.text,rendered.get(target.path));assertEquals(target.hash,sha(rendered.get(target.path)));
            }
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));var reparsed=freshSources(rendered);
            assertTrue(run(reparsed).isEmpty());assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(8,states);
    }
    @Test void recipeRefusesDriftAndDuplicatesForBothQualifiedOwners() throws Exception {
        var rows=targets();var baseline=state(rows,3,false);
        for(var target:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
            refuses(freshSources(drift),"source drift",target.path);
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path,target.text)));
            refuses(duplicate,"duplicate target",target.path);
        }
    }
}
