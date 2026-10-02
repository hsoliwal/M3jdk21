/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class MappedNativeProbe {
 public static void main(String[] args)throws Exception{
  System.load(System.getProperty("m3.jni"));int calls=0;
  for(String text:new String[]{"alpha","\u0100alpha","\ud800","\ud83d\ude00","\udfff"}){
   String mapped=MIndexIntegration.fresh(text);Object atom=MIndexIntegration.body(mapped);
   MIndexIntegration.check(MIndexIntegration.KIND.getByte(atom)==4,"actual existing mapped atom");
   String joined=mapped+MIndexIntegration.fresh("native-local-miss");String slice=joined.substring(0,mapped.length());
   MIndexIntegration.check(MIndexIntegration.body(slice)==atom,"same mapped slice");
   for(String s:new String[]{mapped,joined,slice}){
    for(int mode=0;mode<5;mode++){MIndexIntegration.check(s.equals(JoinJni.roundTrip(s,mode)),"mapped JNI boundary");calls++;}
    MIndexIntegration.clean(s);
    MIndexIntegration.check(SegmentedJvmti.walk(s)==1,"mapped JVMTI callback");MIndexIntegration.clean(s);
   }
  }
  System.out.println("MAPPED_NATIVE_PASS jni="+calls+" jvmti=15 no_java_flatten=true");
 }
}
