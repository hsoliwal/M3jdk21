// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser;
import com.sun.source.tree.Tree;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
public final class M3JavacFactImageContract {
 public M3JavacFactImageContract() {}
 private static int checks;
 private static void check(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
 private static void refuses(Throwing action){checks++;try{action.run();throw new AssertionError("accepted invalid evidence");}catch(IOException expected){}}
 @FunctionalInterface private interface Throwing{void run()throws IOException;}
 public static void main(String[] args)throws Exception{
   String code="class Demo { int answer = 42; }";
   M3File source=M3File.of(URI.create("memory:/Demo.java"),code);
   byte[] context=MessageDigest.getInstance("SHA-256").digest("compiler-context".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   M3JavacTree tree=new M3JavacTree(source,new int[]{Tree.Kind.COMPILATION_UNIT.ordinal(),Tree.Kind.CLASS.ordinal(),Tree.Kind.VARIABLE.ordinal()},new int[]{-1,0,1},new int[]{0,0,13},new int[]{code.length(),code.length(),28});
   byte[] image=M3JavacFactImage.capture(tree,context,10_000);
   var view=M3JavacFactImage.read(image,source,context,10_000,10);
   check(view.size()==3,"three-indexed-nodes");
   check(view.capabilities()==7,"syntax-only capabilities");
   check(view.parent(2)==1,"preorder parent");
   check(view.kind(2)==Tree.Kind.VARIABLE,"original javac tree kind");
   check(view.sourceText(1).equals(code),"source identity shared");
   check(view.hasSourceRange(2),"bounded span");
   check(Arrays.equals(image,view.bytes()),"canonical image roundtrip");
   check(Arrays.equals(image,M3JavacFactImage.capture(tree,context,10_000)),"deterministic codec");
   view.bytes()[7]=100;
   check(view.size()==3,"defensive bytes");
   byte[] corrupt=image.clone();corrupt[130]^=1;
   refuses(()->M3JavacFactImage.read(corrupt,source,context,10_000,10));
   refuses(()->M3JavacFactImage.read(image,M3File.of(URI.create("memory:/Other.java"),code),context,10_000,10));
   refuses(()->M3JavacFactImage.read(image,M3File.of(source.uri(),code+" "),context,10_000,10));
   refuses(()->M3JavacFactImage.read(image,source,new byte[32],10_000,10));
   refuses(()->M3JavacFactImage.read(image,source,context,image.length-1,10));
   refuses(()->M3JavacFactImage.read(image,source,context,10_000,2));
   try{view.sourceText(999);throw new AssertionError("bad row");}catch(IndexOutOfBoundsException expected){checks++;}
   System.out.println("M3_JAVAC_FACT_IMAGE_PASS checks="+checks);
 }
}
