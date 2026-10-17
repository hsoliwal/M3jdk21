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

/** Actual unchanged SDK engines, source-custody mode; no replacement transformation authority. */
final class M3ClosureRecipeTest {
    static final String MODULE="/com/m3/rewrite/backport/jdk21-hash-pinned-text/collections-apache-module/";
    static final String PREFIX="/com/synexia/rewrite/hash-pinned-java/";
    static final List<String> LEAVES=List.of("collections-apache-tests","collections-apache-counts","collections-apache-maps","collections-apache-collectors");
    static final String[] OLD={PREFIX+"collections-library-interop/",PREFIX+"collections-multiset/","/com/m3/rewrite/backport/jdk21-hash-pinned-text/collections-multiset/"};
    static final Set<String> METHODS=Set.of("namedOutputsAndFixedPoint","partialResumes","everyDriftRefused","duplicateTargetsRefused","lateEditRefused","corruptTemplatesRefused","representationAndUnrelatedSource","proofReportsFailClosed");
    static InMemoryExecutionContext context(){return new InMemoryExecutionContext(e->{throw new AssertionError(e);});}
    static String resource(String path){try(var in=M3ClosureRecipeTest.class.getResourceAsStream(path)){assertNotNull(in,path);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    static String hash(String s)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
    static Map<String,String> after(String... roots){Map<String,String>out=new TreeMap<>();for(String root:roots)for(String row:resource(root+"manifest.tsv").lines().toList()){
        if(row.isBlank()||row.startsWith("#"))continue;String[]c=row.split("\t",-1);assertEquals(4,c.length);assertEquals("ABSENT",c[1]);String text=resource(root+c[3]);
        try{assertEquals(c[2],hash(text));}catch(Exception e){throw new IllegalStateException(e);}assertNull(out.put(c[0],text));}return out;}
    static Map<String,String> added(){var r=after(LEAVES.stream().map(n->PREFIX+n+"/").toArray(String[]::new));r.putAll(after(MODULE));return r;}
    static Map<String,String> wanted(){Map<String,String>m=after(OLD);m.putAll(added());return m;}
    static List<SourceFile> parse(Map<String,String>m){List<SourceFile>r=new ArrayList<>();m.forEach((p,t)->r.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));return r;}
    static Recipe named(){String p="/META-INF/rewrite/m3-collections-apache-closure.yml";try(var in=M3ClosureRecipeTest.class.getResourceAsStream(p)){
        assertNotNull(in);var loader=new org.openrewrite.config.YamlResourceLoader(in,java.net.URI.create("classpath:"+p),new Properties(),M3ClosureRecipeTest.class.getClassLoader());
        Recipe r=Environment.builder().load(loader).build().activateRecipes("com.synexia.rewrite.M3CollectionsApacheClosure");assertTrue(r.validateAll().stream().allMatch(Validated::isValid));return r;
    }catch(java.io.IOException e){throw new IllegalStateException(e);}}
    static List<Result> run(Recipe r,List<SourceFile>in){return r.run(new InMemoryLargeSourceSet(in),context(),1).getChangeset().getAllResults();}
    static Map<String,String> outputs(List<Result>results){Map<String,String>r=new TreeMap<>();for(Result e:results){assertNull(e.getBefore());assertNotNull(e.getAfter());assertInstanceOf(PlainText.class,e.getAfter());assertNull(r.put(e.getAfter().getSourcePath().toString().replace('\\','/'),e.getAfter().printAll()));}return r;}
    @Test void namedOutputsAndFixedPoint(){assertEquals(18,wanted().size());assertEquals(wanted(),outputs(run(named(),List.of())));assertTrue(run(named(),parse(wanted())).isEmpty());
        Map<String,String> serial=after(OLD);for(String leaf:LEAVES){var actual=outputs(run(new M3HashPinnedJavaSnapshotRecipe(leaf,false),parse(serial)));assertEquals(after(PREFIX+leaf+"/"),actual);serial.putAll(actual);}serial.putAll(outputs(run(new M3Jdk21HashPinnedTextSnapshotRecipe("collections-apache-module"),parse(serial))));assertEquals(wanted(),serial);
    }
    @Test void partialResumes(){Map<String,String>add=added();List<String>paths=new ArrayList<>(add.keySet());assertEquals(7,paths.size());Recipe recipe=named();
        for(int mask=0;mask<(1<<paths.size());mask++){Map<String,String>present=after(OLD),missing=new TreeMap<>();for(int i=0;i<paths.size();i++){String p=paths.get(i);if((mask&(1<<i))!=0)present.put(p,add.get(p));else missing.put(p,add.get(p));}
            for(int order=0;order<2;order++){List<SourceFile>input=parse(present);if(order==1)Collections.reverse(input);assertEquals(missing,outputs(run(recipe,input)),"mask="+mask+" order="+order);}}
    }
    static void refuse(String leaf,SourceFile source){var recipe=new M3HashPinnedJavaSnapshotRecipe(leaf,false);var ctx=context();var inv=recipe.getInitialValue(ctx);recipe.getScanner(inv).visit(source,ctx);assertThrows(IllegalStateException.class,()->recipe.generate(inv,ctx));}
    static void refuseModule(SourceFile source){var r=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-apache-module");var c=context();var i=r.getInitialValue(c);r.getScanner(i).visit(source,c);assertThrows(IllegalStateException.class,()->r.generate(i,c));}
    @Test void everyDriftRefused(){for(var e:after(MODULE).entrySet())for(String text:List.of("foreign\n",e.getValue()+"<!-- drift -->\n"))refuseModule(parse(Map.of(e.getKey(),text)).get(0));for(String leaf:LEAVES)for(var e:after(PREFIX+leaf+"/").entrySet())for(String text:List.of("class Foreign {}\n",e.getValue()+"// drift\n"))refuse(leaf,parse(Map.of(e.getKey(),text)).get(0));}
    @Test void duplicateTargetsRefused(){var mr=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-apache-module");var mc=context();var mi=mr.getInitialValue(mc);var ms=parse(after(MODULE)).get(0);mr.getScanner(mi).visit(ms,mc);mr.getScanner(mi).visit(ms,mc);assertThrows(IllegalStateException.class,()->mr.generate(mi,mc));for(String leaf:LEAVES){var r=new M3HashPinnedJavaSnapshotRecipe(leaf,false);var ctx=context();var inv=r.getInitialValue(ctx);var source=parse(after(PREFIX+leaf+"/")).get(0);r.getScanner(inv).visit(source,ctx);r.getScanner(inv).visit(source,ctx);assertThrows(IllegalStateException.class,()->r.generate(inv,ctx));}}
    @Test void lateEditRefused(){var mr=new M3Jdk21HashPinnedTextSnapshotRecipe("collections-apache-module");var mc=context();var mi=mr.getInitialValue(mc);var me=after(MODULE).entrySet().iterator().next();mr.getScanner(mi).visit(parse(Map.of(me.getKey(),me.getValue())).get(0),mc);mr.generate(mi,mc);assertThrows(Exception.class,()->mr.getVisitor(mi).visit(parse(Map.of(me.getKey(),me.getValue()+"<!-- late -->\n")).get(0),mc));for(String leaf:LEAVES){var r=new M3HashPinnedJavaSnapshotRecipe(leaf,false);var ctx=context();var inv=r.getInitialValue(ctx);var e=after(PREFIX+leaf+"/").entrySet().iterator().next();
        r.getScanner(inv).visit(parse(Map.of(e.getKey(),e.getValue())).get(0),ctx);r.generate(inv,ctx);assertThrows(Exception.class,()->r.getVisitor(inv).visit(parse(Map.of(e.getKey(),e.getValue()+"// late\n")).get(0),ctx));}}
    @Test void corruptTemplatesRefused()throws Exception{Path mp=Path.of(Objects.requireNonNull(getClass().getResource(MODULE+"apache-pom.xml.txt")).toURI());byte[] mb=Files.readAllBytes(mp);try{Files.writeString(mp,"CORRUPT\n");assertThrows(IllegalStateException.class,()->new M3Jdk21HashPinnedTextSnapshotRecipe("collections-apache-module").getInitialValue(context()));}finally{Files.write(mp,mb);}for(String leaf:LEAVES){String root=PREFIX+leaf+"/";String name=resource(root+"manifest.tsv").lines().filter(l->!l.startsWith("#")&&!l.isBlank()).findFirst().orElseThrow().split("\t")[3];Path path=Path.of(Objects.requireNonNull(getClass().getResource(root+name)).toURI());byte[]original=Files.readAllBytes(path);
        try{Files.writeString(path,"CORRUPT\n");assertThrows(IllegalStateException.class,()->new M3HashPinnedJavaSnapshotRecipe(leaf,false).getInitialValue(context()));}finally{Files.write(path,original);}}}
    @Test void representationAndUnrelatedSource(){var e=after(PREFIX+LEAVES.get(1)+"/").entrySet().iterator().next();refuse(LEAVES.get(1),JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(Path.of(e.getKey()),e.getValue())),null,context()).findFirst().orElseThrow());
        assertEquals(wanted(),outputs(run(named(),parse(Map.of("unchanged.txt","untouched\n")))));}
    @Test void proofReportsFailClosed()throws Exception{for(String xml:List.of("<testsuite tests='0'/>","<testsuite tests='8' failures='1'/>","<testsuite tests='8' errors='1'/>","<testsuite tests='8' skipped='1'/>","<testsuite tests='8' failures='0' errors='0' skipped='0'/>")){
        Path path=Files.createTempFile("m3-invalid-proof-",".xml");try{Files.writeString(path,xml);assertThrows(Exception.class,()->M3ClosureDeliveryTest.requireProof(path));}finally{Files.delete(path);}}}
}
