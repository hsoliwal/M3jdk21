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
import java.util.UUID;
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
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** One exact existing SDK transition; runtime behavior and the source-binding guard are separate. */
final class M3SdkBindingSuccessorRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/sdk-binding-successor-v3/";
    private static final String YAML="/META-INF/rewrite/m3-sdk-binding-successor-v3.yml";
    private static final String NAME="com.synexia.m3.SdkBindingSuccessor20261018";
    private static final String TARGET="m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java";
    private static final String BEFORE_SHA="258a4940136bce3998d7c2d717a6f5512f1412fd874580f242d2185be129b85a";
    private static final String AFTER_SHA="f805ff3b86349480dd575840326eb015e5626b5eb4f0f61deccc73f83e9e1452";
    private static final String ORIGINAL_SHA="1fb66d3c334f4087bfcd2be7502dad25cfe6b18ae7c3a6d968cb44a4612e41e8";
    private static final int BEFORE_BYTES=7020;
    private static final int AFTER_BYTES=7807;
    private static final int ORIGINAL_BYTES=7589;
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";

    private record Images(String before,String after,String original) { }
    private record Execution(Recipe recipe,List<Result> changes,Observation observation) { }
    private static final class Observation {
        private int edited;
        private int generated;
    }
    /** The public Marker contract prints empty text by default; supplied metadata stays observable. */
    private record PreservedMarker(UUID id) implements Marker {
        @Override public UUID getId(){return id;}
        @SuppressWarnings("unchecked")
        @Override public <M extends Marker> M withId(UUID next){return (M)new PreservedMarker(next);}
    }
    /** Observe this supplied installer batch only, not arbitrary composed-recipe transactions. */
    private static final class ObservedSources extends InMemoryLargeSourceSet {
        private final Observation observation;
        private ObservedSources(List<SourceFile> files,Observation observation) {
            super(files);this.observation=observation;
        }
        @Override public LargeSourceSet edit(UnaryOperator<SourceFile> map) {
            return super.edit(before->{
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
        try(var stream=M3SdkBindingSuccessorRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static Images images() throws Exception {
        var rows=resource(ROOT+"manifest.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var receipts=resource(ROOT+"source-receipt.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var engines=resource(ROOT+"engine-receipt.tsv").lines()
                .filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(1,rows.size());assertEquals(1,receipts.size());assertEquals(1,engines.size());
        assertArrayEquals(new String[]{TARGET,BEFORE_SHA,AFTER_SHA,"M3CollectionSdk.java.txt"},
                rows.getFirst().split("\t",-1));
        assertArrayEquals(new String[]{TARGET,AFTER_SHA,Integer.toString(AFTER_BYTES)},
                receipts.getFirst().split("\t",-1));
        assertArrayEquals(new String[]{"engine-src/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java",
                "d58ca50dfaeb60f581b64f43545e688aca5c6530",
                "3fc6e77a1394e292747d689a4a42a328b496000865aa390afe05d5571febd599","13805"},
                engines.getFirst().split("\t",-1));
        String before=resource(ROOT+"M3CollectionSdk.java.before.txt");
        String after=resource(ROOT+"M3CollectionSdk.java.txt");
        String original=resource(ROOT+"M3CollectionSdk.java.original-7589.txt");
        assertEquals(BEFORE_SHA,sha(before));assertEquals(AFTER_SHA,sha(after));assertEquals(ORIGINAL_SHA,sha(original));
        assertEquals(BEFORE_BYTES,before.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(AFTER_BYTES,after.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(ORIGINAL_BYTES,original.getBytes(StandardCharsets.UTF_8).length);
        assertNotEquals(before,after);assertNotEquals(before,original);assertNotEquals(after,original);
        return new Images(before,after,original);
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe engine(Recipe recipe) {
        return recipe.getRecipeList().getFirst().getRecipeList().getFirst();
    }
    private static Recipe activate() throws Exception {
        var location=M3SdkBindingSuccessorRecipeTest.class.getResource(YAML);
        assertNotNull(location,YAML);
        try(var stream=M3SdkBindingSuccessorRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe recipe=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3SdkBindingSuccessorRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,recipe.getRecipeList().size());
            var delivered=recipe.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var selected=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,engine(recipe));
            assertEquals("sdk-binding-successor-v3",selected.getCrateName());assertFalse(selected.isLst());
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
        assertTrue(errors.isEmpty(),()->errors.toString());assertEquals(texts.size(),parsed.size());
        var order=new ArrayList<>(texts.keySet());
        for(int i=0;i<parsed.size();i++) {
            var file=parsed.get(i);
            assertInstanceOf(PlainText.class,file);assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(order.get(i),path(file));assertEquals(texts.get(path(file)),file.printAll());
        }
        return parsed;
    }
    private static List<SourceFile> withSuppliedMarkers(List<SourceFile> source) {
        var marked=new ArrayList<SourceFile>();
        for(var before:source) {
            SourceFile after=before.withMarkers(before.getMarkers().add(new PreservedMarker(UUID.randomUUID())));
            assertEquals(before.printAll(),after.printAll());assertEquals(before.getId(),after.getId());
            assertEquals(path(before),path(after));assertEquals(1,after.getMarkers().getMarkers().size());
            marked.add(after);
        }
        return marked;
    }
    private static Map<String,String> state(String target,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        if(reverse){input.put(UNRELATED,UNTOUCHED);input.put(TARGET,target);}
        else{input.put(TARGET,target);input.put(UNRELATED,UNTOUCHED);}
        return input;
    }
    private static Execution run(List<SourceFile> source) throws Exception {
        Recipe recipe=activate();var errors=new ArrayList<Throwable>();var observation=new Observation();
        var changes=recipe.run(new ObservedSources(source,observation),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        return new Execution(recipe,changes,observation);
    }
    private static Map<String,SourceFile> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new LinkedHashMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNotNull(change.getBefore());assertNotNull(change.getAfter());
            assertEquals(TARGET,path(change.getBefore()));assertEquals(TARGET,path(change.getAfter()));
            assertInstanceOf(PlainText.class,change.getAfter());
            var before=files.get(TARGET);assertNotNull(before);
            assertEquals(before.printAll(),change.getBefore().printAll());
            assertEquals(before.getId(),change.getAfter().getId());
            var supplied=before.getMarkers().getMarkers();
            var retained=change.getAfter().getMarkers().getMarkers();
            assertEquals(supplied.size()+1,retained.size());
            for(var marker:supplied) {
                var matching=retained.stream().filter(item->item.getId().equals(marker.getId())).toList();
                assertEquals(1,matching.size());assertSame(marker,matching.getFirst());
            }
            assertEquals(1L,retained.stream().filter(RecipesThatMadeChanges.class::isInstance).count());
            assertTrue(retained.stream().allMatch(marker->marker instanceof RecipesThatMadeChanges
                    ||supplied.stream().anyMatch(original->original==marker)));
            assertSame(before,files.put(TARGET,change.getAfter()));
        }
        assertEquals(source.stream().map(M3SdkBindingSuccessorRecipeTest::path).toList(),new ArrayList<>(files.keySet()));
        return files;
    }
    private static Map<String,String> printed(Map<String,SourceFile> source) {
        var texts=new LinkedHashMap<String,String>();source.forEach((name,file)->texts.put(name,file.printAll()));
        return texts;
    }
    private static void refuses(List<SourceFile> source,String reason) throws Exception {
        var originals=List.copyOf(source);
        var text=source.stream().map(SourceFile::printAll).toList();
        var paths=source.stream().map(M3SdkBindingSuccessorRecipeTest::path).toList();
        var ids=source.stream().map(SourceFile::getId).toList();
        var markers=source.stream().map(SourceFile::getMarkers).toList();
        var observation=new Observation();var supplied=new ObservedSources(source,observation);
        var errors=new ArrayList<Throwable>();Recipe recipe=activate();
        var failure=assertThrows(IllegalStateException.class,()->recipe.run(supplied,
                new InMemoryExecutionContext(errors::add),1));
        assertTrue(failure.getMessage().contains(reason+": "+TARGET),failure::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
        assertEquals(0,observation.generated,"no additions before refusal");
        assertEquals(0,observation.edited,"no replacement/deletion before refusal");
        assertTrue(supplied.getChangeset().getAllResults().isEmpty(),"no supplied-batch outputs");
        assertEquals(originals.size(),source.size());
        for(int i=0;i<source.size();i++) {
            assertSame(originals.get(i),source.get(i));assertEquals(paths.get(i),path(source.get(i)));
            assertEquals(text.get(i),source.get(i).printAll());assertEquals(ids.get(i),source.get(i).getId());
            assertSame(markers.get(i),source.get(i).getMarkers());
        }
    }
    @Test void exactBeforeAfterBothOrdersAndFreshMarkerFreeNewActivationFixedPoints() throws Exception {
        var images=images();int valid=0;
        for(boolean alreadyAfter:List.of(false,true))for(boolean reverse:List.of(false,true)) {
            String input=alreadyAfter?images.after:images.before;
            var source=withSuppliedMarkers(freshSources(state(input,reverse)));
            var result=run(source);
            assertEquals(alreadyAfter?0:1,result.changes.size());
            assertEquals(0,result.observation.generated);
            assertEquals(alreadyAfter?0:1,result.observation.edited);
            var rendered=renderAll(source,result.changes);
            assertEquals(2,rendered.size());
            assertEquals(images.after,rendered.get(TARGET).printAll());
            assertEquals(AFTER_SHA,sha(rendered.get(TARGET).printAll()));
            assertEquals(AFTER_BYTES,rendered.get(TARGET).printAll().getBytes(StandardCharsets.UTF_8).length);
            assertEquals(UNTOUCHED,rendered.get(UNRELATED).printAll());
            for(var file:source) {
                if(alreadyAfter||!path(file).equals(TARGET))assertSame(file,rendered.get(path(file)));
            }
            var reparsed=freshSources(printed(rendered));var fixed=run(reparsed);
            assertNotSame(result.recipe,fixed.recipe);assertNotSame(engine(result.recipe),engine(fixed.recipe));
            assertTrue(fixed.changes.isEmpty());assertEquals(0,fixed.observation.generated);
            assertEquals(0,fixed.observation.edited);
            var delivered=renderAll(reparsed,fixed.changes);
            assertEquals(printed(rendered),printed(delivered));
            for(var file:reparsed)assertSame(file,delivered.get(path(file)));
            valid++;
        }
        assertEquals(4,valid);
    }
    @Test void missingDriftOriginal7589AndAllFourBeforeAfterDuplicateOrdersRefuseBeforeOutput() throws Exception {
        var images=images();int refused=0;
        refuses(withSuppliedMarkers(freshSources(Map.of(UNRELATED,UNTOUCHED))),"required source missing");refused++;
        refuses(withSuppliedMarkers(freshSources(state(images.after+"\n// unqualified drift\n",false))),"source drift");refused++;
        refuses(withSuppliedMarkers(freshSources(state(images.original,true))),"source drift");refused++;
        for(boolean firstAfter:List.of(false,true))for(boolean secondAfter:List.of(false,true)) {
            var duplicates=new ArrayList<SourceFile>();
            duplicates.addAll(withSuppliedMarkers(freshSources(Map.of(TARGET,firstAfter?images.after:images.before))));
            duplicates.addAll(withSuppliedMarkers(freshSources(Map.of(UNRELATED,UNTOUCHED))));
            duplicates.addAll(withSuppliedMarkers(freshSources(Map.of(TARGET,secondAfter?images.after:images.before))));
            refuses(duplicates,"duplicate target");refused++;
        }
        assertEquals(7,refused);
    }
}
