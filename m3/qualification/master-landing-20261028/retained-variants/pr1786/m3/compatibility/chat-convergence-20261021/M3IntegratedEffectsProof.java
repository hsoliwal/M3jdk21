/* Copyright 2026 Hitesh Soliwal and contributors. GPLv2 with the Classpath exception. */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

/** Actual java.base owner and optional actual JNI library; not an installed-image gate. */
public final class M3IntegratedEffectsProof {
    private static long checks;
    private M3IntegratedEffectsProof() { }
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    private static long[] product(long[] a, long[] b) {
        long[] result = new long[a.length];
        for (int i=0;i<a.length;i++) for(int j=0;j<a.length;j++)
            if ((a[i] & (1L << j)) != 0) result[i] |= b[j];
        return result;
    }
    private static long[] power(long[] a, long count) {
        long[] result=new long[a.length];
        for(int i=0;i<a.length;i++) result[i]=1L<<i;
        while(count!=0) { if((count&1)!=0) result=product(result,a);
            count>>>=1; if(count!=0) a=product(a,a); }
        return result;
    }
    public static void main(String[] args) {
        boolean nativeMode=args.length==1;
        if(nativeMode) System.load(args[0]);
        Random random=new Random(0x4d33494e544547L);
        String[] patterns={"a*", ".*", "a*b*", "a*.b*", ".*a*b.*", "a*".repeat(31)+"b", ".*".repeat(63)};
        char[] alphabet={'a','b','Z','\0','\n','\r','\u2028','\ud800','\udc00','\ud83d','\ude00'};
        for(String pattern:patterns) {
            var plan=M3RegexDpPlan.prepare(pattern);check(plan!=null);
            for(int trial=0;trial<250;trial++) {
                StringBuilder text=new StringBuilder();
                int length=random.nextInt(36);
                for(int i=0;i<length;i++) text.append(alphabet[random.nextInt(alphabet.length)]);
                String s=text.toString();
                var whole=plan.summarizeUtf16(s,null);
                // Bound the backtracking oracle; wide-state algebra is checked below for every split.
                if (pattern.length() < 20)
                    check(plan.accepts(whole.apply(plan.initialState()))==Pattern.matches(pattern,s));
                for(int split=0;split<=s.length();split++) {
                    var joined=plan.summarizeUtf16(s.substring(0,split),null)
                        .then(plan.summarizeUtf16(s.substring(split),null),null);
                    for(int state=0;state<plan.stateCount();state++)
                        check(joined.apply(1L<<state)==whole.apply(1L<<state));
                }
                int copies=trial%5;
                var repeated=whole.repeated(copies,null);
                var expanded=plan.summarizeUtf16(s.repeat(copies),null);
                for(int state=0;state<plan.stateCount();state++)
                    check(repeated.apply(1L<<state)==expanded.apply(1L<<state));
            }
            int[] reads={0}, checkpoints={0};
            CharSequence observed=new CharSequence(){
                public int length(){return 1024;}
                public char charAt(int i){check(i==reads[0]++);return 'a';}
                public CharSequence subSequence(int a,int b){throw new AssertionError("materialized");}
            };
            plan.summarizeUtf16(observed,()->checkpoints[0]++);
            check(reads[0]==1024 && checkpoints[0]==5);
        }
        if(nativeMode) for(int n:new int[]{1,2,16,31,63,64}) {
            long mask=n==64?-1L:(1L<<n)-1;
            for(int trial=0;trial<40;trial++) {
                long[] rows=new long[n];for(int i=0;i<n;i++) rows[i]=random.nextLong()&mask;
                long[] before=rows.clone();
                for(long count:new long[]{0,1,2,255,256,257,1024,Long.MAX_VALUE}) {
                    long[] expected=power(rows,count);
                    check(Arrays.equals(expected,M3RegexDpPlan.repeat0(rows,count,false)));
                    check(Arrays.equals(expected,M3RegexDpPlan.repeat0(rows,count,true)));
                    check(Arrays.equals(before,rows));
                }
            }
            long[] cycle=new long[n], expected=new long[n];
            for(int i=0;i<n;i++){cycle[i]=1L<<((i+1)%n); expected[i]=1L<<((i+(int)(Long.MAX_VALUE%n))%n);}
            check(Arrays.equals(expected,M3RegexDpPlan.repeat0(cycle,Long.MAX_VALUE,false)));
        }
        System.out.println("INTEGRATED_EFFECTS checks="+checks+" native="+nativeMode);
        System.out.println("Patched stock JDK owner execution; full source-built M3JDK21 acceptance pending.");
    }
}
