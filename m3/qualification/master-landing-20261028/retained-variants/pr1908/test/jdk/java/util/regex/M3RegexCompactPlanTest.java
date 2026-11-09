// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Compact trigram plans and mutually exclusive Matcher cache stamps preserve contracts
 * @modules java.base/java.util.regex:+open
 *          java.base/jdk.internal.mindex:+open
 * @run main/othervm -Xmx512m M3RegexCompactPlanTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TQ;

public final class M3RegexCompactPlanTest {
    private static final AtomicLong CHECKS = new AtomicLong();
    private static final Field PLAN = field(M3TQ.class, "scanPlan");
    private static final Field FACTS = field(Matcher.class, "m3TqFacts");
    private static final Field FROM = field(Matcher.class, "m3TqFactsFrom");
    private static final Field TO = field(Matcher.class, "m3TqFactsTo");
    private static Field field(Class<?> owner, String name) {
        try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    private static void check(boolean value, String reason) {
        CHECKS.incrementAndGet(); if (!value) throw new AssertionError(reason);
    }
    private static long key(CharSequence value, int i) {
        return ((long)value.charAt(i)<<32) | ((long)value.charAt(i+1)<<16) | value.charAt(i+2);
    }
    private static void compare(M3TQ q, String value, int from, int to) {
        Set<Long> present = new HashSet<>();
        for (int i=from; i+2<to; i++) present.add(key(value,i));
        boolean expected = q.test(present::contains);
        check(q.testScanning(value,from,to,to-from)==expected,"String region result");
        check(q.testScanning(new StringBuilder(value),from,to,to-from)==expected,"generic UTF16 region result");
    }
    private static void flat() throws Exception {
        List<M3TQ> leaves = new ArrayList<>(); StringBuilder all = new StringBuilder();
        for (int i=0;i<33;i++) {
            String gram = ""+(char)(0xd800+i)+"xy";
            leaves.add(M3TQ.fromExact(List.of(gram))); all.append(gram);
        }
        check(M3TQ.all().hasScanPlan() && M3TQ.none().hasScanPlan(),"constant scans");
        compare(M3TQ.all(),"",0,0); compare(M3TQ.none(),"",0,0);
        for(int n:new int[]{1,2,3,16,31,32}) for(boolean and:new boolean[]{false,true}) {
            M3TQ q=and?M3TQ.and(leaves.subList(0,n)):M3TQ.or(leaves.subList(0,n));
            check(q.hasScanPlan(),"flat plan admitted");
            check(PLAN.get(q)==null,"flat query retains a redundant ScanPlan");
            long[] exported=q.trigrams(); if(exported.length>0)exported[0]=0;
            String value="\0prefix"+all+"suffix\0";
            for(int from=0;from<=value.length();from+=3) {
                compare(q,value,from,value.length()); compare(q,value,from,from);
                compare(q,value,from,Math.min(value.length(),from+4));
            }
        }
        check(!M3TQ.and(leaves).hasScanPlan(),"33-key AND refusal");
        check(!M3TQ.or(leaves).hasScanPlan(),"33-key OR refusal");
        M3TQ compound=M3TQ.and(List.of(M3TQ.or(leaves.subList(0,2)),leaves.get(2)));
        check(PLAN.get(compound)!=null && compound.hasScanPlan(),"compound DNF retained");
        compare(compound,all.toString(),0,all.length());compare(compound,"",0,0);
        M3TQ nested=M3TQ.and(leaves.subList(0,2));
        for(int depth=1;depth<=25;depth++) {
            nested=(depth%2==1)?M3TQ.or(List.of(nested,leaves.get(2))):M3TQ.and(List.of(nested,leaves.get(2)));
            check(nested.hasScanPlan()==(depth<=24),"depth cap "+depth);
        }
        List<Callable<Boolean>> work=new ArrayList<>();
        M3TQ shared=M3TQ.and(leaves.subList(0,32));String value=all.toString();
        for(int t=0;t<8;t++)work.add(()->{
            for(int i=0;i<1000;i++) {
                if(!shared.testScanning(value,0,value.length(),value.length()))return false;
                if(shared.testScanning("",0,0,0))return false;
            }
            return true;
        });
        try(var pool=Executors.newFixedThreadPool(4)) {
            for(var result:pool.invokeAll(work))check(result.get(),"shared immutable flat query");
        }
    }
    private static void stamp(Matcher m,int from,int to,boolean allowed) throws Exception {
        check(FACTS.get(m)==null,"Boolean path must not retain Facts");
        check(TO.getInt(m)==to,"Boolean result not encoded in existing end stamp");
        check(FROM.getInt(m)==(allowed?from:~from),"Boolean start/verdict stamp");
    }
    private static void cache() throws Exception {
        Pattern a=Pattern.compile("abc|xyz"),b=Pattern.compile("qqq|zzz");
        Matcher switched=a.matcher("qqq");
        check(!switched.find(),"first query negative");
        switched.usePattern(b).reset();
        check(switched.find() && "qqq".equals(switched.group()),
                "query swap reused a stale negative verdict");
        Matcher m=a.matcher("abc---qqq").region(3,9);
        check(!m.find(),"negative region");stamp(m,3,9,false);
        m.region(0,9);check(m.find() && m.start()==0,"expanded region");stamp(m,0,9,true);
        int savedFrom=FROM.getInt(m),savedTo=TO.getInt(m);
        try {m.usePattern(null);throw new AssertionError("null pattern accepted");}
        catch(IllegalArgumentException expected){ }
        check(FROM.getInt(m)==savedFrom && TO.getInt(m)==savedTo,"failed pattern mutation preserves cache");
        m.usePattern(b);check(TO.getInt(m)==-1,"usePattern must invalidate Boolean cache");
        check(m.find() && m.start()==6,"new query after old match");stamp(m,0,9,true);
        m.reset("missing");check(!m.find(),"new input");stamp(m,0,7,false);
        m.reset();check(!m.find(),"same immutable input reset");stamp(m,0,7,false);
        m.region(7,7);check(!m.find(),"empty region");stamp(m,7,7,false);
        m.reset("");check(!m.find(),"zero start empty input");stamp(m,0,0,false);
        StringBuilder mutable=new StringBuilder("abc");m.usePattern(a).reset(mutable);
        check(m.find(),"mutable positive");mutable.setCharAt(0,'q');m.reset();
        check(!m.find() && TO.getInt(m)==-1,"mutable input must not cache");
        Matcher literal=Pattern.compile("acb",Pattern.LITERAL).matcher("abc".repeat(100));
        check(!literal.find(),"literal negative");Object facts=FACTS.get(literal);
        check(facts!=null,"literal retains facts");
        literal.usePattern(a).reset();check(literal.find() && FACTS.get(literal)==facts,"facts survive query change");
        literal.region(1,300);check(literal.find(),"changed facts region");
        check(FACTS.get(literal)!=null && FACTS.get(literal)!=facts,"new facts range must not stamp old facts");
        check(FROM.getInt(literal)==1 && TO.getInt(literal)==300,"facts retain ordinary stamps");
    }
    public static void main(String[] args) throws Exception {
        if(args.length==0||args[0].equals("flat"))flat();
        if(args.length==0||args[0].equals("cache"))cache();
        System.out.println("M3RegexCompactPlanTest PASS checks="+CHECKS.get());
    }
}
