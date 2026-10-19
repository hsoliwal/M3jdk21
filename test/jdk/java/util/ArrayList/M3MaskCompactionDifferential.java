/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * Licensed under GPL version 2, with the Classpath exception.
 *
 * @test
 * @summary M3JDK object array bit-mask compaction preserves references, order and offsets
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3MaskCompactionDifferential
 */
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

public final class M3MaskCompactionDifferential {
    static long checks;
    static void check(boolean good,String why) {checks++;if(!good)throw new AssertionError(why+" #"+checks);}
    static int old(Object[] array,int from,int to,long[] mask) {
        int write=from;
        for(int read=from;read<to;read++) {
            int bit=read-from;
            if((mask[bit>>>6]&(1L<<bit))==0)array[write++]=array[read];
        }
        return write;
    }
    public static void main(String[] args) {
        Random random=new Random(20261018);
        for(int trial=0;trial<25000;trial++) {
            int length=trial%31==0?4096+random.nextInt(4096):random.nextInt(800);
            Object[] actual=new Object[length+32], expected=new Object[length+32];
            for(int i=0;i<actual.length;i++) {
                Object value=random.nextInt(19)==0?null:Integer.valueOf(random.nextInt(17));
                actual[i]=value;expected[i]=value;
            }
            int from=random.nextInt(Math.min(25,length)+1);
            int to=from+random.nextInt(length-from+1);
            int n=to-from;
            long[] deleted=new long[n==0?0:((n-1)>>>6)+1];
            int mode=trial%8;
            for(int i=0;i<n;i++) {
                boolean drop=switch(mode) {
                    case 0 -> true;
                    case 1 -> false;
                    case 2 -> i==0;
                    case 3 -> i+1==n;
                    case 4 -> (i&1)==0;
                    case 5 -> i%123==0;
                    case 6 -> random.nextInt(7)==0;
                    default -> random.nextBoolean();
                };
                if(drop)deleted[i>>>6]|=1L<<(i&63);
            }
            int a=old(expected,from,to,deleted);
            int b=M3CollectionMaskCompactor.compact(actual,from,to,deleted);
            check(a==b,"write offset");
            for(int i=0;i<b;i++)check(actual[i]==expected[i],"identity and ordering");
            for(int i=to;i<actual.length;i++)check(actual[i]==expected[i],"outside window");
        }
        System.out.println("M3_JDK_ARRAY_MASK_DIFF_PASS checks="+checks);
    }
}
