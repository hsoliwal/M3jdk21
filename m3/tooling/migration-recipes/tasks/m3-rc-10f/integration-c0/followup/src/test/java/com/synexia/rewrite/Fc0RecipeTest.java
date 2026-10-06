// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.openrewrite.Changeset;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Exact follow-up source qualification over the authenticated Ic0 candidate. */
public final class Fc0RecipeTest {
    private static final String SOURCE = "d1ecbd43dbadaf218deec0d98a2ddc23f3a3f45c";
    private static final String TARGET = "c0a14387009aefc7d62bd3268055d526e04f9074";
    private static final String OWNER = "8015207ce502d22955f7f77697177e5939fa1e25be7cd74be89c639335ae48e6";
    private static final String NOTE = "unrelated/keep.txt";
    private static final String NOTE_TEXT = "Unrelated content must remain byte-identical.\n";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode QUALIFICATION = json(read("/qualification.json"));
    private record Target(String path, String beforeHash, String afterHash, String before, String after) { }
    private record Fixture(List<Target> targets, Map<String, String> before, Map<String, String> after) { }
    private record Replay(Map<String, String> output, String patch) { }
    private record Composition(List<Map<String,Object>> reports, Map<String,String> patches, Replay forward) { }
    private static JsonNode json(String value) {
        try { return JSON.readTree(value); } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static String encoded(Object value) {
        try { return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n"; }
        catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static int count(String key) { return QUALIFICATION.get("counts").get(key).asInt(); }
    private static List<String> familyIds() {
        List<String> ids = new ArrayList<>(); for (JsonNode row : QUALIFICATION.get("families")) ids.add(row.get("id").asText());
        return List.copyOf(ids);
    }
    private static JsonNode family(String id) {
        for (JsonNode row : QUALIFICATION.get("families")) if (id.equals(row.get("id").asText())) return row;
        throw new IllegalArgumentException(id);
    }
    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>(); for (JsonNode value : node) values.add(value.asText()); return List.copyOf(values);
    }
    @Test void closedFamiliesAndResourcePins() {
        assertEquals("synexia.fc0.qualification/1", QUALIFICATION.get("schema").asText());
        assertEquals(SOURCE, QUALIFICATION.get("sourceCommit").asText()); assertEquals(TARGET, QUALIFICATION.get("targetCommit").asText());
        assertEquals(OWNER, QUALIFICATION.get("ownerSHA256").asText());
        assertEquals(4, familyIds().size()); assertEquals(4, Set.copyOf(familyIds()).size());
        int targets=0, replacements=0, states=0, pure=0;
        Set<String> paths=new java.util.HashSet<>();
        for (String id : familyIds()) {
            Fixture f=fixture(id); assertTrue(f.targets().size()>=1 && f.targets().size()<=7);
            targets+=f.targets().size(); states+=1<<f.targets().size();
            int r=(int)f.targets().stream().filter(t->t.before()!=null).count(); replacements+=r; if(r==0)pure++;
            for(Target t:f.targets())assertTrue(paths.add(t.path()),"disjoint family target required");
            assertEquals(family(id).get("yamlSHA256").asText(),sha(read("/"+family(id).get("yamlPath").asText())));
        }
        assertEquals(15,targets);assertEquals(12,replacements);assertEquals(3,targets-replacements);
        assertEquals(count("targets"),targets);assertEquals(count("replacements"),replacements);assertEquals(count("additions"),targets-replacements);
        assertEquals(count("families"),familyIds().size());assertEquals(count("mixedStates"),states);
        assertEquals(count("initialRefusals"),3*targets+2*replacements+2*(familyIds().size()-pure));
        assertEquals(count("postScanRefusals"),2*targets);assertEquals(count("pureAdditionInputs"),2*pure);
        assertEquals(QUALIFICATION.get("compositionOrdersSHA256").asText(),sha(read("/composition-orders.json")));
        assertEquals(QUALIFICATION.get("compositionYamlSHA256").asText(),sha(read("/META-INF/rewrite/fc0-orders.yml")));
        JsonNode orders=json(read("/composition-orders.json"));assertEquals(QUALIFICATION.get("orders"),orders.get("orders"));
        assertEquals(SOURCE,orders.get("sourceCommit").asText());assertEquals(TARGET,orders.get("targetCommit").asText());
        assertEquals(2,orders.get("orders").size());assertEquals(familyIds(),strings(orders.get("orders").get(0).get("families")));
        List<String> reversed=new ArrayList<>(familyIds());Collections.reverse(reversed);
        assertEquals(reversed,strings(orders.get("orders").get(1).get("families")));
    }
    private static Fixture fixture(String id) {
        JsonNode f=family(id);String path=f.get("manifestPath").asText();String manifest=read("/"+path);
        assertEquals(f.get("manifestSHA256").asText(),sha(manifest));String root=path.substring(0,path.lastIndexOf('/')+1);
        List<Target> targets=new ArrayList<>();Map<String,String> before=new TreeMap<>(),after=new TreeMap<>();String previous="";int index=0;
        for(String line:manifest.lines().toList()) {
            String[] fields=line.split("\t",-1);assertEquals(4,fields.length);assertTrue(previous.compareTo(fields[0])<0);
            JsonNode expected=f.get("targets").get(index++);assertEquals(expected.get("path").asText(),fields[0]);
            assertEquals(expected.get("beforeSHA256").asText(),fields[1]);assertEquals(expected.get("afterSHA256").asText(),fields[2]);
            String first=fields[1].equals("ABSENT")?null:read("/before/"+fields[0]);String last=read("/"+root+fields[3]);
            assertEquals(expected.get("operation").asText(),first==null?"ADD":"REPLACE");
            if(first!=null){assertEquals(fields[1],sha(first));assertEquals(expected.get("beforeBytes").asInt(),first.getBytes(StandardCharsets.UTF_8).length);assertNull(before.put(fields[0],first));}
            else assertTrue(expected.get("beforeBytes").isNull());
            assertEquals(fields[2],sha(last));assertEquals(expected.get("afterBytes").asInt(),last.getBytes(StandardCharsets.UTF_8).length);
            assertNotEquals(fields[1],fields[2]);targets.add(new Target(fields[0],fields[1],fields[2],first,last));assertNull(after.put(fields[0],last));previous=fields[0];
        }
        assertEquals(f.get("targets").size(),index);before.put(NOTE,NOTE_TEXT);after.put(NOTE,NOTE_TEXT);
        return new Fixture(List.copyOf(targets),Map.copyOf(before),Map.copyOf(after));
    }
    private static Fixture union() {
        List<Target> targets=new ArrayList<>();Map<String,String> before=new TreeMap<>(),after=new TreeMap<>();
        for(String id:familyIds()){Fixture f=fixture(id);targets.addAll(f.targets());before.putAll(f.before());after.putAll(f.after());}
        targets.sort(Comparator.comparing(Target::path));assertEquals(15,targets.size());assertEquals(16,after.size());
        return new Fixture(List.copyOf(targets),Map.copyOf(before),Map.copyOf(after));
    }
    private static YamlResourceLoader yaml(String path) {
        return new YamlResourceLoader(new ByteArrayInputStream(read("/"+path).getBytes(StandardCharsets.UTF_8)),URI.create("classpath:/"+path),new Properties());
    }
    private static Recipe named(String id) {
        JsonNode f=family(id);Recipe recipe=Environment.builder().load(yaml(f.get("yamlPath").asText())).build().activateRecipes(f.get("namedRecipe").asText());
        assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));return recipe;
    }
    private static Recipe composite(String name) {
        Environment.Builder builder=Environment.builder();for(String id:familyIds())builder.load(yaml(family(id).get("yamlPath").asText()));
        Recipe recipe=builder.load(yaml("META-INF/rewrite/fc0-orders.yml")).build().activateRecipes(name);
        assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));return recipe;
    }
    private static void mixed(String id) {
        Fixture f=fixture(id);int states=1<<f.targets().size();
        for(int mask=0;mask<states;mask++) {
            Map<String,String> sources=new TreeMap<>();sources.put(NOTE,NOTE_TEXT);
            for(int bit=0;bit<f.targets().size();bit++) {Target t=f.targets().get(bit);String value=(mask&(1<<bit))==0?t.before():t.after();if(value!=null)sources.put(t.path(),value);}
            Replay result=replay(named(id),f,plain(sources),f.targets().size()-Integer.bitCount(mask));
            replay(named(id),f,plain(result.output()),0);
        }
    }
    @Test void namedRecipesAndFixedPoint() {
        for(String id:familyIds()) {
            Fixture f=fixture(id);Replay first=replay(named(id),f,plain(f.before()),f.targets().size());
            assertEquals(first,replay(named(id),f,plain(f.before()),f.targets().size()));assertFalse(first.patch().isBlank());
            replay(named(id),f,plain(f.after()),0);List<SourceFile> reversed=plain(f.before());Collections.reverse(reversed);
            assertEquals(first.output(),replay(named(id),f,reversed,f.targets().size()).output());
        }
    }
    @Test void initialRefusals() {
        List<Throwable> parserErrors=new ArrayList<>();SourceFile typed=JavaParser.fromJavaVersion().build().parse(new InMemoryExecutionContext(parserErrors::add),"class TypedRefusal {}\n").findFirst().orElseThrow();
        assertTrue(parserErrors.isEmpty());assertInstanceOf(J.CompilationUnit.class,typed);int total=0;
        for(String id:familyIds()) {
            Fixture f=fixture(id);List<SourceFile> original=plain(f.before());boolean hasReplacement=false;
            for(Target t:f.targets()) {
                refused(named(id),replacing(original,text(t.path(),t.after()+"\n# occupied drift\n")));
                List<SourceFile> duplicate=new ArrayList<>(without(original,t.path()));duplicate.add(text(t.path(),t.after()));duplicate.add(text(t.path(),t.after()));refused(named(id),duplicate);
                refused(named(id),replacing(original,typed.withSourcePath(Path.of(t.path()))));total+=3;
                if(t.before()!=null){hasReplacement=true;refused(named(id),without(original,t.path()));refused(named(id),replacing(original,text("unrelated/wrong.txt",t.before()),t.path()));total+=2;}
            }
            if(hasReplacement){refused(named(id),List.of());refused(named(id),List.of(text(NOTE,NOTE_TEXT)));total+=2;}
        }
        assertEquals(count("initialRefusals"),total);
    }
    @Test void pureAdditionInputs() {
        int total=0;
        for(String id:familyIds()) {
            Fixture f=fixture(id);if(f.targets().stream().anyMatch(t->t.before()!=null))continue;
            Map<String,String> after=new TreeMap<>(f.after());after.remove(NOTE);Fixture empty=new Fixture(f.targets(),Map.of(),Map.copyOf(after));
            Replay generated=replay(named(id),empty,List.of(),f.targets().size());replay(named(id),empty,plain(generated.output()),0);
            replay(named(id),f,List.of(text(NOTE,NOTE_TEXT)),f.targets().size());replay(named(id),f,plain(f.after()),0);total+=2;
        }
        assertEquals(count("pureAdditionInputs"),total);
    }
    @Test void postScanDrift() {
        int total=0;for(String id:familyIds())total+=afterScan(new M3Jdk21HashPinnedTextSnapshotRecipe(family(id).get("crate").asText()),fixture(id));
        assertEquals(count("postScanRefusals"),total);
    }
    private static List<Map<String,Object>> identities(Map<String,String> sources) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(Target t:union().targets()) {
            String value=sources.get(t.path());Map<String,Object> row=new TreeMap<>();row.put("path",t.path());row.put("sha256",value==null?"ABSENT":sha(value));
            row.put("bytes",value==null?null:value.getBytes(StandardCharsets.UTF_8).length);rows.add(row);
        }
        return rows;
    }
    private static String stateSeal(List<Map<String,Object>> rows) {
        StringBuilder b=new StringBuilder("SYNEXIA-FC0-STATE/1\n");for(var row:rows)b.append(row.get("path")).append('\t').append(row.get("sha256")).append('\t').append(row.get("bytes")==null?"ABSENT":row.get("bytes")).append('\n');return sha(b.toString());
    }
    private static Composition compositions() {
        Fixture all=union();List<Map<String,Object>> reports=new ArrayList<>();Map<String,String> patches=new TreeMap<>();Replay forward=null;
        for(JsonNode order:QUALIFICATION.get("orders")) {
            String orderId=order.get("id").asText();Map<String,String> current=all.before();List<Map<String,Object>> steps=new ArrayList<>();StringBuilder orderedPatch=new StringBuilder();int ordinal=0;
            for(String id:strings(order.get("families"))) {
                Fixture local=fixture(id);Map<String,String> expected=new TreeMap<>(current);for(Target t:local.targets())expected.put(t.path(),t.after());
                Fixture step=new Fixture(local.targets(),current,Map.copyOf(expected));List<Map<String,Object>> inputRows=identities(current);
                Replay result=replay(named(id),step,plain(current),local.targets().size());List<Map<String,Object>> outputRows=identities(result.output());
                List<String> changed=all.targets().stream().map(Target::path).filter(path->!java.util.Objects.equals(step.before().get(path),result.output().get(path))).sorted().toList();
                assertEquals(local.targets().stream().map(Target::path).sorted().toList(),changed);
                Replay fixed=replay(named(id),step,plain(result.output()),0);assertEquals("",fixed.patch());assertEquals(result.output(),fixed.output());
                String stem="composition/"+orderId+"/"+String.format(java.util.Locale.ROOT,"%02d",++ordinal)+"-"+id;
                String patchPath=stem+".patch",replayPath=stem+"-replay.patch";assertNull(patches.put(patchPath,result.patch()));assertNull(patches.put(replayPath,fixed.patch()));orderedPatch.append(result.patch());
                Map<String,Object> receipt=new TreeMap<>();receipt.put("ordinal",ordinal);receipt.put("family",id);receipt.put("inputRows",inputRows);receipt.put("outputRows",outputRows);
                receipt.put("inputSeal",stateSeal(inputRows));receipt.put("outputSeal",stateSeal(outputRows));receipt.put("changedPaths",changed);receipt.put("patchPath",patchPath);receipt.put("patchSHA256",sha(result.patch()));
                receipt.put("replayInputSeal",stateSeal(outputRows));receipt.put("replayOutputSeal",stateSeal(identities(fixed.output())));receipt.put("replayChangedPaths",List.of());receipt.put("replayPatchPath",replayPath);receipt.put("replayPatchSHA256",sha(fixed.patch()));steps.add(receipt);current=result.output();
            }
            assertEquals(all.after(),current);Replay namedResult=replay(composite(order.get("recipe").asText()),all,plain(all.before()),15);assertEquals(current,namedResult.output());
            replay(composite(order.get("recipe").asText()),all,plain(current),0);
            Map<String,Object> report=new TreeMap<>();report.put("id",orderId);report.put("recipe",order.get("recipe").asText());report.put("steps",steps);reports.add(report);
            if(orderId.equals("Forward"))forward=new Replay(current,orderedPatch.toString());
        }
        assertNotNull(forward);assertEquals(2,reports.size());assertEquals(16,patches.size());return new Composition(List.copyOf(reports),Map.copyOf(patches),forward);
    }
    @Test void declaredOrderedCompositions() { compositions(); }
    @Test void cwfMixedStates() { mixed("Cwf"); }
    @Test void tqcMixedStates() { mixed("Tqc"); }
    @Test void pdcMixedStates() { mixed("Pdc"); }
    @Test void clrMixedStates() { mixed("Clr"); }
    private static Replay replay(Recipe recipe, Fixture fixture, List<SourceFile> sources, int changes) {
        Map<String, String> original = rendered(sources);
        List<Throwable> errors = new ArrayList<>();
        RecordingSourceSet set = new RecordingSourceSet(new InMemoryLargeSourceSet(sources));
        List<Result> results = recipe.run(set, new InMemoryExecutionContext(errors::add), 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), () -> describe(errors));
        assertEquals(changes, results.size());
        long expectedAdditions = fixture.targets().stream().filter(target -> !original.containsKey(target.path())).count();
        assertEquals(expectedAdditions, set.generated.size());
        assertEquals(fixture.targets().stream().filter(target -> !original.containsKey(target.path())).map(Target::path).sorted().toList(),
                set.generated.stream().map(Fc0RecipeTest::path).sorted().toList());
        if (changes == 0) assertTrue(set.edited.isEmpty(), "fixed point preserves source objects");
        Map<String, String> actual = new TreeMap<>(original);
        for (Result result : results) {
            SourceFile before = result.getBefore();
            SourceFile after = assertNotNullValue(result.getAfter());
            assertInstanceOf(PlainText.class, after);
            if (before == null) {
                assertFalse(original.containsKey(path(after)));
                assertTrue(fixture.targets().stream().anyMatch(target -> target.path().equals(path(after)) && target.before() == null));
            } else {
                assertEquals(path(before), path(after)); assertEquals(before.getId(), after.getId());
                assertSame(source(sources, path(before)), before);
            }
            assertNotEquals(NOTE, path(after));
            assertTrue(fixture.after().containsKey(path(after)));
            actual.put(path(after), after.printAll());
        }
        assertEquals(fixture.after(), actual);
        assertEquals(original, rendered(sources), "scheduler input text remains immutable");
        for (Target target : fixture.targets()) assertEquals(target.afterHash(), sha(actual.get(target.path())));
        StringBuilder patch = new StringBuilder();
        results.stream().sorted(Comparator.comparing(result -> path(result.getAfter())))
                .forEach(result -> patch.append(result.diff()));
        return new Replay(Map.copyOf(actual), patch.toString());
    }

    private static void refused(Recipe recipe, List<SourceFile> input) {
        List<String> before = input.stream().map(SourceFile::printAll).toList();
        RecordingSourceSet set = new RecordingSourceSet(new InMemoryLargeSourceSet(input));
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> recipe.run(set, new InMemoryExecutionContext(ignored -> { }), 1));
        assertTrue(hasIllegalState(failure), failure.toString());
        assertTrue(set.generated.isEmpty());
        assertTrue(set.edited.isEmpty());
        assertTrue(set.getChangeset().getAllResults().isEmpty());
        assertEquals(before, input.stream().map(SourceFile::printAll).toList());
    }

    private static <A> int afterScan(ScanningRecipe<A> recipe, Fixture fixture) {
        int count = 0;
        for (Target target : fixture.targets()) {
            List<String> replacements = target.before() == null
                    ? List.of("occupied after scan", target.after() + "\n# post-scan drift\n")
                    : List.of(target.after(), target.before() + "\n# post-scan drift\n");
            for (String replacement : replacements) {
                ExecutionContext context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
                A inventory = recipe.getInitialValue(context);
                for (SourceFile source : plain(fixture.before())) recipe.getScanner(inventory).visit(source, context);
                assertEquals(fixture.targets().stream().filter(entry -> entry.before() == null).count(), recipe.generate(inventory, context).size());
                RuntimeException failure = assertThrows(RuntimeException.class,
                        () -> recipe.getVisitor(inventory).visit(text(target.path(), replacement), context));
                assertTrue(hasIllegalState(failure), failure.toString());
                count++;
            }
        }
        return count;
    }

    private static List<SourceFile> plain(Map<String, String> sources) {
        List<SourceFile> result = new ArrayList<>();
        new TreeMap<>(sources).forEach((path, value) -> result.add(text(path, value)));
        return result;
    }
    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
    private static SourceFile source(List<SourceFile> sources, String path) {
        return sources.stream().filter(source -> path(source).equals(path)).findFirst().orElseThrow();
    }
    private static List<SourceFile> without(List<SourceFile> sources, String path) {
        return sources.stream().filter(source -> !path(source).equals(path)).toList();
    }
    private static List<SourceFile> replacing(List<SourceFile> sources, SourceFile replacement) {
        return replacing(sources, replacement, path(replacement));
    }
    private static List<SourceFile> replacing(List<SourceFile> sources, SourceFile replacement, String removed) {
        List<SourceFile> result = new ArrayList<>(without(sources, removed)); result.add(replacement); return result;
    }
    private static Map<String, String> rendered(List<SourceFile> sources) {
        Map<String, String> values = new TreeMap<>();
        for (SourceFile source : sources) assertNull(values.put(path(source), source.printAll()));
        return values;
    }
    private static String path(SourceFile source) { return source.getSourcePath().toString().replace('\\', '/'); }
    private static SourceFile assertNotNullValue(SourceFile value) { assertNotNull(value); return value; }
    private static boolean hasIllegalState(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalStateException) return true;
        }
        return false;
    }
    private static String read(String path) {
        try (var stream = Fc0RecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path); return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static String describe(List<Throwable> errors) {
        StringWriter text = new StringWriter();
        try (PrintWriter writer = new PrintWriter(text)) {
            for (Throwable error : errors) error.printStackTrace(writer);
        }
        return text.toString();
    }
    private static String sha(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }

    /** Observes the real scheduler without replacing its source-set implementation. */
    private static final class RecordingSourceSet implements LargeSourceSet {
        private LargeSourceSet current;
        private final List<SourceFile> edited = new ArrayList<>();
        private final List<SourceFile> generated = new ArrayList<>();
        private RecordingSourceSet(LargeSourceSet current) { this.current = current; }
        @Override public void beforeCycle(boolean lastCycle) { current.beforeCycle(lastCycle); }
        @Override public void setRecipe(List<Recipe> recipes) { current.setRecipe(recipes); }
        @Override public LargeSourceSet edit(UnaryOperator<SourceFile> edit) {
            current = current.edit(before -> {
                SourceFile after = edit.apply(before);
                if (path(before).equals(NOTE)) assertSame(before, after);
                if (after != null && after != before) edited.add(after);
                return after;
            }); return this;
        }
        @Override public LargeSourceSet generate(Collection<? extends SourceFile> sources) {
            generated.addAll(sources); current = current.generate(sources); return this;
        }
        @Override public void afterCycle(boolean lastCycle) { current.afterCycle(lastCycle); }
        @Override public Changeset getChangeset() { return current.getChangeset(); }
        @Override public SourceFile getBefore(Path path) { return current.getBefore(path); }
    }


    private static String materialize(Composition composition, Path output) throws IOException {
        Path root=output.toAbsolutePath().normalize();assertFalse(Files.exists(root,LinkOption.NOFOLLOW_LINKS));Map<String,String> files=new TreeMap<>(composition.forward().output());
        assertEquals(NOTE_TEXT,files.remove(NOTE));assertEquals(15,files.size());List<Map<String,Object>> rows=new ArrayList<>();StringBuilder sealInput=new StringBuilder("SYNEXIA-FC0-OUTPUT/1\n");
        for(Target t:union().targets()) {
            String value=files.get(t.path());assertEquals(t.afterHash(),sha(value));byte[] bytes=value.getBytes(StandardCharsets.UTF_8);Path path=root.resolve(t.path()).normalize();assertTrue(path.startsWith(root));Files.createDirectories(path.getParent());Files.write(path,bytes);
            Map<String,Object> row=new TreeMap<>();row.put("path",t.path());row.put("sha256",sha(value));row.put("bytes",bytes.length);row.put("operation",t.before()==null?"ADD":"REPLACE");row.put("beforeSHA256",t.beforeHash());row.put("beforeBytes",t.before()==null?null:t.before().getBytes(StandardCharsets.UTF_8).length);rows.add(row);
            sealInput.append(t.path()).append('\t').append(sha(value)).append('\t').append(bytes.length).append('\n');
        }
        for(var e:composition.patches().entrySet()){Path path=root.resolve(e.getKey()).normalize();assertTrue(path.startsWith(root));Files.createDirectories(path.getParent());Files.writeString(path,e.getValue(),StandardCharsets.UTF_8);}
        String seal=sha(sealInput.toString());Map<String,Object> report=new TreeMap<>();report.put("schema","synexia.fc0.output/1");report.put("sourceCommit",SOURCE);report.put("targetCommit",TARGET);report.put("outputs",rows);report.put("outputSeal",seal);
        QUALIFICATION.get("counts").fields().forEachRemaining(e->{if(!e.getKey().equals("orderedCompositions"))report.put(e.getKey(),e.getValue().asInt());});report.put("qualificationSHA256",sha(read("/qualification.json")));report.put("compositionOrdersSHA256",sha(read("/composition-orders.json")));report.put("orderedCompositions",composition.reports());
        for(String flag:List.of("consumerQualified","crossFamilyAtomicity","crossFamilyCartesianProductEnumerated","unlistedOrderedCompositionsQualified","canonicalProductionApplied","strictCanonicalAdmission"))report.put(flag,false);
        Files.writeString(root.resolve("OUTPUT.json"),encoded(report),StandardCharsets.UTF_8);Files.writeString(root.resolve("candidate.patch"),composition.forward().patch(),StandardCharsets.UTF_8);return seal;
    }
    public static void main(String[] args) throws Exception {
        String output=System.getProperty("fc0.output");if(output==null||output.isBlank())throw new IllegalArgumentException("fc0.output is required");
        Fc0RecipeTest test=new Fc0RecipeTest();test.closedFamiliesAndResourcePins();test.namedRecipesAndFixedPoint();for(String id:familyIds())mixed(id);test.initialRefusals();test.postScanDrift();test.pureAdditionInputs();
        String seal=materialize(compositions(),Path.of(output));
        System.out.println("FC0_VERIFIED targets=15 mixedStates=92 initialRefusals=77 postScanRefusals=30 outputSeal="+seal);
    }
}
