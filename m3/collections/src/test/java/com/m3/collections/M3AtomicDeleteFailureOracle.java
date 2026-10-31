/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;

/** Deliberately corrupt only test-owned counters to force the compaction validation failure. */
public final class M3AtomicDeleteFailureOracle {
    private static int checks;
    private M3AtomicDeleteFailureOracle() { }
    private static Field field(String name) {
        try { Field f=M3LongLongHashMap.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object get(Field f, M3LongLongHashMap m) {
        try { return f.get(m); } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
    private static int getInt(Field f, M3LongLongHashMap m) {
        try { return f.getInt(m); } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
    private static void setInt(Field f, M3LongLongHashMap m, int value) {
        try { f.setInt(m, value); } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
    private static void check(boolean ok, String reason) {
        checks++; if (!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        checks=0;
        for (boolean conditional:new boolean[]{false,true}) {
            M3LongLongHashMap map = new M3LongLongHashMap(12);
            for (int k=0;k<12;k++) map.put(k,k*3L);
            Field kf=field("keys"), vf=field("values"), sf=field("states");
            Field of=field("occupied"), sz=field("size"), mod=field("modCount");
            // Force the already-existing copied-count invariant to fail AFTER all allocations.
            // This is a deterministic surrogate for a failure in the staged rehash path.
            setInt(of,map,map.capacity());
            setInt(sz,map,map.size()+1);
            long[] keys=(long[])get(kf,map), values=(long[])get(vf,map);
            byte[] states=(byte[])get(sf,map);
            long[] priorKeys=keys.clone(), priorValues=values.clone(); byte[] priorStates=states.clone();
            int oldSize=getInt(sz,map), oldOcc=getInt(of,map), oldEpoch=getInt(mod,map);
            M3LongIterator cursor=map.keyIterator();
            try { if(conditional)map.remove(5L,15L); else map.remove(5L); throw new AssertionError("expected staged failure"); }
            catch (IllegalStateException expected) { checks++; }
            check(get(kf,map)==keys&&get(vf,map)==values&&get(sf,map)==states,"owner array identity");
            check(Arrays.equals(keys,priorKeys),"keys preserved");
            check(Arrays.equals(values,priorValues),"values preserved");
            check(Arrays.equals(states,priorStates),"states preserved");
            check(getInt(sz,map)==oldSize&&getInt(of,map)==oldOcc&&getInt(mod,map)==oldEpoch,"counts and epoch preserved");
            check(map.containsKey(5)&&map.getOrDefault(5,-1)==15L,"failed deletion still present");
            try { cursor.hasNext(); checks++; }
            catch (ConcurrentModificationException cme) { throw new AssertionError("failed deletion invalidated iterator",cme); }
        }
        System.out.println("M3_ATOMIC_DELETE_FAILURE_PASS checks="+checks);
    }
}
