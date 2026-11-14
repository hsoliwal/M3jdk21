// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3PrecomputeWireQualification {
 static final Map<String,Object> metrics=new TreeMap<>();
 static String resource(String name)throws Exception {try(var in=M3PrecomputeWireQualification.class.getClassLoader().getResourceAsStream(name)){return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}}
 @Test void finiteDoubleCorpusMatchesJava21()throws Exception{
  int count=0;for(String row:resource("floats.tsv").split("\n")){
   var cells=row.split("\t");long bits=Long.parseUnsignedLong(cells[0],16);double value=Double.longBitsToDouble(bits);
   assertTrue(Double.isFinite(value));assertEquals(Double.toString(value),cells[1],cells[0]);assertEquals(bits,Double.doubleToRawLongBits(Double.parseDouble(cells[1])),cells[0]);count++;
  }
  assertTrue(count>20000);metrics.put("finiteDoubleCases",count);metrics.put("floatCorpusSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(resource("floats.tsv").getBytes(StandardCharsets.UTF_8))));
 }
 @Test void producerPayloadsMatchExistingCanonicalReader()throws Exception{
  var root=new ObjectMapper().readTree(resource("cases.json"));int count=0;
  for(var row:root){String text=row.get("producer").asText();assertEquals(text,M3PrecomputePayload.parse(text).canonicalJson(),row.get("label").asText());count++;}
  assertTrue(count>100);metrics.put("payloadCases",count);
 }
 @Test void receiverRefusalsRemainInForce(){
  for(String text:List.of("{\"x\":null}","{\"x\":{\"y\":1}}","{\"x\":[\"a\"]}","{\"X\":1}","{\"x\":9223372036854775808}","{\"flags\":2147483648}","{\"flags\":1.0}","{\"x\":NaN}"))assertThrows(IllegalArgumentException.class,()->M3PrecomputePayload.parse(text),text);
 }
 public static void main(String[] args)throws Exception {
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3PrecomputeWireQualification.class)).build());
  var s=listener.getSummary();var failures=new ArrayList<String>();s.getFailures().forEach(f->failures.add(f.getException().toString()));var out=new TreeMap<String,Object>();
  out.put("found",s.getTestsFoundCount());out.put("passed",s.getTestsSucceededCount());out.put("failed",s.getTestsFailedCount());out.put("skipped",s.getTestsSkippedCount());out.put("aborted",s.getTestsAbortedCount());out.put("failures",failures);out.put("metrics",metrics);out.put("javaVersion",System.getProperty("java.runtime.version"));
  var c=(HttpURLConnection)URI.create(args[0]).toURL().openConnection();c.setDoOutput(true);c.setRequestMethod("POST");var data=new ObjectMapper().writeValueAsBytes(out);c.setFixedLengthStreamingMode(data.length);try(var stream=c.getOutputStream()){stream.write(data);}if(c.getResponseCode()!=204)throw new AssertionError("receipt refused");c.disconnect();
  if(s.getTestsSucceededCount()!=3||s.getTestsFailedCount()!=0)throw new AssertionError(out);
 }
}
