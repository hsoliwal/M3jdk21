// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Finite deterministic Java21/real-JNI parity contract for a bounded frame. */
public final class M3FrameJavaContract {
    private static long checks;
    private static long digest = 0x5bd1e995L;

    private M3FrameJavaContract() { }

    private static void check(boolean ok, String where) {
        ++checks;
        if (!ok) throw new AssertionError(where + " check=" + checks);
    }

    private static boolean matches(int flags, int required, int forbidden) {
        for (int bit=0; bit<Integer.SIZE; bit++) {
            int b=1<<bit;
            if ((required & b)!=0 && (flags & b)==0) return false;
            if ((forbidden & b)!=0 && (flags & b)!=0) return false;
        }
        return true;
    }

    private static int reference(int[] ids, int[] flags, int first, int length,
            int required, int forbidden, int skip, int max,
            int[] rows, int rowOffset, int[] slots, int slotOffset,
            int[] words, int wordOffset) {
        if (ids == null || flags == null || rows == null || slots == null || words == null)
            throw new NullPointerException("null frame lane");
        if (ids.length != flags.length)
            throw new IllegalArgumentException("offset and state array lengths differ");
        if (rows == ids || rows == flags || slots == ids || slots == flags || words == ids || words == flags
                || rows == slots || rows == words || slots == words)
            throw new IllegalArgumentException("frame outputs must be disjoint");
        if (first < 0 || length < 0 || first > ids.length - length || skip < 0 || max < 0)
            throw new IndexOutOfBoundsException("invalid source window");
        int available = 0;
        for (int i=first; i<first+length; ++i) {
            if (ids[i]<0) throw new IllegalArgumentException("negative logical offset");
            if (matches(flags[i],required,forbidden)) ++available;
        }
        int needed = skip >= available ? 0 : Math.min(available-skip,max);
        if (rowOffset<0 || slotOffset<0 || wordOffset<0
                || needed>rows.length || rowOffset>rows.length-needed
                || needed>slots.length || slotOffset>slots.length-needed
                || needed>words.length || wordOffset>words.length-needed)
            throw new IndexOutOfBoundsException("destination capacity");
        int cursor=0,left=skip;
        for(int i=first; i<first+length && cursor<needed; ++i) {
            if(matches(flags[i],required,forbidden)) {
                if(left>0) --left;
                else { rows[rowOffset+cursor]=ids[i]; slots[slotOffset+cursor]=i;
                       words[wordOffset+cursor]=flags[i];cursor++; }
            }
        }
        check(cursor==needed,"reference output filled");
        return needed;
    }

    private static void equal(int[] expect, int[] actual, String label) {
        check(Arrays.equals(expect,actual),label);
        for(int v:actual) digest=(digest*0x100000001b3L) ^ (v & 0xffffffffL);
    }

    private static void scenario(int[] idInput,int[] stateInput, int first,int length,
            int required,int forbidden,int skip,int max, int offset, boolean sameInputs) {
        int[] ids=idInput.clone(), flags=sameInputs?ids:stateInput.clone();
        int expectedLen=Math.min(Math.max(0,length),Math.min(Math.max(0,max),256));
        int outLen=expectedLen+7;
        int[] wantRows=new int[outLen],wantSlots=new int[outLen],wantWords=new int[outLen];
        int[] gotRows=new int[outLen],gotSlots=new int[outLen],gotWords=new int[outLen];
        Arrays.fill(wantRows,-41); Arrays.fill(gotRows,-41);
        Arrays.fill(wantSlots,-51);Arrays.fill(gotSlots,-51);
        Arrays.fill(wantWords,-61);Arrays.fill(gotWords,-61);
        int[] originalIds=ids.clone(),originalStates=flags.clone();
        int expected=reference(ids,flags,first,length,required,forbidden,skip,max,
                wantRows,offset,wantSlots,offset+1,wantWords,offset+2);
        int actual=M3MaskViewportKernel.projectFramePage(ids,flags,first,length,
                required,forbidden,skip,max,gotRows,offset,gotSlots,offset+1,gotWords,offset+2);
        check(actual==expected,"frame count");
        equal(wantRows,gotRows,"frame rows");
        equal(wantSlots,gotSlots,"frame ordinal slots");
        equal(wantWords,gotWords,"frame original state words");
        equal(originalIds,ids,"unchanged input ids");
        equal(originalStates,flags,"unchanged input states");
    }

    private static void refuse(Class<? extends Throwable> kind,String message,
            int[] ids,int[] flags,int first,int length,int required,int forbidden,
            int skip,int max,int[] rows,int rowOffset,int[] slots,int slotOffset,
            int[] words,int wordOffset) {
        int[] originalRows=rows==null?null:rows.clone();
        int[] originalSlots=slots==null?null:slots.clone();
        int[] originalWords=words==null?null:words.clone();
        Throwable actual=null;
        try { M3MaskViewportKernel.projectFramePage(ids,flags,first,length,required,forbidden,skip,max,
                    rows,rowOffset,slots,slotOffset,words,wordOffset); }
        catch(Throwable x){ actual=x; }
        check(actual != null && actual.getClass()==kind,"exception type "+kind+" actual="+actual);
        check(message.equals(actual.getMessage()),"exception text "+message+" got="+actual.getMessage());
        if(rows!=null)equal(originalRows,rows,"failed frame rows unchanged");
        if(slots!=null)equal(originalSlots,slots,"failed frame slots unchanged");
        if(words!=null)equal(originalWords,words,"failed frame words unchanged");
    }

    private static void corpus() {
        Random rng=new Random(0x42a612fL);
        int[] lengths={0,1,2,3,7,15,63,127,128,129,255,256,257,511,512,513};
        int[] required={0,1,2,Integer.MIN_VALUE,-1,Integer.MIN_VALUE|1,0xff,0};
        int[] forbidden={0,2,1,0,0,4,1,Integer.MIN_VALUE};
        for(int trial=0;trial<5000;trial++) {
            int n=lengths[trial%lengths.length];
            int[] ids=new int[n],states=new int[n];
            for(int i=0;i<n;i++) {
                ids[i]=rng.nextInt(200000);
                states[i]=switch((i+trial)%7){case 0->0;case 1->1;case 2->-1;
                    case 3->Integer.MIN_VALUE;case 4->2;case 5->Integer.MIN_VALUE|1;default->rng.nextInt();};
            }
            int first=n==0?0:rng.nextInt(n+1);
            int length=rng.nextInt(n-first+1);
            int mask=trial%required.length;
            int skip=switch(trial%7){case 0->0;case 1->1;case 2->127;case 3->128;
                case 4->Integer.MAX_VALUE;case 5->length/2;default->2;};
            int max=switch(trial%6){case 0->0;case 1->1;case 2->17;case 3->128;
                case 4->Integer.MAX_VALUE;default->3;};
            scenario(ids,states,first,length,required[mask],forbidden[mask],skip,max,1,trial%11==0);
        }
        // n=1,000,000 is never fully copied by JNI: input read windows <=128 ints.
        int n=1_000_000;
        int[] ids=new int[n],states=new int[n];
        for(int i=0;i<n;i++) {ids[i]=i+1;states[i]=(i%71==0)?Integer.MIN_VALUE|1:0;}
        int[][] cases={{0,0,999900,64},{0,0,0,64},{Integer.MIN_VALUE,0,0,64},
            {Integer.MIN_VALUE,Integer.MIN_VALUE,0,64},{1,0,0,64},{0,1,0,64}};
        for(int[] c:cases) {
            int[] r=new int[67],s=new int[68],w=new int[69];
            int[] rr=r.clone(),ss=s.clone(),ww=w.clone();
            int exp=reference(ids,states,0,n,c[0],c[1],c[2],c[3],rr,1,ss,2,ww,3);
            int got=M3MaskViewportKernel.projectFramePage(ids,states,0,n,c[0],c[1],c[2],c[3],r,1,s,2,w,3);
            check(exp==got,"million length");equal(rr,r,"million rows");equal(ss,s,"million slots");equal(ww,w,"million words");
        }
        int[] data={1,2,3,4,5},flags={0,1,0,2,0};
        refuse(NullPointerException.class,"null frame lane",null,null,0,0,0,0,0,0,new int[10],0,new int[10],0,new int[10],0);
        refuse(IllegalArgumentException.class,"offset and state array lengths differ",data,new int[0],0,0,0,0,0,0,new int[10],0,new int[10],0,new int[10],0);
        refuse(IllegalArgumentException.class,"frame outputs must be disjoint",data,flags,0,5,0,0,0,4,data,0,new int[10],0,new int[10],0);
        refuse(IllegalArgumentException.class,"frame outputs must be disjoint",data,flags,0,5,0,0,0,4,new int[10],0,flags,0,new int[10],0);
        refuse(IndexOutOfBoundsException.class,"invalid source window",data,flags,-1,5,0,0,0,4,new int[10],0,new int[10],0,new int[10],0);
        refuse(IndexOutOfBoundsException.class,"invalid source window",data,flags,0,5,0,0,-1,4,new int[10],0,new int[10],0,new int[10],0);
        refuse(IndexOutOfBoundsException.class,"destination capacity",data,flags,0,5,0,0,0,5,new int[3],0,new int[3],0,new int[3],0);
        refuse(IndexOutOfBoundsException.class,"destination capacity",data,flags,0,5,0,0,0,0,new int[3],-1,new int[3],0,new int[3],0);
        int[] late=new int[257];late[256]=-1;
        refuse(IllegalArgumentException.class,"negative logical offset",late,new int[257],0,257,1,1,0,0,new int[10],0,new int[10],0,new int[10],0);
        refuse(IllegalArgumentException.class,"negative logical offset",late,new int[257],0,257,0,0,Integer.MAX_VALUE,0,new int[10],0,new int[10],0,new int[10],0);
    }

    public static void main(String[] args) {
        corpus();
        System.out.println("M3_FRAME_JAVA_PASS checks="+checks+" digest="+Long.toUnsignedString(digest,16));
    }
}
