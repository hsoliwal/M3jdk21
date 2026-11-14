// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.Path;import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import org.openrewrite.*;import org.openrewrite.internal.InMemoryLargeSourceSet;import org.openrewrite.java.JavaParser;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3LexiconsReceiverV2RecipeTest {
 static final String OLD="m3-lexicons-receiver-v1",NEW="m3-lexicons-receiver-v2";
 static final String ROOT="com/synexia/rewrite/hash-pinned-java/";
 static final Map<String,Object> metrics=new TreeMap<>();static Map<String,String> materialized;static List<SourceFile> current;
 static InMemoryExecutionContext context(){var ctx=new InMemoryExecutionContext(t->{throw new AssertionError(t);});ctx.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",JavaParser.runtimeClasspath());return ctx;}
 static String resource(String name)throws Exception{try(var in=M3LexiconsReceiverV2RecipeTest.class.getClassLoader().getResourceAsStream(name)){return new String(Objects.requireNonNull(in,name).readAllBytes(),StandardCharsets.UTF_8);}}
 static Map<String,String> expected(String crate)throws Exception{var out=new TreeMap<String,String>();for(String row:resource(ROOT+crate+"/manifest.tsv").split("\n")){if(row.isBlank()||row.startsWith("#"))continue;var c=row.split("\t");assertEquals("ABSENT",c[1]);out.put(c[0],M3ResourceEnvelope.decode(resource(ROOT+crate+"/"+c[3]),c[2]));}return out;}
 static List<SourceFile> current()throws Exception{
  if(current==null){var inputs=new ArrayList<Parser.Input>();for(var e:expected(NEW).entrySet())inputs.add(Parser.Input.fromString(Path.of(e.getKey()),e.getValue()));current=JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build().parseInputs(inputs,null,context()).toList();assertEquals(33,current.size());}
  return current;
 }
 @Test void legacyRecipeRecreatesSixteenParallelOwners()throws Exception{
  var result=new M3HashPinnedJavaSnapshotRecipe(OLD).run(new InMemoryLargeSourceSet(current()),context(),1).getChangeset().getAllResults();
  assertEquals(16,result.size());var expected=expected(OLD);
  for(var r:result){assertNull(r.getBefore());assertNotNull(r.getAfter());String path=r.getAfter().getSourcePath().toString().replace('\\','/');assertTrue(path.contains("/com/m3/lexicon/"));assertEquals(expected.get(path),r.getAfter().printAll());assertFalse(expected(NEW).containsKey(path));}
  metrics.put("legacyParallelAdditions",result.size());metrics.put("legacyVersionChanged",false);
 }
 @Test void newRecipeRebuildsAllAcceptedOwnersAndIsFixedPoint()throws Exception{
  var results=new M3HashPinnedJavaSnapshotRecipe(NEW).run(new InMemoryLargeSourceSet(List.of()),context(),1).getChangeset().getAllResults();assertEquals(33,results.size());
  var generated=new ArrayList<SourceFile>();var bodies=new TreeMap<String,String>();
  for(var r:results){assertNull(r.getBefore());var s=Objects.requireNonNull(r.getAfter());String p=s.getSourcePath().toString().replace('\\','/');assertTrue(p.contains("/jdk/m3/lexicon/"));assertFalse(p.contains("/com/m3/"));generated.add(s);bodies.put(p,s.printAll());}
  assertEquals(expected(NEW),bodies);materialized=bodies;
  assertTrue(new M3HashPinnedJavaSnapshotRecipe(NEW).run(new InMemoryLargeSourceSet(generated),context(),1).getChangeset().getAllResults().isEmpty());metrics.put("exactRebuiltOwners",bodies.size());
 }
 @Test void currentAcceptedTreeIsFixedPoint()throws Exception{
  assertTrue(new M3HashPinnedJavaSnapshotRecipe(NEW).run(new InMemoryLargeSourceSet(current()),context(),1).getChangeset().getAllResults().isEmpty());metrics.put("currentOwnersRewritten",0);
 }
 @Test void foreignOccupantIsRefused()throws Exception{
  String path="m3/lexicon/receiver/src/main/java/jdk/m3/lexicon/M3LexiconFormat.java";
  var foreign=JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(Path.of(path),"package jdk.m3.lexicon;\npublic enum M3LexiconFormat {}\n")),null,context()).toList();
  assertThrows(RuntimeException.class,()->new M3HashPinnedJavaSnapshotRecipe(NEW).run(new InMemoryLargeSourceSet(foreign),context(),1).getChangeset().getAllResults());
 }
 @Test void markedTestResourcesAreBoundAndRejectDrift()throws Exception{
  int count=0;for(String line:resource(ROOT+NEW+"/manifest.tsv").split("\n")){
   if(line.isBlank()||line.startsWith("#"))continue;var c=line.split("\t");String raw=resource(ROOT+NEW+"/"+c[3]);if(!raw.startsWith("M3_RESOURCE_ENVELOPE\n"))continue;
   String decoded=M3ResourceEnvelope.decode(raw,c[2]);assertTrue(decoded.contains("package jdk.m3.lexicon;"));int at=raw.indexOf("base64=")+7;char flip=raw.charAt(at)=='A'?'B':'A';String corrupt=raw.substring(0,at)+flip+raw.substring(at+1);
   assertThrows(IllegalStateException.class,()->M3ResourceEnvelope.decode(corrupt,c[2]));count++;
  }assertEquals(2,count);metrics.put("hashBoundTestResources",count);
 }
 public static void main(String[] args)throws Exception{
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3LexiconsReceiverV2RecipeTest.class)).build());var s=listener.getSummary();var fails=new ArrayList<String>();s.getFailures().forEach(f->fails.add(f.getException().toString()));
  var out=new TreeMap<String,Object>();out.put("found",s.getTestsFoundCount());out.put("passed",s.getTestsSucceededCount());out.put("failed",s.getTestsFailedCount());out.put("skipped",s.getTestsSkippedCount());out.put("aborted",s.getTestsAbortedCount());out.put("failures",fails);out.put("metrics",metrics);out.put("generated",materialized);
  var c=(HttpURLConnection)URI.create(args[0]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);var bytes=new ObjectMapper().writeValueAsBytes(out);c.setFixedLengthStreamingMode(bytes.length);try(var stream=c.getOutputStream()){stream.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("receipt refused");c.disconnect();
  if(s.getTestsSucceededCount()!=5||s.getTestsFailedCount()!=0)throw new AssertionError(out);
 }
}
