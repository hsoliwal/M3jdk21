// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.M3CnfSat;
import com.m3.tools.cpu.M3ArrayGraph;
import com.m3.tools.cpu.M3StreamMath;
import com.m3.tools.cpu.M3EvidenceMath;

import java.util.Arrays;
import java.util.Random;

/** Brute reference to verify primitive ranking and evidence projection. */
public final class M3EvidenceMathContract {
    private static long checks;
    private static final Random RNG = new Random(0x4D3352414E4BL);
    private M3EvidenceMathContract() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message + " check=" + checks);
        checks++;
    }

    private static void reject(Runnable action) {
        boolean thrown = false;
        try { action.run(); }
        catch (IllegalArgumentException | ArithmeticException expected) { thrown = true; }
        check(thrown, "invalid input unexpectedly admitted");
    }

    private static int[] rankOracle(int population,int[] ends,int[] candidates,int constant) {
        double[] scores=new double[population];
        for(int list=0;list+1<ends.length;list++)
            for(int i=ends[list];i<ends[list+1];i++) scores[candidates[i]]+=1d/(constant+1+i-ends[list]);
        Integer[] ids=new Integer[population];
        for(int i=0;i<population;i++)ids[i]=i;
        Arrays.sort(ids,(a,b)->{int compare=Double.compare(scores[b],scores[a]);
            return compare==0?Integer.compare(a,b):compare;});
        int[] out=new int[population]; for(int i=0;i<population;i++)out[i]=ids[i];return out;
    }

    private static void ranking() {
        check(Arrays.equals(M3EvidenceMath.reciprocalRankFusion(3,new int[]{0,2,4},
                new int[]{1,0,0,2},60),new int[]{0,1,2}),"rank example");
        check(M3EvidenceMath.reciprocalRankFusion(0,new int[]{0},new int[]{},1).length==0,"empty population");
        reject(()->M3EvidenceMath.reciprocalRankFusion(2,new int[]{0,2},new int[]{1,1},60));
        reject(()->M3EvidenceMath.reciprocalRankFusion(2,new int[]{0,1},new int[]{2},60));
        for(int trial=0;trial<16000;trial++) {
            int population=RNG.nextInt(25)+1, lists=RNG.nextInt(7)+1;
            int[] ends=new int[lists+1]; int total=0;
            for(int list=0;list<lists;list++){total+=RNG.nextInt(population+1);ends[list+1]=total;}
            int[] candidates=new int[total];
            for(int list=0;list<lists;list++) {
                int[] ids=new int[population];for(int i=0;i<population;i++)ids[i]=i;
                for(int i=population;i>1;i--) {int k=RNG.nextInt(i),swap=ids[i-1];ids[i-1]=ids[k];ids[k]=swap;}
                for(int j=ends[list];j<ends[list+1];j++)candidates[j]=ids[j-ends[list]];
            }
            int constant=1+RNG.nextInt(1000);
            int[] actual=M3EvidenceMath.reciprocalRankFusion(population,ends,candidates,constant);
            check(Arrays.equals(actual,rankOracle(population,ends,candidates,constant)),"rank comparison");
        }
    }

    private static void quorum() {
        check(Arrays.equals(M3EvidenceMath.quorumMask(new long[]{3,2,1},3,3,2),new long[]{3}),
                "quorum example");
        reject(()->M3EvidenceMath.quorumMask(new long[]{1},2,64,2));
        for(int trial=0;trial<12000;trial++) {
            int voters=RNG.nextInt(10)+1, entities=RNG.nextInt(141);
            int words=(entities+63)/64;
            long[] ballots=new long[voters*words];
            for(int i=0;i<ballots.length;i++)ballots[i]=RNG.nextLong();
            int required=1+RNG.nextInt(voters);
            long[] mask=M3EvidenceMath.quorumMask(ballots,voters,entities,required);
            for(int entity=0;entity<entities;entity++){
                int votes=0;
                for(int voter=0;voter<voters;voter++)
                    if((ballots[voter*words+(entity>>>6)]&(1L<<(entity&63)))!=0)votes++;
                check((((mask[entity>>>6]>>>(entity&63))&1L)!=0)==(votes>=required),"quorum oracle");
            }
        }
    }

    private static void pareto() {
        check(Arrays.equals(M3EvidenceMath.paretoFrontier(new double[]{1,1,2,2,2,0,1,1},4,2),
                new long[]{(1L<<1)}),"Pareto sample");
        reject(()->M3EvidenceMath.paretoFrontier(new double[]{Double.NaN},1,1));
        for(int trial=0;trial<8000;trial++) {
            int count=RNG.nextInt(48)+1, dimensions=RNG.nextInt(6)+1;
            double[] packed=new double[count*dimensions];
            for(int i=0;i<packed.length;i++)packed[i]=RNG.nextInt(21)-10;
            long[] mask=M3EvidenceMath.paretoFrontier(packed,count,dimensions);
            for(int i=0;i<count;i++) {
                boolean dominated=false;
                for(int j=0;j<count;j++) if(i!=j){
                    boolean good=true,better=false;
                    for(int d=0;d<dimensions;d++){
                        double a=packed[j*dimensions+d],b=packed[i*dimensions+d];
                        good &= a>=b; better |= a>b;
                    }
                    dominated|=good&&better;
                }
                check((((mask[i>>>6]>>>(i&63))&1L)!=0)==!dominated,"Pareto oracle");
            }
        }
    }

    public static void main(String[] args) {
        ranking();quorum();pareto();
        System.out.println("M3_EVIDENCE_MATH_PASS checks="+checks);
    }
}
