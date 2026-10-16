// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.Method;

/** Verification-only JNI boundary; never a toolkit production dependency. */
public final class RunSearchJni {
    private RunSearchJni() { }
    private static native int first(int[] pairs,int runs,int value);
    public static void main(String[] args)throws Exception {
        System.load(args[0]); long checks=0;
        for(String cls:new String[]{"org.eclipse.swt.internal.residency.ItemBitSet","org.eclipse.nebula.widgets.grid.model.RowSparseBits"}) {
            Method method=Class.forName(cls).getDeclaredMethod("firstRunAfter",int[].class,int.class,int.class);method.setAccessible(true);
            for(int runs:new int[]{0,1,2,31,32,33,1024,2048}) {
                int[] pairs=new int[2*runs+4];
                for(int i=0;i<runs;i++){pairs[2*i]=i*32;pairs[2*i+1]=i*32+11;}
                // Capacity tail must be ignored even when it contains invalid endpoints.
                pairs[2*runs]=-99;pairs[2*runs+1]=Integer.MIN_VALUE;
                int cursor=0;
                for(int v=-1;v<=65536;v++) {
                    while(cursor<runs&&pairs[2*cursor+1]<=v)cursor++;
                    int nativeResult=first(pairs,runs,v),javaResult=(Integer)method.invoke(null,pairs,runs,v);
                    if(nativeResult!=cursor||javaResult!=cursor)throw new AssertionError("upper bound v="+v+" runs="+runs);
                    checks+=2;
                }
            }
        }
        int refusals=0;
        int[][] bad={null,new int[1],new int[]{1,1},new int[]{0,65537},new int[]{3,9,1,2}};
        int[] counts={0,1,1,1,2};
        for(int i=0;i<bad.length;i++)try{first(bad[i],counts[i],0);throw new AssertionError("missing refusal");}catch(IllegalArgumentException expected){refusals++;}
        try{first(new int[0],-1,0);throw new AssertionError("negative runs");}catch(IllegalArgumentException expected){refusals++;}
        System.out.println("JNI_RUN_SEARCH_PASS comparisons="+checks+" refusals="+refusals);
    }
}
