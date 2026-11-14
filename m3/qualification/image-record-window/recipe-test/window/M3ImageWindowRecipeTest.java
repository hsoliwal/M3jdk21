// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;
public final class M3ImageWindowRecipeTest {
 static String CRATE;static String stage;
 static final Map<String,String> qualified=new TreeMap<>();
 static String resource(String n)throws Exception{try(var in=M3ImageWindowRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/"+n)){if(in==null)throw new IllegalStateException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static String[] row()throws Exception{return resource("manifest.tsv").strip().split("\t",-1);}
 static InMemoryExecutionContext ctx(List<Throwable> errors){var c=new InMemoryExecutionContext(errors::add);c.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",List.<Path>of());return c;}
 static SourceFile before()throws Exception{var errors=new ArrayList<Throwable>();var files=JavaParser.fromJavaVersion().classpath(List.<Path>of()).build().parseInputs(List.of(Parser.Input.fromString(Path.of(row()[0]),resource(row()[3].replace(".txt",".before")))),null,ctx(errors)).toList();assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,files.size());assertInstanceOf(J.CompilationUnit.class,files.getFirst());return files.getFirst();}
 static void reject(List<SourceFile> f){var e=new ArrayList<Throwable>();try{var result=new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(f),ctx(e));assertTrue(result.getChangeset().getAllResults().isEmpty());assertFalse(e.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}}
 @Test void replayAndFixedPoint()throws Exception{var original=before();var other=PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("keep\n").build();var errors=new ArrayList<Throwable>();var recipe=new M3HashPinnedJavaSnapshotRecipe(CRATE);var changes=recipe.run(new InMemoryLargeSourceSet(List.of(original,other)),ctx(errors)).getChangeset().getAllResults();assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,changes.size());var after=changes.getFirst().getAfter();assertNotNull(after);assertInstanceOf(J.CompilationUnit.class,after);assertEquals(original.getId(),after.getId());assertEquals(original.getSourcePath(),after.getSourcePath());assertEquals(resource(row()[3]),after.printAll());qualified.put(row()[0],after.printAll());var replay=recipe.run(new InMemoryLargeSourceSet(List.of(after,other)),ctx(errors));assertTrue(errors.isEmpty(),errors.toString());assertTrue(replay.getChangeset().getAllResults().isEmpty());}
 @Test void missingOwnerRefuses(){reject(List.of());}
 @Test void duplicateRefuses()throws Exception{var f=before();reject(List.of(f,f));}
 @Test void wrongAstRefuses()throws Exception{reject(List.of(PlainText.builder().sourcePath(Path.of(row()[0])).text(resource(row()[3].replace(".txt",".before"))).build()));}
 @Test void sourceDriftRefuses()throws Exception{reject(List.of(((J.CompilationUnit)before()).withPrefix(org.openrewrite.java.tree.Space.format("// drift\n"))));}
 @Test void changedAfterScanRefuses()throws Exception{var f=before();var errors=new ArrayList<Throwable>();var c=ctx(errors);var r=new M3HashPinnedJavaSnapshotRecipe(CRATE);var i=r.getInitialValue(c);r.getScanner(i).visit(f,c);var failure=assertThrows(RuntimeException.class,()->r.getVisitor(i).visit(((J.CompilationUnit)f).withPrefix(org.openrewrite.java.tree.Space.format("// moved\n")),c));Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(IllegalStateException.class,cause);assertTrue(cause.getMessage().startsWith("target changed after scan: "));}
 @Test void onlyFrozenAtomChanges()throws Exception{String before=resource(row()[3].replace(".txt",".before")),after=resource(row()[3]);String old=resource("atom-before.txt"),change=resource("atom-after.txt");assertEquals(2,before.split(java.util.regex.Pattern.quote(old),-1).length);assertEquals(before.replace(old,change),after);assertTrue(old.contains("recordDigest.update(recordBytes(i))"));assertFalse(change.contains("recordDigest.update(recordBytes(i))"));assertTrue(change.contains("recordView.rewind().limit(recordBegin + length * Character.BYTES)"));assertTrue(change.contains(".position(recordBegin)"));assertTrue(change.contains("recordView = mapping.asReadOnlyBuffer()"));assertTrue(change.contains("System.arraycopy(recordDigestBytes, 0, recordDigests, i * 32, 32)"));}

 @Test void exactOwnerAdmissionScope()throws Exception{var method=M3HashPinnedJavaSnapshotRecipe.class.getDeclaredMethod("javaPath",String.class);method.setAccessible(true);assertEquals(true,method.invoke(null,"m3/core/src/jdk/m3/text/SharedLexiconImage.java"));for(String path:List.of("m3/core/src/jdk/m3/text/SharedLexiconImageCopy.java","m3/core/src/jdk/m3/text/M3Other.java","m3/core/src/jdk/m3/text/sub/SharedLexiconImage.java"))assertEquals(false,method.invoke(null,path));assertEquals(true,method.invoke(null,"m3/core/src/jdk/m3/text/LocalM3Arena.java"));}
}
