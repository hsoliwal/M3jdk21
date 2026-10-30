/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/Json.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
package jdk.m3.llm.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small strict JSON codec for deterministic receipts and the invocation protocol. */
public final class Json {
    private Json() { }
    public static Object parse(String text) {
        return parse(text,4_000_000);
    }
    public static Object parse(String text,int maximumCharacters) {
        if(maximumCharacters<1||text.length()>maximumCharacters)throw new IllegalArgumentException("JSON budget exceeded");
        var parser=new Parser(text);Object value=parser.value(0);parser.space();
        if(parser.index!=text.length())throw new IllegalArgumentException("trailing JSON data");return value;
    }
    public static String write(Object value) {
        if(value==null)return "null";
        if(value instanceof Boolean || value instanceof BigInteger || value instanceof Integer || value instanceof Long)return value.toString();
        if(value instanceof String s)return quote(s);
        if(value instanceof List<?> list)return "["+String.join(",",list.stream().map(Json::write).toList())+"]";
        if(value instanceof Map<?,?> map){var entries=new ArrayList<String>();
            for(var entry:map.entrySet()){if(!(entry.getKey() instanceof String key))throw new IllegalArgumentException("JSON key must be string");entries.add(quote(key)+":"+write(entry.getValue()));}
            return "{"+String.join(",",entries)+"}";}
        throw new IllegalArgumentException("unsupported JSON value "+value.getClass());
    }
    public static Object typed(Object value) {
        if(value instanceof Py.Range r)return Map.of("$range",List.of(r.start(),r.stop(),r.step()));
        if(value instanceof Py.Tuple t)return Map.of("$tuple",t.values().stream().map(Json::typed).toList());
        if(value instanceof Set<?> s){var values=new ArrayList<>(s.stream().map(Json::typed).toList());values.sort(Comparator.comparing(Json::write));return Map.of("$set",values);}
        if(value instanceof List<?> list)return list.stream().map(Json::typed).toList();
        if(value instanceof Map<?,?> map){var pairs=new ArrayList<Object>();for(var e:map.entrySet()){var pair=new ArrayList<Object>();pair.add(typed(e.getKey()));pair.add(typed(e.getValue()));pairs.add(pair);}return Map.of("$dict",pairs);}
        return value;
    }
    public static Object untyped(Object value) {
        if(value instanceof List<?> list){var result=new ArrayList<Object>();for(Object v:list)result.add(untyped(v));return result;}
        if(value instanceof Map<?,?> map){
            if(map.size()==1&&map.get("$tuple") instanceof List<?> list)return new Py.Tuple(list.stream().map(Json::untyped).toList());
            if(map.size()==1&&map.get("$set") instanceof List<?> list)return Py.set(list.stream().map(Json::untyped).toArray());
            if(map.size()==1&&map.get("$dict") instanceof List<?> pairs){var result=Py.dict();for(Object pair:pairs){var p=(List<?>)pair;result.put(untyped(p.get(0)),untyped(p.get(1)));}return result;}
            var result=Py.dict();map.forEach((k,v)->result.put(k,untyped(v)));return result;
        }
        return value;
    }
    private static String quote(String text) {
        var out=new StringBuilder("\"");
        for(int i=0;i<text.length();i++){char c=text.charAt(i);switch(c){
            case '"' -> out.append("\\\"");case '\\' -> out.append("\\\\");case '\n' -> out.append("\\n");case '\r' -> out.append("\\r");case '\t' -> out.append("\\t");case '\b' -> out.append("\\b");case '\f' -> out.append("\\f");
            default -> {if(c<32||c>126)out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}
        }}return out.append('"').toString();
    }
    private static final class Parser {
        private final String text;private int index;
        Parser(String text){this.text=text;}
        void space(){while(index<text.length()&&" \r\n\t".indexOf(text.charAt(index))>=0)index++;}
        Object value(int depth){if(depth>128)throw fail();space();if(index>=text.length())throw fail();char c=text.charAt(index);
            if(c=='"')return string();
            if(c=='['){index++;var out=new ArrayList<Object>();space();if(take(']'))return out;do{out.add(value(depth+1));space();if(take(']'))return out;}while(take(','));throw fail();}
            if(c=='{'){index++;var out=new LinkedHashMap<String,Object>();space();if(take('}'))return out;do{space();String key=string();space();if(!take(':')||out.containsKey(key))throw fail();out.put(key,value(depth+1));space();if(take('}'))return out;}while(take(','));throw fail();}
            for(String keyword:List.of("true","false","null")){if(text.startsWith(keyword,index)){index+=keyword.length();return keyword.equals("null")?null:keyword.equals("true");}}
            int begin=index;if(take('-')&&index>=text.length())throw fail();
            if(take('0')){if(index<text.length()&&Character.isDigit(text.charAt(index)))throw fail();}
            else{if(index>=text.length()||text.charAt(index)<'1'||text.charAt(index)>'9')throw fail();while(index<text.length()&&text.charAt(index)>='0'&&text.charAt(index)<='9')index++;}
            return new BigInteger(text.substring(begin,index));
        }
        String string(){if(!take('"'))throw fail();var out=new StringBuilder();while(index<text.length()){char c=text.charAt(index++);if(c=='"')return out.toString();if(c<32)throw fail();if(c!='\\'){out.append(c);continue;}if(index>=text.length())throw fail();char esc=text.charAt(index++);switch(esc){
            case '"','\\','/' -> out.append(esc);case 'b' -> out.append('\b');case 'f' -> out.append('\f');case 'n' -> out.append('\n');case 'r' -> out.append('\r');case 't' -> out.append('\t');
            case 'u' -> {if(index+4>text.length())throw fail();for(int end=index+4,j=index;j<end;j++){char h=text.charAt(j);if(!(h>='0'&&h<='9'||h>='a'&&h<='f'||h>='A'&&h<='F'))throw fail();}out.append((char)Integer.parseInt(text.substring(index,index+4),16));index+=4;}
            default -> throw fail();}}
            throw fail();}
        boolean take(char c){if(index<text.length()&&text.charAt(index)==c){index++;return true;}return false;}
        IllegalArgumentException fail(){return new IllegalArgumentException("invalid JSON at "+index);}
    }
}
