// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RunLengthPostingIndex;
import java.util.Arrays;
import java.util.Random;

/** Independent scalar oracle for endpoint-specialized sorted run index. */
public final class M3RunLengthBoundaryOracle {
    private static long checks;
    private static void check(boolean ok,String label){
        checks++;
        if(!ok)throw new AssertionError(label+" check="+checks);
    }
    private static void verify(int[] sorted,int[] targets){
        M3RunLengthPostingIndex index=M3RunLengthPostingIndex.fromSorted(sorted);
        for(int value:targets){
            int lower=0,upper=0,exact=0;
            for(int current:sorted){
                if(current<value)lower++;
                if(current<=value)upper++;
                if(current==value)exact++;
            }
            check(index.lowerBound(value)==lower,"lower");
            check(index.upperBound(value)==upper,"upper");
            check(index.countOf(value)==exact,"count");
        }
        check(Arrays.equals(sorted,index.toArray()),"immutable snapshot");
        if(index.size()!=0){
            check(index.select(0)==sorted[0],"select first");
            check(index.select(index.size()-1)==sorted[sorted.length-1],"select last");
        }
    }
    public static void main(String[] args){
        verify(new int[0],new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE});
        verify(new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE,0,Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1,-1,0,1,Integer.MAX_VALUE-1,Integer.MAX_VALUE});
        Random random=new Random(0x4d3352563230L);
        for(int k=0;k<15000;k++){
            int[] sorted=new int[random.nextInt(150)];
            for(int i=0;i<sorted.length;i++){
                int r=random.nextInt(30);
                sorted[i]=r==0?Integer.MIN_VALUE:r==1?Integer.MAX_VALUE:random.nextInt(61)-30;
            }
            Arrays.sort(sorted);
            verify(sorted,new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1,-31,-1,0,1,31,
                    Integer.MAX_VALUE-1,Integer.MAX_VALUE,random.nextInt()});
        }
        int[] unique=new int[2_000_000];for(int i=0;i<unique.length;i++)unique[i]=i-1_000_000;
        M3RunLengthPostingIndex wide=M3RunLengthPostingIndex.fromSorted(unique);
        check(wide.lowerBound(Integer.MIN_VALUE)==0,"minimum negative");
        check(wide.upperBound(Integer.MAX_VALUE)==2_000_000,"maximum positive");
        check(wide.countOf(Integer.MIN_VALUE)==0,"low miss");
        check(wide.countOf(Integer.MAX_VALUE)==0,"high miss");
        check(wide.countOf(-1_000_000)==1,"first");
        check(wide.countOf(999_999)==1,"last");
        System.out.println("M3_RUNLENGTH_BOUNDARIES_PASS checks="+checks);
    }
}
