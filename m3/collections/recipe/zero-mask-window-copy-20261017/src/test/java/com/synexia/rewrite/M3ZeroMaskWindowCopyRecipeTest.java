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

/** Exact source-custody replay; native execution is bound by the separate root JNI receipts. */
final class M3ZeroMaskWindowCopyRecipeTest {
    private static final String YAML="/META-INF/rewrite/m3-zero-mask-window-copy.yml";
    private static final String NAME="com.synexia.m3.ZeroMaskWindowCopy20261017";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String beforeHash,String afterHash,String before,String after) { }

    private static String resource(String path) throws Exception {
        try(var stream=M3ZeroMaskWindowCopyRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static Target manifest(String root,String path,String beforeHash,String afterHash,
            String beforeName) throws Exception {
        var rows=resource(root+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());String[] cells=rows.getFirst().split("\t",-1);
        assertEquals(4,cells.length);assertEquals(path,cells[0]);
        assertEquals(beforeHash,cells[1]);assertEquals(afterHash,cells[2]);
        String before=resource(root+beforeName),after=resource(root+cells[3]);
        assertEquals(beforeHash,sha(before));assertEquals(afterHash,sha(after));
        assertNotEquals(beforeHash,afterHash);
        return new Target(path,beforeHash,afterHash,before,after);
    }
    private static List<Target> targets() throws Exception {
        return List.of(
                manifest("/com/synexia/rewrite/hash-pinned-java/native-zero-mask-window-java/",
                    "m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java",
                    "86eb2c9a436aac6f6e5b99f5c275636ca272d849dd08aa07f8796271167c63a1",
                    "6b6fe32c60db7fb0e31e17e6cce0df727c856f17adff9eba32d5efd141498872",
                    "M3IndexMaskCompactorSelfTest.java.before"),
                manifest("/com/synexia/rewrite/hash-pinned-text/native-zero-mask-window-c/",
                    "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
                    "59b1bb47f2df09f1455e4ce2160becbeefd6da877ecddca4ad245a286d14fa48",
                    "cb8e8c66e07cc4651e95a0c28ad834d2c9de48df34dc90c6d7da68443f1da531",
                    "m3_index_mask_compactor.c.before"));
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3ZeroMaskWindowCopyRecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3ZeroMaskWindowCopyRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3ZeroMaskWindowCopyRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());
            Recipe delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(2,delivered.getRecipeList().size());
            var javaEngine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().get(0));
            assertEquals("native-zero-mask-window-java",javaEngine.getCrateName());assertFalse(javaEngine.isLst());
            var textEngine=assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class,delivered.getRecipeList().get(1));
            assertEquals("native-zero-mask-window-c",textEngine.getCrateName());
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
            assertNotNull(change.getAfter());assertInstanceOf(PlainText.class,change.getAfter());
            assertNotNull(change.getBefore());assertNotNull(files.remove(path(change.getBefore())));
            assertNull(files.put(path(change.getAfter()),change.getAfter()));
        }
        var rendered=new TreeMap<String,String>();
        files.forEach((name,file)->rendered.put(name,file.printAll()));return rendered;
    }
    private static LinkedHashMap<String,String> state(List<Target> targets,int mask,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        for(int i=0;i<targets.size();i++) {
            var target=targets.get(i);input.put(target.path,(mask & 1<<i)==0?target.before:target.after);
        }
        input.put(UNRELATED,UNTOUCHED);if(!reverse)return input;
        var inverted=new LinkedHashMap<String,String>();
        var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
        for(String key:keys)inverted.put(key,input.get(key));return inverted;
    }
    private static void refuses(List<SourceFile> source,String reason,String target) throws Exception {
        Recipe composite=activate();var errors=new ArrayList<Throwable>();
        var refusal=assertThrows(IllegalStateException.class,()->composite.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(refusal.getMessage().contains(reason+": "+target),refusal::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
    }
    @Test void allFourOldNewStatesBothOrdersAndFreshParseFixedPoint() throws Exception {
        var rows=targets();int states=0;
        for(int mask=0;mask<4;mask++)for(boolean reverse:List.of(false,true)) {
            var source=freshSources(state(rows,mask,reverse));var changes=run(source);
            assertEquals(2-Integer.bitCount(mask),changes.size());
            var rendered=renderAll(source,changes);assertEquals(3,rendered.size());
            for(var target:rows) {
                assertEquals(target.after,rendered.get(target.path));
                assertEquals(target.afterHash,sha(rendered.get(target.path)));
            }
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));
            var reparsed=freshSources(rendered);assertTrue(run(reparsed).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(8,states);
    }
    @Test void compositeRefusesDriftDuplicatesAndMissingForBothRequiredFiles() throws Exception {
        var rows=targets();var baseline=state(rows,0,false);
        for(var target:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
            refuses(freshSources(drift),"source drift",target.path);
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path,target.before)));
            refuses(duplicate,"duplicate target",target.path);
            var missing=new LinkedHashMap<>(baseline);assertNotNull(missing.remove(target.path));
            refuses(freshSources(missing),"required source missing",target.path);
        }
    }
}
