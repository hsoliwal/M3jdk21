// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class M3ManifestIdentityQualification {
 public static void main(String[] args){
  M3ManifestIdentityTest.after=args[0].equals("after");
  var l=new SummaryGeneratingListener();var x=LauncherFactory.create();x.registerTestExecutionListeners(l);
  var q=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(M3ManifestIdentityTest.class));
  if(M3ManifestIdentityTest.after)q.selectors(DiscoverySelectors.selectClass(M3ManifestIdentityGapTest.class));
  x.execute(q.build());var s=l.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  long count=M3ManifestIdentityTest.after?7:6;
  if(s.getTestsSucceededCount()!=count||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("manifest identity qualification");
  System.out.println("M3_MANIFEST_IDENTITY_API "+M3ManifestIdentityTest.api());
  System.out.println("M3_MANIFEST_IDENTITY_PASS checks="+M3ManifestIdentityTest.checks+" digest="+M3ManifestIdentityTest.digest()+" accepted="+M3ManifestIdentityTest.accepted+" rejected="+M3ManifestIdentityTest.rejected);
  M3ManifestIdentityTest.measure();
 }
}
