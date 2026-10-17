/*
 * Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Planned source-difference discriminators, not an execution or equivalence claim.
 * Each JSON line becomes runtime evidence only when this program is actually executed
 * against the provisioned M3 Android-derived body and real Guava 33.4.8-jre binary.
 */
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.m3.collections.guava.M3HashBiMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Public-API differential inventory; no provider implementation is a candidate storage owner. */
public final class M3HashBiMapProtocolDiscriminator {
    private M3HashBiMapProtocolDiscriminator() { }

    private static final class Token {
        final int logicalId;
        final String label;
        Token(int logicalId,String label) { this.logicalId=logicalId;this.label=label; }
        @Override public int hashCode() { return 17; }
        @Override public boolean equals(Object other) {
            return other instanceof Token token&&logicalId==token.logicalId;
        }
        @Override public String toString() { return label; }
    }

    private static final class ProbeFailure extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID=1L;
        ProbeFailure() { super("deliberate callback failure"); }
    }

    private static final class Fixture {
        final Token[] keys=new Token[4];
        final Token[] values=new Token[4];
        final Token equalKey;
        final Token equalValue;
        final IdentityHashMap<Object,String> labels=new IdentityHashMap<>();
        Fixture() {
            for(int i=0;i<4;i++) {
                keys[i]=new Token(100+i,"k"+i+"a");
                values[i]=new Token(200+i,"v"+i+"a");
                labels.put(keys[i],keys[i].label);
                labels.put(values[i],values[i].label);
            }
            equalKey=new Token(keys[0].logicalId,"k0b");
            equalValue=new Token(values[0].logicalId,"v0b");
            labels.put(equalKey,equalKey.label);
            labels.put(equalValue,equalValue.label);
        }
        Object key(boolean inverse,int index) { return inverse?values[index]:keys[index]; }
        Object value(boolean inverse,int index) { return inverse?keys[index]:values[index]; }
        Object equalKey(boolean inverse) { return inverse?equalValue:equalKey; }
        Object equalValue(boolean inverse) { return inverse?equalKey:equalValue; }
        String label(Object object) {
            if(object==null)return "null";
            String known=labels.get(object);
            if(known!=null)return known;
            if(object instanceof String string)return string;
            if(object instanceof Boolean||object instanceof Number)return object.toString();
            throw new AssertionError("unlabelled payload returned by public API: "+object.getClass().getName());
        }
        Object normalize(Object object) {
            if(object==null||object instanceof Boolean||object instanceof Number||object instanceof String)
                return object;
            if(object instanceof Token)return label(object);
            if(object instanceof Map.Entry<?,?> entry)return "entry["+label(entry.getKey())+"="+label(entry.getValue())+"]";
            if(object instanceof Outcome outcome)return outcome.export();
            if(object instanceof List<?> list) {
                List<Object> normalized=new ArrayList<>();
                for(Object value:list)normalized.add(normalize(value));
                return normalized;
            }
            throw new AssertionError("unhandled API result shape: "+object.getClass().getName());
        }
    }

    @FunctionalInterface private interface Action {
        Object run(BiMap<Object,Object> map,Fixture fixture,boolean inverse,List<String> callbacks);
    }
    @FunctionalInterface private interface Supplier { Object get(); }
    @FunctionalInterface private interface CommonCheck {
        void check(BiMap<Object,Object> map,Fixture fixture,boolean inverse,Observation observed);
    }
    private record Case(String name,boolean inverse,boolean common,Action action,CommonCheck check) { }
    private record Outcome(Object result,String error) {
        Map<String,Object> export() { return object("return",result,"error",error); }
    }
    private record Observation(String before,String after,Outcome outcome,List<String> callbacks,
            List<String> invariants,List<String> commonFailures) {
        Map<String,Object> export() {
            return object("before",before,"after",after,"outcome",outcome.export(),"callbacks",callbacks,
                    "invariant_failures",invariants,"common_api_failures",commonFailures);
        }
    }

    private static Outcome observe(Fixture fixture,Supplier action) {
        try { return new Outcome(fixture.normalize(action.get()),""); }
        catch(RuntimeException failure) { return new Outcome(null,failure.getClass().getName()); }
    }

    private static String orderedState(BiMap<Object,Object> map,Fixture fixture) {
        StringBuilder result=new StringBuilder("size=").append(map.size()).append(";F[");
        appendEntries(result,map,fixture);
        result.append("];I[");
        appendEntries(result,map.inverse(),fixture);
        return result.append(']').toString();
    }
    private static void appendEntries(StringBuilder result,Map<Object,Object> map,Fixture fixture) {
        boolean first=true;
        for(Map.Entry<Object,Object> entry:map.entrySet()) {
            if(!first)result.append(',');
            first=false;
            result.append(fixture.label(entry.getKey())).append('=').append(fixture.label(entry.getValue()));
        }
    }
    private static void require(boolean condition,String description) {
        if(!condition)throw new AssertionError(description);
    }
    private static List<String> invariants(BiMap<Object,Object> map) {
        List<String> failures=new ArrayList<>();
        try {
            BiMap<Object,Object> inverse=map.inverse();
            require(inverse==map.inverse(),"cached inverse identity");
            require(inverse.inverse()==map,"inverse-of-inverse identity");
            require(map.size()==inverse.size(),"forward/inverse cardinality");
            require(map.size()==map.entrySet().size(),"entry cardinality");
            require(map.size()==map.keySet().size(),"key cardinality");
            Set<Object> values=map.values();
            require(map.size()==values.size(),"unique value cardinality");
            int count=0;
            for(Map.Entry<Object,Object> entry:map.entrySet()) {
                Object key=entry.getKey(),value=entry.getValue();
                require(map.containsKey(key),"forward key presence, including null");
                require(map.containsValue(value),"forward value presence, including null");
                require(inverse.containsKey(value),"inverse key presence, including null");
                require(Objects.equals(key,inverse.get(value)),"reverse association");
                count++;
            }
            require(count==map.size(),"forward iteration cardinality");
            count=0;
            for(Map.Entry<Object,Object> entry:inverse.entrySet()) {
                require(map.containsKey(entry.getValue()),"inverse has live forward key");
                require(Objects.equals(entry.getKey(),map.get(entry.getValue())),"forward association");
                count++;
            }
            require(count==inverse.size(),"inverse iteration cardinality");
            require(values.equals(new java.util.HashSet<>(values)),"values Set content equality");
        } catch(AssertionError|RuntimeException failure) {
            failures.add(failure.getClass().getName()+": "+failure.getMessage());
        }
        return failures;
    }

    private static Observation run(Case test,Fixture fixture,boolean candidate) {
        BiMap<Object,Object> root=candidate?M3HashBiMap.create():HashBiMap.create();
        for(int i=0;i<3;i++)root.put(fixture.keys[i],fixture.values[i]);
        BiMap<Object,Object> map=test.inverse()?root.inverse():root;
        String before=orderedState(map,fixture);
        List<String> callbacks=new ArrayList<>();
        Outcome outcome=observe(fixture,()->test.action().run(map,fixture,test.inverse(),callbacks));
        String after=orderedState(map,fixture);
        List<String> invariantFailures=invariants(root);
        List<String> commonFailures=new ArrayList<>();
        Observation observation=new Observation(before,after,outcome,List.copyOf(callbacks),
                invariantFailures,commonFailures);
        if(test.common()) {
            try { test.check().check(map,fixture,test.inverse(),observation); }
            catch(AssertionError|RuntimeException failure) {
                commonFailures.add(failure.getClass().getName()+": "+failure.getMessage());
            }
        }
        return observation;
    }

    private static final CommonCheck NO_CHECK=(map,fixture,inverse,observation)->{ };
    private static final CommonCheck DUPLICATE_UNCHANGED=(map,fixture,inverse,observation)->{
        require(observation.outcome().error().equals(IllegalArgumentException.class.getName()),
                "ordinary duplicate must throw IllegalArgumentException");
        require(observation.before().equals(observation.after()),"ordinary duplicate must leave map unchanged");
    };
    private static void add(List<Case> cases,String name,boolean inverse,Action action) {
        cases.add(new Case(name,inverse,false,action,NO_CHECK));
    }
    private static void common(List<Case> cases,String name,boolean inverse,Action action,CommonCheck check) {
        cases.add(new Case(name,inverse,true,action,check));
    }
    private static Map.Entry<Object,Object> entry(BiMap<Object,Object> map,Object key) {
        for(Map.Entry<Object,Object> entry:map.entrySet())
            if(Objects.equals(key,entry.getKey()))return entry;
        throw new AssertionError("missing fixture entry");
    }
    private static Iterator<?> viewIterator(BiMap<Object,Object> map,int kind) {
        return switch(kind) { case 0->map.keySet().iterator();case 1->map.values().iterator();default->map.entrySet().iterator(); };
    }

    private static List<Case> cases() {
        List<Case> cases=new ArrayList<>();
        for(boolean inverse:new boolean[]{false,true}) {
            String side=inverse?"inverse/":"forward/";
            for(int mode=0;mode<6;mode++) {
                final int selected=mode;
                String label=switch(mode) {
                    case 0->"replaceAll/swap";
                    case 1->"replaceAll/cycle";
                    case 2->"replaceAll/duplicate-new";
                    case 3->"replaceAll/duplicate-existing";
                    case 4->"replaceAll/throw-first";
                    default->"replaceAll/throw-second";
                };
                add(cases,side+label,inverse,(map,f,inv,trace)->{
                    map.replaceAll((key,value)->{
                        trace.add(f.label(key)+"="+f.label(value));
                        if(selected==4||selected==5&&Objects.equals(key,f.key(inv,1)))throw new ProbeFailure();
                        if(selected==0)return Objects.equals(value,f.value(inv,0))?f.value(inv,1)
                                :Objects.equals(value,f.value(inv,1))?f.value(inv,0):value;
                        if(selected==1)return Objects.equals(value,f.value(inv,0))?f.value(inv,1)
                                :Objects.equals(value,f.value(inv,1))?f.value(inv,2):f.value(inv,0);
                        if(selected==3)return f.value(inv,0);
                        return f.value(inv,3);
                    });
                    return null;
                });
            }
            for(int mode=0;mode<6;mode++) {
                final int selected=mode;
                String label=switch(mode) {
                    case 0->"forEach/insert";
                    case 1->"forEach/remove-current";
                    case 2->"forEach/remove-next";
                    case 3->"forEach/clear";
                    case 4->"forEach/replace-current";
                    default->"forEach/throw-second";
                };
                Action action=(map,f,inv,trace)->{
                    boolean[] changed={false};
                    map.forEach((key,value)->{
                        trace.add(f.label(key)+"="+f.label(value));
                        if(selected==5) {
                            if(Objects.equals(key,f.key(inv,1)))throw new ProbeFailure();
                            return;
                        }
                        if(changed[0])return;
                        changed[0]=true;
                        switch(selected) {
                            case 0->map.put(f.key(inv,3),f.value(inv,3));
                            case 1->map.remove(key);
                            case 2->map.remove(f.key(inv,1));
                            case 3->map.clear();
                            default->map.put(key,f.value(inv,3));
                        }
                    });
                    return null;
                };
                if(mode==5)common(cases,side+label,inverse,action,(map,f,inv,o)->{
                    require(o.outcome().error().equals(ProbeFailure.class.getName()),"callback exception class");
                    require(o.before().equals(o.after()),"read-only callback failure leaves map unchanged");
                    require(o.callbacks().size()==2,"callback trace reaches deliberate second failure");
                });
                else add(cases,side+label,inverse,action);
            }
            for(int mode=0;mode<8;mode++) {
                final int selected=mode;
                String label=switch(mode) {
                    case 0->"identity/put-equal-key-new-value";
                    case 1->"identity/put-equal-key-equal-value";
                    case 2->"identity/forcePut-equal-key-new-value";
                    case 3->"identity/forcePut-new-key-equal-value";
                    case 4->"identity/entry-setValue-equal-value";
                    case 5->"identity/entry-setValue-new-value";
                    case 6->"identity/inverse-put-equal-value-new-key";
                    default->"identity/inverse-forcePut-equal-value-new-key";
                };
                add(cases,side+label,inverse,(map,f,inv,trace)->switch(selected) {
                    case 0->map.put(f.equalKey(inv),f.value(inv,3));
                    case 1->map.put(f.equalKey(inv),f.equalValue(inv));
                    case 2->map.forcePut(f.equalKey(inv),f.value(inv,3));
                    case 3->map.forcePut(f.key(inv,3),f.equalValue(inv));
                    case 4->entry(map,f.key(inv,0)).setValue(f.equalValue(inv));
                    case 5->entry(map,f.key(inv,0)).setValue(f.value(inv,3));
                    case 6->map.inverse().put(f.equalValue(inv),f.key(inv,3));
                    default->map.inverse().forcePut(f.equalValue(inv),f.key(inv,3));
                });
            }
            common(cases,side+"ordinary-duplicate/new-key",inverse,
                    (map,f,inv,trace)->map.put(f.key(inv,3),f.value(inv,1)),DUPLICATE_UNCHANGED);
            common(cases,side+"ordinary-duplicate/existing-key",inverse,
                    (map,f,inv,trace)->map.put(f.key(inv,0),f.value(inv,1)),DUPLICATE_UNCHANGED);
            common(cases,side+"null-support",inverse,(map,f,inv,trace)->{
                List<Object> observations=new ArrayList<>();
                observations.add(map.put(null,null));
                observations.add(map.containsKey(null));
                observations.add(map.containsValue(null));
                observations.add(map.inverse().containsKey(null));
                observations.add(observe(f,()->map.put(f.key(inv,3),null)));
                return observations;
            },(map,f,inv,o)->{
                require(o.outcome().error().isEmpty(),"null support action completes");
                require(map.size()==4&&map.containsKey(null)&&map.containsValue(null),"null pair remains present");
                require(map.get(null)==null&&map.inverse().get(null)==null,"null forward/inverse association");
                require(!map.containsKey(f.key(inv,3)),"duplicate null value refuses new key");
                @SuppressWarnings("unchecked") List<Object> results=(List<Object>)o.outcome().result();
                @SuppressWarnings("unchecked") Map<String,Object> refused=(Map<String,Object>)results.get(4);
                require(IllegalArgumentException.class.getName().equals(refused.get("error")),"duplicate null rejection class");
            });
            common(cases,side+"forcePut/evicts-other-key",inverse,
                    (map,f,inv,trace)->map.forcePut(f.key(inv,0),f.value(inv,1)),(map,f,inv,o)->{
                        require(o.outcome().error().isEmpty(),"forcePut completes");
                        require(map.size()==2,"forcePut conflicting existing keys decreases size");
                        require(!map.containsKey(f.key(inv,1)),"forcePut evicts previous value owner");
                        require(Objects.equals(map.get(f.key(inv,0)),f.value(inv,1)),"forcePut installs requested pair");
                        require(f.label(f.value(inv,0)).equals(o.outcome().result()),"forcePut returns previous value");
                    });
            common(cases,side+"entry-setValue/duplicate-refused",inverse,
                    (map,f,inv,trace)->entry(map,f.key(inv,0)).setValue(f.value(inv,1)),DUPLICATE_UNCHANGED);
            for(int kind=0;kind<3;kind++) {
                final int selected=kind;
                common(cases,side+"view-lifecycle/"+kind,inverse,(map,f,inv,trace)->{
                    Iterator<?> iterator=viewIterator(map,selected);
                    List<Object> events=new ArrayList<>();
                    events.add(observe(f,()->{ iterator.remove();return "void"; }));
                    Object first=iterator.next();
                    events.add(f.normalize(first));
                    if(first instanceof Map.Entry<?,?>) {
                        @SuppressWarnings("unchecked") Map.Entry<Object,Object> returned=(Map.Entry<Object,Object>)first;
                        events.add(observe(f,()->returned.setValue(f.value(inv,3))));
                    }
                    events.add(observe(f,()->{ iterator.remove();return "void"; }));
                    events.add(observe(f,()->{ iterator.remove();return "void"; }));
                    while(iterator.hasNext()) {
                        events.add(observe(f,iterator::next));
                        events.add(observe(f,()->{ iterator.remove();return "void"; }));
                    }
                    events.add(observe(f,iterator::next));
                    return events;
                },(map,f,inv,o)->{
                    require(o.outcome().error().isEmpty(),"iterator lifecycle completes");
                    require(map.isEmpty(),"iterator remove visits and removes every mapping");
                    @SuppressWarnings("unchecked") List<Object> events=(List<Object>)o.outcome().result();
                    @SuppressWarnings("unchecked") Map<String,Object> before=(Map<String,Object>)events.get(0);
                    @SuppressWarnings("unchecked") Map<String,Object> after=(Map<String,Object>)events.get(events.size()-1);
                    require(IllegalStateException.class.getName().equals(before.get("error")),"remove before next rejects");
                    int removalIndex=selected==2?3:2;
                    @SuppressWarnings("unchecked") Map<String,Object> removed=(Map<String,Object>)events.get(removalIndex);
                    @SuppressWarnings("unchecked") Map<String,Object> repeated=(Map<String,Object>)events.get(removalIndex+1);
                    require("".equals(removed.get("error")),"first iterator removal succeeds");
                    require(IllegalStateException.class.getName().equals(repeated.get("error")),"repeated iterator removal rejects");
                    if(selected==2) {
                        @SuppressWarnings("unchecked") Map<String,Object> setter=(Map<String,Object>)events.get(2);
                        require("".equals(setter.get("error")),"entry setter before iterator removal succeeds");
                        require(f.label(f.value(inv,0)).equals(setter.get("return")),"entry setter returns previous value");
                    }
                    require(java.util.NoSuchElementException.class.getName().equals(after.get("error")),"exhausted next rejects");
                });
                add(cases,side+"view-external-replacement/"+kind,inverse,(map,f,inv,trace)->{
                    Iterator<?> iterator=viewIterator(map,selected);
                    List<Object> events=new ArrayList<>();
                    events.add(observe(f,iterator::next));
                    events.add(observe(f,()->map.put(f.key(inv,0),f.value(inv,3))));
                    events.add(observe(f,()->{ iterator.remove();return "void"; }));
                    events.add(observe(f,iterator::hasNext));
                    events.add(observe(f,iterator::next));
                    return events;
                });
            }
        }
        for(int mode=0;mode<4;mode++) {
            final int selected=mode;
            add(cases,"inverse/forcePut-order/"+mode,true,(map,f,inv,trace)->switch(selected) {
                case 0->map.forcePut(f.value(false,3),f.key(false,1));
                case 1->map.forcePut(f.value(false,0),f.key(false,3));
                case 2->map.forcePut(f.value(false,0),f.key(false,1));
                default->map.forcePut(f.equalValue,f.keys[1]);
            });
        }
        require(cases.size()==66,"discriminator case inventory drift");
        return List.copyOf(cases);
    }

    private static Map<String,Object> object(Object... pairs) {
        require(pairs.length%2==0,"JSON pair count");
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);
        return result;
    }
    private static String json(Object value) {
        if(value==null)return "null";
        if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof String string) {
            StringBuilder result=new StringBuilder("\"");
            for(int i=0;i<string.length();i++) {
                char c=string.charAt(i);
                switch(c) {
                    case '"'->result.append("\\\"");
                    case '\\'->result.append("\\\\");
                    case '\n'->result.append("\\n");
                    case '\r'->result.append("\\r");
                    case '\t'->result.append("\\t");
                    default->{
                        if(c<32) {
                            result.append("\\u");
                            String hex=Integer.toHexString(c);
                            result.append("0".repeat(4-hex.length())).append(hex);
                        } else result.append(c);
                    }
                }
            }
            return result.append('"').toString();
        }
        if(value instanceof Map<?,?> map) {
            StringBuilder result=new StringBuilder("{");
            boolean first=true;
            for(Map.Entry<?,?> entry:map.entrySet()) {
                if(!first)result.append(',');
                first=false;
                result.append(json(entry.getKey())).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if(value instanceof List<?> list) {
            StringBuilder result=new StringBuilder("[");
            for(int i=0;i<list.size();i++) {
                if(i!=0)result.append(',');
                result.append(json(list.get(i)));
            }
            return result.append(']').toString();
        }
        throw new AssertionError("unhandled JSON shape "+value.getClass().getName());
    }

    private static String oracleArtifact() {
        var source=HashBiMap.class.getProtectionDomain().getCodeSource();
        require(source!=null,"actual oracle CodeSource is required for runtime provenance");
        try {
            var fileName=java.nio.file.Path.of(source.getLocation().toURI()).getFileName();
            require(fileName!=null,"actual oracle artifact basename is required");
            return fileName.toString();
        } catch(java.net.URISyntaxException failure) {
            throw new AssertionError("cannot identify actual oracle artifact",failure);
        }
    }

    public static void main(String[] args) {
        require(args.length==0,"no mutable test options");
        int total=0,equivalent=0,different=0,commonCases=0,invariantFailures=0,commonFailures=0;
        for(Case test:cases()) {
            Fixture fixture=new Fixture();
            Observation candidate=run(test,fixture,true);
            Observation oracle=run(test,fixture,false);
            boolean same=candidate.before().equals(oracle.before())&&candidate.after().equals(oracle.after())
                    &&candidate.outcome().equals(oracle.outcome())&&candidate.callbacks().equals(oracle.callbacks());
            if(same)equivalent++;else different++;
            total++;
            if(test.common())commonCases++;
            invariantFailures+=candidate.invariants().size()+oracle.invariants().size();
            commonFailures+=candidate.commonFailures().size()+oracle.commonFailures().size();
            System.out.println(json(object("case",test.name(),"classification",
                    test.common()?"COMMON_API_ASSERTED":"SOURCE_DIFFERENCE_RUNTIME_INVENTORY",
                    "equivalent",same,"candidate",candidate.export(),"oracle",oracle.export())));
        }
        System.out.println(json(object("summary","M3_GUAVA_HASHBIMAP_PROTOCOL_INVENTORY","cases",total,
                "equivalent",equivalent,"different",different,"common_api_cases",commonCases,
                "invariant_failures",invariantFailures,"common_api_failures",commonFailures,
                "candidate_source","Guava 33.4.8 Android array donor-derived body",
                "oracle_api","com.google.common.collect.HashBiMap","oracle_artifact",oracleArtifact(),
                "scope","actual paired outcomes when executed; differences are inventory, not admission")));
        require(invariantFailures==0,"BiMap invariant failure; paired output retained");
        require(commonFailures==0,"common BiMap API failure; paired output retained");
    }
}
