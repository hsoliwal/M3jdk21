/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 * @test
 * @summary Regex effect reception preserves M3 String ownership, seams, fallback and eviction
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexEffectsTest on
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexEffectsTest on
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexEffectsTest off
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public final class M3StringRegexEffectsTest {
    public static void main(String[] args) throws Exception {
        boolean enabled=args[0].equals("on");
        Field backing=null;Method candidate=null,evict=null;
        if(enabled){
            backing=String.class.getDeclaredField("m3");backing.setAccessible(true);
            Class<?> owner=Class.forName("java.lang.M3String"),dp=Class.forName("java.lang.M3StringRegexDp");
            candidate=dp.getDeclaredMethod("tryMatch",owner,String.class,boolean.class);candidate.setAccessible(true);
            evict=dp.getDeclaredMethod("clearPrecompute");evict.setAccessible(true);
            // Fail if the target libjava did not actually receive/register this symbol.
            Method nativeJoin=Class.forName("jdk.internal.mindex.M3RX").getDeclaredMethod("composeRows0",long[].class,long[].class,int.class);
            nativeJoin.setAccessible(true);
            long[] rows=(long[])nativeJoin.invoke(null,new long[]{1},new long[]{1},1);
            if(rows.length!=1||rows[0]!=1)throw new AssertionError("TARGET_JNI");
        }
        long checks=0;
        for(String text:new String[]{"abbbc","a\u0100b","a\nb","a\u2028b","a\ud83d\ude00b","xyabbbczz","a".repeat(256)+"b","a".repeat(128)+"\u0100"+"b".repeat(128)}) {
            for(int cut=0;cut<=text.length();cut++){
                String source=String.join("",text.substring(0,cut),text.substring(cut));
                for(String regex:new String[]{"ab*c","a.*b",".*","a*b*c*","(a)\\1","a+","(?=a).*"}) {
                    boolean expected=Pattern.compile(regex).matcher(new StringBuilder(source)).matches();
                    for(int pass=0;pass<4;pass++) {
                        if(source.matches(regex)!=expected)throw new AssertionError("STRING_MATCHES");checks++;
                        if(enabled){
                            Object m3=backing.get(source);if(m3==null)throw new AssertionError("M3_NOT_EXERCISED");
                            int decision=(int)candidate.invoke(null,m3,regex,true);
                            if(decision>=0&&(decision==1)!=expected)throw new AssertionError("NATIVE_CANDIDATE");checks++;
                        }
                    }
                    if(enabled)evict.invoke(null);
                }
            }
        }
        for(String bad:new String[]{"[","a**","("}){
            try{String.join("","a","b").matches(bad);throw new AssertionError("INVALID_PATTERN_ACCEPTED");}
            catch(java.util.regex.PatternSyntaxException expected){checks++;}
        }
        System.out.println("M3_STRING_REGEX_EFFECTS enabled="+enabled+" checks="+checks+" PASS");
    }
}
