// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;
import java.io.PrintWriter;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3DerivativeSelectorQualification {
 public static void main(String[] args){
  var l=new SummaryGeneratingListener();var x=LauncherFactory.create();x.registerTestExecutionListeners(l);
  x.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(RegexDerivativeTest.class),selectClass(RegexDerivativeEffectsBridgeTest.class)).build());
  var s=l.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=6||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsSucceededCount()+s.getTestsAbortedCount()!=6||s.getTestsAbortedCount()>1)throw new AssertionError("actual target test discovery/results");
  System.out.println("M3_ACTUAL_DERIVATIVE_TESTS_PASS found=6 succeeded="+s.getTestsSucceededCount()+" aborted="+s.getTestsAbortedCount()+" nativeAvailable="+RegexDerivativeNative.available());
 }
}
