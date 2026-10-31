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

/**
 * Custody of the D1 transition: the three v2 multivalue bodies (preimages = the v2 postimages of
 * owned-apache-multivalue-20261018) become the counter bodies, and the counter differential test is installed (the
 * size bench lies outside the engine's Maven roots; the v4 receipt pins it). Finite matrix: every subset of already-transitioned bodies, new files absent or present, both input
 * orders, each with a fixed point; every missing, drifted or duplicated preimage refused before any output.
 * Recipe custody is not runtime proof; the differential test and the contract self-tests qualify the bodies.
 */
//FLLDCIM: ALG - recipe-first: the D1 counter change is delivered as an exact hash-pinned v2 -> v3 transition
final class M3ApacheMultiValueV2ToV3RecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/apache-multivalue-v2-to-v3/";
    private static final String YAML="/META-INF/rewrite/m3-apache-multivalue-v2-to-v3.yml";
    private static final String NAME="com.synexia.m3.ApacheMultiValueV2ToV320261024";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    /** The v2 postimages (owned-apache-multivalue-v2 manifest) of the three changed bodies, in manifest order. */
    private static final List<String> V2=List.of(
            "bdd6051aecad2ff0a2c2ce7b788aeae51331e5dcef2d5c9f38270520bcdfbfc1",
            "455e4a5cd4974be4bc9f52aef362a3fe9993802806d3b8fa6b31d2954f322ed8",
            "b6d1c925716f44578706b5a5786f23ecab1eba652a8c6dea5f02fdf67695fcbd");
    private record Row(String path,String before,String after,String text) {
        boolean added() { return "ABSENT".equals(before); }
    }

    private static String resource(String path) throws Exception {
        try(var stream=M3ApacheMultiValueV2ToV3RecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Row> rows() throws Exception {
        var rows=new ArrayList<Row>();String previous="";
        for(String line:resource(ROOT+"manifest.tsv").lines().toList()) {
            String[] cells=line.split("\t",-1);assertEquals(4,cells.length);
            assertTrue(previous.compareTo(cells[0])<0);previous=cells[0];
            String text=resource(ROOT+cells[3]);assertEquals(cells[2],sha(text));
            rows.add(new Row(cells[0],cells[1],cells[2],text));
        }
        assertEquals(4,rows.size());
        assertEquals(V2,rows.stream().filter(row->!row.added()).map(Row::before).toList());
        assertEquals(1,rows.stream().filter(Row::added).count());
        return rows;
    }
    /** The v2 preimage kept beside the template as {@code <name>.before.txt}; its hash is the manifest preimage. */
    private static String v2(Row row) throws Exception {
        String text=resource(ROOT+row.path().substring(row.path().lastIndexOf('/')+1)+".before.txt");
        assertEquals(row.before(),sha(text));return text;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3ApacheMultiValueV2ToV3RecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3ApacheMultiValueV2ToV3RecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3ApacheMultiValueV2ToV3RecipeTest.class.getClassLoader())).build().activateRecipes(NAME);
            var delivered=activated.getRecipeList().getFirst();assertEquals(NAME,delivered.getName());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("apache-multivalue-v2-to-v3",engine.getCrateName());assertFalse(engine.isLst());
            assertTrue(activated.validateAll().stream().allMatch(validation->validation.isValid()));return activated;
        }
    }
    private static List<SourceFile> sources(Map<String,String> source) {
        var errors=new ArrayList<Throwable>();
        var inputs=source.entrySet().stream().map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());assertEquals(source.size(),parsed.size());
        parsed.forEach(file->assertInstanceOf(PlainText.class,file));return parsed;
    }
    private static List<Result> run(Recipe recipe,List<SourceFile> source) {
        var errors=new ArrayList<Throwable>();
        var changes=recipe.run(new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());return changes;
    }
    private static void refused(Recipe recipe,List<SourceFile> source) {
        var errors=new ArrayList<Throwable>();
        assertThrows(IllegalStateException.class,()->recipe.run(new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(errors.isEmpty(),()->errors.toString());
    }

    @Test void everyBodySubsetWithNewFilesAbsentOrPresentBothOrdersAndFixedPoints() throws Exception {
        var rows=rows();var recipe=activate();int states=0;
        var bodies=rows.stream().filter(row->!row.added()).toList();
        for(int mask=0;mask<1<<bodies.size();mask++)for(boolean installed:List.of(false,true))for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(var row:rows) {
                if(row.added()) { if(installed)input.put(row.path(),row.text()); continue; }
                input.put(row.path(),(mask&1<<bodies.indexOf(row))!=0?row.text():v2(row));
            }
            input.put(UNRELATED,UNTOUCHED);
            if(reverse) {
                var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
                var reversed=new LinkedHashMap<String,String>();for(String key:keys)reversed.put(key,input.get(key));input=reversed;
            }
            var changes=run(recipe,sources(input));
            assertEquals(bodies.size()-Integer.bitCount(mask)+(installed?0:1),changes.size());
            var output=new LinkedHashMap<>(input);
            for(var change:changes)output.put(path(change.getAfter()),change.getAfter().printAll());
            for(var row:rows)assertEquals(row.text(),output.get(row.path()),row.path());
            assertEquals(UNTOUCHED,output.get(UNRELATED));assertEquals(rows.size()+1,output.size());
            assertTrue(run(recipe,sources(output)).isEmpty());states++;
        }
        assertEquals(32,states); // 8 body subsets x new file absent/present x 2 orders, not every permutation.
    }

    @Test void everyMissingDriftedOrDuplicatedPreimageRefusedBeforeAnyOutput() throws Exception {
        var rows=rows();var recipe=activate();int refusals=0;
        var baseline=new LinkedHashMap<String,String>();
        for(var row:rows)if(!row.added())baseline.put(row.path(),v2(row));
        baseline.put(UNRELATED,UNTOUCHED);
        for(var row:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(row.path(),"drift\n");
            refused(recipe,sources(drift));refusals++;
            var duplicate=new ArrayList<>(sources(baseline));
            String copy=row.added()?row.text():baseline.get(row.path());
            if(row.added())duplicate.addAll(sources(Map.of(row.path(),copy)));
            duplicate.addAll(sources(Map.of(row.path(),copy)));
            refused(recipe,duplicate);refusals++;
            if(!row.added()) {
                var missing=new LinkedHashMap<>(baseline);missing.remove(row.path());
                refused(recipe,sources(missing));refusals++;
            }
        }
        assertEquals(11,refusals);
    }
}
