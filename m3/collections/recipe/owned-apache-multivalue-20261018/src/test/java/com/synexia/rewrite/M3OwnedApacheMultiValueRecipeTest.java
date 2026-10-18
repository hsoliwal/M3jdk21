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

/** Custody of the exact owned implementation and its scoped common/dense contract oracles.
 * Receiving-source proof does not admit full Android or JRE provider parity. */
final class M3OwnedApacheMultiValueRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/owned-apache-multivalue/";
    private static final String YAML="/META-INF/rewrite/m3-owned-apache-multivalue.yml";
    private static final String NAME="com.synexia.m3.OwnedApacheMultiValue20261018";
    private static final int COUNT=32;
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String hash,int bytes,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedApacheMultiValueRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var rows=resource(ROOT+"manifest.tsv").lines().filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        var receipts=resource(ROOT+"source-receipt.tsv").lines().filter(row->!row.isBlank()&&!row.startsWith("#")).toList();
        assertEquals(COUNT,rows.size());assertEquals(COUNT,receipts.size());
        var hashes=List.of(            "bdd6051aecad2ff0a2c2ce7b788aeae51331e5dcef2d5c9f38270520bcdfbfc1",
            "86689d4c9f7cf1362b4d2d7fcc37f5e70f8d9a06b71395128ae3dabdd7454177",
            "b6d1c925716f44578706b5a5786f23ecab1eba652a8c6dea5f02fdf67695fcbd",
            "b4b6e3255f848b3868e2ccc73a8033601159e2d35ffce8bf0e0ebb28413645ff",
            "10eedc073af073a512bfd40720158d9eb5c48a52340c446cd4c21f95fe90d17f",
            "a177d9871c8f7fefdffcce30deaf6b1222009fee8fcd7cee021436db77896e6a",
            "53cf346e8fcef0b587d817664bee332e0fd93f0e9ecc8507a03acb4db7c5eef7",
            "a269da7e121cf24eb581d25692da017995b3e6b4eccdea7707a99b800bf6e090",
            "5868cd6a6786c9bb79dcb26056d3d63141b5e8fbfb9d3926ffbfc8ec5b958c76",
            "bd12713e1a455a2aa421ba339d7188b0d1f20df6ef9dabacf160be6bc90b192f",
            "4bcaf5fc528f36d873707385f2742ec58b68352b36271f26838f1cf939b55310",
            "a09f89f1e870a558d2fe8c27785ba25c3151fb4e2f77ea2667d15a02e7fd743b",
            "cc4adaf111c2dbf56c6cb74bfb69bfe73406e9b0a26e67e041067235c604bac9",
            "850ee586a0b7632a3084c0bb1b79d9d60ef673e2176b4d437252f2cff4327da1",
            "af72d5f7b48eff47c8e8b4c509b9b5fce5c9c78f44d6b0f9f3bfb826b0f98799",
            "0888237d6a62e668b7f858769b9474454be28d1648667f1cbb078ffb10339b07",
            "23098870483d7cd41437a18331a565bdf062f45428c64bfd421fe80e0c128af6",
            "08488af8201dffab0ed06986fad120339c03d5f688ac870510995ab40ee38bd2",
            "930ec303ac1a447af8a8915b7d8c7e62647d1894f01f5091be47bd9191e7e9ed",
            "4b85ca639e63a53d254b5a82efa0f127303ce5449e406deb4d2620c825f8fe90",
            "7a8db0b619100dc3f6401b711bc9e006079a9c58ea0c22cd7a154f8639de9894",
            "16972945a5775f7f1703a22b47ca72483bc8638f9afb97204d6765028d216931",
            "084193b77b70454d0c0bf5a7efe456816253a90b5ae94929384300d1273377b7",
            "471da0c0cf8ce80a50c0bcbb5266ea1c1df7d0c0dc75739ea6f39caaa42dc464",
            "091a983eab4f61e2ce1148cebb12f3cc10b3107cd6d068687f1a37b3526c1557",
            "1df26a4061354f143fd8ec81c819427941df4886e0927e5765fff6e3743d1ca4",
            "0f516ccc2615ee38cd9a4a74b7fba45a2a4a27e845d877a39b16cd9149e2bc53",
            "571a775a9ef45e13e1359dd8686a143aa016e927a42c2fae17ed0d156fbf5d73",
            "6278d11e5ecff72826de09ce1e9f7b778f0ce10403ed6ffcd3fdec2385092274",
            "134cebe67bd4576d6ac34fbb783157dc69612572a47a4c74e63c59f3a1962eef",
            "fefbef946c8fadf5cdce089d71de0146c36469f6546f6627957fc4374b0df0cb",
            "02160fe717e1c9b4e430c2a5d03e30b14487e8e8f6470161ec71d8c0efeaacdf");var lengths=List.of(8715,33514,4278,6209,3766,2255,1795,2836,3099,14604,3085,2653,2302,2281,1796,2705,1976,10170,1608,5936,2203,1844,4685,2838,2793,5709,3686,4327,5675,5335,39782,14524);
        var targets=new ArrayList<Target>();String previous="";
        for(int i=0;i<COUNT;i++) {
            String[] cells=rows.get(i).split("\t",-1);String[] receipt=receipts.get(i).split("\t",-1);
            assertEquals(4,cells.length);assertEquals(3,receipt.length);
            assertEquals("ABSENT",cells[1]);assertEquals(cells[0],receipt[0]);
            assertEquals(hashes.get(i),cells[2]);assertEquals(cells[2],receipt[1]);
            assertEquals(lengths.get(i).intValue(),Integer.parseInt(receipt[2]));
            assertTrue(previous.compareTo(cells[0])<0);previous=cells[0];
            String text=resource(ROOT+cells[3]);assertEquals(cells[2],sha(text));
            assertEquals(lengths.get(i).intValue(),text.getBytes(StandardCharsets.UTF_8).length);
            targets.add(new Target(cells[0],cells[2],lengths.get(i),text));
        }
        return targets;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3OwnedApacheMultiValueRecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3OwnedApacheMultiValueRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedApacheMultiValueRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());var delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("owned-apache-multivalue",engine.getCrateName());assertFalse(engine.isLst());
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
    @Test void allAbsentPresentSingleMissingBothOrdersAndFreshFixedPoints() throws Exception {
        var rows=targets();int states=0;
        for(int omitted=-2;omitted<COUNT;omitted++)for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(int i=0;i<COUNT;i++)if(omitted!=-2&&i!=omitted)input.put(rows.get(i).path,rows.get(i).text);
            input.put(UNRELATED,UNTOUCHED);
            if(reverse) {
                var reversed=new LinkedHashMap<String,String>();
                var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
                for(String key:keys)reversed.put(key,input.get(key));input=reversed;
            }
            var source=freshSources(input);var changes=run(source);
            assertEquals(omitted==-2?COUNT:omitted==-1?0:1,changes.size());
            var rendered=renderAll(source,changes);assertEquals(COUNT+1,rendered.size());
            for(var target:rows) {
                assertEquals(target.text,rendered.get(target.path));assertEquals(target.hash,sha(rendered.get(target.path)));
            }
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));
            assertTrue(run(freshSources(rendered)).isEmpty());states++;
        }
        assertEquals(68,states); // Finite omission/order matrix, not all 2^32 subsets.
    }
    @Test void everyTargetDriftAndDuplicateRefusedBeforeAnyOutput() throws Exception {
        var rows=targets();var baseline=new LinkedHashMap<String,String>();
        for(var row:rows)baseline.put(row.path,row.text);baseline.put(UNRELATED,UNTOUCHED);
        int refusals=0;
        for(var target:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
            refuses(freshSources(drift),"source drift",target.path);refusals++;
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path,target.text)));
            refuses(duplicate,"duplicate target",target.path);refusals++;
        }
        assertEquals(64,refusals);
    }

    @Test void exactCorePrerequisiteTransitionFixedPointAndThreeRefusals() throws Exception {
        String root="/com/synexia/rewrite/hash-pinned-java/sdk-duplicate-sorted-factories/";
        String target="m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java";
        String before=resource(root+"M3CollectionSdk.java.before.txt"),after=resource(root+"M3CollectionSdk.java.txt");
        assertEquals("1fb66d3c334f4087bfcd2be7502dad25cfe6b18ae7c3a6d968cb44a4612e41e8",sha(before));
        assertEquals("258a4940136bce3998d7c2d717a6f5512f1412fd874580f242d2185be129b85a",sha(after));
        String yaml="/META-INF/rewrite/m3-sdk-duplicate-sorted-factories.yml";
        var url=M3OwnedApacheMultiValueRecipeTest.class.getResource(yaml);assertNotNull(url);
        Recipe recipe;
        try(var stream=M3OwnedApacheMultiValueRecipeTest.class.getResourceAsStream(yaml)) {
            assertNotNull(stream);
            recipe=Environment.builder().load(new YamlResourceLoader(stream,url.toURI(),new Properties(),
                M3OwnedApacheMultiValueRecipeTest.class.getClassLoader())).build()
                .activateRecipes("com.synexia.m3.SdkDuplicateSortedFactories20261018");
        }
        var errors=new ArrayList<Throwable>();
        var source=freshSources(Map.of(target,before,UNRELATED,UNTOUCHED));
        var results=recipe.run(new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty());assertEquals(1,results.size());
        assertEquals(before,results.getFirst().getBefore().printAll());
        assertEquals(after,results.getFirst().getAfter().printAll());
        assertEquals(target,path(results.getFirst().getAfter()));
        assertTrue(recipe.run(new InMemoryLargeSourceSet(freshSources(Map.of(target,after,UNRELATED,UNTOUCHED))),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults().isEmpty());
        assertTrue(errors.isEmpty());
        var missing=freshSources(Map.of(UNRELATED,UNTOUCHED));
        var drift=freshSources(Map.of(target,"drift\n"));
        var duplicate=new ArrayList<>(freshSources(Map.of(target,before)));
        duplicate.addAll(freshSources(Map.of(target,after)));
        for(var invalid:List.of(missing,drift,duplicate)) {
            assertThrows(IllegalStateException.class,()->recipe.run(
                    new InMemoryLargeSourceSet(invalid),new InMemoryExecutionContext(errors::add),1));
        }
        assertTrue(errors.isEmpty());
    }
}
