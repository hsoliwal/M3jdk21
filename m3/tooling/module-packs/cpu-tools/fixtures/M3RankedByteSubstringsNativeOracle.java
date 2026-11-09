// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RankedByteSubstrings;
import jdk.m3.tools.cpu.M3RankedByteSubstringsNative;
import java.util.Arrays;
import java.util.Random;

/** JNI result/exception parity; native arrays must be detached from immutable Java owners. */
public final class M3RankedByteSubstringsNativeOracle {
    private static long checks;
    private static void require(boolean yes,String label) {
        checks++;
        if(!yes)throw new AssertionError(label+" check="+checks);
    }
    private static void compare(byte[] source, Random rnd) {
        M3RankedByteSubstrings index=M3RankedByteSubstrings.copyOf(source);
        long total=index.distinctCount();
        long[] queries=new long[total==0?0:Math.min(50,source.length*2+3)];
        for(int i=0;i<queries.length;i++)queries[i]=1+Math.floorMod(rnd.nextLong(),total);
        int[] javaOutput=index.selectSpans(queries);
        int[] nativeOutput=M3RankedByteSubstringsNative.selectSpans(index,queries);
        require(Arrays.equals(javaOutput,nativeOutput),"JNI parity");
        require(javaOutput!=nativeOutput,"detached arrays");
        if(nativeOutput.length>0)nativeOutput[0]^=0x5f;
        require(Arrays.equals(javaOutput,M3RankedByteSubstringsNative.selectSpans(index,queries)),"native ownership");
        require(Arrays.equals(javaOutput,index.selectSpans(queries)),"Java ownership");
    }
    public static void main(String[] args) {
        require(M3RankedByteSubstringsNative.isAvailable(),"native required for this oracle");
        Random rnd=new Random(0x4d334a4e4952414eL);
        compare(new byte[0],rnd);
        compare(new byte[]{0,(byte)255,0,(byte)255},rnd);
        for(int i=0;i<6000;i++){
            byte[] bytes=new byte[rnd.nextInt(65)];rnd.nextBytes(bytes);
            compare(bytes,rnd);
        }
        byte[] longInput=new byte[131072];rnd.nextBytes(longInput);
        compare(longInput,rnd);
        M3RankedByteSubstrings index=M3RankedByteSubstrings.copyOf(new byte[]{1,2,3});
        try { M3RankedByteSubstringsNative.selectSpans(index,new long[]{0});throw new AssertionError("bad JNI rank"); }
        catch (IllegalArgumentException expected) {checks++;}
        try { M3RankedByteSubstringsNative.selectSpans(index,new long[32769]);throw new AssertionError("bad JNI size"); }
        catch (IllegalArgumentException expected) {checks++;}
        System.out.println("M3_RANKED_BYTE_JNI_PASS checks="+checks);
    }
}
