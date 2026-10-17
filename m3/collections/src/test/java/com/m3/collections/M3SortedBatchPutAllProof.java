// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/** Independent JDK TreeMap differential and JNI contract for sorted filtered putAll. */
public final class M3SortedBatchPutAllProof {
    private static long checks;
    private static boolean nativeEnabled;
    private static final Comparator<Key> ORDER =
            Comparator.comparing(k -> k.text, String.CASE_INSENSITIVE_ORDER);

    private static final class Key {
        final String text;
        final boolean visible;
        Key(String text, boolean visible) { this.text=text; this.visible=visible; }
    }
    private static native void nativePutAll(Map<?,?> target, Map<?,?> incoming);

    private static void check(boolean pass, String description) {
        checks++;
        if (!pass) throw new AssertionError(description + " checks=" + checks);
    }
    private static void refusal(Runnable operation, String description) {
        try { operation.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(description);
    }
    private static void putAll(Map<Key,Integer> target, Map<Key,Integer> incoming) {
        if (nativeEnabled) nativePutAll(target,incoming);
        else target.putAll(incoming);
    }
    private static Map<Key,Integer> view(NavigableMap<Key,Integer> source) {
        return M3WritableFilteredMaps.filtered((Map<Key,Integer>)source,(key,value)->key.visible);
    }
    private static void compare(NavigableMap<Key,Integer> actual, NavigableMap<Key,Integer> expected) {
        check(actual.size()==expected.size(),"size");
        var a=actual.entrySet().iterator();
        var b=expected.entrySet().iterator();
        while(a.hasNext()) {
            var one=a.next();var two=b.next();
            check(one.getKey()==two.getKey(),"retained key identity");
            check(Objects.equals(one.getValue(),two.getValue()),"value equality");
        }
    }
    private static void reference(NavigableMap<Key,Integer> source, Map<Key,Integer> incoming) {
        // Build a test-only JDK shadow, NOT part of production collection storage.
        NavigableMap<Key,Integer> projected=new TreeMap<>(source.comparator());
        projected.putAll(source);
        for (var e:incoming.entrySet()) {
            Key candidate=e.getKey();
            Key retained=projected.containsKey(candidate)?projected.ceilingKey(candidate):candidate;
            if (!retained.visible) throw new IllegalArgumentException("rejected");
            projected.put(candidate,e.getValue());
        }
        source.putAll(incoming);
    }
    private static void witnesses() {
        Key first=new Key("Alpha",true);
        Key alias=new Key("ALPHA",false);
        var source=new TreeMap<Key,Integer>(ORDER);
        Map<Key,Integer> incoming=new LinkedHashMap<>();
        incoming.put(first,1);incoming.put(alias,2);
        try {
            putAll(view(source),incoming);
        } catch (IllegalArgumentException originalBug) {
            throw new AssertionError("M3_BATCH_EXPECTED_STORED previously rejected", originalBug);
        }
        check(source.size()==1 && source.firstKey()==first && source.get(first)==2,
                "first staged key retained and last value published");
        source.clear();incoming.clear();incoming.put(alias,1);incoming.put(first,2);
        refusal(()->putAll(view(source),incoming),"reject invisible first");
        check(source.isEmpty(),"rejected batch publishes nothing");
        source.put(first,10);incoming.clear();incoming.put(alias,11);
        putAll(view(source),incoming);
        check(source.firstKey()==first && source.get(first)==11,"existing stored key");
        source.clear();source.put(alias,10);incoming.clear();incoming.put(first,11);
        refusal(()->putAll(view(source),incoming),"hidden existing key cannot be exposed");
        check(source.firstKey()==alias && source.get(alias)==10,"hidden owner survives");
        var nullable=new TreeMap<Key,Integer>(Comparator.nullsFirst(ORDER));
        Map<Key,Integer> oneNull=new LinkedHashMap<>();oneNull.put(null,4);
        putAll(M3WritableFilteredMaps.filtered((Map<Key,Integer>)nullable,(k,v)->k==null),oneNull);
        check(nullable.containsKey(null) && nullable.get(null)==4,"null comparator key");
        var hash=new HashMap<Key,Integer>();
        Map<Key,Integer> singleton=new LinkedHashMap<>();singleton.put(first,12);
        putAll(M3WritableFilteredMaps.filtered((Map<Key,Integer>)hash,(k,v)->k.visible),singleton);
        check(hash.size()==1 && hash.get(first)==12,"generic hash path unchanged");
    }
    private static void differential() {
        var random=new Random(0x5a17baceL);
        for (int round=0;round<128;round++) {
            var left=new TreeMap<Key,Integer>(ORDER);
            var right=new TreeMap<Key,Integer>(ORDER);
            for (int i=0;i<40;i++) if (random.nextBoolean()) {
                Key key=new Key("key"+i,random.nextBoolean());
                left.put(key,i);right.put(key,i);
            }
            var incoming=new LinkedHashMap<Key,Integer>();
            for (int i=0;i<240;i++) {
                int row=random.nextInt(70);
                incoming.put(new Key(i%3==0?"KEY"+row:"key"+row,random.nextBoolean()),
                        random.nextInt());
            }
            boolean a=true,b=true;
            try { putAll(view(left),incoming); } catch (IllegalArgumentException reject) {a=false;}
            try { reference(right,incoming); } catch (IllegalArgumentException reject) {b=false;}
            check(a==b,"same admission as independent sequential JDK oracle");
            compare(left,right);
        }
    }
    private static void scale() {
        var source=new TreeMap<Key,Integer>(ORDER);
        var incoming=new LinkedHashMap<Key,Integer>();
        for(int i=0;i<4096;i++)incoming.put(new Key(String.format("%05d",i),true),i);
        putAll(view(source),incoming);
        check(source.size()==4096,"large batch inserts");
        var aliases=new LinkedHashMap<Key,Integer>();
        for(int i=0;i<4096;i+=4)aliases.put(new Key(String.format("%05d",i),false),i*2);
        putAll(view(source),aliases);
        check(source.size()==4096,"alias batch retains first owners");
        for(int i=0;i<4096;i+=4)
            check(source.get(new Key(String.format("%05d",i),false))==i*2,
                    "comparator alias value replacement");
    }
    /**
     * Adversarial 65,536-key AVL proof, including ascending/random orders.
     * The index stores pair coordinates, not per-element entry objects.
     */
    private static void largePrimitiveIndex() {
        final int size=65536;
        Object[] pairs=new Object[size*2];
        int[] permutation=new int[size];
        for(int i=0;i<size;i++) permutation[i]=i;
        var random=new Random(0x5a17a11L);
        for(int i=size-1;i>0;i--) {
            int j=random.nextInt(i+1);
            int value=permutation[i];permutation[i]=permutation[j];permutation[j]=value;
        }
        var index=new M3PendingSortedKeys<Integer>(null);
        for(int i=0;i<size;i++) {
            Integer key=permutation[i];
            pairs[i*2]=key;pairs[i*2+1]=i;
            check(index.firstOffset(pairs,key)==-1,"new unique key");
            index.recordFirst(pairs,i*2);
        }
        for(int i=0;i<size;i++)
            check(index.firstOffset(pairs,permutation[i])==i*2,"stable first key position");
    }

    public static void main(String[] args) {
        if(args.length==1){System.load(args[0]);nativeEnabled=true;}
        witnesses();differential();scale();largePrimitiveIndex();
        System.out.println("M3_SORTED_BATCH_PUTALL_RUNTIME_PASS checks="+checks+
                " native="+nativeEnabled);
    }
}
