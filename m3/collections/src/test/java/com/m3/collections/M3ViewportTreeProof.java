/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

/** Actual folded viewport owners + existing mask selector; fixtures are caller-owned items. */
public final class M3ViewportTreeProof {
    private static long checks, digest = 53;
    private M3ViewportTreeProof() { }
    private static void equal(long wanted, long actual) {
        checks++; if (wanted != actual) throw new AssertionError(wanted + " != " + actual + " @" + checks);
        digest = digest * 31 + actual;
    }
    private static void check(boolean value) { equal(1, value ? 1 : 0); }
    private static final class Row implements M3RowTreeItem {
        final int id;
        int height, slot = -1, last = -1, reads, entered, left;
        boolean visible;
        Row(int id, int height, boolean visible) { this.id=id; this.height=height; this.visible=visible; }
        @Override public int getRowIndex() { return last; }
        @Override public int rowTreeSlot() { return slot; }
        @Override public void enteredRowTree(int value) { slot=value; entered++; }
        @Override public void leftRowTree(int value) { slot=-1; last=value; left++; }
        @Override public boolean isVisible() { reads++; return visible; }
        @Override public int getHeight() { reads++; return height; }
    }
    private static int extent(Row row) { return row.visible ? row.height+1 : 0; }
    private static void verify(M3RowTree tree, List<Row> rows) {
        long total=0; int visible=0;
        equal(rows.size(),tree.size());
        for (int i=0;i<rows.size();i++) {
            Row row=rows.get(i); int weight=extent(row);
            check(tree.get(i)==row); equal(i,tree.indexOf(row)); equal(i,tree.lastIndexOf(row));
            equal(total,tree.extentBefore(i)); equal(visible,tree.rankOf(i)); equal(weight,tree.extentAt(i));
            if (weight>0) {
                equal(i,tree.positionOfRank(visible)); equal(total,tree.extentOfRank(visible));
                equal(visible,tree.rankAtExtent(total)); equal(visible,tree.rankAtExtent(total+weight-1L));
                visible++;total+=weight;
            }
        }
        equal(total,tree.totalExtent());equal(visible,tree.visibleCount());equal(total,tree.extentOfRank(visible));
        int reads=0;for(Row row:rows) reads+=row.reads;
        for(long offset:new long[]{Long.MIN_VALUE,-1,0,total/2,total==0?0:total-1,total,total+1,Long.MAX_VALUE}) {
            int rank=offset<0?-1:0;long prefix=0;
            if(offset>=0)for(Row row:rows) {
                int e=extent(row);if(e==0)continue;
                if(prefix+e>offset)break;prefix+=e;rank++;
            }
            equal(rank,tree.rankAtExtent(offset));
        }
        int after=0;for(Row row:rows)after+=row.reads;equal(reads,after);
        if(!rows.isEmpty()) {
            List<M3RowTreeItem> selected=List.of(rows.get(rows.size()-1),rows.get(0),rows.get(0));
            check(Arrays.equals(new int[]{rows.size()-1,0,0},tree.rowsOf(selected)));
        }
    }
    private static void geometry() {
        for(int size:new int[]{0,1,2,3,7,31,64,129})for(int profile=0;profile<5;profile++) {
            var tree=new M3RowTree();var rows=new ArrayList<Row>();
            for(int i=0;i<size;i++) {
                Row row=new Row(i,i%7==0?Integer.MAX_VALUE-1:i%53,profile!=0&&(profile==1||i%profile!=0));
                int at=(i*17)%(rows.size()+1);rows.add(at,row);tree.add(at,row);
            }
            verify(tree,rows);
            for(int k=0;k<Math.min(size,13);k++) {
                int at=(k*11)%rows.size();Row row=rows.get(at);
                row.visible=!row.visible;row.height=k*31;tree.update(row);verify(tree,rows);
            }
            if(!rows.isEmpty()) {
                Row replacement=new Row(900,0,true);Row old=rows.set(rows.size()/2,replacement);
                check(tree.set(rows.size()/2,replacement)==old);equal(-1,old.slot);verify(tree,rows);
                var cursor=tree.iterator();Row extra=new Row(1000,9,true);
                tree.add(0,extra);rows.add(0,extra);
                try { cursor.next();throw new AssertionError("lost fail-fast"); }
                catch(ConcurrentModificationException expected) {checks++;}
                verify(tree,rows);
            }
            while(!rows.isEmpty()) {
                int at=rows.size()/2;Row old=rows.remove(at);check(tree.remove(at)==old);
                equal(-1,old.slot);equal(at,old.last);
                if(rows.size()<6 || rows.size()%17==0)verify(tree,rows);
            }
            verify(tree,rows);Row reused=new Row(2000,1,true);tree.add(reused);rows.add(reused);verify(tree,rows);
            tree.clear();equal(-1,reused.slot);equal(0,reused.last);rows.clear();verify(tree,rows);
        }
    }
    private static void unusualHeights() {
        // A blanket offset>=sum shortcut changes this legacy answer: total is five,
        // but the first row covers offset five before a later negative row weight.
        var trap=new M3RowTree();trap.add(new Row(0,9,true));trap.add(new Row(1,-21,true));trap.add(new Row(2,14,true));
        equal(5,trap.totalExtent());equal(0,trap.rankAtExtent(5));
        int[] heights={-21,-1,0,9,14,Integer.MAX_VALUE,Integer.MAX_VALUE-1};
        for(int a:heights)for(int b:heights)for(int c:heights)for(int visibility=0;visibility<8;visibility++) {
            var tree=new M3RowTree();int[] h={a,b,c};
            for(int i=0;i<3;i++)tree.add(new Row(i,h[i],(visibility&(1<<i))!=0));
            long total=tree.totalExtent();
            // Compare the emitted fingerprint across original and candidate, rather
            // than impose monotonic-prefix semantics on negative legacy weights.
            for(long q:new long[]{-1,0,5,10,total,Long.MAX_VALUE}) {
                int answer=tree.rankAtExtent(q);checks++;digest=digest*31+answer;
            }
        }
    }
    private static void projection(M3IndexMaskCompactor.Backend backend) {
        for(int count:new int[]{0,1,2,7,31,64,65,257}) {
            Row[] owners=new Row[count];long[] ordered=new long[count];
            for(int i=0;i<count;i++) {
                owners[i]=new Row(i,i%19,true);
                // Stable numeric ordering by repeated key, original occurrence breaks ties.
                ordered[i]=((long)(i%5)<<32)|(i&0xffffffffL);
            }
            Arrays.sort(ordered);int[] order=new int[count];
            for(int i=0;i<count;i++)order[i]=(int)ordered[i];
            var tree=new M3RowTree();var model=new ArrayList<Row>();
            for(int i:order){tree.add(owners[i]);model.add(owners[i]);}
            var plan=new M3WindowPlan();
            for(int required:new int[]{0,1,3,128,255})for(int excluded:new int[]{0,2,128}) {
                byte[] states=new byte[count];
                for(int i=0;i<count;i++)states[i]=(byte)(owners[order[i]].id*37+11);
                byte[] copy=states.clone();
                int[] javaPositions=M3IndexMaskCompactor.selectPositions(states,required,excluded,M3IndexMaskCompactor.Backend.JAVA);
                int[] chosen=M3IndexMaskCompactor.selectPositions(states,required,excluded,backend);
                check(Arrays.equals(javaPositions,chosen));check(Arrays.equals(states,copy));
                boolean[] admitted=new boolean[count];int scalarCount=0;
                for(int i=0;i<count;i++) {
                    int flags=states[i]&255;
                    admitted[i]=(flags&required)==required&&(flags&excluded)==0;
                    if(admitted[i])equal(i,chosen[scalarCount++]);
                    Row row=owners[order[i]];row.visible=admitted[i];tree.update(row);
                }
                equal(scalarCount,chosen.length);equal(count,tree.size());verify(tree,model);
                int visible=chosen.length;
                for(int first=0;first<=visible;first+=Math.max(1,visible/7)) {
                    int length=Math.min(9,visible-first);plan.plan(first,length);plan.commit();
                    equal(length,plan.size());
                    for(int nativeIndex=0;nativeIndex<length;nativeIndex++) {
                        int rank=plan.rowOf(nativeIndex);equal(nativeIndex,plan.entryOf(rank));
                        int display=tree.positionOfRank(rank);equal(chosen[rank],display);
                        check(tree.get(display)==owners[order[display]]);
                    }
                }
            }
            tree.clear();for(Row row:owners)equal(-1,row.slot);
        }
        // Native duplicate row IDs remain occurrences; they are not distinct row-tree items.
        int[] rows={Integer.MIN_VALUE,4,4,Integer.MAX_VALUE};byte[] flags={(byte)255,0,(byte)128,1};
        check(Arrays.equals(M3IndexMaskCompactor.select(rows,flags,128,backend),new int[]{Integer.MIN_VALUE,4}));
    }
    public static void main(String[] args) {
        boolean nativeMode=args.length>0&&args[0].equals("native");
        if(nativeMode && !M3IndexMaskCompactor.nativeAvailable())throw new AssertionError("native library required");
        geometry();unusualHeights();projection(nativeMode?M3IndexMaskCompactor.Backend.NATIVE_REQUIRED:M3IndexMaskCompactor.Backend.JAVA);
        System.out.println("VIEWPORT_TREE_PASS checks="+checks+" digest="+Long.toUnsignedString(digest));
    }
}
