/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
/*
 * @test
 * @summary Weak bounded M3Object plans, pressure and adaptation identity
 * @modules java.base/java.lang
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xmx128m M3ObjectPressureCacheTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint -Xmx128m M3ObjectPressureCacheTest
 */
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
public final class M3ObjectPressureCacheTest {
    static final class Sample { volatile int value = 42; }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] ignored) throws Exception {
        Class<?> rt=Class.forName("java.lang.M3ObjectRuntime");
        Method policy=rt.getDeclaredMethod("planCapacityFor",long.class,long.class);
        policy.setAccessible(true);
        check((int)policy.invoke(null,0L,1000L)==256,"normal cap");
        check((int)policy.invoke(null,750L,1000L)==64,"pressure cap");
        check((int)policy.invoke(null,875L,1000L)==16,"critical cap");
        Object source=new Sample();
        M3Object facade=source.toAdapter(M3Object.class);
        check(facade==M3Object.of(source),"canonical facade");
        MethodHandles.Lookup lookup=MethodHandles.privateLookupIn(Sample.class,MethodHandles.lookup());
        M3Object.Field first=facade.field(lookup,Sample.class,"value",int.class);
        M3Object.Field again=facade.field(lookup,Sample.class,"value",int.class);
        Field planField=M3Object.Field.class.getDeclaredField("plan");
        planField.setAccessible(true);
        check(planField.get(first)==planField.get(again),"exact lookup shares plan");
        List<M3Object.Field> retained=new ArrayList<>();
        for(int i=0;i<700;i++)
            retained.add(facade.field(MethodHandles.privateLookupIn(Sample.class,
                       MethodHandles.lookup()),Sample.class,"value",int.class));
        Field plans=rt.getDeclaredField("plans");plans.setAccessible(true);
        check(((Object[])plans.get(null)).length<=256,"bounded weak plan cache");
        check((int)first.get()==42 && (int)retained.get(600).get()==42,"evicted plan live");
        check(source.toAdapter(M3Object.class)==facade,"owner must not be LFU evicted");
        check(source.toAdapter(Object.class)==source,"assignable identity");
        check(source.toAdapter(String.class)==null,"unsupported conversion");
        try {source.toAdapter(null);throw new AssertionError("null Class accepted");}
        catch (NullPointerException expected) {}
        missOnlyAging(rt, facade, lookup, planField);
    }

    /** A hot plan's historical frequency must decrease during miss-only traffic. */
    private static void missOnlyAging(Class<?> runtime, M3Object facade,
                                     MethodHandles.Lookup lookup, Field planField)
            throws Exception {
        M3Object.Field hot = facade.field(lookup, Sample.class, "value", int.class);
        Object hotPlan = planField.get(hot);
        for (int i = 0; i < 8191; i++) {
            check(planField.get(facade.field(lookup, Sample.class, "value", int.class))
                    == hotPlan, "stable hot plan");
        }
        int before = frequencyOf(runtime, hotPlan);
        check(before >= 100, "fixture must establish hot frequency");
        // Each privateLookupIn returns a fresh authority object. All 8,192
        // requests miss the canonical weak plan cache.
        for (int i = 0; i < 8192; i++) {
            MethodHandles.Lookup fresh = MethodHandles.privateLookupIn(
                    Sample.class, MethodHandles.lookup());
            check(fresh != lookup, "distinct Lookup identity");
            facade.field(fresh, Sample.class, "value", int.class);
        }
        int after = frequencyOf(runtime, hotPlan);
        check(after > 0 && after < before,
                "LFU frequency must age during misses: before=" + before + ", after=" + after);
    }

    private static int frequencyOf(Class<?> runtime, Object selected) throws Exception {
        Field refs = runtime.getDeclaredField("plans");
        Field counts = runtime.getDeclaredField("planFrequency");
        refs.setAccessible(true);
        counts.setAccessible(true);
        Object[] slots = (Object[]) refs.get(null);
        int[] frequencies = (int[]) counts.get(null);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] instanceof java.lang.ref.WeakReference<?> weak
                    && weak.get() == selected) {
                return frequencies[i];
            }
        }
        return 0;
    }
}
