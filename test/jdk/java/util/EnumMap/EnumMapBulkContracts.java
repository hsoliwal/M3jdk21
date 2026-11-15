/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 * @test
 * @summary EnumMap bulk ordinal copies preserve nulls, key types, subclasses, views and serialization
 * @modules java.base/java.util:open
 * @run main/othervm --add-opens java.base/java.util=ALL-UNNAMED EnumMapBulkContracts
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class EnumMapBulkContracts {
    private EnumMapBulkContracts() { }
    enum Tiny { A, B, C, D, E { @Override public String toString(){return "custom";} } }
    enum Other { A, B }
    enum Empty { }
    enum Wide { K0, K1, K2, K3, K4, K5, K6, K7, K8, K9, K10, K11, K12, K13, K14, K15, K16, K17, K18, K19, K20, K21, K22, K23, K24, K25, K26, K27, K28, K29, K30, K31, K32, K33, K34, K35, K36, K37, K38, K39, K40, K41, K42, K43, K44, K45, K46, K47, K48, K49, K50, K51, K52, K53, K54, K55, K56, K57, K58, K59, K60, K61, K62, K63, K64, K65, K66, K67, K68, K69, K70, K71, K72, K73, K74, K75, K76, K77, K78, K79, K80, K81, K82, K83, K84, K85, K86, K87, K88, K89, K90, K91, K92, K93, K94, K95, K96, K97, K98, K99, K100, K101, K102, K103, K104, K105, K106, K107, K108, K109, K110, K111, K112, K113, K114, K115, K116, K117, K118, K119, K120, K121, K122, K123, K124, K125, K126, K127, K128, K129, K130, K131, K132, K133, K134, K135, K136, K137, K138, K139, K140, K141, K142, K143, K144, K145, K146, K147, K148, K149, K150, K151, K152, K153, K154, K155, K156, K157, K158, K159, K160, K161, K162, K163, K164, K165, K166, K167, K168, K169, K170, K171, K172, K173, K174, K175, K176, K177, K178, K179, K180, K181, K182, K183, K184, K185, K186, K187, K188, K189, K190, K191, K192, K193, K194, K195, K196, K197, K198, K199, K200, K201, K202, K203, K204, K205, K206, K207, K208, K209, K210, K211, K212, K213, K214, K215, K216, K217, K218, K219, K220, K221, K222, K223, K224, K225, K226, K227, K228, K229, K230, K231, K232, K233, K234, K235, K236, K237, K238, K239, K240, K241, K242, K243, K244, K245, K246, K247, K248, K249, K250, K251, K252, K253, K254, K255, K256 }
    private static long checks;
    private static final Field VALUES=field("vals");
    private static Field field(String name){try{Field f=EnumMap.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static Object array(EnumMap<?,?> map){try{return VALUES.get(map);}catch(IllegalAccessException e){throw new AssertionError(e);}}
    private static void check(boolean good,String why){checks++;if(!good)throw new AssertionError(why+" at "+checks);}
    private static <E extends Enum<E>> void same(EnumMap<E,Object> expected,EnumMap<E,Object> actual,Class<E> kind){
        check(expected.size()==actual.size(),"size");
        for(E key:kind.getEnumConstants()){
            check(expected.containsKey(key)==actual.containsKey(key),"presence");
            check(expected.get(key)==actual.get(key),"value identity including null");
        }
        Iterator<E> a=expected.keySet().iterator(),b=actual.keySet().iterator();
        while(a.hasNext()){check(b.hasNext(),"order count");check(a.next()==b.next(),"ordinal order");}
        check(!b.hasNext(),"order end");
    }
    private static <E extends Enum<E>> void trials(Class<E> type,long seed){
        Random random=new Random(seed);E[] universe=type.getEnumConstants();Object[] values={null,new Object(),new Object(),"same"};
        for(int trial=0;trial<800;trial++){
            EnumMap<E,Object> source=new EnumMap<>(type),start=new EnumMap<>(type);
            for(E key:universe){
                if(random.nextInt(4)!=0)source.put(key,values[random.nextInt(values.length)]);
                if((trial&3)!=0 && random.nextInt(5)==0)start.put(key,values[random.nextInt(values.length)]);
            }
            if(trial%7==0)source.clear();
            EnumMap<E,Object> expected=new EnumMap<>(start),actual=new EnumMap<>(start);
            EnumMap<E,Object> before=new EnumMap<>(source);
            for(Map.Entry<E,Object> entry:source.entrySet())expected.put(entry.getKey(),entry.getValue());
            Object existingArray=array(actual);actual.putAll(source);
            check(array(actual)==existingArray,"destination ordinal lane retained");same(expected,actual,type);same(before,source,type);
            actual.putAll(actual);same(expected,actual,type);
            source.clear();same(expected,actual,type);
        }
    }
    private static final class CountingMap<E extends Enum<E>> extends EnumMap<E,Object>{
        private static final long serialVersionUID=1L;
        int puts,entries,emptyCalls,sizeCalls;boolean hideEntries;Boolean forcedEmpty;
        CountingMap(Class<E> type){super(type);}
        @Override public Object put(E key,Object value){puts++;return super.put(key,value);}
        @Override public Set<Map.Entry<E,Object>> entrySet(){entries++;return hideEntries?Collections.emptySet():super.entrySet();}
        @Override public int size(){sizeCalls++;return super.size();}
        @Override public boolean isEmpty(){emptyCalls++;return forcedEmpty==null?super.isEmpty():forcedEmpty;}
        void reset(){puts=entries=emptyCalls=sizeCalls=0;}
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void subclassesAndTypes(){
        CountingMap<Tiny> source=new CountingMap<>(Tiny.class);source.put(Tiny.A,null);Object v=new Object();source.put(Tiny.E,v);source.hideEntries=true;source.reset();
        CountingMap<Tiny> target=new CountingMap<>(Tiny.class);target.hideEntries=true;target.reset();target.putAll(source);
        check(target.get(Tiny.E)==v && target.containsKey(Tiny.A),"physical enum mappings copied despite hidden entrySet");
        check(source.entries==0 && source.sizeCalls==0 && source.emptyCalls==0,"same-universe source callbacks unchanged");
        check(target.puts==0,"same-universe target put remains bypassed");
        CountingMap<Other> wrong=new CountingMap<>(Other.class);wrong.forcedEmpty=true;wrong.put(Other.A,v);wrong.reset();
        target.putAll((Map)wrong);check(wrong.emptyCalls==1,"wrong universe retains virtual isEmpty");check(target.get(Tiny.E)==v,"wrong universe reported empty leaves target");
        wrong.clear();wrong.forcedEmpty=false;wrong.reset();boolean cce=false;try{target.putAll((Map)wrong);}catch(ClassCastException expected){cce=true;}
        check(cce && wrong.emptyCalls==1,"wrong universe reported nonempty throws");
        EnumMap<Other,Object> emptyWrong=new EnumMap<>(Other.class);target.putAll((Map)emptyWrong);check(target.containsKey(Tiny.A),"empty foreign enum accepted");
        EnumMap<Empty,Object> zero=new EnumMap<>(Empty.class);zero.putAll(new EnumMap<>(Empty.class));check(zero.isEmpty(),"zero-constant universe");
        boolean npe=false;try{target.putAll(null);}catch(NullPointerException expected){npe=true;}check(npe,"null source");
        CountingMap<Tiny> genericTarget=new CountingMap<>(Tiny.class);Map foreign=new LinkedHashMap();foreign.put(Tiny.B,v);foreign.put(Other.B,v);
        cce=false;try{genericTarget.putAll(foreign);}catch(ClassCastException expected){cce=true;}
        check(cce && genericTarget.puts==2 && genericTarget.get(Tiny.B)==v,"generic partial progress and target dispatch");
        foreign.clear();foreign.put(Tiny.D,null);foreign.put(null,v);npe=false;
        try{genericTarget.putAll(foreign);}catch(NullPointerException expected){npe=true;}
        check(npe && genericTarget.containsKey(Tiny.D) && genericTarget.get(Tiny.D)==null,"generic null-key partial progress");
    }
    private static void viewsAndSerialization() throws Exception {
        EnumMap<Tiny,Object> source=new EnumMap<>(Tiny.class);source.put(Tiny.A,null);source.put(Tiny.E,"last");
        EnumMap<Tiny,Object> target=new EnumMap<>(Tiny.class);
        Set<Tiny> keys=target.keySet();Collection<Object> values=target.values();Set<Map.Entry<Tiny,Object>> entries=target.entrySet();Iterator<Tiny> weak=keys.iterator();
        target.putAll(source);
        check(keys==target.keySet() && values==target.values() && entries==target.entrySet(),"view identity");
        List<Tiny> observed=new ArrayList<>();weak.forEachRemaining(observed::add);
        check(observed.equals(List.of(Tiny.A,Tiny.E)),"held weak iterator sees ordinal additions");
        Map.Entry<Tiny,Object> held=entries.iterator().next();held.setValue("updated");check(target.get(Tiny.A)=="updated","entry projection writes through");
        check(source.get(Tiny.A)==null && source.containsKey(Tiny.A),"source retains null marker");
        EnumMap<Tiny,Object> expected=new EnumMap<>(Tiny.class);for(Map.Entry<Tiny,Object> e:source.entrySet())expected.put(e.getKey(),e.getValue());
        target.clear();target.putAll(source);check(Arrays.equals(bytes(expected),bytes(target)),"serialized form and order");
        Object restored;try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes(target)))){restored=in.readObject();}
        check(restored instanceof EnumMap<?,?>,"serialization owner");
        Map<?,?> copy=(Map<?,?>)restored;check(copy.size()==2 && copy.containsKey(Tiny.A) && copy.get(Tiny.A)==null && "last".equals(copy.get(Tiny.E)),"serialization contents");
    }
    private static byte[] bytes(Object value)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(value);}return bytes.toByteArray();}
    public static void main(String[] args)throws Exception{
        trials(Tiny.class,0x454e554dL);trials(Wide.class,0x4f5244494e414cL);trials(Empty.class,17);subclassesAndTypes();viewsAndSerialization();
        System.out.println("PASS EnumMapBulkContracts checks="+checks);
    }
}
