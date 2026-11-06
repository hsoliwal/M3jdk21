/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ConcurrentModificationException;

/** Drives the actual tombstone cleanup branch and compares byte-exact physical state. */
public final class M3LongMapCompactionSuccessOracle {
    private static final Field KEYS=field("keys"), VALUES=field("values"), STATES=field("states");
    private static final Field SIZE=field("size"), OCCUPIED=field("occupied"), MOD=field("modCount");
    private static int checks, compactions;
    private M3LongMapCompactionSuccessOracle() { }

    private static Field field(String name) {
        try { Field f=M3LongLongHashMap.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static Object get(Field f,M3LongLongHashMap m) {
        try { return f.get(m); } catch (IllegalAccessException error) { throw new AssertionError(error); }
    }
    private static int number(Field f,M3LongLongHashMap m) {
        try { return f.getInt(m); } catch (IllegalAccessException error) { throw new AssertionError(error); }
    }
    private static void assertThat(boolean condition,String description) {
        checks++; if(!condition) throw new AssertionError(description+" check="+checks);
    }
    private static void appendLong(MessageDigest digest,long value) {
        for(int shift=56;shift>=0;shift-=8) digest.update((byte)(value>>>shift));
    }
    private static void snapshot(MessageDigest digest,M3LongLongHashMap m) {
        appendLong(digest,number(SIZE,m)); appendLong(digest,number(OCCUPIED,m));
        appendLong(digest,number(MOD,m));
        long[] keys=(long[])get(KEYS,m), values=(long[])get(VALUES,m);
        byte[] states=(byte[])get(STATES,m);
        assertThat(keys.length==values.length&&keys.length==states.length,"lane geometry");
        appendLong(digest,keys.length);
        for(int i=0;i<states.length;i++) {
            digest.update(states[i]); appendLong(digest,keys[i]); appendLong(digest,values[i]);
        }
    }
    private static long key(int n) {
        return switch(n) { case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE;
            case 2 -> 0L; default -> n*17L-71L; };
    }
    public static void main(String[] args) throws Exception {
        checks=0; compactions=0;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(int n:new int[]{20,40,80}) {
            M3LongLongHashMap map=new M3LongLongHashMap(n);
            Map<Long,Long> model=new HashMap<>();
            for(int i=0;i<n;i++) {
                long k=key(i),v=i*29L+7L;
                assertThat(map.put(k,v),"unique insert");model.put(k,v);
            }
            assertThat(map.size()==n,"initial size");
            int capacity=map.capacity();
            for(int i=0;i<n-1;i++) {
                long k=key(i),v=model.get(k);
                long[] old=(long[])get(KEYS,map);
                M3LongIterator oldCursor=map.keyIterator();
                if((i&1)==0) assertThat(map.remove(k),"remove");
                else assertThat(map.remove(k,v),"conditional remove");
                model.remove(k);
                assertThat(map.size()==model.size(),"post-delete size");
                assertThat(map.capacity()==capacity,"compaction should not resize");
                for(Map.Entry<Long,Long> e:model.entrySet())
                    assertThat(map.containsKey(e.getKey()) && map.getOrDefault(e.getKey(),-1)==e.getValue(),"remaining value");
                assertThat(!map.containsKey(k),"deleted key absent");
                try { oldCursor.hasNext(); throw new AssertionError("stale iterator accepted"); }
                catch(ConcurrentModificationException expected) { checks++; }
                if(old!=get(KEYS,map)) {
                    compactions++;
                    byte[] state=(byte[])get(STATES,map);
                    for(byte b:state) assertThat(b!=2,"compaction must purge tombstones");
                    assertThat(number(OCCUPIED,map)==map.size(),"compaction occupancy");
                }
                snapshot(digest,map);
            }
            assertThat(map.size()==1,"retained final key");
            assertThat(Arrays.equals(map.keysToArray(),new long[]{key(n-1)}),"final key");
            map.clear(); snapshot(digest,map);
        }
        assertThat(compactions>=3,"actual compaction branch not covered");
        System.out.printf("M3_COMPACTION_SUCCESS_PASS checks=%d compactions=%d sha256=%s%n",
                checks,compactions,java.util.HexFormat.of().formatHex(digest.digest()));
    }
}
