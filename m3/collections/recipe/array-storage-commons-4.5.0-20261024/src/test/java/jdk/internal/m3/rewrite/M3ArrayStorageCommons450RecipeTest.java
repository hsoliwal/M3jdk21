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
package jdk.internal.m3.rewrite;

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
 * Custody of leaf E2: the five received Commons Collections 4.5.0 bodies (preimages = the afterSHA256 of
 * receiving/commons-collections-4.5.0-20261018) become their array-storage derivatives, and the seven array-storage
 * tests are installed. Finite matrix: every subset of already-transitioned bodies, the tests absent or present, both
 * input orders, each with a fixed point; every missing, drifted or duplicated preimage refused before any output.
 * Recipe custody is not runtime proof; the array-storage tests and the donor tests qualify the bodies.
 */
//FLLDCIM: ALG - recipe-first: the E2 array storage is delivered as an exact hash-pinned 4.5.0 transition
final class M3ArrayStorageCommons450RecipeTest {
    private static final String ROOT="/jdk/internal/m3/rewrite/hash-pinned-java/array-storage-commons-450/";
    private static final String YAML="/META-INF/rewrite/m3-array-storage-commons-450.yml";
    private static final String NAME="jdk.internal.m3.ArrayStorageCommons450V20261024";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private static final int TESTS=7;
    /** The 4.5.0 receipt (receiving/commons-collections-4.5.0-20261018) afterSHA256 of the five bodies, manifest order. */
    private static final List<String> RECEIVED=List.of(
            "6d79ee74b7d17fca422f3b3c7eda99ad0236c49c75afc416dcb07d9ce956f53c",
            "a4ff2e871654e2da438b9dadd84bffccc8636d597db9f99cd6558a64bf697a51",
            "77e8c550a8e53c08ceaaaab58b27fb97adc9a854db2a710a47eaf416684368be",
            "7cdd20571ca41d6c6f1d01f929c7223313b87fbb29030017c84400ff3f7debf8",
            "cd875ce591d9fe39ea39ee72cafef8064a66cde0fe9eaad32403accb7e1e216a");
    private record Row(String path,String before,String after,String text) {
        boolean added() { return "ABSENT".equals(before); }
    }

    private static String resource(String path) throws Exception {
        try(var stream=M3ArrayStorageCommons450RecipeTest.class.getResourceAsStream(path)) {
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
        assertEquals(RECEIVED.size()+TESTS,rows.size());
        assertEquals(RECEIVED,rows.stream().filter(row->!row.added()).map(Row::before).toList());
        return rows;
    }
    /** The received 4.5.0 preimage kept beside the template as {@code <name>.before.txt}. */
    private static String received(Row row) throws Exception {
        String text=resource(ROOT+row.path().substring(row.path().lastIndexOf('/')+1)+".before.txt");
        assertEquals(row.before(),sha(text));return text;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3ArrayStorageCommons450RecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3ArrayStorageCommons450RecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3ArrayStorageCommons450RecipeTest.class.getClassLoader())).build().activateRecipes(NAME);
            var delivered=activated.getRecipeList().getFirst();assertEquals(NAME,delivered.getName());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("array-storage-commons-450",engine.getCrateName());assertFalse(engine.isLst());
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

    @Test void everyBodySubsetWithTestsAbsentOrPresentBothOrdersAndFixedPoints() throws Exception {
        var rows=rows();var recipe=activate();int states=0;
        var bodies=rows.stream().filter(row->!row.added()).toList();
        for(int mask=0;mask<1<<bodies.size();mask++)for(boolean installed:List.of(false,true))for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(var row:rows) {
                if(row.added()) { if(installed)input.put(row.path(),row.text()); continue; }
                input.put(row.path(),(mask&1<<bodies.indexOf(row))!=0?row.text():received(row));
            }
            input.put(UNRELATED,UNTOUCHED);
            if(reverse) {
                var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
                var reversed=new LinkedHashMap<String,String>();for(String key:keys)reversed.put(key,input.get(key));input=reversed;
            }
            var changes=run(recipe,sources(input));
            assertEquals(bodies.size()-Integer.bitCount(mask)+(installed?0:TESTS),changes.size());
            var output=new LinkedHashMap<>(input);
            for(var change:changes)output.put(path(change.getAfter()),change.getAfter().printAll());
            for(var row:rows)assertEquals(row.text(),output.get(row.path()),row.path());
            assertEquals(UNTOUCHED,output.get(UNRELATED));assertEquals(rows.size()+1,output.size());
            assertTrue(run(recipe,sources(output)).isEmpty());states++;
        }
        assertEquals(128,states); // 32 body subsets x tests absent/present x 2 orders, not every permutation.
    }

    @Test void everyMissingDriftedOrDuplicatedPreimageRefusedBeforeAnyOutput() throws Exception {
        var rows=rows();var recipe=activate();int refusals=0;
        var baseline=new LinkedHashMap<String,String>();
        for(var row:rows)if(!row.added())baseline.put(row.path(),received(row));
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
        assertEquals(29,refusals);
    }
}
