// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.lexicon;
import java.net.*;import java.util.*;import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3ReceiverAcceptedRuntimeQualification {
 public static void main(String[] args)throws Exception{
  var before=System.nanoTime();var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);
  launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(HierarchyHeaderTest.class),selectClass(M3InternedLanguageSubstrateTest.class)).build());
  var s=listener.getSummary();var failures=new ArrayList<String>();s.getFailures().forEach(f->failures.add(f.getTestIdentifier().getDisplayName()+": "+f.getException()));
  M3LexiconReceiverProof.main(new String[0]);M3CaseLiangParityProof.main(new String[0]);M3LexiconWireProof.main(new String[0]);M3RegexWordTransferBoundaryProof.main(new String[0]);M3RegexWordTransferCompositionProof.main(new String[0]);
  var out=new TreeMap<String,Object>();out.put("found",s.getTestsFoundCount());out.put("passed",s.getTestsSucceededCount());out.put("failed",s.getTestsFailedCount());out.put("skipped",s.getTestsSkippedCount());out.put("aborted",s.getTestsAbortedCount());out.put("failures",failures);out.put("unchangedMainProofs",List.of("M3LexiconReceiverProof","M3CaseLiangParityProof","M3LexiconWireProof","M3RegexWordTransferBoundaryProof","M3RegexWordTransferCompositionProof"));out.put("javaVersion",System.getProperty("java.runtime.version"));out.put("elapsedMillis",(System.nanoTime()-before)/1000000);out.put("nativeProofsRun",false);
  var c=(HttpURLConnection)URI.create(args[0]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);var bytes=new ObjectMapper().writeValueAsBytes(out);c.setFixedLengthStreamingMode(bytes.length);try(var stream=c.getOutputStream()){stream.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("receipt refused");c.disconnect();
  if(s.getTestsFoundCount()==0||s.getTestsSucceededCount()!=s.getTestsFoundCount())throw new AssertionError(out);
 }
}
