/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/app/Data.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
package jdk.m3.llm.summary;

import jdk.m3.llm.internal.Json;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Portable V4 canonical.py semantics: sorted keys, UTF-8, no floating-point values. */
public final class Data {
    private Data() { }
    public static final Comparator<String> TEXT_ORDER = (a,b) -> {
        var x=a.codePoints().iterator(); var y=b.codePoints().iterator();
        while(x.hasNext()&&y.hasNext()){int c=Integer.compare(x.nextInt(),y.nextInt());if(c!=0)return c;}
        return Boolean.compare(x.hasNext(),y.hasNext());
    };
    public static Map<String,Object> obj(Object... pairs) {
        if(pairs.length%2!=0)throw new IllegalArgumentException("unpaired fields");
        var out=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;
    }
    public static Map<String,Object> map(Object value) {
        if(!(value instanceof Map<?,?> source))throw new IllegalArgumentException("expected object");
        var result=new LinkedHashMap<String,Object>();
        source.forEach((k,v)->{if(!(k instanceof String s))throw new IllegalArgumentException("expected string key");result.put(s,v);});return result;
    }
    public static List<Object> list(Object value){if(!(value instanceof List<?> l))throw new IllegalArgumentException("expected array");return new ArrayList<>(l);}
    public static List<Map<String,Object>> rows(Object value){return list(value).stream().map(Data::map).toList();}
    public static String str(Object v){if(!(v instanceof String s))throw new IllegalArgumentException("expected string");return s;}
    public static String s(Map<String,Object> m,String key){return str(m.get(key));}
    public static int integer(Object v){if(v instanceof BigInteger b)return b.intValueExact();if(v instanceof Integer i)return i;if(v instanceof Long l)return Math.toIntExact(l);throw new IllegalArgumentException("expected integer");}
    public static int n(Map<String,Object> m,String key,int fallback){return m.containsKey(key)?integer(m.get(key)):fallback;}
    public static List<String> strings(Object v){return list(v).stream().map(Data::str).toList();}
    public static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public static String sha(String value){return sha(value.getBytes(StandardCharsets.UTF_8));}
    public static String sha(Path path)throws IOException {
        try{var h=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(path)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)h.update(b,0,n);}return HexFormat.of().formatHex(h.digest());}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    public static String hash(String namespace,Object value){return sha(namespace+"\0"+json(value));}
    public static String json(Object v){
        if(v==null||v instanceof Boolean||v instanceof BigInteger||v instanceof Integer||v instanceof Long)return Json.write(v);
        if(v instanceof String s)return quote(s);
        if(v instanceof List<?> l)return "["+String.join(",",l.stream().map(Data::json).toList())+"]";
        if(v instanceof Set<?> set){var ordered=new ArrayList<>(set);ordered.sort(Comparator.comparing(Data::json,TEXT_ORDER));return json(ordered);}
        if(v instanceof Map<?,?>){var m=map(v);var keys=new ArrayList<>(m.keySet());keys.sort(TEXT_ORDER);var entries=new ArrayList<String>();for(String k:keys)entries.add(quote(k)+":"+json(m.get(k)));return "{"+String.join(",",entries)+"}";}
        throw new IllegalArgumentException("unsupported canonical type: "+v.getClass());
    }
    private static String quote(String s){
        var b=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);switch(c){
            case '"' -> b.append("\\\"");case '\\' -> b.append("\\\\");case '\n' -> b.append("\\n");case '\r' -> b.append("\\r");case '\t' -> b.append("\\t");case '\b' -> b.append("\\b");case '\f' -> b.append("\\f");
            default -> {if(c<32)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else if(Character.isHighSurrogate(c)){if(i+1>=s.length()||!Character.isLowSurrogate(s.charAt(i+1)))throw new IllegalArgumentException("unpaired surrogate");b.append(c).append(s.charAt(++i));}else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("unpaired surrogate");else b.append(c);}
        }}return b.append('"').toString();
    }
    public static Object read(Path path)throws IOException{return Json.parse(Files.readString(path));}
    public static String clip(String s,int length){return s.substring(0,s.offsetByCodePoints(0,Math.min(length,s.codePointCount(0,s.length()))));}
    public static boolean space(int cp){return Character.isWhitespace(cp)||Character.isSpaceChar(cp)||cp==0x85;}
    public static String strip(String s){int start=0,end=s.length();while(start<end&&space(s.codePointAt(start)))start+=Character.charCount(s.codePointAt(start));while(end>start&&space(s.codePointBefore(end)))end-=Character.charCount(s.codePointBefore(end));return s.substring(start,end);}
    public static List<String> split(String s){var out=new ArrayList<String>();var b=new StringBuilder();s.codePoints().forEach(cp->{if(space(cp)){if(!b.isEmpty()){out.add(b.toString());b.setLength(0);}}else b.appendCodePoint(cp);});if(!b.isEmpty())out.add(b.toString());return out;}
    private static final Map<Integer,String> FOLD = loadFold();
    private static Map<Integer,String> loadFold(){
        try(var in=Data.class.getResourceAsStream("casefold.tsv")){if(in==null)throw new IllegalStateException("missing generated casefold table");var out=new HashMap<Integer,String>();for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){String[] fields=line.split("\t",2);out.put(Integer.parseInt(fields[0],16),str(Json.parse(fields[1])));}return Map.copyOf(out);}catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static String fold(String s){var out=new StringBuilder();s.codePoints().forEach(cp->{String replacement=FOLD.get(cp);if(replacement==null)out.appendCodePoint(cp);else out.append(replacement);});return out.toString();}
}
