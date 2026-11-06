// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3RunLengthPostingIndex;
import com.m3.tools.cpu.M3RunLengthPostingIndexNative;
import java.util.Arrays;
import java.util.Random;

/** Differential parity between explicit JNI and independent Java countOf oracles. */
public final class M3RunLengthPostingIndexNativeOracle {
    private static long checks;
    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }

    private static void compare(int[] input, int[] queries) {
        M3RunLengthPostingIndex index=M3RunLengthPostingIndex.fromSorted(input);
        int[] clone=queries.clone();
        int[] result=M3RunLengthPostingIndexNative.batchCounts(index,queries);
        check(result.length==queries.length,"output cardinality");
        for(int i=0;i<queries.length;i++) {
            int expected=0;
            for(int value:input) if(value==queries[i]) expected++;
            check(result[i]==expected,"native count parity");
            check(index.countOf(queries[i])==expected,"Java count parity");
        }
        check(Arrays.equals(clone,queries),"query not mutated");
        if(result.length!=0) result[0]^=17;
        int[] again=M3RunLengthPostingIndexNative.batchCounts(index,queries);
        check(again!=result,"detached result");
        for(int i=0;i<again.length;i++) check(again[i]==index.countOf(queries[i]),"JNI replay parity");
    }

    public static void main(String[] args) {
        if (System.getProperty("m3.cpu.runlength.native.path") == null) {
            boolean failed=false;
            try {M3RunLengthPostingIndexNative.batchCounts(
                    M3RunLengthPostingIndex.fromSorted(new int[]{1}),new int[]{1});}
            catch(IllegalStateException e){failed=true;}
            check(failed,"disabled native must refuse");
            System.out.println("M3_RUNLENGTH_POSTINGS_NATIVE_DISABLED_PASS checks="+checks);
            return;
        }
        compare(new int[0],new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE});
        compare(new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE,0,Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE,-1,0,1,Integer.MAX_VALUE});
        Random random=new Random(0x9372b10adL);
        for(int trial=0;trial<4500;trial++) {
            int n=random.nextInt(180), q=random.nextInt(200);
            int[] input=new int[n], queries=new int[q];
            for(int i=0;i<n;i++)input[i]=random.nextInt(9)==0?random.nextInt():random.nextInt(21)-10;
            for(int i=0;i<q;i++)queries[i]=random.nextInt(13)==0?random.nextInt():random.nextInt(31)-15;
            Arrays.sort(input);
            compare(input,queries);
        }
        int[] longRun=new int[200_000];
        Arrays.fill(longRun,0,80_000,Integer.MIN_VALUE);
        Arrays.fill(longRun,80_000,190_000,0);
        Arrays.fill(longRun,190_000,200_000,Integer.MAX_VALUE);
        compare(longRun,new int[]{Integer.MIN_VALUE,0,0,Integer.MAX_VALUE,Integer.MIN_VALUE+1});
        M3RunLengthPostingIndex index=M3RunLengthPostingIndex.fromSorted(new int[]{-1,0});
        boolean refused=false;
        try {M3RunLengthPostingIndexNative.batchCounts(index,new int[2_000_001]);}
        catch(IllegalArgumentException e){refused=true;}
        check(refused,"oversized batch refused");
        System.out.println("M3_RUNLENGTH_POSTINGS_NATIVE_PASS checks="+checks);
    }
}
