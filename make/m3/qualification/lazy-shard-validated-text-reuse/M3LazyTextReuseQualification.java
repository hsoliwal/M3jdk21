// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class M3LazyTextReuseQualification {
 public static void main(String[] args) throws Exception {
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);
  launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(M3LazyTextReuseTest.class)).build());
  var summary=listener.getSummary();summary.printTo(new PrintWriter(System.out,true));summary.printFailuresTo(new PrintWriter(System.out,true));
  if(summary.getTestsSucceededCount()!=8 || summary.getTestsFailedCount()!=0 || summary.getTestsSkippedCount()!=0)throw new AssertionError("lazy text reuse qualification");
  System.out.println("M3_LAZY_API "+M3LazyTextReuseTest.api());
  M3LazyTextReuseTest.measure();
  System.out.println("M3_LAZY_BEHAVIOR_PASS checks="+M3LazyTextReuseTest.checks+" digest="+M3LazyTextReuseTest.result());
  M3LazyTextReuseTest.mutationWindow(args[0]);
 }
}
