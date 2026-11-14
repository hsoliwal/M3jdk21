// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.openrewrite.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3ImageYamlQualification {
 static final String TARGET="m3/core/src/jdk/m3/text/SharedLexiconImage.java";
 static final Map<String,Object> evidence=new TreeMap<>();
 static String read(String n)throws Exception{try(var in=M3ImageYamlQualification.class.getResourceAsStream("/"+n)){if(in==null)throw new AssertionError(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static InMemoryExecutionContext context(List<Throwable> e){var c=new InMemoryExecutionContext(e::add);c.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",List.<Path>of());return c;}
 static SourceFile parse(String text)throws Exception{var e=new ArrayList<Throwable>();var list=JavaParser.fromJavaVersion().classpath(List.<Path>of()).build().parseInputs(List.of(Parser.Input.fromString(Path.of(TARGET),text)),null,context(e)).toList();assertTrue(e.isEmpty(),e.toString());assertEquals(1,list.size());return list.getFirst();}
 static Recipe recipe(String name)throws Exception{
  String yaml=read("META-INF/rewrite/m3-image-record-digest-r237.yml")+"\n---\n"+read("META-INF/rewrite/m3-image-record-window-r238.yml");
  var loader=new YamlResourceLoader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),URI.create("memory:m3-image-admission"),new Properties(),M3ImageYamlQualification.class.getClassLoader());
  var env=new Environment.Builder(new Properties()).load(loader).build();
  return env.activateRecipes(name);
 }
 static Recipe recipe()throws Exception{return recipe("com.synexia.rewrite.M3ImageAdmissionPrecomputeR238");
 }
 @Test void yamlEmitsFinalSource()throws Exception{
  var e=new ArrayList<Throwable>();String before=read("com/synexia/rewrite/hash-pinned-java/m3-image-record-digest/SharedLexiconImage.java.before");
  var result=recipe().run(new InMemoryLargeSourceSet(List.of(parse(before))),context(e)).getChangeset().getAllResults();
  assertTrue(e.isEmpty(),e.toString());assertEquals(1,result.size());var after=result.getFirst().getAfter();assertNotNull(after);assertEquals(read("com/synexia/rewrite/hash-pinned-java/m3-image-record-window/SharedLexiconImage.java.txt"),after.printAll());evidence.put("yamlFinalSource",true);
 }
 @Test void yamlFinalSourceIsFixedPoint()throws Exception{
  var e=new ArrayList<Throwable>();String after=read("com/synexia/rewrite/hash-pinned-java/m3-image-record-window/SharedLexiconImage.java.txt");
  var result=recipe().run(new InMemoryLargeSourceSet(List.of(parse(after))),context(e));
  assertTrue(e.isEmpty(),e.toString());assertTrue(result.getChangeset().getAllResults().isEmpty());evidence.put("yamlFixedPoint",true);
 }
 @Test void yamlSourceDriftRefuses()throws Exception{
  String before=read("com/synexia/rewrite/hash-pinned-java/m3-image-record-digest/SharedLexiconImage.java.before");var e=new ArrayList<Throwable>();
  try{var r=recipe().run(new InMemoryLargeSourceSet(List.of(parse(before+"\n// drift\n"))),context(e));assertTrue(r.getChangeset().getAllResults().isEmpty());assertFalse(e.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
  evidence.put("yamlDriftRefusal",true);
 }
 @Test void yamlWindowStandaloneEmitsFinalSource()throws Exception{
  var e=new ArrayList<Throwable>();String before=read("com/synexia/rewrite/hash-pinned-java/m3-image-record-window/SharedLexiconImage.java.before");
  var changes=recipe("com.synexia.rewrite.M3ImageRecordWindowReuseR238").run(new InMemoryLargeSourceSet(List.of(parse(before))),context(e)).getChangeset().getAllResults();
  assertTrue(e.isEmpty(),e.toString());assertEquals(1,changes.size());assertEquals(read("com/synexia/rewrite/hash-pinned-java/m3-image-record-window/SharedLexiconImage.java.txt"),Objects.requireNonNull(changes.getFirst().getAfter()).printAll());evidence.put("yamlWindowStandaloneFinal",true);
 }
 public static void main(String[] a)throws Exception{
  var listener=new SummaryGeneratingListener();var l=LauncherFactory.create();l.registerTestExecutionListeners(listener);l.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3ImageYamlQualification.class)).build());var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=4||s.getTestsSucceededCount()!=4||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("YAML JUnit counts");
  var data=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of("found",4,"passed",4,"evidence",evidence));
  var c=(HttpURLConnection)URI.create(a[0]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(data.length);try(var out=c.getOutputStream()){out.write(data);}if(c.getResponseCode()!=204)throw new AssertionError("receipt");c.disconnect();
  System.out.println("M3_IMAGE_YAML_PASS tests=4");
 }
}
