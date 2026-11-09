/* *****************************************************************************
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
package jdk.internal.m3.collections.mat;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatAdaptiveIntSet.java}
 * blob {@code 0324f969db705597649cbb845a4d87350f6093a5} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAdaptiveIntSet{
    public enum Encoding{SORTED_ARRAY,RUNS,BITMAP}
    private final int universe,cardinality;private final Encoding encoding;private final int[] ints;private final long[] words;private final MatCollectionPlan plan;
    private MatAdaptiveIntSet(int universe,int cardinality,Encoding encoding,int[] ints,long[] words,MatCollectionPlan plan){this.universe=universe;this.cardinality=cardinality;this.encoding=encoding;this.ints=ints;this.words=words;this.plan=plan;}
    public static MatAdaptiveIntSet of(int universeExclusive,int... source){
        if(universeExclusive<0)throw new IllegalArgumentException("negative universe");Objects.requireNonNull(source,"source");
        int[] sorted=source.clone();Arrays.sort(sorted);
        int unique=deduplicate(sorted,universeExclusive);
        return fromSortedOwned(universeExclusive,sorted,unique);
    }
    /** Internal only: the owned prefix is already validated, strictly increasing and distinct. */
    private static MatAdaptiveIntSet fromSortedOwned(int universeExclusive,int[] sorted,int size){
        int runs=countRuns(sorted,size);long arrayBytes=4L*size,runBytes=8L*runs,bitmapBytes=8L*((universeExclusive+63L)>>>6);
        if(arrayBytes<=runBytes&&arrayBytes<=bitmapBytes){
            int[] retained=size==sorted.length?sorted:Arrays.copyOf(sorted,size);
            return new MatAdaptiveIntSet(universeExclusive,size,Encoding.SORTED_ARRAY,retained,null,new MatCollectionPlan(MatStorageKind.SET_SORTED_ARRAY,arrayBytes,size,universeExclusive,"sorted int[]"));
        }
        if(runBytes<=bitmapBytes){
            int[] pairs=new int[runs*2];int out=0;
            for(int i=0;i<size;){int start=sorted[i],j=i+1;while(j<size&&(long)sorted[j]==(long)sorted[j-1]+1L)j++;pairs[out++]=start;pairs[out++]=j-i;i=j;}
            return new MatAdaptiveIntSet(universeExclusive,size,Encoding.RUNS,pairs,null,new MatCollectionPlan(MatStorageKind.SET_RUNS,runBytes,size,universeExclusive,"start,length int pairs; runs="+runs));
        }
        // Use long arithmetic for ceil(U/64); int U+63 would overflow near MAX_VALUE.
        long[] words=new long[(int)((universeExclusive+63L)>>>6)];
        for(int i=0;i<size;i++){int value=sorted[i];words[value>>>6]|=1L<<(value&63);}
        return new MatAdaptiveIntSet(universeExclusive,size,Encoding.BITMAP,null,words,new MatCollectionPlan(MatStorageKind.SET_BITMAP,bitmapBytes,size,universeExclusive,"finite-universe long[] bitmap"));
    }
    public int universe(){return universe;}public int size(){return cardinality;}public boolean isEmpty(){return cardinality==0;}public Encoding encoding(){return encoding;}
    public MatCollectionPlan plan(){return plan;}public long primitivePayloadBytes(){return plan.primitivePayloadBytes();}
    public boolean contains(int value){if(value<0||value>=universe)return false;return switch(encoding){case SORTED_ARRAY->Arrays.binarySearch(ints,value)>=0;case BITMAP->(words[value>>>6]&(1L<<(value&63)))!=0;case RUNS->containsRun(value);};}
    public int[] toArray(){return switch(encoding){case SORTED_ARRAY->ints.clone();case RUNS->expandRuns();case BITMAP->expandBitmap();};}
    public IntStream intStream(){return Arrays.stream(toArray());}
    public MatAdaptiveIntSet union(MatAdaptiveIntSet other){return combine(other,0);}public MatAdaptiveIntSet intersect(MatAdaptiveIntSet other){return combine(other,1);}
    public MatAdaptiveIntSet xor(MatAdaptiveIntSet other){return combine(other,2);}public MatAdaptiveIntSet minus(MatAdaptiveIntSet other){return combine(other,3);}
    public MatAdaptiveIntSet complement(){int[] r=new int[universe-cardinality];int p=0;for(int i=0;i<universe;i++)if(!contains(i))r[p++]=i;return of(universe,r);}
    private MatAdaptiveIntSet combine(MatAdaptiveIntSet other,int op){
        Objects.requireNonNull(other,"other");if(universe!=other.universe)throw new IllegalArgumentException("different universes");
        int[] a=toArray(),b=other.toArray();
        int capacity=switch(op){case 1->Math.min(a.length,b.length);case 3->a.length;default->Math.addExact(a.length,b.length);};
        int[] temp=new int[capacity];int i=0,j=0,out=0;
        while(i<a.length||j<b.length){
            int av=i<a.length?a[i]:Integer.MAX_VALUE,bv=j<b.length?b[j]:Integer.MAX_VALUE;
            if(i<a.length&&(j>=b.length||av<bv)){if(op==0||op==2||op==3)temp[out++]=av;i++;}
            else if(j<b.length&&(i>=a.length||bv<av)){if(op==0||op==2)temp[out++]=bv;j++;}
            else{if(op==0||op==1)temp[out++]=av;i++;j++;}
        }
        // The two-pointer merge already produced a sorted unique owned prefix.
        return fromSortedOwned(universe,temp,out);
    }
    private boolean containsRun(int value){int lo=0,hi=ints.length/2-1;while(lo<=hi){int mid=(lo+hi)>>>1,start=ints[mid*2],length=ints[mid*2+1];if(value<start)hi=mid-1;else if((long)value>=(long)start+length)lo=mid+1;else return true;}return false;}
    private int[] expandRuns(){int[] r=new int[cardinality];int out=0;for(int i=0;i<ints.length;i+=2)for(int j=0;j<ints[i+1];j++)r[out++]=ints[i]+j;return r;}
    private int[] expandBitmap(){int[] r=new int[cardinality];int out=0;for(int word=0;word<words.length;word++){long bits=words[word];while(bits!=0){int bit=Long.numberOfTrailingZeros(bits),value=(word<<6)+bit;if(value<universe)r[out++]=value;bits&=bits-1;}}return r;}
    private static int deduplicate(int[] sorted,int universe){int out=0,previous=-1;for(int value:sorted){if(value<0||value>=universe)throw new IllegalArgumentException("set value outside universe: "+value);if(out==0||value!=previous)sorted[out++]=value;previous=value;}return out;}
    private static int countRuns(int[] sorted,int size){if(size==0)return 0;int runs=1;for(int i=1;i<size;i++)if((long)sorted[i]!=(long)sorted[i-1]+1L)runs++;return runs;}
}
