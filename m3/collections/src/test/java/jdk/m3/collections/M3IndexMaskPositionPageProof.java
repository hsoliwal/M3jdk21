// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Deterministic independent bit-test oracle; no junit dependency. */
public final class M3IndexMaskPositionPageProof {
    private static long checks;
    private M3IndexMaskPositionPageProof() { }
    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }
    private static boolean oracle(byte state, int required, int excluded) {
        int bits=Byte.toUnsignedInt(state);
        for (int b=0;b<8;b++) {
            int factor=1<<b;
            if ((required & factor)!=0 && (bits & factor)==0) return false;
            if ((excluded & factor)!=0 && (bits & factor)!=0) return false;
        }
        return true;
    }
    private static long oraclePage(byte[] states, int from, int length,
            int required, int excluded, int[] output, int outputOffset, int capacity) {
        int written=0;
        for(int i=from;i<from+length;i++) {
            if(oracle(states[i],required,excluded)) {
                output[outputOffset+written++]=i;
                if(written==capacity) return ((long)(i+1)<<32)|written;
            }
        }
        return ((long)(from+length)<<32)|written;
    }
    private static void expect(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try {op.run();} catch(Throwable e) {
            if(type.isInstance(e)) return;
            throw new AssertionError("expected "+type.getName()+" got "+e,e);
        }
        throw new AssertionError("expected "+type.getName());
    }
    private static void one(byte[] states,int from,int length,int required,int excluded,int cap,
            M3IndexMaskCompactor.Backend backend) {
        final int offset=3;
        int[] actual=new int[cap+offset+5];
        int[] expected=new int[actual.length];
        Arrays.fill(actual,-8675309);
        Arrays.fill(expected,-8675309);
        long x=oraclePage(states,from,length,required,excluded,expected,offset,cap);
        long y=M3IndexMaskCompactor.selectPositionsPage(states,from,length,required,excluded,actual,offset,cap,backend);
        check(x==y,"result cursor/count differs: "+Long.toHexString(x)+" vs "+Long.toHexString(y));
        check(Arrays.equals(expected,actual),"positions/guard bytes differ");
        int count=(int)y;
        int next=(int)(y>>>32);
        check(count>=0 && count<=cap,"bad count");
        check(next>=from && next<=from+length,"bad next cursor");
        if(length>0) check(next>from,"nonprogressing page");
    }
    private static void pages(byte[] states,int required,int excluded,int cap,
            M3IndexMaskCompactor.Backend backend) {
        int start=0; int visited=0; int[] found=new int[states.length];
        while(start<states.length) {
            int[] into=new int[cap+2];
            Arrays.fill(into,-7);
            long packed=M3IndexMaskCompactor.selectPositionsPage(states,start,
                    states.length-start,required,excluded,into,1,cap,backend);
            int count=(int)packed; int next=(int)(packed>>>32);
            check(next>start && next<=states.length,"page did not progress");
            check(into[0]==-7 && into[cap+1]==-7,"output guard modified");
            System.arraycopy(into,1,found,visited,count);
            visited+=count;
            start=next;
        }
        int[] expected=new int[states.length];int count=0;
        for(int i=0;i<states.length;i++)if(oracle(states[i],required,excluded)) expected[count++]=i;
        check(visited==count,"missing paged hits");
        check(Arrays.equals(Arrays.copyOf(found,visited),Arrays.copyOf(expected,count)),"paged order differs");
    }
    public static void main(String[] args) throws Exception {
        boolean nativeMode=args.length>0 && args[0].equals("native");
        if(nativeMode) check(M3IndexMaskCompactor.nativeAvailable(),"native required");
        M3IndexMaskCompactor.Backend backend=nativeMode
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        for(int n:new int[] {0,1,2,3,7,63,255,256,257,511,512,513,1023,1024,4097}){
            byte[] states=new byte[n];
            for(int i=0;i<n;i++) states[i]=(byte)(i*197+17);
            for(int mask:new int[] {0,1,2,3,7,15,31,63,127,128,255}){
                for(int excluded:new int[] {0,1,3,17,128,255}){
                    for(int cap:new int[] {1,2,7,17,255,257}) {
                        one(states,0,n,mask,excluded,cap,backend);
                        if(n>0) one(states,n/3,n-n/3,mask,excluded,cap,backend);
                    }
                    if(n<=1024 && n>0 && mask%3==0) pages(states,mask,excluded,13,backend);
                }
            }
        }
        Random rand=new Random(0xDD00F5);
        for(int trial=0;trial<1500;trial++){
            int n=rand.nextInt(1300), start=rand.nextInt(n+1), len=rand.nextInt(n-start+1);
            byte[] states=new byte[n];rand.nextBytes(states);
            int required=rand.nextInt(256),excluded=rand.nextInt(256),cap=1+rand.nextInt(320);
            one(states,start,len,required,excluded,cap,backend);
        }
        byte[] sample={0,1,-1};int[] output=new int[4];
        expect(NullPointerException.class,()->M3IndexMaskCompactor.selectPositionsPage(null,0,0,0,0,output,0,1,backend));
        expect(NullPointerException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,0,0,0,null,0,1,backend));
        expect(NullPointerException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,0,0,0,output,0,1,null));
        expect(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,-1,1,0,0,output,0,1,backend));
        expect(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,1,Integer.MAX_VALUE,0,0,output,0,1,backend));
        expect(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,1,0,0,output,-1,1,backend));
        expect(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,1,0,0,output,4,1,backend));
        expect(IllegalArgumentException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,1,-1,0,output,0,1,backend));
        expect(IllegalArgumentException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,1,0,256,output,0,1,backend));
        expect(IllegalArgumentException.class,()->M3IndexMaskCompactor.selectPositionsPage(sample,0,1,0,0,output,0,0,backend));
        int[] sentinel={111,222,333};
        long empty=M3IndexMaskCompactor.selectPositionsPage(sample,3,0,0,0,sentinel,3,0,backend);
        check(empty==(3L<<32)&&Arrays.equals(sentinel,new int[] {111,222,333}),"empty boundary writes");
        if(nativeMode){
            Method m=M3IndexMaskCompactor.class.getDeclaredMethod("selectPositionsPageNative",
                    byte[].class,int.class,int.class,int.class,int.class,int[].class,int.class,int.class);
            m.setAccessible(true);
            for(int[] bad:new int[][] {{-1,1,0,0,0,1},{0,4,0,0,0,1},{0,1,256,0,0,1},
                    {0,1,0,-1,0,1},{0,1,0,0,-1,1},{0,1,0,0,3,2},{0,1,0,0,0,0}}){
                checks++;
                try {m.invoke(null,sample,bad[0],bad[1],bad[2],bad[3],output,bad[4],bad[5]);
                    throw new AssertionError("native admitted invalid "+Arrays.toString(bad));
                } catch(InvocationTargetException e){
                    if(!(e.getCause() instanceof IllegalArgumentException)) throw e;
                }
            }
        }
        System.out.println("PASS M3IndexMaskPositionPageProof checks="+checks+" native="+nativeMode);
    }
}
