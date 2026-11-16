/*
 * @test
 * @summary Array-backed ordered set/map and composite map compared with Commons contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main OrderedCollectionBulkTest
 */
import jdk.internal.m3.collections.ListOrderedSetView;
import jdk.internal.m3.collections.ListOrderedMapView;
import jdk.internal.m3.collections.CompositeMapView;
import java.util.*;
import java.lang.reflect.*;

public class OrderedCollectionBulkTest {
    private static long assertions;
    private static void assertThat(boolean ok, String why) { assertions++; if (!ok) throw new AssertionError(why); }
    private static void same(Object x, Object y, String why) { assertThat(Objects.equals(x,y),why+" "+x+" != "+y); }
    private static Class<? extends Throwable> throwsType(Runnable r) {try {r.run(); return null;}catch(Throwable t){return t.getClass();}}
    private static final class SetOracle {
        final ArrayList<Object> values = new ArrayList<>();
        boolean add(Object o) { if (values.contains(o)) return false; return values.add(o); }
        void add(int i, Object o) { if(i<0||i>values.size())throw new IndexOutOfBoundsException();if(!values.contains(o))values.add(i,o); }
        Object remove(int i) { return values.remove(i); }
        boolean remove(Object x) {return values.remove(x);}
    }
    private static void setSame(ListOrderedSetView<Object> actual,SetOracle expected) {
        same(actual.size(),expected.values.size(),"set size");
        same(actual.asList().size(),expected.values.size(),"set projected size");
        assertThat(Arrays.equals(actual.toArray(),expected.values.toArray()),"set snapshots");
        for(int i=0;i<expected.values.size();i++) {
            assertThat(actual.get(i)==expected.values.get(i),"set identity");
            assertThat(actual.asList().get(i)==expected.values.get(i),"set view identity");
            same(actual.indexOf(expected.values.get(i)),i,"set position");
        }
        same(throwsType(()->actual.asList().add("x")),UnsupportedOperationException.class,"unmodifiable list");
    }
    private static void sets() {
        Object[] pool = {null,"Aa","BB","a","b",Integer.valueOf(0),Integer.valueOf(1),Long.valueOf(0),Long.valueOf(1)};
        Random random = new Random(0x51f891L);
        for (int seed=0;seed<65;seed++) {
            SetOracle reference=new SetOracle(); ListOrderedSetView<Object> actual=new ListOrderedSetView<>();
            for(int step=0;step<230;step++) {
                Object v=pool[random.nextInt(pool.length)];int op=random.nextInt(5);
                if(op==0) same(actual.add(v),reference.add(v),"set add");
                else if(op==1) {int i=random.nextInt(reference.values.size()+1);actual.add(i,v);reference.add(i,v);}
                else if(op==2) same(actual.remove(v),reference.remove(v),"set remove value");
                else if(op==3&&!reference.values.isEmpty()) {int i=random.nextInt(reference.values.size());assertThat(actual.remove(i)==reference.remove(i),"set remove at");}
                else if(op==4&&step%43==0) {actual.clear();reference.values.clear();}
                setSame(actual,reference);
            }
        }
        ListOrderedSetView<Object> actual=new ListOrderedSetView<>();SetOracle reference=new SetOracle();
        for(int i=0;i<512;i++){
            Object v=collisionString(i); assertThat(actual.add(v),"unique hash collision");assertThat(reference.add(v),"reference add");
        }
        setSame(actual,reference);
        for(int i=0;i<512;i+=5) {String v=collisionString(i); same(actual.remove(v),reference.remove(v),"collision remove");}
        setSame(actual,reference);
        same(throwsType(()->actual.add(-1,"bad")),IndexOutOfBoundsException.class,"set negative index");
    }
    private static String collisionString(int n) {
        StringBuilder b=new StringBuilder();for(int bit=0;bit<9;bit++) b.append((n&(1<<bit))==0?"Aa":"BB");return b.toString();
    }
    private static final class MapOracle {
        final ArrayList<Object> keys=new ArrayList<>();final ArrayList<Object> values=new ArrayList<>();
        Object put(Object k,Object v) {int i=keys.indexOf(k);if(i>=0)return values.set(i,v);keys.add(k);values.add(v);return null;}
        Object put(int idx,Object k,Object v) {if(idx<0||idx>keys.size())throw new IndexOutOfBoundsException();int i=keys.indexOf(k);Object old=null;if(i>=0){old=values.remove(i);keys.remove(i);if(i<idx)idx--;}keys.add(idx,k);values.add(idx,v);return old;}
        Object remove(Object k){int i=keys.indexOf(k);return i<0?null:removeAt(i);}
        Object removeAt(int i){keys.remove(i);return values.remove(i);}
        Object get(Object k){int i=keys.indexOf(k);return i<0?null:values.get(i);}
    }
    private static void mapSame(ListOrderedMapView<Object,Object> actual,MapOracle exp) {
        same(actual.size(),exp.keys.size(),"map size");
        same(actual.keyList().size(),exp.keys.size(),"live key view size");
        same(actual.valueList().size(),exp.values.size(),"live value view size");
        for(int i=0;i<exp.keys.size();i++){
            assertThat(actual.get(i)==exp.keys.get(i),"ordered key at index");
            assertThat(actual.getValue(i)==exp.values.get(i),"ordered value at index");
            assertThat(actual.keyList().get(i)==exp.keys.get(i),"key view identity");
            assertThat(actual.valueList().get(i)==exp.values.get(i),"value view identity");
            same(actual.indexOf(exp.keys.get(i)),i,"key index");
            assertThat(actual.get(exp.keys.get(i))==exp.values.get(i),"map get by key");
        }
        same(throwsType(()->actual.keyList().add(new Object())),UnsupportedOperationException.class,"key view no add");
        same(throwsType(()->actual.valueList().add(new Object())),UnsupportedOperationException.class,"values view no add");
    }
    private static void maps() {
        Object[] keys={null,"Aa","BB","a","b",Integer.valueOf(0),Integer.valueOf(1),Long.valueOf(0)};
        Random rand=new Random(0x18a9faL);
        for(int seed=0;seed<54;seed++){
            MapOracle exp=new MapOracle();ListOrderedMapView<Object,Object> actual=new ListOrderedMapView<>();
            for(int step=0;step<250;step++){
                Object k=keys[rand.nextInt(keys.length)];Object v=rand.nextInt(8)==0?null:new Object();int op=rand.nextInt(7);
                if(op==0)assertThat(actual.put(k,v)==exp.put(k,v),"map put previous");
                else if(op==1){int i=rand.nextInt(exp.keys.size()+1);assertThat(actual.put(i,k,v)==exp.put(i,k,v),"map indexed put previous");}
                else if(op==2)assertThat(actual.remove(k)==exp.remove(k),"map remove by key");
                else if(op==3&&!exp.keys.isEmpty()){int i=rand.nextInt(exp.keys.size());assertThat(actual.remove(i)==exp.removeAt(i),"map remove index");}
                else if(op==4&&!exp.keys.isEmpty()){int i=rand.nextInt(exp.keys.size());assertThat(actual.valueList().set(i,v)==exp.values.set(i,v),"map values set");}
                else if(op==5&&!exp.keys.isEmpty()){int i=rand.nextInt(exp.keys.size());assertThat(actual.valueList().remove(i)==exp.removeAt(i),"map values remove");}
                else if(op==6&&step%51==0){actual.clear();exp.keys.clear();exp.values.clear();}
                mapSame(actual,exp);
            }
        }
        ListOrderedMapView<Object,Object> actual=new ListOrderedMapView<>();MapOracle exp=new MapOracle();
        for(int i=0;i<512;i++) {String k=collisionString(i);Object value=new Object();same(actual.put(k,value),exp.put(k,value),"hash collision put");}
        mapSame(actual,exp);
        for(int i=0;i<512;i+=11) {String k=collisionString(i);assertThat(actual.remove(k)==exp.remove(k),"hash collision remove");}
        mapSame(actual,exp);
        same(throwsType(()->actual.put(-1,"key","value")),IndexOutOfBoundsException.class,"map negative index");
    }
    private static final class CompositeOracle {
        final ArrayList<Map<Object,Object>> backings=new ArrayList<>();
        void add(Map<Object,Object> m){if(m==null)return;for(int i=backings.size()-1;i>=0;i--)for(Object k:m.keySet())if(backings.get(i).containsKey(k))throw new IllegalArgumentException();backings.add(m);}
        Map<Object,Object> remove(Map<Object,Object> map){for(int i=0;i<backings.size();i++)if(Objects.equals(backings.get(i),map)){backings.remove(i);return map;}return null;}
        Object get(Object key){for(int i=backings.size()-1;i>=0;i--)if(backings.get(i).containsKey(key))return backings.get(i).get(key);return null;}
        Object removeKey(Object key){for(int i=backings.size()-1;i>=0;i--)if(backings.get(i).containsKey(key))return backings.get(i).remove(key);return null;}
        boolean contains(Object key){for(int i=backings.size()-1;i>=0;i--)if(backings.get(i).containsKey(key))return true;return false;}
        int size(){long n=0;for(Map<Object,Object> m:backings)n+=m.size();return (int)Math.min(n,Integer.MAX_VALUE);}
        boolean isEmpty(){for(Map<Object,Object> m:backings)if(!m.isEmpty())return false;return true;}
        void clear(){for(int i=backings.size()-1;i>=0;i--)backings.get(i).clear();}
    }
    private static void compositeSame(CompositeMapView<Object,Object> actual,CompositeOracle exp) {
        same(actual.size(),exp.size(),"composite size");same(actual.isEmpty(),exp.isEmpty(),"composite empty");
        for(Object k:new Object[]{null,"a","b","c",1,2,3}) {
            same(actual.containsKey(k),exp.contains(k),"composite key");
            assertThat(actual.get(k)==exp.get(k),"composite ref value");
        }
    }
    private static void composites(){
        CompositeMapView<Object,Object> act=new CompositeMapView<>();CompositeOracle exp=new CompositeOracle();
        Map<Object,Object> first=new HashMap<>(),second=new HashMap<>();first.put("a",null);second.put("b",new Object());
        act.addComposited(first);exp.add(first);act.addComposited(second);exp.add(second);
        compositeSame(act,exp);same(act.get("a"),null,"present null");
        Map<Object,Object> overlap=new HashMap<>();overlap.put("a",new Object());
        CompositeMapView<Object,Object> initialComposite=act;
        same(throwsType(()->initialComposite.addComposited(overlap)),IllegalArgumentException.class,"collision refusal");
        compositeSame(act,exp);same(throwsType(()->initialComposite.put("c",new Object())),UnsupportedOperationException.class,"no mutator");
        second.put("c",new Object());compositeSame(act,exp);
        Map<Object,Object> older=act.removeComposited(first);Map<Object,Object> olderExp=exp.remove(first);assertThat(older==olderExp,"remove backing identity");
        compositeSame(act,exp);
        first.clear();second.clear();compositeSame(act,exp);
        Random rand=new Random(0x6cef416L);
        for(int seed=0;seed<65;seed++){
            act=new CompositeMapView<>();exp=new CompositeOracle();List<Map<Object,Object>> pool=new ArrayList<>();
            for(int x=0;x<8;x++)pool.add(new HashMap<>());
            for(int step=0;step<180;step++) {
                int idx=rand.nextInt(pool.size());Map<Object,Object> chosen=pool.get(idx);int op=rand.nextInt(6);
                if(op==0){CompositeMapView<Object,Object> a=act;CompositeOracle o=exp;Map<Object,Object> m=chosen;Class<? extends Throwable> ca=throwsType(()->a.addComposited(m));Class<? extends Throwable> cb=throwsType(()->o.add(m));same(ca,cb,"composite insertion collision");}
                else if(op==1){assertThat(act.removeComposited(chosen)==exp.remove(chosen),"remove composite source");}
                else if(op==2){Object key="k"+rand.nextInt(14);Object val=rand.nextInt(10)==0?null:new Object();chosen.put(key,val);}
                else if(op==3){Object key="k"+rand.nextInt(14);chosen.remove(key);}
                else if(op==4){Object key="k"+rand.nextInt(14);Object wanted=exp.get(key);assertThat(act.remove(key)==wanted,"composite remove key");}
                else if(op==5&&step%61==0){act.clear();exp.clear();}
                compositeSame(act,exp);
            }
        }
    }
    private static void storage() throws Exception {
        for(Class<?> type:List.of(ListOrderedSetView.class,ListOrderedMapView.class,CompositeMapView.class)){
            for(Field f:type.getDeclaredFields()) if(!Modifier.isStatic(f.getModifiers())) {
                assertThat(!java.util.Map.class.isAssignableFrom(f.getType()),"no retained map wrapper "+f);
                assertThat(!java.util.Collection.class.isAssignableFrom(f.getType()),"no retained collection wrapper "+f);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        sets();maps();composites();storage();
        System.out.println("OrderedCollectionBulkTest PASS assertions="+assertions);
    }
}
