package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Bounded exact oracle: every result is compared with independent scalar selection. */
public final class MaskDenseSelectionProof {
    private static long checks;
    private static final Random RANDOM = new Random(0x4d334d41534b2026L);

    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("check #" + checks);
    }
    private static void fail(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try { op.run(); }
        catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("wrong exception " + t, t);
        }
        throw new AssertionError("exception missing: " + type.getSimpleName());
    }
    private static void equal(int[] expected, int[] actual) {
        check(Arrays.equals(expected,actual));
    }
    private static void equal(long[] expected, long[] actual) {
        check(Arrays.equals(expected,actual));
    }
    private static int[] expected(int[] source, byte[] state, int mask) {
        int n=0;for(byte b:state)if(((b&255)&mask)==mask)n++;
        int[] r=new int[n];int j=0;
        for(int i=0;i<source.length;i++)if(((state[i]&255)&mask)==mask)r[j++]=source[i];
        return r;
    }
    private static long[] expected(long[] source, byte[] state, int mask) {
        int n=0;for(byte b:state)if(((b&255)&mask)==mask)n++;
        long[] r=new long[n];int j=0;
        for(int i=0;i<source.length;i++)if(((state[i]&255)&mask)==mask)r[j++]=source[i];
        return r;
    }
    public static void main(String[] args) {
        var backend=IndexMaskCompactor.Backend.JAVA;
        int[] ints={0, 27, -1, 27, Integer.MAX_VALUE, Integer.MIN_VALUE};
        long[] longs={0, Long.MIN_VALUE, 27, 27, Long.MAX_VALUE, -1};
        byte[] states={(byte)0xff,0,1,2,3,(byte)0x80};
        for(int mask=0;mask<256;mask++) {
            int[] i=IndexMaskCompactor.select(ints,states,mask,backend);
            long[] l=IndexMaskCompactor.selectLong(longs,states,mask);
            equal(expected(ints,states,mask),i);
            equal(expected(longs,states,mask),l);
            check(IndexMaskCompactor.selectedCount(states,mask)==i.length);
            check(i!=ints);
            check(l!=longs);
        }
        // Both a zero mask and an all-ff state are entire-selection fast paths.
        for(byte[] all: new byte[][]{new byte[256],filled((byte)0xff,256)}) {
            int[] ii=new int[256];long[] ll=new long[256];
            for(int j=0;j<256;j++){ii[j]=j;ll[j]=(long)j*Integer.MAX_VALUE;}
            for(int mask: new int[]{0, 1, 2, 3, 127, 128, 255}) {
                int[] i=IndexMaskCompactor.select(ii,all,mask,backend);
                long[] l=IndexMaskCompactor.selectLong(ll,all,mask);
                equal(expected(ii,all,mask),i);
                equal(expected(ll,all,mask),l);
                check(i!=ii);check(l!=ll);
                if(i.length>0){int before=ii[0];i[0]++;check(ii[0]==before);}
                if(l.length>0){long before=ll[0];l[0]++;check(ll[0]==before);}
            }
        }
        for(int seed=0;seed<50;seed++) {
            int length=RANDOM.nextInt(2000);
            int[] ii=new int[length];long[] ll=new long[length];byte[] bb=new byte[length];
            for(int j=0;j<length;j++) {ii[j]=RANDOM.nextInt();ll[j]=RANDOM.nextLong();bb[j]=(byte)RANDOM.nextInt(256);}
            for(int m=0;m<256;m++) {
                int[] i=IndexMaskCompactor.select(ii,bb,m,backend);
                long[] l=IndexMaskCompactor.selectLong(ll,bb,m);
                equal(expected(ii,bb,m),i);equal(expected(ll,bb,m),l);
                check(IndexMaskCompactor.selectedCount(bb,m)==i.length);
                check(i!=ii&&l!=ll);
            }
        }
        check(IndexMaskCompactor.selectedCount(new byte[0],0)==0);
        equal(new int[0],IndexMaskCompactor.select(new int[0],new byte[0],0,backend));
        equal(new long[0],IndexMaskCompactor.selectLong(new long[0],new byte[0],0));
        fail(NullPointerException.class,()->IndexMaskCompactor.select(null,new byte[0],0,backend));
        fail(NullPointerException.class,()->IndexMaskCompactor.select(new int[0],null,0,backend));
        fail(NullPointerException.class,()->IndexMaskCompactor.select(new int[0],new byte[0],0,null));
        fail(IllegalArgumentException.class,()->IndexMaskCompactor.select(new int[1],new byte[0],0,backend));
        fail(IllegalArgumentException.class,()->IndexMaskCompactor.select(new int[0],new byte[0],-1,backend));
        fail(IllegalArgumentException.class,()->IndexMaskCompactor.selectLong(new long[0],new byte[0],256));
        fail(NullPointerException.class,()->IndexMaskCompactor.selectLong(new long[0],null,0));
        fail(NullPointerException.class,()->IndexMaskCompactor.selectedCount(null,0));
        // Native-required must still fail closed if the optional binary was not loaded.
        if(!IndexMaskCompactor.nativeAvailable()) {
            fail(UnsatisfiedLinkError.class,()->IndexMaskCompactor.select(ints,states,0,IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("PASS M3MaskDenseSelectionProof checks="+checks+" nativeAvailable="+IndexMaskCompactor.nativeAvailable());
    }
    private static byte[] filled(byte b,int n) {byte[] r=new byte[n];Arrays.fill(r,b);return r;}
}
