// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RunLengthPostingIndex;
import jdk.m3.tools.cpu.M3RunLengthPostingIndexNative;
import java.util.Arrays;
import java.util.Random;

/** Public Java/JNI parity and independent run-frequency reference for sorted/unsorted batches. */
public final class M3RunLengthBatchAdaptiveOracle {
    private static long checks;
    private static void check(boolean truth,String desc) {
        checks++;
        if(!truth) throw new AssertionError(desc+" at "+checks);
    }
    private static int scalarCount(int[] raw,int query){
        int count=0;
        for(int value:raw)if(value==query)count++;
        return count;
    }
    private static void inspect(int[] raw,int[] queries,String name){
        int[] snapshot=raw.clone(), originalQueries=queries.clone();
        M3RunLengthPostingIndex idx=M3RunLengthPostingIndex.fromSorted(raw);
        int[] actual=M3RunLengthPostingIndexNative.batchCounts(idx,queries);
        check(actual.length==queries.length,name+" cardinality");
        for(int i=0;i<queries.length;i++){
            int oracle=scalarCount(snapshot,queries[i]);
            check(actual[i]==oracle,name+" native @"+i);
            check(idx.countOf(queries[i])==oracle,name+" Java @"+i);
        }
        check(Arrays.equals(snapshot,raw),name+" source mutated");
        check(Arrays.equals(originalQueries,queries),name+" query mutated");
        if(actual.length!=0) actual[0]^=0x147147;
        int[] repeat=M3RunLengthPostingIndexNative.batchCounts(idx,queries);
        check(actual!=repeat,name+" detached result");
        for(int i=0;i<queries.length;i++)check(repeat[i]==scalarCount(snapshot,queries[i]),name+" replay");
    }
    public static void main(String[] args){
        if(System.getProperty("m3.cpu.runlength.native.path")==null){
            boolean refused=false;
            try {M3RunLengthPostingIndexNative.batchCounts(
                    M3RunLengthPostingIndex.fromSorted(new int[]{1}),new int[]{1});}
            catch(IllegalStateException expected){refused=true;}
            check(refused,"opt-in gate");
            System.out.println("M3_RUNLENGTH_BATCH_DISABLED_PASS checks="+checks);
            return;
        }
        inspect(new int[0],new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE},"empty runs");
        inspect(new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE,0,0,0,Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE,-1,0,0,1,Integer.MAX_VALUE},"extremes");
        Random random=new Random(0x4d3357423230L);
        for(int trial=0;trial<220;trial++){
            int n=random.nextInt(180),q=random.nextInt(400);
            int[] source=new int[n],queries=new int[q];
            for(int i=0;i<n;i++)source[i]=random.nextInt(25)==0?random.nextInt():random.nextInt(21)-10;
            for(int i=0;i<q;i++)queries[i]=random.nextInt(40)==0?random.nextInt():random.nextInt(31)-15;
            Arrays.sort(source);
            if(trial%3==0)Arrays.sort(queries);
            if(trial%3==1){Arrays.sort(queries);for(int i=0;i<q/2;i++){
                int old=queries[i];queries[i]=queries[q-i-1];queries[q-i-1]=old;
            }}
            inspect(source,queries,"trial-"+trial);
        }
        // Enough queries to select native monotone merge; sparse and run-heavy inputs.
        int[] lane=new int[512*5];
        for(int i=0;i<lane.length;i++)lane[i]=i/5*3-6144;
        int[] sorted=new int[2048];
        for(int i=0;i<sorted.length;i++)sorted[i]=(i/8)*3-6144;
        inspect(lane,sorted,"monotone large");
        for(int i=0;i<sorted.length/2;i++){
            int t=sorted[i];sorted[i]=sorted[sorted.length-i-1];sorted[sorted.length-i-1]=t;
        }
        inspect(lane,sorted,"unsorted/descending large");
        int[] duplicate=new int[2_000_000];Arrays.fill(duplicate,Integer.MAX_VALUE);
        inspect(duplicate,new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE},"maximum run");
        System.out.println("M3_RUNLENGTH_BATCH_JNI_PASS checks="+checks);
    }
}
