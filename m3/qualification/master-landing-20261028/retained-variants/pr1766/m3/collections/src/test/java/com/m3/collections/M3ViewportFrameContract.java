/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import java.util.Arrays;
import java.util.Random;
/** Real existing-owner CPU/JNI differential for source-order viewport frame lanes. */
public final class M3ViewportFrameContract {
    private M3ViewportFrameContract() { }
    private static long checks;
    private static void check(boolean b) { checks++; if (!b) throw new AssertionError("check="+checks); }
    public static void main(String[] args) {
        boolean nativeLoaded = args.length>0 && "--native".equals(args[0]);
        if (nativeLoaded) System.load(System.getProperty("m3.native.path"));
        Random r = new Random(0x6d330ddc74a7L);
        for (int trial=0;trial<12000;trial++) {
            int n=r.nextInt(192), first=r.nextInt(n+1), len=r.nextInt(n-first+1);
            int[] ids=new int[n], states=new int[n];
            for (int i=0;i<n;i++) { ids[i]=r.nextInt(35); states[i]=r.nextInt(); }
            int required=trial%17==0?Integer.MIN_VALUE:(trial%7==0?0:r.nextInt());
            int forbidden=trial%13==0?required:(trial%11==0?0:r.nextInt());
            int skip=trial%19==0?Integer.MAX_VALUE:r.nextInt(25);
            int budget=trial%23==0?Integer.MAX_VALUE:r.nextInt(36);
            int[] expectedIds=new int[n], expectedOrd=new int[n], expectedStates=new int[n];
            int matched=0;
            for (int i=first,end=first+len;i<end;i++) {
                if ((states[i]&required)==required && (states[i]&forbidden)==0) {
                    expectedIds[matched]=ids[i]; expectedOrd[matched]=i;
                    expectedStates[matched++]=states[i];
                }
            }
            int count=skip>=matched?0:Math.min(budget,matched-skip);
            int[] rows=new int[count+5], ord=new int[count+5], flags=new int[count+5];
            Arrays.fill(rows,99); Arrays.fill(ord,88); Arrays.fill(flags,77);
            int wrote=M3MaskViewportKernel.projectFramePage(ids,states,first,len,
                required,forbidden,skip,budget,rows,2,ord,1,flags,3);
            check(wrote==count);
            for(int j=0;j<count;j++){
                check(rows[j+2]==expectedIds[skip+j]);
                check(ord[j+1]==expectedOrd[skip+j]);
                check(flags[j+3]==expectedStates[skip+j]);
            }
            check(rows[1]==99&&rows[count+2]==99&&ord[0]==88&&flags[2]==77);
            if(nativeLoaded){
                int[] r2=new int[count+5],o2=new int[count+5],s2=new int[count+5];
                Arrays.fill(r2,99);Arrays.fill(o2,88);Arrays.fill(s2,77);
                int nw=M3MaskViewportNative.projectFramePage(ids,states,first,len,
                    required,forbidden,skip,budget,r2,2,o2,1,s2,3);
                check(nw==count&&Arrays.equals(r2,rows)&&Arrays.equals(o2,ord)
                    &&Arrays.equals(s2,flags));
            }
        }
        System.out.println("M3_VIEWPORT_FRAME_PASS checks="+checks+" JNI="+nativeLoaded);
    }
}
