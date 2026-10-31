/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
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
final class M3OwnedApacheMultiValueV2RecipeTest {
    private static final String ROOT="/jdk/internal/m3/rewrite/hash-pinned-java/owned-apache-multivalue-v2/";
    private static final String YAML="/META-INF/rewrite/m3-owned-apache-multivalue-v2.yml";
    private static final String NAME="jdk.internal.m3.OwnedApacheMultiValueV220261018";
    private static final int COUNT=32;
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private record Target(String path,String hash,int bytes,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3OwnedApacheMultiValueV2RecipeTest.class.getResourceAsStream(path)) {
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
        var hashes=List.of(            "fc9a03a963e2ae8e2b3dd116d575c3cdb976f3bba9326bede088a4cdafaa4f2a",
            "68ea6bee03da4976eb237f08d6773b68a001bd44f3573abccc5a3aa3fbf3bc55",
            "de9479635c91bd64b7570733a5074fd99b0e9db9a8bbf09b39c5a3ef0fe0e48a",
            "0162d5aa74289bddf338476ff6718dbe9e87d709c090d3368d36d63ab44ab552",
            "07d4669871f03ee4eef42048ff2b8c7ebafd354d2cbbd124156d1f3ec63658f9",
            "9420a4449dc48c0acb6a4da1e868f3954b65f7c0690b24497a59f56b6cb02566",
            "511609c336f077ea9c8fc372deec65f7c4e3d8e76af7802272ce5fe8d9011693",
            "0f8f4cdc9620a44e2077106d678d2f646f03a167682e18eff83075cbbd48d51f",
            "6e22c364227f324e209b9db2fcf72152c47bfd353a0c3a0b2856978d94307ae5",
            "e300b7c016556a6d32ac7496db55644a97239b6118fb1f311383e686f0225069",
            "e1b5fc1c588060ea818e3266ef12fbd5098b2c7e5145643d979489633f4974ac",
            "fe5c83b599a9132eef9420fa9326d22af19f5ba61b9c73f455bfe4afde020f94",
            "1beae08bdadad1f08b1622c13bc875ed1ae4edf64d8222da94401968ad72e61e",
            "0c1be9f3076d3a68afb5089a3a48c3213a80730c0c917c9c74b5479828920f06",
            "f425474837caeeeaf266463b1f959be1c293de25da8577c235fef0d2637157d7",
            "a858e07e766d1a3c5b33860ad532ea6794b4071cd3daad7eca35003919657228",
            "d9543d4d9bae571118ae75ef7cc33873ccdd631132e54764084450478faee394",
            "3d32f5711640c7c041d338a278b7a5b75e8ede4b6783e119adddee5f8a34a457",
            "4a09d728aa720d8d53a7067d46f05a1e9acec0b36296fa484473e68be07a1695",
            "d44921e7c04bdb2b72e799e98ddfdda690c4e665dbc24dd01e535034099d173c",
            "9480dc84967ab74f4aedea07aed54cfdd0716036a0b2d58b187c08f84b8a77aa",
            "2c2434f5b72c830bfbc271518b85fa82327edceda4f74f50f528ac618d8b5672",
            "20f4b0ba50954a1cb99816261aedfecd554c4ece927364c56c4dadf2bed73705",
            "a05f69dffcd2eb019a26db01eb8c59bf62f810efc50f3c3083ecd39749219c46",
            "a953b6d129fdd9f4db9000300a49a5bec9e46022a763b6b2ea42c035b0e7cf78",
            "e21efb44ec141ec4fe0f1c3c58b9bc227ffafc41d183a9bae587c2334ec2c1d2",
            "80ce4dbf9bd4366e0f903fe8ce616ea1ca38a0d5dee9256a1e6f2518dff38daa",
            "5ed36b2010fe34630889a54a7d0024c9caf533195fd7e0ced6a0b241137138c1",
            "fdbe4ed21b7dff9a31a98ddfb2ff615819c43231da65a4d3d98e70d2a0695664",
            "608edffec1b33c9719f09eb4637e3b980d75fa2ea486f874863c16b3b048b720",
            "c17c697e00c273f5a14b45fa1ce8352781f5ce3e4476263955ff5da4d3a2e1d4",
            "3715acc08fed0d5f8defeaa7b27df76a36f41fce6ffe9467269eb513520f1698");var lengths=List.of(8715,33602,4278,6209,3766,2255,1795,2836,3099,14604,3085,2653,2302,2281,1796,2705,1976,10170,1608,5936,2203,1844,4685,2838,2793,5709,3686,4327,5675,5335,39782,15821);
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
        var location=M3OwnedApacheMultiValueV2RecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3OwnedApacheMultiValueV2RecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3OwnedApacheMultiValueV2RecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());var delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("owned-apache-multivalue-v2",engine.getCrateName());assertFalse(engine.isLst());
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
        String root="/jdk/internal/m3/rewrite/hash-pinned-java/sdk-duplicate-sorted-factories/";
        String target="m3/collections/src/main/java/jdk/m3/collections/M3CollectionSdk.java";
        String before=resource(root+"M3CollectionSdk.java.before.txt"),after=resource(root+"M3CollectionSdk.java.txt");
        assertEquals("1fb66d3c334f4087bfcd2be7502dad25cfe6b18ae7c3a6d968cb44a4612e41e8",sha(before));
        assertEquals("e4f05110dd6aaebcfb4f6a6d45d1ff0d8f5b50cc154f6372337d09877b62e83f",sha(after));
        String yaml="/META-INF/rewrite/m3-sdk-duplicate-sorted-factories.yml";
        var url=M3OwnedApacheMultiValueV2RecipeTest.class.getResource(yaml);assertNotNull(url);
        Recipe recipe;
        try(var stream=M3OwnedApacheMultiValueV2RecipeTest.class.getResourceAsStream(yaml)) {
            assertNotNull(stream);
            recipe=Environment.builder().load(new YamlResourceLoader(stream,url.toURI(),new Properties(),
                M3OwnedApacheMultiValueV2RecipeTest.class.getClassLoader())).build()
                .activateRecipes("jdk.internal.m3.SdkDuplicateSortedFactories20261018");
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

    @Test void twoFileTransitionAllEightStatesAndSixRefusals() throws Exception {
        String root="/jdk/internal/m3/rewrite/hash-pinned-java/apache-multivalue-v1-to-v2/";
        String oldRoot="/jdk/internal/m3/rewrite/hash-pinned-java/owned-apache-multivalue/";
        var rows=resource(root+"manifest.tsv").lines().map(line->line.split("\t",-1)).toList();
        assertEquals(2,rows.size());
        var before=new LinkedHashMap<String,String>();var after=new LinkedHashMap<String,String>();
        for(var row:rows) {
            String a=resource(oldRoot+row[3]),b=resource(root+row[3]);
            assertEquals(row[1],sha(a));assertEquals(row[2],sha(b));
            before.put(row[0],a);after.put(row[0],b);
        }
        var engine=new M3HashPinnedJavaSnapshotRecipe("apache-multivalue-v1-to-v2",false);
        for(int mask=0;mask<4;mask++)for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(int j=0;j<2;j++) {
                int i=reverse?1-j:j;String path=rows.get(i)[0];
                input.put(path,(mask&(1<<i))==0?before.get(path):after.get(path));
            }
            input.put(UNRELATED,UNTOUCHED);
            var source=freshSources(input);var errors=new ArrayList<Throwable>();
            var changes=engine.run(new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
            assertTrue(errors.isEmpty());assertEquals(2-Integer.bitCount(mask),changes.size());
            var output=new LinkedHashMap<>(input);
            for(var change:changes) {
                assertNotNull(change.getBefore());assertNotNull(change.getAfter());
                output.put(path(change.getAfter()),change.getAfter().printAll());
            }
            for(var e:after.entrySet())assertEquals(e.getValue(),output.get(e.getKey()));
            assertEquals(UNTOUCHED,output.get(UNRELATED));
            assertTrue(engine.run(new InMemoryLargeSourceSet(freshSources(output)),
                    new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults().isEmpty());
            assertTrue(errors.isEmpty());
        }
        for(var row:rows) {
            var missing=new LinkedHashMap<>(before);missing.remove(row[0]);
            var drift=new LinkedHashMap<>(before);drift.put(row[0],"drift\n");
            var duplicate=new ArrayList<>(freshSources(before));duplicate.addAll(freshSources(Map.of(row[0],before.get(row[0]))));
            for(var invalid:List.of(freshSources(missing),freshSources(drift),duplicate)) {
                var errors=new ArrayList<Throwable>();
                assertThrows(IllegalStateException.class,()->engine.run(new InMemoryLargeSourceSet(invalid),
                        new InMemoryExecutionContext(errors::add),1));assertTrue(errors.isEmpty());
            }
        }
    }
}
