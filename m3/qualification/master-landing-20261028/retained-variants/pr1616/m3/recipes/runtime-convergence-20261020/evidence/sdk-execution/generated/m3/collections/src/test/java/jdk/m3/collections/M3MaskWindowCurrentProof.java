// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Original synthetic oracle; executes actual collection and JNI owners, never API stand-ins. */
public final class M3MaskWindowCurrentProof {
    private static long checks;
    private static final MessageDigest DIGEST = digest();
    private static final M3IndexMaskCompactor.Backend JAVA = M3IndexMaskCompactor.Backend.JAVA;
    private static final M3IndexMaskCompactor.Backend NATIVE = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
    private M3MaskWindowCurrentProof() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !List.of("java", "native", "missing", "legacy").contains(args[0])) {
            throw new IllegalArgumentException("java|native|missing|legacy");
        }
        boolean nativeMode = args[0].equals("native") || args[0].equals("legacy");
        if (nativeMode) yes(M3IndexMaskCompactor.nativeAvailable(), "required binary missing");
        if (args[0].equals("missing")) {
            yes(!M3IndexMaskCompactor.nativeAvailable(), "unexpected native binary");
            for (int mask : new int[]{0, 1, 128, 255}) {
                refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.select(new int[0], new byte[0], mask, NATIVE));
                refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectWindow(new int[0], 0, new byte[0], 0, 0, mask, NATIVE));
                refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectLongWindow(new long[0], 0, new byte[0], 0, 0, mask, NATIVE));
                refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectPositions(new byte[0], mask, 128, NATIVE));
                refuse(UnsatisfiedLinkError.class, () -> M3LongIndexProjection.snapshot(new long[0]).filtered(new byte[0], mask, NATIVE));
            }
            System.out.println("M3_MISSING_NATIVE checks=" + checks + " PASS"); return;
        }
        boolean legacy = args[0].equals("legacy");
        masks(nativeMode && !legacy);
        windows(nativeMode);
        projections(nativeMode);
        invalid(nativeMode);
        parallel(nativeMode);
        if (legacy) {
            // Existing binary lacks only the additive excluded-mask methods. No Java fallback.
            refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectPositions(new byte[]{1}, 1, 2, NATIVE));
            refuse(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectPositions(new byte[0], 1, 1, NATIVE));
            same(new int[]{0}, M3IndexMaskCompactor.selectPositions(new byte[]{1}, 1, 0, NATIVE));
        }
        System.out.println("M3_MASK_CURRENT mode=" + args[0] + " checks=" + checks
                + " digest=" + HexFormat.of().formatHex(DIGEST.digest()) + " PASS");
    }

    private static void masks(boolean nativeMode) {
        byte[] states = new byte[256];
        for (int i = 0; i < 256; i++) states[i] = (byte)i;
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                int[] expected = positions(states, required, excluded);
                same(expected, M3IndexMaskCompactor.selectPositions(states, required, excluded, JAVA));
                yes(expected.length == M3IndexMaskCompactor.selectedCount(states, required, excluded), "count");
                if (nativeMode) same(expected, M3IndexMaskCompactor.selectPositions(states, required, excluded, NATIVE));
            }
        }
        for (int i = 0; i < 256; i++) yes(states[i] == (byte)i, "state input mutation");
    }

    private static void windows(boolean nativeMode) {
        Random rng = new Random(20261020);
        for (int size : new int[]{0,1,2,63,64,65,255,256,257,511,512,513,1024,1025,4097}) {
            int[] rows = new int[size + 17]; long[] longs = new long[rows.length];
            byte[] states = new byte[size + 31]; rng.nextBytes(states);
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i % 7 == 0 ? Integer.MIN_VALUE : i % 11 == 0 ? Integer.MAX_VALUE : (i % 19) - 9;
                longs[i] = i % 7 == 0 ? Long.MIN_VALUE : i % 11 == 0 ? Long.MAX_VALUE : ((long)rows[i] << 35);
            }
            for (int mask = 0; mask < 256; mask++) {
                int ro = mask % 18, so = mask % 32;
                byte[] marks = Arrays.copyOfRange(states, so, so + size);
                int[] selection = positions(marks, mask, 0);
                int[] expected = new int[selection.length]; long[] expectedLong = new long[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    expected[i] = rows[ro + selection[i]]; expectedLong[i] = longs[ro + selection[i]];
                }
                same(expected, M3IndexMaskCompactor.selectWindow(rows, ro, states, so, size, mask, JAVA));
                same(expectedLong, M3IndexMaskCompactor.selectLongWindow(longs, ro, states, so, size, mask, JAVA));
                same(expected, M3IndexMaskCompactor.select(Arrays.copyOfRange(rows,ro,ro+size), marks, mask, JAVA));
                same(expectedLong, M3IndexMaskCompactor.selectLong(Arrays.copyOfRange(longs,ro,ro+size), marks, mask));
                if (nativeMode) {
                    same(expected, M3IndexMaskCompactor.selectWindow(rows, ro, states, so, size, mask, NATIVE));
                    same(expectedLong, M3IndexMaskCompactor.selectLongWindow(longs, ro, states, so, size, mask, NATIVE));
                    same(expected, M3IndexMaskCompactor.selectNativeRange(rows, ro, size, marks, mask));
                    same(expected, M3IndexMaskCompactor.select(Arrays.copyOfRange(rows,ro,ro+size), marks, mask, NATIVE));
                }
            }
            // Empty, all, and last-row-only selection at native page seams.
            for (int density = 0; density < 3; density++) {
                byte[] marks = new byte[size];
                if (density == 1) Arrays.fill(marks, (byte)128);
                if (density == 2 && size > 0) marks[size - 1] = (byte)128;
                int[] expected = Arrays.stream(positions(marks,128,0)).map(i -> rows[i]).toArray();
                same(expected, M3IndexMaskCompactor.selectWindow(rows,0,marks,0,size,128,JAVA));
                if (nativeMode) same(expected,M3IndexMaskCompactor.selectWindow(rows,0,marks,0,size,128,NATIVE));
            }
        }
        int[] rows = {4,4,-1}; byte[] marks = {1,1,1}; long[] values = {4,4,-1};
        for (var backend : nativeMode ? new M3IndexMaskCompactor.Backend[]{JAVA,NATIVE} : new M3IndexMaskCompactor.Backend[]{JAVA}) {
            int[] got=M3IndexMaskCompactor.select(rows,marks,0,backend); yes(got!=rows,"detached identity"); got[0]=99;yes(rows[0]==4,"input alias");
            long[] out=M3IndexMaskCompactor.selectLongWindow(values,0,marks,0,3,1,backend);yes(out!=values,"detached long");out[0]=99;yes(values[0]==4,"long alias");
        }
    }

    private static void projections(boolean nativeMode) {
        Random random = new Random(12012026);
        for (int scenario=0;scenario<400;scenario++) {
            long[] values=new long[scenario%129];
            for(int i=0;i<values.length;i++) values[i]=i%29==0?Long.MIN_VALUE:i%31==0?Long.MAX_VALUE:random.nextInt(17)-8;
            M3LongIndexProjection actual=M3LongIndexProjection.snapshot(values);
            List<Integer> model=new ArrayList<>();for(int i=0;i<values.length;i++)model.add(i);
            for(int pass=0;pass<12;pass++) {
                switch(pass%4) {
                    case 0 -> {actual=actual.sortedAscending();model.sort(Comparator.comparingLong(i->values[i]));}
                    case 1 -> {actual=actual.sortedDescending();model.sort((a,b)->Long.compare(values[b],values[a]));}
                    case 2 -> {
                        int offset=random.nextInt(actual.size()+1);int length=random.nextInt(actual.size()-offset+1);
                        actual=actual.slice(offset,length);model=new ArrayList<>(model.subList(offset,offset+length));
                    }
                    default -> {
                        byte[] marks=new byte[actual.size()];random.nextBytes(marks);int mask=pass%3==0?0:128;
                        M3LongIndexProjection javaView=actual.filtered(marks,mask,JAVA);
                        if(nativeMode) {
                            M3LongIndexProjection nativeView=actual.filtered(marks,mask,NATIVE);
                            same(javaView.physicalIndices(),nativeView.physicalIndices());same(javaView.toArray(),nativeView.toArray());
                        }
                        actual=javaView;List<Integer> selected=new ArrayList<>();
                        for(int i=0;i<marks.length;i++)if(((marks[i]&255)&mask)==mask)selected.add(model.get(i));
                        model=selected;
                    }
                }
                same(model.stream().mapToInt(Integer::intValue).toArray(),actual.physicalIndices());
                long[] expected=new long[model.size()];for(int i=0;i<expected.length;i++)expected[i]=values[model.get(i)];
                same(expected,actual.toArray());
                for(int i=0;i<expected.length;i++)yes(actual.get(i)==expected[i] && actual.physicalIndex(i)==model.get(i),"indexed access");
            }
        }
    }

    private static void invalid(boolean nativeMode) {
        for(var b:nativeMode?new M3IndexMaskCompactor.Backend[]{JAVA,NATIVE}:new M3IndexMaskCompactor.Backend[]{JAVA}) {
            refuse(NullPointerException.class,()->M3IndexMaskCompactor.select(null,null,-1,b));
            refuse(NullPointerException.class,()->M3IndexMaskCompactor.select(new int[0],null,-1,b));
            refuse(IllegalArgumentException.class,()->M3IndexMaskCompactor.select(new int[1],new byte[0],0,b));
            for(int invalid:new int[]{-1,256,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
                refuse(IllegalArgumentException.class,()->M3IndexMaskCompactor.select(new int[0],new byte[0],invalid,b));
                refuse(IllegalArgumentException.class,()->M3IndexMaskCompactor.selectPositions(new byte[0],0,invalid,b));
                refuse(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectWindow(new int[1],invalid,new byte[1],0,1,invalid,b));
                refuse(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectLongWindow(new long[1],0,new byte[1],invalid,1,0,b));
            }
            refuse(IndexOutOfBoundsException.class,()->M3IndexMaskCompactor.selectWindow(new int[1],1,new byte[1],0,Integer.MAX_VALUE,0,b));
            refuse(NullPointerException.class,()->M3IndexMaskCompactor.selectPositions(new byte[0],0,null));
        }
    }

    private static void parallel(boolean nativeMode) throws Exception {
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new ArrayList<Callable<Boolean>>();
            for(int task=0;task<16;task++)tasks.add(()->{
                int[] rows={7,-2,7,9};byte[] states={1,0,1,(byte)128};
                for(int i=0;i<500;i++) {
                    if(!Arrays.equals(new int[]{7,7},M3IndexMaskCompactor.select(rows,states,1,JAVA)))return false;
                    if(nativeMode && !Arrays.equals(new int[]{9},M3IndexMaskCompactor.select(rows,states,128,NATIVE)))return false;
                }return true;
            });
            for(var future:executor.invokeAll(tasks))yes(future.get(),"concurrent independent calls");
        }
    }
    private static int[] positions(byte[] states,int required,int excluded) {
        int[] tmp=new int[states.length];int n=0;
        for(int i=0;i<states.length;i++){int bits=states[i]&255;if((bits&required)==required&&(bits&excluded)==0)tmp[n++]=i;}
        return Arrays.copyOf(tmp,n);
    }
    private static void same(int[] a,int[] b){checks++;if(!Arrays.equals(a,b))throw new AssertionError("int sequence");for(int v:b)DIGEST.update(ByteBuffer.allocate(4).putInt(v).array());}
    private static void same(long[] a,long[] b){checks++;if(!Arrays.equals(a,b))throw new AssertionError("long sequence");for(long v:b)DIGEST.update(ByteBuffer.allocate(8).putLong(v).array());}
    private static void yes(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void refuse(Class<? extends Throwable> type,Runnable action){checks++;try{action.run();}catch(Throwable failure){if(type.isInstance(failure))return;throw new AssertionError(failure);}throw new AssertionError("missing refusal "+type);}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
}
