/*
 * @test
 * @summary Bounded M3 GINQ-inspired inner/outer hash join over indexed M3 collections
 * @modules jdk.m3.collections
 * @run main/othervm GinqHashJoinTest
 */
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
import jdk.m3.collections.GinqHashJoin;
import jdk.m3.collections.ObjectStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public class GinqHashJoinTest {
    private static int checks;
    private record Row(Integer key, String name) { }
    private record Colliding(int key) { @Override public int hashCode() { return 7; } }
    private static final Function<Row, Integer> KEY = row -> row == null ? null : row.key();
    private static final GinqHashJoin.RowMapper<Row, Row, String> SHOW = (lp, l, rp, r) ->
            (lp ? name(l) : "absent") + "/" + (rp ? name(r) : "absent");

    private static String name(Row row) { return row == null ? "null" : row.name(); }
    private static Row row(Integer key, String name) { return new Row(key, name); }
    private static void eq(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected=" + expected + " actual=" + actual);
        checks++;
    }
    private static void yes(boolean value) { if (!value) throw new AssertionError("assertion failed"); checks++; }
    private static void rejected(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); throw new AssertionError("expected " + type.getSimpleName()); }
        catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("wrong failure: " + failure, failure);
        }
        checks++;
    }
    private static List<String> join(List<Row> l, List<Row> r, GinqHashJoin.Mode mode,
            int maxLeft, int maxRight, int maxKeys, int maxOutput) {
        return GinqHashJoin.execute(ObjectStream.snapshot(l), ObjectStream.snapshot(r), KEY, KEY,
                SHOW, mode, maxLeft, maxRight, maxKeys, maxOutput);
    }
    private static List<String> reference(List<Row> left, List<Row> right, GinqHashJoin.Mode mode) {
        ArrayList<String> result = new ArrayList<>();
        BitSet marked = new BitSet();
        for (Row l : left) {
            boolean found = false;
            for (int i = 0; i < right.size(); i++) {
                Row r = right.get(i);
                if (Objects.equals(KEY.apply(l), KEY.apply(r))) {
                    result.add(SHOW.apply(true, l, true, r));
                    marked.set(i);
                    found = true;
                }
            }
            if (!found && (mode == GinqHashJoin.Mode.LEFT || mode == GinqHashJoin.Mode.FULL))
                result.add(SHOW.apply(true, l, false, null));
        }
        if (mode == GinqHashJoin.Mode.RIGHT || mode == GinqHashJoin.Mode.FULL) {
            for (int i = 0; i < right.size(); i++)
                if (!marked.get(i)) result.add(SHOW.apply(false, null, true, right.get(i)));
        }
        return result;
    }
    private static void fixed() {
        List<Row> left = List.of(row(1,"L1"),row(2,"L2"),row(1,"L3"));
        List<Row> right = List.of(row(1,"R1"),row(1,"R2"),row(3,"R3"));
        eq(List.of("L1/R1", "L1/R2", "L3/R1", "L3/R2"), join(left,right,GinqHashJoin.Mode.INNER,3,3,2,4));
        eq(List.of("L1/R1", "L1/R2", "L2/absent", "L3/R1", "L3/R2"),
                join(left,right,GinqHashJoin.Mode.LEFT,3,3,2,5));
        eq(List.of("L1/R1", "L1/R2", "L3/R1", "L3/R2", "absent/R3"),
                join(left,right,GinqHashJoin.Mode.RIGHT,3,3,2,5));
        eq(List.of("L1/R1", "L1/R2", "L2/absent", "L3/R1", "L3/R2", "absent/R3"),
                join(left,right,GinqHashJoin.Mode.FULL,3,3,2,6));
        eq(List.of("L1/R1", "L1/R2", "L3/R1", "L3/R2"),
                GinqHashJoin.inner(ObjectStream.snapshot(left), ObjectStream.snapshot(right), KEY, KEY,
                    (l,r)->l.name()+"/"+r.name(),3,3,2,4));
        List<Row> maybeNull = new ArrayList<>();
        maybeNull.add(null);
        maybeNull.add(row(5,"five"));
        List<String> hits = join(maybeNull,Collections.singletonList(null),GinqHashJoin.Mode.FULL,2,1,1,2);
        eq(List.of("null/null","five/absent"), hits);
        rejected(UnsupportedOperationException.class, () -> hits.add("another"));
        eq(List.of("absent/R3"), join(List.of(),List.of(row(3,"R3")),GinqHashJoin.Mode.RIGHT,0,1,1,1));
        eq(List.of("L1/absent"), join(List.of(row(1,"L1")),List.of(),GinqHashJoin.Mode.LEFT,1,0,0,1));
        eq(List.of("absent/null"), join(List.of(row(7,"L7")), Collections.singletonList(null),
                GinqHashJoin.Mode.RIGHT, 1, 1, 1, 1));
        List<String> nullResult=GinqHashJoin.execute(ObjectStream.snapshot(List.of(row(1,"x"))),
                ObjectStream.snapshot(List.of(row(1,"y"))),KEY,KEY,(lp,l,rp,r)->null,
                GinqHashJoin.Mode.INNER,1,1,1,1);
        eq(1,nullResult.size());eq(null,nullResult.get(0));
        eq(List.of("L1/R1", "L3/R3"), GinqHashJoin.execute(
                ObjectStream.snapshot(List.of(row(1,"L1"),row(2,"L2"),row(3,"L3"))),
                ObjectStream.snapshot(List.of(row(1,"R1"),row(3,"R3"))),
                l -> new Colliding(l.key()), r -> new Colliding(r.key()), SHOW,
                GinqHashJoin.Mode.INNER,3,2,2,3));
    }
    private static void callbacksAndBudgets() {
        List<Row> left = List.of(row(1,"a"),row(1,"b"));
        List<Row> right = List.of(row(1,"x"),row(1,"y"));
        ArrayList<String> callOrder = new ArrayList<>();
        List<String> results = GinqHashJoin.execute(ObjectStream.snapshot(left), ObjectStream.snapshot(right),
                l->{callOrder.add("L"+l.name());return l.key();},
                r->{callOrder.add("R"+r.name());return r.key();},
                (lp,l,rp,r)->{callOrder.add("P"+l.name()+r.name());return l.name()+r.name();},
                GinqHashJoin.Mode.INNER,2,2,1,4);
        eq(List.of("ax","ay","bx","by"), results);
        eq(List.of("Rx","Ry","La","Pax","Pay","Lb","Pbx","Pby"),callOrder);
        AtomicInteger mapped = new AtomicInteger();
        rejected(IllegalStateException.class, () -> GinqHashJoin.execute(ObjectStream.snapshot(left),
                ObjectStream.snapshot(right),KEY,KEY,(lp,l,rp,r)->{mapped.incrementAndGet(); return "ok";},
                GinqHashJoin.Mode.INNER,2,2,1,3));
        eq(3,mapped.get()); // fourth projection never invoked on budget refusal
        rejected(IllegalStateException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,1,2,1,4));
        rejected(IllegalStateException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,1,1,4));
        rejected(IllegalStateException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,2,0,4));
        rejected(IllegalStateException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,2,1,0));
        rejected(IllegalArgumentException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,-1,2,1,4));
        rejected(IllegalArgumentException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,-1,1,4));
        rejected(IllegalArgumentException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,2,-1,4));
        rejected(IllegalArgumentException.class, () -> join(left,right,GinqHashJoin.Mode.INNER,2,2,1,-1));
        rejected(NullPointerException.class, () -> GinqHashJoin.execute(ObjectStream.snapshot(left),
                ObjectStream.snapshot(right), KEY,KEY,SHOW,null,2,2,1,4));
    }
    private static void resourceLifetime() {
        AtomicInteger closedRight = new AtomicInteger();
        AtomicInteger closedLeft = new AtomicInteger();
        ObjectStream<Row> right = ObjectStream.once(Stream.of(row(1,"r")).onClose(closedRight::incrementAndGet));
        ObjectStream<Row> left = ObjectStream.once(Stream.of(row(1,"l")).onClose(closedLeft::incrementAndGet));
        eq(List.of("l/r"), GinqHashJoin.execute(left,right,KEY,KEY,SHOW,
                GinqHashJoin.Mode.INNER,1,1,1,1));
        eq(1,closedRight.get());eq(1,closedLeft.get());
        ObjectStream<Row> exhaustedLeft=left, exhaustedRight=right;
        rejected(IllegalStateException.class, () -> GinqHashJoin.execute(exhaustedLeft,exhaustedRight,KEY,KEY,SHOW,
                GinqHashJoin.Mode.INNER,1,1,1,1));
        closedRight.set(0); closedLeft.set(0);
        right=ObjectStream.once(Stream.of(row(1,"r"),row(2,"R2")).onClose(closedRight::incrementAndGet));
        left=ObjectStream.once(Stream.of(row(1,"l")).onClose(closedLeft::incrementAndGet));
        ObjectStream<Row> firstRight=right, firstLeft=left;
        rejected(IllegalStateException.class, () -> GinqHashJoin.execute(firstLeft,firstRight,KEY,KEY,SHOW,
                GinqHashJoin.Mode.INNER,1,1,1,1));
        eq(1,closedRight.get());eq(0,closedLeft.get());
        closedRight.set(0);closedLeft.set(0);
        right=ObjectStream.once(Stream.of(row(1,"r")).onClose(closedRight::incrementAndGet));
        left=ObjectStream.once(Stream.of(row(1,"l")).onClose(closedLeft::incrementAndGet));
        ObjectStream<Row> secondRight=right, secondLeft=left;
        rejected(IllegalArgumentException.class, () -> GinqHashJoin.execute(secondLeft,secondRight,
                r->{throw new IllegalArgumentException("mapper failure");},KEY,SHOW,
                GinqHashJoin.Mode.INNER,1,1,1,1));
        eq(1,closedRight.get());eq(1,closedLeft.get());
        rejected(NullPointerException.class, () -> GinqHashJoin.inner(null, ObjectStream.snapshot(List.of()),
                KEY,KEY,(l,r)->"x",1,1,1,1));
        closedRight.set(0);closedLeft.set(0);
        ObjectStream<Row> projectingRight=ObjectStream.once(Stream.of(row(1,"R")).onClose(closedRight::incrementAndGet));
        ObjectStream<Row> projectingLeft=ObjectStream.once(Stream.of(row(1,"L")).onClose(closedLeft::incrementAndGet));
        rejected(UnsupportedOperationException.class, () -> GinqHashJoin.execute(
                projectingLeft,projectingRight,KEY,KEY,
                (lp,l,rp,r)->{throw new UnsupportedOperationException("projection failure");},
                GinqHashJoin.Mode.FULL,1,1,1,1));
        eq(1,closedRight.get());eq(1,closedLeft.get());
    }
    private static void randomized() {
        Random rng = new Random(0x6A17_42F0L);
        for (int run = 0; run < 2_000; run++) {
            int ln=rng.nextInt(9),rn=rng.nextInt(9);
            List<Row> left=new ArrayList<>(),right=new ArrayList<>();
            for(int i=0;i<ln;i++)left.add(rng.nextInt(11)==0?null:row(rng.nextInt(5)==0?null:rng.nextInt(4),"L"+i));
            for(int i=0;i<rn;i++)right.add(rng.nextInt(11)==0?null:row(rng.nextInt(5)==0?null:rng.nextInt(4),"R"+i));
            for (GinqHashJoin.Mode mode:GinqHashJoin.Mode.values()) {
                eq(reference(left,right,mode),join(left,right,mode,ln,rn,5,90));
            }
        }
    }
    public static void main(String[] args) {
        fixed(); callbacksAndBudgets(); resourceLifetime(); randomized();
        yes(checks >= 8_000);
        System.out.println("M3_GROOVY_GINQ_JOIN_PASS checks=" + checks + " seededTrials=2000 joinModes=4");
    }
}
