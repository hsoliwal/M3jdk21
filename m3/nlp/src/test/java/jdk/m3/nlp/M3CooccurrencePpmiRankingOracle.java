// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3CooccurrencePpmi;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Differential/full-sort oracle; no substitute for official JUnit/SDK gates. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-ppmi-topk-20261031/PpmiRankingOracle.java blob 0e3bdfc32989; recipe m3/nlp/recipe/cooccurrence-ppmi-20261106/relocate.py v3
public final class M3CooccurrencePpmiRankingOracle {
    private static long checks;
    private static volatile Object sink;
    private static final String[] TOKENS = {"a","b","c","café","猫","😀","\uD800","\uDC00","\uE000","a b","\0"};
    private M3CooccurrencePpmiRankingOracle() { }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static List<M3CooccurrencePpmi.ContextScore> expected(M3CooccurrencePpmi.Snapshot s, String center, int k) {
        List<M3CooccurrencePpmi.ContextScore> result = new ArrayList<>();
        for (String token : s.vocabulary()) {
            double score = s.ppmi(center, token);
            if (score > 0) result.add(new M3CooccurrencePpmi.ContextScore(token, s.pairCount(center, token), score));
        }
        result.sort(Comparator.comparingDouble(M3CooccurrencePpmi.ContextScore::ppmi).reversed()
                .thenComparing(M3CooccurrencePpmi.ContextScore::context));
        return List.copyOf(result.subList(0,Math.min(k,result.size())));
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--allocation")) { allocation(); return; }
        if (args.length == 1 && args[0].equals("--native-boundary")) { nativeBoundary(); return; }
        Random random = new Random(1467920261031L);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int trial=0; trial<2500; trial++) {
            List<List<String>> docs = new ArrayList<>();
            for (int d=0, n=random.nextInt(10); d<n; d++) {
                List<String> words = new ArrayList<>();
                for (int i=0, n2=random.nextInt(24); i<n2; i++) words.add(TOKENS[random.nextInt(TOKENS.length)]);
                docs.add(words);
            }
            var s = M3CooccurrencePpmi.build(docs,1+random.nextInt(5));
            for (String center : TOKENS) {
                for (int k : new int[]{0,1,2,5,20,Integer.MAX_VALUE}) {
                    var got = s.topPositiveContexts(center,k);
                    require(got.equals(expected(s,center,k)),"rank parity trial="+trial+" k="+k);
                    for (var r : got) {
                        for (char c : r.context().toCharArray()) { digest.update((byte)(c>>>8)); digest.update((byte)c); }
                        digest.update((byte)255);
                        long bits=Double.doubleToLongBits(r.ppmi());
                        for(int shift=56;shift>=0;shift-=8) digest.update((byte)(bits>>>shift));
                    }
                }
            }
            require(s.topPositiveContexts("missing",Integer.MAX_VALUE).isEmpty(),"unknown center");
            try { s.topPositiveContexts(null,-1); throw new AssertionError("null order"); }
            catch (NullPointerException correct) { checks++; }
            try { s.topPositiveContexts("missing",-1); throw new AssertionError("negative order"); }
            catch (IllegalArgumentException correct) { checks++; }
        }
        var s=M3CooccurrencePpmi.build(List.of(List.of("a","b","a","c")),1);
        try { s.topPositiveContexts("a",2).clear(); throw new AssertionError("mutable output"); }
        catch(UnsupportedOperationException correct) { checks++; }
        try (var pool=Executors.newFixedThreadPool(8)) {
            List<Future<Boolean>> jobs=new ArrayList<>();
            for(int i=0;i<1000;i++) jobs.add(pool.submit(()->s.topPositiveContexts("a",2).equals(expected(s,"a",2))));
            for(var f:jobs) require(f.get(),"concurrent parity");
        }
        System.out.println("RANK_ORACLE_PASS corpora=2500 checks="+checks+" digest="+HexFormat.of().formatHex(digest.digest()));
    }
    private static void allocation() {
        var bean=(com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation instrumentation unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        List<List<String>> docs=new ArrayList<>();
        for(int i=0;i<6000;i++) docs.add(List.of("center","word"+i));
        var s=M3CooccurrencePpmi.build(docs,1);
        require(s.topPositiveContexts("center",Integer.MAX_VALUE).size()==6000,"wide fixture positive cardinality");
        for(int i=0;i<500;i++) sink=s.topPositiveContexts("center",10);
        long tid=Thread.currentThread().threadId();
        long before=bean.getThreadAllocatedBytes(tid);
        for(int i=0;i<100;i++) sink=s.topPositiveContexts("center",10);
        long perCall=(bean.getThreadAllocatedBytes(tid)-before)/100;
        System.out.println("RANK_ALLOCATION bytesPerCall="+perCall+" vocabulary="+s.vocabulary().size()+" k=10");
        require(perCall<65536,"BOUND_ALLOCATION: "+perCall+" >= 65536");
    }
    private static Object call(Method m,double[] scores,int k) throws Exception {
        try { return m.invoke(null,scores,k); }
        catch(InvocationTargetException e) {
            Throwable t=e.getCause();
            if(t instanceof Exception ex) throw ex;
            if(t instanceof Error er) throw er;
            throw new AssertionError(t);
        }
    }
    private static void nativeBoundary() throws Exception {
        // Exercise actual runtime loader before reaching the private JNI contract.
        M3CooccurrencePpmi.build(List.of(List.of("a","b")),1).topPositiveContexts("a",1);
        Method method=M3CooccurrencePpmi.class.getDeclaredMethod("selectPositiveNative",double[].class,int.class);
        method.setAccessible(true);
        Random r=new Random(103114679L);
        for(int trial=0;trial<10000;trial++) {
            double[] a=new double[r.nextInt(513)];
            for(int i=0;i<a.length;i++) a[i]=switch(r.nextInt(8)) {
                case 0 -> -0.0; case 1 -> Double.MIN_VALUE; case 2 -> Double.MAX_VALUE;
                case 3 -> -1.0; default -> r.nextInt(40)/4.0;
            };
            int k=trial%9==0?Integer.MAX_VALUE:r.nextInt(a.length+2);
            double[] before=a.clone();
            List<Integer> expected=new ArrayList<>();
            for(int i=0;i<a.length;i++) if(a[i]>0) expected.add(i);
            expected.sort(Comparator.<Integer>comparingDouble(i->a[i]).reversed().thenComparingInt(i->i));
            int[] oracle=expected.stream().limit(k).mapToInt(Integer::intValue).toArray();
            require(Arrays.equals(oracle,(int[])call(method,a,k)),"native tie/bounds parity "+trial);
            require(Arrays.equals(before,a),"native mutated input");
        }
        try { call(method,null,1); throw new AssertionError("null native"); }
        catch(NullPointerException correct){checks++;}
        try { call(method,new double[0],-1); throw new AssertionError("negative native"); }
        catch(IllegalArgumentException correct){checks++;}
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            for(int k:new int[]{0,1,100}) {
                double[] input=new double[2050]; input[2049]=bad;
                try { call(method,input,k); throw new AssertionError("nonfinite native"); }
                catch(IllegalArgumentException correct){checks++;}
            }
        }
        System.out.println("JNI_BOUNDARY_PASS arrays=10000 checks="+checks);
    }
}
