/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
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

/** Custody of the exact qualified owned implementation, scalar view and independent provider oracle. */
final class M3OwnedSpringMultiValueMapRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/owned-spring-multivalue-map/";
    private static final String YAML="/META-INF/rewrite/m3-owned-spring-multivalue-map.yml";
    private static final String NAME="com.synexia.m3.OwnedSpringMultiValueMap20261017";
    private static final String BODY="m3/collections/implementations/spring/src/main/java/com/m3/collections/spring/M3MultiValueMap.java";
    private static final String VIEW="m3/collections/implementations/spring/src/main/java/com/m3/collections/spring/M3SingleValueMapView.java";
    private static final String ORACLE="m3/collections/implementations/spring/src/test/java/com/m3/collections/spring/M3MultiValueMapSelfTest.java";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String hash,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedSpringMultiValueMapRecipeTest.class.getResourceAsStream(path)) {
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
        assertEquals(3,rows.size());assertEquals(3,receipts.size());var expected=List.of(BODY,VIEW,ORACLE);
        var templates=List.of("M3MultiValueMap.java.txt","M3SingleValueMapView.java.txt","M3MultiValueMapSelfTest.java.txt");
        var targets=new ArrayList<Target>();String previous="";
        for(int i=0;i<rows.size();i++) {
            String[] cells=rows.get(i).split("\t",-1);assertEquals(4,cells.length);
            String[] receipt=receipts.get(i).split("\t",-1);assertEquals(3,receipt.length);
            assertEquals(expected.get(i),cells[0]);assertEquals("ABSENT",cells[1]);
            assertEquals(cells[0],receipt[0]);assertEquals(cells[2],receipt[1]);
            assertTrue(cells[2].matches("[0-9a-f]{64}"));
            assertTrue(previous.compareTo(cells[0])<0);previous=cells[0];
            assertEquals(templates.get(i),cells[3]);
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
        var location=M3OwnedSpringMultiValueMapRecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3OwnedSpringMultiValueMapRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedSpringMultiValueMapRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());var delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("owned-spring-multivalue-map",engine.getCrateName());assertFalse(engine.isLst());
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
    @Test void allEightInstallationStatesBothOrdersAndFreshParseFixedPoint() throws Exception {
        var rows=targets();int states=0;
        for(int mask=0;mask<8;mask++)for(boolean reverse:List.of(false,true)) {
            var source=freshSources(state(rows,mask,reverse));var changes=run(source);
            assertEquals(3-Integer.bitCount(mask),changes.size());var rendered=renderAll(source,changes);
            assertEquals(4,rendered.size());
            for(var target:rows) {
                assertEquals(target.text,rendered.get(target.path));assertEquals(target.hash,sha(rendered.get(target.path)));
            }
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));var reparsed=freshSources(rendered);
            assertTrue(run(reparsed).isEmpty());assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(16,states);
    }
    @Test void recipeRefusesDriftAndDuplicatesForAllThreeQualifiedOwners() throws Exception {
        var rows=targets();var baseline=state(rows,7,false);int refusals=0;
        for(var target:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
            refuses(freshSources(drift),"source drift",target.path);refusals++;
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path,target.text)));
            refuses(duplicate,"duplicate target",target.path);refusals++;
        }
        assertEquals(6,refusals);
    }
}
