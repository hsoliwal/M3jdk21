// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import java.lang.reflect.Field;
import java.util.Random;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Repeated forced rollover exact-state differential fingerprint. */
public final class LFUOverflowTrace {
    private LFUOverflowTrace() { }
    private static Object field(Object o,String n) throws Exception {
        Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);
    }
    private static void setClock(Object o,long t) throws Exception {
        Field f=o.getClass().getDeclaredField("clock");f.setAccessible(true);f.setLong(o,t);
    }
    public static void main(String[] args) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(int capacity:new int[]{0,1,2,3,17,97}) {
            Object[] keys=new Object[128];
            for(int i=0;i<keys.length;i++)keys[i]=new Object();
            LFUCache<Object,Integer> c=new LFUCache<>(capacity);
            Random rng=new Random(0x4d334c46554f5646L+capacity);
            for(int step=0;step<800;step++) {
                if(step%11==0 && capacity!=0)setClock(c,Long.MAX_VALUE);
                Object key=keys[rng.nextInt(keys.length)];
                if(step%19==0) c.clear();
                else if(step%5==0)c.removeIdentity(key);
                else c.getOrCreate(key,k->197);
                StringBuilder row=new StringBuilder(capacity*40+40);
                row.append(capacity).append(',').append(step).append(',').append(c.size())
                   .append(',').append(field(c,"clock"));
                Object[] k=(Object[])field(c,"keys"),v=(Object[])field(c,"values");
                int[] freq=(int[])field(c,"frequencies");
                long[] stamp=(long[])field(c,"accessStamps");
                byte[] state=(byte[])field(c,"states");
                for(int j=0;j<capacity;j++){
                    int id=-1;
                    if(state[j]!=0){for(int p=0;p<keys.length;p++)if(k[j]==keys[p]){id=p;break;}}
                    row.append('|').append(state[j]).append(',').append(id).append(',')
                       .append(v[j]).append(',').append(freq[j]).append(',').append(stamp[j]);
                }
                digest.update(row.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        System.out.println("LFU_ROLLOVER_TRACE_SHA256="+HexFormat.of().formatHex(digest.digest()));
    }
}
