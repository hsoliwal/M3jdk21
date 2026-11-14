// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import java.net.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class M3BigStringUnitRecipeQualification {
 public static void main(String[] a)throws Exception{
  M3BigStringUnitRecipeTest.CRATE=a[1];M3BigStringUnitRecipeTest.stage=a[2];
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);
  launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3BigStringUnitRecipeTest.class)).build());
  var s=listener.getSummary();s.printTo(new java.io.PrintWriter(System.out,true));s.printFailuresTo(new java.io.PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=9||s.getTestsSucceededCount()!=9||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("recipe counts");
  var map=Map.copyOf(M3BigStringUnitRecipeTest.qualified);if(map.size()!=1)throw new AssertionError("qualified outputs");
  var b=new StringBuilder();
  for(var e:map.entrySet()){byte[] value=e.getValue().getBytes(StandardCharsets.UTF_8);String h=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value));b.append(e.getKey()).append('\t').append(h).append('\t').append(Base64.getEncoder().encodeToString(value)).append('\n');}
  byte[] bytes=b.toString().getBytes(StandardCharsets.UTF_8);
  var c=(HttpURLConnection)URI.create(a[0]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(bytes.length);
  try(var out=c.getOutputStream()){out.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("output receipt");c.disconnect();
  String merged=M3BigStringUnitRecipeTest.merged;if(merged==null)throw new AssertionError("missing merge");
  byte[] mergedBytes=merged.getBytes(StandardCharsets.UTF_8);
  String mergedHash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mergedBytes));
  String mergedRecord="synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java\t"+mergedHash+"\t"+Base64.getEncoder().encodeToString(mergedBytes)+"\n";
  byte[] mergeRecordBytes=mergedRecord.getBytes(StandardCharsets.UTF_8);
  var mc=(HttpURLConnection)URI.create(a[3]).toURL().openConnection();mc.setRequestMethod("POST");mc.setDoOutput(true);mc.setConnectTimeout(10000);mc.setReadTimeout(10000);mc.setFixedLengthStreamingMode(mergeRecordBytes.length);
  try(var out=mc.getOutputStream()){out.write(mergeRecordBytes);}if(mc.getResponseCode()!=204)throw new AssertionError("merge receipt");mc.disconnect();
  System.out.println("M3_BIGSTRING_UNIT_RECIPE_PASS tests=9 outputs=1 merge=1");
 }
}
