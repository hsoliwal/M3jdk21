// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Maintenance qualification adapter: reuses the unmodified ArrayNativeTest owner.
package jdk.m3.arrays;
import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.Path;import java.util.*;
import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public final class M3ArrayProviderQualification {
 public static void main(String[] a)throws Exception{
  String mode=a[0];
  var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
  for(String option:List.of("UsePerfData","PerfDataSaveToFile"))if(!vm.getVMOption(option).getValue().equals("false"))throw new AssertionError(option);
  if(!vm.getVMOption("PerfDisableSharedMem").getValue().equals("true"))throw new AssertionError("private PerfData");
  if(!mode.equals("java")){System.setProperty("m3.arrays.native.required","true");System.setProperty("m3.arrays.native.path",a[2]);ArrayNative.load(Path.of(a[2]));if(!ArrayNative.isLoaded())throw new AssertionError("required native load");}
  var request=LauncherDiscoveryRequestBuilder.request();
  if(mode.equals("java"))for(String name:List.of("javaFallbackHasExactCompareAndMaterializationSemantics","comparisonRetainsExactResultsAcrossLanesOffsetsAndSeededInputs","publicRangeNullAndEmptyValidationStaysExact","sameArraySameWindowIdentityPreservesJavaAndJniBounds","distinctArraysEmptyWindowsStillCheckBothBoundsAndOptionalNative"))request.selectors(selectMethod(ArrayNativeTest.class,name));
  else request.selectors(selectClass(ArrayNativeTest.class));
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request.build());
  var summary=listener.getSummary();summary.printTo(new java.io.PrintWriter(System.out,true));summary.printFailuresTo(new java.io.PrintWriter(System.out,true));
  var failures=new ArrayList<String>();for(var failure:summary.getFailures()){var w=new java.io.StringWriter();failure.getException().printStackTrace(new java.io.PrintWriter(w));failures.add(failure.getTestIdentifier().getDisplayName()+"\n"+w);}
  long expected=mode.equals("java")?5:7;
  if(summary.getTestsFoundCount()!=expected||summary.getTestsAbortedCount()!=0||summary.getTestsSkippedCount()!=0)throw new AssertionError("discovery/abort");
  boolean negative=mode.equals("legacy");
  if(negative){if(summary.getTestsFailedCount()!=7||failures.stream().anyMatch(s->!s.contains("UnsatisfiedLinkError")))throw new AssertionError("old namespace discriminator");}
  else if(summary.getTestsSucceededCount()!=expected||summary.getTestsFailedCount()!=0)throw new AssertionError("positive tests");
  if(ArrayNative.isLoaded()==mode.equals("java"))throw new AssertionError("provider state");
  String json="{\"mode\":\""+mode+"\",\"found\":"+summary.getTestsFoundCount()+",\"passed\":"+summary.getTestsSucceededCount()+",\"failed\":"+summary.getTestsFailedCount()+",\"aborted\":"+summary.getTestsAbortedCount()+",\"loaded\":"+ArrayNative.isLoaded()+"}";
  byte[] bytes=json.getBytes(StandardCharsets.UTF_8);var connection=(HttpURLConnection)URI.create(a[1]).toURL().openConnection();connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setFixedLengthStreamingMode(bytes.length);try(var out=connection.getOutputStream()){out.write(bytes);}if(connection.getResponseCode()!=204)throw new AssertionError("receipt");connection.disconnect();
  System.out.println("M3_ARRAY_PROVIDER_"+(negative?"EXPECTED_ABI_REFUSAL":"PASS")+" mode="+mode);
  if(negative)throw new AssertionError("M3_ARRAY_PROVIDER_EXPECTED_ABI_REFUSAL");
 }
}
