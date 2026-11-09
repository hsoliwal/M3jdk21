/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.ref.WeakReference;

public class CanonicalAdmissionProbe {
 static record Pair(WeakReference<String> stringRef,Object owner){}
 static Pair create()throws Exception{
  String text=new String(new char[]{'c','a','n','o','n','i','c','a','l','\u0453'});
  Object owner=M3StringIntegration.owner(text);
  return new Pair(new WeakReference<>(text),owner);
 }
 public static void main(String[] args)throws Exception{
  Pair pair=create();
  for(int i=0;i<100&&pair.stringRef().get()!=null;i++){System.gc();Thread.sleep(10);}
  if(pair.stringRef().get()!=null)throw new AssertionError("String wrapper not collectible");
  String again=new String(new char[]{'c','a','n','o','n','i','c','a','l','\u0453'});
  if(M3StringIntegration.owner(again)!=pair.owner())
    throw new AssertionError("canonical process-life owner was lost");
  System.out.println("CANONICAL_ADMISSION_PASS wrapper_collected=true owner_retained=true");
 }
}
