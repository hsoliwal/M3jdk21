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

/**
 * Immutable V1/V2 receiving lineage through the existing Java snapshot engine.
 * PlainText replay proves exact source custody, not Java behavior or type admission.
 */
final class M3OwnedApacheBidiMapV2RecipeTest {
    private static final String RESOURCE_ROOT="/com/synexia/rewrite/hash-pinned-java/";
    private static final String V1="owned-apache-bidimap";
    private static final String INSTALL="owned-apache-bidimap-v2";
    private static final String TRANSITION="owned-apache-bidimap-values-transition-v2";
    private static final String YAML="/META-INF/rewrite/m3-owned-apache-bidimap-v2.yml";
    private static final String INSTALL_NAME="com.synexia.m3.OwnedApacheBidiMap20261017V2";
    private static final String TRANSITION_NAME="com.synexia.m3.OwnedApacheBidiMapValuesTransition20261017V2";
    private static final String BODY="m3/collections/implementations/apache/src/main/java/com/m3/collections/apache/M3BidiMap.java";
    private static final String ORACLE="m3/collections/implementations/apache/src/test/java/com/m3/collections/apache/M3BidiMapSelfTest.java";
    private static final String V1_BODY_HASH="f811a07b8fcd6be3a4b64d61f548742ad44a047f58ecb8641c490850d51bad9e";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String before,String after,String text) { }
    private record Lineage(List<Target> installed,Target transition,Target oldBody,Target oldOracle) { }

    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedApacheBidiMapV2RecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<Target> targets(String crate,List<String> expectedPaths) throws Exception {
        String root=RESOURCE_ROOT+crate+"/";
        var rows=resource(root+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(expectedPaths.size(),rows.size());
        var result=new ArrayList<Target>();
        String previous="";
        for(int i=0;i<rows.size();i++) {
            String[] cells=rows.get(i).split("\t",-1);
            assertEquals(4,cells.length);
            assertEquals(expectedPaths.get(i),cells[0]);
            assertTrue(previous.compareTo(cells[0])<0);
            previous=cells[0];
            assertTrue(cells[1].equals("ABSENT")||cells[1].matches("[0-9a-f]{64}"));
            assertTrue(cells[2].matches("[0-9a-f]{64}"));
            assertEquals(Path.of(cells[0]).getFileName()+".txt",cells[3]);
            String text=resource(root+cells[3]);
            assertEquals(cells[2],sha(text));
            result.add(new Target(cells[0],cells[1],cells[2],text));
        }
        return List.copyOf(result);
    }

    private static Lineage lineage() throws Exception {
        var old=targets(V1,List.of(BODY,ORACLE));
        var installed=targets(INSTALL,List.of(BODY,ORACLE));
        var transitions=targets(TRANSITION,List.of(BODY));
        Target oldBody=old.get(0),oldOracle=old.get(1),transition=transitions.get(0);
        assertEquals("ABSENT",oldBody.before());
        assertEquals("ABSENT",oldOracle.before());
        assertEquals(V1_BODY_HASH,oldBody.after());
        assertEquals(V1_BODY_HASH,sha(oldBody.text()));
        for(var target:installed)assertEquals("ABSENT",target.before());
        assertNotEquals(oldBody.after(),installed.get(0).after(),"V2 requires a new body postimage");
        assertEquals(oldOracle.after(),installed.get(1).after(),"existing SelfTest hash remains unchanged");
        assertEquals(oldOracle.text(),installed.get(1).text(),"existing SelfTest bytes remain unchanged");
        assertEquals(V1_BODY_HASH,transition.before(),"only the frozen V1 body is a transition preimage");
        assertEquals(installed.get(0).after(),transition.after());
        assertEquals(installed.get(0).text(),transition.text(),"install and transition share one exact V2 body");
        return new Lineage(installed,transition,oldBody,oldOracle);
    }

    private static Recipe activate(boolean transition) throws Exception {
        String name=transition?TRANSITION_NAME:INSTALL_NAME;
        String crate=transition?TRANSITION:INSTALL;
        var location=M3OwnedApacheBidiMapV2RecipeTest.class.getResource(YAML);
        assertNotNull(location,YAML);
        try(var stream=M3OwnedApacheBidiMapV2RecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedApacheBidiMapV2RecipeTest.class.getClassLoader()))
                    .build().activateRecipes(name);
            assertEquals(1,activated.getRecipeList().size());
            var delivered=activated.getRecipeList().getFirst();
            assertEquals(name,delivered.getName());
            assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals(crate,engine.getCrateName());
            assertFalse(engine.isLst());
            assertTrue(activated.validateAll().stream().allMatch(validation->validation.isValid()));
            return activated;
        }
    }

    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }

    private static List<SourceFile> freshSources(Map<String,String> source) {
        var errors=new ArrayList<Throwable>();
        var inputs=source.entrySet().stream()
                .map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(source.size(),parsed.size());
        for(var file:parsed) {
            assertInstanceOf(PlainText.class,file);
            assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(source.get(path(file)),file.printAll());
        }
        return parsed;
    }

    private static List<Result> run(boolean transition,List<SourceFile> source) throws Exception {
        var errors=new ArrayList<Throwable>();
        var changes=activate(transition).run(new InMemoryLargeSourceSet(source),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        return changes;
    }

    private static Map<String,String> renderAll(List<SourceFile> source,List<Result> changes,
            boolean transition) {
        var files=new TreeMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNotNull(change.getAfter());
            assertInstanceOf(PlainText.class,change.getAfter());
            if(transition) {
                assertNotNull(change.getBefore());
                assertEquals(BODY,path(change.getBefore()));
                assertEquals(BODY,path(change.getAfter()));
                var removed=files.remove(BODY);
                assertNotNull(removed);
                assertEquals(removed.printAll(),change.getBefore().printAll());
            } else {
                assertNull(change.getBefore());
            }
            assertNull(files.put(path(change.getAfter()),change.getAfter()));
        }
        var rendered=new TreeMap<String,String>();
        files.forEach((name,file)->rendered.put(name,file.printAll()));
        return rendered;
    }

    private static LinkedHashMap<String,String> ordered(Map<String,String> input,boolean reverse) {
        var result=new LinkedHashMap<String,String>();
        var keys=new ArrayList<>(input.keySet());
        if(reverse)java.util.Collections.reverse(keys);
        for(String key:keys)result.put(key,input.get(key));
        return result;
    }

    private static LinkedHashMap<String,String> installationState(List<Target> targets,int mask,
            boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        for(int i=0;i<targets.size();i++)if((mask & 1<<i)!=0)
            input.put(targets.get(i).path(),targets.get(i).text());
        input.put(UNRELATED,UNTOUCHED);
        return ordered(input,reverse);
    }

    private static void assertInstalled(Lineage lineage,Map<String,String> rendered) throws Exception {
        assertEquals(3,rendered.size());
        for(var target:lineage.installed()) {
            assertEquals(target.text(),rendered.get(target.path()));
            assertEquals(target.after(),sha(rendered.get(target.path())));
        }
        assertEquals(UNTOUCHED,rendered.get(UNRELATED));
    }

    private static void refuses(boolean transition,List<SourceFile> source,String reason,
            String target) throws Exception {
        var original=source.stream().map(file->path(file)+"\n"+file.printAll()).toList();
        var errors=new ArrayList<Throwable>();
        Recipe recipe=activate(transition);
        var refusal=assertThrows(IllegalStateException.class,()->recipe.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(refusal.getMessage().contains(reason+": "+target),refusal::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(original,source.stream().map(file->path(file)+"\n"+file.printAll()).toList());
    }

    @Test void allFourV2InstallationStatesBothOrdersAndFreshParseFixedPoint() throws Exception {
        var lineage=lineage();
        int states=0;
        for(int mask=0;mask<4;mask++)for(boolean reverse:List.of(false,true)) {
            var source=freshSources(installationState(lineage.installed(),mask,reverse));
            var changes=run(false,source);
            assertEquals(2-Integer.bitCount(mask),changes.size());
            var rendered=renderAll(source,changes,false);
            assertInstalled(lineage,rendered);
            var reparsed=freshSources(rendered);
            assertTrue(run(false,reparsed).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of(),false));
            states++;
        }
        assertEquals(8,states);
    }

    @Test void v2InstallationRefusesDriftAndDuplicatesForBothQualifiedOwners() throws Exception {
        var lineage=lineage();
        var baseline=installationState(lineage.installed(),3,false);
        int refusals=0;
        for(var target:lineage.installed()) {
            var drift=new LinkedHashMap<>(baseline);
            drift.put(target.path(),"drift\n");
            refuses(false,freshSources(drift),"source drift",target.path());
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path(),target.text())));
            refuses(false,duplicate,"duplicate target",target.path());
            refusals+=2;
        }
        assertEquals(4,refusals);
    }

    @Test void actualFrozenV1BodyTransitionsInBothOrdersThenFreshV2IsFixedPoint() throws Exception {
        var lineage=lineage();
        int transitions=0;
        for(boolean reverse:List.of(false,true)) {
            var baseline=new LinkedHashMap<String,String>();
            baseline.put(BODY,lineage.oldBody().text());
            baseline.put(ORACLE,lineage.oldOracle().text());
            baseline.put(UNRELATED,UNTOUCHED);
            var source=freshSources(ordered(baseline,reverse));
            var changes=run(true,source);
            assertEquals(1,changes.size());
            assertEquals(V1_BODY_HASH,sha(changes.getFirst().getBefore().printAll()));
            assertEquals(lineage.transition().after(),sha(changes.getFirst().getAfter().printAll()));
            var rendered=renderAll(source,changes,true);
            assertInstalled(lineage,rendered);
            var reparsed=freshSources(rendered);
            assertTrue(run(true,reparsed).isEmpty());
            assertTrue(run(false,freshSources(rendered)).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of(),true));
            transitions++;
        }
        assertEquals(2,transitions);
    }

    @Test void transitionRefusesMissingDriftDuplicateAndAlteredV1Preimage() throws Exception {
        var lineage=lineage();
        var baseline=new LinkedHashMap<String,String>();
        baseline.put(BODY,lineage.oldBody().text());
        baseline.put(ORACLE,lineage.oldOracle().text());
        baseline.put(UNRELATED,UNTOUCHED);
        var missing=new LinkedHashMap<>(baseline);
        missing.remove(BODY);
        refuses(true,freshSources(missing),"required source missing",BODY);
        var drift=new LinkedHashMap<>(baseline);
        drift.put(BODY,lineage.transition().text()+"// drifted V2 postimage\n");
        assertNotEquals(lineage.transition().after(),sha(drift.get(BODY)));
        refuses(true,freshSources(drift),"source drift",BODY);
        var duplicate=new ArrayList<>(freshSources(baseline));
        duplicate.addAll(freshSources(Map.of(BODY,lineage.oldBody().text())));
        refuses(true,duplicate,"duplicate target",BODY);
        var alteredV1=new LinkedHashMap<>(baseline);
        alteredV1.put(BODY,lineage.oldBody().text()+"// altered V1 preimage\n");
        assertNotEquals(V1_BODY_HASH,sha(alteredV1.get(BODY)));
        assertNotEquals(lineage.transition().after(),sha(alteredV1.get(BODY)));
        refuses(true,freshSources(alteredV1),"source drift",BODY);
    }
}
