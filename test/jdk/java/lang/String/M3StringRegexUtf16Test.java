/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Code-point-correct regex effects across canonical UTF-16 fragments and repetitions
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexUtf16Test enabled
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexUtf16Test enabled
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexUtf16Test disabled
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
public final class M3StringRegexUtf16Test {
    private static int checks;
    private static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        for(String unit:new String[]{"\ud800","\udc00","\ud800\udc00","\udc00\ud800","a\ud800\udc00b","\udc00\n\ud800"}){
            String value=String.join("",unit.substring(0,1),unit.substring(1));
            for(int i=0;i<9;i++)value=value.concat(value);
            for(String regex:new String[]{".*","a*.","a*..",".*a*","(?s).*","(.*)","a*"}){
                check(value.matches(regex)==Pattern.compile(regex).matcher(new StringBuilder(value)).matches(),"paired String result");
                String slice=value.substring(1,value.length()-1);
                check(slice.matches(regex)==Pattern.matches(regex,new StringBuilder(slice)),"range result");
            }
        }
        if(args.length>0&&args[0].equals("enabled")){
            String unit=String.join("","\udc00","\ud800"),value=unit;
            for(int i=0;i<9;i++)value=value.concat(value);
            Field field=String.class.getDeclaredField("m3");field.setAccessible(true);Object m3=field.get(value);
            check(m3!=null,"real M3 storage required");
            Method match=Class.forName("java.lang.M3StringRegexDp").getDeclaredMethod("tryMatch",m3.getClass(),String.class);
            match.setAccessible(true);
            int result=(int)match.invoke(null,m3,".*");
            // Under pressure the optional lane may decline; public result must remain exact.
            check(result==-1||result==1,"M3 surrogate seam decision");
        }
        System.out.println("M3_UTF16_PUBLIC_CHECKS="+checks);
    }
}
