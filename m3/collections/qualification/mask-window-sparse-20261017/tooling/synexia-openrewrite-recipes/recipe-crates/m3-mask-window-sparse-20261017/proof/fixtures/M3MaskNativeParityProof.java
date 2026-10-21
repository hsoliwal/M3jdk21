package com.m3.collections;
import java.util.Arrays;
import java.util.Random;
/** Actual checked JNI parity using the current native symbol and unchanged C source. */
public final class M3MaskNativeParityProof {
    public static void main(String[] args) {
        if (!M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("native library unavailable");
        Random random=new Random(0x734d43544cL);
        long checks=0;
        for (int seed=0; seed<45; seed++) {
            int n=switch(seed%10){case 0->0;case 1->1;case 2->256;case 3->257;default->random.nextInt(2000);};
            int[] rows=new int[n];byte[] states=new byte[n];
            for(int j=0;j<n;j++){rows[j]=random.nextInt();states[j]=(byte)random.nextInt();}
            for(int mask=0;mask<=255;mask++){
                int[] java=M3IndexMaskCompactor.select(rows,states,mask,M3IndexMaskCompactor.Backend.JAVA);
                int[] nativeRows=M3IndexMaskCompactor.select(rows,states,mask,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                if(!Arrays.equals(java,nativeRows)||java==rows||nativeRows==rows)throw new AssertionError("seed="+seed+" mask="+mask);
                checks++;
            }
        }
        System.out.println("PASS M3MaskNativeParityProof checks="+checks+" checkedJNI=true");
    }
}
