/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 * @test
 * @summary IdentityHashMap bulk copies preserve entry protocol and avoid internal entry projections
 * @modules java.base/java.util:open
 * @run main/othervm --add-opens java.base/java.util=ALL-UNNAMED IdentityMapBulkContracts
 * @run main/othervm --add-opens java.base/java.util=ALL-UNNAMED IdentityMapBulkContracts --projection-budget
 */
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class IdentityMapBulkContracts {
    private IdentityMapBulkContracts() { }
    private static long checks;
    private static final Field TABLE = field("table");
    private static final Field MOD_COUNT = field("modCount");
    private static final Field ENTRY_SET = field("entrySet");
    private static final class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        Key(int id) { this.id = id; }
        @Override public boolean equals(Object other) { throw new AssertionError("user equals called"); }
        @Override public int hashCode() { throw new AssertionError("user hashCode called"); }
        @Override public String toString() { return "k" + id; }
    }
    private static Field field(String name) {
        try { Field f = IdentityHashMap.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object get(Field field, Object owner) {
        try { return field.get(owner); } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
    private static void check(boolean good, String reason) {
        checks++;
        if (!good) throw new AssertionError(reason + " at " + checks);
    }
    @SuppressWarnings("unchecked")
    private static IdentityHashMap<Object,Object> copy(IdentityHashMap<Object,Object> map) {
        return (IdentityHashMap<Object,Object>) map.clone();
    }
    private static void oldPutAll(IdentityHashMap<Object,Object> target, Map<?,?> source) {
        if (source.size() == 0) return;
        for (Map.Entry<?,?> entry : source.entrySet()) target.put(entry.getKey(), entry.getValue());
    }
    private static void same(IdentityHashMap<Object,Object> expected, IdentityHashMap<Object,Object> actual) {
        check(expected.size() == actual.size(), "mapping count");
        for (Map.Entry<Object,Object> e : expected.entrySet()) {
            check(actual.containsKey(e.getKey()), "identity key present");
            check(actual.get(e.getKey()) == e.getValue(), "value identity");
        }
        Object[] a = (Object[]) get(TABLE, expected), b = (Object[]) get(TABLE, actual);
        check(a.length == b.length, "capacity unchanged");
        for (int i = 0; i < a.length; i++) check(a[i] == b[i], "physical reinsertion order");
        check(get(MOD_COUNT, expected).equals(get(MOD_COUNT, actual)), "modCount evolution");
    }
    private static void randomized() {
        Random random = new Random(0x4944454e54495459L);
        Object[] keys = new Object[97], values = new Object[43];
        for (int i=0;i<keys.length;i++) keys[i] = i == 0 ? null : new Key(i % 13);
        for (int i=0;i<values.length;i++) values[i] = i == 0 ? null : new Key(1000 + i);
        for (int trial=0;trial<750;trial++) {
            IdentityHashMap<Object,Object> source = new IdentityHashMap<>(trial % 2 == 0 ? 4 : 300);
            for (int step=0;step<180;step++) {
                Object k=keys[random.nextInt(keys.length)];
                if (random.nextInt(4)==0) source.remove(k); else source.put(k,values[random.nextInt(values.length)]);
            }
            IdentityHashMap<Object,Object> seed = new IdentityHashMap<>(300);
            for (int step=0;step<30;step++) if (random.nextBoolean()) seed.put(keys[random.nextInt(keys.length)],values[random.nextInt(values.length)]);
            IdentityHashMap<Object,Object> expected=copy(seed), actual=copy(seed);
            Object[] before = ((Object[])get(TABLE,source)).clone();
            oldPutAll(expected,source); actual.putAll(source); same(expected,actual);
            Object[] after = (Object[])get(TABLE,source);
            for(int i=0;i<before.length;i++) check(before[i]==after[i],"source table unchanged");
            oldPutAll(expected,expected); actual.putAll(actual); same(expected,actual);
            IdentityHashMap<Object,Object> constructed=new IdentityHashMap<>(source);
            check(constructed.size()==source.size(),"map constructor count");
            for(Map.Entry<Object,Object> e:source.entrySet())check(constructed.get(e.getKey())==e.getValue() && constructed.containsKey(e.getKey()),"map constructor identity");
            if(!source.isEmpty()){
                Object key=source.keySet().iterator().next(); source.remove(key);
                check(constructed.containsKey(key),"copy does not alias source storage");
            }
        }
    }
    private static void wrappedCluster() throws Exception {
        IdentityHashMap<Object,Object> source = new IdentityHashMap<>(6);
        int length=((Object[])get(TABLE,source)).length;
        Method hash=IdentityHashMap.class.getDeclaredMethod("hash",Object.class,int.class);hash.setAccessible(true);
        List<Object> keys=new ArrayList<>();
        for(int attempt=0;keys.size()<6 && attempt<100000;attempt++){
            Object key=new Key(attempt);
            if(((Integer)hash.invoke(null,key,length))==length-2){keys.add(key);source.put(key,"v"+keys.size());}
        }
        check(keys.size()==6,"constructed wrapped identity cluster");
        IdentityHashMap<Object,Object> expected=new IdentityHashMap<>(6),actual=new IdentityHashMap<>(6);
        oldPutAll(expected,source);actual.putAll(source);same(expected,actual);
        check(Arrays.equals(serialized(expected),serialized(actual)),"serialized encounter order");
        source.remove(keys.get(2));expected.clear();actual.clear();oldPutAll(expected,source);actual.putAll(source);same(expected,actual);
    }
    private static byte[] serialized(Object value) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(value);}
        return bytes.toByteArray();
    }
    private static final class SourceSubclass extends IdentityHashMap<Object,Object> {
        private static final long serialVersionUID=1L;
        private final transient Object virtualKey,virtualValue;
        int entryCalls,sizeCalls;
        SourceSubclass(Object key,Object value){virtualKey=key;virtualValue=value;super.put(new Key(77),"hidden");}
        @Override public int size(){sizeCalls++;return super.size();}
        @Override public Set<Map.Entry<Object,Object>> entrySet(){entryCalls++;return Collections.singleton(new AbstractMap.SimpleImmutableEntry<>(virtualKey,virtualValue));}
    }
    private static final class Receiver extends IdentityHashMap<Object,Object> {
        private static final long serialVersionUID=1L;
        final transient List<Object> trace=new ArrayList<>();
        IdentityHashMap<Object,Object> source;
        transient Object extraKey,extraValue;
        int action,calls,trigger=1;
        RuntimeException failure;
        Receiver(){super(512);}
        @Override public Object put(Object key,Object value){
            trace.add(key);trace.add(value);calls++;
            if(calls==trigger){
                switch(action){
                    case 1 -> source.clear();
                    case 2 -> source.put(extraKey,extraValue);
                    case 3 -> source.remove(key);
                    case 4 -> source.put(key,extraValue);
                    case 5 -> { if(failure!=null)throw failure; }
                    case 6 -> { source.clear();source.put(extraKey,extraValue); }
                    default -> { }
                }
            }
            return super.put(key,value);
        }
    }
    private static Throwable transfer(Receiver receiver,IdentityHashMap<Object,Object> source,boolean old){
        try{if(old)oldPutAll(receiver,source);else receiver.putAll(source);return null;}catch(Throwable error){return error;}
    }
    private static void subclassAndReentrancy() {
        Object virtualKey=new Key(8000),virtualValue=new Key(9000);
        SourceSubclass subclass=new SourceSubclass(virtualKey,virtualValue);
        IdentityHashMap<Object,Object> target=new IdentityHashMap<>();target.putAll(subclass);
        check(subclass.entryCalls==1 && subclass.sizeCalls==1,"source subclass dispatch");
        check(target.size()==1 && target.get(virtualKey)==virtualValue,"source virtual entries");
        for(int population:new int[]{1,2,8,31}){
            IdentityHashMap<Object,Object> initial=new IdentityHashMap<>();
            for(int i=0;i<population;i++)initial.put(new Key(i),new Key(100+i));
            initial.put(null,null);
            for(int action=0;action<7;action++)for(int trigger:new int[]{1,2,population+1}){
                IdentityHashMap<Object,Object> a=copy(initial),b=copy(initial);
                Receiver expected=new Receiver(),actual=new Receiver();
                Object extraKey=new Key(700),extraValue=new Key(701);
                RuntimeException failure=new IllegalStateException("callback witness");
                for(Receiver r:new Receiver[]{expected,actual}){r.extraKey=extraKey;r.extraValue=extraValue;r.action=action;r.trigger=trigger;r.failure=failure;}
                expected.source=a;actual.source=b;
                Throwable x=transfer(expected,a,true),y=transfer(actual,b,false);
                check(x==null ? y==null : y!=null && x.getClass()==y.getClass(),"callback exception kind");
                if(x==failure)check(y==failure,"callback exception identity");
                check(expected.trace.size()==actual.trace.size(),"callback count");
                for(int i=0;i<expected.trace.size();i++)check(expected.trace.get(i)==actual.trace.get(i),"callback order and identities");
                same(expected,actual);same(a,b);
            }
        }
    }
    private static void viewsAndNull() {
        IdentityHashMap<Object,Object> source=new IdentityHashMap<>();
        Object key=new Key(1),value=new Key(2);source.put(null,null);source.put(key,value);
        IdentityHashMap<Object,Object> target=new IdentityHashMap<>(64);
        Set<Object> keys=target.keySet();Collection<Object> values=target.values();Set<Map.Entry<Object,Object>> entries=target.entrySet();
        Iterator<Object> held=keys.iterator();target.putAll(source);
        check(keys==target.keySet() && values==target.values() && entries==target.entrySet(),"view identities");
        check(keys.contains(null) && values.contains(null) && target.get(key)==value,"live views and null");
        boolean failed=false;try{held.next();}catch(ConcurrentModificationException expected){failed=true;}
        check(failed,"old target iterator detects structural insertion");
        Map.Entry<Object,Object> entry=entries.iterator().next();Object changed=new Key(3);entry.setValue(changed);
        check(target.get(entry.getKey())==changed,"held entry remains writable projection");
        boolean nullFailed=false;try{target.putAll(null);}catch(NullPointerException expected){nullFailed=true;}
        check(nullFailed,"null source");
        target.putAll(Collections.emptyMap());
    }
    private static void genericAccessors() {
        List<String> trace=new ArrayList<>();Object key=new Key(17),value=new Key(18);
        Map<Object,Object> source=new AbstractMap<>(){
            @Override public int size(){trace.add("size");return 1;}
            @Override public Set<Map.Entry<Object,Object>> entrySet(){trace.add("entries");return new AbstractSet<>(){
                @Override public int size(){return 1;}
                @Override public Iterator<Map.Entry<Object,Object>> iterator(){trace.add("iterator");return Collections.<Map.Entry<Object,Object>>singleton(new Map.Entry<>(){
                    @Override public Object getKey(){trace.add("key");return key;}
                    @Override public Object getValue(){trace.add("value");return value;}
                    @Override public Object setValue(Object v){throw new UnsupportedOperationException();}
                }).iterator();}
            };}
        };
        IdentityHashMap<Object,Object> target=new IdentityHashMap<>();target.putAll(source);
        check(trace.equals(List.of("size","entries","iterator","key","value")),"generic accessor order");
        check(target.get(key)==value,"generic mapping");
    }
    private static void projectionBudget() {
        IdentityHashMap<Object,Object> source=new IdentityHashMap<>(100);
        for(int i=0;i<64;i++)source.put(new Key(i),new Key(i+100));
        check(get(ENTRY_SET,source)==null,"fresh source entrySet unmaterialized");
        IdentityHashMap<Object,Object> destination=new IdentityHashMap<>(100);destination.putAll(source);
        check(destination.size()==64,"budget copy result");
        check(get(ENTRY_SET,source)==null,"bulk transfer must not materialize source EntrySet or Entry projections");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=0){projectionBudget();System.out.println("PASS projection budget checks="+checks);return;}
        randomized();wrappedCluster();subclassAndReentrancy();viewsAndNull();genericAccessors();
        System.out.println("PASS IdentityMapBulkContracts checks="+checks);
    }
}
