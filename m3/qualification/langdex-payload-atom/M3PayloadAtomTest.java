// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import jdk.m3.text.M3PrecomputePayload;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.MethodName.class)
public final class M3PayloadAtomTest {
 static boolean after; static long checks;
 static final MessageDigest D=sha();
 static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new AssertionError(e);}}
 static void check(boolean v,String label){checks++;assertTrue(v,label);D.update(label.getBytes(StandardCharsets.UTF_8));D.update((byte)0);}
 static Object constant(String n)throws Exception{Field f=LangDexProjectionReceiverTest.class.getDeclaredField(n);f.setAccessible(true);return f.get(null);}
 static String payload()throws Exception{return (String)constant("PAYLOAD");}
 static String legacy()throws Exception{return (String)constant("LEGACY_PAYLOAD");}
 static List<String> names(String n)throws Exception{Object x=constant(n);List<String> out=new ArrayList<>();for(Object value:(List<?>)x)out.add((String)value);return out;}
 static String result(Throwable t,int count){return (t==null?"OK":t.getClass().getName()+":"+String.valueOf(t.getMessage()))+":"+count;}
 static String oracle(M3PrecomputePayload p){M3PayloadReference.reset();try{M3PayloadReference.verify(p);return result(null,M3PayloadReference.checks());}catch(Throwable t){return result(t,M3PayloadReference.checks());}}
 static String actual(M3PrecomputePayload p)throws Exception{
  if(!after)return oracle(p);
  Method m=LangDexProjectionReceiverTest.class.getDeclaredMethod("verifyPayload",M3PrecomputePayload.class);m.setAccessible(true);
  Field f=LangDexProjectionReceiverTest.class.getDeclaredField("checks");f.setAccessible(true);f.setInt(null,0);
  try{m.invoke(null,p);return result(null,f.getInt(null));}catch(InvocationTargetException e){return result(e.getCause(),f.getInt(null));}
 }
 static void compare(M3PrecomputePayload p,boolean accepted,String label)throws Exception{
  String expected=oracle(p),observed=actual(p);check(expected.equals(observed),label+" outcome:"+observed);
  check(observed.startsWith("OK:")==accepted,label+" acceptance:"+accepted);
 }
 static String changed(String field,String value)throws Exception{
  Matcher m=Pattern.compile("\""+Pattern.quote(field)+"\":-?[0-9]+").matcher(payload());
  if(!m.find())throw new AssertionError("fixture field "+field);
  return payload().substring(0,m.start())+"\""+field+"\":"+value+payload().substring(m.end());
 }
 @Test public void a_helperIsPrivateAndMainApiUnchanged()throws Exception{
  Method m=null;try{m=LangDexProjectionReceiverTest.class.getDeclaredMethod("verifyPayload",M3PrecomputePayload.class);}catch(NoSuchMethodException expected){assertFalse(after);}
  check((m==null&&!after)||(m!=null&&after&&Modifier.isPrivate(m.getModifiers())&&Modifier.isStatic(m.getModifiers())&&m.getReturnType()==void.class),"private helper registration shape");
  check(api().contains("main(java.lang.String[])"),"main signature retained");
 }
 @Test public void b_originalModernFixtureChecksAllFields()throws Exception{
  M3PrecomputePayload p=M3PrecomputePayload.parse(payload());compare(p,true,"original fourteen-field payload");
  check(actual(p).equals("OK:18"),"all eighteen original checks execute");
  check(p.requireInt("langdex_confidence_permille")==1000&&p.requireInt("langdex_lexical_profile_confidence_permille")==735,"independent confidence owners");
  check(p.requireLong("langdex_lexical_class_mask")==4611686018427387905L&&p.requireLong("langdex_lexical_profile_lexical_class_mask")==12L,"independent lexical mask owners");
 }
 @Test public void c_eachFieldValueAndKindMutationRejects()throws Exception{
  for(String n:names("EXPECTED_FIELDS")){
   compare(M3PrecomputePayload.parse(changed(n,"0")),false,"zero mutation "+n);
   String wrongKind=changed(n,"\"text\"");
   IllegalArgumentException rejected=assertThrows(IllegalArgumentException.class,()->M3PrecomputePayload.parse(wrongKind));
   check(rejected.getMessage().startsWith("payload type mismatch for "+n+" at "),"parser refuses String kind "+n);
  }
 }
 @Test public void d_everyMissingFieldAndExtraFieldRejects()throws Exception{
  List<String> parts=List.of(payload().substring(1,payload().length()-1).split(","));
  for(String n:names("EXPECTED_FIELDS")){
   String missing="{"+String.join(",",parts.stream().filter(x->!x.startsWith("\""+n+"\":")).toList())+"}";
   compare(M3PrecomputePayload.parse(missing),false,"missing "+n);
  }
  String extra=payload().substring(0,payload().length()-1)+",\"unclassified_profile\":0}";
  compare(M3PrecomputePayload.parse(extra),false,"extra fabricated profile");
 }
 @Test public void e_legacyOwnerAbsenceRemainsExplicit()throws Exception{
  M3PrecomputePayload p=M3PrecomputePayload.parse(legacy());
  check(p.fieldNames().equals(new TreeSet<>(names("REQUIRED_FIELDS"))),"legacy ten fields retained");
  for(String n:names("EXPECTED_FIELDS"))if(n.startsWith("langdex_lexical_profile_")){
   check(p.field(n).isEmpty(),"legacy owner absent "+n);
   check(assertThrows(IllegalArgumentException.class,()->p.requireLong(n))!=null,"missing owner has no zero fallback "+n);
  }
  compare(p,false,"legacy is not fourteen-field fixture");
 }
 @Test public void f_payloadCanonicalizationIsDistinctFromCatalogWireAdmission()throws Exception{
  List<String> rows=new ArrayList<>(List.of(payload().substring(1,payload().length()-1).split(",")));Collections.reverse(rows);
  M3PrecomputePayload p=M3PrecomputePayload.parse("{"+String.join(",",rows)+"}");
  check(p.canonicalJson().equals(payload()),"value model canonicalizes order");
  compare(p,true,"same parsed values reversed order");
  String observed=actual(null);check(observed.equals(oracle(null))&&!observed.startsWith("OK:"),"null behavior retained");
  check(assertThrows(IllegalArgumentException.class,()->M3PrecomputePayload.parse("{\"x\":null}"))!=null,"null JSON value refused");
 }
 static String api(){List<String> result=new ArrayList<>();for(Method m:LangDexProjectionReceiverTest.class.getDeclaredMethods())if(Modifier.isPublic(m.getModifiers()))result.add(m.toGenericString());Collections.sort(result);return String.join("|",result);}
 static String digest(){return HexFormat.of().formatHex(D.digest());}
}
