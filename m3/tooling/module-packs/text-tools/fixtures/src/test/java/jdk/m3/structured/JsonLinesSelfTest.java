package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import java.util.*;
public final class JsonLinesSelfTest {
 private static final Limits LIMITS=Limits.standard();
 private static TextFrame f(String s){return TextFrame.fromString(s);}
 private static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 private static void refuse(Runnable r){try{r.run();throw new AssertionError("accepted bad JSON");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
   eq("42\n\"hi\"\n",JsonLinesTools.select(f("{\"x\":{\"y\":[42]}}\n{\"x\":{\"y\":[\"hi\"]}}"),"/x/y/0",LIMITS).text());
   eq("{\"a\":1}\n",JsonLinesTools.select(f("{\"a\":1}"),"",LIMITS).text());
   eq("true\n",JsonLinesTools.select(f("{\"a/b~c\":true}"),"/a~1b~0c",LIMITS).text());
   eq("\"😀\"\n",JsonLinesTools.select(f("{\"x\":\"\\uD83D\\uDE00\"}"),"/x",LIMITS).text().replace("\\uD83D\\uDE00","😀"));
   refuse(()->JsonLinesTools.select(f("{\"x\":01}"),"/x",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1,\"x\":2}"),"/x",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":true,}"),"/x",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":\"\\uD800\"}"),"/x",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1}"),"/missing",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1}"),"/bad~2",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1}"),"x",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1}"),"/x/0",LIMITS));
   refuse(()->JsonLinesTools.select(f("{\"x\":1}\n\n"),"",LIMITS));
   System.out.println("JSONL_TEST_PASS");
 }
}

