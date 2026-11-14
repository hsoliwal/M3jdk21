// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public final class M3LanguageDiscoverySchemaTest {
 static boolean after;static int existingProofRows;
 static final Set<String> FIX=Set.of("unitex-gramlab-lingua-resources","acoli-dicts-ontolex-tiad","conceptnet-5-7-semantic-network","panlexia-concept-linked-wordlists","cygnet-multilingual-wordnet-sqlite");
 static String text(String n)throws IOException{try(var in=M3LanguageDiscoverySchemaTest.class.getResourceAsStream(n)){if(in==null)throw new IOException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static List<String> rows()throws IOException{return text("/jdk/m3/lexicon/hf/language-resource-discovery.tsv").lines().toList();}
 @Test void aIdsRowCountAndEvidenceRemainStable()throws Exception{
  var lines=rows();assertEquals(285,lines.size());assertEquals(9,lines.getFirst().split("\t",-1).length);
  var ids=new HashSet<String>();for(String line:lines.subList(1,lines.size())){var f=line.split("\t",-1);assertTrue(ids.add(f[0]));assertTrue(f[f.length-1].startsWith("https://"));}assertEquals(284,ids.size());assertTrue(ids.containsAll(FIX));
 }
 @Test void bOnlyMissingStateCellsChange()throws Exception{
  var old=text("/m3-discovery-before.tsv").lines().toList();var now=rows();int differences=0;
  assertEquals(old.size(),now.size());
  for(int i=0;i<old.size();i++){var a=old.get(i).split("\t",-1);var b=now.get(i).split("\t",-1);if(after&&i>0&&FIX.contains(a[0])){
   assertEquals(8,a.length);assertEquals(9,b.length);for(int k=0;k<7;k++){if(k==4&&a[0].equals("cygnet-multilingual-wordnet-sqlite"))assertEquals(a[k].replace("preserve per-row source provenance","preserve per-row source provenance and per-wordnet license"),b[k]);else assertEquals(a[k],b[k]);}assertEquals("REFERENCE_ONLY",b[7]);assertEquals(a[7],b[8]);assertTrue(a[6].contains("no payload admitted"));differences++;
  }else assertEquals(old.get(i),now.get(i));}
  assertEquals(after?5:0,differences);
 }
 @Test void cExistingIntakeProofIsAuthoritative()throws Exception{
  var cls=Class.forName("jdk.m3.lexicon.hf.M3HfLexiconIntakeProof");
  var method=cls.getDeclaredMethod("verifyLanguageResourceDiscovery");method.setAccessible(true);
  if(after){existingProofRows=(int)method.invoke(null);assertEquals(284,existingProofRows);}
  else{var failure=assertThrows(InvocationTargetException.class,()->method.invoke(null));assertInstanceOf(AssertionError.class,failure.getCause());assertEquals("language-resource row has nine fields: 280",failure.getCause().getMessage());}
 }
 @Test void dEveryRecordHasExplicitReferenceOnlyState()throws Exception{
  var checks=new ArrayList<org.junit.jupiter.api.function.Executable>();
  for(String line:rows().subList(1,rows().size()))checks.add(()->{var f=line.split("\t",-1);assertEquals(9,f.length,line.substring(0,line.indexOf('\t')));assertEquals("REFERENCE_ONLY",f[7]);});
  assertAll("existing nine-column catalogue contract",checks);
 }
}
