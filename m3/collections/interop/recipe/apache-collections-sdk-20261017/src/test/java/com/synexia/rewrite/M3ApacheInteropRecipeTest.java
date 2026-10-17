/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
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
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** Actual OpenRewrite Java source generation, POM text operation, and refusal tests. */
final class M3ApacheInteropRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/apache-collections-interop/";
    private static final String TEXT_ROOT="/com/synexia/rewrite/hash-pinned-text/apache-collections-pom/";
    private static final String YAML="/META-INF/rewrite/m3-apache-collections-interop.yml";
    private static final String RECIPE_NAME="com.synexia.m3.ApacheCollectionsInterop20261017";
    private static final String DISPLAY_NAME="Apache Commons ListValuedMap / SetValuedMap backed by canonical M3 arrays and slot indexes";
    private static final String REFINEMENT_ROOT="/com/synexia/rewrite/hash-pinned-java/apache-provider-contract-refinement/";
    private static final String REFINEMENT_YAML="/META-INF/rewrite/m3-apache-provider-contract-refinement.yml";
    private static final String REFINEMENT_NAME="com.synexia.m3.ApacheProviderContractRefinement20261017";
    private static final String CONTRACT_PATH="m3/collections/interop/src/test/java/com/m3/collections/interop/M3ApacheCollectionsContract.java";
    private static final String ORIGINAL_CONTRACT_SHA="0c983a1132ba969d612742f4dfe7c5bcfa76f00bcfb7a8038ba4d7b2074ac488";
    private static final String REFINED_CONTRACT_SHA="0a15f1f7d992c1e5fb371e50e2ad7dad8fe880c259614f1293cfe585e106f875";
    private static final String UNRELATED_PATH="unrelated.txt";
    private static final String UNRELATED_TEXT="untouched\n";
    private record Input(String path,String hash,String text) { }
    private record PomInput(String path,String beforeHash,String afterHash,String before,String after) { }
    private record RefinementInput(String path,String beforeHash,String afterHash,String before,String after) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3ApacheInteropRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Input> targets() throws Exception {
        var records=new ArrayList<Input>();
        for (String row : resource(ROOT+"manifest.tsv").lines().toList()) {
            if(row.startsWith("#")||row.isBlank())continue;
            String[] cells=row.split("\t");assertEquals(4,cells.length);assertEquals("ABSENT",cells[1]);
            String text=resource(ROOT+cells[3]);assertEquals(cells[2],sha(text));
            records.add(new Input(cells[0],cells[2],text));
        }
        assertEquals(3,records.size());return records;
    }
    private static PomInput pomTarget() throws Exception {
        var rows=resource(TEXT_ROOT+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());
        String[] cells=rows.getFirst().split("\t",-1);assertEquals(4,cells.length);
        String before=resource(TEXT_ROOT+"pom.xml.before"),after=resource(TEXT_ROOT+cells[3]);
        assertEquals(cells[1],sha(before));assertEquals(cells[2],sha(after));
        return new PomInput(cells[0],cells[1],cells[2],before,after);
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static RefinementInput refinementTarget() throws Exception {
        var rows=resource(REFINEMENT_ROOT+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());String[] cells=rows.getFirst().split("\t",-1);
        assertEquals(4,cells.length);assertEquals(CONTRACT_PATH,cells[0]);
        assertEquals(ORIGINAL_CONTRACT_SHA,cells[1]);assertEquals(REFINED_CONTRACT_SHA,cells[2]);
        assertNotEquals(cells[1],cells[2]);
        String before=resource(ROOT+"M3ApacheCollectionsContract.java.txt");
        String after=resource(REFINEMENT_ROOT+cells[3]);
        assertEquals(cells[1],sha(before));assertEquals(cells[2],sha(after));
        var receipts=resource(REFINEMENT_ROOT+"source-receipt.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(List.of(
                "before\t"+ORIGINAL_CONTRACT_SHA+"\toriginal/src/test/java/com/m3/collections/interop/M3ApacheCollectionsContract.java",
                "after\t"+REFINED_CONTRACT_SHA+"\tpacket/src/test/java/com/m3/collections/interop/M3ApacheCollectionsContract.java"),receipts);
        return new RefinementInput(cells[0],cells[1],cells[2],before,after);
    }
    private static Recipe activateYaml(String yaml,String name) throws Exception {
        var location=M3ApacheInteropRecipeTest.class.getResource(yaml);assertNotNull(location,yaml);
        try(var stream=M3ApacheInteropRecipeTest.class.getResourceAsStream(yaml)) {
            assertNotNull(stream,yaml);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3ApacheInteropRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(name);
            assertEquals(1,activated.getRecipeList().size());
            assertEquals(name,activated.getRecipeList().getFirst().getName());
            assertTrue(activated.validateAll().stream().allMatch(validation->validation.isValid()));
            return activated;
        }
    }
    private static Recipe activateComposite() throws Exception {
        Recipe activated=activateYaml(YAML,RECIPE_NAME),delivered=activated.getRecipeList().getFirst();
        assertEquals(DISPLAY_NAME,delivered.getDisplayName());assertEquals(2,delivered.getRecipeList().size());
        var javaEngine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().get(0));
        assertEquals("apache-collections-interop",javaEngine.getCrateName());assertFalse(javaEngine.isLst());
        var textEngine=assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class,delivered.getRecipeList().get(1));
        assertEquals("apache-collections-pom",textEngine.getCrateName());return activated;
    }
    private static Recipe activateRefinement() throws Exception {
        Recipe activated=activateYaml(REFINEMENT_YAML,REFINEMENT_NAME);
        Recipe delivered=activated.getRecipeList().getFirst();assertEquals(1,delivered.getRecipeList().size());
        var javaEngine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
        assertEquals("apache-provider-contract-refinement",javaEngine.getCrateName());assertFalse(javaEngine.isLst());
        return activated;
    }
    /** Reparse rendered text with new tree identities and empty markers on every invocation. */
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
    private static List<Result> runComposite(List<SourceFile> source) throws Exception {
        return runRecipe(activateComposite(),source);
    }
    private static List<Result> runRecipe(Recipe recipe,List<SourceFile> source) {
        var errors=new ArrayList<Throwable>();
        var changes=recipe.run(new InMemoryLargeSourceSet(source),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());return changes;
    }
    /** Render every resulting file, including unchanged inputs; no tree or marker crosses replay. */
    private static Map<String,String> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new TreeMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNotNull(change.getAfter());assertInstanceOf(PlainText.class,change.getAfter());
            if(change.getBefore()!=null)assertNotNull(files.remove(path(change.getBefore())));
            assertNull(files.put(path(change.getAfter()),change.getAfter()));
        }
        var rendered=new TreeMap<String,String>();
        files.forEach((name,file)->rendered.put(name,file.printAll()));return rendered;
    }
    private static LinkedHashMap<String,String> compositeState(List<Input> rows,PomInput pom,
            int mask,boolean pomAfter,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        for(int i=0;i<rows.size();i++)if((mask & 1<<i)!=0)input.put(rows.get(i).path,rows.get(i).text);
        input.put(pom.path,pomAfter?pom.after:pom.before);input.put(UNRELATED_PATH,UNRELATED_TEXT);
        if(!reverse)return input;
        var inverted=new LinkedHashMap<String,String>();
        var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
        for(String key:keys)inverted.put(key,input.get(key));return inverted;
    }
    private static void assertCompositeRefuses(List<SourceFile> source,String reason,String target)
            throws Exception {
        assertRecipeRefuses(activateComposite(),source,reason,target);
    }
    private static void assertRecipeRefuses(Recipe recipe,List<SourceFile> source,String reason,String target) {
        var errors=new ArrayList<Throwable>();
        var refusal=assertThrows(IllegalStateException.class,()->recipe.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(refusal.getMessage().contains(reason+": "+target),refusal::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
    }
    private static Map<String,String> apply(Map<String,String> source) throws Exception {
        var input=new ArrayList<SourceFile>();
        source.forEach((p,t)->input.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));
        var errors=new ArrayList<Throwable>();
        var changes=new M3HashPinnedJavaSnapshotRecipe("apache-collections-interop",false)
                .run(new InMemoryLargeSourceSet(input),new InMemoryExecutionContext(errors::add),1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        var output=new TreeMap<String,String>();
        for(Result item:changes) {
            assertNotNull(item.getAfter());
            assertInstanceOf(PlainText.class,item.getAfter());
            assertNull(output.put(path(item.getAfter()),item.getAfter().printAll()));
        }
        return output;
    }
    @Test void allEightJavaInstallationStatesAndFixedPoint() throws Exception {
        var rows=targets();
        for(int mask=0;mask<8;mask++) for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(int i=0;i<3;i++)if((mask & 1<<i)!=0)input.put(rows.get(i).path,rows.get(i).text);
            if(reverse){var inverted=new LinkedHashMap<String,String>();
                var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
                for(String k:keys)inverted.put(k,input.get(k));input=inverted;}
            input.put("unrelated.txt","untouched\n");
            var changed=apply(input);assertEquals(3-Integer.bitCount(mask),changed.size());
            input.putAll(changed);
            for(var row:rows)assertEquals(row.hash,sha(input.get(row.path)));
            assertTrue(apply(input).isEmpty());
        }
    }
    @Test void driftAndDuplicateRefuse() throws Exception {
        var core=new M3HashPinnedJavaSnapshotRecipe("apache-collections-interop",false);
        for(var row:targets()){
            var context=new InMemoryExecutionContext();
            var scan=core.getInitialValue(context);
            core.getScanner(scan).visit(PlainText.builder().sourcePath(Path.of(row.path)).text("drift").build(),context);
            assertThrows(IllegalStateException.class,()->core.generate(scan,context));
            var duplicate=core.getInitialValue(context);
            var source=PlainText.builder().sourcePath(Path.of(row.path)).text(row.text).build();
            core.getScanner(duplicate).visit(source,context);core.getScanner(duplicate).visit(source,context);
            assertThrows(IllegalStateException.class,()->core.generate(duplicate,context));
        }
    }
    @Test void isolatedFindAndReplacePomEditIsExactAndReusedMarkerStable() throws Exception {
        String root="/com/synexia/rewrite/hash-pinned-text/apache-collections-pom/";
        String old=resource(root+"pom.xml.before"),next=resource(root+"pom.xml.after");
        String row=resource(root+"manifest.tsv").strip();
        String[] cols=row.split("\t");assertEquals(4,cols.length);
        assertEquals(sha(old),cols[1]);assertEquals(sha(next),cols[2]);
        String spring="    <dependency><groupId>org.springframework</groupId><artifactId>spring-core</artifactId><version>6.2.0</version></dependency>";
        String replacement=spring+"\n    <dependency><groupId>org.apache.commons</groupId><artifactId>commons-collections4</artifactId><version>4.5.0</version></dependency>";
        assertEquals(1,old.split(java.util.regex.Pattern.quote(spring),-1).length-1);
        Recipe edit=new FindAndReplace(spring,replacement,false,true,false,false,"**/m3/collections/interop/pom.xml");
        Map<String,String> source=Map.of(cols[0],old);
        var file=PlainText.builder().sourcePath(Path.of(cols[0])).text(old).build();
        var changes=edit.run(new InMemoryLargeSourceSet(List.of(file)),new InMemoryExecutionContext(),1)
                .getChangeset().getAllResults();
        assertEquals(1,changes.size());
        assertEquals(next,changes.getFirst().getAfter().printAll());
        assertEquals(sha(next),sha(changes.getFirst().getAfter().printAll()));
        // This isolated operation reuses OpenRewrite markers; fresh composite replay is proved below.
        assertTrue(edit.run(new InMemoryLargeSourceSet(List.of(changes.getFirst().getAfter())),
                new InMemoryExecutionContext(),1).getChangeset().getAllResults().isEmpty());
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.newDocumentBuilder().parse(new ByteArrayInputStream(next.getBytes(StandardCharsets.UTF_8)));
        assertEquals(source.size(),1);
    }
    @Test void generatedJavaHashesAreSealed() throws Exception {
        Map<String,String> results=apply(Map.of());assertEquals(3,results.size());
        for(var row:targets()){assertEquals(row.text,results.get(row.path));assertEquals(row.hash,sha(results.get(row.path)));}
    }
    @Test void deliveredYamlAllThirtyTwoStatesAndFreshParseFixedPoint() throws Exception {
        var rows=targets();var pom=pomTarget();int states=0;
        for(int mask=0;mask<8;mask++)for(boolean pomAfter:List.of(false,true))
                for(boolean reverse:List.of(false,true)) {
            var input=freshSources(compositeState(rows,pom,mask,pomAfter,reverse));
            var changes=runComposite(input);
            assertEquals(3-Integer.bitCount(mask)+(pomAfter?0:1),changes.size());
            var rendered=renderAll(input,changes);assertEquals(5,rendered.size());
            for(var row:rows) {
                assertEquals(row.text,rendered.get(row.path));assertEquals(row.hash,sha(rendered.get(row.path)));
            }
            assertEquals(pom.after,rendered.get(pom.path));assertEquals(pom.afterHash,sha(rendered.get(pom.path)));
            assertEquals(UNRELATED_TEXT,rendered.get(UNRELATED_PATH));
            var reparsed=freshSources(rendered);
            assertTrue(runComposite(reparsed).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(32,states);
    }
    @Test void deliveredYamlRefusesDriftDuplicatesForAllFourTargetsAndMissingPom() throws Exception {
        var rows=targets();var pom=pomTarget();
        var baseline=compositeState(rows,pom,7,true,false);
        var expected=new LinkedHashMap<String,String>();
        for(var row:rows)expected.put(row.path,row.text);expected.put(pom.path,pom.after);
        for(var target:expected.entrySet()) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.getKey(),"drift\n");
            assertCompositeRefuses(freshSources(drift),"source drift",target.getKey());
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.getKey(),target.getValue())));
            assertCompositeRefuses(duplicate,"duplicate target",target.getKey());
        }
        var missing=new LinkedHashMap<>(baseline);assertNotNull(missing.remove(pom.path));
        assertCompositeRefuses(freshSources(missing),"required source missing",pom.path);
    }
    @Test void refinementYamlReplacesExactOriginalAndFreshRefinementIsFixedPoint() throws Exception {
        var target=refinementTarget();
        var baseline=compositeState(targets(),pomTarget(),7,true,false);
        assertEquals(target.before,baseline.get(target.path));
        var source=freshSources(baseline);var changes=runRecipe(activateRefinement(),source);
        assertEquals(1,changes.size());var change=changes.getFirst();assertNotNull(change.getBefore());
        assertEquals(target.path,path(change.getBefore()));assertEquals(target.path,path(change.getAfter()));
        assertEquals(target.before,change.getBefore().printAll());
        assertEquals(target.beforeHash,sha(change.getBefore().printAll()));
        assertEquals(target.after,change.getAfter().printAll());
        assertEquals(target.afterHash,sha(change.getAfter().printAll()));
        var rendered=renderAll(source,changes);var expected=new LinkedHashMap<>(baseline);
        expected.put(target.path,target.after);assertEquals(expected,rendered);
        var reparsed=freshSources(rendered);
        assertTrue(runRecipe(activateRefinement(),reparsed).isEmpty());
        assertEquals(rendered,renderAll(reparsed,List.of()));
        // Historical V1 custody remains a separate transition and rejects its successor oracle.
        assertCompositeRefuses(freshSources(rendered),"source drift",target.path);
    }
    @Test void refinementYamlRefusesDriftDuplicatesAndMissingOriginal() throws Exception {
        var target=refinementTarget();var baseline=compositeState(targets(),pomTarget(),7,true,false);
        var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
        assertRecipeRefuses(activateRefinement(),freshSources(drift),"source drift",target.path);
        var duplicate=new ArrayList<>(freshSources(baseline));
        duplicate.addAll(freshSources(Map.of(target.path,target.before)));
        assertRecipeRefuses(activateRefinement(),duplicate,"duplicate target",target.path);
        var missing=new LinkedHashMap<>(baseline);assertNotNull(missing.remove(target.path));
        assertRecipeRefuses(activateRefinement(),freshSources(missing),"required source missing",target.path);
    }
}
