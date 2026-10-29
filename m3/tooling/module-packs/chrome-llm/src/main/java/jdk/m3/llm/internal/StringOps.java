/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/StringOps.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
package jdk.m3.llm.internal;

import jdk.m3.llm.summary.Data;
import java.math.BigInteger;
import java.util.*;

/** Python Unicode string operations used by mechanically lowered calls. */
public final class StringOps {
    private StringOps() { }
    public static final Set<String> METHODS=Set.of("strip","lstrip","rstrip","split","rsplit","splitlines","casefold","lower","upper","find","rfind","index","rindex","partition","rpartition","isspace","isascii","isdecimal");
    private static int integer(Object v,int lower,int upper){return Py.integer(v).max(BigInteger.valueOf(lower)).min(BigInteger.valueOf(upper)).intValue();}
    private static void arity(Object[] a,int min,int max){if(a.length<min||a.length>max)throw Py.error("TypeError","wrong argument count");}
    private static boolean cut(int cp,String chars){return chars==null?Data.space(cp):chars.codePoints().anyMatch(c->c==cp);}
    public static Object call(String text,String name,Object[] args){
        switch(name){
            case "casefold":arity(args,0,0);return Data.fold(text);
            case "lower":arity(args,0,0);return text.toLowerCase(Locale.ROOT);
            case "upper":arity(args,0,0);return text.toUpperCase(Locale.ROOT);
            case "strip", "lstrip", "rstrip":{arity(args,0,1);String chars=args.length==0||args[0]==null?null:Py.string(args[0]);int begin=0,end=text.length();if(!name.equals("rstrip"))while(begin<end&&cut(text.codePointAt(begin),chars))begin+=Character.charCount(text.codePointAt(begin));if(!name.equals("lstrip"))while(end>begin&&cut(text.codePointBefore(end),chars))end-=Character.charCount(text.codePointBefore(end));return text.substring(begin,end);}
            case "split", "rsplit":{arity(args,0,2);String separator=args.length==0||args[0]==null?null:Py.string(args[0]);if(args.length==2&&(Py.integer(args[1]).compareTo(BigInteger.valueOf(Long.MIN_VALUE))<0||Py.integer(args[1]).compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0))throw Py.error("OverflowError","Python index exceeds platform size");int maximum=args.length<2?-1:integer(args[1],-1,Py.LIMIT);return split(text,separator,maximum,name.equals("rsplit"));}
            case "splitlines":{arity(args,0,1);boolean keep=args.length==1&&Py.truth(args[0]);var result=new ArrayList<Object>();int start=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if("\n\r\u000b\f\u001c\u001d\u001e\u0085\u2028\u2029".indexOf(c)<0)continue;int end=i;if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;result.add(text.substring(start,keep?i+1:end));start=i+1;}if(start<text.length())result.add(text.substring(start));return result;}
            case "isspace":arity(args,0,0);return !text.isEmpty()&&text.codePoints().allMatch(Data::space);
            case "isascii":arity(args,0,0);return text.codePoints().allMatch(c->c<128);
            case "isdecimal":arity(args,0,0);return !text.isEmpty()&&text.codePoints().allMatch(Character::isDigit);
            case "partition", "rpartition":{arity(args,1,1);String sep=Py.string(args[0]);if(sep.isEmpty())throw Py.error("ValueError","empty separator");int at=name.equals("partition")?text.indexOf(sep):text.lastIndexOf(sep);return at<0?(name.equals("partition")?Py.tuple(text,"",""):Py.tuple("","",text)):Py.tuple(text.substring(0,at),sep,text.substring(at+sep.length()));}
            case "find", "rfind", "index", "rindex":{arity(args,1,3);String needle=Py.string(args[0]);int length=text.codePointCount(0,text.length());BigInteger start=args.length<2||args[1]==null?BigInteger.ZERO:Py.integer(args[1]),end=args.length<3||args[2]==null?BigInteger.valueOf(length):Py.integer(args[2]);if(start.signum()<0)start=start.add(BigInteger.valueOf(length)).max(BigInteger.ZERO);if(end.signum()<0)end=end.add(BigInteger.valueOf(length)).max(BigInteger.ZERO);end=end.min(BigInteger.valueOf(length));int found=-1;if(start.compareTo(end)<=0){int from=text.offsetByCodePoints(0,start.intValue()),to=text.offsetByCodePoints(0,end.intValue());String slice=text.substring(from,to);int index=name.startsWith("r")?slice.lastIndexOf(needle):slice.indexOf(needle);if(index>=0)found=start.intValue()+slice.codePointCount(0,index);}if(found<0&&name.endsWith("index"))throw Py.error("ValueError","substring not found");return BigInteger.valueOf(found);}
            default:throw Py.error("AttributeError","unsupported string method: "+name);
        }
    }
    private static List<Object> split(String text,String separator,int maximum,boolean right){
        if(separator!=null&&separator.isEmpty())throw Py.error("ValueError","empty separator");var result=new ArrayList<Object>();int begin=0,end=text.length(),count=0;
        if(right){while(true){if(separator==null){while(end>begin&&Data.space(text.codePointBefore(end)))end-=Character.charCount(text.codePointBefore(end));if(end==begin)break;if(maximum>=0&&count>=maximum){result.add(text.substring(begin,end));break;}int start=end;while(start>begin&&!Data.space(text.codePointBefore(start)))start-=Character.charCount(text.codePointBefore(start));result.add(text.substring(start,end));end=start;count++;}
                else{if(maximum>=0&&count>=maximum){result.add(text.substring(begin,end));break;}int at=text.lastIndexOf(separator,end-separator.length());if(at<0){result.add(text.substring(begin,end));break;}result.add(text.substring(at+separator.length(),end));end=at;count++;}if(result.size()>Py.LIMIT)throw Py.error("ResourceLimit","split budget");}Collections.reverse(result);return result;}
        while(true){if(separator==null){while(begin<end&&Data.space(text.codePointAt(begin)))begin+=Character.charCount(text.codePointAt(begin));if(begin==end)break;if(maximum>=0&&count>=maximum){result.add(text.substring(begin,end));break;}int stop=begin;while(stop<end&&!Data.space(text.codePointAt(stop)))stop+=Character.charCount(text.codePointAt(stop));result.add(text.substring(begin,stop));begin=stop;count++;}
            else{if(maximum>=0&&count>=maximum){result.add(text.substring(begin,end));break;}int at=text.indexOf(separator,begin);if(at<0){result.add(text.substring(begin,end));break;}result.add(text.substring(begin,at));begin=at+separator.length();count++;}if(result.size()>Py.LIMIT)throw Py.error("ResourceLimit","split budget");}return result;
    }
}
