// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

public final class M3MaskedGatherContract {
    private static long checks;
    private M3MaskedGatherContract() { }
    private static void check(boolean b, String s) { checks++; if (!b) throw new AssertionError(s); }
    private static void same(long[] a, long[] b, String s) { checks++; if (!Arrays.equals(a, b)) throw new AssertionError(s); }
    private static <T extends Throwable> void fails(Class<T> clazz, Runnable action, String s) {
        checks++;
        try { action.run(); } catch (Throwable t) {
            if (clazz.isInstance(t)) return;
            throw new AssertionError(s + ": unexpected " + t, t);
        }
        throw new AssertionError(s + ": expected " + clazz.getName());
    }
    public static void main(String[] ignored) {
        final boolean nativeAvailable = M3LongAddressKernel.nativeAvailable();
        if (Boolean.getBoolean("m3.test.native.required") && !nativeAvailable) {
            throw new AssertionError("JNI required for masked gather");
        }
        Random rng = new Random(0x4d33474e494d4153L);
        int[] sizes = {0,1,2,7,64,255,256,257,511,512,513,1024,4097};
        for(int size:sizes) {
            for(int round=0;round<64;round++) {
                final long[] payload=new long[Math.max(1,size/2+5)];
                for(int i=0;i<payload.length;i++) payload[i]=rng.nextLong();
                int prefix=1+rng.nextInt(6),suffix=1+rng.nextInt(6);
                final int[] addresses=new int[prefix+size+suffix];
                final byte[] states=new byte[addresses.length];
                Arrays.fill(addresses,-1);
                final int required=round%4==0?0:1<<(round%8);
                final int forbidden=round%4==1?0:((1<<(3+(round%5)))&~required);
                for(int i=0;i<size;i++){
                    addresses[prefix+i]=rng.nextInt(payload.length);
                    states[prefix+i]=(byte)rng.nextInt(256);
                }
                long[] originalPayload=payload.clone();
                int[] originalAddresses=addresses.clone();
                byte[] originalStates=states.clone();
                long[] expected=new long[size];int selected=0;
                for(int i=0;i<size;i++){
                    int flag=states[prefix+i]&255;
                    if((flag&required)==required&&(flag&forbidden)==0) expected[selected++]=payload[addresses[prefix+i]];
                }
                for(M3IndexMaskCompactor.Backend mode:M3IndexMaskCompactor.Backend.values()){
                    if(mode==M3IndexMaskCompactor.Backend.NATIVE_REQUIRED&&!nativeAvailable) continue;
                    long[] output=new long[size+13];Arrays.fill(output,0x5eedbeefL);
                    int n=M3LongAddressKernel.gatherSelectedInto(payload,addresses,states,prefix,size,output,7,required,forbidden,mode);
                    check(n==selected,"selected count n="+size+" mode="+mode);
                    for(int i=0;i<selected;i++)check(output[7+i]==expected[i],"order,duplicates,long values");
                    for(int i=0;i<output.length;i++)if(i<7||i>=7+selected)check(output[i]==0x5eedbeefL,"padding");
                }
                same(originalPayload,payload,"payload unchanged");
                check(Arrays.equals(originalAddresses,addresses),"address owner unchanged");
                check(Arrays.equals(originalStates,states),"state lane unchanged");
                // Unselected invalid physical IDs are not read. Selected invalids
                // reject atomically before writing even if after a chunk boundary.
                if(size>0){
                    int chosen=prefix+size-1;
                    addresses[chosen]=-4;states[chosen]=(byte)required;
                    // required=0, forbidden could exclude all bits in -1 state?
                    states[chosen]=(byte)(required & ~forbidden);
                    for(M3IndexMaskCompactor.Backend mode:M3IndexMaskCompactor.Backend.values()){
                        if(mode==M3IndexMaskCompactor.Backend.NATIVE_REQUIRED&&!nativeAvailable) continue;
                        long[] untouched=new long[size+2];Arrays.fill(untouched,-77);
                        fails(IndexOutOfBoundsException.class,()->M3LongAddressKernel.gatherSelectedInto(
                                payload,addresses,states,prefix,size,untouched,1,required,forbidden,mode),"selected invalid address");
                        for(long x:untouched)check(x==-77,"atomic preflight");
                    }
                }
            }
        }
        final long[] data={Long.MIN_VALUE,Long.MAX_VALUE,0};
        final int[] idx={0,1,2};
        final byte[] bits={1,2,3};
        for(M3IndexMaskCompactor.Backend mode:M3IndexMaskCompactor.Backend.values()){
            if(mode==M3IndexMaskCompactor.Backend.NATIVE_REQUIRED&&!nativeAvailable) continue;
            check(2==M3LongAddressKernel.gatherSelectedInto(data,idx,bits,0,3,new long[3],0,1,0,mode),"bit compaction");
            check(0==M3LongAddressKernel.gatherSelectedInto(data,new int[] {-9,-8,-7},
                    new byte[] {0,0,0},0,3,new long[3],0,1,0,mode),
                    "unselected physical garbage is ignored");
            fails(IllegalArgumentException.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,bits,0,3,new long[3],0,1,1,mode),"overlap");
            fails(IndexOutOfBoundsException.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,bits,-1,3,new long[3],0,1,0,mode),"window");
            fails(IndexOutOfBoundsException.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,bits,0,3,new long[1],0,1,0,mode),"output capacity");
            fails(IllegalArgumentException.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,bits,0,3,data,0,1,0,mode),"alias");
            fails(NullPointerException.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,null,0,3,new long[3],0,1,0,mode),"null");
        }
        if(!nativeAvailable){
            fails(UnsatisfiedLinkError.class,()->M3LongAddressKernel.gatherSelectedInto(data,idx,bits,0,3,new long[3],0,1,0,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),"native required");
        }
        System.out.println("PASS M3_MASKED_GATHER checks="+checks+" native="+nativeAvailable);
    }
}
