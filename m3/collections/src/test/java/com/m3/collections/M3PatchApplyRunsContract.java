/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Oracle: primitive run composition equals the original ordered edit sequence. */
public final class M3PatchApplyRunsContract {
    private static long checks;
    private M3PatchApplyRunsContract() { }
    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }
    private static int[][] runs(boolean[] selected, boolean descending) {
        int[] starts = new int[selected.length], sizes = new int[selected.length];
        int count=0;
        for(int i=0;i<selected.length;i++) {
            if (!selected[i]) continue;
            int first=i;
            while(i+1<selected.length&&selected[i+1])i++;
            starts[count]=first;sizes[count++]=i-first+1;
        }
        starts=Arrays.copyOf(starts,count);sizes=Arrays.copyOf(sizes,count);
        if(descending)for(int i=0,j=count-1;i<j;i++,j--){
            int a=starts[i];starts[i]=starts[j];starts[j]=a;
            int b=sizes[i];sizes[i]=sizes[j];sizes[j]=b;
        }
        return new int[][]{starts,sizes};
    }
    private static M3Patch patch(boolean[] remove, boolean[] insert) {
        int[][] r=runs(remove,true),a=runs(insert,false);
        int kept=0;for(boolean bit:remove)if(!bit)kept++;
        return new M3Patch(r[0],r[1],a[0],a[1],new int[0],new int[0],kept);
    }
    private static long[] oracle(M3Patch patch,long[] before,long[] after) {
        List<Long> values=new ArrayList<>();
        for(long v:before)values.add(v);
        patch.applyTo(new M3Patch.Target() {
            @Override public void remove(int index,int count) {
                for(int i=0;i<count;i++)values.remove(index);
            }
            @Override public void insert(int index,int count) {
                for(int i=0;i<count;i++)values.add(index+i,after[index+i]);
            }
        });
        long[] answer=new long[values.size()];
        for(int i=0;i<answer.length;i++)answer[i]=values.get(i);
        return answer;
    }
    private static void compare(long[] before,boolean[] removed,boolean[] inserted) {
        int n=before.length,target=inserted.length;
        long[] after=new long[target];
        int read=0;
        for(int dest=0;dest<target;dest++) {
            if(inserted[dest])after[dest]=-1000000L-dest;
            else {
                while(read<n&&removed[read])read++;
                after[dest]=before[read++];
            }
        }
        M3Patch p=patch(removed,inserted);
        long[] original=before.clone(),newValues=after.clone();
        long[] actual=p.apply(before,after);
        yes(Arrays.equals(actual,after),"direct target values differ");
        yes(Arrays.equals(actual,oracle(p,before,after)),"historical Target edit ordering differs");
        yes(Arrays.equals(before,original)&&Arrays.equals(after,newValues),"input array mutated");
        yes(p.removedCount()+p.keptCount()==n,"removed and kept conservation");
        yes(n-p.removedCount()+p.insertedCount()==target,"length conservation");
    }
    private static void boundaries() {
        compare(new long[0],new boolean[0],new boolean[0]);
        compare(new long[]{1,2,3},new boolean[]{false,false,false},new boolean[]{false,false,false});
        compare(new long[]{1,2,3},new boolean[]{true,true,true},new boolean[0]);
        compare(new long[]{1,2,3},new boolean[]{false,false,false},new boolean[]{true,false,true,false,true,false});
        compare(new long[0],new boolean[0],new boolean[]{true,true,true});
        compare(new long[]{1,2,3,4},new boolean[]{false,true,false,true},new boolean[]{false,true,false,true});
        // A zero-edit patch makes one independent primitive copy, without a list.
        M3Patch empty=patch(new boolean[2],new boolean[2]);
        long[] original={13,17};
        long[] result=empty.apply(original,null);
        yes(result!=original&&Arrays.equals(result,original),"no-edit copy and unused after-null");
    }
    private static void movedKeyRetainsValueOrder() {
        long[] before={1,2,3,4}, after={4,1,2,3};
        M3Patch move=new M3Patch(new int[]{3},new int[]{1},
                new int[]{0},new int[]{1},new int[]{3},new int[]{0},3);
        yes(Arrays.equals(move.apply(before,after),after),"old key moved via remove+insert runs");
        yes(Arrays.equals(move.apply(before,after),oracle(move,before,after)),
                "move is the same ordered primitive run sequence");
        yes(move.moveCount()==1&&move.moveFrom(0)==3&&move.moveTo(0)==0,
                "existing move metadata unchanged");
    }
    private static void randomized() {
        Random rng=new Random(0x4d335041544348L);
        for(int trial=0;trial<25_000;trial++) {
            int n=rng.nextInt(145);
            long[] before=new long[n];boolean[] remove=new boolean[n];
            int kept=0;
            for(int i=0;i<n;i++) {
                before[i]=rng.nextLong();remove[i]=rng.nextInt(3)==0;
                if(!remove[i])kept++;
            }
            int count=kept+rng.nextInt(42);
            boolean[] insert=new boolean[count];
            int extra=count-kept;
            int[] positions=new int[count];
            for(int i=0;i<count;i++)positions[i]=i;
            for(int i=0;i<extra;i++) {
                int other=i+rng.nextInt(count-i);
                int temp=positions[i];positions[i]=positions[other];positions[other]=temp;
                insert[positions[i]]=true;
            }
            compare(before,remove,insert);
        }
    }
    private static void largeRunScale() {
        final int n=200_000;
        long[] before=new long[n];boolean[] remove=new boolean[n],insert=new boolean[n];
        for(int i=0;i<n;i++){
            before[i]=i;remove[i]=(i&1)==0;insert[i]=(i&1)==1;
        }
        // Intentionally avoid the old O(n^2) List oracle at 200k edits.
        long[] after=new long[n];
        int r=0;for(int i=0;i<n;i++)if(insert[i])after[i]=-i;else{
            while(remove[r])r++;
            after[i]=before[r++];
        }
        M3Patch p=patch(remove,insert);
        long[] result=p.apply(before,after);
        yes(Arrays.equals(result,after),"large alternating primitive run output");
        yes(p.removeRuns()==100_000&&p.insertRuns()==100_000,
                "large alternating runs retained without Entry objects");
    }
    public static void main(String[] args) {
        boundaries(); movedKeyRetainsValueOrder(); randomized();
        if(args.length==0||!"--quick".equals(args[0])) largeRunScale();
        System.out.println("M3_PATCH_RUN_APPLY_PASS checks="+checks);
    }
}
