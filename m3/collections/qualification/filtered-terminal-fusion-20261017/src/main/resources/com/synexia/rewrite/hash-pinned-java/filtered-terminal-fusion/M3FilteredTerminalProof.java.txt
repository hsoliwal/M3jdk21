/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.function.LongPredicate;

/** Identical classpath-independent test of baseline and source-recipe candidate. */
public final class M3FilteredTerminalProof {
    private static long assertions;
    private static final StringBuilder TAPE = new StringBuilder(1 << 20);
    private static final class Source implements M3LongCollection {
        private final long[] values;
        private final int failAt;
        Source(long[] values, int failAt) { this.values=values;this.failAt=failAt; }
        @Override public int size() { TAPE.append('S');return values.length; }
        @Override public boolean contains(long value) {TAPE.append('C');for(long v:values)if(v==value)return true;return false;}
        @Override public void clear() {throw new UnsupportedOperationException();}
        @Override public long[] toArray() {throw new AssertionError("no source-toArray allowed");}
        @Override public M3LongIterator iterator() {
            TAPE.append('I');
            return new M3LongIterator() {
                private int cursor;
                @Override public boolean hasNext() {
                    TAPE.append('H').append(cursor).append(';');return cursor<values.length;
                }
                @Override public long nextLong() {
                    TAPE.append('N').append(cursor).append(';');
                    if(cursor==failAt)throw new IllegalStateException("source callback");
                    return values[cursor++];
                }
            };
        }
    }
    private static void check(boolean value){assertions++;if(!value)throw new AssertionError(assertions);}
    private static void eq(long actual,long expected){assertions++;if(actual!=expected)throw new AssertionError(actual+" != "+expected+" at "+assertions);}
    private static void observe(Runnable f) {
        try {f.run();}catch(RuntimeException e){TAPE.append("EX:").append(e.getClass().getName()).append(':').append(e.getMessage());}
    }
    private static void cases() {
        Random rng=new Random(1234567);
        for(int seed=0;seed<128;seed++) {
            final int ss=seed;
            int n=seed%7==0?0:seed%11==0?1:rng.nextInt(300);
            long[] values=new long[n];
            for(int i=0;i<n;i++)values[i]=i%31==0?Long.MIN_VALUE:i%37==0?Long.MAX_VALUE:rng.nextInt(31)-15;
            int mask=rng.nextInt(31);int fail=seed%19==0?n/2:-1;
            for(int kind=0;kind<5;kind++) {
                Source src=new Source(values,fail);
                LongPredicate test=v->{TAPE.append('P').append(v).append(';');
                    if((ss%43)==5&&v==Long.MAX_VALUE)throw new IllegalArgumentException("predicate");
                    return (v&mask)==0;};
                M3LongFilteredView view=new M3LongFilteredView(src,test);
                if((seed&1)!=0) {
                    LongPredicate second=v->{TAPE.append('Q').append(v).append(';');return v%3!=0;};
                    view=new M3LongFilteredView(view,second);
                }
                final M3LongFilteredView target=view;
                TAPE.append("\nseed=").append(seed).append(" kind=").append(kind).append(':');
                switch(kind) {
                    case 0 -> observe(()->TAPE.append("size=").append(target.size()));
                    case 1 -> observe(()->TAPE.append("empty=").append(target.isEmpty()));
                    case 2 -> observe(()->target.forEachLong(v->{TAPE.append('A').append(v).append(';');
                        if(ss%29==3&&v>10)throw new ArithmeticException("consumer");}));
                    case 3 -> observe(()->TAPE.append("toArray=").append(Arrays.toString(target.toArray())));
                    default -> observe(()->{
                        long count=target.longStream().limit(2).count();TAPE.append("stream=").append(count);
                    });
                }
                check(true);
            }
        }
        Source empty=new Source(new long[0],-1);
        var v=new M3LongFilteredView(empty,x->true);
        observe(()->v.forEachLong(null));
        observe(v::clear);
        check(v.isEmpty());eq(v.size(),0);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("no args");
        cases();
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("M3_FILTER_TERMINAL_TRACE digest="+sha+" assertions="+assertions+" chars="+TAPE.length());
    }
}
