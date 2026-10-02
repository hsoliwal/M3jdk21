/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.ref.WeakReference;
public class WeakAdmissionProbe {
 static WeakReference<Object> create()throws Exception{
  String text=new String(new char[]{'u','n','r','e','t','a','i','n','e','d','\u0453'});
  return new WeakReference<>(MIndexIntegration.body(text));
 }
 public static void main(String[] args)throws Exception{
  WeakReference<Object> ref=create();
  for(int i=0;i<100&&ref.get()!=null;i++){System.gc();Thread.sleep(10);}
  if(ref.get()!=null)throw new AssertionError("global table strongly retained local atom");
  new String(new char[]{'t','r','i','g','g','e','r'}).hashCode();
  System.out.println("WEAK_ADMISSION_PASS atom_collected=true");
 }
}
