// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class M3EnvelopeNumberQualification{
 public static void main(String[] args)throws Exception{
  var l=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(l);
  launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(M3EnvelopeNumberTest.class)).build());
  var s=l.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsSucceededCount()!=6||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("envelope numeric grammar");
  System.out.println("M3_ENVELOPE_NUMBER_API "+M3EnvelopeNumberTest.api());
  M3EnvelopeNumberTest.measure();
  System.out.println("M3_ENVELOPE_NUMBER_PASS checks="+M3EnvelopeNumberTest.checks+" digest="+M3EnvelopeNumberTest.result());
 }
}
