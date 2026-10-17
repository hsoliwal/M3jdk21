/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Original M3 source custody first; only reviewed byte-exact outputs can be emitted. */
final class M3SequencedSdkRecipeTest {
    private static final String R="/com/synexia/rewrite/hash-pinned-java/sequenced-sdk-jdk21/";
    private static final String CLASS="m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java";
    private static final String PROOF="m3/collections/src/test/java/com/m3/collections/M3SequencedSdkContract.java";
    private record Output(String path,String prior,String finalHash,String text) { }
    private static String read(String path) throws Exception {
        try(var stream=M3SequencedSdkRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Output> outputs() throws Exception {
        List<Output> result=new ArrayList<>();
        for(String row:read(R+"manifest.tsv").split("\n")) {
            if(row.isEmpty()||row.startsWith("#"))continue;
            String[] parts=row.split("\t");assertEquals(4,parts.length);
            String text=read(R+parts[3]);assertEquals(parts[2],sha(text));
            result.add(new Output(parts[0],parts[1],parts[2],text));
        }
        assertEquals(List.of(CLASS,PROOF),result.stream().map(Output::path).toList());
        assertEquals("ABSENT",result.get(1).prior());
        assertEquals(result.get(0).prior(),sha(before()));return result;
    }
    private static String before()throws Exception{return read("/preimage/M3CollectionSdk.java.txt");}
    private static Recipe named()throws Exception {
        String path="/META-INF/rewrite/m3-sequenced-sdk-jdk21.yml";
        try(var in=M3SequencedSdkRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in);return Environment.builder().load(new YamlResourceLoader(in,
                    java.net.URI.create("classpath:"+path),new Properties(),
                    M3SequencedSdkRecipeTest.class.getClassLoader())).build()
                    .activateRecipes("com.synexia.m3.SequencedSdkJdk21");
        }
    }
    private static Map<String,String> execute(Map<String,String> current)throws Exception {
        List<SourceFile> sources=new ArrayList<>();
        current.forEach((p,t)->sources.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));
        List<Throwable> errors=new ArrayList<>();
        List<Result> changes=named().run(new InMemoryLargeSourceSet(sources),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        Map<String,String> result=new TreeMap<>();
        for(var change:changes) {
            assertInstanceOf(PlainText.class,change.getAfter());
            assertNull(result.put(change.getAfter().getSourcePath().toString(),change.getAfter().printAll()));
        }
        return result;
    }
    @Test void eightPartialStatesAndFixedPoint()throws Exception {
        var expected=outputs();
        for(int mask=0;mask<4;mask++) for(boolean backwards:List.of(false,true)) {
            var installed=new LinkedHashMap<String,String>();
            installed.put(CLASS,(mask&1)==0?before():expected.get(0).text());
            if((mask&2)!=0)installed.put(PROOF,expected.get(1).text());
            if(backwards){var reordered=new LinkedHashMap<String,String>();
                var keys=new ArrayList<>(installed.keySet());Collections.reverse(keys);
                for(String path:keys)reordered.put(path,installed.get(path));installed=reordered;}
            installed.put("unrelated.txt","retain\n");
            var changes=execute(installed);assertEquals(2-Integer.bitCount(mask),changes.size());
            installed.putAll(changes);assertTrue(execute(installed).isEmpty());
            for(var v:expected)assertEquals(v.finalHash(),sha(installed.get(v.path())));
            assertEquals("retain\n",installed.get("unrelated.txt"));
        }
    }
    @Test void absentDriftAndDuplicateTargetsRefuse()throws Exception {
        var recipe=new M3HashPinnedJavaSnapshotRecipe("sequenced-sdk-jdk21",false);
        var context=new InMemoryExecutionContext();
        assertThrows(IllegalStateException.class,()->recipe.generate(recipe.getInitialValue(context),context));
        var inventory=recipe.getInitialValue(context);
        var drift=PlainText.builder().sourcePath(Path.of(CLASS)).text("drift").build();
        recipe.getScanner(inventory).visit(drift,context);
        assertThrows(IllegalStateException.class,()->recipe.generate(inventory,context));
        var duplicated=recipe.getInitialValue(context);
        var old=PlainText.builder().sourcePath(Path.of(CLASS)).text(before()).build();
        recipe.getScanner(duplicated).visit(old,context);
        recipe.getScanner(duplicated).visit(old,context);
        assertThrows(IllegalStateException.class,()->recipe.generate(duplicated,context));
    }
    @Test void staleEditAfterScanRefuses() throws Exception {
        var recipe=new M3HashPinnedJavaSnapshotRecipe("sequenced-sdk-jdk21",false);
        var context=new InMemoryExecutionContext();
        var inventory=recipe.getInitialValue(context);
        var old=PlainText.builder().sourcePath(Path.of(CLASS)).text(before()).build();
        recipe.getScanner(inventory).visit(old,context);recipe.generate(inventory,context);
        var error=assertThrows(org.openrewrite.internal.RecipeRunException.class,
                ()->recipe.getVisitor(inventory).visit(old.withText("late drift"),context));
        assertInstanceOf(IllegalStateException.class,error.getCause());
    }
    @Test void factoryContractsAndOriginalDescriptorPreserved() throws Exception {
        var outputs=outputs();var file=outputs.get(0).text();
        assertEquals(1,file.split("public static <K, V> SequencedMap<K, V> jdkSequencedMap",-1).length-1);
        assertEquals(1,file.split("public static <E> SequencedSet<E> jdkSequencedSet",-1).length-1);
        assertEquals(1,file.split("public static <E> SequencedCollection<E> jdkSequencedCollection",-1).length-1);
        assertEquals(1,file.split("return new M3LinkedHashMap<>\\(\\);",-1).length-1);
        assertEquals(1,file.split("return new M3LinkedHashSet<>\\(\\);",-1).length-1);
        assertEquals(1,file.split("return new M3LinkedList<>\\(\\);",-1).length-1);
        String after=file.replaceAll("(?s)    /\\*\\* Java 21 SequencedMap.*?    public static M3CollectionSdkCatalog", "    public static M3CollectionSdkCatalog")
                .replace("import java.util.SequencedCollection;\n", "")
                .replace("import java.util.SequencedMap;\n", "")
                .replace("import java.util.SequencedSet;\n", "");
        assertEquals(before(),after,"Original SDK API methods changed unexpectedly");
        assertTrue(outputs.get(1).text().contains("147456") || outputs.get(1).text().contains("64*768*3"));
    }
}
