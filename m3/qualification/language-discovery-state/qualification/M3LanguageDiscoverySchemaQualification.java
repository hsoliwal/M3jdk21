// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.PrintWriter;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3LanguageDiscoverySchemaQualification {
 public static void main(String[] a){
  M3LanguageDiscoverySchemaTest.after=a[0].equals("after");var l=new SummaryGeneratingListener();var x=LauncherFactory.create();x.registerTestExecutionListeners(l);
  x.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3LanguageDiscoverySchemaTest.class)).build());var s=l.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  long expected=M3LanguageDiscoverySchemaTest.after?4:3;
  if(s.getTestsFoundCount()!=4||s.getTestsSucceededCount()!=expected||s.getTestsFailedCount()!=4-expected||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("catalogue characterization counts");
  System.out.println("M3_DISCOVERY_SCHEMA_RESULT variant="+a[0]+" passed="+s.getTestsSucceededCount()+" failed="+s.getTestsFailedCount()+" existingRows="+M3LanguageDiscoverySchemaTest.existingProofRows);
 }
}
