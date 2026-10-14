/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 * @test
 * @summary Bounded regex epsilon-frontier effects, seams, native relation ABI and fallbacks
 * @modules java.base/jdk.internal.mindex
 * @compile --patch-module java.base=${test.src} M3RXTest.java
 * @run main/othervm --patch-module java.base=${test.classes} jdk.internal.mindex.M3RXTest
 */
package jdk.internal.mindex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/** Finite independent oracle corpus; not complete JDK runtime qualification. */
public final class M3RXTest {
    private static long checks;
    private static Method nativeMethod;
    private static boolean nativeLoaded;

    public static void main(String[] args) throws Exception {
        M3RX.Plan basic = plan("a*b");
        var a = effect(basic, "aaa"); var b = effect(basic, "b");
        check(basic.matches(basic.composeNative(a, b, null)), "JAVA_FALLBACK");
        if (args.length == 1) {
            // This helper is patched into java.base, so the library belongs to the bootstrap loader.
            System.load(args[0]);
            nativeLoaded = true;
        } else if (args.length != 0) throw new IllegalArgumentException("optional absolute native library");
        nativeMethod = M3RX.class.getDeclaredMethod("composeRows0", long[].class, long[].class, int.class);
        nativeMethod.setAccessible(true);
        corpus(); wide(); laws(); limits(); allBmp(); readers();
        if (nativeLoaded) nativeAbi();
        System.out.println("M3RX_PROOF native=" + nativeLoaded + " checks=" + checks + " PASS");
    }

    static int[] atoms(String regex) {
        int[] temp = new int[regex.length()]; int count = 0;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '*') temp[count - 1] |= M3RX.STAR;
            else temp[count++] = c;
        }
        return Arrays.copyOf(temp, count);
    }
    static M3RX.Plan plan(String regex) { int[] a = atoms(regex); return M3RX.compile(a, a.length, null); }
    static M3RX.Effect effect(M3RX.Plan p, String text) {
        M3RX.Effect result = p.summarize(text.length(), text::charAt, 10_000_000, null);
        if (result == null) throw new AssertionError("effect refused");
        return result;
    }

    /** Independent Boolean array NFA: no bit masks, lifting or production closure methods. */
    static long oracle(int[] atoms, String text, long input) {
        boolean[] current = new boolean[atoms.length + 1];
        for (int s = 0; s < current.length; s++) current[s] = (input & (1L << s)) != 0;
        closure(atoms, current);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            boolean[] next = new boolean[current.length];
            for (int s = 0; s < atoms.length; s++) if (current[s]) {
                int unit = atoms[s] & 0xffff;
                boolean accepted = unit == '.' ? c != 10 && c != 13 && c != 0x85 && c != 0x2028 && c != 0x2029 : unit == c;
                if (accepted) next[(atoms[s] & M3RX.STAR) != 0 ? s : s + 1] = true;
            }
            closure(atoms, next); current = next;
        }
        long result = 0;
        for (int s = 0; s < current.length; s++) if (current[s]) result |= 1L << s;
        return result;
    }
    private static void closure(int[] atoms, boolean[] states) {
        for (int s = 0; s < atoms.length; s++) if ((atoms[s] & M3RX.STAR) != 0 && states[s]) states[s + 1] = true;
    }

    private static void corpus() {
        List<String> texts = new ArrayList<>(List.of("", "\u0100", "\u0085", "\u2028", "\u2029", "\r"));
        addWords(texts, "", 4);
        String[] choices = {"a", "b", ".", "a*", "b*", ".*"};
        for (int count = 1; count <= 4; count++) {
            int combinations = (int) Math.pow(choices.length, count);
            for (int code = 0; code < combinations; code++) {
                StringBuilder expression = new StringBuilder(); int temp = code;
                for (int i = 0; i < count; i++, temp /= choices.length) expression.append(choices[temp % choices.length]);
                String regex = expression.toString(); int[] atoms = atoms(regex);
                M3RX.Plan p = M3RX.compile(atoms, atoms.length, null);
                Pattern jdk = Pattern.compile(regex);
                for (String text : texts) {
                    long expected = oracle(atoms, text, 1L);
                    boolean match = (expected & (1L << count)) != 0;
                    check(jdk.matcher(text).matches() == match, "JDK_ORACLE");
                    check(p.tryMatch(text.length(), text::charAt, null) == (match ? 1 : 0), "FRONTIER");
                    M3RX.Effect full = effect(p, text);
                    check(p.matches(full) == match, "EFFECT_MATCH");
                    int split = text.length() / 2;
                    M3RX.Effect joined = p.compose(effect(p, text.substring(0, split)), effect(p, text.substring(split)), null);
                    for (int s = 0; s <= count; s++) {
                        long entry = 1L << s;
                        long want = oracle(atoms, text, entry);
                        check(p.apply(full, entry, null) == want, "ALL_ENTRY");
                        check(p.apply(joined, entry, null) == want, "ORDERED_SEAM");
                    }
                    if (nativeLoaded && text.length() == 2 && code % 17 == 0) {
                        M3RX.Effect nativeJoined = p.composeNative(effect(p, text.substring(0, split)), effect(p, text.substring(split)), null);
                        check(p.apply(nativeJoined, -1L >>> (63 - count), null) == oracle(atoms, text, -1L >>> (63 - count)), "NATIVE_EFFECT");
                    }
                }
            }
        }
    }
    private static void addWords(List<String> target, String prefix, int remaining) {
        if (remaining == 0) return;
        for (char c : new char[] {'a', 'b', '\n'}) {
            String text = prefix + c; target.add(text); addWords(target, text, remaining - 1);
        }
    }

    private static void wide() {
        for (int n : new int[] {1, 2, 31, 32, 33, 62, 63}) {
            for (String regex : new String[] {"a*".repeat(n), "a".repeat(n - 1) + ".*"}) {
                var p = plan(regex); int[] atoms = atoms(regex);
                for (String text : new String[] {"", "a", "a".repeat(n), "a".repeat(n) + "b", "\u0100"}) {
                    var e = effect(p, text);
                    for (int s = 0; s <= n; s++) {
                        check(p.apply(e, 1L << s, null) == oracle(atoms, text, 1L << s), "WIDE_EPSILON");
                    }
                    if (n <= 3 || text.isEmpty()) check(p.matches(e) == Pattern.matches(regex, text), "WIDE_JDK");
                }
            }
        }
    }

    private static void laws() {
        var p = plan("a*b*.*c");
        var a = effect(p, "aa"); var b = effect(p, "bb"); var c = effect(p, "c"); var id = p.identity(null);
        long domain = (1L << p.states()) - 1;
        for (long in = 0; in <= domain; in++) {
            long expected = oracle(atoms("a*b*.*c"), "aabbc", in);
            check(p.apply(p.compose(p.compose(a,b,null),c,null),in,null) == expected, "ASSOCIATIVE_LEFT");
            check(p.apply(p.compose(a,p.compose(b,c,null),null),in,null) == expected, "ASSOCIATIVE_RIGHT");
            check(p.apply(p.compose(id,a,null),in,null) == p.apply(a,in,null), "LEFT_IDENTITY");
            check(p.apply(p.compose(a,id,null),in,null) == p.apply(a,in,null), "RIGHT_IDENTITY");
        }
        for (int n=0;n<20;n++) {
            var repeated=p.repeat(a,n,null); var direct=effect(p,"aa".repeat(n));
            check(repeated.length()==2L*n,"REPEAT_LENGTH");
            check(p.apply(repeated,domain,null)==p.apply(direct,domain,null),"REPEAT");
        }
        expect(IllegalArgumentException.class, ()->p.compose(a,effect(plan("a*"),"a"),null),"OWNER");
        var unit=effect(p,"a"); var huge=p.repeat(unit,Long.MAX_VALUE,null);
        expect(ArithmeticException.class,()->p.repeat(huge,2,null),"LENGTH_OVERFLOW");
        expect(ArithmeticException.class,()->p.compose(huge,unit,null),"JOIN_OVERFLOW");
    }

    private static void limits() {
        int[] a={'a'|M3RX.STAR,'.'}; var p=M3RX.compile(a,2,null); a[0]='b';
        check(p.tryMatch(2,i->'a',null)==1,"INSTRUCTION_COPY");
        check(!p.sameInstructions(a,2),"EXACT_PLAN_KEY");
        expect(IllegalArgumentException.class,()->M3RX.compile(new int[64],64,null),"WIDTH_REFUSAL");
        expect(IllegalArgumentException.class,()->M3RX.compile(new int[]{'['},1,null),"TOKEN_REFUSAL");
        AtomicInteger reads=new AtomicInteger();
        check(p.summarize(10,i->{reads.incrementAndGet();return 'a';},29,null)==null,"WORK_REFUSAL");
        check(reads.get()==0,"NO_REFUSED_READS");
        for(int unit:new int[]{-1,0xd800,0xdfff,0x10000}) {
            check(p.tryMatch(2,i->i==0?'x':unit,null)==-1,"LATE_UNSUPPORTED");
            check(p.summarize(2,i->i==0?'x':unit,100,null)==null,"EFFECT_UNSUPPORTED");
        }
        AtomicInteger checkpoints=new AtomicInteger(); p.summarize(130,i->'a',1000,checkpoints::incrementAndGet);
        for(int stop=1;stop<=checkpoints.get();stop++) {
            final int at=stop; AtomicInteger seen=new AtomicInteger();
            expect(CancellationException.class,()->p.summarize(130,i->'a',1000,()->{
                if(seen.incrementAndGet()==at)throw new CancellationException();
            }),"CHECKPOINT");
        }
        Thread.currentThread().interrupt();
        try { check(p.tryMatch(1,i->'a',null)==1,"STRING_INTERRUPT_UNCHANGED"); check(Thread.currentThread().isInterrupted(),"INTERRUPT_FLAG"); }
        finally {Thread.interrupted();}
    }

    private static void allBmp() {
        var p=plan(".*");
        for(int c=0;c<=0xffff;c++) {
            final int unit=c;
            int expected=c>=0xd800&&c<=0xdfff?-1:c==10||c==13||c==0x85||c==0x2028||c==0x2029?0:1;
            check(p.tryMatch(1,i->unit,null)==expected,"ALL_BMP_UNITS");
        }
    }

    private static void readers() throws Exception {
        var p=plan("a*b*c*"); var e=effect(p,"aaabbbc");
        try(var pool=Executors.newFixedThreadPool(4)) {
            var futures=new ArrayList<java.util.concurrent.Future<Integer>>();
            for(int worker=0;worker<4;worker++)futures.add(pool.submit(()->{
                for(int i=0;i<1000;i++)if(!p.matches(e))throw new AssertionError("CONCURRENT"); return 1000;
            }));
            for(var future:futures)checks+=future.get();
        }
    }

    private static void nativeAbi() throws Exception {
        Random random=new Random(23351);
        for(int states:new int[]{1,2,3,31,32,63,64}) {
            long valid=states==64?-1L:(1L<<states)-1;
            for(int trial=0;trial<100;trial++) {
                long[] a=new long[states],b=new long[states];
                for(int i=0;i<states;i++){a[i]=random.nextLong()&valid;b[i]=random.nextLong()&valid;}
                long[] out=raw(a,b,states);
                for(int i=0;i<states;i++) {
                    long expected=0;
                    for(int s=0;s<states;s++)if((a[i]&(1L<<s))!=0)expected|=b[s];
                    check(out[i]==expected,"RAW_JNI_RELATION");
                }
            }
        }
        expect(NullPointerException.class,()->raw(null,new long[1],1),"JNI_NULL");
        for(int n:new int[]{-1,0,65,Integer.MAX_VALUE})expect(IllegalArgumentException.class,()->raw(new long[0],new long[0],n),"JNI_GEOMETRY");
        expect(IllegalArgumentException.class,()->raw(new long[2],new long[1],2),"JNI_LENGTH");
        expect(IllegalArgumentException.class,()->raw(new long[]{0,0},new long[]{0,4},2),"UNREACHABLE_VALIDATION");
        expect(IllegalArgumentException.class,()->raw(new long[]{4,0},new long[]{0,0},2),"LEFT_VALIDATION");
    }
    private static long[] raw(long[] a,long[] b,int n) {
        try{return (long[])nativeMethod.invoke(null,a,b,n);}
        catch(InvocationTargetException ex){if(ex.getCause() instanceof RuntimeException r)throw r;if(ex.getCause() instanceof Error e)throw e;throw new AssertionError(ex);}
        catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
    }
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void expect(Class<? extends Throwable> kind,Runnable action,String label){
        checks++;try{action.run();}catch(Throwable t){if(kind.isInstance(t))return;throw new AssertionError(label,t);}throw new AssertionError(label);
    }
}
