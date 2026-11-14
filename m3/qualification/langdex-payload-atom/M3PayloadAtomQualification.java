// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class M3PayloadAtomQualification {
 public static void main(String[] args)throws Exception {
  M3PayloadAtomTest.after=args[0].equals("after");
  var l=new SummaryGeneratingListener();var x=LauncherFactory.create();x.registerTestExecutionListeners(l);
  x.execute(LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(M3PayloadAtomTest.class)).build());
  var s=l.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=6||s.getTestsSucceededCount()!=6||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("payload atom qualification");
  System.out.println("M3_PAYLOAD_ATOM_API "+M3PayloadAtomTest.api());
  System.out.println("M3_PAYLOAD_ATOM_PASS checks="+M3PayloadAtomTest.checks+" digest="+M3PayloadAtomTest.digest());
 }
}
