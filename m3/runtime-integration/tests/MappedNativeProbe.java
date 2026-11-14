/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class MappedNativeProbe {
 public static void main(String[] args)throws Exception{
  System.load(System.getProperty("m3.jni"));int calls=0;
  for(String text:new String[]{"alpha","\u0100alpha","\ud800","\ud83d\ude00","\udfff"}){
   String mapped=M3StringIntegration.fresh(text);
   Object atom=M3StringIntegration.owner(mapped);
   M3StringIntegration.check(M3StringInvariant.atom(atom),"mapped scalar owner");
   M3StringIntegration.check(M3StringInvariant.payloadOwner(atom)!=null,"actual mapped payload owner");

   String joined=mapped.concat(M3StringIntegration.fresh("native-local-miss"));
   String slice=joined.substring(0,mapped.length());
   M3StringIntegration.check(M3StringIntegration.owner(slice)==M3StringIntegration.owner(joined),
       "mapped prefix remains tuple-owner range");
   M3StringIntegration.check(slice.equals(mapped),"mapped prefix content");

   for(String value:new String[]{mapped,joined,slice}){
    Object before=M3StringIntegration.body(value);
    Object beforeOwner=M3StringInvariant.owner(before);
    long beforeCoordinate=M3StringInvariant.M3_VALUE.getLong(before);
    for(int mode=0;mode<5;mode++){
      M3StringIntegration.check(value.equals(JoinJni.roundTrip(value,mode)),"mapped JNI boundary");
      calls++;
    }
    Object after=M3StringIntegration.body(value);
    M3StringIntegration.check(M3StringInvariant.owner(after)==beforeOwner,"JNI preserves canonical owner");
    M3StringIntegration.check(M3StringInvariant.M3_VALUE.getLong(after)==beforeCoordinate,
        "JNI preserves canonical coordinate");
    M3StringIntegration.clean(value);
    M3StringIntegration.check(SegmentedJvmti.walk(value)==1,"mapped JVMTI callback");
    M3StringIntegration.clean(value);
   }
  }
  System.out.println("MAPPED_NATIVE_PASS jni="+calls+" jvmti=15 canonical_unchanged=true");
 }
}
