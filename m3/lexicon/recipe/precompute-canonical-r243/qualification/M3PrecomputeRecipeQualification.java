// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.qualification;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.Path;import java.security.MessageDigest;import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import org.openrewrite.*;import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3PrecomputeRecipeQualification {
 static final String TARGET="m3/runtime-integration/export-m3-lexicon.py";
 static final Map<String,Object> metrics=new TreeMap<>();static String generated;
 static String resource(String name)throws Exception {try(var in=M3PrecomputeRecipeQualification.class.getClassLoader().getResourceAsStream(name)){return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}}
 static String hash(String text)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
 static M3Jdk21HashPinnedTextSnapshotRecipe recipe(){return new M3Jdk21HashPinnedTextSnapshotRecipe("precompute-canonical-r243");}
 static InMemoryExecutionContext context(){return new InMemoryExecutionContext(t->{throw new AssertionError(t);});}
 @Test void exactPreimageProducesQualifiedPostimage()throws Exception{
  var recipe=recipe();var context=context();var state=recipe.getInitialValue(context);
  var before=PlainText.builder().sourcePath(Path.of(TARGET)).text(resource("before.py")).build();
  recipe.getScanner(state).visit(before,context);assertTrue(recipe.generate(state,context).isEmpty());
  var after=(PlainText)recipe.getVisitor(state).visit(before,context);assertNotNull(after);assertEquals(resource("after.py"),after.printAll());
  assertEquals(before.getId(),after.getId());assertEquals(before.getSourcePath(),after.getSourcePath());assertEquals(before.getMarkers(),after.getMarkers());
  generated=after.printAll();metrics.put("generatedSha256",hash(generated));metrics.put("productionTargets",recipe.targetPaths());assertEquals(List.of(TARGET),recipe.targetPaths());
 }
 @Test void postimageIsFixedPoint()throws Exception{
  var recipe=recipe();var context=context();var state=recipe.getInitialValue(context);var before=PlainText.builder().sourcePath(Path.of(TARGET)).text(resource("after.py")).build();
  recipe.getScanner(state).visit(before,context);assertTrue(recipe.generate(state,context).isEmpty());assertSame(before,recipe.getVisitor(state).visit(before,context));
 }
 @Test void foreignSourceRefusedBeforeReplacement(){
  var recipe=recipe();var context=context();var state=recipe.getInitialValue(context);var before=PlainText.builder().sourcePath(Path.of(TARGET)).text("# foreign occupant\n").build();
  recipe.getScanner(state).visit(before,context);assertThrows(IllegalStateException.class,()->recipe.generate(state,context));
 }
 @Test void missingOwnerRefused(){var recipe=recipe();var context=context();var state=recipe.getInitialValue(context);assertThrows(IllegalStateException.class,()->recipe.generate(state,context));}
 public static void main(String[] args)throws Exception {
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3PrecomputeRecipeQualification.class)).build());
  var s=listener.getSummary();var failures=new ArrayList<String>();s.getFailures().forEach(f->failures.add(f.getException().toString()));var out=new TreeMap<String,Object>();
  out.put("found",s.getTestsFoundCount());out.put("passed",s.getTestsSucceededCount());out.put("failed",s.getTestsFailedCount());out.put("skipped",s.getTestsSkippedCount());out.put("aborted",s.getTestsAbortedCount());out.put("failures",failures);out.put("metrics",metrics);out.put("generated",generated);
  var mapper=new ObjectMapper();var c=(HttpURLConnection)URI.create(args[0]).toURL().openConnection();c.setDoOutput(true);c.setRequestMethod("POST");var data=mapper.writeValueAsBytes(out);c.setFixedLengthStreamingMode(data.length);try(var stream=c.getOutputStream()){stream.write(data);}if(c.getResponseCode()!=204)throw new AssertionError("receipt refused");c.disconnect();
  if(s.getTestsSucceededCount()!=4||s.getTestsFailedCount()!=0)throw new AssertionError(out);
 }
}
