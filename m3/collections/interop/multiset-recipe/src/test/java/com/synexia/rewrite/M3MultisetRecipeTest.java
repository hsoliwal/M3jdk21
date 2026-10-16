/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Executes existing SDK engines; no replacement rewrite implementation. */
final class M3MultisetRecipeTest {
    static final String JROOT="/com/synexia/rewrite/hash-pinned-java/collections-multiset/";
    static final String OLDROOT="/com/synexia/rewrite/hash-pinned-java/collections-library-interop/";
    static final String CROOT="/com/m3/rewrite/backport/jdk21-hash-pinned-text/collections-multiset/";
    static final String NAME="com.synexia.rewrite.M3CollectionsMultiset";
    static final Set<String> METHODS=Set.of("namedOutputsAndFixedPoint", "partialResumes", "everyDriftRefused",
            "duplicateTargetsRefused", "lateEditRefused", "corruptTemplatesRefused", "representationAndUnrelatedSource", "proofReportsFailClosed");
    static InMemoryExecutionContext context(){return new InMemoryExecutionContext(e->{throw new AssertionError(e);});}
    static String resource(String path) {
        try(var in=M3MultisetRecipeTest.class.getResourceAsStream(path)){
            assertNotNull(in,path);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    static String hash(String text) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    static Map<String,String> after(String... roots){
        Map<String,String> result=new TreeMap<>();
        for(String root:roots)for(String row:resource(root+"manifest.tsv").lines().toList()){
            if(row.isBlank()||row.startsWith("#"))continue;String[] c=row.split("\t",-1);
            assertEquals(4,c.length);assertEquals("ABSENT",c[1]);
            try{assertEquals(c[2],hash(resource(root+c[3])));}catch(Exception e){throw new IllegalStateException(e);}
            assertNull(result.put(c[0],resource(root+c[3])));
        }return result;
    }
    static List<SourceFile> parse(Map<String,String> source){
        List<SourceFile> result=new ArrayList<>();
        for(var e:source.entrySet())result.add(PlainText.builder().sourcePath(Path.of(e.getKey())).text(e.getValue()).build());
        return result;
    }
    static Recipe named(){
        String path="/META-INF/rewrite/m3-collections-multiset.yml";
        try(var in=M3MultisetRecipeTest.class.getResourceAsStream(path)){
            assertNotNull(in);var loader=new org.openrewrite.config.YamlResourceLoader(in,java.net.URI.create("classpath:"+path),new Properties(),M3MultisetRecipeTest.class.getClassLoader());
            var recipe=Environment.builder().load(loader).build().activateRecipes(NAME);
            assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));return recipe;
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    static List<Result> run(Recipe recipe,List<SourceFile> inputs){return recipe.run(new InMemoryLargeSourceSet(inputs),context(),1).getChangeset().getAllResults();}
    static Map<String,String> outputs(List<Result> results){
        Map<String,String> result=new TreeMap<>();
        for(Result r:results){assertNull(r.getBefore());assertNotNull(r.getAfter());String p=r.getAfter().getSourcePath().toString().replace('\\','/');
            assertInstanceOf(PlainText.class,r.getAfter());
            assertNull(result.put(p,r.getAfter().printAll()));}
        return result;
    }
    @Test void namedOutputsAndFixedPoint(){
        var wanted=after(OLDROOT,JROOT,CROOT);assertEquals(11,wanted.size());var recipe=named();
        assertEquals(wanted,outputs(run(recipe,parse(Map.of("Other.java","class Other {}\n")))));
        assertTrue(run(recipe,parse(wanted)).isEmpty());
    }
    @Test void partialResumes(){
        Map<String,String> old=after(OLDROOT),added=after(JROOT,CROOT);List<SourceFile> all=parse(after(OLDROOT,JROOT,CROOT));
        List<String>paths=new ArrayList<>(added.keySet());Recipe recipe=named();
        for(int mask=0;mask<(1<<paths.size());mask++){
            Set<String>included=new HashSet<>(old.keySet());Map<String,String>missing=new TreeMap<>();
            for(int i=0;i<paths.size();i++)if((mask&(1<<i))!=0)included.add(paths.get(i));else missing.put(paths.get(i),added.get(paths.get(i)));
            List<SourceFile>input=all.stream().filter(f->included.contains(f.getSourcePath().toString())).toList();
            if((mask&1)!=0){input=new ArrayList<>(input);Collections.reverse(input);}
            assertEquals(missing,outputs(run(recipe,input)),"resume "+mask);
        }
    }
    static void refuseJava(SourceFile file){
        var r=new M3HashPinnedJavaSnapshotRecipe("collections-multiset",false);var ctx=context();var inv=r.getInitialValue(ctx);r.getScanner(inv).visit(file,ctx);
        assertThrows(IllegalStateException.class,()->r.generate(inv,ctx));
    }
    static void refuseText(SourceFile file){
        var r=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-multiset");var ctx=context();var inv=r.getInitialValue(ctx);r.getScanner(inv).visit(file,ctx);
        assertThrows(IllegalStateException.class,()->r.generate(inv,ctx));
    }
    @Test void everyDriftRefused(){
        for(var entry:after(JROOT).entrySet())for(String s:List.of("class Foreign {}\n",entry.getValue()+"// drift\n"))refuseJava(parse(Map.of(entry.getKey(),s)).get(0));
        for(var entry:after(CROOT).entrySet())for(String s:List.of("foreign\n",entry.getValue()+"/* drift */\n"))refuseText(parse(Map.of(entry.getKey(),s)).get(0));
    }
    @Test void duplicateTargetsRefused(){
        var ctx=context();var jr=new M3HashPinnedJavaSnapshotRecipe("collections-multiset",false);var ji=jr.getInitialValue(ctx);var jf=parse(after(JROOT)).get(0);
        jr.getScanner(ji).visit(jf,ctx);jr.getScanner(ji).visit(jf,ctx);assertThrows(IllegalStateException.class,()->jr.generate(ji,ctx));
        var cr=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-multiset");var ci=cr.getInitialValue(ctx);var cf=parse(after(CROOT)).get(0);
        cr.getScanner(ci).visit(cf,ctx);cr.getScanner(ci).visit(cf,ctx);assertThrows(IllegalStateException.class,()->cr.generate(ci,ctx));
    }
    @Test void lateEditRefused(){
        var ctx=context();var jr=new M3HashPinnedJavaSnapshotRecipe("collections-multiset",false);var ji=jr.getInitialValue(ctx);var je=after(JROOT).entrySet().iterator().next();
        jr.getScanner(ji).visit(parse(Map.of(je.getKey(),je.getValue())).get(0),ctx);jr.generate(ji,ctx);
        assertThrows(Exception.class,()->jr.getVisitor(ji).visit(parse(Map.of(je.getKey(),je.getValue()+"// late\n")).get(0),ctx));
        var cr=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-multiset");var ci=cr.getInitialValue(ctx);var ce=after(CROOT).entrySet().iterator().next();
        cr.getScanner(ci).visit(parse(Map.of(ce.getKey(),ce.getValue())).get(0),ctx);cr.generate(ci,ctx);
        assertThrows(Exception.class,()->cr.getVisitor(ci).visit(parse(Map.of(ce.getKey(),ce.getValue()+"/* late */\n")).get(0),ctx));
    }
    @Test void corruptTemplatesRefused() throws Exception {
        for(String root:List.of(JROOT,CROOT)){
            String name=resource(root+"manifest.tsv").lines().filter(l->!l.startsWith("#")&&!l.isBlank()).findFirst().orElseThrow().split("\t")[3];
            Path path=Path.of(Objects.requireNonNull(getClass().getResource(root+name)).toURI());byte[] original=Files.readAllBytes(path);
            try{Files.writeString(path,"CORRUPT\n");
                if(root.equals(JROOT))assertThrows(IllegalStateException.class,()->new M3HashPinnedJavaSnapshotRecipe("collections-multiset",false).getInitialValue(context()));
                else assertThrows(IllegalStateException.class,()->new M3Jdk21HashPinnedTextSnapshotRecipe("collections-multiset").getInitialValue(context()));
            }finally{Files.write(path,original);}
        }
    }
    @Test void representationAndUnrelatedSource(){
        var first=after(JROOT).entrySet().iterator().next();
        refuseJava(JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(Path.of(first.getKey()),first.getValue())),null,context()).findFirst().orElseThrow());
        var unrelated=PlainText.builder().sourcePath(Path.of("unchanged.txt")).text("untouched\n").build();
        assertEquals(after(OLDROOT,JROOT,CROOT),outputs(run(named(),List.of(unrelated))));
    }
    @Test void proofReportsFailClosed() throws Exception {
        for(String xml:List.of("<testsuite tests='0'/>","<testsuite tests='8' failures='1'/>","<testsuite tests='8' errors='1'/>","<testsuite tests='8' skipped='1'/>","<testsuite tests='8' failures='0' errors='0' skipped='0'/>")){
            Path f=Files.createTempFile("m3-failed-proof-",".xml");try{Files.writeString(f,xml);assertThrows(Exception.class,()->M3MultisetDeliveryTest.requireProof(f));}finally{Files.delete(f);}
        }
    }
}
