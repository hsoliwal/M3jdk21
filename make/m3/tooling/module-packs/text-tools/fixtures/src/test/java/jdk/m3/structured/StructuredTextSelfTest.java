package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import java.util.*;
public final class StructuredTextSelfTest {
 private static final Limits LIMITS=Limits.standard();
 private static TextFrame f(String s){return TextFrame.fromString(s);}
 private static void eq(Object want,Object got){if(!Objects.equals(want,got))throw new AssertionError("want="+want+" got="+got);}
 private static void refuse(Runnable r){try{r.run();throw new AssertionError("accepted invalid input");}catch(IllegalArgumentException expected) {}}
 public static void main(String[] args){
   eq(List.of(List.of("a","b"),List.of("x","y")), StructuredTextTools.delimited(f("a,b\r\nx,y\n"),',',LIMITS));
   eq(List.of(List.of("a,b","line\r\nnext","a\"b")),StructuredTextTools.delimited(f("\"a,b\",\"line\r\nnext\",\"a\"\"b\""),',',LIMITS));
   eq("\"a,b\",\"a\"\"b\"\n",StructuredTextTools.selectDelimited(f("\"a,b\",ok,\"a\"\"b\"\n"),',',new int[]{0,2},LIMITS).text());
   eq("b\ta\n",StructuredTextTools.selectDelimited(f("a\tb\n"),'\t',new int[]{1,0},LIMITS).text());
   eq(List.of(List.of("")),StructuredTextTools.delimited(f("\n"),',',LIMITS));
   eq(List.of(List.of("a","")),StructuredTextTools.delimited(f("a,"),',',LIMITS));
   eq(List.of(),StructuredTextTools.delimited(f(""),',',LIMITS));
   refuse(()->StructuredTextTools.delimited(f("\"a\"junk"),',',LIMITS));
   refuse(()->StructuredTextTools.delimited(f("\"unfinished"),',',LIMITS));
   refuse(()->StructuredTextTools.delimited(f("a\"b"),',',LIMITS));
   refuse(()->StructuredTextTools.selectDelimited(f("a,b"),',',new int[]{2},LIMITS));
   eq("value",StructuredTextTools.ini(f("# comment\n[sec]\n key = value \n"),LIMITS).get("sec").get("key"));
   refuse(()->StructuredTextTools.ini(f("a=1\na=2\n"),LIMITS));
   eq("hello",StructuredTextTools.dotenv(f("export HELLO='hello'\n"),LIMITS).get("HELLO"));
   eq("a#b",StructuredTextTools.dotenv(f("A=a#b # comment\n"),LIMITS).get("A"));
   refuse(()->StructuredTextTools.dotenv(f("a-b=1\n"),LIMITS));
   refuse(()->StructuredTextTools.dotenv(f("X='broken\n"),LIMITS));
   System.out.println("STRUCTURED_TEST_PASS");
 }
}

