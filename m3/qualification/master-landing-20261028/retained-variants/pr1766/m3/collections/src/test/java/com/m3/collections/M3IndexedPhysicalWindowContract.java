/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
/** Checks the original sorted/filter/slice owners against their full index oracle. */
public final class M3IndexedPhysicalWindowContract {
    private M3IndexedPhysicalWindowContract() { }
    private static long checks;
    private static void check(boolean b) { checks++; if(!b) throw new AssertionError("check="+checks); }
    private static void refuses(Class<? extends Throwable> t, Runnable r) {
        checks++;
        try { r.run(); } catch(Throwable thrown) {
            if(t.isInstance(thrown)) return;
            throw new AssertionError("wrong refusal", thrown);
        }
        throw new AssertionError("missing refusal");
    }
    public static void main(String[] args) {
        Random rng=new Random(0x4d335049565754L);
        for(int trial=0;trial<4000;trial++) {
            int n=rng.nextInt(130);
            Integer[] input=new Integer[n];
            for(int i=0;i<n;i++) input[i]=rng.nextInt(31);
            M3IndexedListSnapshot<Integer> owner=M3IndexedListSnapshot.copyOf(input);
            M3IndexedListSnapshot<Integer> sorted=owner.sorted(Comparator.naturalOrder());
            M3IndexedListSnapshot<Integer> filtered=sorted.filtered(v->(v&1)==0);
            int skip=rng.nextInt(filtered.size()+1);
            M3IndexedListSnapshot<Integer> view=filtered.slice(
                skip,rng.nextInt(filtered.size()-skip+1));
            int from=rng.nextInt(view.size()+1);
            int limit=trial%17==0?Integer.MAX_VALUE:rng.nextInt(40);
            int selected=Math.min(limit,view.size()-from);
            int[] target=new int[selected+5];Arrays.fill(target,-7);
            check(view.copyPhysicalWindow(from,limit,target,2)==selected);
            int[] full=view.physicalIndices();
            for(int j=0;j<selected;j++){
                check(target[j+2]==full[from+j]);
                check(view.get(from+j).equals(input[target[j+2]]));
            }
            check(target[1]==-7&&target[selected+2]==-7);
            int[] saved=target.clone();
            refuses(IndexOutOfBoundsException.class,()->
                view.copyPhysicalWindow(view.size()+1,1,target,0));
            refuses(IndexOutOfBoundsException.class,()->
                view.copyPhysicalWindow(0,-1,target,0));
            refuses(IndexOutOfBoundsException.class,()->
                view.copyPhysicalWindow(0,limit,target,target.length+1));
            refuses(NullPointerException.class,()->
                view.copyPhysicalWindow(0,0,null,0));
            check(Arrays.equals(target,saved));
            check(Arrays.equals(owner.toArray(),input));
        }
        System.out.println("M3_INDEXED_PHYSICAL_PAGE_PASS checks="+checks);
    }
}
